#!/usr/bin/env bash
# 并发下单验证：创建库存 100 的商品，发起并发订单，并核对库存守恒。
# 前置条件：docker compose 已启动，服务地址默认经前端 Nginx 代理到 http://127.0.0.1:8088/api。
set -euo pipefail

BASE="${BASE_URL:-http://127.0.0.1:8088/api}"
CONCURRENCY="${CONCURRENCY:-30}"
QTY="${QTY:-3}"
INITIAL_STOCK="${INITIAL_STOCK:-100}"
TMP_DIR="$(mktemp -d /tmp/orderflow-concurrency.XXXXXX)"
trap 'rm -rf "$TMP_DIR"' EXIT

token() {
  curl -fsS -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d '{"username":"admin-a","password":"admin123"}' \
    | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["token"])'
}

TOKEN="$(token)"
SKU="CONC-$(date +%s)"
CREATE=$(curl -fsS -X POST "$BASE/products" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"productCode\":\"$SKU\",\"productName\":\"并发压测商品\",\"unitPriceCent\":9900}")
PID=$(printf '%s' "$CREATE" | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["id"])')

curl -fsS -X POST "$BASE/inventories/adjustments" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"productId\":$PID,\"changeQuantity\":$INITIAL_STOCK,\"reason\":\"并发压测初始入库\"}" >/dev/null

echo "[stress] productId=$PID stock=$INITIAL_STOCK concurrency=$CONCURRENCY qty=$QTY"
for i in $(seq 1 "$CONCURRENCY"); do
  (
    curl -sS -X POST "$BASE/orders" -H "Authorization: Bearer $TOKEN" \
      -H "Idempotency-Key: stress-$SKU-$i" -H 'Content-Type: application/json' \
      -d "{\"customerName\":\"stress-$i\",\"items\":[{\"productId\":$PID,\"quantity\":$QTY}]}" \
      > "$TMP_DIR/$i.json"
  ) &
done
wait

SUCCESS=$(python3 - "$TMP_DIR" <<'PY'
import json, pathlib, sys
count = 0
for path in pathlib.Path(sys.argv[1]).glob('*.json'):
    try:
        count += json.loads(path.read_text()).get('code') == 0
    except json.JSONDecodeError:
        pass
print(count)
PY
)
INVENTORY=$(curl -fsS "$BASE/inventories" -H "Authorization: Bearer $TOKEN")
AVAILABLE=$(printf '%s' "$INVENTORY" | python3 -c "import json,sys; rows=json.load(sys.stdin)['data']; print(next(x['availableQuantity'] for x in rows if x['productId']==$PID))")
EXPECTED=$((INITIAL_STOCK - SUCCESS * QTY))

echo "[stress] success=$SUCCESS expectedAvailable=$EXPECTED actualAvailable=$AVAILABLE"
if [ "$AVAILABLE" -lt 0 ] || [ "$AVAILABLE" -ne "$EXPECTED" ]; then
  echo "[stress] FAIL: 库存不守恒或出现负数" >&2
  exit 1
fi
echo "[stress] PASS: 并发下库存不超卖，库存守恒"
