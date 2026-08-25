<template>
  <div class="destination-list-page">
    <section class="list-hero" :style="heroStyle">
      <div>
        <div class="eyebrow">热门城市</div>
        <h1>从城市开始，找到下一段旅程</h1>
        <p>精选 {{ destinations.length }} 个高频旅行城市，整理真实目的地资料、代表景点和实用出行建议。</p>
      </div>
      <el-button type="primary" round @click="$router.push('/ai-plan')">
        <el-icon><MagicStick /></el-icon>
        AI 规划行程
      </el-button>
    </section>

    <section class="destination-grid">
      <article
        v-for="(dest, index) in destinations"
        :key="dest.slug"
        class="destination-card"
        @click="$router.push(`/destination/${dest.slug}`)"
      >
        <div class="image-wrap">
          <SafeImage
            :src="dest.img"
            :alt="dest.name"
            :loading="index < 3 ? 'eager' : 'lazy'"
            :fetchpriority="index === 0 ? 'high' : 'auto'"
            sizes="(max-width: 768px) 100vw, 33vw"
          />
          <span>{{ dest.tag }}</span>
        </div>
        <div class="card-body">
          <div class="card-title-row">
            <h2>{{ dest.name }}</h2>
            <el-icon><ArrowRight /></el-icon>
          </div>
          <p>{{ dest.desc }}</p>
          <div class="keyword-row">
            <el-tag v-for="tag in dest.keywords" :key="tag" size="small" round>
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ArrowRight, MagicStick } from "@element-plus/icons-vue";
import SafeImage from "@/components/SafeImage.vue";
import { fallbackDestinations, fetchDestinationList } from "@/utils/destinations";
import { getResponsiveImageData } from "@/utils/image";

const destinations = ref(fallbackDestinations);

const heroStyle = computed(() => ({
  background:
    "linear-gradient(135deg, rgba(13, 148, 136, 0.94), rgba(14, 165, 233, 0.88)), url('" +
    getResponsiveImageData(
      destinations.value.find((item) => item.slug === "hangzhou")?.img ||
        destinations.value[0]?.img ||
        "",
    ).src +
    "') center/cover",
}));

onMounted(async () => {
  destinations.value = await fetchDestinationList();
});
</script>

<style scoped>
.destination-list-page {
  animation: fadeIn 0.4s ease;
}

.list-hero {
  min-height: 260px;
  padding: 44px;
  border-radius: 24px;
  color: #fff;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  overflow: hidden;
}

.eyebrow {
  font-size: 13px;
  font-weight: 800;
  opacity: 0.9;
  margin-bottom: 12px;
}

.list-hero h1 {
  font-size: 34px;
  line-height: 1.25;
  margin-bottom: 12px;
}

.list-hero p {
  max-width: 620px;
  color: rgba(255, 255, 255, 0.86);
  font-size: 16px;
}

.destination-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-top: 24px;
}

.destination-card {
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: var(--tm-shadow-card);
  transition: transform 0.28s ease, box-shadow 0.28s ease, border-color 0.28s ease;
}

.destination-card:hover {
  transform: translateY(-6px);
  border-color: var(--el-color-primary-light-7);
  box-shadow: var(--tm-shadow-card-hover);
}

.image-wrap {
  height: 190px;
  position: relative;
  overflow: hidden;
}

.image-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.45s ease;
}

.destination-card:hover img {
  transform: scale(1.06);
}

.image-wrap span {
  position: absolute;
  top: 14px;
  right: 14px;
  padding: 6px 13px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 800;
}

.card-body {
  padding: 18px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.card-title-row h2 {
  font-size: 21px;
}

.card-title-row .el-icon {
  color: var(--el-color-primary);
}

.card-body p {
  min-height: 44px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  line-height: 1.55;
  margin-bottom: 14px;
}

.keyword-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

@media (max-width: 992px) {
  .destination-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .list-hero {
    padding: 30px 22px;
    align-items: flex-start;
    flex-direction: column;
  }

  .list-hero h1 {
    font-size: 26px;
  }

  .destination-grid {
    grid-template-columns: 1fr;
  }
}
</style>
