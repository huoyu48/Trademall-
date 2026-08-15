#!/usr/bin/env bash
# OrderFlow 一键停止（释放所有容器资源，避免中间件常驻持续发热）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
docker compose down
echo "✅ OrderFlow 容器已停止，资源已释放"
