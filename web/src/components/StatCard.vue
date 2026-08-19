<template>
  <div class="stat-card of-fade-up" :class="{ 'is-clickable': clickable }" :style="{ animationDelay: delay + 'ms' }"
       :role="clickable ? 'button' : undefined" :tabindex="clickable ? 0 : undefined"
       @click="clickable && emit('click')" @keydown.enter="clickable && emit('click')">
    <div class="stat-icon" :style="{ background: tint, color: color }">
      <el-icon :size="22"><component :is="icon" /></el-icon>
    </div>
    <div class="stat-body">
      <div class="stat-label">{{ title }}</div>
      <div class="stat-value">{{ value }}</div>
      <div v-if="trend || footer" class="stat-foot">
        <span v-if="trend" class="trend" :class="trend.dir">
          <el-icon v-if="trend.dir === 'up'" :size="13"><CaretTop /></el-icon>
          <el-icon v-else-if="trend.dir === 'down'" :size="13"><CaretBottom /></el-icon>
          {{ trend.text }}
        </span>
        <span class="stat-sub">{{ footer }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  value: string | number
  icon: string
  color?: string
  tint?: string
  delay?: number
  trend?: { dir: 'up' | 'down' | 'flat'; text: string }
  footer?: string
  clickable?: boolean
}>()

const emit = defineEmits<{ click: [] }>()
</script>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--of-surface);
  border-radius: var(--of-radius);
  padding: 20px 22px;
  box-shadow: var(--of-shadow);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--of-shadow-lg);
}
.stat-card.is-clickable { cursor: pointer; }
.stat-card.is-clickable:focus-visible { outline: 3px solid rgba(79, 70, 229, 0.35); outline-offset: 3px; }
.stat-icon {
  flex: none;
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-body { min-width: 0; }
.stat-label {
  font-size: 13px;
  color: var(--of-text-2);
  margin-bottom: 6px;
}
.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--of-text);
}
.stat-foot {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.trend {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-weight: 600;
  padding: 1px 7px;
  border-radius: 20px;
}
.trend.up { color: var(--of-success); background: rgba(16, 185, 129, 0.12); }
.trend.down { color: var(--of-danger); background: rgba(239, 68, 68, 0.12); }
.trend.flat { color: var(--of-text-2); background: rgba(107, 114, 128, 0.12); }
.stat-sub { color: var(--of-text-3); }
</style>
