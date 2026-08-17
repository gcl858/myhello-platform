#!/usr/bin/env bash
# ==============================================================================
# offline-build.sh
#
# 作用：在斷網/無外網連線 (Air-Gapped / Offline) 環境中，使用專案內建的離線套件庫進行完整編譯與打包
# ==============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OFFLINE_REPO="${SCRIPT_DIR}/offline-packages/maven-repo"

if [ ! -d "${OFFLINE_REPO}" ]; then
    echo "❌ 錯誤：找不到離線套件庫目錄: ${OFFLINE_REPO}"
    exit 1
fi

echo "================================================================="
echo "開始執行 100% 嚴格離線編譯與打包 (mvn clean package -o)..."
echo "使用離線 Maven Repository: ${OFFLINE_REPO}"
echo "================================================================="

mvn clean package -o -Dmaven.repo.local="${OFFLINE_REPO}" "$@"

echo "================================================================="
echo "✅ 離線編譯與打包成功！"
echo "產出物："
echo "  - distribution/transfer-app/target/quarkus-app/quarkus-run.jar"
echo "  - distribution/reconciliation-app/target/quarkus-app/quarkus-run.jar"
echo "================================================================="
