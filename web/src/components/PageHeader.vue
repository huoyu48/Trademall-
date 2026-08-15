<template>
  <header class="page-header">
    <div class="ph-text">
      <h1 class="ph-title">{{ title }}</h1>
      <p v-if="subtitle" class="ph-sub">{{ subtitle }}</p>
    </div>
    <div class="ph-actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 20px;
  gap: 16px;
  flex-wrap: wrap;
}
.ph-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.2px;
  color: var(--of-text);
}
.ph-sub {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--of-text-2);
}
.ph-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style>
