<template>
  <div class="skeleton-box" :style="containerStyle">
    <!-- 卡片骨架 -->
    <template v-if="type === 'card'">
      <div class="sk-card" v-for="i in count" :key="i">
        <div class="sk-card-img sk-shimmer"></div>
        <div class="sk-card-body">
          <div class="sk-line sk-line-title sk-shimmer"></div>
          <div class="sk-line sk-line-text sk-shimmer"></div>
          <div class="sk-line sk-line-short sk-shimmer"></div>
        </div>
      </div>
    </template>

    <!-- 列表骨架 -->
    <template v-else-if="type === 'list'">
      <div class="sk-list-item" v-for="i in count" :key="i">
        <div class="sk-circle sk-shimmer"></div>
        <div class="sk-list-content">
          <div class="sk-line sk-line-title sk-shimmer"></div>
          <div class="sk-line sk-line-text sk-shimmer"></div>
        </div>
        <div class="sk-list-extra sk-shimmer"></div>
      </div>
    </template>

    <!-- 表格骨架 -->
    <template v-else-if="type === 'table'">
      <div class="sk-table" v-for="i in count" :key="i">
        <div class="sk-line sk-shimmer" style="height: 18px; flex: 1;"></div>
        <div class="sk-line sk-shimmer" style="height: 18px; flex: 1.5;"></div>
        <div class="sk-line sk-shimmer" style="height: 18px; flex: 0.8;"></div>
        <div class="sk-line sk-shimmer" style="height: 18px; flex: 0.6;"></div>
      </div>
    </template>

    <!-- 详情骨架 -->
    <template v-else-if="type === 'detail'">
      <div class="sk-detail-img sk-shimmer"></div>
      <div class="sk-line sk-line-title sk-shimmer" style="margin-top: 20px; width: 60%;"></div>
      <div class="sk-line sk-line-text sk-shimmer" style="margin-top: 12px;"></div>
      <div class="sk-line sk-line-text sk-shimmer" style="margin-top: 8px; width: 90%;"></div>
      <div class="sk-line sk-line-short sk-shimmer" style="margin-top: 8px; width: 40%;"></div>
    </template>

    <!-- 默认块 -->
    <template v-else>
      <div class="sk-line sk-shimmer" v-for="i in count" :key="i"
        :style="{ height: height + 'px', borderRadius: radius + 'px', marginBottom: gap + 'px' }">
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  type: {
    type: String,
    default: 'block',
    validator: v => ['block', 'card', 'list', 'table', 'detail'].includes(v)
  },
  count: { type: Number, default: 4 },
  height: { type: Number, default: 16 },
  radius: { type: Number, default: 8 },
  gap: { type: Number, default: 12 },
  width: { type: String, default: '100%' }
})

const containerStyle = computed(() => ({
  width: props.width
}))
</script>

<style scoped>
.skeleton-box {
  width: 100%;
}

/* shimmer 动画 */
.sk-shimmer {
  background: linear-gradient(90deg, #E8ECF1 25%, #F3F5F8 50%, #E8ECF1 75%);
  background-size: 200% 100%;
  animation: shimmer 1.6s infinite;
  border-radius: 8px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.sk-line {
  width: 100%;
  height: 16px;
}

.sk-line-title {
  width: 55%;
  height: 20px;
}

.sk-line-text {
  width: 100%;
  height: 14px;
}

.sk-line-short {
  width: 35%;
  height: 14px;
}

/* 卡片骨架 */
.sk-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #F0F2F5;
}

.sk-card-img {
  width: 100%;
  height: 180px;
}

.sk-card-body {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 列表骨架 */
.sk-list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #fff;
  border-radius: 14px;
  border: 1px solid #F0F2F5;
}

.sk-circle {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
}

.sk-list-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sk-list-extra {
  width: 60px;
  height: 28px;
  border-radius: 6px;
  flex-shrink: 0;
}

/* 表格骨架 */
.sk-table {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  background: #fff;
  border-bottom: 1px solid #F0F2F5;
}

/* 详情骨架 */
.sk-detail-img {
  width: 100%;
  height: 240px;
  border-radius: 16px;
}
</style>
