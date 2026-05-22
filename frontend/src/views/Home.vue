<template>
  <div class="home-page">
    <!-- ========== Hero 区 ========== -->
    <section class="hero-section">
      <div class="hero-bg">
        <div class="hero-blob hero-blob-1"></div>
        <div class="hero-blob hero-blob-2"></div>
        <div class="hero-blob hero-blob-3"></div>
      </div>

      <div class="hero-content">
        <div class="hero-badge">
          <span class="badge-dot"></span>
          AI 驱动的智能旅行规划
        </div>
        <h1 class="hero-title">
          探索世界，
          <span class="title-gradient">从这里开始</span>
        </h1>
        <p class="hero-sub">
          机票 · 火车票 · 酒店 · 景点 — 一站式智慧出行，让每次旅途都精彩
        </p>

        <!-- 搜索卡片 -->
        <div class="search-card">
          <el-tabs v-model="searchTab" class="hero-search-tabs">
            <el-tab-pane name="flight">
              <template #label><el-icon style="margin-right:4px"><Promotion /></el-icon>机票</template>
            </el-tab-pane>
            <el-tab-pane name="train">
              <template #label><el-icon style="margin-right:4px"><Tickets /></el-icon>火车票</template>
            </el-tab-pane>
            <el-tab-pane name="hotel">
              <template #label><el-icon style="margin-right:4px"><House /></el-icon>酒店</template>
            </el-tab-pane>
          </el-tabs>

          <div class="search-row" v-if="searchTab === 'flight'">
            <div class="search-field">
              <el-input
                v-model="flightForm.depCity"
                placeholder="出发城市"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <div class="search-swap">
              <el-icon :size="18"><Right /></el-icon>
            </div>
            <div class="search-field">
              <el-input
                v-model="flightForm.arrCity"
                placeholder="到达城市"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <div class="search-field">
              <el-date-picker
                v-model="flightForm.date"
                type="date"
                placeholder="出发日期"
                size="large"
                value-format="YYYY-MM-DD"
                class="search-date"
                :prefix-icon="Calendar"
              />
            </div>
            <el-button
              type="primary"
              size="large"
              class="search-btn"
              @click="searchFlight"
            >
              <el-icon><Search /></el-icon>
              <span>搜索</span>
            </el-button>
          </div>

          <div class="search-row" v-if="searchTab === 'train'">
            <div class="search-field">
              <el-input
                v-model="trainForm.depStation"
                placeholder="出发站"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <div class="search-swap">
              <el-icon :size="18"><Right /></el-icon>
            </div>
            <div class="search-field">
              <el-input
                v-model="trainForm.arrStation"
                placeholder="到达站"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <div class="search-field">
              <el-date-picker
                v-model="trainForm.date"
                type="date"
                placeholder="出发日期"
                size="large"
                value-format="YYYY-MM-DD"
                class="search-date"
              />
            </div>
            <el-button
              type="primary"
              size="large"
              class="search-btn"
              @click="searchTrain"
            >
              <el-icon><Search /></el-icon>
              <span>搜索</span>
            </el-button>
          </div>

          <div class="search-row" v-if="searchTab === 'hotel'">
            <div class="search-field">
              <el-input
                v-model="hotelForm.city"
                placeholder="目的城市"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <div class="search-field">
              <el-date-picker
                v-model="hotelForm.dateRange"
                type="daterange"
                start-placeholder="入住日期"
                end-placeholder="退房日期"
                size="large"
                class="search-date"
              />
            </div>
            <el-button
              type="primary"
              size="large"
              class="search-btn"
              @click="searchHotel"
            >
              <el-icon><Search /></el-icon>
              <span>搜索酒店</span>
            </el-button>
          </div>

          <!-- 热门搜索提示 -->
          <div class="search-hints">
            <span class="hint-label">热门搜索:</span>
            <el-tag
              v-for="hint in hotSearches"
              :key="hint"
              size="small"
              class="hint-tag"
              @click="quickSearch(hint)"
            >
              {{ hint }}
            </el-tag>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== 统计数据 ========== -->
    <section class="stats-section">
      <div class="stats-grid">
        <div class="stat-item" v-for="stat in stats" :key="stat.label">
          <div class="stat-icon-wrap" :style="{ background: stat.gradient }">
            <el-icon class="stat-icon-el" :size="24"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <CountUp :target="stat.num" class="stat-num" />
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== 热门目的地 ========== -->
    <section class="dest-section">
      <div class="section-header">
        <div>
          <h2 class="section-title">热门目的地</h2>
          <p class="section-sub">精选热门旅行目的地，发现你的下一站</p>
        </div>
        <el-button text type="primary" @click="$router.push('/hotel-search')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="dest-grid">
        <div
          class="dest-card"
          v-for="(dest, idx) in hotDestinations"
          :key="dest.name"
          :style="{ animationDelay: idx * 0.08 + 's' }"
          @click="goDestination(dest)"
        >
          <div class="dest-img-wrap">
            <img :src="dest.img" :alt="dest.name" class="dest-img" />
            <div class="dest-gradient"></div>
            <div class="dest-badge">{{ dest.tag }}</div>
            <div class="dest-img-info">
              <span class="dest-city">{{ dest.name }}</span>
              <span class="dest-country">{{ dest.country }}</span>
            </div>
          </div>
          <div class="dest-body">
            <p class="dest-desc">{{ dest.desc }}</p>
            <div class="dest-tags">
              <el-tag
                v-for="t in dest.keywords"
                :key="t"
                size="small"
                effect="plain"
                round
              >
                {{ t }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== 功能入口 ========== -->
    <section class="feature-section">
      <div class="section-header">
        <div>
          <h2 class="section-title">发现更多精彩</h2>
          <p class="section-sub">不止出行，还有更多旅行灵感等你探索</p>
        </div>
      </div>

      <div class="feature-grid">
        <div class="feature-card" @click="$router.push('/ai-plan')">
          <div class="feat-visual feat-visual-ai">
            <div class="feat-illustration">
              <img :src="aiPlannerIcon" alt="AI 智能规划" class="feat-icon-img" />
              <div class="feat-rings">
                <span class="ring ring-1"></span>
                <span class="ring ring-2"></span>
              </div>
            </div>
          </div>
          <div class="feat-body">
            <h3 class="feat-title">AI 智能规划</h3>
            <p class="feat-desc">一键生成专属行程方案，让你的旅行更轻松高效</p>
            <span class="feat-link">开始规划 <el-icon><ArrowRight /></el-icon></span>
          </div>
        </div>

        <div class="feature-card" @click="$router.push('/community')">
          <div class="feat-visual feat-visual-community">
            <div class="feat-illustration">
              <img :src="communityIcon" alt="旅行社区" class="feat-icon-img" />
              <div class="feat-rings">
                <span class="ring ring-1"></span>
                <span class="ring ring-2"></span>
              </div>
            </div>
          </div>
          <div class="feat-body">
            <h3 class="feat-title">旅行社区</h3>
            <p class="feat-desc">分享你的旅途故事，发现更多旅行达人游记</p>
            <span class="feat-link">探索社区 <el-icon><ArrowRight /></el-icon></span>
          </div>
        </div>

        <div class="feature-card" @click="$router.push('/attractions')">
          <div class="feat-visual feat-visual-attraction">
            <div class="feat-illustration">
              <img :src="attractionIcon" alt="热门景点" class="feat-icon-img" />
              <div class="feat-rings">
                <span class="ring ring-1"></span>
                <span class="ring ring-2"></span>
              </div>
            </div>
          </div>
          <div class="feat-body">
            <h3 class="feat-title">热门景点</h3>
            <p class="feat-desc">探索各地必打卡景点，在线购票免排队</p>
            <span class="feat-link">探索景点 <el-icon><ArrowRight /></el-icon></span>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== CTA Banner ========== -->
    <section class="cta-section">
      <div class="cta-card">
        <div class="cta-content">
          <h2 class="cta-title">准备好开始你的下一次旅行了吗？</h2>
          <p class="cta-sub">加入 <strong>50万+</strong> 旅行者，用 TravelMate 规划完美旅程</p>
          <el-button type="primary" size="large" round @click="$router.push('/ai-plan')">
            <el-icon style="margin-right:6px"><MagicStick /></el-icon>免费开始规划
          </el-button>
        </div>
        <div class="cta-decoration">
          <span class="cta-shape cta-shape-1"></span>
          <span class="cta-shape cta-shape-2"></span>
          <span class="cta-shape cta-shape-3"></span>
          <span class="cta-shape cta-shape-4"></span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import {
  Search,
  Right,
  LocationFilled,
  Calendar,
  ArrowRight,
  Promotion,
  Tickets,
  House,
  UserFilled,
  Aim,
  ChatDotSquare,
  MagicStick,
} from "@element-plus/icons-vue";
import CountUp from "../components/CountUp.vue";
import aiPlannerIcon from "@/assets/feature-icons/ai-planner.png";
import communityIcon from "@/assets/feature-icons/travel-community.png";
import attractionIcon from "@/assets/feature-icons/attraction-ticket.png";

const router = useRouter();
const searchTab = ref("flight");

const flightForm = ref({ depCity: "", arrCity: "", date: "" });
const trainForm = ref({ depStation: "", arrStation: "", date: "" });
const hotelForm = ref({ city: "", dateRange: [] });

const hotSearches = ["北京", "上海", "三亚", "成都", "杭州", "西安"];

const stats = [
  {
    icon: Promotion,
    gradient: "linear-gradient(135deg, #0D9488, #0EA5E9)",
    num: 1280,
    label: "航线覆盖",
  },
  {
    icon: UserFilled,
    gradient: "linear-gradient(135deg, #6366F1, #8B5CF6)",
    num: 523600,
    label: "用户信赖",
  },
  {
    icon: Aim,
    gradient: "linear-gradient(135deg, #F59E0B, #F97316)",
    num: 99.9,
    label: "出票成功率 (%)",
    isDecimal: true,
  },
  {
    icon: ChatDotSquare,
    gradient: "linear-gradient(135deg, #10B981, #22C55E)",
    num: 24,
    label: "小时客服在线",
  },
];

const hotDestinations = [
  {
    name: "北京",
    country: "中国",
    desc: "古都风韵与现代繁华完美交融，探索千年历史遗迹",
    tag: "文化古都",
    keywords: ["故宫", "长城", "胡同"],
    img: "https://upload.wikimedia.org/wikipedia/commons/e/ef/The_Forbidden_City_-_View_from_Coal_Hill.jpg",
  },
  {
    name: "上海",
    country: "中国",
    desc: "东方明珠，感受魔都的摩登魅力与海派风情",
    tag: "魔都风情",
    keywords: ["外滩", "迪士尼", "田子坊"],
    img: "https://upload.wikimedia.org/wikipedia/commons/2/2b/Shanghai_Bund-20150516-RM-173803.jpg",
  },
  {
    name: "三亚",
    country: "中国",
    desc: "碧海蓝天、椰风树影，你的热带度假天堂",
    tag: "海岛度假",
    keywords: ["海滩", "潜水", "海鲜"],
    img: "https://upload.wikimedia.org/wikipedia/commons/4/44/Yalong_Bay_01.jpg",
  },
  {
    name: "成都",
    country: "中国",
    desc: "慢生活与美食的天堂，来了就不想走的城市",
    tag: "美食天堂",
    keywords: ["熊猫", "火锅", "茶馆"],
    img: "https://upload.wikimedia.org/wikipedia/commons/2/20/Chengdu_skyline_June_2017.jpg",
  },
  {
    name: "杭州",
    country: "中国",
    desc: "江南水乡的诗意栖居，西湖美景冠绝天下",
    tag: "江南水乡",
    keywords: ["西湖", "灵隐寺", "龙井"],
    img: "https://upload.wikimedia.org/wikipedia/commons/d/d8/West_Lake%2C_Hangzhou_%28Nine-turn_bridge%29.jpg",
  },
  {
    name: "西安",
    country: "中国",
    desc: "十三朝古都，触摸中华文明的厚重历史",
    tag: "历史名城",
    keywords: ["兵马俑", "古城墙", "回民街"],
    img: "https://upload.wikimedia.org/wikipedia/commons/8/8e/Xi-an_city_wall_side.jpg",
  },
];

const searchFlight = () => {
  router.push({
    path: "/flight-search",
    query: {
      depCity: flightForm.value.depCity,
      arrCity: flightForm.value.arrCity,
      date: flightForm.value.date,
    },
  });
};

const searchTrain = () => {
  router.push({
    path: "/train-search",
    query: {
      depStation: trainForm.value.depStation,
      arrStation: trainForm.value.arrStation,
      date: trainForm.value.date,
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

const quickSearch = (hint) => {
  if (searchTab.value === "flight") {
    flightForm.value.depCity = "上海";
    flightForm.value.arrCity = hint;
  } else if (searchTab.value === "train") {
    trainForm.value.depStation = "上海";
    trainForm.value.arrStation = hint;
  } else {
    hotelForm.value.city = hint;
  }
  router.push({
    path:
      searchTab.value === "flight"
        ? "/flight-search"
        : searchTab.value === "train"
          ? "/train-search"
          : "/hotel-search",
    query:
      searchTab.value === "flight"
        ? { depCity: "上海", arrCity: hint }
        : searchTab.value === "train"
          ? { depStation: "上海", arrStation: hint }
          : { city: hint },
  });
};
</script>

<style scoped>
.home-page {
  margin: -28px calc(-1 * var(--tm-page-padding)) 0;
  animation: fadeIn 0.5s ease;
}

/* ==================== Hero 区 ==================== */
.hero-section {
  position: relative;
  padding: 80px var(--tm-page-padding) 80px;
  overflow: hidden;
  min-height: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hero-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, #F0FDFA 0%, #F7F8FA 40%, #EEF2FF 100%);
}

.hero-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 8s ease-in-out infinite;
}

.hero-blob-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(13,148,136,0.2), transparent);
  top: -150px;
  left: -100px;
}

.hero-blob-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(99,102,241,0.15), transparent);
  bottom: -100px;
  right: -50px;
  animation-delay: -3s;
}

.hero-blob-3 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(245,158,11,0.12), transparent);
  top: 50%;
  left: 50%;
  animation-delay: -5s;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  max-width: 960px;
  width: 100%;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(255,255,255,0.8);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(13,148,136,0.15);
  border-radius: 100px;
  padding: 6px 18px;
  font-size: 13px;
  font-weight: 600;
  color: #0D9488;
  margin-bottom: 24px;
  animation: fadeInUp 0.6s ease;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #0D9488;
  animation: pulse 2s ease infinite;
}

.hero-title {
  font-size: 52px;
  font-weight: 800;
  color: #1A1A2E;
  margin-bottom: 16px;
  line-height: 1.25;
  letter-spacing: -0.5px;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.title-gradient {
  background: var(--tm-gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 4s ease infinite;
}

.hero-sub {
  font-size: 18px;
  color: #71718B;
  margin-bottom: 48px;
  line-height: 1.6;
  animation: fadeInUp 0.6s ease 0.2s both;
}

/* 搜索卡片 */
.search-card {
  background: #fff;
  border-radius: 24px;
  padding: 32px 36px 28px;
  box-shadow:
    0 4px 6px rgba(0,0,0,0.02),
    0 12px 40px rgba(0,0,0,0.06),
    0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid rgba(0,0,0,0.04);
  animation: fadeInUp 0.6s ease 0.3s both;
}

.hero-search-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.hero-search-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #F0F2F5;
}

.hero-search-tabs :deep(.el-tabs__item) {
  font-size: 16px;
  font-weight: 600;
  padding: 0 24px;
  height: 44px;
  line-height: 44px;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.search-field {
  flex: 1;
  min-width: 0;
}

.search-input :deep(.el-input__wrapper),
.search-date :deep(.el-input__wrapper) {
  border-radius: 14px;
  height: 50px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
  border: 1px solid #E8ECF3;
}
.search-input :deep(.el-input__wrapper:hover),
.search-date :deep(.el-input__wrapper:hover) {
  border-color: #CCD5DE;
}

.search-swap {
  color: #A0A0B8;
  flex-shrink: 0;
}

.search-btn {
  height: 50px;
  padding: 0 32px;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
}

/* 热门搜索提示 */
.search-hints {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.hint-label {
  font-size: 13px;
  color: #A0A0B8;
  flex-shrink: 0;
}

.hint-tag {
  cursor: pointer;
  transition: all 0.2s ease;
}
.hint-tag:hover {
  background: var(--el-color-primary-light-8);
  color: var(--el-color-primary);
}

/* ==================== 统计数据 ==================== */
.stats-section {
  padding: 0 var(--tm-page-padding);
  margin-top: -30px;
  position: relative;
  z-index: 2;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  background: #fff;
  border-radius: 20px;
  padding: 28px 32px;
  box-shadow: var(--tm-shadow-card);
  border: 1px solid #F0F2F5;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 12px;
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-el {
  color: #fff;
}

.stat-body {
  min-width: 0;
}

.stat-num {
  font-size: 24px;
  font-weight: 800;
  color: #1A1A2E;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #71718B;
  font-weight: 500;
  margin-top: 2px;
}

/* ==================== Section 通用 ==================== */
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28px;
  gap: 16px;
}

.section-title {
  font-size: 26px;
  font-weight: 800;
  color: #1A1A2E;
  margin-bottom: 6px;
  letter-spacing: -0.3px;
}

.section-sub {
  font-size: 15px;
  color: #71718B;
}

/* ==================== 热门目的地 ==================== */
.dest-section {
  padding: 64px var(--tm-page-padding) 0;
}

.dest-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.dest-card {
  background: #fff;
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #F0F2F5;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease both;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}
.dest-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 20px 50px rgba(0,0,0,0.1);
}

.dest-img-wrap {
  position: relative;
  overflow: hidden;
  height: 200px;
}

.dest-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.6s cubic-bezier(0.4, 0, 0.2, 1);
}
.dest-card:hover .dest-img {
  transform: scale(1.08);
}

.dest-gradient {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.55) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.4s ease;
}
.dest-card:hover .dest-gradient {
  opacity: 1;
}

.dest-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  background: rgba(255,255,255,0.9);
  backdrop-filter: blur(8px);
  color: #0D9488;
  font-size: 12px;
  font-weight: 700;
  padding: 6px 14px;
  border-radius: 100px;
  letter-spacing: 0.5px;
}

.dest-img-info {
  position: absolute;
  bottom: 14px;
  left: 14px;
  display: flex;
  flex-direction: column;
  opacity: 0;
  transform: translateY(8px);
  transition: all 0.4s ease;
}
.dest-card:hover .dest-img-info {
  opacity: 1;
  transform: translateY(0);
}

.dest-city {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0,0,0,0.3);
}

.dest-country {
  font-size: 12px;
  color: rgba(255,255,255,0.85);
  text-shadow: 0 1px 3px rgba(0,0,0,0.3);
}

.dest-body {
  padding: 16px 18px;
}

.dest-desc {
  font-size: 14px;
  color: #71718B;
  margin-bottom: 12px;
  line-height: 1.5;
}

.dest-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* ==================== 功能入口 ==================== */
.feature-section {
  padding: 64px var(--tm-page-padding) 0;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid #F0F2F5;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}
.feature-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 20px 50px rgba(0,0,0,0.08);
}

.feat-visual {
  height: 160px;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.feat-visual-ai {
  background: linear-gradient(135deg, #EEF2FF 0%, #E0E7FF 50%, #C7D2FE 100%);
}
.feat-visual-community {
  background: linear-gradient(135deg, #ECFDFA 0%, #D7FBF7 50%, #B3F6EF 100%);
}
.feat-visual-attraction {
  background: linear-gradient(135deg, #FFFBEB 0%, #FEF3C7 50%, #FDE68A 100%);
}

.feat-illustration {
  position: relative;
  z-index: 1;
}

.feat-icon-img {
  width: 112px;
  height: 112px;
  object-fit: contain;
  position: relative;
  z-index: 2;
  display: block;
  filter: drop-shadow(0 16px 24px rgba(15,23,42,0.16));
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.feature-card:hover .feat-icon-img {
  transform: translateY(-4px) scale(1.05);
}

.feat-rings {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.ring {
  position: absolute;
  border-radius: 50%;
  border: 2px solid rgba(255,255,255,0.4);
  animation: pulse 3s ease infinite;
}
.ring-1 {
  width: 100px;
  height: 100px;
  top: -50px;
  left: -50px;
}
.ring-2 {
  width: 140px;
  height: 140px;
  top: -70px;
  left: -70px;
  animation-delay: -1s;
}

.feat-body {
  padding: 24px;
  text-align: center;
}

.feat-title {
  font-size: 20px;
  font-weight: 700;
  color: #1A1A2E;
  margin-bottom: 8px;
}

.feat-desc {
  font-size: 14px;
  color: #71718B;
  line-height: 1.6;
  margin-bottom: 16px;
}

.feat-link {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-color-primary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: gap 0.2s ease;
}
.feature-card:hover .feat-link {
  gap: 8px;
}

/* ==================== CTA ==================== */
.cta-section {
  padding: 64px var(--tm-page-padding) 0;
}

.cta-card {
  background: var(--tm-gradient-brand);
  background-size: 200% 200%;
  animation: gradientShift 6s ease infinite;
  border-radius: 24px;
  padding: 56px 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  position: relative;
  overflow: hidden;
}

.cta-content {
  position: relative;
  z-index: 1;
}

.cta-title {
  font-size: 28px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 10px;
}

.cta-sub {
  font-size: 16px;
  color: rgba(255,255,255,0.85);
  margin-bottom: 24px;
}

.cta-decoration {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  justify-content: center;
}

.cta-shape {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.4);
  animation: float 4s ease infinite;
  opacity: 0.9;
}
.cta-shape-1 { background: rgba(255, 255, 255, 0.25); }
.cta-shape-2 { background: rgba(255, 255, 255, 0.2); animation-delay: -1s; }
.cta-shape-3 { background: rgba(255, 255, 255, 0.15); animation-delay: -2s; }
.cta-shape-4 { background: rgba(255, 255, 255, 0.1); animation-delay: -3s; }

/* ==================== 响应式 ==================== */
@media (max-width: 992px) {
  .dest-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .feature-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .feature-grid .feature-card:last-child {
    grid-column: 1 / -1;
  }
  .hero-title {
    font-size: 40px;
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .hero-section {
    padding: 48px 12px 48px;
    min-height: auto;
  }
  .hero-title {
    font-size: 30px;
  }
  .hero-sub {
    font-size: 15px;
    margin-bottom: 32px;
  }
  .search-card {
    padding: 20px 16px 16px;
    border-radius: 18px;
  }
  .search-row {
    flex-direction: column;
    gap: 8px;
  }
  .search-field,
  .search-btn {
    width: 100%;
  }
  .search-swap {
    transform: rotate(90deg);
  }
  .dest-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .feature-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .feature-grid .feature-card:last-child {
    grid-column: auto;
  }
  .stats-grid {
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    padding: 20px 16px;
  }
  .cta-card {
    flex-direction: column;
    text-align: center;
    padding: 36px 24px;
  }
  .cta-title {
    font-size: 22px;
  }
}
</style>
