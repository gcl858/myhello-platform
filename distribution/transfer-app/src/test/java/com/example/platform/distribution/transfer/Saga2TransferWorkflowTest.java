package com.example.platform.distribution.transfer;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * saga2-transfer-workflow 完整測試
 * 對應 saga-transfer-workflow v14 測試集,驗證 4 種情境:
 *   - AMT=100 → SUCCESS
 *   - AMT=600 → ROLLED_BACK (LIFO 完整回沖)
 *   - AMT=30 → FAILED (A16229 業務失敗短路)
 *   - AMT=2000 → ROLLED_BACK (A16220 失敗僅回沖 A16229)
 *
 * 本測試透過 SonataFlow HTTP 端點 ({@code POST /saga2-transfer-workflow})
 * 啟動 workflow,並用 rest-assured 驗證輸出 JSON。
 */
@QuarkusTest
@DisplayName("saga2-transfer-workflow saga 測試")
class Saga2TransferWorkflowTest {

    private static final String WORKFLOW_URL = "/saga2-transfer-workflow";

    private static final String PAYLOAD_TEMPLATE =
            "{\"Account_A\":\"A123\",\"Account_B\":\"B123\",\"AMT\":%d}";

    @Test
    @DisplayName("[1] AMT=100 → SUCCESS (兩 API 都成功)")
    void testAmt100Success() {
        given()
            .contentType("application/json")
            .body(String.format(PAYLOAD_TEMPLATE, 100))
        .when()
            .post(WORKFLOW_URL)
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .body("message", equalTo("交易成功"))
            .body("AMT", equalTo(100))
            .body("a16229Result.code", equalTo("100"))
            .body("a16229Result._httpStatus", equalTo(200))
            .body("a16220Result.code", equalTo("100"))
            .body("a16220Result._httpStatus", equalTo(200));
    }

    @Test
    @DisplayName("[2] AMT=600 → ROLLED_BACK LIFO (兩 API + 兩 rollback)")
    void testAmt600RolledBackLifo() {
        given()
            .contentType("application/json")
            .body(String.format(PAYLOAD_TEMPLATE, 600))
        .when()
            .post(WORKFLOW_URL)
        .then()
            .statusCode(200)
            .body("status", equalTo("ROLLED_BACK"))
            .body("message", equalTo("AMT > 500 觸發 saga 回沖 (LIFO)"))
            .body("AMT", equalTo(600))
            .body("a16229Result.code", equalTo("100"))
            .body("a16220Result.code", equalTo("100"))
            // LIFO: 兩個 rollback 都用 EC=true
            .body("a16229Rollback.code", equalTo("100"))
            .body("a16229Rollback.message", equalTo("成功(EC)"))
            .body("a16220Rollback.code", equalTo("100"))
            .body("a16220Rollback.message", equalTo("成功(EC)"));
    }

    @Test
    @DisplayName("[3] AMT=30 → FAILED (A16229 業務失敗短路,不呼叫 A16220)")
    void testAmt30Failed() {
        given()
            .contentType("application/json")
            .body(String.format(PAYLOAD_TEMPLATE, 30))
        .when()
            .post(WORKFLOW_URL)
        .then()
            .statusCode(200)
            .body("status", equalTo("FAILED"))
            .body("AMT", equalTo(30))
            .body("a16229Result.code", equalTo("999"))
            .body("a16229Result._httpStatus", equalTo(403))
            // 短路:A16220 與 rollback 都不該被呼叫
            .body("a16220Result", nullValue())
            .body("a16229Rollback", nullValue())
            .body("a16220Rollback", nullValue());
    }

    @Test
    @DisplayName("[4] AMT=2000 → ROLLED_BACK (A16220 失敗,僅回沖 A16229)")
    void testAmt2000RolledBackA16229Only() {
        given()
            .contentType("application/json")
            .body(String.format(PAYLOAD_TEMPLATE, 2000))
        .when()
            .post(WORKFLOW_URL)
        .then()
            .statusCode(200)
            .body("status", equalTo("ROLLED_BACK"))
            .body("message", equalTo("A16220 業務失敗,僅回沖 A16229"))
            .body("AMT", equalTo(2000))
            .body("a16229Result.code", equalTo("100"))
            .body("a16220Result.code", equalTo("999"))
            .body("a16220Result._httpStatus", equalTo(403))
            // 只回沖 A16229 (A16220 本來就沒扣款)
            .body("a16229Rollback.code", equalTo("100"))
            .body("a16229Rollback.message", equalTo("成功(EC)"))
            .body("a16220Rollback", nullValue());
    }

    @Test
    @DisplayName("[5] AMT=500 邊界值 → SUCCESS (AMT <= 500 才算成功)")
    void testAmt500BoundarySuccess() {
        given()
            .contentType("application/json")
            .body(String.format(PAYLOAD_TEMPLATE, 500))
        .when()
            .post(WORKFLOW_URL)
        .then()
            .statusCode(200)
            .body("status", equalTo("SUCCESS"))
            .body("AMT", equalTo(500));
    }

    @Test
    @DisplayName("[6] AMT=501 邊界值 → ROLLED_BACK (AMT > 500 觸發 LIFO)")
    void testAmt501BoundaryRolledBack() {
        given()
            .contentType("application/json")
            .body(String.format(PAYLOAD_TEMPLATE, 501))
        .when()
            .post(WORKFLOW_URL)
        .then()
            .statusCode(200)
            .body("status", equalTo("ROLLED_BACK"))
            .body("AMT", equalTo(501));
    }

    @Test
    @DisplayName("[7] 驗證 SQLite 通用資料表與對帳 View (vw_saga_reconciliation) 紀錄內容")
    void testSqliteDatabaseRecords() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (java.sql.Connection conn = java.sql.DriverManager.getConnection("jdbc:sqlite:target/test-reconciliation.db");
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT id, workflow_id, business_key, status, account_a, account_b, amt, a16229_code, a16220_code FROM vw_saga_reconciliation")) {
            int count = 0;
            while (rs.next()) {
                count++;
                System.out.printf("[Reconciliation Test] Row %d -> ID: %d, WF: %s, BKey: %s, Status: %s, AccountA: %s, AccountB: %s, AMT: %d, A16229Code: %s, A16220Code: %s%n",
                        count, rs.getInt("id"), rs.getString("workflow_id"), rs.getString("business_key"),
                        rs.getString("status"), rs.getString("account_a"), rs.getString("account_b"),
                        rs.getInt("amt"), rs.getString("a16229_code"), rs.getString("a16220_code"));
            }
            org.junit.jupiter.api.Assertions.assertTrue(count > 0, "SQLite View 應包含寫入的對帳紀錄");
        }
    }

    @Test
    @DisplayName("[8] POST /reconciliation/export-csv (無過濾) — 匯出全部對帳紀錄到 data/ 目錄")
    void testExportCsvAllRecords() {
        String csvFilePath = given()
                .contentType("application/json")
                .body("{}")
            .when()
                .post("/reconciliation/export-csv")
            .then()
                .statusCode(200)
                .body("ok", equalTo(true))
                .body("fileName", notNullValue())
                .body("fileName", matchesPattern("^reconciliation-\\d{8}-\\d{6}\\.csv$"))
                .body("rowCount", greaterThan(0))
                .body("byteSize", greaterThan(0))
                .body("filePath", matchesPattern(".*/data/reconciliation-\\d{8}-\\d{6}\\.csv$"))
                .extract().path("filePath");

        // 驗證檔案實際存在於磁碟
        Path path = Paths.get(csvFilePath);
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(path),
                "CSV 檔案應實際寫入磁碟: " + csvFilePath);

        // 驗證內容:含 UTF-8 BOM + 表頭 + 至少 1 行資料
        try {
            String content = Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
            org.junit.jupiter.api.Assertions.assertTrue(content.startsWith("\uFEFF"),
                    "CSV 開頭應為 UTF-8 BOM (避免 Excel 中文亂碼)");
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("id,created_at,workflow_id,business_key,status"),
                    "CSV 應含標準表頭");
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("saga2-transfer-workflow"),
                    "CSV 應含 workflow_id 欄位內容");
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("SUCCESS"),
                    "CSV 應含至少一筆 SUCCESS 紀錄");
            System.out.printf("[CSV Export Test] File: %s, Lines: %d%n",
                    path.getFileName(), content.lines().count());
        } catch (Exception e) {
            org.junit.jupiter.api.Assertions.fail("讀取 CSV 失敗: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("[9] POST /reconciliation/export-csv (status=ROLLED_BACK) — 過濾匯出")
    void testExportCsvFilteredByStatus() {
        given()
            .contentType("application/json")
            .body("{\"status\":\"ROLLED_BACK\"}")
        .when()
            .post("/reconciliation/export-csv")
        .then()
            .statusCode(200)
            .body("ok", equalTo(true))
            .body("rowCount", greaterThan(0))
            .body("filters.status", equalTo("ROLLED_BACK"));
    }

    @Test
    @DisplayName("[10] POST /reconciliation/export-csv (workflowId=non-existent) — 過濾無資料")
    void testExportCsvNoMatchingRecords() {
        given()
            .contentType("application/json")
            .body("{\"workflowId\":\"non-existent-workflow-xyz\"}")
        .when()
            .post("/reconciliation/export-csv")
        .then()
            .statusCode(200)
            .body("ok", equalTo(true))
            .body("rowCount", equalTo(0));
    }
}