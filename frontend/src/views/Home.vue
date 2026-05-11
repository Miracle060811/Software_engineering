<template>
  <div class="home-page">
    <!-- Hero 搜索区 -->
    <section class="hero-section">
      <div class="hero-bg"></div>
      <div class="hero-content">
        <h1 class="hero-title">
          <span class="title-line">探索世界，</span>
          <span class="title-line accent">从这里开始</span>
        </h1>
        <p class="hero-sub">机票 · 火车票 · 酒店 · 景点 — 一站式智慧出行</p>

        <el-card class="search-card" shadow="always">
          <el-tabs v-model="searchTab" class="search-tabs">
            <el-tab-pane label="✈️ 机票" name="flight">
              <el-row :gutter="12">
                <el-col :span="7">
                  <el-input
                    v-model="flightForm.depCity"
                    placeholder="出发城市"
                    size="large"
                    :prefix-icon="Location"
                  />
                </el-col>
                <el-col :span="7">
                  <el-input
                    v-model="flightForm.arrCity"
                    placeholder="到达城市"
                    size="large"
                    :prefix-icon="Location"
                  />
                </el-col>
                <el-col :span="6">
                  <el-date-picker
                    v-model="flightForm.date"
                    type="date"
                    placeholder="出发日期"
                    size="large"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </el-col>
                <el-col :span="4">
                  <el-button
                    type="primary"
                    size="large"
                    style="width: 100%; height: 44px"
                    @click="searchFlight"
                  >
                    <el-icon><Search /></el-icon>搜索
                  </el-button>
                </el-col>
              </el-row>
            </el-tab-pane>

            <el-tab-pane label="🚆 火车票" name="train">
              <el-row :gutter="12">
                <el-col :span="7">
                  <el-input
                    v-model="trainForm.depStation"
                    placeholder="出发站"
                    size="large"
                    :prefix-icon="Location"
                  />
                </el-col>
                <el-col :span="7">
                  <el-input
                    v-model="trainForm.arrStation"
                    placeholder="到达站"
                    size="large"
                    :prefix-icon="Location"
                  />
                </el-col>
                <el-col :span="6">
                  <el-date-picker
                    v-model="trainForm.date"
                    type="date"
                    placeholder="出发日期"
                    size="large"
                    style="width: 100%"
                    value-format="YYYY-MM-DD"
                  />
                </el-col>
                <el-col :span="4">
                  <el-button
                    type="primary"
                    size="large"
                    style="width: 100%; height: 44px"
                    @click="searchTrain"
                  >
                    <el-icon><Search /></el-icon>搜索
                  </el-button>
                </el-col>
              </el-row>
            </el-tab-pane>

            <el-tab-pane label="🏨 酒店" name="hotel">
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-input
                    v-model="hotelForm.city"
                    placeholder="目的城市"
                    size="large"
                    :prefix-icon="Location"
                  />
                </el-col>
                <el-col :span="8">
                  <el-date-picker
                    v-model="hotelForm.dateRange"
                    type="daterange"
                    start-placeholder="入住日期"
                    end-placeholder="退房日期"
                    size="large"
                    style="width: 100%"
                  />
                </el-col>
                <el-col :span="8">
                  <el-button
                    type="primary"
                    size="large"
                    style="width: 100%; height: 44px"
                    @click="searchHotel"
                  >
                    <el-icon><Search /></el-icon>搜索酒店
                  </el-button>
                </el-col>
              </el-row>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </section>

    <!-- 热门目的地 -->
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">🔥 热门目的地</h2>
        <span class="section-more">发现更多 →</span>
      </div>
      <el-row :gutter="20">
        <el-col :span="6" v-for="dest in hotDestinations" :key="dest.name">
          <el-card
            class="dest-card"
            :body-style="{ padding: 0 }"
            shadow="hover"
            @click="goDestination(dest)"
          >
            <div class="dest-img-wrap">
              <img :src="dest.img" class="dest-img" :alt="dest.name" />
              <div class="dest-overlay"></div>
              <div class="dest-badge">{{ dest.tag }}</div>
            </div>
            <div class="dest-info">
              <div class="dest-name">{{ dest.name }}</div>
              <div class="dest-desc">{{ dest.desc }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 功能入口 -->
    <section class="section">
      <div class="section-header">
        <h2 class="section-title">✨ 发现更多</h2>
      </div>
      <el-row :gutter="20">
        <el-col :span="8" v-for="feat in features" :key="feat.label">
          <el-card
            class="feature-card"
            shadow="hover"
            @click="$router.push(feat.path)"
          >
            <div class="feature-icon-wrap" :style="{ background: feat.color }">
              <span class="feature-icon">{{ feat.icon }}</span>
            </div>
            <div class="feature-title">{{ feat.label }}</div>
            <div class="feature-desc">{{ feat.desc }}</div>
          </el-card>
        </el-col>
      </el-row>
    </section>

    <!-- 统计数据 -->
    <section class="section stats-section">
      <el-row :gutter="24">
        <el-col :span="6" v-for="stat in stats" :key="stat.label">
          <div class="stat-card">
            <div class="stat-num">{{ stat.num }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </el-col>
      </el-row>
    </section>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { Search, Location } from "@element-plus/icons-vue";

const router = useRouter();
const searchTab = ref("flight");

const flightForm = ref({ depCity: "", arrCity: "" });
const trainForm = ref({ depStation: "", arrStation: "" });
const hotelForm = ref({ city: "", dateRange: [] });

const features = [
  {
    icon: "🤖",
    label: "AI 智能规划",
    desc: "一键生成专属行程，出行更轻松",
    path: "/ai-plan",
    color: "linear-gradient(135deg, #6366F1, #8B5CF6)",
  },
  {
    icon: "📖",
    label: "旅行社区",
    desc: "分享旅途故事，发现精彩游记",
    path: "/community",
    color: "linear-gradient(135deg, #0D9488, #10B981)",
  },
  {
    icon: "🏛️",
    label: "热门景点",
    desc: "探索各地必打卡景点，提前购票",
    path: "/attractions",
    color: "linear-gradient(135deg, #F59E0B, #F97316)",
  },
];

const hotDestinations = [
  {
    name: "北京",
    desc: "古都风韵，历史与现代交融",
    tag: "文化古都",
    img: "https://picsum.photos/seed/beijing/400/260",
  },
  {
    name: "上海",
    desc: "东方明珠，繁华都市体验",
    tag: "魔都风情",
    img: "https://picsum.photos/seed/shanghai/400/260",
  },
  {
    name: "三亚",
    desc: "碧海蓝天，度假天堂首选",
    tag: "海岛度假",
    img: "https://picsum.photos/seed/sanya/400/260",
  },
  {
    name: "成都",
    desc: "熊猫故乡，慢生活美食之都",
    tag: "美食天堂",
    img: "https://picsum.photos/seed/chengdu/400/260",
  },
];

const stats = [
  { num: "1000+", label: "航线覆盖" },
  { num: "50万+", label: "用户信赖" },
  { num: "99.9%", label: "出票成功率" },
  { num: "24h", label: "客服在线" },
];

const searchFlight = () => {
  router.push({
    path: "/flight-search",
    query: {
      depCity: flightForm.value.depCity,
      arrCity: flightForm.value.arrCity,
    },
  });
};

const searchTrain = () => {
  router.push({
    path: "/train-search",
    query: {
      depStation: trainForm.value.depStation,
      arrStation: trainForm.value.arrStation,
    },
  });
};

const searchHotel = () => {
  router.push({
    path: "/hotel-search",
    query: { city: hotelForm.value.city },
  });
};

const goDestination = (dest) => {
  router.push({ path: "/hotel-search", query: { city: dest.name } });
};
</script>

<style scoped>
.home-page {
  padding-bottom: 60px;
}

/* ====== Hero ====== */
.hero-section {
  position: relative;
  margin: -28px -40px 48px;
  padding: 80px 40px 64px;
  overflow: hidden;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 60% at 30% 20%, rgba(13, 148, 136, 0.15), transparent),
    radial-gradient(ellipse 60% 80% at 70% 60%, rgba(99, 102, 241, 0.12), transparent),
    radial-gradient(ellipse 50% 50% at 50% 100%, rgba(16, 185, 129, 0.08), transparent);
  z-index: 0;
}

.hero-section::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    repeating-linear-gradient(
      45deg,
      transparent,
      transparent 40px,
      rgba(255, 255, 255, 0.015) 40px,
      rgba(255, 255, 255, 0.015) 80px
    );
  z-index: 0;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.hero-title {
  font-size: 44px;
  font-weight: 800;
  margin-bottom: 16px;
  line-height: 1.3;
}

.title-line {
  display: block;
  color: #1E293B;
}
.title-line.accent {
  background: linear-gradient(135deg, #0D9488 0%, #10B981 50%, #6366F1 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-sub {
  font-size: 17px;
  color: #64748B;
  margin-bottom: 40px;
}

.search-card {
  max-width: 920px;
  margin: 0 auto;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 4px rgba(0, 0, 0, 0.04);
}

.search-card :deep(.el-card__body) {
  padding: 28px 32px 32px;
}

.search-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}
.search-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #F1F5F9;
}
.search-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  padding: 0 20px;
  height: 40px;
  line-height: 40px;
}

/* ====== 通用 Section ====== */
.section {
  margin-bottom: 56px;
}

.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 24px;
}

.section-title {
  font-size: 24px;
  font-weight: 700;
  color: #1E293B;
  position: relative;
}

.section-more {
  font-size: 14px;
  color: #0D9488;
  cursor: pointer;
  font-weight: 500;
  transition: opacity 0.2s;
}
.section-more:hover {
  opacity: 0.7;
}

/* ====== 目的地卡片 ====== */
.dest-card {
  cursor: pointer;
  border-radius: 16px;
  overflow: hidden;
  border: none;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.dest-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12);
}

.dest-img-wrap {
  position: relative;
  overflow: hidden;
}

.dest-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
  transition: transform 0.5s ease;
}
.dest-card:hover .dest-img {
  transform: scale(1.08);
}

.dest-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.35), transparent 50%);
  opacity: 0;
  transition: opacity 0.35s;
}
.dest-card:hover .dest-overlay {
  opacity: 1;
}

.dest-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  color: #0D9488;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}

.dest-info {
  padding: 16px 18px;
}

.dest-name {
  font-size: 18px;
  font-weight: 700;
  color: #1E293B;
  margin-bottom: 4px;
}

.dest-desc {
  font-size: 13px;
  color: #94A3B8;
}

/* ====== 功能入口卡片 ====== */
.feature-card {
  cursor: pointer;
  text-align: center;
  padding: 12px;
  border-radius: 16px;
  border: 1px solid #F1F5F9;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  border-color: #CCFBF1;
}

.feature-card :deep(.el-card__body) {
  padding: 36px 24px 32px;
}

.feature-icon-wrap {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.feature-icon {
  font-size: 36px;
}

.feature-title {
  font-size: 18px;
  font-weight: 700;
  color: #1E293B;
  margin-bottom: 8px;
}

.feature-desc {
  font-size: 14px;
  color: #94A3B8;
  line-height: 1.5;
}

/* ====== 统计区 ====== */
.stats-section {
  background: #fff;
  border-radius: 20px;
  padding: 36px 40px;
  border: 1px solid #F1F5F9;
}

.stat-card {
  text-align: center;
  padding: 12px 0;
}

.stat-num {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #0D9488, #10B981);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 14px;
  color: #94A3B8;
  font-weight: 500;
}
</style>
