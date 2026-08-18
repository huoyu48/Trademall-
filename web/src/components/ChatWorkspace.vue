<template>
  <div class="chat-workspace" v-loading="loading">
    <aside class="conversation-panel">
      <div class="conversation-head">
        <div>
          <h2>{{ title }}</h2>
          <p>消息已保存，离线后可继续查看</p>
        </div>
        <el-button circle text @click="loadConversations"><el-icon><Refresh /></el-icon></el-button>
      </div>
      <el-empty v-if="!conversations.length" description="暂时没有咨询消息" :image-size="72" />
      <div v-else class="conversation-list">
        <button v-for="conversation in conversations" :key="conversation.id"
                class="conversation-item" :class="{ active: activeId === conversation.id }"
                @click="selectConversation(conversation.id)">
          <el-avatar class="peer-avatar">{{ conversation.peerName?.charAt(0) || '?' }}</el-avatar>
          <span class="conversation-main">
            <span class="conversation-top"><b>{{ conversation.peerName }}</b><time>{{ formatTime(conversation.lastMessageAt) }}</time></span>
            <span class="conversation-preview">{{ conversation.lastMessageContent || '开始咨询吧' }}</span>
          </span>
          <el-badge v-if="conversation.unreadCount > 0" :value="conversation.unreadCount" :max="99" />
        </button>
      </div>
    </aside>

    <section class="message-panel">
      <el-empty v-if="!activeConversation" description="从左侧选择一个会话" />
      <template v-else>
        <header class="message-head">
          <div><h3>{{ activeConversation.peerName }}</h3><span><i class="online-dot" /> 实时消息已连接</span></div>
        </header>
        <div ref="messageBox" class="message-list">
          <el-button v-if="history?.nextBeforeId" link type="primary" class="more-btn" @click="loadOlder">加载更早消息</el-button>
          <div v-for="message in messages" :key="message.id" class="message-row"
               :class="{ mine: message.senderType === selfType }">
            <div class="bubble">{{ message.content }}</div>
            <time>{{ formatFullTime(message.createdAt) }}</time>
          </div>
        </div>
        <footer class="message-input">
          <el-input v-model="draft" type="textarea" :rows="3" resize="none" maxlength="1000"
                    show-word-limit placeholder="输入消息，Enter 发送，Shift + Enter 换行"
                    @keydown.enter.exact.prevent="send" />
          <div class="send-row"><span>消息发送后将同步保存到会话历史</span><el-button type="primary" :loading="sending" @click="send">发送</el-button></div>
        </footer>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { connectChatSocket } from '../composables/useChatSocket'
import type { ChatConversation, ChatHistory, ChatMessage } from '../types'

interface ChatApi {
  list: () => Promise<ChatConversation[]>
  history: (id: number, beforeId?: number) => Promise<ChatHistory>
  send: (id: number, content: string) => Promise<ChatMessage>
  markRead: (id: number) => Promise<void>
}

const props = defineProps<{
  title: string
  selfType: 'CUSTOMER' | 'MERCHANT'
  tokenKey: string
  api: ChatApi
  initialConversationId?: number
}>()

const conversations = ref<ChatConversation[]>([])
const activeId = ref<number | null>(null)
const history = ref<ChatHistory | null>(null)
const messages = ref<ChatMessage[]>([])
const draft = ref('')
const loading = ref(false)
const sending = ref(false)
const messageBox = ref<HTMLElement>()
let disconnect = () => undefined

const activeConversation = computed(() => conversations.value.find((item) => item.id === activeId.value))

function formatTime(value?: string) {
  if (!value) return ''
  const date = new Date(value.replace(' ', 'T'))
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

function formatFullTime(value?: string) {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

async function loadConversations() {
  loading.value = true
  try {
    conversations.value = await props.api.list()
    if (!activeId.value && conversations.value.length) {
      const requested = props.initialConversationId
      await selectConversation(conversations.value.some((item) => item.id === requested) ? requested! : conversations.value[0].id)
    }
  } finally {
    loading.value = false
  }
}

async function selectConversation(id: number) {
  activeId.value = id
  history.value = await props.api.history(id)
  messages.value = history.value.list
  await props.api.markRead(id)
  const conversation = conversations.value.find((item) => item.id === id)
  if (conversation) conversation.unreadCount = 0
  await scrollToBottom()
}

async function loadOlder() {
  if (!activeId.value || !history.value?.nextBeforeId) return
  const older = await props.api.history(activeId.value, history.value.nextBeforeId)
  messages.value = [...older.list, ...messages.value]
  history.value.nextBeforeId = older.nextBeforeId
}

async function send() {
  if (!activeId.value || !draft.value.trim() || sending.value) return
  sending.value = true
  try {
    const message = await props.api.send(activeId.value, draft.value)
    appendIfAbsent(message)
    draft.value = ''
    await loadConversations()
  } catch {
    // Axios 拦截器已经展示错误信息。
  } finally {
    sending.value = false
  }
}

function appendIfAbsent(message: ChatMessage) {
  if (message.conversationId !== activeId.value || messages.value.some((item) => item.id === message.id)) return
  messages.value.push(message)
  scrollToBottom()
}

async function onRealtimeMessage(message: ChatMessage) {
  if (message.conversationId === activeId.value) {
    appendIfAbsent(message)
    if (message.senderType !== props.selfType && activeId.value) {
      await props.api.markRead(activeId.value)
    }
  }
  await loadConversations()
}

async function scrollToBottom() {
  await nextTick()
  if (messageBox.value) messageBox.value.scrollTop = messageBox.value.scrollHeight
}

onMounted(async () => {
  await loadConversations()
  disconnect = connectChatSocket(props.tokenKey, onRealtimeMessage)
})

onUnmounted(() => disconnect())
</script>

<style scoped>
.chat-workspace { min-height: 620px; display: grid; grid-template-columns: 320px 1fr; overflow: hidden; border-radius: 16px; background: #fff; box-shadow: var(--of-shadow); border: 1px solid var(--of-border); }
.conversation-panel { border-right: 1px solid var(--of-border); min-width: 0; }
.conversation-head { padding: 20px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid var(--of-border); }
.conversation-head h2 { margin: 0; font-size: 18px; color: var(--of-text); }.conversation-head p { margin: 5px 0 0; color: var(--of-text-3); font-size: 12px; }
.conversation-list { max-height: 570px; overflow-y: auto; }.conversation-item { width: 100%; border: 0; background: #fff; padding: 14px 16px; display: flex; text-align: left; gap: 10px; align-items: center; cursor: pointer; }.conversation-item:hover, .conversation-item.active { background: #f0fdfa; }.peer-avatar { flex: none; background: linear-gradient(135deg, #14b8a6, #4f46e5); color: #fff; font-weight: 700; }.conversation-main { min-width: 0; flex: 1; }.conversation-top { display: flex; justify-content: space-between; gap: 8px; align-items: center; }.conversation-top b { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--of-text); }.conversation-top time, .conversation-preview { color: var(--of-text-3); font-size: 12px; }.conversation-preview { display: block; margin-top: 5px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.message-panel { min-width: 0; display: flex; flex-direction: column; }.message-head { padding: 17px 22px; border-bottom: 1px solid var(--of-border); }.message-head h3 { margin: 0; color: var(--of-text); font-size: 16px; }.message-head span { display: inline-flex; align-items: center; gap: 5px; margin-top: 5px; font-size: 12px; color: #0f766e; }.online-dot { width: 6px; height: 6px; border-radius: 50%; background: #10b981; }
.message-list { flex: 1; min-height: 360px; max-height: 460px; overflow-y: auto; padding: 20px 24px; background: #f8fafc; }.more-btn { display: block; margin: 0 auto 14px; }.message-row { display: flex; flex-direction: column; align-items: flex-start; margin-bottom: 15px; }.message-row.mine { align-items: flex-end; }.bubble { max-width: min(75%, 520px); padding: 10px 13px; border-radius: 4px 14px 14px 14px; background: #fff; color: #334155; box-shadow: 0 1px 3px rgba(15, 23, 42, .08); white-space: pre-wrap; word-break: break-word; }.mine .bubble { color: #fff; background: linear-gradient(135deg, #0f766e, #14b8a6); border-radius: 14px 4px 14px 14px; }.message-row time { margin-top: 5px; font-size: 11px; color: #94a3b8; }
.message-input { padding: 14px 20px; border-top: 1px solid var(--of-border); }.send-row { margin-top: 10px; display: flex; justify-content: space-between; align-items: center; color: var(--of-text-3); font-size: 12px; }
@media (max-width: 760px) { .chat-workspace { grid-template-columns: 1fr; }.conversation-panel { border-right: 0; border-bottom: 1px solid var(--of-border); }.conversation-list { max-height: 180px; }.message-list { min-height: 300px; } }
</style>
