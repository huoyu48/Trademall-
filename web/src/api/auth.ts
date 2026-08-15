import http from './http'

export interface TokenResponse {
  token: string
  tokenType?: string
  expiresInMinutes?: number
  userId: number
  username: string
  tenantId: number
  tenantName?: string
  roles: string[]
}

export function login(username: string, password: string) {
  return http.post<TokenResponse>('/auth/login', { username, password })
}

export function me() {
  return http.get<any>('/auth/me')
}
