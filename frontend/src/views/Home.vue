<template>
  <div class="home-page">
    <!-- ========== Hero 区 ========== -->
    <section class="hero-section">
      <div class="hero-bg">
        <SafeImage
          :src="heroPhoto"
          :fallback="seedImage('coast.svg')"
          image-class="hero-photo-img"
          alt="海岛旅行风景"
          loading="eager"
        />
        <div class="hero-overlay"></div>
      </div>

      <div class="hero-content">
        <div class="hero-copy">
          <div class="hero-badge">
            <span class="badge-dot"></span>
            AI 驱动的智能旅行规划
          </div>
          <h1 class="hero-title">
            慢下来，
            <span class="title-gradient">去抵达</span>
          </h1>
          <p class="hero-sub">
            把交通、住宿、景点与 AI 行程规划收进一处，让出发前的安排安静、有序，也更接近你想要的旅行节奏。
          </p>
          <div class="hero-chips" aria-label="TravelMate 服务">
            <span>实时票务</span>
            <span>酒店房型</span>
            <span>AI 行程</span>
          </div>
          <div class="hero-actions">
            <el-button type="primary" size="large" round @click="$router.push('/ai-plan')">
              <el-icon><MagicStick /></el-icon>
              <span>AI 生成行程</span>
            </el-button>
            <el-button size="large" round class="hero-secondary-btn" @click="$router.push('/destinations')">
              看热门城市
            </el-button>
          </div>
        </div>

        <div class="hero-gallery">
          <button
            class="hero-image-card hero-image-main"
            type="button"
            @click="goDestination(showcaseDestinations[0])"
          >
            <SafeImage :src="showcaseDestinations[0].img" :alt="showcaseDestinations[0].name" image-class="hero-gallery-img" />
            <div class="hero-image-caption">
              <span>{{ showcaseDestinations[0].tag }}</span>
              <strong>{{ showcaseDestinations[0].name }}</strong>
            </div>
          </button>
          <div class="hero-mini-grid">
            <button
              v-for="dest in showcaseDestinations.slice(1)"
              :key="dest.slug"
              class="hero-image-card"
              type="button"
              @click="goDestination(dest)"
            >
              <SafeImage :src="dest.img" :alt="dest.name" image-class="hero-gallery-img" />
              <div class="hero-image-caption">
                <span>{{ dest.tag }}</span>
                <strong>{{ dest.name }}</strong>
              </div>
            </button>
          </div>
        </div>

        <!-- 搜索卡片 -->
        <div class="search-card">
          <div class="search-card-head">
            <strong>快速出发</strong>
            <span>选择类型后输入目的地</span>
          </div>
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

          <div class="search-row search-row-traffic" v-if="searchTab === 'flight'">
            <div class="search-field">
              <span class="field-label">出发</span>
              <el-input
                v-model="flightForm.depCity"
                placeholder="出发城市"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <button
              class="search-swap"
              type="button"
              aria-label="交换出发和到达城市"
              @click="swapFlightCities"
            >
              <el-icon :size="18"><Right /></el-icon>
            </button>
            <div class="search-field">
              <span class="field-label">到达</span>
              <el-input
                v-model="flightForm.arrCity"
                placeholder="到达城市"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <div class="search-field">
              <span class="field-label">日期</span>
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

          <div class="search-row search-row-traffic" v-if="searchTab === 'train'">
            <div class="search-field">
              <span class="field-label">出发站</span>
              <el-input
                v-model="trainForm.depStation"
                placeholder="出发站"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <button
              class="search-swap"
              type="button"
              aria-label="交换出发站和到达站"
              @click="swapTrainStations"
            >
              <el-icon :size="18"><Right /></el-icon>
            </button>
            <div class="search-field">
              <span class="field-label">到达站</span>
              <el-input
                v-model="trainForm.arrStation"
                placeholder="到达站"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <div class="search-field">
              <span class="field-label">日期</span>
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

          <div class="search-row search-row-hotel" v-if="searchTab === 'hotel'">
            <div class="search-field">
              <span class="field-label">目的地</span>
              <el-input
                v-model="hotelForm.city"
                placeholder="目的城市"
                size="large"
                :prefix-icon="LocationFilled"
                class="search-input"
              />
            </div>
            <div class="search-field">
              <span class="field-label">入住时间</span>
              <el-date-picker
                v-model="hotelForm.dateRange"
                type="daterange"
                value-format="YYYY-MM-DD"
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

    <!-- ========== 安静规划步骤 ========== -->
    <section class="itinerary-section">
      <div class="itinerary-panel">
        <div class="itinerary-copy">
          <span class="section-kicker">ROUTE PREVIEW</span>
          <h2>先看一条完整路线，再决定怎么出发</h2>
          <p>
            TravelMate 会把城市、交通、住宿和每日节奏放在同一张路线里，避免只看到孤立的票务或酒店信息。
          </p>
          <div class="itinerary-meta">
            <span>{{ recommendedTrip.route }}</span>
            <span>{{ recommendedTrip.budget }}</span>
          </div>
          <div class="itinerary-tags">
            <span v-for="tag in recommendedTrip.tags" :key="tag">{{ tag }}</span>
          </div>
          <div class="itinerary-actions">
            <el-button type="primary" round @click="$router.push('/ai-plan')">
              <el-icon><MagicStick /></el-icon>
              生成我的行程
            </el-button>
            <el-button text type="primary" @click="$router.push('/destinations')">
              看更多城市 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </div>

        <div class="itinerary-board">
          <div class="itinerary-board-head">
            <span>{{ recommendedTrip.title }}</span>
            <strong>4 Days</strong>
          </div>
          <div class="itinerary-days">
            <div class="itinerary-day" v-for="item in recommendedTrip.days" :key="item.day">
              <span class="day-index">{{ item.day }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <small>{{ item.meta }}</small>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="retreat-section">
      <div class="retreat-panel">
        <div class="retreat-copy">
          <div class="retreat-heading">
            <span class="section-kicker">CALM PLANNING</span>
            <h2>把复杂行程，拆成三段安静的准备</h2>
          </div>
          <p>
            参考自然 retreat 的慢节奏，把票务、住宿和路线从拥挤流程里拆开。每一步都保留明确入口，也留出足够的呼吸感。
          </p>
          <div class="retreat-art-notes">
            <span>Tickets</span>
            <span>Stay</span>
            <span>AI Route</span>
          </div>
        </div>

        <div class="retreat-visual">
          <div class="retreat-image-wrap">
            <SafeImage
              :src="retreatPhoto"
              :fallback="seedImage('garden.svg')"
              image-class="retreat-photo"
              alt="安静旅行规划"
            />
            <div class="retreat-image-overlay"></div>
          </div>
          <div class="retreat-steps">
            <button class="retreat-step" type="button" @click="$router.push('/flight-search')">
              <span class="step-index">01</span>
              <span>
                <strong>先确定抵达方式</strong>
                <small>机票、火车票与价格趋势，先把大的时间框架定下来。</small>
              </span>
            </button>
            <button class="retreat-step" type="button" @click="$router.push('/hotel-search')">
              <span class="step-index">02</span>
              <span>
                <strong>再选择停留空间</strong>
                <small>酒店、房型、评分和位置，让每天醒来都接近想去的地方。</small>
              </span>
            </button>
            <button class="retreat-step" type="button" @click="$router.push('/ai-plan')">
              <span class="step-index">03</span>
              <span>
                <strong>最后交给 AI 编排</strong>
                <small>把偏好、预算和天数整理成一份可执行的慢行路线。</small>
              </span>
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- ========== 热门城市 ========== -->
    <section class="dest-section">
      <div class="section-header">
        <div>
          <h2 class="section-title">热门城市</h2>
          <p class="section-sub">精选热门旅行目的地，发现你的下一站</p>
        </div>
        <el-button text type="primary" @click="$router.push('/destinations')">
          查看全部 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="dest-grid">
        <button
          class="dest-card"
          type="button"
          v-for="(dest, idx) in hotDestinations"
          :key="dest.slug"
          :style="{ animationDelay: idx * 0.08 + 's' }"
          @click="goDestination(dest)"
        >
          <div class="dest-img-wrap">
            <SafeImage :src="dest.img" :alt="dest.name" image-class="dest-img" />
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
        </button>
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
        <button class="feature-card" type="button" @click="$router.push('/ai-plan')">
          <div class="feat-visual feat-visual-ai">
            <SafeImage :src="featurePhotos.ai" :fallback="seedImage('lake.svg')" image-class="feat-photo" alt="AI 智能规划" />
            <div class="feat-photo-overlay"></div>
            <span class="feat-number">01</span>
          </div>
          <div class="feat-body">
            <h3 class="feat-title">AI 智能规划</h3>
            <p class="feat-desc">一键生成专属行程方案，让你的旅行更轻松高效</p>
            <span class="feat-link">开始规划 <el-icon><ArrowRight /></el-icon></span>
          </div>
        </button>

        <button class="feature-card" type="button" @click="$router.push('/community')">
          <div class="feat-visual feat-visual-community">
            <SafeImage :src="featurePhotos.community" :fallback="seedImage('garden.svg')" image-class="feat-photo" alt="旅行社区" />
            <div class="feat-photo-overlay"></div>
            <span class="feat-number">02</span>
          </div>
          <div class="feat-body">
            <h3 class="feat-title">旅行社区</h3>
            <p class="feat-desc">分享你的旅途故事，发现更多旅行达人游记</p>
            <span class="feat-link">探索社区 <el-icon><ArrowRight /></el-icon></span>
          </div>
        </button>

        <button class="feature-card" type="button" @click="$router.push('/attractions')">
          <div class="feat-visual feat-visual-attraction">
            <SafeImage :src="featurePhotos.attraction" :fallback="seedImage('mountain.svg')" image-class="feat-photo" alt="景点门票" />
            <div class="feat-photo-overlay"></div>
            <span class="feat-number">03</span>
          </div>
          <div class="feat-body">
            <h3 class="feat-title">景点门票</h3>
            <p class="feat-desc">探索各地必打卡景点，在线购票免排队</p>
            <span class="feat-link">探索景点 <el-icon><ArrowRight /></el-icon></span>
          </div>
        </button>
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
          <div
            v-for="(avatar, idx) in travelerAvatars"
            :key="avatar.name"
            class="cta-avatar"
            :class="`cta-avatar-${idx + 1}`"
          >
            <SafeImage :src="avatar.src" :alt="avatar.name" />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from "vue";
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
import SafeImage from "@/components/SafeImage.vue";
import { destinations } from "@/data/destinations";
import { seedImage } from "@/utils/image";

const router = useRouter();
const searchTab = ref("flight");

const formatDate = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const addDays = (days) => {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return formatDate(date);
};

const flightForm = ref({ depCity: "", arrCity: "", date: addDays(1) });
const trainForm = ref({ depStation: "", arrStation: "", date: addDays(1) });
const hotelForm = ref({ city: "", dateRange: [addDays(1), addDays(3)] });

const hotSearches = ["北京", "上海", "三亚", "成都", "杭州", "西安"];
const heroPhoto =
  destinations.find((item) => item.slug === "guilin")?.img || seedImage("mountain.svg");

const showcaseDestinations = ["guilin", "hangzhou", "chengdu"]
  .map((slug) => destinations.find((item) => item.slug === slug))
  .filter(Boolean);

const featurePhotos = {
  ai: destinations.find((item) => item.slug === "hangzhou")?.img || seedImage("lake.svg"),
  community: destinations.find((item) => item.slug === "chengdu")?.img || seedImage("garden.svg"),
  attraction: destinations.find((item) => item.slug === "zhangjiajie")?.img || seedImage("mountain.svg"),
};

const retreatPhoto =
  destinations.find((item) => item.slug === "dali")?.img ||
  destinations.find((item) => item.slug === "hangzhou")?.img ||
  seedImage("garden.svg");

const stats = [
  {
    icon: Promotion,
    gradient: "linear-gradient(135deg, oklch(0.551 0.097 180), oklch(0.68 0.070 190))",
    num: 1280,
    label: "航线覆盖",
  },
  {
    icon: UserFilled,
    gradient: "linear-gradient(135deg, oklch(0.64 0.068 180), oklch(0.78 0.042 205))",
    num: 523600,
    label: "用户信赖",
  },
  {
    icon: Aim,
    gradient: "linear-gradient(135deg, oklch(0.72 0.052 190), oklch(0.56 0.090 180))",
    num: 99.9,
    label: "出票成功率 (%)",
    isDecimal: true,
  },
  {
    icon: ChatDotSquare,
    gradient: "linear-gradient(135deg, oklch(0.58 0.064 205), oklch(0.42 0.085 180))",
    num: 24,
    label: "小时客服在线",
  },
];

const hotDestinations = [...destinations]
  .sort(() => Math.random() - 0.5)
  .slice(0, 6);

const recommendedTrip = {
  title: "桂林山水慢行",
  route: "桂林 -> 阳朔 -> 龙脊梯田",
  budget: "人均约 2800 起",
  tags: ["江畔住宿", "轻徒步", "清晨游船"],
  days: [
    { day: "D1", title: "抵达桂林", meta: "象鼻山 · 两江四湖夜景" },
    { day: "D2", title: "漓江到阳朔", meta: "竹筏 · 西街 · 山景民宿" },
    { day: "D3", title: "骑行遇龙河", meta: "田园午餐 · 日落观景" },
    { day: "D4", title: "龙脊梯田返程", meta: "清晨梯田 · 高铁/航班衔接" },
  ],
};

const travelerAvatars = [
  { name: "旅行者头像 1", src: seedImage("avatar-1.svg") },
  { name: "旅行者头像 2", src: seedImage("avatar-2.svg") },
  { name: "旅行者头像 3", src: seedImage("avatar-3.svg") },
  { name: "旅行者头像 4", src: seedImage("avatar-4.svg") },
];

const swapFlightCities = () => {
  [flightForm.value.depCity, flightForm.value.arrCity] = [
    flightForm.value.arrCity,
    flightForm.value.depCity,
  ];
};

const swapTrainStations = () => {
  [trainForm.value.depStation, trainForm.value.arrStation] = [
    trainForm.value.arrStation,
    trainForm.value.depStation,
  ];
};

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
  router.push(`/destination/${dest.slug}`);
};

const quickSearch = (hint) => {
  if (searchTab.value === "flight") {
    flightForm.value.depCity = "上海";
    flightForm.value.arrCity = hint;
  } else if (searchTab.value === "train") {
    trainForm.value.depStation = "上海";
    trainForm.value.arrStation = hint;
  } else if (searchTab.value === "hotel") {
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
  margin: -30px calc(-1 * var(--tm-page-padding)) 0;
  background:
    linear-gradient(180deg, oklch(0.935 0.020 112) 0%, oklch(0.952 0.016 112) 44%, oklch(0.964 0.008 197) 100%);
  animation: fadeIn 0.55s ease;
}

/* ==================== Hero 区 ==================== */
.hero-section {
  position: relative;
  padding: 62px var(--tm-page-padding) 56px;
  overflow: hidden;
  min-height: min(820px, calc(100vh - 62px));
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    linear-gradient(180deg, oklch(0.935 0.020 112), oklch(0.952 0.016 112));
}

.hero-bg {
  display: none;
}

.hero-photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: saturate(0.86) contrast(0.96) brightness(1.04);
  object-position: center 48%;
  animation: natureDrift 24s ease-in-out infinite alternate;
}

@keyframes natureDrift {
  from {
    transform: scale(1);
  }
  to {
    transform: scale(1.045);
  }
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, oklch(0.985 0.002 248 / 0.97) 0%, oklch(0.964 0.008 197 / 0.88) 38%, oklch(0.935 0.018 180 / 0.38) 70%, oklch(0.78 0.036 180 / 0.10) 100%),
    linear-gradient(180deg, oklch(0.985 0.002 248 / 0.54), oklch(0.90 0.014 197 / 0.14));
}

.hero-content {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 0.72fr) minmax(460px, 1fr);
  align-items: center;
  gap: clamp(34px, 5vw, 78px);
  max-width: 1280px;
  width: 100%;
}

.hero-copy {
  max-width: 610px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: transparent;
  border: 0;
  border-radius: 0;
  padding: 0;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  color: var(--tm-bark);
  margin-bottom: 24px;
  animation: fadeInUp 0.55s ease;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--tm-olive);
  animation: pulse 3.5s ease infinite;
}

.hero-title {
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(58px, 6.5vw, 92px);
  font-weight: 500;
  color: var(--tm-ink);
  margin-bottom: 26px;
  line-height: 0.95;
  letter-spacing: 0;
  animation: fadeInUp 0.65s ease 0.05s both;
  text-shadow: none;
}

.title-gradient {
  display: block;
  color: var(--tm-olive);
  font-style: italic;
}

.hero-sub {
  max-width: 50ch;
  font-size: 17px;
  color: var(--tm-ink-soft);
  margin-bottom: 28px;
  line-height: 1.9;
  animation: fadeInUp 0.65s ease 0.1s both;
}

.hero-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-chips span {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  color: var(--tm-bark);
  background: oklch(0.985 0.002 248 / 0.72);
  border: 1px solid oklch(0.86 0.018 180 / 0.42);
  font-size: 13px;
  font-weight: 650;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.hero-actions :deep(.el-button) {
  height: 46px;
  padding: 0 22px;
  font-weight: 760;
}

.hero-secondary-btn {
  background: oklch(0.985 0.002 248 / 0.76);
  border-color: var(--tm-line);
  color: var(--tm-ink);
}

.hero-secondary-btn:hover {
  color: var(--tm-olive);
  border-color: var(--tm-olive);
  background: oklch(0.935 0.030 180);
}

.hero-gallery {
  display: grid;
  grid-template-columns: minmax(0, 0.96fr) minmax(180px, 0.72fr);
  gap: 16px;
  min-height: 560px;
}

.hero-mini-grid {
  display: grid;
  gap: 14px;
}

.hero-image-card {
  position: relative;
  overflow: hidden;
  border: 1px solid oklch(0.985 0.002 248 / 0.72);
  border-radius: 10px;
  padding: 0;
  min-height: 0;
  font: inherit;
  appearance: none;
  background: var(--tm-surface);
  color: oklch(0.985 0.002 248);
  cursor: pointer;
  box-shadow: 0 22px 54px oklch(0.239 0.006 180 / 0.13);
  isolation: isolate;
}

.hero-image-main {
  min-height: 560px;
}

.hero-gallery-img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  filter: saturate(0.94) brightness(1.02);
  transition: transform 0.7s ease, filter 0.7s ease;
}

.hero-image-card::after {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(180deg, transparent 40%, oklch(0.16 0.050 180 / 0.68)),
    linear-gradient(135deg, oklch(0.551 0.097 180 / 0.18), transparent 52%);
}

.hero-image-caption {
  position: absolute;
  left: 18px;
  bottom: 18px;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 6px;
  text-align: left;
}

.hero-image-card span {
  font-size: 12px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: 0.12em;
}

.hero-image-card strong {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 34px;
  font-weight: 500;
  line-height: 1;
}

.hero-mini-grid .hero-image-card strong {
  font-size: 22px;
}

.hero-image-card:hover .hero-gallery-img {
  transform: scale(1.055);
  filter: saturate(1) brightness(1.05);
}

/* 搜索卡片 */
.search-card {
  grid-column: 1 / -1;
  background: oklch(0.985 0.002 248);
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 20px 46px oklch(0.239 0.006 180 / 0.10);
  border: 1px solid oklch(0.86 0.012 112 / 0.78);
  animation: fadeInUp 0.65s ease 0.16s both;
  backdrop-filter: none;
}

.search-card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 10px;
}

.search-card-head strong {
  color: var(--tm-ink);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 24px;
  font-weight: 500;
}

.search-card-head span {
  color: var(--tm-muted);
  font-size: 12px;
  white-space: nowrap;
}

.hero-search-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.hero-search-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.hero-search-tabs :deep(.el-tabs__nav) {
  width: 100%;
  padding: 4px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  border-radius: 10px;
  background: oklch(0.964 0.008 197);
}

.hero-search-tabs :deep(.el-tabs__active-bar) {
  display: none;
}

.hero-search-tabs :deep(.el-tabs__item) {
  justify-content: center;
  padding: 0 12px;
  height: 38px;
  line-height: 38px;
  border-radius: 8px;
  color: var(--tm-ink-soft);
  font-size: 14px;
  font-weight: 750;
}

.hero-search-tabs :deep(.el-tabs__item.is-active) {
  color: oklch(0.985 0.002 248);
  background: var(--tm-olive);
  box-shadow: 0 10px 22px oklch(0.38 0.050 112 / 0.16);
}

.search-row {
  display: grid;
  align-items: center;
  gap: 12px;
}

.search-row-traffic {
  grid-template-columns: minmax(0, 1fr) 34px minmax(0, 1fr) minmax(150px, 0.72fr) auto;
}

.search-row-hotel {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1.35fr) auto;
}

.search-field {
  width: 100%;
  min-width: 0;
  padding: 9px 12px 11px;
  border-radius: 8px;
  background: oklch(0.985 0.002 248);
  border: 1px solid var(--tm-line);
}

.field-label {
  display: block;
  margin-bottom: 7px;
  color: var(--tm-muted);
  font-size: 12px;
  font-weight: 750;
}

.search-input :deep(.el-input__wrapper),
.search-date :deep(.el-input__wrapper) {
  height: 42px;
  border-radius: 8px;
  box-shadow: none;
  border: 1px solid var(--tm-line);
  background: oklch(0.995 0.004 197);
}
.search-input :deep(.el-input__wrapper:hover),
.search-date :deep(.el-input__wrapper:hover) {
  border-color: var(--tm-olive);
}

.search-swap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  border: 1px solid oklch(0.84 0.034 180);
  padding: 0;
  font: inherit;
  appearance: none;
  color: var(--tm-olive);
  background: oklch(0.935 0.030 180);
  cursor: pointer;
  transition: transform 0.22s ease, background 0.22s ease, border-color 0.22s ease;
}

.search-swap:hover,
.search-swap:focus-visible {
  transform: translateY(-1px);
  border-color: var(--tm-olive);
  background: oklch(0.965 0.008 197);
}

.search-btn {
  height: 64px;
  min-width: 126px;
  padding: 0 28px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 800;
  width: auto;
  color: oklch(0.985 0.002 248);
  background: var(--tm-olive);
  border-color: var(--tm-olive);
  box-shadow: 0 12px 24px oklch(0.42 0.085 180 / 0.14);
}

/* 热门搜索提示 */
.search-hints {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
  padding-top: 14px;
  border-top: 1px solid var(--tm-line);
}

.hint-label {
  font-size: 13px;
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}

.hint-tag {
  cursor: pointer;
  transition: all 0.2s ease;
  background: oklch(0.965 0.008 197);
  border-color: var(--tm-line);
  color: var(--tm-bark);
}
.hint-tag:hover {
  background: var(--tm-olive);
  color: oklch(0.96 0.018 82);
}

/* ==================== 统计数据 ==================== */
.stats-section {
  padding: 0 var(--tm-page-padding);
  margin-top: 0;
  position: relative;
  z-index: 2;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  background: transparent;
  border-radius: 0;
  padding: 26px 0 0;
  box-shadow: none;
  border: 0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  border: 1px solid oklch(0.86 0.012 112 / 0.62);
  border-radius: 10px;
  background: oklch(0.985 0.002 248 / 0.66);
}

.stat-icon-wrap {
  width: 52px;
  height: 52px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-el {
  color: oklch(0.985 0.002 248);
}

.stat-body {
  min-width: 0;
}

.stat-num {
  font-size: 24px;
  font-weight: 800;
  color: var(--tm-ink);
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: var(--tm-muted);
  font-weight: 500;
  margin-top: 2px;
}

/* ==================== 安静规划步骤 ==================== */
.itinerary-section {
  padding: 54px var(--tm-page-padding) 0;
}

.itinerary-panel {
  display: grid;
  grid-template-columns: minmax(0, 0.88fr) minmax(340px, 0.7fr);
  gap: clamp(28px, 5vw, 70px);
  align-items: center;
  padding: 52px;
  border: 1px solid var(--tm-line);
  min-height: 520px;
  border-radius: 10px;
  background:
    linear-gradient(135deg, oklch(0.985 0.002 248), oklch(0.950 0.018 112));
  box-shadow: var(--tm-shadow-card);
  overflow: hidden;
  position: relative;
}

.itinerary-panel::before {
  content: "";
  position: absolute;
  right: -120px;
  top: -120px;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: oklch(0.935 0.030 180 / 0.46);
  filter: blur(6px);
}

.itinerary-copy {
  position: relative;
  z-index: 1;
}

.itinerary-copy h2 {
  max-width: 10em;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(36px, 4vw, 56px);
  font-weight: 500;
  line-height: 1.04;
  color: var(--tm-ink);
  margin-bottom: 18px;
}

.itinerary-copy p {
  max-width: 58ch;
  color: var(--tm-muted);
  font-size: 16px;
  line-height: 1.9;
}

.itinerary-meta,
.itinerary-tags,
.itinerary-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.itinerary-meta {
  gap: 12px;
  margin-top: 22px;
  color: var(--tm-bark);
  font-weight: 760;
}

.itinerary-meta span {
  padding: 8px 13px;
  border-radius: 999px;
  background: oklch(0.985 0.002 248 / 0.72);
  border: 1px solid var(--tm-line);
}

.itinerary-tags {
  gap: 8px;
  margin-top: 14px;
}

.itinerary-tags span {
  padding: 6px 12px;
  border-radius: 999px;
  color: var(--tm-muted);
  background: oklch(0.985 0.002 248 / 0.56);
  border: 1px solid var(--tm-line-soft);
  font-size: 13px;
  font-weight: 650;
}

.itinerary-actions {
  gap: 12px;
  margin-top: 28px;
}

.itinerary-board {
  position: relative;
  z-index: 1;
  padding: 26px;
  border-radius: 10px;
  background: oklch(0.985 0.002 248 / 0.86);
  border: 1px solid var(--tm-line);
  box-shadow: 0 24px 52px oklch(0.239 0.006 180 / 0.10);
}

.itinerary-board-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 20px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--tm-line);
}

.itinerary-board-head span {
  color: var(--tm-muted);
  font-size: 13px;
  font-weight: 760;
  letter-spacing: 0.06em;
}

.itinerary-board-head strong {
  color: var(--tm-olive);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 32px;
  font-weight: 500;
  line-height: 1;
}

.itinerary-days {
  display: grid;
  gap: 4px;
}

.itinerary-day {
  display: grid;
  grid-template-columns: 48px 1fr;
  gap: 14px;
  align-items: start;
  padding: 16px 0;
  border-bottom: 1px solid var(--tm-line-soft);
}

.itinerary-day:last-child {
  border-bottom: 0;
}

.day-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--tm-primary-soft);
  color: var(--tm-olive);
  font-weight: 800;
}

.itinerary-day strong {
  display: block;
  color: var(--tm-ink);
  font-size: 16px;
  margin-bottom: 4px;
}

.itinerary-day small {
  color: var(--tm-muted);
  line-height: 1.6;
}

.retreat-section {
  padding: 24px var(--tm-page-padding) 12px;
}

.retreat-panel {
  display: grid;
  grid-template-columns: minmax(0, 0.88fr) minmax(340px, 0.7fr);
  gap: clamp(28px, 5vw, 70px);
  align-items: center;
  padding: 52px;
  border: 1px solid var(--tm-line);
  min-height: 520px;
  border-radius: 10px;
  background:
    linear-gradient(135deg, oklch(0.985 0.002 248), oklch(0.950 0.018 112));
  box-shadow: var(--tm-shadow-card);
  overflow: hidden;
  position: relative;
}

.retreat-panel::before {
  content: "";
  position: absolute;
  left: -110px;
  bottom: -120px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: oklch(0.935 0.030 180 / 0.42);
  filter: blur(8px);
}

.retreat-copy {
  position: relative;
  z-index: 1;
}

.retreat-heading {
  min-width: 0;
}

.section-kicker {
  display: inline-flex;
  margin-bottom: 18px;
  color: var(--tm-olive);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.retreat-copy h2 {
  max-width: 10.4em;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(36px, 4vw, 56px);
  font-weight: 500;
  line-height: 1.04;
  color: var(--tm-ink);
  margin: 0 0 18px;
}

.retreat-copy p {
  max-width: 58ch;
  margin: 0;
  color: var(--tm-muted);
  font-size: 16px;
  line-height: 1.85;
}

.retreat-art-notes {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 24px;
}

.retreat-art-notes span {
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid var(--tm-line);
  background: oklch(0.985 0.002 248 / 0.68);
  color: var(--tm-bark);
  font-size: 12px;
  font-weight: 760;
}

.retreat-visual {
  position: relative;
  z-index: 1;
  min-height: 380px;
  overflow: hidden;
  border-radius: 10px;
}

.retreat-image-wrap {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border-radius: 10px;
  border: 1px solid oklch(0.985 0.002 248 / 0.72);
  box-shadow: 0 24px 52px oklch(0.239 0.006 180 / 0.12);
}

.retreat-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: saturate(0.82) contrast(0.96) brightness(1.06);
  transition: transform 0.7s ease, filter 0.7s ease;
}

.retreat-panel:hover .retreat-photo {
  transform: scale(1.035);
  filter: saturate(0.9) contrast(0.98) brightness(1.08);
}

.retreat-image-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, oklch(0.985 0.002 248 / 0.08), oklch(0.18 0.050 180 / 0.30)),
    linear-gradient(135deg, oklch(0.935 0.030 180 / 0.28), transparent 52%);
}

.retreat-steps {
  position: absolute;
  right: 18px;
  bottom: 18px;
  width: min(calc(100% - 36px), 360px);
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.retreat-step {
  width: 100%;
  display: grid;
  grid-template-columns: 46px 1fr;
  align-items: start;
  gap: 14px;
  min-height: 0;
  padding: 16px;
  border: 1px solid var(--tm-line);
  border-radius: 8px;
  background: oklch(0.985 0.002 248 / 0.94);
  color: inherit;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 16px 34px oklch(0.239 0.006 180 / 0.09);
  backdrop-filter: blur(10px);
  transition: transform 0.22s ease, border-color 0.22s ease, background 0.22s ease, box-shadow 0.22s ease;
}

.retreat-step:hover {
  transform: translateY(-3px);
  border-color: var(--tm-olive);
  background: oklch(0.964 0.008 197);
  box-shadow: 0 18px 42px oklch(0.239 0.006 180 / 0.08);
}

.step-index {
  grid-row: auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: var(--tm-primary-soft);
  color: var(--tm-olive);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 24px;
  line-height: 1;
}

.retreat-step strong {
  display: block;
  color: var(--tm-ink);
  font-size: 16px;
  font-weight: 760;
  margin-bottom: 4px;
}

.retreat-step small {
  display: block;
  color: var(--tm-muted);
  font-size: 13px;
  line-height: 1.55;
}

/* ==================== Section 通用 ==================== */
.section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 32px;
  gap: 16px;
}

.section-title {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 42px;
  font-weight: 500;
  color: var(--tm-ink);
  margin-bottom: 10px;
  letter-spacing: 0;
}

.dest-section .section-title::before,
.feature-section .section-title::before {
  display: block;
  margin-bottom: 10px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  color: var(--tm-olive);
}

.dest-section .section-title::before {
  content: "DESTINATIONS";
}

.feature-section .section-title::before {
  content: "SERVICES";
}

.section-sub {
  font-size: 15px;
  color: var(--tm-muted);
  line-height: 1.8;
}

/* ==================== 热门城市 ==================== */
.dest-section {
  padding: 78px var(--tm-page-padding) 0;
}

.dest-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.dest-card {
  background: var(--tm-surface);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--tm-line-soft);
  padding: 0;
  font: inherit;
  color: inherit;
  text-align: left;
  appearance: none;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  animation: fadeInUp 0.5s ease both;
  box-shadow: var(--tm-shadow-card);
}
.dest-card:hover {
  transform: translateY(-6px);
  border-color: var(--el-color-primary-light-7);
  box-shadow: var(--tm-shadow-card-hover);
}

.dest-img-wrap {
  position: relative;
  overflow: hidden;
  height: 220px;
}

.dest-card:first-child {
  grid-column: auto;
}

.dest-card:first-child .dest-img-wrap {
  height: 220px;
}

.dest-card:first-child .dest-city {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 22px;
  font-weight: 500;
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
  background: linear-gradient(to top, oklch(0.18 0.050 180 / 0.64) 0%, oklch(0.32 0.050 180 / 0.18) 58%, transparent 100%);
  opacity: 0.78;
  transition: opacity 0.4s ease;
}
.dest-card:hover .dest-gradient {
  opacity: 1;
}

.dest-badge {
  position: absolute;
  top: 14px;
  right: 14px;
  background: oklch(0.985 0.002 248 / 0.92);
  color: var(--tm-bark);
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
  opacity: 1;
  transform: translateY(0);
  transition: all 0.4s ease;
}
.dest-card:hover .dest-img-info {
  opacity: 1;
  transform: translateY(0);
}

.dest-city {
  font-size: 18px;
  font-weight: 700;
  color: oklch(0.985 0.002 248);
  text-shadow: 0 1px 4px oklch(0.18 0.050 180 / 0.32);
}

.dest-country {
  font-size: 12px;
  color: oklch(0.935 0.012 197 / 0.90);
  text-shadow: 0 1px 3px oklch(0.18 0.050 180 / 0.32);
}

.dest-body {
  padding: 14px;
}

.dest-desc {
  font-size: 14px;
  color: var(--tm-muted);
  margin-bottom: 10px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.dest-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* ==================== 功能入口 ==================== */
.feature-section {
  padding: 78px var(--tm-page-padding) 0;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.feature-card {
  background: var(--tm-surface);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--tm-line-soft);
  padding: 0;
  font: inherit;
  color: inherit;
  text-align: left;
  appearance: none;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--tm-shadow-card);
}

.feature-card:first-child {
  display: block;
  grid-column: auto;
}

.feature-card:first-child .feat-visual {
  height: 180px;
  min-height: 0;
}

.feature-card:first-child .feat-body {
  text-align: center;
  display: block;
}
.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--tm-shadow-card-hover);
}

.feat-visual {
  height: 180px;
  position: relative;
  overflow: hidden;
  background: oklch(0.964 0.008 197);
}

.feat-visual-ai {
  background: oklch(0.955 0.014 197);
}
.feat-visual-community {
  background: oklch(0.965 0.008 197);
}
.feat-visual-attraction {
  background: oklch(0.955 0.018 180);
}

.feat-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  filter: saturate(0.82) brightness(1.05);
  transition: transform 0.55s ease, filter 0.55s ease;
}

.feat-photo-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, oklch(0.985 0.002 248 / 0.12), oklch(0.18 0.050 180 / 0.34)),
    linear-gradient(135deg, oklch(0.964 0.008 197 / 0.34), transparent 48%);
  pointer-events: none;
}

.feat-number {
  position: absolute;
  left: 16px;
  top: 16px;
  z-index: 2;
  min-width: 46px;
  height: 30px;
  padding: 0 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 1px solid oklch(0.985 0.002 248 / 0.56);
  background: oklch(0.985 0.002 248 / 0.24);
  color: oklch(0.985 0.002 248);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 15px;
  letter-spacing: 0;
  backdrop-filter: blur(10px);
}

.feature-card:hover .feat-photo {
  transform: scale(1.045);
  filter: saturate(0.9) brightness(1.08);
}

.feat-body {
  padding: 24px;
  text-align: center;
}

.feat-title {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 24px;
  font-weight: 500;
  color: var(--tm-ink);
  margin-bottom: 8px;
}

.feat-desc {
  font-size: 14px;
  color: var(--tm-muted);
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
  padding: 78px var(--tm-page-padding) 0;
}

.cta-card {
  background: linear-gradient(135deg, oklch(0.985 0.002 248), oklch(0.964 0.008 197));
  border: 1px solid var(--tm-line);
  border-radius: 8px;
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
  font-family: Georgia, "Times New Roman", serif;
  font-size: 38px;
  font-weight: 500;
  color: var(--tm-ink);
  margin-bottom: 10px;
}

.cta-sub {
  font-size: 16px;
  color: var(--tm-muted);
  margin-bottom: 24px;
}

.cta-card :deep(.el-button--primary) {
  background: oklch(0.985 0.002 248);
  border-color: oklch(0.76 0.045 180);
  color: var(--tm-bark);
  box-shadow: 0 12px 24px oklch(0.38 0.050 112 / 0.10);
}

.cta-card :deep(.el-button--primary:hover) {
  background: oklch(0.935 0.030 180);
  border-color: var(--tm-olive);
  color: var(--tm-bark);
}

.cta-decoration {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  min-width: 310px;
  min-height: 108px;
  justify-content: center;
}

.cta-avatar {
  width: 78px;
  height: 78px;
  border-radius: 50%;
  border: 4px solid oklch(0.985 0.002 248 / 0.88);
  background: oklch(0.985 0.002 248 / 0.58);
  box-shadow: 0 18px 34px oklch(0.24 0.026 82 / 0.10);
  overflow: hidden;
  margin-left: -12px;
  animation: float 4s ease infinite;
  transition: transform 0.25s ease;
}

.cta-avatar:first-child {
  margin-left: 0;
}

.cta-avatar:hover {
  transform: translateY(-6px) scale(1.04);
}

.cta-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cta-avatar-2 { animation-delay: -1s; }
.cta-avatar-3 { animation-delay: -2s; }
.cta-avatar-4 { animation-delay: -3s; }

/* ==================== 响应式 ==================== */
@media (max-width: 992px) {
  .hero-content {
    grid-template-columns: 1fr;
    gap: 28px;
  }
  .hero-copy {
    max-width: 520px;
  }
  .hero-gallery {
    min-height: 360px;
    grid-template-columns: minmax(0, 1fr) minmax(180px, 0.82fr);
  }
  .hero-image-main {
    min-height: 360px;
  }
  .search-row-traffic,
  .search-row-hotel {
    grid-template-columns: 1fr;
  }
  .search-swap {
    display: none;
  }
  .search-btn {
    width: 100%;
    height: 54px;
  }
  .retreat-section {
    grid-template-columns: 1fr;
    gap: 28px;
  }
  .itinerary-panel {
    grid-template-columns: 1fr;
    min-height: auto;
    padding: 40px;
  }
  .retreat-panel {
    grid-template-columns: 1fr;
    min-height: auto;
    padding: 40px;
  }
  .retreat-visual {
    min-height: 430px;
  }
  .retreat-steps {
    width: min(92%, 420px);
  }
  .retreat-copy h2 {
    max-width: 12em;
    font-size: 42px;
  }
  .retreat-copy p {
    max-width: 64ch;
  }
  .dest-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .dest-card:first-child {
    grid-column: auto;
  }
  .feature-grid {
    grid-template-columns: 1fr 1fr;
  }
  .feature-card:first-child {
    grid-column: auto;
  }
  .feature-card:first-child .feat-visual {
    height: 180px;
    min-height: 0;
  }
  .feature-grid .feature-card:last-child {
    grid-column: 1 / -1;
  }
  .hero-title {
    font-size: 44px;
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 680px) {
  .hero-content {
    grid-template-columns: 1fr;
  }
  .hero-gallery {
    grid-template-columns: 1fr;
    min-height: 0;
  }
  .hero-mini-grid {
    grid-template-columns: 1fr 1fr;
  }
  .hero-image-main {
    min-height: 300px;
  }
  .hero-image-card {
    min-height: 160px;
  }
}

@media (max-width: 640px) {
  .home-page {
    margin-top: -30px;
  }
  .hero-section {
    padding: 42px 16px 48px;
    min-height: auto;
    align-items: flex-start;
  }
  .hero-overlay {
    background:
      linear-gradient(180deg, oklch(0.985 0.002 248 / 0.96), oklch(0.964 0.008 197 / 0.88) 56%, oklch(0.82 0.026 180 / 0.24));
  }
  .hero-content {
    gap: 24px;
  }
  .hero-title {
    font-size: 42px;
  }
  .hero-sub {
    font-size: 15px;
    margin-bottom: 18px;
  }
  .hero-chips {
    display: none;
  }
  .hero-actions {
    margin-top: 18px;
  }
  .hero-actions :deep(.el-button) {
    width: 100%;
  }
  .search-card {
    padding: 18px 16px 16px;
    border-radius: 12px;
  }
  .search-card-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 2px;
  }
  .hero-search-tabs :deep(.el-tabs__item) {
    padding: 0 12px;
    font-size: 14px;
  }
  .dest-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .itinerary-section {
    padding: 50px 16px 0;
  }
  .itinerary-panel {
    padding: 28px 20px;
  }
  .itinerary-copy h2 {
    font-size: 34px;
  }
  .itinerary-board {
    padding: 20px;
  }
  .itinerary-board-head {
    flex-direction: column;
    gap: 8px;
  }
  .itinerary-day {
    grid-template-columns: 42px 1fr;
  }
  .retreat-section {
    padding: 22px 16px 0;
  }
  .retreat-panel {
    padding: 28px 20px;
  }
  .retreat-visual {
    min-height: 0;
    display: grid;
    gap: 12px;
  }
  .retreat-image-wrap {
    position: relative;
    inset: auto;
    height: 240px;
    width: 100%;
  }
  .retreat-steps {
    position: relative;
    right: auto;
    bottom: auto;
    width: 100%;
  }
  .retreat-copy h2 {
    font-size: 34px;
  }
  .retreat-step {
    grid-template-columns: 40px 1fr;
    min-height: 0;
    padding: 14px;
  }
  .step-index {
    width: 40px;
    height: 40px;
    font-size: 22px;
  }
  .dest-section,
  .feature-section,
  .cta-section {
    padding-top: 58px;
  }
  .section-title {
    font-size: 32px;
  }
  .dest-card:first-child {
    grid-column: auto;
  }
  .dest-card:first-child .dest-img-wrap,
  .dest-img-wrap {
    height: 220px;
  }
  .dest-card:first-child .dest-city {
    font-size: 24px;
  }
  .feature-grid {
    grid-template-columns: 1fr;
    gap: 14px;
  }
  .feature-card:first-child {
    grid-column: auto;
  }
  .feature-grid .feature-card:last-child {
    grid-column: auto;
  }
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 20px 16px;
  }
  .stat-item {
    padding: 8px;
  }
  .stat-icon-wrap {
    width: 46px;
    height: 46px;
  }
  .stat-num {
    font-size: 22px;
  }
  .cta-card {
    flex-direction: column;
    text-align: center;
    padding: 36px 24px;
  }
  .cta-decoration {
    min-width: 0;
    min-height: 76px;
  }
  .cta-avatar {
    width: 58px;
    height: 58px;
    margin-left: -8px;
  }
  .cta-title {
    font-size: 22px;
  }
}
</style>
