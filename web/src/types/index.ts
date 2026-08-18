export interface Product {
  id: number
  productCode: string
  productName: string
  unitPriceCent: number
  status: number
  categoryId?: number
  categoryName?: string
  storeId?: number
  storeName?: string
  sales?: number
  createdAt?: string
  updatedAt?: string
}

export interface Category {
  id: number
  categoryCode: string
  categoryName: string
  parentId?: number
  sort?: number
  status?: number
}

export interface Inventory {
  productId: number
  productName: string
  physicalQuantity: number
  reservedQuantity: number
  availableQuantity: number
}

export interface OrderItem {
  productId: number
  productCode: string
  productName: string
  unitPriceCent: number
  quantity: number
  lineAmountCent: number
}

export interface Order {
  id: number
  orderNo: string
  customerName: string
  status: string
  totalAmountCent: number
  createdAt?: string
  items: OrderItem[]
}

export interface PageResult<T> {
  total: number
  page: number
  size: number
  list: T[]
}

export interface ChatConversation {
  id: number
  tenantId: number
  peerName: string
  lastMessageContent?: string
  lastMessageAt?: string
  unreadCount: number
}

export interface ChatMessage {
  id: number
  conversationId: number
  senderType: 'CUSTOMER' | 'MERCHANT'
  senderId: number
  content: string
  createdAt: string
  readAt?: string
}

export interface ChatHistory {
  list: ChatMessage[]
  nextBeforeId?: number
}
