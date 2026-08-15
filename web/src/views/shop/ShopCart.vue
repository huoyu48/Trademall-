<template>
  <div>
    <PageHeader title="购物车" subtitle="不同商家的商品将拆分为独立订单结算" />

    <div v-if="cart.items.length === 0" class="empty">
      <el-empty description="购物车还是空的，去挑点好物吧">
        <el-button type="primary" @click="$router.push('/shop')">去逛逛</el-button>
      </el-empty>
    </div>

    <div v-else class="cart-wrap">
      <div class="cart-left">
        <div class="cart-toolbar">
          <el-checkbox :model-value="cart.allChecked" @change="(v: boolean) => cart.checkAll(v)">全选</el-checkbox>
          <el-button link type="danger" :disabled="cart.checkedCount === 0" @click="batchRemove">
            <el-icon><Delete /></el-icon> 删除选中
          </el-button>
        </div>

        <!-- 按商家分组 -->
        <div v-for="g in allGroups" :key="g.storeId" class="store-group" :class="{ dim: !groupChecked(g) }">
          <div class="group-head">
            <el-checkbox :model-value="groupChecked(g)" @change="(v: boolean) => cart.toggleStore(g.storeId, v)" />
            <el-icon color="#d97706" :size="16"><Shop /></el-icon>
            <span class="group-name">{{ g.storeName }}</span>
            <el-tag size="small" type="warning" effect="plain">本店优惠</el-tag>
            <span class="group-sub">小计 ¥ {{ centToYuan(g.totalCent) }}</span>
            <el-button link type="danger" size="small" @click="cart.removeStore(g.storeId)">删除整组</el-button>
          </div>

          <div v-for="it in g.items" :key="it.productId" class="cart-item" :class="{ dim: !it.checked }">
            <el-checkbox :model-value="it.checked" @change="() => cart.toggle(it.productId)" />
            <div class="ci-cover" @click="$router.push(`/shop/product/${it.productId}`)">{{ it.productName.charAt(0) }}</div>
            <div class="ci-name" @click="$router.push(`/shop/product/${it.productId}`)">{{ it.productName }}</div>
            <div class="ci-price">¥ {{ centToYuan(it.unitPriceCent) }}</div>
            <el-input-number :model-value="it.quantity" :min="1" :max="99" size="small"
                             @update:model-value="(v: number) => cart.setQty(it.productId, v)" />
            <div class="ci-line">¥ {{ centToYuan(it.unitPriceCent * it.quantity) }}</div>
            <el-button link type="danger" @click="cart.remove(it.productId)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <!-- 结算栏 -->
      <div class="cart-summary">
        <div class="sum-row"><span>已选件数</span><b>{{ cart.checkedCount }}</b></div>
        <div class="sum-row"><span>涉及商家</span><b>{{ cart.checkedGroups.length }} 家</b></div>
        <div class="sum-row"><span>商品合计</span><b>¥ {{ centToYuan(cart.checkedTotalCent) }}</b></div>
        <div class="sum-row sum-total"><span>应付金额</span><b class="sum-price">¥ {{ centToYuan(cart.checkedTotalCent) }}</b></div>
        <el-button type="primary" size="large" class="submit-btn" :loading="submitting"
                   :disabled="cart.checkedCount === 0" @click="checkout">
          提交订单{{ cart.checkedGroups.length > 1 ? `（拆 ${cart.checkedGroups.length} 单）` : '' }}
        </el-button>
        <p class="sum-tip">跨商家自动拆单，每笔订单独立按商家优惠结算</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '../../components/PageHeader.vue'
import { useCartStore, type StoreGroup } from '../../stores/cart'
import { createOrder } from '../../api/customer'
import { centToYuan } from '../../utils/money'

const cart = useCartStore()
const router = useRouter()
const submitting = ref(false)

/** 展示所有分组（含未勾选的，便于整组展示；结算只看 checkedGroups） */
const allGroups = computed<StoreGroup[]>(() => {
  const map = new Map<number, StoreGroup>()
  for (const i of cart.items) {
    const key = i.storeId || 0
    if (!map.has(key)) {
      map.set(key, { storeId: key, storeName: i.storeName || '官方直营', items: [], totalCent: 0, count: 0 })
    }
    const g = map.get(key)!
    g.items.push(i)
    g.totalCent += i.unitPriceCent * i.quantity
    g.count += i.quantity
  }
  return Array.from(map.values())
})

function groupChecked(g: StoreGroup) {
  return g.items.length > 0 && g.items.every((i) => i.checked)
}

async function checkout() {
  const groups = cart.checkedGroups
  if (groups.length === 0) return
  submitting.value = true
  try {
    // 按商家逐个下单（拆单）
    for (const g of groups) {
      const items = g.items.map((i) => ({ productId: i.productId, quantity: i.quantity }))
      await createOrder(items)
      g.items.forEach((i) => cart.remove(i.productId))
    }
    ElMessage.success(`已按商家拆分为 ${groups.length} 笔订单`)
    router.push('/shop/orders')
  } finally {
    submitting.value = false
  }
}

async function batchRemove() {
  try {
    await ElMessageBox.confirm('确认删除选中的商品？', '提示', { type: 'warning' })
  } catch {
    return
  }
  cart.removeChecked()
  ElMessage.success('已删除')
}
</script>

<style scoped>
.empty { padding: 60px 0; }
.cart-wrap { display: grid; grid-template-columns: 1fr 320px; gap: 20px; align-items: start; }

.cart-left { display: flex; flex-direction: column; gap: 16px; }
.cart-toolbar {
  display: flex; align-items: center; justify-content: space-between; padding: 12px 20px;
  background: var(--of-surface); border-radius: 12px; box-shadow: var(--of-shadow);
}

.store-group {
  background: var(--of-surface); border-radius: 14px; box-shadow: var(--of-shadow); padding: 8px 20px 16px;
  transition: opacity 0.2s;
}
.store-group.dim { opacity: 0.55; }
.group-head {
  display: flex; align-items: center; gap: 8px; padding: 12px 0;
  border-bottom: 1px solid var(--of-border);
}
.group-name { font-weight: 700; font-size: 15px; color: var(--of-text); }
.group-sub { margin-left: auto; font-size: 13px; color: #d97706; font-weight: 600; }

.cart-item {
  display: grid; grid-template-columns: 20px 48px 1fr 100px 130px 120px 40px;
  align-items: center; gap: 12px; padding: 14px 0; border-bottom: 1px solid var(--of-border);
  transition: opacity 0.2s;
}
.cart-item:last-child { border-bottom: none; }
.cart-item.dim { opacity: 0.45; }
.ci-cover {
  width: 44px; height: 44px; border-radius: 10px; color: #fff; font-weight: 700; font-size: 20px;
  background: linear-gradient(135deg, #14b8a6, #0d9488); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}
.ci-name { font-size: 14px; font-weight: 600; color: var(--of-text); cursor: pointer; }
.ci-price { font-size: 14px; color: var(--of-text-2); }
.ci-line { font-size: 15px; font-weight: 700; color: #ef4444; text-align: right; }

.cart-summary {
  background: var(--of-surface); border-radius: 14px; box-shadow: var(--of-shadow); padding: 22px;
  position: sticky; top: 84px;
}
.sum-row { display: flex; justify-content: space-between; margin-bottom: 14px; font-size: 14px; color: var(--of-text-2); }
.sum-row b { color: var(--of-text); }
.sum-total { border-top: 1px dashed var(--of-border); padding-top: 14px; font-size: 15px; }
.sum-price { color: #ef4444; font-size: 24px; }
.submit-btn { width: 100%; margin-top: 6px; font-weight: 600; }
.sum-tip { margin: 12px 0 0; font-size: 12px; color: var(--of-text-3); text-align: center; }

@media (max-width: 860px) { .cart-wrap { grid-template-columns: 1fr; } }
</style>
