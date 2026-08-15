#!/usr/bin/env bash
# OrderFlow 一键构建脚本：后端 jar + 前端静态产物 + docker 镜像
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MVN="${MVN:-./mvnw}"

echo "==> [1/3] 构建后端 jar"
cd "$ROOT"
$MVN -q -DskipTests clean package

echo "==> [2/3] 构建前端静态产物"
cd "$ROOT/web"
npm install
npm run build

echo "==> [3/3] 构建 docker 镜像"
cd "$ROOT"
docker compose build

echo "✅ 构建完成。运行: ./scripts/run.sh"
