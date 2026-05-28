<template>
  <div v-if="destination" class="destination-detail-page">
    <section class="detail-hero">
      <SafeImage :src="destination.img" :alt="destination.name" />
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <el-button class="back-btn" round @click="$router.push('/destinations')">
          <el-icon><ArrowLeft /></el-icon>
          返回目的地
        </el-button>
        <div class="hero-meta">{{ destination.country }} · {{ destination.tag }}</div>
        <h1>{{ destination.name }}</h1>
        <p>{{ destination.intro }}</p>
        <div class="hero-tags">
          <el-tag v-for="tag in destination.keywords" :key="tag" round effect="dark">
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </section>

    <section class="detail-layout">
      <main class="detail-main">
        <section class="content-block">
          <h2>城市亮点</h2>
          <div class="highlight-list">
            <div v-for="item in destination.highlights" :key="item" class="highlight-item">
              <span></span>
              <p>{{ item }}</p>
            </div>
          </div>
        </section>

        <section class="content-block">
          <h2>旅行气质</h2>
          <p>{{ destination.culture }}</p>
        </section>
      </main>

      <aside class="detail-side">
        <div class="info-card">
          <h3>推荐季节</h3>
          <p>{{ destination.bestSeason }}</p>
        </div>
        <div class="info-card">
          <h3>交通建议</h3>
          <p>{{ destination.transport }}</p>
        </div>
        <div class="info-card source-card">
          <h3>资料来源</h3>
          <a :href="destination.sourceUrl" target="_blank" rel="noreferrer">
            {{ destination.sourceName }}
            <el-icon><Link /></el-icon>
          </a>
        </div>
      </aside>
    </section>
  </div>

  <div v-else class="missing-page">
    <h1>目的地不存在</h1>
    <p>当前城市资料暂未收录。</p>
    <el-button type="primary" round @click="$router.push('/destinations')">
      返回目的地列表
    </el-button>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ArrowLeft, Link } from "@element-plus/icons-vue";
import SafeImage from "@/components/SafeImage.vue";
import { fetchDestinationBySlug } from "@/utils/destinations";

const route = useRoute();
const destination = ref(null);

const loadDestination = async () => {
  destination.value = await fetchDestinationBySlug(route.params.slug);
};

onMounted(loadDestination);
watch(() => route.params.slug, loadDestination);
</script>

<style scoped>
.destination-detail-page {
  animation: fadeIn 0.4s ease;
}

.detail-hero {
  min-height: 430px;
  border-radius: 24px;
  overflow: hidden;
  position: relative;
  display: flex;
  align-items: flex-end;
  background: #0f172a;
}

.detail-hero img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(15, 23, 42, 0.82), rgba(15, 23, 42, 0.3)),
    linear-gradient(0deg, rgba(15, 23, 42, 0.78), transparent 58%);
}

.hero-content {
  position: relative;
  z-index: 1;
  padding: 36px;
  max-width: 780px;
  color: #fff;
}

.back-btn {
  margin-bottom: 28px;
  border: none;
}

.hero-meta {
  font-size: 14px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.82);
  margin-bottom: 10px;
}

.hero-content h1 {
  font-size: 46px;
  line-height: 1.18;
  margin-bottom: 14px;
}

.hero-content p {
  color: rgba(255, 255, 255, 0.88);
  font-size: 16px;
  line-height: 1.8;
}

.hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 20px;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  margin-top: 24px;
}

.detail-main,
.detail-side {
  display: grid;
  gap: 18px;
  align-content: start;
}

.content-block,
.info-card {
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  padding: 24px;
  box-shadow: var(--tm-shadow-card);
}

.content-block h2,
.info-card h3 {
  font-size: 20px;
  color: var(--el-text-color-primary);
  margin-bottom: 14px;
}

.content-block p,
.info-card p {
  color: var(--el-text-color-regular);
  line-height: 1.8;
}

.highlight-list {
  display: grid;
  gap: 14px;
}

.highlight-item {
  display: grid;
  grid-template-columns: 12px 1fr;
  gap: 12px;
  align-items: start;
}

.highlight-item span {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-top: 9px;
  background: linear-gradient(135deg, var(--el-color-primary), var(--tm-color-sunset));
}

.source-card a {
  color: var(--el-color-primary);
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.missing-page {
  min-height: 55vh;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 12px;
  text-align: center;
}

@media (max-width: 900px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .detail-hero {
    min-height: 380px;
  }
}

@media (max-width: 640px) {
  .hero-content {
    padding: 26px 22px;
  }

  .hero-content h1 {
    font-size: 34px;
  }
}
</style>
