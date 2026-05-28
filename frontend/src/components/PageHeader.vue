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
  margin-bottom: 24px;
  animation: fadeInUp 0.45s ease;
  padding: 22px 24px;
  border: 1px solid oklch(0.78 0.026 82 / 0.44);
  border-radius: 8px;
  background:
    linear-gradient(135deg, oklch(0.975 0.018 82), oklch(0.915 0.030 82));
  box-shadow: var(--tm-shadow-card);
}

.page-header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.page-title {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 30px;
  font-weight: 500;
  color: var(--tm-ink);
  letter-spacing: 0;
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: oklch(0.90 0.032 82);
  color: var(--tm-bark);
}

.page-subtitle {
  font-size: 15px;
  color: var(--tm-muted);
  margin-top: 6px;
  line-height: 1.5;
}

.page-header-extra {
  flex-shrink: 0;
}

@media (max-width: 640px) {
  .page-title {
    font-size: 22px;
  }
  .page-header-main {
    flex-direction: column;
  }
}
</style>
