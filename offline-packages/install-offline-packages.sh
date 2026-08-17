#!/usr/bin/env bash
# ==============================================================================
# install-offline-packages.sh
# 
# 作用：將本機客製修補的 Kogito SQLite 模組直接安裝至本機 Maven 儲存庫 (~/.m2/repository)
# 適用場景：在斷網主機 (Offline / Air-Gapped Environment) 或新環境中快速就緒
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
M2_REPO="${HOME}/.m2/repository"

echo "================================================================="
echo "開始安裝客製修補 Kogito SQLite 離線套件至本機 Maven 儲存庫..."
echo "目標路徑: ${M2_REPO}"
echo "================================================================="

# 建立目標目錄
mkdir -p "${M2_REPO}/org/kie"
mkdir -p "${M2_REPO}/org/kie/kogito"

# 複製 org.kie 下的 kogito-addons-quarkus-data-index-*sqlite* 模組
echo "[1/2] 安裝 org.kie.kogito-addons-quarkus-data-index-sqlite 相關模組..."
cp -r "${SCRIPT_DIR}/kogito-sqlite-modules/kogito-addons-quarkus-data-index-sqlite" "${M2_REPO}/org/kie/"
cp -r "${SCRIPT_DIR}/kogito-sqlite-modules/kogito-addons-quarkus-data-index-sqlite-deployment" "${M2_REPO}/org/kie/"
cp -r "${SCRIPT_DIR}/kogito-sqlite-modules/kogito-addons-quarkus-data-index-sqlite-parent" "${M2_REPO}/org/kie/"
cp -r "${SCRIPT_DIR}/kogito-sqlite-modules/kogito-addons-quarkus-data-index-persistence-sqlite" "${M2_REPO}/org/kie/"
cp -r "${SCRIPT_DIR}/kogito-sqlite-modules/kogito-addons-quarkus-data-index-persistence-sqlite-deployment" "${M2_REPO}/org/kie/"
cp -r "${SCRIPT_DIR}/kogito-sqlite-modules/kogito-addons-quarkus-data-index-persistence-sqlite-parent" "${M2_REPO}/org/kie/"

# 複製 org.kie.kogito 下的 data-index-storage-sqlite 模組
echo "[2/2] 安裝 org.kie.kogito.data-index-storage-sqlite 模組..."
cp -r "${SCRIPT_DIR}/kogito-sqlite-modules/data-index-storage-sqlite" "${M2_REPO}/org/kie/kogito/"

echo "================================================================="
echo "✅ 離線套件安裝完成！"
echo "您現在可以在斷網環境中執行 'mvn clean package' 編譯本專案。"
echo "================================================================="
