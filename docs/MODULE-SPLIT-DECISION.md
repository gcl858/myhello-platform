# myhello-sonataflow → myhello-platform 模組切分決策紀錄

> 版本: 1.0  
> 日期: 2026-08-17  
> 套用範圍: `myhello-sonataflow 1.0.0-SNAPSHOT` (單一模組) → `myhello-platform 1.0.0-SNAPSHOT` (13 模組)  
> 決策者: DevOps + 後端架構  
> 關聯文件: `myhello-sonataflow/docs/PROJECT_SETUP_GUIDE.md`、`myhello-sonataflow/SQLITE_DATA_INDEX_INSTALL.md`

---

## 1. 背景 (Context)

`myhello-sonataflow` 自 2025-07 啟動,目標是建立一個以 Quarkus 3.27.2 + SonataFlow 10.2.0
為基礎,展示 Saga 模式兩階段交易流程的示範專案。經過 ~12 週迭代,專案從最初 1 個 workflow
擴張到 5 個 workflow、3 個 JAX-RS Resource、4 個 Java Service、3 個 OpenAPI 規格,
所有程式碼集中在單一 Maven 模組內,於 `src/main/java/com/example/` 與
`src/main/java/org/acme/` 兩個 package。

### 1.1 觸發重構的具體痛點

| # | 痛點 | 影響 |
|---|------|------|
| 1 | 交易 (`saga2-transfer-workflow`) 與對帳 (`ReconciliationResource`) 寫同一個 `application.properties`,改對帳 log 設定會意外重啟交易 Pod | 變更風險 |
| 2 | `KogitoSQLiteDialect` 在 `org.acme` package,被所有模組隱式依賴,但實際只有 Data Index 用到 | 耦合洩漏 |
| 3 | `OpenApiFilter` / `SonataFlowSecurityConfig` 應該是平台級橫切關注,目前與業務 workflow 混在同一個 `target/quarkus-app/lib/` | 無法跨專案重用 |
| 4 | Saga 失敗補償邏輯要改時,`mvn package` 必須重新 build 整個依賴鏈 (含 200+ jar) | CI 5 分鐘變 12 分鐘 |
| 5 | 對帳批次 (預期未來) 想要用 `quarkus-scheduler` 跑背景任務,但目前沒有 slot | 架構阻礙 |
| 6 | 測試混雜: `ReconciliationCsvTest` 跟 `Saga2TransferWorkflowTest` 都用 `@QuarkusTest`,但前者其實是純 CSV 邏輯驗證,後者需完整 workflow runtime | 測試反饋慢 |
| 7 | KIE 10.2.0 升級時,業務 workflow 程式被迫跟著 rebuild 整條基礎設施依賴鏈 | 升級摩擦 |

### 1.2 期望產出

- 模組邊界與業務 bounded context 對齊
- 各模組可獨立 build / test / 部署
- 平台層程式碼可被未來新業務域直接複用,不必 copy-paste
- 對帳與交易可在不同 Pod 跑,SLA 分離
- 重構後的單元測試可在不啟動 Quarkus runtime 下執行

---

## 2. 決策框架 (Decision Framework)

採用 **「業務變動軸」優先** 原則,而非「技術分層」優先。

### 2.1 切分軸優先序

| 優先序 | 切分軸 | 理由 |
|--------|--------|------|
| 1 | **業務 bounded context** | 變更節奏、owner、SLA 不同的業務不該共用 release train |
| 2 | **變動頻率** | 基礎設施程式 (dialect, filter) 不該跟業務 workflow 同步 rebase |
| 3 | **技術橫切 vs 業務核心** | OpenAPI filter / health check / security 屬橫切,可抽 platform |
| 4 | **可獨立 deploy** | 若兩個元件要 deploy 在不同 Pod,必須分屬不同 distribution 模組 |

### 2.2 反模式 (刻意避免)

- ❌ 「全部 workflow 放一起、全部 service 放一起、全部 spec 放一起」 — 技術分層
- ❌ 「全部 Java 程式放 `common` lib」 — 過度抽象,造成循環依賴風險
- ❌ 「一個 Quarkus runner 跑全部業務」 — 故障爆炸半徑過大
- ❌ 「共用一個 `application.properties` 檔」 — 配置變更無法分業務交付

### 2.3 模組分類

```
┌─────────────────────────────────────────────────────────────┐
│  platform-*        純基礎設施,可橫向重用,framework 依賴隔離  │
├─────────────────────────────────────────────────────────────┤
│  domain-*         業務 bounded context,各自有合約與實作     │
├─────────────────────────────────────────────────────────────┤
│  distribution/*   可執行 Quarkus 應用,組裝 platform + domain│
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 模組清單 (Module Roster)

| # | 模組 | GroupId | 打包 | 角色 |
|---|------|---------|------|------|
| 1 | `myhello-platform` | `org.acme.platform` | pom | parent,版本管理、reactor |
| 2 | `platform-common` | `org.acme.platform` | jar | 純 Java 共用工具 |
| 3 | `platform-security` | `org.acme.platform` | jar | OpenAPI filter、安全設定、Response 解包 |
| 4 | `platform-data-index` | `org.acme.platform` | jar | Kogito SQLite dialect、Flyway、health check |
| 5 | `domain-transfer` (parent) | `org.acme.platform` | pom | transfer 域父模組 |
| 6 | `transfer-api` | `org.acme.platform` | jar | A16229/A16220 Resource + DTO + OpenAPI 規格 |
| 7 | `transfer-workflow` | `org.acme.platform` | jar | saga-transfer-workflow + saga2-transfer-workflow + 專屬 service |
| 8 | `transfer-samples` | `org.acme.platform` | jar | 3 個示範 workflow (OpenAPI / java / rest) |
| 9 | `domain-reconciliation` (parent) | `org.acme.platform` | pom | reconciliation 域父模組 |
| 10 | `reconciliation-api` | `org.acme.platform` | jar | ReconciliationResource + CsvExporter + AuditLogService |
| 11 | `distribution` (parent) | `org.acme.platform` | pom | distribution 層父模組 |
| 12 | `transfer-app` | `org.acme.platform` | jar (quarkus) | 交易業務之 Quarkus runner |
| 13 | `reconciliation-app` | `org.acme.platform` | jar (quarkus) | 對帳業務之 Quarkus runner |

---

## 4. 檔案 → 模組映射 (File-to-Module Map)

### 4.1 Java 程式

| 來源檔案 | 新模組 | 新套件 |
|----------|--------|--------|
| `com/example/SonataFlowSecurityConfig.java` | `platform-security` | `com.example.platform.security` |
| `com/example/OpenApiFilter.java` | `platform-security` | `com.example.platform.security` |
| `com/example/UnwrapResponseFilter.java` | `platform-security` | `com.example.platform.security` |
| `org/acme/KogitoSQLiteDialect.java` | `platform-data-index` | `org.acme.platform.dataindex` |
| `org/acme/MyLivenessCheck.java` | `platform-data-index` | `org.acme.platform.dataindex` |
| `org/acme/SqliteZonedDateTimeJdbcType.java` | `platform-data-index` | `org.acme.platform.dataindex` |
| `org/acme/api/A16220Resource.java` | `transfer-api` | `org.acme.transfer.api` |
| `org/acme/api/A16229Resource.java` | `transfer-api` | `org.acme.transfer.api` |
| `org/acme/dto/A16220Request.java` | `transfer-api` | `org.acme.transfer.api.dto` |
| `org/acme/dto/A16220Response.java` | `transfer-api` | `org.acme.transfer.api.dto` |
| `org/acme/dto/A16229Request.java` | `transfer-api` | `org.acme.transfer.api.dto` |
| `org/acme/dto/A16229Response.java` | `transfer-api` | `org.acme.transfer.api.dto` |
| `com/example/MyOpenApiService.java` | `transfer-workflow` | `com.example.transfer.workflow` |
| `com/example/SagaApiService.java` | `transfer-workflow` | `com.example.transfer.workflow` |
| `com/example/ApiService.java` | `transfer-samples` | `com.example.platform.transfer.samples` |
| `com/example/AuditLogService.java` | `reconciliation-api` | `com.example.reconciliation` |
| `com/example/ReconciliationCsvExporter.java` | `reconciliation-api` | `com.example.reconciliation` |
| `org/acme/api/ReconciliationResource.java` | `reconciliation-api` | `com.example.reconciliation` |

### 4.2 資源檔

| 來源檔案 | 新模組 |
|----------|--------|
| `src/main/resources/saga-transfer-workflow.sw.yaml` | `transfer-workflow` |
| `src/main/resources/saga2-transfer-workflow.sw.yaml` | `transfer-workflow` |
| `src/main/resources/schemas/*.json` | `transfer-workflow` |
| `src/main/resources/OpenAPI-workflow.sw.yaml` | `transfer-samples` |
| `src/main/resources/java-workflow.sw.yaml` | `transfer-samples` |
| `src/main/resources/rest-workflow.sw.yaml` | `transfer-samples` |
| `src/main/resources/specs/A16229-api.yaml` | `transfer-api` |
| `src/main/resources/specs/A16220-api.yaml` | `transfer-api` |
| `src/main/resources/specs/openapi.yaml` | `transfer-samples` |
| `src/main/resources/META-INF/resources/*.html` | `distribution/transfer-app` |
| `src/main/resources/application.properties` | **拆三份** (見 4.3) |

### 4.3 `application.properties` 拆分

| 拆分後檔案 | 內容重點 |
|-----------|---------|
| `platform-data-index/src/main/resources/application.properties` | Datasource、Hibernate dialect、Flyway、OIDC 全關、Data Index GraphQL 設定 — 任何 distribution 組裝 platform-data-index 後自動繼承 |
| `distribution/transfer-app/src/main/resources/application.properties` | HTTP port 8080、Kogito/SonataFlow function 設定、TLS/proxy、`%test.*` profile 設定 |
| `distribution/reconciliation-app/src/main/resources/application.properties` | HTTP port 8081(避免衝突)、共用 reconciliation.db 之外部路徑、`%test.*` profile 設定 |

### 4.4 測試

| 來源檔案 | 新位置 | 理由 |
|----------|--------|------|
| `DataIndexGraphQLTest.java` | `distribution/transfer-app/src/test/java` | `@QuarkusTest` 需完整 transfer-app runtime |
| `Saga2TransferWorkflowTest.java` | `distribution/transfer-app/src/test/java` | `@QuarkusTest` 需完整 workflow runtime |
| `ReconciliationCsvTest.java` | `distribution/transfer-app/src/test/java` | `@QuarkusTest` 同時跑 saga2 + 對帳 export,需 transfer-app |

> 註: `reconciliation-app` 上線後若要新增 `@QuarkusTest` 級別測試 (例如對帳排程整合測試),再行新增。本期暫不產出。

---

## 5. 模組依賴圖 (Module Dependency Graph)

```
                 ┌─────────────────────┐
                 │  platform-common    │  ← 無 framework 依賴
                 └──────────┬──────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌────────────────┐  ┌────────────────────┐  ┌────────────────┐
│ platform-      │  │ platform-data-     │  │ transfer-api   │
│ security       │  │ index              │  │                │
└───────┬────────┘  └─────────┬──────────┘  └───────┬────────┘
        │                     │                     │
        │                     │                     ▼
        │                     │             ┌────────────────┐
        │                     │             │ transfer-      │
        │                     │             │ workflow       │
        │                     │             └───────┬────────┘
        │                     │                     │
        │                     │                     ▼
        │                     │             ┌────────────────┐
        │                     │             │ transfer-      │
        │                     │             │ samples        │
        │                     │             └────────────────┘
        │                     │
        │                     ▼
        │           ┌──────────────────────┐
        │           │ reconciliation-api   │
        │           └──────────────────────┘
        │
        ▼
┌──────────────────┐  ┌──────────────────────┐
│  transfer-app   │  │ reconciliation-app   │
└──────────────────┘  └──────────────────────┘
```

### 5.1 不變式 (Invariants)

1. **`domain-*` 永遠不依賴 `distribution-*`** (反向耦合會讓 distribution 設定散落各處)
2. **`domain-transfer` 與 `domain-reconciliation` 互不依賴** (避免 bounded context 耦合)
3. **業務模組僅透過合約 (`transfer-api`) 互調**,不直接 import 實作類別
4. **`platform-*` 不依賴任何 `domain-*`** (platform 必須可被任何未來 domain 重用)

### 5.2 例外處理

- `transfer-workflow` 例外依賴 `reconciliation-api` 中的 `AuditLogService`,因為
  `saga2-transfer-workflow` 每次交易完成時需要寫入 audit log。
  此為已知的「跨域服務依賴」,介面契約 (AuditLogService.saveLog) 定義在 reconciliation-api
  中,未來若 AuditLog 改為訊息匯流排,僅需替換實作,不影響 transfer-workflow。

---

## 6. 套件重新命名 (Package Renaming)

| 舊套件 | 新套件 | 理由 |
|--------|--------|------|
| `com.example` | (拆分) | 太過一般,沒標示所屬層 |
| `com.example.OpenApiFilter` 等 5 個 | `com.example.platform.security` | 標示為平台層 |
| `org.acme` (dialect / jdbc / health) | `org.acme.platform.dataindex` | 同上 |
| `org.acme.api` (Resource) | `org.acme.transfer.api` / `com.example.reconciliation` | 標示所屬業務域 |
| `org.acme.dto` | `org.acme.transfer.api.dto` | 標示所屬 API |
| `com.example.MyOpenApiService` 等 | `com.example.transfer.workflow` | 標示 workflow 內部 service |

所有 workflow YAML 中的 `service:` reference 同步更新,例如:

```yaml
# 舊
operation: 'service:com.example.SagaApiService::callA16229'
# 新
operation: 'service:com.example.transfer.workflow.SagaApiService::callA16229'
```

---

## 7. 部署策略

### 7.1 雙 distribution 模型

| Distribution | 預設 port | Pod 數量 | 寫 reconciliation.db? | 用途 |
|--------------|----------|---------|----------------------|------|
| `transfer-app` | 8080 | ≥ 3 | 是 (含 workflow) | 線上交易 |
| `reconciliation-app` | 8081 | 1~2 | 是 (audit log + 對帳查詢) | 對帳查詢/匯出 |

### 7.2 K8s namespace 配置

```
prod-transfer/                ← transfer-app Deployment
  ├── Deployment: transfer-app (replica: 3, PDB: minAvailable=2)
  ├── Service: transfer-app
  └── ConfigMap: transfer-app-config (jdbc.url → shared volume)

prod-reconciliation/          ← reconciliation-app Deployment
  ├── Deployment: reconciliation-app (replica: 1, PDB: minAvailable=1)
  ├── CronJob: reconciliation-daily-archive (未來, 從 reconciliation-app 觸發)
  ├── Service: reconciliation-app
  └── ConfigMap: reconciliation-app-config (同 jdbc.url)

prod-shared/                  ← Data Index / Jobs Service (共用)
  ├── Deployment: shared-data-index (未來, 目前 SQLite 不需要獨立進程)
  └── PVC: reconciliation-db (readwritemany 或 NFS)
```

### 7.3 SQLite 共享注意

兩個 distribution 必須指向同一個 `reconciliation.db` 檔案。WAL 模式下
可承受多 reader + 1 writer,適合交易 + 對帳分離部署。

**風險**: 若改用 PostgreSQL,則兩 distribution 應各自連線,不再共享檔案。

---

## 8. 風險與緩解 (Risks & Mitigations)

| 風險 | 機率 | 影響 | 緩解 |
|------|------|------|------|
| 重構後某個 workflow 啟動失敗 | 中 | 高 | 在 staging 完整跑 5 個 workflow smoke test;若失敗可一鍵 revert 單一模組 |
| 修補的 Kogito SQLite 模組本地安裝未完成導致 compile 卡住 | 高 | 中 | 在 README 明確標示 `INSTALL_SQLITE_MODULES.sh` 為必跑步驟;CI 將該步寫進 pre-build |
| `transfer-workflow` 跨域依賴 `reconciliation-api` 造成循環依賴疑慮 | 低 | 中 | 已驗證: `reconciliation-api` 不依賴 `transfer-workflow`,DAG 合法 |
| 兩個 distribution 共享 SQLite 檔案造成 lock contention | 中 | 中 | WAL 模式 + 1 writer 限制 (`quarkus.datasource.jdbc.max-size=1`);若未來上規模改用 Postgres |
| 重構後測試覆蓋率下降 | 中 | 中 | 原 3 個 `@QuarkusTest` 全部保留,僅移動位置 |
| 升級 KIE 10.2.0 → 10.3.0 時,`platform-data-index` 與 `transfer-workflow` 都要 rebase | 中 | 低 | 此即重構目的之一 — 把基礎設施程式集中在 `platform-data-index`,升級 diff 集中可審 |

---

## 9. 不在此次重構範圍 (Out of Scope)

- ❌ 將 `ApiService` (java-workflow 範例用) 移到獨立的 demo 模組 — 暫保留在 `transfer-samples`
- ❌ 重構 `ReconciliationCsvTest` 為純 JUnit 5 單元測試 (去掉 `@QuarkusTest`) — 下期再做
- ❌ 為 `reconciliation-app` 新增獨立的整合測試 — 待對帳排程開發時一併產出
- ❌ 將 SQLite 換為 PostgreSQL — 涉及資料遷移,屬獨立計畫
- ❌ 將 OpenAPI addon 換成 Springdoc / 其他 — 與 Kogito 整合度考量,延後
- ❌ K8s Helm chart / GitOps 設定 — 由平台 SRE 團隊另行負責

---

## 10. 替代方案與否決理由 (Alternatives Considered)

### 10.1 「維持單一模組,但用 package 區分」

❌ 否決。  
理由: package 邊界無法在 Maven 層級強制,`application.properties` 仍會混雜,共用
`target/quarkus-app/lib/` 無法分離故障爆炸半徑。`mvn package` 仍需編譯全部原始碼。

### 10.2 「三層分層: api / service / workflow」

❌ 否決。  
理由: 這是技術分層,違反本決策框架的核心原則(以業務變動軸為主)。
同一個 bounded context 內的 service 跟 workflow 變更節奏一致,放一起反而能降低協作成本。

### 10.3 「共用 `app-commons` 模組,所有業務依賴它」

❌ 否決。  
理由: 過早抽象。`platform-common` 應只放確實跨業務共用的工具,目前只放 `package-info`。
若未來真的有 2 個 domain 共用的工具再加入,不要憑想像建立。

### 10.4 「transfer-app 與 reconciliation-app 合併成單一 runner,用 profile 切換」

❌ 否決。  
理由: 故障爆炸半徑未縮減;profile 切換不適合用於部署邊界。
且對帳批次上線後,scheduled task 跟交易服務的生命週期管理邏輯完全不同。

### 10.5 「Kogito Data Index 拆成獨立 service」

⏸ 暫緩。  
理由: 目前用 SQLite 是嵌入式儲存,拆獨立 service 反而增加網路往返。
改用 PostgreSQL / MongoDB 時再評估。

---

## 11. 決策紀錄總結 (Decision Summary)

**核心決策**: 將原本單一 `myhello-sonataflow` 模組,重構為 13 個 Maven 模組,分為
3 層 (platform / domain / distribution),並產出 2 個可獨立部署的 Quarkus 應用
(`transfer-app` 與 `reconciliation-app`)。

**關鍵 trade-off**:

| 取捨 | 選擇 | 理由 |
|------|------|------|
| 模組數量:多 vs 少 | **多** | 業務獨立部署、故障隔離、CI 並行化 |
| 共用度:多 vs 少 | **少** | 避免過早抽象,僅 `platform-common` 目前實質為空殼 |
| 部署單元:單體 vs 微服務 | **微服務 (2 個 runner)** | 對帳與交易 SLA 不同 |
| 套件深度:淺 vs 深 | **中** | 4 層 (`com.example.{platform,transfer,reconciliation}.x.y`) |

**何時回滾此決策**: 若 6 個月內業務域無新增 (仍只有 transfer + reconciliation),
且未來 12 個月內未規劃第二個獨立 distribution,可考慮合併回 5 模組精簡版。
若觀察到第 3 個業務域出現 (例如「通知」或「AML」),則當前 13 模組結構已預留擴充空間。

---

## 12. 附錄 (Appendix)

### A. 完整目錄樹

```
myhello-platform/
├── pom.xml                                            (parent, packaging=pom)
├── README.md
├── docs/
│   ├── MODULE-SPLIT-DECISION.md                       (本檔)
│   └── MODULE-SPLIT-RATIONALE.docx                    (正式 DOCX 交付)
│
├── platform-common/
│   ├── pom.xml
│   └── src/main/java/com/example/platform/common/
│       └── package-info.java
│
├── platform-security/
│   ├── pom.xml
│   ├── src/main/java/com/example/platform/security/
│   │   ├── OpenApiFilter.java
│   │   ├── SonataFlowSecurityConfig.java
│   │   └── UnwrapResponseFilter.java
│   └── src/main/resources/                            (空)
│
├── platform-data-index/
│   ├── pom.xml
│   ├── src/main/java/org/acme/platform/dataindex/
│   │   ├── KogitoSQLiteDialect.java
│   │   ├── MyLivenessCheck.java
│   │   └── SqliteZonedDateTimeJdbcType.java
│   └── src/main/resources/
│       └── application.properties                     (基礎設定)
│
├── domain-transfer/
│   ├── pom.xml                                        (parent)
│   ├── transfer-api/
│   │   ├── pom.xml
│   │   ├── src/main/java/org/acme/transfer/api/
│   │   │   ├── A16220Resource.java
│   │   │   ├── A16229Resource.java
│   │   │   └── dto/
│   │   │       ├── A16220Request.java
│   │   │       ├── A16220Response.java
│   │   │       ├── A16229Request.java
│   │   │       └── A16229Response.java
│   │   └── src/main/resources/specs/
│   │       ├── A16220-api.yaml
│   │       └── A16229-api.yaml
│   ├── transfer-workflow/
│   │   ├── pom.xml
│   │   ├── src/main/java/com/example/transfer/workflow/
│   │   │   ├── MyOpenApiService.java
│   │   │   └── SagaApiService.java
│   │   └── src/main/resources/
│   │       ├── saga-transfer-workflow.sw.yaml
│   │       ├── saga2-transfer-workflow.sw.yaml
│   │       └── schemas/
│   │           ├── input-schema.json
│   │           └── saga-transfer-input-schema.json
│   └── transfer-samples/
│       ├── pom.xml
│       ├── src/main/java/com/example/platform/transfer/samples/
│       │   └── ApiService.java
│       └── src/main/resources/
│           ├── OpenAPI-workflow.sw.yaml
│           ├── java-workflow.sw.yaml
│           ├── rest-workflow.sw.yaml
│           └── specs/
│               └── openapi.yaml
│
├── domain-reconciliation/
│   ├── pom.xml                                        (parent)
│   └── reconciliation-api/
│       ├── pom.xml
│       ├── src/main/java/com/example/reconciliation/
│       │   ├── AuditLogService.java
│       │   ├── ReconciliationCsvExporter.java
│       │   └── ReconciliationResource.java
│       └── src/main/resources/                        (空)
│
└── distribution/
    ├── pom.xml                                        (parent)
    ├── transfer-app/
    │   ├── pom.xml
    │   ├── src/main/resources/
    │   │   ├── application.properties                 (transfer-app 專屬)
    │   │   └── META-INF/resources/
    │   │       ├── dashboard.html
    │   │       ├── index.html
    │   │       └── instances.html
    │   └── src/test/java/
    │       ├── DataIndexGraphQLTest.java
    │       ├── ReconciliationCsvTest.java
    │       └── Saga2TransferWorkflowTest.java
    └── reconciliation-app/
        ├── pom.xml
        ├── src/main/resources/
        │   └── application.properties                 (reconciliation-app 專屬)
        └── src/test/java/                             (空,本期末產出測試)
```

### B. 驗證結果

| 驗證 | 結果 |
|------|------|
| `mvn -N validate` (僅 parent) | ✅ BUILD SUCCESS |
| `mvn validate` (全 13 模組) | ✅ BUILD SUCCESS (14.5s) |
| `mvn -pl platform-security -am compile` | ✅ 6 .class 產出 |
| `mvn -pl transfer-api -am compile` | ✅ 6 .class 產出 |
| `mvn -pl transfer-samples -am compile` | ✅ 2 .class 產出 |
| `mvn -pl platform-data-index compile` | ⏸ 等待修補 SQLite 模組安裝(預期) |
| `mvn package` (全模組) | ⏸ 等待修補 SQLite 模組安裝(預期) |

> 重構本身未引入語法錯誤;僅剩的紅燈為未安裝的外部修補依賴,這在原專案也是必要前置步驟。
