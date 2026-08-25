export const ORDER_STATUS: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  PENDING_PAYMENT: { label: '待付款', type: 'warning' },
  PENDING_MERCHANT_CONFIRMATION: { label: '待商家确认', type: 'primary' },
  PENDING_SHIPMENT: { label: '待发货', type: 'warning' },
  SHIPPED: { label: '已发货', type: 'info' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' },
  REFUNDED: { label: '已退款', type: 'success' }
}

// 状态机允许的流转动作
export const TRANSITIONS: Record<string, ('confirm' | 'ship' | 'complete')[]> = {
  PENDING_PAYMENT: [],
  PENDING_MERCHANT_CONFIRMATION: ['confirm'],
  PENDING_SHIPMENT: ['ship'],
  SHIPPED: ['complete'],
  COMPLETED: [],
  CANCELLED: [],
  REFUNDED: []
}

export const TRANSITION_LABEL: Record<string, string> = {
  confirm: '确认订单',
  ship: '发货',
  complete: '完成订单',
  cancel: '取消订单'
}
