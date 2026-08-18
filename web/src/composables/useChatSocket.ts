import { Client } from '@stomp/stompjs'
import type { ChatMessage } from '../types'

/**
 * WebSocket 只接收“有新消息”的实时通知；消息发送和历史加载仍走 REST，
 * 因此断线、刷新或离线都不会丢记录。
 */
export function connectChatSocket(tokenKey: string, onMessage: (message: ChatMessage) => void) {
  const token = localStorage.getItem(tokenKey)
  if (!token) return () => undefined

  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const client = new Client({
    brokerURL: `${protocol}://${window.location.host}/api/ws/chat`,
    connectHeaders: { Authorization: `Bearer ${token}` },
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => undefined,
    onConnect: () => {
      client.subscribe('/user/queue/chat', (frame) => {
        try { onMessage(JSON.parse(frame.body) as ChatMessage) } catch { /* 忽略无效消息 */ }
      })
    }
  })
  client.activate()
  return () => client.deactivate()
}
