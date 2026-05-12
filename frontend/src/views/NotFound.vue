<template>
  <div class="not-found-page">
    <div class="not-found-content">
      <div class="not-found-code">404</div>
      <h1 class="not-found-title">页面未找到</h1>
      <p class="not-found-desc">你访问的页面不存在或已被移除</p>
      <div class="not-found-actions">
        <el-button type="primary" size="large" @click="$router.push('/')">
          <el-icon style="margin-right:6px"><HomeFilled /></el-icon>返回首页
        </el-button>
        <el-button size="large" @click="$router.back()">
          返回上一页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { HomeFilled } from "@element-plus/icons-vue";
</script>

<style scoped>
.not-found-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 40px 20px;
}

.not-found-content {
  text-align: center;
  animation: fadeInUp 0.5s ease;
}

.not-found-code {
  font-size: 120px;
  font-weight: 900;
  line-height: 1;
  background: var(--tm-gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 4s ease infinite;
  margin-bottom: 8px;
}

.not-found-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-bottom: 8px;
}

.not-found-desc {
  font-size: 15px;
  color: var(--el-text-color-secondary);
  margin-bottom: 32px;
}

.not-found-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
}
</style>
