import { defineStore } from 'pinia'
import type { Product } from '../types'

export interface CartItem {
  productId: number
  productName: string
  unitPriceCent: number
  quantity: number
  checked: boolean
  storeId?: number
  storeName?: string
}

export interface StoreGroup {
  storeId: number
  storeName: string
  items: CartItem[]
  totalCent: number
  count: number
}

function loadItems(): CartItem[] {
  const raw = JSON.parse(localStorage.getItem('of_cart') || '[]') as CartItem[]
  // 兼容旧数据（无 checked/storeName 字段）
  return raw.map((i) => ({ ...i, checked: i.checked !== false, storeName: i.storeName || '官方直营' }))
}

/** 本地购物车：结算时按商家（租户）拆单，每个商家一个订单。 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    items: loadItems()
  }),
  getters: {
    count: (s) => s.items.reduce((n, i) => n + i.quantity, 0),
    totalCent: (s) => s.items.reduce((n, i) => n + i.unitPriceCent * i.quantity, 0),
    checkedCount: (s) => s.items.filter((i) => i.checked).reduce((n, i) => n + i.quantity, 0),
    checkedTotalCent: (s) => s.items.filter((i) => i.checked).reduce((n, i) => n + i.unitPriceCent * i.quantity, 0),
    allChecked: (s) => s.items.length > 0 && s.items.every((i) => i.checked),
    /**
     * 按商家分组的已勾选商品（用于购物车分组展示 + 拆单结算）。
     * storeId 相同的商品归为一组，一个组对应一个订单。
     */
    checkedGroups: (s): StoreGroup[] => {
      const map = new Map<number, StoreGroup>()
      for (const i of s.items) {
        if (!i.checked) continue
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
    }
  },
  actions: {
    add(p: Product, qty = 1) {
      const found = this.items.find((i) => i.productId === p.id)
      if (found) found.quantity += qty
      else this.items.push({
        productId: p.id, productName: p.productName, unitPriceCent: p.unitPriceCent,
        quantity: qty, checked: true, storeId: p.storeId, storeName: p.storeName
      })
      this.persist()
    },
    setQty(productId: number, qty: number) {
      const found = this.items.find((i) => i.productId === productId)
      if (found) {
        found.quantity = qty
        if (qty <= 0) this.remove(productId)
      }
      this.persist()
    },
    toggle(productId: number) {
      const found = this.items.find((i) => i.productId === productId)
      if (found) found.checked = !found.checked
      this.persist()
    },
    /** 按商家整组勾选/取消 */
    toggleStore(storeId: number, checked: boolean) {
      this.items.forEach((i) => {
        if ((i.storeId || 0) === storeId) i.checked = checked
      })
      this.persist()
    },
    checkAll(checked: boolean) {
      this.items.forEach((i) => (i.checked = checked))
      this.persist()
    },
    remove(productId: number) {
      this.items = this.items.filter((i) => i.productId !== productId)
      this.persist()
    },
    removeChecked() {
      this.items = this.items.filter((i) => !i.checked)
      this.persist()
    },
    /** 按商家移除整组 */
    removeStore(storeId: number) {
      this.items = this.items.filter((i) => (i.storeId || 0) !== storeId)
      this.persist()
    },
    clear() {
      this.items = []
      this.persist()
    },
    persist() {
      localStorage.setItem('of_cart', JSON.stringify(this.items))
    }
  }
})
