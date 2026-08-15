import http from './http'

export function pageOrders(params: { page?: number; size?: number; orderNo?: string; status?: string }) {
  return http.get<any>('/orders', { params })
}

export function getOrder(id: number) {
  return http.get<any>(`/orders/${id}`)
}

export function createOrder(data: { customerName: string; promoCode?: string; items: { productId: number; quantity: number }[] }, key: string) {
  return http.post<any>('/orders', data, { headers: { 'Idempotency-Key': key } })
}

export function transitionOrder(id: number, action: 'confirm' | 'ship' | 'complete' | 'cancel') {
  return http.post<any>(`/orders/${id}/${action}`)
}

export function getOrderStats() {
  return http.get<any>('/orders/stats')
}

export function getOrderHistory(id: number) {
  return http.get<any[]>(`/orders/${id}/history`)
}
