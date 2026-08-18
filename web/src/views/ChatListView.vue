<template>
  <ChatWorkspace title="客户咨询" self-type="MERCHANT" token-key="of_token" :api="merchantChatApi" />
</template>

<script setup lang="ts">
import ChatWorkspace from '../components/ChatWorkspace.vue'
import { merchantChatApi } from '../api/chat'
</script>
