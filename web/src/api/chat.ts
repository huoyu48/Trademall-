import http from './http'
import customerHttp from './customerHttp'
import type { ChatConversation, ChatHistory, ChatMessage } from '../types'

// 商家后台接口
export const merchantChatApi = {
  list: () => http.get<ChatConversation[]>('/chat/conversations'),
  history: (id: number, beforeId?: number) => http.get<ChatHistory>(`/chat/conversations/${id}/messages`, { params: { beforeId, size: 50 } }),
  send: (id: number, content: string) => http.post<ChatMessage>(`/chat/conversations/${id}/messages`, { content }),
  markRead: (id: number) => http.post<void>(`/chat/conversations/${id}/read`)
}

// 顾客商城接口；开启会话只接收商品 ID，由服务端反查商家租户。
export const customerChatApi = {
  open: (productId: number) => customerHttp.post<ChatConversation>('/customer/chat/conversations', { productId }),
  list: () => customerHttp.get<ChatConversation[]>('/customer/chat/conversations'),
  history: (id: number, beforeId?: number) => customerHttp.get<ChatHistory>(`/customer/chat/conversations/${id}/messages`, { params: { beforeId, size: 50 } }),
  send: (id: number, content: string) => customerHttp.post<ChatMessage>(`/customer/chat/conversations/${id}/messages`, { content }),
  markRead: (id: number) => customerHttp.post<void>(`/customer/chat/conversations/${id}/read`)
}
