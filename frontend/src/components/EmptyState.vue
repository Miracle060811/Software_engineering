<template>
  <div class="empty-state" :class="{ 'empty-compact': compact }">
    <div class="empty-icon-wrap">
      <slot name="icon">
        <el-icon :size="compact ? 48 : 72" color="var(--tm-primary)">
          <component :is="iconComponent" />
        </el-icon>
      </slot>
    </div>
    <h3 class="empty-title" v-if="title">{{ title }}</h3>
    <p class="empty-desc" v-if="description">{{ description }}</p>
    <div class="empty-action" v-if="actionText || $slots.action">
      <slot name="action">
        <el-button type="primary" round @click="$emit('action')">{{ actionText }}</el-button>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Document, Search, List, Bell, Picture, Tickets } from '@element-plus/icons-vue'

const props = defineProps({
  title: { type: String, default: '暂无内容' },
  description: { type: String, default: '' },
  icon: { type: String, default: 'document' },
  actionText: { type: String, default: '' },
  compact: { type: Boolean, default: false }
})

defineEmits(['action'])

const iconMap = {
  document: Document,
  search: Search,
  list: List,
  bell: Bell,
  picture: Picture,
  tickets: Tickets,
}

const iconComponent = computed(() => iconMap[props.icon] || iconMap.document)
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
}

.empty-state.empty-compact {
  padding: 32px 16px;
}

.empty-icon-wrap {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: var(--tm-paper-3);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  color: var(--tm-primary);
}

.empty-compact .empty-icon-wrap {
  width: 72px;
  height: 72px;
  margin-bottom: 14px;
}

.empty-title {
  font-size: 18px;
  font-weight: 650;
  color: var(--tm-ink);
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 14px;
  color: var(--tm-muted);
  max-width: 320px;
  line-height: 1.6;
}

.empty-action {
  margin-top: 20px;
}
</style>
