package com.example.reconciliation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import jakarta.inject.Inject;
import javax.sql.DataSource;

/**
 * 對帳單 CSV 匯出服務
 *
 * 讀取 SQLite View {@code vw_saga_reconciliation} (位於 data/reconciliation.db),
 * 將紀錄寫成 UTF-8 (含 BOM) CSV 檔案,放到 {@code data/} 目錄下。
 *
 * 設計重點:
 *   - 檔名固定格式: {@code reconciliation-YYYYMMDD-HHmmss.csv},避免覆蓋
 *   - UTF-8 BOM: 讓 Excel 直接開啟中文不亂碼
 *   - 支援三種過濾條件 (workflow_id / business_key / status),null = 不過濾
 *   - 同時提供 workflow function 進入點,讓 SonataFlow 流程可在終點自動匯出
 *     (但預設 workflow 不會呼叫,保持現有行為 — 主要觸發介面為 REST endpoint)
 */
@ApplicationScoped
public class ReconciliationCsvExporter {

    private static final Logger LOG = LoggerFactory.getLogger(ReconciliationCsvExporter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DB_URL = "jdbc:sqlite:data/reconciliation.db";
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Inject
    DataSource dataSource;

    /** View 的所有欄位,也是 CSV 表頭 */
    private static final String[] COLUMNS = {
            "id", "created_at", "workflow_id", "business_key", "status", "message",
            "account_a", "account_b", "amt",
            "a16229_code", "a16229_msg", "a16220_code", "a16220_msg",
            "a16229_rollback_code", "a16220_rollback_code"
    };

    /** 應用啟動時確保 data 目錄存在 */
    void onStart(@Observes StartupEvent ev) {
        ensureDataDir();
    }

    private void ensureDataDir() {
        File dataDir = new File("data");
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            LOG.warn("[ReconciliationCsvExporter] 無法建立 data 目錄,匯出可能失敗");
        }
    }

    private Connection getConnection() throws Exception {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * CSV 欄位跳脫:含逗號 / 引號 / 換行時,以雙引號包裹,內部雙引號跳脫為 ""
     */
    static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuote = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return needsQuote ? "\"" + escaped + "\"" : escaped;
    }

    /**
     * 組出 CSV 檔案的完整路徑,位於 {@code data/} 目錄下
     */
    static Path resolveCsvPath() {
        ensureDataDirStatic();
        String fileName = "reconciliation-" + LocalDateTime.now().format(FILE_TS) + ".csv";
        return Paths.get("data", fileName);
    }

    /** 靜態版本,給靜態方法 resolveCsvPath() 使用 */
    private static void ensureDataDirStatic() {
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }

    /**
     * 讀取 view 並寫出 CSV 檔案。
     *
     * @param workflowId  過濾 workflow_id (null = 全部)
     * @param businessKey 過濾 business_key (null = 全部)
     * @param status      過濾 status (null = 全部)
     * @return JSON 結果 { filePath, fileName, rowCount, byteSize, exportedAt }
     */
    public JsonNode exportToCsv(String workflowId, String businessKey, String status) {
        ObjectNode response = MAPPER.createObjectNode();

        Path csvPath = resolveCsvPath();
        long byteSize = 0;
        int rowCount = 0;

        try (Connection conn = getConnection()) {
            // 先確認 view 是否存在(初次部署可能尚未建立)
            try (var rs = conn.getMetaData().getTables(null, null, "vw_saga_reconciliation", null)) {
                if (!rs.next()) {
                    response.put("ok", false);
                    response.put("error", "View vw_saga_reconciliation 不存在,請先執行 workflow 觸發 AuditLogService 初始化");
                    LOG.warn("[ReconciliationCsvExporter] View 不存在,取消匯出");
                    return response;
                }
            }

            // 動態組 SQL (3 個過濾條件獨立)
            StringBuilder sql = new StringBuilder("SELECT ");
            sql.append(String.join(", ", COLUMNS));
            sql.append(" FROM vw_saga_reconciliation WHERE 1=1");
            if (workflowId != null && !workflowId.isBlank()) {
                sql.append(" AND workflow_id = ?");
            }
            if (businessKey != null && !businessKey.isBlank()) {
                sql.append(" AND business_key = ?");
            }
            if (status != null && !status.isBlank()) {
                sql.append(" AND status = ?");
            }
            sql.append(" ORDER BY id ASC");

            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (workflowId != null && !workflowId.isBlank()) {
                    pstmt.setString(idx++, workflowId);
                }
                if (businessKey != null && !businessKey.isBlank()) {
                    pstmt.setString(idx++, businessKey);
                }
                if (status != null && !status.isBlank()) {
                    pstmt.setString(idx++, status);
                }

                try (ResultSet rs = pstmt.executeQuery();
                     BufferedWriter writer = new BufferedWriter(
                             new OutputStreamWriter(Files.newOutputStream(csvPath), StandardCharsets.UTF_8))) {

                    // UTF-8 BOM: 讓 Excel 開啟中文不亂碼
                    writer.write('\ufeff');

                    // 表頭
                    StringBuilder header = new StringBuilder();
                    for (int i = 0; i < COLUMNS.length; i++) {
                        if (i > 0) header.append(',');
                        header.append(escapeCsv(COLUMNS[i]));
                    }
                    writer.write(header.toString());
                    writer.newLine();

                    // 資料列
                    ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        StringBuilder row = new StringBuilder();
                        for (int i = 0; i < COLUMNS.length; i++) {
                            if (i > 0) row.append(',');
                            // 統一用 getString 處理,NULL 會回傳 null → 輸出空字串
                            String val = rs.getString(i + 1);
                            row.append(escapeCsv(val));
                        }
                        writer.write(row.toString());
                        writer.newLine();
                        rowCount++;
                    }
                    writer.flush();
                }
            }

            byteSize = Files.size(csvPath);

            response.put("ok", true);
            response.put("filePath", csvPath.toAbsolutePath().toString());
            response.put("fileName", csvPath.getFileName().toString());
            response.put("rowCount", rowCount);
            response.put("byteSize", byteSize);
            response.put("exportedAt", LocalDateTime.now().toString());
            response.put("filters", MAPPER.createObjectNode()
                    .put("workflowId", workflowId == null ? "" : workflowId)
                    .put("businessKey", businessKey == null ? "" : businessKey)
                    .put("status", status == null ? "" : status));

            LOG.info("[ReconciliationCsvExporter] 匯出完成: file={}, rows={}, bytes={}",
                    csvPath.getFileName(), rowCount, byteSize);

        } catch (Exception e) {
            LOG.error("[ReconciliationCsvExporter] 匯出失敗: {}", e.getMessage(), e);
            response.put("ok", false);
            response.put("error", e.getMessage());
        }

        return response;
    }

    // ====================================================================
    //  Workflow function 進入點 — 對應 SonataFlow 反射呼叫
    //  Workflow arguments 攤平為獨立 method param,故提供多個 overload:
    //    - exportReconciliationCsv(JsonNode)
    //    - exportReconciliationCsv(Map<String, Object>)
    //    - exportReconciliationCsv(String workflowId, String businessKey, String status)
    // ====================================================================

    /**
     * JsonNode 進入點 (Kogito 反射首選 — arguments 為 JsonNode)
     */
    public JsonNode exportReconciliationCsv(JsonNode arguments) {
        String workflowId = arguments != null && arguments.hasNonNull("workflowId") ? arguments.get("workflowId").asText() : null;
        String businessKey = arguments != null && arguments.hasNonNull("businessKey") ? arguments.get("businessKey").asText() : null;
        String status = arguments != null && arguments.hasNonNull("status") ? arguments.get("status").asText() : null;
        return exportToCsv(workflowId, businessKey, status);
    }

    /**
     * Map 進入點 (備援)
     */
    public JsonNode exportReconciliationCsv(Map<String, Object> arguments) {
        if (arguments == null) {
            return exportToCsv(null, null, null);
        }
        String workflowId = arguments.get("workflowId") == null ? null : arguments.get("workflowId").toString();
        String businessKey = arguments.get("businessKey") == null ? null : arguments.get("businessKey").toString();
        String status = arguments.get("status") == null ? null : arguments.get("status").toString();
        return exportToCsv(workflowId, businessKey, status);
    }

    /**
     * 多參數進入點 (workflow 顯式傳入時用)
     */
    public JsonNode exportReconciliationCsv(String workflowId, String businessKey, String status) {
        return exportToCsv(workflowId, businessKey, status);
    }

    /**
     * 無參數進入點 — 匯出全部紀錄 (供測試或快速觸發用)
     */
    public JsonNode exportReconciliationCsv() {
        return exportToCsv(null, null, null);
    }
}
