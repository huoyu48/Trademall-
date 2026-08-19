import http from './http'

export function listInventory() {
  return http.get<any[]>('/inventories')
}

export function adjustInventory(data: { productId: number; changeQuantity: number; reason?: string }) {
  return http.post<any>('/inventories/adjustments', data)
}

export function lowStockInventory() {
  return http.get<any[]>('/inventories/low-stock')
}
