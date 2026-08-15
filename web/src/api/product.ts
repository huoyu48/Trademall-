import http from './http'

export function pageProducts(params: {
  page?: number
  size?: number
  productCode?: string
  productName?: string
  status?: number
}) {
  return http.get<any>('/products', { params })
}

export function getProduct(id: number) {
  return http.get<any>(`/products/${id}`)
}

export function createProduct(data: {
  productCode: string
  productName: string
  unitPriceCent: number
  status?: number
  remark?: string
}) {
  return http.post<any>('/products', data)
}

export function updateProduct(
  id: number,
  data: { productCode?: string; productName?: string; unitPriceCent?: number; status?: number; remark?: string }
) {
  return http.put<any>(`/products/${id}`, data)
}
