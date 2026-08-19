export const ORDER_STATUS: Record<string, { label: string; type: 'primary' | 'success' | 'warning' | 'info' | 'danger' }> = {
  PENDING_PAYMENT: { label: '待付款', type: 'warning' },
  PAID: { label: '已付款', type: 'primary' },
  CREATED: { label: '已创建', type: 'primary' },
  CONFIRMED: { label: '已确认', type: 'warning' },
  SHIPPED: { label: '已发货', type: 'info' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'danger' },
  REFUNDING: { label: '退款中', type: 'warning' },
  REFUNDED: { label: '已退款', type: 'success' }
}

// 状态机允许的流转动作
export const TRANSITIONS: Record<string, ('confirm' | 'ship' | 'complete' | 'cancel' | 'refund')[]> = {
  PENDING_PAYMENT: [],
  PAID: ['confirm', 'refund'],
  CREATED: ['confirm', 'cancel'],
  CONFIRMED: ['ship', 'refund'],
  SHIPPED: ['complete', 'refund'],
  COMPLETED: ['refund'],
  CANCELLED: []
}

export const TRANSITION_LABEL: Record<string, string> = {
  confirm: '确认订单',
  ship: '发货',
  complete: '完成订单',
  cancel: '取消订单',
  refund: '发起模拟退款'
}
