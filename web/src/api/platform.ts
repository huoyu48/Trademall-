import http from './platformHttp'

export interface PlatformTokenResponse {
  token: string
  userId: number
  username: string
  tenantId: number
  roles: string[]
}

export interface PlatformOverview {
  tenants: number
  orders: number
  gmvCent: number
}

export interface TenantStat {
  id: number
  tenantCode: string
  tenantName: string
  status: number
  orderCount: number
  gmvCent: number
}

export interface CreatedTenant {
  id: number
  tenantCode: string
  tenantName: string
  status: number
}

export function login(username: string, password: string) {
  return http.post<PlatformTokenResponse>('/platform/auth/login', { username, password })
}

export function overview() {
  return http.get<PlatformOverview>('/platform/stats')
}

export function tenants() {
  return http.get<TenantStat[]>('/platform/tenants')
}

export function createTenant(data: { tenantCode: string; tenantName: string; adminUsername?: string; adminPassword?: string }) {
  return http.post<CreatedTenant>('/platform/tenants', data)
}

export function setTenantStatus(id: number, status: number) {
  return http.post<void>(`/platform/tenants/${id}/status`, null, { params: { status } })
}
