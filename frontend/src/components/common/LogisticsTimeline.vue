<template>
  <div class="logistics-timeline">
    <div class="lg-meta">
      <div class="lg-meta-row">
        <span class="lg-label">承运商</span>
        <span class="lg-value">{{ logistics.company || '-' }}</span>
      </div>
      <div class="lg-meta-row">
        <span class="lg-label">运单号</span>
        <span class="lg-value mono">{{ logistics.trackingNumber || '-' }}</span>
      </div>
      <div class="lg-meta-row">
        <span class="lg-label">当前状态</span>
        <span class="lg-status" :class="'lg-status-' + statusClass">{{ statusText }}</span>
      </div>
    </div>
    <div v-if="reversedTraces.length > 0" class="lg-traces">
      <div
        v-for="(trace, index) in reversedTraces"
        :key="index"
        class="lg-trace"
        :class="{ latest: index === 0 }"
      >
        <span class="lg-dot"></span>
        <div class="lg-trace-body">
          <div class="lg-trace-desc">{{ trace.description }}</div>
          <div class="lg-trace-time">{{ formatTime(trace.traceTime) }}</div>
        </div>
      </div>
    </div>
    <div v-else class="lg-empty">暂无轨迹信息</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  logistics: {
    type: Object,
    required: true
  }
})

const STATUS_TEXT = {
  SHIPPED: '已发货',
  IN_TRANSIT: '运输中',
  DELIVERING: '派送中',
  SIGNED: '已签收'
}

const statusText = computed(() => STATUS_TEXT[props.logistics.status] || props.logistics.status || '-')

const statusClass = computed(() => (props.logistics.status || '').toLowerCase())

// 后端按时间正序返回，时间线从最新节点开始展示
const reversedTraces = computed(() => [...(props.logistics.traces || [])].reverse())

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  if (isNaN(d.getTime())) return String(time)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}
</script>

<style scoped>
.logistics-timeline {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.lg-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
}

.lg-meta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.lg-label {
  color: var(--color-text-tertiary);
  width: 56px;
  flex-shrink: 0;
}

.lg-value {
  color: var(--color-text);
}

.mono {
  font-family: var(--font-mono, ui-monospace, 'SF Mono', Consolas, monospace);
}

.lg-status {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background: var(--color-bg-overlay);
  color: var(--color-text-secondary);
}

.lg-status-shipped {
  background: var(--color-warning-light, #FAEEDA);
  color: var(--color-warning, #854F0B);
}

.lg-status-in_transit,
.lg-status-delivering {
  background: var(--color-success-light, #E1F5EE);
  color: var(--color-primary);
}

.lg-status-signed {
  background: var(--color-bg-overlay);
  color: var(--color-text-tertiary);
}

.lg-traces {
  display: flex;
  flex-direction: column;
}

.lg-trace {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  position: relative;
}

.lg-trace:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 14px;
  bottom: 0;
  width: 1px;
  background: var(--color-border);
}

.lg-dot {
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: var(--color-border);
  margin-top: 3px;
  flex-shrink: 0;
}

.lg-trace.latest .lg-dot {
  background: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-success-light, #E1F5EE);
}

.lg-trace-desc {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.lg-trace.latest .lg-trace-desc {
  color: var(--color-text);
  font-weight: 600;
}

.lg-trace-time {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.lg-empty {
  text-align: center;
  color: var(--color-text-tertiary);
  font-size: 13px;
  padding: 16px 0;
}
</style>
