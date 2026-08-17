# 離線套件庫與斷網建置說明 (Offline Packages & Build Guide)

本專案提供**完整自包含（Self-Contained）的離線依賴庫**，當專案移植至**完全斷網主機（Air-Gapped / Offline Environment）**或無外部連線的伺服器時，無需存取網際網路或遠端 Maven 儲存庫即可 100% 獨立編譯與執行。

---

## 1. 離線套件目錄結構

```
offline-packages/
├── README.md                              # 本說明文件
├── install-offline-packages.sh            # 將修補套件拷貝至本機 ~/.m2/repository 腳本
├── kogito-sqlite-modules/                 # 客製修補之 Kogito SQLite 模組 (JAR, POM)
└── maven-repo/                            # 完整離線 Maven 儲存庫 (包含 Quarkus/Kogito/第三方依賴及外掛)
```

---

## 2. 兩種離線建置方式

### 方式 A：免安裝全自包含離線建置 (推薦 ⭐)
直接指定專案內的 `offline-packages/maven-repo` 作為 Maven 本機庫，不污染目標主機的 `~/.m2`：

```bash
# 執行專案根目錄的一鍵離線建置腳本
./offline-build.sh

# 或直接使用 Maven 指令 (加上 -o 嚴格離線模式)
mvn clean package -o -Dmaven.repo.local=./offline-packages/maven-repo
```

### 方式 B：將修補模組安裝至主機環境
若目標主機已有標準 Maven 環境與既有相依庫，僅需安裝本機客製修補的 Kogito SQLite 套件：

```bash
./offline-packages/install-offline-packages.sh
mvn clean package
```

---

## 3. 驗證記錄
本離線依賴庫已通過 `mvn clean package -o -Dmaven.repo.local=./offline-packages/maven-repo` 嚴格離線驗證，全 13 個模組均能順利編譯並產出執行檔。
