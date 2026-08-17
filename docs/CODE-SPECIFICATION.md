# myhello-platform Java 程式碼詳細說明文件

> **專案名稱**：`myhello-platform`  
> **Java 版本**：Java 21 (LTS)  
> **框架環境**：Quarkus 3.27.2 + Apache Kogito / SonataFlow 10.2.0  
> **文件範圍**：全專案所有正式 Java 原始程式碼（共 19 隻，排除測試程式與自動生成程式碼）

---

## 目錄 (Table of Contents)

- [1. 程式碼分佈與全景統計](#1-程式碼分佈與全景統計)
- [2. 平台安全與過濾層 (platform-security)](#2-平台安全與過濾層-platform-security)
  - [2.1 SonataFlowSecurityConfig.java](#21-sonataflowsecurityconfigjava)
  - [2.2 UnwrapResponseFilter.java](#22-unwrapresponsefilterjava)
  - [2.3 OpenApiFilter.java](#23-openapifilterjava)
- [3. 平台資料索引與持久化層 (platform-data-index)](#3-平台資料索引與持久化層-platform-data-index)
  - [3.1 KogitoSQLiteDialect.java](#31-kogitosqlitedialectjava)
  - [3.2 SqliteZonedDateTimeJdbcType.java](#32-sqlitezoneddatetimejdbctypejava)
  - [3.3 MyLivenessCheck.java](#33-mylivenesscheckjava)
- [4. 平台共用工具層 (platform-common)](#4-平台共用工具層-platform-common)
  - [4.1 package-info.java](#41-package-infojava)
- [5. 轉帳 API 合約與 Mock 服務 (domain-transfer / transfer-api)](#5-轉帳-api-合約與-mock-服務-domain-transfer--transfer-api)
  - [5.1 A16229Resource.java](#51-a16229resourcejava)
  - [5.2 A16220Resource.java](#52-a16220resourcejava)
  - [5.3 A16229Request.java & A16229Response.java](#53-a16229requestjava--a16229responsejava)
  - [5.4 A16220Request.java & A16220Response.java](#54-a16220requestjava--a16220responsejava)
- [6. 轉帳流程與服務調用層 (domain-transfer / transfer-workflow)](#6-轉帳流程與服務調用層-domain-transfer--transfer-workflow)
  - [6.1 SagaApiService.java](#61-sagaapiservicejava)
  - [6.2 MyOpenApiService.java](#62-myopenapiservicejava)
- [7. 範例流程服務層 (domain-transfer / transfer-samples)](#7-範例流程服務層-domain-transfer--transfer-samples)
  - [7.1 ApiService.java](#71-apiservicejava)
- [8. 對帳審計與報表匯出層 (domain-reconciliation / reconciliation-api)](#8-對帳審計與報表匯出層-domain-reconciliation--reconciliation-api)
  - [8.1 AuditLogService.java](#81-auditlogservicejava)
  - [8.2 ReconciliationResource.java](#82-reconciliationresourcejava)
  - [8.3 ReconciliationCsvExporter.java](#83-reconciliationcsvexporterjava)
- [9. 分發應用層代碼生成機制 (distribution/*)](#9-分發應用層代碼生成機制-distribution)

---

## 1. 程式碼分佈與全景統計

| 模組名稱 | 檔案路徑 | 主要職責 / 類型 |
| :--- | :--- | :--- |
| **`platform-security`** | `.../security/SonataFlowSecurityConfig.java` | HTTP 方法限制、審計日誌與狀態改寫過濾器 |
| **`platform-security`** | `.../security/UnwrapResponseFilter.java` | SonataFlow 工作流輸出解包過濾器 |
| **`platform-security`** | `.../security/OpenApiFilter.java` | OpenAPI 規格動態過濾器 (排除非 POST) |
| **`platform-data-index`** | `.../dataindex/KogitoSQLiteDialect.java` | 自訂 Hibernate 6 SQLite 方言 |
| **`platform-data-index`** | `.../dataindex/SqliteZonedDateTimeJdbcType.java` | SQLite ZonedDateTime JDBC 型別轉換處理器 |
| **`platform-data-index`** | `.../dataindex/MyLivenessCheck.java` | SmallRye Health 存活檢查探針 |
| **`platform-common`** | `.../common/package-info.java` | 套件宣告與 Javadoc |
| **`transfer-api`** | `.../transfer/api/A16229Resource.java` | A16229 (扣款/轉出) JAX-RS 服務端點 |
| **`transfer-api`** | `.../transfer/api/A16220Resource.java` | A16220 (入帳/轉入) JAX-RS 服務端點 |
| **`transfer-api`** | `.../transfer/api/dto/A16229Request.java` | A16229 請求 DTO |
| **`transfer-api`** | `.../transfer/api/dto/A16229Response.java` | A16229 回應 DTO |
| **`transfer-api`** | `.../transfer/api/dto/A16220Request.java` | A16220 請求 DTO |
| **`transfer-api`** | `.../transfer/api/dto/A16220Response.java` | A16220 回應 DTO |
| **`transfer-workflow`** | `.../transfer/workflow/SagaApiService.java` | 非同步 HTTP Client 封裝 (提供 Saga 呼叫) |
| **`transfer-workflow`** | `.../transfer/workflow/MyOpenApiService.java` | 通用 OpenAPI 動態規格解析與呼叫引擎 (含快取) |
| **`transfer-samples`** | `.../transfer/samples/ApiService.java` | Java 工作流外部呼叫與健康檢查示範服務 |
| **`reconciliation-api`** | `.../reconciliation/AuditLogService.java` | SQLite WAL 審計日誌非阻塞儲存服務 |
| **`reconciliation-api`** | `.../reconciliation/ReconciliationResource.java` | 對帳單 CSV 匯出 JAX-RS REST 端點 |
| **`reconciliation-api`** | `.../reconciliation/ReconciliationCsvExporter.java` | 高效能 UTF-8 BOM 對帳 CSV 串流匯出引擎 |

---

## 2. 平台安全與過濾層 (platform-security)

### 2.1 `SonataFlowSecurityConfig.java`
- **檔案路徑**：`platform-security/src/main/java/com/example/platform/security/SonataFlowSecurityConfig.java`
- **套件名稱**：`com.example.platform.security`
- **設計定位**：整合微服務安全策略、HTTP 請求約束、全鏈路審計日誌與業務狀態碼改寫。
- **內部元件結構**：
  1. **`SwaggerFilter`** (實作 `OASFilter`)：
     - **職責**：在產生 OpenAPI / Swagger UI 規格時，主動移除除 POST 以外的所有 HTTP Method（如 GET, PUT, DELETE, PATCH, HEAD），符合金融 API 單一 POST 進入點規範。
  2. **`RuntimeFilter`** (實作 `ContainerRequestFilter`)：
     - **標註**：`@Provider`, `@ApplicationScoped`, `@Priority(Priorities.USER)`
     - **核心邏輯**：
       - 自動放行系統監控路徑（`path.startsWith("q/")`，如 `/q/health`, `/q/swagger-ui`）。
       - 嚴格攔截非 `POST` 請求，直接以 `403 Forbidden`（`{"error": "Only POST method is allowed"}`）中斷請求。
       - 讀取 Request Entity Stream 轉換為字串緩存至 `requestContext.setProperty("rawInputPayload", ...)`，並使用 `ByteArrayInputStream` 重設 Stream 供後續反序列化使用。
       - 記錄請求進來之奈秒時間戳記 `startTimeNano` 供耗時計算。
  3. **`WorkflowStatusFilter`** (實作 `ContainerResponseFilter`)：
     - **標註**：`@Provider`, `@ApplicationScoped`, `@Priority(Priorities.USER + 10)`
     - **核心邏輯**：
       - 僅處理 POST 方法傳出的工作流回應。
       - **狀態碼精準改寫**：若回應為 `saga2-transfer-workflow`，檢查 JSON 內容中的 `status` 欄位：
         - `VALIDATION_FAILED` $\rightarrow$ 改寫為 `HTTP 400 Bad Request`
         - `ROLLBACK_FAILED` $\rightarrow$ 改寫為 `HTTP 409 Conflict`
         - `SUCCESS` / `FAILED` / `ROLLED_BACK` $\rightarrow$ 保持 `HTTP 200 OK`
       - **統一 HTTP 審計輸出**：收集 URI、HTTP Code、耗時 (ms)、原始 Input 與 Output Payload，以標準格式輸出日誌：
         ```log
         [HTTP-AUDIT] URI: ... | HTTP: ... | Cost: ...ms | INPUT: ... | OUTPUT: ...
         ```

---

### 2.2 `UnwrapResponseFilter.java`
- **檔案路徑**：`platform-security/src/main/java/com/example/platform/security/UnwrapResponseFilter.java`
- **套件名稱**：`com.example.platform.security`
- **設計定位**：JAX-RS 回應解包過濾器，隱藏 Kogito 流程實例元數據。
- **標註**：`@Provider`, `@ApplicationScoped`, `@Priority(Priorities.USER)`
- **核心邏輯**：
  - Kogito 原生執行完畢時，產生的 JSON 為 `{"id": "uuid", "workflowdata": { ... }}`。
  - 本過濾器在 HTTP 回應寫入 Socket 前攔截 Response Entity：
    - 若 Entity 為 `Map` 且包含 `workflowdata`，直接將 Entity 替換為該子物件，並設定 HTTP 200。
    - 若 Entity 為 Jackson `JsonNode` 且包含 `workflowdata`，提取該節點並設定 HTTP 200。
    - 對其他 POJO 物件，透過 `@Inject ObjectMapper` 轉為樹狀結構進行判斷與解包。

---

### 2.3 `OpenApiFilter.java`
- **檔案路徑**：`platform-security/src/main/java/com/example/platform/security/OpenApiFilter.java`
- **套件名稱**：`com.example.platform.security`
- **設計定位**：標準 MicroProfile OpenAPI 攔截器。
- **核心方法**：
  - `filterPathItem(PathItem pathItem)`：將 `pathItem` 中的 `setGET(null)`, `setDELETE(null)`, `setPUT(null)`, `setPATCH(null)`, `setHEAD(null)` 清空，使 Swagger UI 僅呈現 POST 端點。

---

## 3. 平台資料索引與持久化層 (platform-data-index)

### 3.1 `KogitoSQLiteDialect.java`
- **檔案路徑**：`platform-data-index/src/main/java/org/acme/platform/dataindex/KogitoSQLiteDialect.java`
- **套件名稱**：`org.acme.platform.dataindex`
- **繼承**：`org.hibernate.community.dialect.SQLiteDialect`
- **設計定位**：解決 Hibernate ORM 6+ 搭配 SQLite 儲存 `ZonedDateTime` 欄位時的型別不相容問題。
- **核心方法**：
  - `contributeTypes(TypeContributions, ServiceRegistry)`：
    - 註冊 `SqliteZonedDateTimeJdbcType.INSTANCE` 與 `SqliteZonedDateTimeJavaType.INSTANCE`。
    - 註冊自訂 BasicType 映射，支援別名 `"zdt"`, `"ZonedDateTime"`, 解決 GraphQL `ProcessInstances` 查詢時間戳記解析失敗的缺陷。

---

### 3.2 `SqliteZonedDateTimeJdbcType.java`
- **檔案路徑**：`platform-data-index/src/main/java/org/acme/platform/dataindex/SqliteZonedDateTimeJdbcType.java`
- **套件名稱**：`org.acme.platform.dataindex`
- **實作介面**：`org.hibernate.type.descriptor.jdbc.JdbcType`
- **設計定位**：自訂 JDBC 型別描述器，處理 SQLite TEXT 欄位與 Java `ZonedDateTime` 之間的雙向轉換。
- **核心邏輯**：
  - **寫入 (Binder)**：將 `ZonedDateTime` 轉為 Unix Epoch 毫秒字串（如 `"1786185258763"`）存入 SQLite TEXT 欄位。
  - **讀取 (Extractor)**：支援雙模式相容解析：
    1. 優先嘗試將字串解析為 `Long`（毫秒），以系統預設時區組裝為 `ZonedDateTime`。
    2. 若失敗，則嘗試以標準 ISO-8601 格式（`ZonedDateTime.parse(str)`）進行解析，防止 `DateTimeParseException` 拋出。

---

### 3.3 `MyLivenessCheck.java`
- **檔案路徑**：`platform-data-index/src/main/java/org/acme/platform/dataindex/MyLivenessCheck.java`
- **套件名稱**：`org.acme.platform.dataindex`
- **實作介面**：`org.eclipse.microprofile.health.HealthCheck`
- **標註**：`@Liveness`
- **核心方法**：
  - `call()`：回傳 `HealthCheckResponse.up("alive")`，供 Kubernetes Liveness Probe 探測 Pod 存活狀態。

---

## 4. 平台共用工具層 (platform-common)

### 4.1 `package-info.java`
- **檔案路徑**：`platform-common/src/main/java/com/example/platform/common/package-info.java`
- **設計定位**：作為無框架依賴之 Leaf 模組宣告與共用規範指引。

---

## 5. 轉帳 API 合約與 Mock 服務 (domain-transfer / transfer-api)

### 5.1 `A16229Resource.java`
- **檔案路徑**：`domain-transfer/transfer-api/src/main/java/org/acme/transfer/api/A16229Resource.java`
- **套件名稱**：`org.acme.transfer.api`
- **標註**：`@Path("/A16229")`, `@Consumes(MediaType.APPLICATION_JSON)`, `@Produces(MediaType.APPLICATION_JSON)`
- **設計定位**：銀行轉帳流程第一階段——**A 帳戶扣款/轉出服務**（Mock 實作）。
- **核心方法**：
  - `POST execute(A16229Request request)`：
    - **業務檢核**：
      - 若 `request == null` $\rightarrow$ 回傳 HTTP 403 (`code: "999"`, `message: "Request不可為空"`).
      - 若 `request.AMT > 50` $\rightarrow$ 扣款成功，回傳 HTTP 200 (`code: "100"`)。若 `request.EC == true`，則訊息顯示為 `"成功(EC)"`。
      - 若 `request.AMT <= 50` $\rightarrow$ 業務拒絕，回傳 HTTP 403 (`code: "999"`, `message: "AMT必須大於50"`).

---

### 5.2 `A16220Resource.java`
- **檔案路徑**：`domain-transfer/transfer-api/src/main/java/org/acme/transfer/api/A16220Resource.java`
- **套件名稱**：`org.acme.transfer.api`
- **標註**：`@Path("/A16220")`, `@Consumes(MediaType.APPLICATION_JSON)`, `@Produces(MediaType.APPLICATION_JSON)`
- **設計定位**：銀行轉帳流程第二階段——**B 帳戶入帳/轉入服務**（Mock 實作）。
- **核心方法**：
  - `POST execute(A16220Request request)`：
    - **業務檢核**：
      - 若 `request == null` $\rightarrow$ 回傳 HTTP 403 (`code: "999"`, `message: "Request不可為空"`).
      - 若 `request.AMT < 1000` $\rightarrow$ 入帳成功，回傳 HTTP 200 (`code: "100"`)。若 `request.EC == true`，則訊息顯示為 `"成功(EC)"`。
      - 若 `request.AMT >= 1000` $\rightarrow$ 業務拒絕，回傳 HTTP 403 (`code: "999"`, `message: "AMT必須小於1000"`).

---

### 5.3 `A16229Request.java` & `A16229Response.java`
- **檔案路徑**：
  - `domain-transfer/transfer-api/src/main/java/org/acme/transfer/api/dto/A16229Request.java`
  - `domain-transfer/transfer-api/src/main/java/org/acme/transfer/api/dto/A16229Response.java`
- **DTO 欄位定義**：
  - `A16229Request`：`String Account_A`, `Integer AMT`, `Boolean EC`
  - `A16229Response`：`String code`, `String message`, `String Account`

---

### 5.4 `A16220Request.java` & `A16220Response.java`
- **檔案路徑**：
  - `domain-transfer/transfer-api/src/main/java/org/acme/transfer/api/dto/A16220Request.java`
  - `domain-transfer/transfer-api/src/main/java/org/acme/transfer/api/dto/A16220Response.java`
- **DTO 欄位定義**：
  - `A16220Request`：`String Account_B`, `Integer AMT`, `Boolean EC`
  - `A16220Response`：`String code`, `String message`, `String Account`

---

## 6. 轉帳流程與服務調用層 (domain-transfer / transfer-workflow)

### 6.1 `SagaApiService.java`
- **檔案路徑**：`domain-transfer/transfer-workflow/src/main/java/com/example/transfer/workflow/SagaApiService.java`
- **套件名稱**：`com.example.transfer.workflow`
- **標註**：`@ApplicationScoped`
- **設計定位**：提供給 SonataFlow 工作流程（`type: custom`）調用銀行 API 的底層非同步 HTTP 客戶端封裝。
- **成員變數**：
  - `HttpClient httpClient`：JDK 11+ 原生連線池客戶端，逾時設定為 10 秒。
  - `ObjectMapper objectMapper`：JSON 序列化與節點操作。
- **核心方法**：
  1. `JsonNode callA16229(String Account_A, Integer AMT, Boolean EC)`：組裝 JSON Payload 並發送至 `/A16229`。
  2. `JsonNode callA16220(String Account_B, Integer AMT, Boolean EC)`：組裝 JSON Payload 並發送至 `/A16220`。
  3. `CompletableFuture<JsonNode> callApiAsync(String path, JsonNode body)`：
     - 使用 `httpClient.sendAsync()` 發送非同步 POST 請求。
     - 自動在回應 JSON 附帶 `_httpStatus` 欄位（例如 200, 403, 500），供工作流程 switch condition 判斷成功或失敗。
     - 透過 `.exceptionally()` 攔截網路連線逾時或斷線異常，回傳 `code: "999"` 與 `_httpStatus: 0`，避免流程因拋出未捕獲例外而死鎖。

---

### 6.2 `MyOpenApiService.java`
- **檔案路徑**：`domain-transfer/transfer-workflow/src/main/java/com/example/transfer/workflow/MyOpenApiService.java`
- **套件名稱**：`com.example.transfer.workflow`
- **標註**：`@ApplicationScoped`
- **設計定位**：通用型 OpenAPI 動態調用引擎。讓工作流能僅透過傳入 OpenAPI 規格路徑與 Operation ID（如 `specs/A16229-api.yaml#executeA16229`），動態解析端點、路徑、方法並發送非同步 HTTP 請求。
- **高並發快取設計**：
  - `operationCache` (`ConcurrentHashMap<String, ApiEndpoint>`)：快取 `specPath#operationId` 之 URL 路徑與 HTTP Method。
  - `baseUrlCache` (`ConcurrentHashMap<String, String>`)：快取 Base URL 解析結果。
  - `specAstCache` (`ConcurrentHashMap<String, JsonNode>`)：快取 OpenAPI YAML 的語法樹（AST），避免重複磁碟 I/O。
  - `requestFieldsCache` (`ConcurrentHashMap<String, List<String>>`)：快取 Request Body 欄位順序與結構。
- **核心方法**：
  1. `callOpenApi(JsonNode arguments)`：主進入點，解析 `schemaRef`、提取 Payload、發送非同步 HTTP 請求並回傳結果。
  2. `callOpenApi(...)` 多載方法：提供 Map、JsonNode 與多參數簽章，相容 Kogito 編譯期反射生成的多種 WorkItemHandler 呼叫約定。
  3. `resolveBaseUrl(...)`：依序自 `application.properties` 配置中檢索 Base URL（依據 `kogito.sw.functions.<name>.base_url`、`.host`、`.port` 或 OpenAPI spec 中的 `servers[0].url`）。

---

## 7. 範例流程服務層 (domain-transfer / transfer-samples)

### 7.1 `ApiService.java`
- **檔案路徑**：`domain-transfer/transfer-samples/src/main/java/com/example/platform/transfer/samples/ApiService.java`
- **套件名稱**：`com.example.platform.transfer.samples`
- **標註**：`@ApplicationScoped`
- **設計定位**：展示 Java CDI 服務與 SonataFlow 整合之示範程式碼。
- **核心方法**：
  1. `JsonNode callExternalApi(String idno, String spcd, String brno)`：
     - 展示如何在 Java 服務中建立支援 TrustAllCerts 的 SSLContext 呼叫外部 API Gateway。
  2. `JsonNode callHealthApi(JsonNode parameters)`：
     - 展示由 `java-workflow.sw.yaml` 呼叫本機健康檢查 API（`GET http://localhost:8080/q/health`）並傳回 JsonNode。

---

## 8. 對帳審計與報表匯出層 (domain-reconciliation / reconciliation-api)

### 8.1 `AuditLogService.java`
- **檔案路徑**：`domain-reconciliation/reconciliation-api/src/main/java/com/example/reconciliation/AuditLogService.java`
- **套件名稱**：`com.example.reconciliation`
- **標註**：`@ApplicationScoped`
- **設計定位**：通用工作流程審計日誌收集與 SQLite WAL 儲存服務。
- **成員變數與注入**：
  - `@Inject DataSource dataSource`：Quarkus Agroal 資料庫連線池。
  - `AtomicBoolean initialized`：確保資料表結構與 PRAGMA 配置只執行一次。
- **核心方法**：
  1. `onStart(@Observes StartupEvent ev)`：在應用程式啟動時呼叫 `initDatabase()`。
  2. `initDatabase()`：
     - 自動建立 `data/` 目錄。
     - 執行 `PRAGMA journal_mode=WAL;`、`PRAGMA synchronous=NORMAL;`、`PRAGMA busy_timeout=5000;` 啟用高並發寫入。
     - 建立日誌表 `workflow_execution_log`。
     - 建立對帳展平視圖 `vw_saga_reconciliation`（使用 SQLite `json_extract()`）。
  3. `saveLog(...)` 多載方法：接收工作流傳入之 `workflowId`, `businessKey`, `inputData`, `outputData`，執行 SQL INSERT 並回傳 `{"saved": true}`。

---

### 8.2 `ReconciliationResource.java`
- **檔案路徑**：`domain-reconciliation/reconciliation-api/src/main/java/com/example/reconciliation/ReconciliationResource.java`
- **套件名稱**：`com.example.reconciliation`
- **標註**：`@Path("/reconciliation")`, `@Produces(MediaType.APPLICATION_JSON)`
- **設計定位**：提供對外觸發對帳單 CSV 匯出之 JAX-RS REST 端點。
- **核心方法**：
  1. `GET /reconciliation/export-csv`：支援 Query Parameters（`workflowId`, `businessKey`, `status`）進行過濾。
  2. `POST /reconciliation/export-csv`：支援 JSON Request Body 進行程式化篩選與匯出。

---

### 8.3 `ReconciliationCsvExporter.java`
- **檔案路徑**：`domain-reconciliation/reconciliation-api/src/main/java/com/example/reconciliation/ReconciliationCsvExporter.java`
- **套件名稱**：`com.example.reconciliation`
- **標註**：`@ApplicationScoped`
- **設計定位**：高效能對帳單 CSV 串流匯出引擎。
- **核心特性與方法**：
  1. `exportToCsv(String workflowId, String businessKey, String status)`：
     - 查詢視圖 `vw_saga_reconciliation`，動態拼接 SQL WHERE 條件。
     - 產出檔案名稱格式：`data/reconciliation-YYYYMMDD-HHmmss.csv`。
     - **UTF-8 BOM 支援**：開頭寫入 `\ufeff`，確保 Microsoft Excel 開啟時中文欄位不亂碼。
     - **記憶體保護**：使用 `BufferedWriter` 配合 JDBC `ResultSet` 逐列串流輸出，支援超大資料量匯出。
     - **CSV 跳脫 (`escapeCsv`)**：自動對包含逗號、換行或引號之字串加上雙引號並將內部引號轉義為 `""`。
  2. `exportReconciliationCsv(...)` 多載方法：提供 JsonNode 與 Map 進入點，供 SonataFlow 流程在終點透過工作流動作自動觸發匯出。

---

## 9. 分發應用層代碼生成機制 (distribution/*)

在 `myhello-platform` 架構中，`distribution/transfer-app` 與 `distribution/reconciliation-app` 模組內部**完全不需要手寫任何 Java 原始程式碼**：

1. **資源自動同步 (`maven-resources-plugin`)**：
   - 在 `initialize` 階段，Maven 外掛將 `domain-transfer` 與 `domain-reconciliation` 下的 `.sw.yaml` 流程圖及 OpenAPI YAML 同步至 `src/main/resources`。
2. **Kogito 編譯期代碼生成 (`quarkus:generate-code`)**：
   - Kogito 自動為每個 `.sw.yaml` 產生對應的 JAX-RS REST Controller（如 `Saga2_transfer_workflowResource.java`）、流程定義類別（`Saga2_transfer_workflowProcess.java`）以及 WorkItemHandlers。
3. **Quarkus Arc CDI 依賴發現 (`jandex-maven-plugin`)**：
   - 透過跨模組的 Jandex 索引與 `application.properties` 中的 `quarkus.index-dependency.*` 配置，Quarkus 自動裝配平台層過濾器與領域層業務服務。
