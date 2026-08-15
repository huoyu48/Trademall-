import http from './http'

export function pageRefunds(params: { page?: number; size?: number }) {
  return http.get<any>('/refunds', { params })
}

export function getRefund(id: number) {
  return http.get<any>(`/refunds/${id}`)
}

export function applyRefund(orderId: number, reason?: string) {
  return http.post<any>('/refunds/apply', {}, { params: { orderId, reason } })
}

export function approveRefund(id: number) {
  return http.post<any>(`/refunds/${id}/approve`)
}

export function rejectRefund(id: number) {
  return http.post<any>(`/refunds/${id}/reject`)
}
