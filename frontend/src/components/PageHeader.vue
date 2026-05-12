<template>
  <div class="page-header">
    <!-- 面包屑 -->
    <el-breadcrumb v-if="breadcrumbs.length" class="page-breadcrumb" separator="/">
      <el-breadcrumb-item v-for="(crumb, idx) in breadcrumbs" :key="idx" :to="crumb.to">
        {{ crumb.label }}
      </el-breadcrumb-item>
    </el-breadcrumb>

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
  animation: fadeInUp 0.45s ease;
}

.page-breadcrumb {
  margin-bottom: 12px;
}

.page-breadcrumb :deep(.el-breadcrumb__inner) {
  color: #A0A0B8;
  transition: color 0.2s;
}
.page-breadcrumb :deep(.el-breadcrumb__inner:hover) {
  color: var(--el-color-primary);
}
.page-breadcrumb :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.page-header-main {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--el-text-color-primary);
  letter-spacing: -0.3px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title-icon {
  display: inline-flex;
  align-items: center;
  color: var(--el-color-primary);
}

.page-subtitle {
  font-size: 15px;
  color: var(--el-text-color-secondary);
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
