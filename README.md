# myhello-platform

> 從單一 `myhello-sonataflow` 模組重構出來的企業級多模組 SonataFlow 平台。

## 模組全景

```
myhello-platform/                                    ← parent POM (packaging=pom)
│
├── platform-common/                                 ← 共用工具層 (無 framework 依賴)
│
├── platform-security/                               ← 橫切:OpenAPI filter, SonataFlow security, Response unwrap
│
├── platform-extensions/                             ← Kogito / Quarkus 擴展與 SQLite 修補原始碼 (Data Index & Storage)
│   ├── data-index-storage-sqlite/                   ← SQLite 儲存適配器與 22 個 Flyway SQL 腳本
│   ├── kogito-addons-quarkus-data-index-sqlite/     ← Data Index Runtime 與 Deployment 擴展
│   └── kogito-addons-quarkus-data-index-persistence-sqlite/ ← 持久化 Runtime 與 Deployment 擴展
│
├── platform-data-index/                             ← Kogito Data Index + SQLite 自訂 dialect + 健康檢查
│
├── domain-transfer/                                 ← Bounded context: 轉帳 (parent)
│   ├── transfer-api/                                ← OpenAPI specs + DTO + JAX-RS mock
│   ├── transfer-workflow/                           ← saga-transfer-workflow + saga2-transfer-workflow
│   └── transfer-samples/                            ← OpenAPI/java/rest-workflow 示範
│
├── domain-reconciliation/                           ← Bounded context: 對帳 (parent)
│   └── reconciliation-api/                          ← ReconciliationResource + CsvExporter + AuditLog
│
└── distribution/                                    ← Runnable Quarkus 應用 (parent)
    ├── transfer-app/                                ← 組裝平台層 + transfer domain → 1 個 quarkus-run.jar
    └── reconciliation-app/                          ← 組裝平台層 + reconciliation domain → 1 個 quarkus-run.jar
```

## 模組依賴圖 (DAG)

```
                  ┌──────────────────┐
                  │ platform-common  │  ← leaf
                  └────────┬─────────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
┌──────────────┐   ┌────────────────┐   ┌────────────────┐
│platform-     │   │platform-       │   │transfer-api    │
│security      │   │data-index      │   │                │
└──────┬───────┘   └────────┬───────┘   └────────┬───────┘
       │                    │                    │
       │                    │                    ▼
       │                    │            ┌────────────────┐
       │                    │            │transfer-       │
       │                    │            │workflow        │
       │                    │            └────────┬───────┘
       │                    │                     │
       │                    │            ┌────────▼───────┐
       │                    │            │transfer-       │
       │                    │            │samples         │
       │                    │            └────────────────┘
       │                    │
       │                    ▼
       │           ┌─────────────────┐
       │           │reconciliation-  │
       │           │api              │
       │           └─────────────────┘
       │
       ▼
┌──────────────────┐        ┌──────────────────┐
│transfer-app      │        │reconciliation-   │
│(Quarkus runner)  │        │app               │
│                  │        │(Quarkus runner)  │
└──────────────────┘        └──────────────────┘
```

**核心不變式**:

1. 沒有任何 `domain-*` 反向依賴 `platform-*`(domain 只能往下)
2. 沒有 `transfer-*` 依賴 `reconciliation-*` 內部實作(只有 `transfer-workflow` 用 `AuditLogService` 介面,合約定義在 reconciliation-api)
3. `distribution/*` 是唯一允許組裝 platform + domain 的地方
4. 沒有任何循環依賴

## 部署模型

| Distribution       | Port | 寫入 reconciliation.db? | Pod 數量 | 用途         |
|--------------------|------|------------------------|---------|--------------|
| `transfer-app`     | 8080 | 是 (含 workflow)       | ≥ 3     | 線上交易     |
| `reconciliation-app` | 8081 | 是 (audit log)        | 1~2     | 對帳查詢/匯出 |

兩個 distribution 透過共用 volume / 共享 storage 寫同一個 `reconciliation.db`。
SQLite WAL 模式可承受多 reader + 1 writer,但若改用 Postgres 就不必共享。

## 快速建置與執行

### A. 一般建置
```bash
# 1. 執行完整編譯與打包
mvn clean package

# 2. 分別啟動服務
java -jar distribution/transfer-app/target/quarkus-app/quarkus-run.jar        # Port 8080 (轉帳交易)
java -jar distribution/reconciliation-app/target/quarkus-app/quarkus-run.jar  # Port 8081 (對帳查詢)
```

### B. 斷網主機 100% 離線建置 (Air-Gapped / Offline)
專案內建完整的離線依賴庫，完全不需要外網連線：
```bash
# 一鍵離線編譯與打包 (使用內建 offline-packages/maven-repo)
./offline-build.sh
```

## 相關架構與決策文件

- **完整系統架構說明文件 (Markdown + Mermaid)**：[docs/SYSTEM-ARCHITECTURE.md](docs/SYSTEM-ARCHITECTURE.md)
- **Java 程式碼詳細說明文件**：[docs/CODE-SPECIFICATION.md](docs/CODE-SPECIFICATION.md)
- **模組切分決策紀錄**：[docs/MODULE-SPLIT-DECISION.md](docs/MODULE-SPLIT-DECISION.md)
- **模組切分論證報告**：[docs/MODULE-SPLIT-RATIONALE.docx](docs/MODULE-SPLIT-RATIONALE.docx)
