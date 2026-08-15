import http from './http'

export function pageCategories(params: { page?: number; size?: number }) {
  return http.get<any>('/categories', { params })
}

export function listAllCategories() {
  return http.get<any>('/categories/all')
}

export function createCategory(data: {
  categoryCode: string
  categoryName: string
  parentId?: number
  sort?: number
  status?: number
}) {
  return http.post<any>('/categories', data)
}

export function updateCategory(
  id: number,
  data: { categoryName?: string; parentId?: number; sort?: number; status?: number }
) {
  return http.put<any>(`/categories/${id}`, data)
}
