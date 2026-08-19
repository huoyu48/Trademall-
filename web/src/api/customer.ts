import http from './customerHttp'
import type { Product, Order, OrderPricing, PageResult, Category } from '../types'

export interface CustomerTokenResponse {
  token: string
  userId: number
  username: string
  tenantId: number
  roles: string[]
}

export interface CustomerRegisterRequest {
  username: string
  password: string
  confirmPassword: string
  nickname?: string
  phone?: string
}

export function login(username: string, password: string) {
  return http.post<CustomerTokenResponse>('/customer/auth/login', { username, password })
}

export function register(request: CustomerRegisterRequest) {
  return http.post<CustomerTokenResponse>('/customer/auth/register', request)
}

export function products(page = 1, size = 12, categoryId?: number, keyword?: string) {
  return http.get<PageResult<Product>>('/customer/products', { params: { page, size, categoryId, keyword } })
}

export function categories() {
  return http.get<Category[]>('/customer/categories')
}

export function productDetail(id: number) {
  return http.get<Product>(`/customer/products/${id}`)
}

export function createOrder(items: { productId: number; quantity: number }[], promoCode?: string, idempotencyKey?: string) {
  return http.post<Order>('/customer/orders', { items, promoCode }, {
    headers: { 'Idempotency-Key': idempotencyKey || crypto.randomUUID() }
  })
}

export function previewOrder(items: { productId: number; quantity: number }[], promoCode?: string) {
  return http.post<OrderPricing>('/customer/orders/preview', { items, promoCode })
}

export function myOrders() {
  return http.get<Order[]>('/customer/orders')
}
