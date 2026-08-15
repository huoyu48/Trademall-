#!/usr/bin/env bash
# OrderFlow 一键启动（docker compose 全栈：mysql/redis/rabbitmq/backend/frontend）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

docker compose up -d --build

echo "✅ OrderFlow 已启动"
echo "   前端访问:  http://localhost:8088"
echo "   后端 API:  http://localhost:8088/api"
echo "   登录账号:  admin-a / admin123  (或 admin-b / admin123)"
echo ""
echo "⚠️ 演示完毕请执行: ./scripts/stop.sh  停止容器，避免中间件常驻持续发热"
