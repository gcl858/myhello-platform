package com.example.reconciliation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 通用型 Workflow 執行紀錄與對帳服務 (Audit & Reconciliation Service)
 *
 * 高併發優化版:
 *   1. 啟用 SQLite WAL (Write-Ahead Logging) 模式與 busy_timeout=5000
 *   2. 注入 Quarkus Agroal DataSource 連線池管理連線
 *   3. 使用 AtomicBoolean 確保資料庫結構與 PRAGMA 配置僅初始化一次，避免全域鎖與硬碟重複存取
 */
@ApplicationScoped
public class AuditLogService {

    private static final Logger LOG = LoggerFactory.getLogger(AuditLogService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DB_URL = "jdbc:sqlite:data/reconciliation.db";
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Inject
    DataSource dataSource;

    /**
     * 應用程式啟動時，確保 data 目錄與 SQLite 表格/視圖已初始化
     */
    void onStart(@Observes StartupEvent ev) {
        initDatabase();
    }

    private Connection getConnection() throws Exception {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * 初始化 SQLite 資料庫結構 (建立通用表與 Saga 對帳視圖)，全效能 WAL 模式設定
     */
    public void initDatabase() {
        if (initialized.get()) {
            return;
        }
        synchronized (this) {
            if (initialized.get()) {
                return;
            }
            try {
                // 確保 data 目錄存在
                File dataDir = new File("data");
                if (!dataDir.exists()) {
                    dataDir.mkdirs();
                }

                try (Connection conn = getConnection();
                     Statement stmt = conn.createStatement()) {

                    // 0. 啟用高併發 WAL 模式與繁忙超時設定
                    stmt.execute("PRAGMA journal_mode=WAL;");
                    stmt.execute("PRAGMA synchronous=NORMAL;");
                    stmt.execute("PRAGMA busy_timeout=5000;");

                    // 1. 建立全系統通用流程執行紀錄表
                    String createTableSql = "CREATE TABLE IF NOT EXISTS workflow_execution_log (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "workflow_id TEXT, " +
                            "business_key TEXT, " +
                            "status TEXT, " +
                            "message TEXT, " +
                            "input_data TEXT, " +
                            "output_data TEXT, " +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                            ");";
                    stmt.execute(createTableSql);

                    // 2. 建立 Saga2 流程專屬對帳視圖 (利用 SQLite json_extract 抽出對帳欄位)
                    String createViewSql = "CREATE VIEW IF NOT EXISTS vw_saga_reconciliation AS " +
                            "SELECT " +
                            "  id, " +
                            "  created_at, " +
                            "  workflow_id, " +
                            "  business_key, " +
                            "  status, " +
                            "  message, " +
                            "  json_extract(input_data, '$.Account_A') AS account_a, " +
                            "  json_extract(input_data, '$.Account_B') AS account_b, " +
                            "  json_extract(input_data, '$.AMT')       AS amt, " +
                            "  json_extract(output_data, '$.a16229Result.code')    AS a16229_code, " +
                            "  json_extract(output_data, '$.a16229Result.message') AS a16229_msg, " +
                            "  json_extract(output_data, '$.a16220Result.code')    AS a16220_code, " +
                            "  json_extract(output_data, '$.a16220Result.message') AS a16220_msg, " +
                            "  json_extract(output_data, '$.a16220Rollback.code') AS a16220_rollback_code, " +
                            "  json_extract(output_data, '$.a16229Rollback.code') AS a16229_rollback_code " +
                            "FROM workflow_execution_log " +
                            "WHERE workflow_id = 'saga2-transfer-workflow';";
                    stmt.execute(createViewSql);

                    initialized.set(true);
                    LOG.info("[AuditLogService] SQLite 通用資料表 (WAL 模式) workflow_execution_log 及對帳視圖 vw_saga_reconciliation 已就緒。");
                }
            } catch (Exception e) {
                LOG.error("[AuditLogService] 初始化 SQLite 資料庫失敗: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 多參數進入點 (對應 SonataFlow 反射將 arguments key 展開為獨立參數)
     */
    public JsonNode saveLog(String workflowId, Object businessKey, Object inputData, Object outputData) {
        String bKeyStr = businessKey != null ? businessKey.toString() : null;
        JsonNode inputNode = MAPPER.valueToTree(inputData);
        JsonNode outputNode = MAPPER.valueToTree(outputData);
        return doSaveLog(workflowId, bKeyStr, inputNode, outputNode);
    }

    /**
     * 單一 JsonNode 參數進入點
     */
    public JsonNode saveLog(JsonNode arguments) {
        if (arguments == null) {
            return MAPPER.createObjectNode().put("saved", false);
        }
        String workflowId = arguments.has("workflowId") ? arguments.get("workflowId").asText() : "unknown-workflow";
        String businessKey = arguments.has("businessKey") ? arguments.get("businessKey").asText() : null;
        JsonNode inputDataNode = arguments.get("inputData");
        JsonNode outputDataNode = arguments.get("outputData");
        return doSaveLog(workflowId, businessKey, inputDataNode, outputDataNode);
    }

    /**
     * 單一 Map 參數進入點
     */
    public JsonNode saveLog(Map<String, Object> arguments) {
        if (arguments == null) {
            return MAPPER.createObjectNode().put("saved", false);
        }
        JsonNode node = MAPPER.valueToTree(arguments);
        return saveLog(node);
    }

    /**
     * 實際執行 DB 寫入邏輯
     */
    private JsonNode doSaveLog(String workflowId, String businessKey, JsonNode inputDataNode, JsonNode outputDataNode) {
        try {
            String inputDataJson = inputDataNode != null ? inputDataNode.toString() : "{}";
            String outputDataJson = outputDataNode != null ? outputDataNode.toString() : "{}";

            String status = "UNKNOWN";
            String message = "";
            if (outputDataNode != null) {
                if (outputDataNode.has("status")) {
                    status = outputDataNode.get("status").asText();
                }
                if (outputDataNode.has("message")) {
                    message = outputDataNode.get("message").asText();
                }
            }

            if (!initialized.get()) {
                initDatabase();
            }

            String insertSql = "INSERT INTO workflow_execution_log " +
                    "(workflow_id, business_key, status, message, input_data, output_data) " +
                    "VALUES (?, ?, ?, ?, ?, ?);";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, workflowId);
                pstmt.setString(2, businessKey);
                pstmt.setString(3, status);
                pstmt.setString(4, message);
                pstmt.setString(5, inputDataJson);
                pstmt.setString(6, outputDataJson);

                int rows = pstmt.executeUpdate();
                LOG.info("[AuditLogService] 成功寫入對帳紀錄! Workflow: {}, BusinessKey: {}, Status: {}, Rows: {}",
                        workflowId, businessKey, status, rows);
            }

            ObjectNode response = MAPPER.createObjectNode();
            response.put("saved", true);
            response.put("workflowId", workflowId);
            response.put("status", status);
            return response;

        } catch (Exception e) {
            LOG.error("[AuditLogService] 寫入對帳紀錄失敗: {}", e.getMessage(), e);
            ObjectNode response = MAPPER.createObjectNode();
            response.put("saved", false);
            response.put("error", e.getMessage());
            return response;
        }
    }
}
