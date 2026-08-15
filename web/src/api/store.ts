import http from './http'

export function pageStores(params: { page?: number; size?: number }) {
  return http.get<any>('/stores', { params })
}

export function listAllStores() {
  return http.get<any>('/stores/all')
}

export function createStore(data: {
  storeCode: string
  storeName: string
  province?: string
  city?: string
  address?: string
  status?: number
}) {
  return http.post<any>('/stores', data)
}

export function updateStore(
  id: number,
  data: { storeName?: string; province?: string; city?: string; address?: string; status?: number }
) {
  return http.put<any>(`/stores/${id}`, data)
}
