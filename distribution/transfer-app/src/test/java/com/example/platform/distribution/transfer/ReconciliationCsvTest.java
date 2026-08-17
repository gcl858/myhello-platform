package com.example.platform.distribution.transfer;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Reconciliation CSV 匯出額外驗證測試
 *
 * 涵蓋四個方向:
 *   A. CSV 匯出健全性 (5 個): 檔名唯一、BOM、空資料處理、Content-Type、NULL 跳脫
 *   B. CSV vs DB 一致性 (3 個): rowCount 一致、欄位值抽樣比對、過濾一致
 *   C. Saga 業務邏輯一致性 (4 個): AMT>500 必 ROLLED_BACK、SUCCESS 無 rollback、FAILED 短路、A16220 失敗僅回沖 A16229
 *   D. 全鏈路整合 (2 個): 跑一輪 workflow → 匯出 → CSV 反映執行結果
 *
 * 設計: 用 @BeforeAll 自動跑 4 種 workflow 累積資料,確保測試獨立可執行
 *       不依賴 Saga2TransferWorkflowTest 的執行順序
 */
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Reconciliation CSV 匯出額外驗證")
class ReconciliationCsvTest {

    private static final String WORKFLOW_URL = "/saga2-transfer-workflow";
    private static final String EXPORT_URL = "/reconciliation/export-csv";
    private static final String DB_URL = "jdbc:sqlite:target/test-reconciliation.db";

    /**
     * 預先跑一輪 4 種情境 workflow,確保後續驗證有資料可用
     */
    @BeforeAll
    void seedWorkflowData() {
        int[] amts = {100, 30, 600, 2000, 500, 501};
        for (int amt : amts) {
            String payload = String.format(
                    "{\"Account_A\":\"A123\",\"Account_B\":\"B123\",\"AMT\":%d}", amt);
            given()
                .contentType("application/json")
                .body(payload)
            .when()
                .post(WORKFLOW_URL)
            .then()
                .statusCode(200);
        }
    }

    // ===================================================================
    //  A 類:CSV 匯出健全性
    // ===================================================================

    @Test
    @Order(1)
    @DisplayName("[A1] 連續匯出兩次檔名不重複 (秒級時間戳)")
    void testACsvFilenameUniqueness() {
        String name1 = exportField("{}", "fileName");
        sleepBriefly(1100); // 確保跨秒
        String name2 = exportField("{}", "fileName");
        assertNotEquals(name1, name2, "連續兩次匯出應產生不同檔名");
        assertTrue(name1.matches("^reconciliation-\\d{8}-\\d{6}\\.csv$"),
                "檔名應符合 reconciliation-YYYYMMDD-HHmmss.csv 格式");
        assertTrue(name2.matches("^reconciliation-\\d{8}-\\d{6}\\.csv$"));
    }

    @Test
    @Order(2)
    @DisplayName("[A2] 匯出無資料時 CSV 仍有 UTF-8 BOM + 表頭 (rowCount=0, byteSize>0)")
    void testAEmptyCsvStillHasHeader() throws Exception {
        String path = exportField("{\"workflowId\":\"non-existent-xyz\"}", "filePath");
        Path p = Paths.get(path);
        assertTrue(Files.exists(p), "CSV 檔案應存在: " + path);

        // 檢查 BOM (讀 byte)
        byte[] bytes = Files.readAllBytes(p);
        assertEquals((byte) 0xEF, bytes[0], "第 1 byte 應為 0xEF");
        assertEquals((byte) 0xBB, bytes[1], "第 2 byte 應為 0xBB");
        assertEquals((byte) 0xBF, bytes[2], "第 3 byte 應為 0xBF (UTF-8 BOM)");

        // 檢查表頭 (跳過 BOM 讀文字)
        String content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("id,created_at,workflow_id,business_key,status"),
                "CSV 表頭應包含所有欄位");

        long lineCount = content.lines().count();
        assertEquals(1L, lineCount, "無資料時 CSV 應只有 1 行 (表頭)");
    }

    @Test
    @Order(3)
    @DisplayName("[A3] NULL 欄位在 CSV 為空字串 (不是字面 \"null\")")
    void testANullFieldsAsEmptyString() throws Exception {
        String path = exportField("{\"status\":\"FAILED\"}", "filePath");
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        assertTrue(lines.size() >= 2, "FAILED 情境應至少有 1 筆資料");

        for (String line : lines) {
            if (line.contains("FAILED")) {
                String[] cols = parseCsvLine(line);
                assertEquals("999", cols[9], "a16229_code=999 (業務失敗)");
                assertEquals("", cols[11], "a16220_code 應為空 (短路未呼叫)");
                assertEquals("", cols[12], "a16220_msg 應為空");
                assertEquals("", cols[13], "a16229_rollback_code 應為空 (未回沖)");
                assertEquals("", cols[14], "a16220_rollback_code 應為空");
                // 確保沒有 "null" 字串
                assertFalse(line.contains(",null,"), "不應有 ',null,' 字串");
                assertFalse(line.contains(",null\r"), "不應有 ',null\\r' 字串");
                assertFalse(line.endsWith(",null"), "不應以 ',null' 結尾");
                return;
            }
        }
        fail("找不到 FAILED 紀錄");
    }

    @Test
    @Order(4)
    @DisplayName("[A4] 含逗號/中文的訊息欄位正確跳脫為雙引號包裹")
    void testACsvEscapeForCommas() throws Exception {
        String path = exportField("{\"status\":\"ROLLED_BACK\"}", "filePath");
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        // 跳過 BOM
        String content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        // A16220 失敗的訊息含逗號: "A16220 業務失敗,僅回沖 A16229"
        assertTrue(content.contains("\"A16220 業務失敗,僅回沖 A16229\""),
                "含逗號的 message 應用雙引號包裹,實際內容:\n" + content);
    }

    @Test
    @Order(5)
    @DisplayName("[A5] 匯出回傳 Content-Type 為 application/json")
    void testAContentType() {
        given()
            .contentType("application/json")
            .body("{}")
        .when()
            .post(EXPORT_URL)
        .then()
            .statusCode(200)
            .contentType("application/json")
            .body("ok", equalTo(true));
    }

    // ===================================================================
    //  B 類:CSV vs DB 一致性
    // ===================================================================

    @Test
    @Order(6)
    @DisplayName("[B1] CSV rowCount 應等於 DB COUNT(*) (不含表頭)")
    void testBCsvRowCountMatchesDbTotal() throws Exception {
        long dbCount = countDbRows(null, null, null);
        int csvRowCount = exportField("{}", "rowCount");
        assertEquals((int) dbCount, csvRowCount,
                "CSV rowCount (" + csvRowCount + ") 應等於 DB 紀錄數 (" + dbCount + ")");
    }

    @Test
    @Order(7)
    @DisplayName("[B2] status=ROLLED_BACK 過濾:CSV rowCount = DB 過濾筆數")
    void testBFilteredCsvMatchesDb() throws Exception {
        long dbRolledBack = countDbRows(null, null, "ROLLED_BACK");
        int csvRolledBack = exportField("{\"status\":\"ROLLED_BACK\"}", "rowCount");
        assertTrue(dbRolledBack > 0, "DB 應至少有 1 筆 ROLLED_BACK 紀錄");
        assertEquals((int) dbRolledBack, csvRolledBack,
                "ROLLED_BACK: CSV rowCount=" + csvRolledBack + " vs DB=" + dbRolledBack);

        long dbSuccess = countDbRows(null, null, "SUCCESS");
        int csvSuccess = exportField("{\"status\":\"SUCCESS\"}", "rowCount");
        assertEquals((int) dbSuccess, csvSuccess);

        long dbFailed = countDbRows(null, null, "FAILED");
        int csvFailed = exportField("{\"status\":\"FAILED\"}", "rowCount");
        assertEquals((int) dbFailed, csvFailed);
    }

    @Test
    @Order(8)
    @DisplayName("[B3] 抽樣比對: CSV 欄位值應與 DB 完全一致")
    void testBCsvContentMatchesDbFieldByField() throws Exception {
        String path = exportField("{}", "filePath");
        List<String[]> csvRows = parseCsv(Paths.get(path));
        assertTrue(csvRows.size() >= 6, "至少需 6 筆資料做抽樣比對,實際 " + csvRows.size());

        int expectedCompareCount = Math.min(10, csvRows.size());

        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT id, workflow_id, business_key, status, message, " +
                 "       account_a, account_b, amt, a16229_code, a16220_code, " +
                 "       a16229_rollback_code, a16220_rollback_code " +
                 "FROM vw_saga_reconciliation ORDER BY id LIMIT " + expectedCompareCount)) {

            int idx = 0;
            while (rs.next()) {
                String[] csv = csvRows.get(idx);
                assertEquals(String.valueOf(rs.getInt("id")), csv[0], "Row " + idx + " id");
                assertEquals(rs.getString("workflow_id"), csv[2], "Row " + idx + " workflow_id");
                assertEquals(rs.getString("business_key"), csv[3], "Row " + idx + " business_key");
                assertEquals(rs.getString("status"), csv[4], "Row " + idx + " status");
                // message (含逗號的可能有雙引號包裹,csv parser 已處理)
                assertEquals(rs.getString("message"), csv[5], "Row " + idx + " message");
                assertEquals(rs.getString("account_a"), csv[6], "Row " + idx + " account_a");
                assertEquals(rs.getString("account_b"), csv[7], "Row " + idx + " account_b");
                // amt 可能是 null (FAILED 短路)
                String dbAmt = rs.getObject("amt") == null ? "" : String.valueOf(rs.getInt("amt"));
                assertEquals(dbAmt, csv[8], "Row " + idx + " amt");
                String a16229 = rs.getString("a16229_code");
                assertEquals(a16229 == null ? "" : a16229, csv[9], "Row " + idx + " a16229_code");
                String a16220 = rs.getString("a16220_code");
                assertEquals(a16220 == null ? "" : a16220, csv[11], "Row " + idx + " a16220_code");
                idx++;
            }
            assertEquals(expectedCompareCount, idx, "應比對 " + expectedCompareCount + " 筆");
        }
    }

    // ===================================================================
    //  C 類:Saga 業務邏輯一致性
    // ===================================================================

    @Test
    @Order(9)
    @DisplayName("[C1] 500 < AMT < 1000 的紀錄必為 ROLLED_BACK 且雙 rollback code 都是 100 (LIFO 完整回沖)")
    void testCAmtGreaterThan500() throws Exception {
        int count = 0;
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT status, amt, a16229_rollback_code, a16220_rollback_code " +
                 "FROM vw_saga_reconciliation WHERE amt > 500 AND amt < 1000")) {
            while (rs.next()) {
                count++;
                int amt = rs.getInt("amt");
                assertEquals("ROLLED_BACK", rs.getString("status"),
                        "AMT=" + amt + " 應為 ROLLED_BACK");
                assertEquals("100", rs.getString("a16229_rollback_code"),
                        "AMT=" + amt + " 應有 a16229 rollback code");
                assertEquals("100", rs.getString("a16220_rollback_code"),
                        "AMT=" + amt + " 應有 a16220 rollback code (LIFO 完整回沖)");
            }
        }
        assertTrue(count > 0, "應至少有 1 筆 500<AMT<1000 紀錄 (預期至少 AMT=600)");
    }

    @Test
    @Order(10)
    @DisplayName("[C2] SUCCESS 紀錄: a16229_code + a16220_code 都是 100,無任何 rollback")
    void testCSuccessHasNoRollback() throws Exception {
        int count = 0;
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT a16229_code, a16220_code, a16229_rollback_code, a16220_rollback_code " +
                 "FROM vw_saga_reconciliation WHERE status = 'SUCCESS'")) {
            while (rs.next()) {
                count++;
                assertEquals("100", rs.getString("a16229_code"));
                assertEquals("100", rs.getString("a16220_code"));
                assertTrue(isNullOrEmpty(rs.getString("a16229_rollback_code")),
                        "SUCCESS 不該有 a16229 rollback");
                assertTrue(isNullOrEmpty(rs.getString("a16220_rollback_code")),
                        "SUCCESS 不該有 a16220 rollback");
            }
        }
        assertTrue(count > 0, "應至少有 1 筆 SUCCESS 紀錄");
    }

    @Test
    @Order(11)
    @DisplayName("[C3] FAILED 紀錄 (A16229 業務失敗短路): a16229_code=999,無 a16220,無 rollback")
    void testCFailedShortCircuit() throws Exception {
        int count = 0;
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT a16229_code, a16220_code, a16220_msg, " +
                 "       a16229_rollback_code, a16220_rollback_code " +
                 "FROM vw_saga_reconciliation WHERE status = 'FAILED'")) {
            while (rs.next()) {
                count++;
                assertEquals("999", rs.getString("a16229_code"));
                assertTrue(isNullOrEmpty(rs.getString("a16220_code")),
                        "FAILED 短路不該有 a16220_code");
                assertTrue(isNullOrEmpty(rs.getString("a16220_msg")),
                        "FAILED 短路不該有 a16220_msg");
                assertTrue(isNullOrEmpty(rs.getString("a16229_rollback_code")),
                        "FAILED 不該回沖");
                assertTrue(isNullOrEmpty(rs.getString("a16220_rollback_code")),
                        "FAILED 不該回沖");
            }
        }
        assertTrue(count > 0, "應至少有 1 筆 FAILED 紀錄");
    }

    @Test
    @Order(12)
    @DisplayName("[C4] A16220 失敗情境 (AMT>=1000): 僅回沖 A16229,不回沖 A16220")
    void testCA16220FailedOnlyRollbackA16229() throws Exception {
        int count = 0;
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT status, a16220_code, a16229_rollback_code, a16220_rollback_code " +
                 "FROM vw_saga_reconciliation WHERE amt >= 1000")) {
            while (rs.next()) {
                count++;
                assertEquals("ROLLED_BACK", rs.getString("status"));
                assertEquals("999", rs.getString("a16220_code"),
                        "AMT>=1000 時 A16220 應失敗");
                assertEquals("100", rs.getString("a16229_rollback_code"),
                        "應回沖 A16229");
                assertTrue(isNullOrEmpty(rs.getString("a16220_rollback_code")),
                        "A16220 從未成功扣款,不該回沖");
            }
        }
        assertTrue(count > 0, "應至少有 1 筆 AMT>=1000 紀錄");
    }

    // ===================================================================
    //  D 類:全鏈路整合
    // ===================================================================

    @Test
    @Order(13)
    @DisplayName("[D1] 全鏈路: 跑 4 種 workflow → 觸發匯出 → CSV 必含 4 種 status + DB 遞增 4 筆")
    void testDEndToEndAllFourStatuses() throws Exception {
        long beforeCount = countDbRows(null, null, null);

        // 用 unique Account_A 區隔這次測試的紀錄
        String uniqueSuffix = String.valueOf(System.currentTimeMillis() % 1000000);

        String[][] scenarios = {
            {uniqueSuffix + "_S", "100", "SUCCESS"},
            {uniqueSuffix + "_F", "30", "FAILED"},
            {uniqueSuffix + "_R", "600", "ROLLED_BACK"},
            {uniqueSuffix + "_P", "2000", "ROLLED_BACK"} // Partial rollback
        };
        for (String[] s : scenarios) {
            String payload = String.format(
                "{\"Account_A\":\"%s\",\"Account_B\":\"X\",\"AMT\":%s}", s[0], s[1]);
            String status = given()
                .contentType("application/json")
                .body(payload)
            .when()
                .post(WORKFLOW_URL)
            .then()
                .statusCode(200)
                .extract().path("status");
            assertEquals(s[2], status,
                    s[0] + " AMT=" + s[1] + " 預期 " + s[2] + ",實際 " + status);
        }

        // DB 應遞增 4 筆
        long afterCount = countDbRows(null, null, null);
        assertEquals(beforeCount + 4, afterCount, "DB 應新增 4 筆");

        // 匯出全部 CSV,驗證 4 種 status 都出現
        String path = exportField("{}", "filePath");
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        String content = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        assertTrue(content.contains("," + uniqueSuffix + "_S,"), "CSV 應含 SUCCESS 紀錄");
        assertTrue(content.contains("," + uniqueSuffix + "_F,"), "CSV 應含 FAILED 紀錄");
        assertTrue(content.contains("," + uniqueSuffix + "_R,"), "CSV 應含 LIFO 回沖紀錄");
        assertTrue(content.contains("," + uniqueSuffix + "_P,"), "CSV 應含部分回沖紀錄");
    }

    @Test
    @Order(14)
    @DisplayName("[D2] 連續匯出兩次 (無新增紀錄) rowCount 應一致")
    void testDConsistentExportResults() {
        int count1 = exportField("{}", "rowCount");
        int count2 = exportField("{}", "rowCount");
        assertEquals(count1, count2, "無新增紀錄時,兩次匯出 rowCount 應相同 (count1=" + count1 + ", count2=" + count2 + ")");

        // 同樣的過濾條件也應一致
        int r1 = exportField("{\"status\":\"SUCCESS\"}", "rowCount");
        int r2 = exportField("{\"status\":\"SUCCESS\"}", "rowCount");
        assertEquals(r1, r2, "SUCCESS 過濾 rowCount 兩次應一致");
    }

    // ===================================================================
    //  工具方法
    // ===================================================================

    private void sleepBriefly(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException e) { /* ignore */ }
    }

    private <T> T exportField(String body, String field) {
        return given()
            .contentType("application/json")
            .body(body)
        .when()
            .post(EXPORT_URL)
        .then()
            .statusCode(200)
            .body("ok", equalTo(true))
            .extract().path(field);
    }

    private long countDbRows(String workflowId, String businessKey, String status) throws Exception {
        Class.forName("org.sqlite.JDBC");
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM vw_saga_reconciliation WHERE 1=1");
        if (workflowId != null) sql.append(" AND workflow_id = ?");
        if (businessKey != null) sql.append(" AND business_key = ?");
        if (status != null) sql.append(" AND status = ?");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (workflowId != null) pstmt.setString(idx++, workflowId);
            if (businessKey != null) pstmt.setString(idx++, businessKey);
            if (status != null) pstmt.setString(idx++, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    /** 解析整個 CSV 檔,跳過表頭 */
    private List<String[]> parseCsv(Path path) throws Exception {
        List<String[]> rows = new ArrayList<>();
        byte[] bytes = Files.readAllBytes(path);
        // 跳過 BOM
        int start = (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) ? 3 : 0;
        String content = new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; } // 跳過表頭
                if (line.isEmpty()) continue;
                rows.add(parseCsvLine(line));
            }
        }
        return rows;
    }

    /** 處理雙引號跳脫的 CSV 行解析 */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == ',') {
                    fields.add(cur.toString());
                    cur.setLength(0);
                } else if (c == '"' && cur.length() == 0) {
                    inQuotes = true;
                } else {
                    cur.append(c);
                }
            }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
