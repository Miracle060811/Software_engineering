<template>
  <div class="empty-state" :class="{ 'empty-compact': compact }">
    <div class="empty-icon-wrap">
      <slot name="icon">
        <el-icon :size="compact ? 48 : 72" color="#A0AEC0">
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
  animation: fadeInScale 0.4s ease;
}

.empty-state.empty-compact {
  padding: 32px 16px;
}

.empty-icon-wrap {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: linear-gradient(135deg, #F7F8FA 0%, #EEF1F5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
  transition: all 0.3s ease;
}

.empty-compact .empty-icon-wrap {
  width: 72px;
  height: 72px;
  margin-bottom: 14px;
}

.empty-icon-wrap:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
}

.empty-title {
  font-size: 18px;
  font-weight: 700;
  color: #3D3D5C;
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 14px;
  color: #A0A0B8;
  max-width: 320px;
  line-height: 1.6;
}

.empty-action {
  margin-top: 20px;
}
</style>
