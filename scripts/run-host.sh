#!/usr/bin/env bash
# OrderFlow 本地后端启动（host 模式）
# 带 JVM 内存上限，避免空载时 GC 风暴/内存 swap 导致电脑发烫
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
unset SERVER__PORT
MVN="${MVN:-./mvnw}"

if ! ls target/*.jar >/dev/null 2>&1; then
  echo "未找到 jar，先离线构建..."
  $MVN -o -DskipTests package
fi

echo "▶ 启动 OrderFlow 后端 (http://localhost:8080)，JVM 堆上限 512m"
exec java -Xms256m -Xmx512m -XX:+UseG1GC -Dserver.port=8080 -jar target/*.jar
