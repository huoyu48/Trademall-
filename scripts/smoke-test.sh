#!/usr/bin/env bash
# OrderFlow 端到端冒烟测试：启动应用 --> 健康检查 --> 登录 --> 商品/库存 --> 订单生命周期
# --> 幂等 --> 跨租户隔离 --> RabbitMQ 接线验证
export no_proxy="127.0.0.1,localhost"
export NO_PROXY="127.0.0.1,localhost"

BASE="http://127.0.0.1:8080/api"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 17 2>/dev/null)}"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_BIN="${JAVA_BIN:-java}"
JAR="$ROOT/target/orderflow.jar"
LOG="/tmp/orderflow.log"

pass=0; fail=0
ck(){ # desc expected actual
  if [ "$2" = "$3" ]; then echo "  PASS ✅ $1 (HTTP $3)"; pass=$((pass+1));
  else echo "  FAIL ❌ $1 (expected $2 got $3)"; fail=$((fail+1)); fi
}
ok(){ echo "  PASS ✅ $1"; pass=$((pass+1)); }
no(){ echo "  FAIL ❌ $1"; fail=$((fail+1)); }

tok(){ # user pass
  curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" \
    | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])"
}

echo "=============================================="
echo "[1] 启动 OrderFlow 应用"
echo "=============================================="
pkill -f OrderFlowApplication 2>/dev/null; sleep 1
unset SERVER__PORT
nohup "$JAVA_BIN" -Dserver.port=8080 -jar "$JAR" > "$LOG" 2>&1 &
APP_PID=$!
echo "app pid=$APP_PID, waiting for /actuator/health ..."

up=""
for i in $(seq 1 40); do
  code=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/actuator/health")
  if [ "$code" = "200" ] || [ "$code" = "503" ]; then up=$code; echo "  health responded (HTTP $code) after ~$((i*3))s"; break; fi
  sleep 3
done
[ -z "$up" ] && { echo "APP DID NOT START. tail log:"; tail -30 "$LOG"; exit 1; }

echo "=============================================="
echo "[2] 健康检查（验证 Redis 密码修复后应为 UP）"
echo "=============================================="
HEALTH=$(curl -s "$BASE/actuator/health")
echo "$HEALTH" | python3 -c "
import sys,json
d=json.load(sys.stdin)
print('  status:', d.get('status'))
for k,v in d.get('components',{}).items():
    print('   -',k,':',v.get('status'))
"
RSTAT=$(echo "$HEALTH" | python3 -c "import sys,json;print(json.load(sys.stdin)['components']['redis']['status'])" 2>/dev/null)
[ "$RSTAT" = "UP" ] && ok "Redis 组件 UP（密码修复生效）" || no "Redis 组件未 UP (=$RSTAT)"

echo "=============================================="
echo "[3] 登录双租户"
echo "=============================================="
TOKEN_A=$(tok admin-a admin123); ck "租户A登录(admin-a)" "200" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin-a","password":"admin123"}')"
TOKEN_B=$(tok admin-b admin123); ck "租户B登录(admin-b)" "200" "$(curl -s -o /dev/null -w '%{http_code}' -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin-b","password":"admin123"}')"
echo "  tokenA len=${#TOKEN_A}, tokenB len=${#TOKEN_B}"

echo "=============================================="
echo "[4] 租户A 创建商品 + 调整库存"
echo "=============================================="
PCODE="SKU-$(date +%s)"
PCREATE=$(curl -s -X POST "$BASE/products" -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d "{\"productCode\":\"$PCODE\",\"productName\":\"测试商品\",\"unitPriceCent\":9900}")
ck "创建商品" "0" "$(echo "$PCREATE" | python3 -c "import sys,json;print(json.load(sys.stdin)['code'])")"
PID=$(echo "$PCREATE" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
echo "  productId=$PID"

ADJ=$(curl -s -X POST "$BASE/inventories/adjustments" -H "Authorization: Bearer $TOKEN_A" -H 'Content-Type: application/json' \
  -d "{\"productId\":$PID,\"changeQuantity\":100,\"reason\":\"初始入库\"}")
ck "调整库存 +100" "0" "$(echo "$ADJ" | python3 -c "import sys,json;print(json.load(sys.stdin)['code'])")"

INV=$(curl -s "$BASE/inventories" -H "Authorization: Bearer $TOKEN_A")
STOCK=$(echo "$INV" | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];print([x for x in d if x['productId']==$PID][0]['availableQuantity']) if any(x['productId']==$PID for x in d) else print('NA')" 2>/dev/null)
echo "  available stock for product $PID = $STOCK"

echo "=============================================="
echo "[5] 租户A 创建订单（带幂等键）+ 生命周期推进"
echo "=============================================="
IDEK="idem-$(date +%s)-001"
OCREATE=$(curl -s -X POST "$BASE/orders" -H "Authorization: Bearer $TOKEN_A" -H "Idempotency-Key: $IDEK" -H 'Content-Type: application/json' \
  -d "{\"customerName\":\"张三\",\"items\":[{\"productId\":$PID,\"quantity\":2}]}")
ck "创建订单" "0" "$(echo "$OCREATE" | python3 -c "import sys,json;print(json.load(sys.stdin)['code'])")"
OID=$(echo "$OCREATE" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
ONO1=$(echo "$OCREATE" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['orderNo'])")
echo "  orderId=$OID, orderNo=$ONO1"

# 幂等：相同幂等键再创建一次，应返回同一订单
OCREATE2=$(curl -s -X POST "$BASE/orders" -H "Authorization: Bearer $TOKEN_A" -H "Idempotency-Key: $IDEK" -H 'Content-Type: application/json' \
  -d "{\"customerName\":\"张三\",\"items\":[{\"productId\":$PID,\"quantity\":2}]}")
ONO2=$(echo "$OCREATE2" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['orderNo'])")
[ "$ONO1" = "$ONO2" ] && ok "幂等生效：重复幂等键返回同一订单 ($ONO1)" || no "幂等失效：两次返回不同订单 ($ONO1 vs $ONO2)"

# 状态机推进
st(){ # id action expected
  r=$(curl -s -X POST "$BASE/orders/$1/$2" -H "Authorization: Bearer $TOKEN_A")
  s=$(echo "$r" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['status'])" 2>/dev/null)
  [ "$s" = "$3" ] && ok "订单 $1 $2 -> $s" || no "订单 $1 $2 期望 $3 实际 $s"
}
st "$OID" confirm CONFIRMED
st "$OID" ship SHIPPED
st "$OID" complete COMPLETED

# 终态后再取消应失败
RCANCEL_BODY=$(curl -s -X POST "$BASE/orders/$OID/cancel" -H "Authorization: Bearer $TOKEN_A")
RCANCEL=$(echo "$RCANCEL_BODY" | python3 -c "import sys,json;print(json.load(sys.stdin)['code'])")
echo "  cancel-on-completed response: $RCANCEL_BODY"
[ "$RCANCEL" = "40904" ] && ok "终态订单取消被拒绝 (code $RCANCEL)" || no "终态订单取消返回 code $RCANCEL（期望 40904）"

echo "=============================================="
echo "[6] 跨租户隔离验证"
echo "=============================================="
# 租户B 查询租户A的订单 -> 业务码 40302（DB 层租户拦截；本系统错误统一返回 HTTP 200 + body code）
RISO=$(curl -s "$BASE/orders/$OID" -H "Authorization: Bearer $TOKEN_B" | python3 -c "import sys,json;print(json.load(sys.stdin).get('code'))")
[ "$RISO" = "40302" ] && ok "租户B 无法访问租户A订单 (body code $RISO 隔离生效)" || no "租户B 越权读到租户A订单 (返回 code $RISO)"
# 租户B 用租户A的商品ID下单（带幂等键以绕过幂等前置校验）-> 业务码 40301（商品不在本租户）
RB=$(curl -s -X POST "$BASE/orders" -H "Authorization: Bearer $TOKEN_B" -H "Idempotency-Key: x-tenant-b-$(date +%s)" -H 'Content-Type: application/json' \
  -d "{\"customerName\":\"李四\",\"items\":[{\"productId\":$PID,\"quantity\":1}]}" | python3 -c "import sys,json;print(json.load(sys.stdin).get('code'))")
[ "$RB" = "40301" ] && ok "租户B 不能下单租户A的商品 (body code $RB)" || no "租户B 越权使用租户A商品 (返回 code $RB)"
# 租户B 商品列表应不含 A 的商品
PB=$(curl -s "$BASE/products" -H "Authorization: Bearer $TOKEN_B" | python3 -c "import sys,json;d=json.load(sys.stdin)['data']['list'];print('hasA' if any(x['productCode']=='$PCODE' for x in d) else 'clean')")
[ "$PB" = "clean" ] && ok "租户B 商品列表不含租户A商品" || no "租户B 看到了租户A商品！"

echo "=============================================="
echo "[7] RabbitMQ 接线验证（order.created.queue 有发布记录）"
echo "=============================================="
MQ=$(curl -s -u orderflow:orderflow "http://127.0.0.1:15672/api/queues/%2F/order.created.queue")
echo "$MQ" | python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    stats=d.get('message_stats',{})
    pub=stats.get('publish',0); ack=stats.get('ack',0); deliver=stats.get('deliver',0)
    print('  order.created.queue: published=%s, delivered=%s, acked=%s' % (pub, deliver, ack))
    sys.exit(0 if pub and pub>0 else 1)
except Exception as e:
    print('  queue 查询失败:', e); sys.exit(1)
" \
  && ok "RabbitMQ 已发布并消费订单创建事件 (order.created.queue)" \
  || no "RabbitMQ 未检测到订单事件发布（队列不存在或无发布）"

echo "=============================================="
echo "结果: PASS=$pass FAIL=$fail"
echo "=============================================="
kill "$APP_PID" 2>/dev/null
[ "$fail" -eq 0 ] && echo "🎉 全部通过" || echo "⚠️ 存在失败项"
exit $fail
