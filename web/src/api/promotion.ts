import http from './http'

export function pagePromotions(params: { page?: number; size?: number }) {
  return http.get<any>('/promotions', { params })
}

export function listAllPromotions() {
  return http.get<any>('/promotions/all')
}

export function createPromotion(data: {
  promoCode: string
  promoName: string
  promoType: string
  thresholdCent: number
  discountAmountCent: number
  beginAt?: string
  endAt?: string
  status?: number
}) {
  return http.post<any>('/promotions', data)
}

export function updatePromotion(
  id: number,
  data: {
    promoName?: string
    promoType?: string
    thresholdCent?: number
    discountAmountCent?: number
    beginAt?: string
    endAt?: string
    status?: number
  }
) {
  return http.put<any>(`/promotions/${id}`, data)
}
