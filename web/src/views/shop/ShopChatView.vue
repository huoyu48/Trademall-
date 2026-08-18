<template>
  <ChatWorkspace title="咨询消息" self-type="CUSTOMER" token-key="of_customer_token"
                 :api="customerChatApi" :initial-conversation-id="initialConversationId" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ChatWorkspace from '../../components/ChatWorkspace.vue'
import { customerChatApi } from '../../api/chat'

const route = useRoute()
const initialConversationId = computed(() => {
  const id = Number(route.query.conversationId)
  return Number.isInteger(id) && id > 0 ? id : undefined
})
</script>
