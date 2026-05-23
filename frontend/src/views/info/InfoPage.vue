<template>
  <div class="info-page">
    <section class="info-hero">
      <div class="info-badge">{{ page.badge }}</div>
      <h1>{{ page.title }}</h1>
      <p>{{ page.subtitle }}</p>
    </section>

    <section class="info-content">
      <article v-for="section in page.sections" :key="section.title" class="info-card">
        <h2>{{ section.title }}</h2>
        <p>{{ section.body }}</p>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { infoPages } from "@/data/infoPages";

const route = useRoute();
const page = computed(() => infoPages[route.meta.pageKey] || infoPages.about);
</script>

<style scoped>
.info-page {
  max-width: var(--tm-content-detail);
  margin: 0 auto;
  animation: fadeIn 0.4s ease;
}

.info-hero {
  padding: 42px;
  border-radius: 24px;
  background:
    radial-gradient(circle at 16% 20%, rgba(245, 158, 11, 0.18), transparent 28%),
    linear-gradient(135deg, #ecfdfa 0%, #ffffff 52%, #eef2ff 100%);
  border: 1px solid rgba(13, 148, 136, 0.12);
  margin-bottom: 22px;
}

.info-badge {
  display: inline-flex;
  padding: 6px 14px;
  border-radius: 999px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 16px;
}

.info-hero h1 {
  font-size: 34px;
  line-height: 1.25;
  color: var(--el-text-color-primary);
  margin-bottom: 10px;
}

.info-hero p {
  max-width: 720px;
  color: var(--el-text-color-secondary);
  font-size: 16px;
  line-height: 1.75;
}

.info-content {
  display: grid;
  gap: 16px;
}

.info-card {
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  padding: 24px;
  box-shadow: var(--tm-shadow-card);
}

.info-card h2 {
  font-size: 19px;
  color: var(--el-text-color-primary);
  margin-bottom: 10px;
}

.info-card p {
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

@media (max-width: 640px) {
  .info-hero {
    padding: 30px 22px;
  }

  .info-hero h1 {
    font-size: 28px;
  }
}
</style>
