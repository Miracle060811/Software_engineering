<template>
  <div class="page-header">
    <div class="page-header-main">
      <div class="page-header-left">
        <h1 class="page-title">
          <span class="page-title-icon" v-if="icon">
            <el-icon v-if="typeof icon !== 'string'" :size="30"><component :is="icon" /></el-icon>
            <span v-else>{{ icon }}</span>
          </span>
          {{ title }}
        </h1>
        <p class="page-subtitle" v-if="subtitle">{{ subtitle }}</p>
      </div>
      <div class="page-header-extra" v-if="$slots.extra">
        <slot name="extra" />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  icon: { type: [String, Object], default: '' },
  breadcrumbs: {
    type: Array,
    default: () => []
    // [{ label: '首页', to: '/' }, { label: '机票搜索' }]
  }
})
</script>

<style scoped>
.page-header {
  margin-bottom: 28px;
  padding: 12px 0 24px;
  border-bottom: 1px solid var(--tm-line);
}

.page-header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.page-title {
  margin: 0;
  font-family: var(--tm-font-serif);
  font-size: clamp(28px, 3vw, 42px);
  font-weight: 650;
  color: var(--tm-ink);
  letter-spacing: -0.025em;
  line-height: 1.25;
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: var(--tm-gold);
}

.page-subtitle {
  font-size: 15px;
  color: var(--tm-muted);
  margin: 8px 0 0 48px;
  line-height: 1.7;
}

.page-header-extra {
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .page-header-main {
    flex-direction: column;
  }

  .page-subtitle {
    margin-left: 0;
  }
}
</style>
