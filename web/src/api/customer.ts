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

export interface MockCheckout {
  paymentNo: string
  amountCent: number
  qrCodeImage: string
}

export function createMockCheckout(id: number) {
  return http.post<MockCheckout>(`/customer/orders/${id}/payments/mock`)
}

export interface PaymentStatus {
  orderStatus: string
  paid: boolean
}

export function paymentStatus(id: number) {
  return http.get<PaymentStatus>(`/customer/orders/${id}/payments/mock/status`)
}

export interface MockPaymentPage {
  paymentNo: string
  orderNo: string
  amountCent: number
  status: string
  paid: boolean
  expiresAt?: string
}

/** 手机扫码后的公开模拟收银台接口，不要求顾客登录。 */
export function mockPaymentPage(token: string) {
  return http.get<MockPaymentPage>('/payments/mock/checkout', { params: { token } })
}

export function confirmMockPayment(token: string) {
  return http.post<MockPaymentPage>('/payments/mock/checkout/confirm', undefined, { params: { token } })
}

export function cancelPendingPaymentOrder(id: number) {
  return http.post<Order>(`/customer/orders/${id}/cancel`)
}

export function applyCustomerRefund(id: number, reason?: string) {
  return http.post<any>(`/customer/orders/${id}/refunds`, undefined, { params: { reason } })
}

export function applyCustomerReturn(id: number, reason?: string) {
  return http.post<any>(`/customer/orders/${id}/returns`, undefined, { params: { reason } })
}

export function customerRefunds() {
  return http.get<any[]>('/customer/refunds')
}

export function submitReturnShipment(id: number, trackingNo: string) {
  return http.post<any>(`/customer/refunds/${id}/return-shipment`, undefined, { params: { trackingNo } })
}

export function myOrders() {
  return http.get<Order[]>('/customer/orders')
}
