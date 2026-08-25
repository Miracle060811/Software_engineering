<template>
  <div class="home-page">
    <section class="hero-section">
      <div class="hero-bg">
        <SafeImage
          :src="heroPhoto"
          :fallback="FALLBACK_IMAGE"
          image-class="hero-photo-img"
          alt="桂林漓江山水"
          loading="eager"
          fetchpriority="high"
          sizes="100vw"
        />
        <div class="hero-overlay"></div>
      </div>

      <div class="hero-content">
        <div class="hero-main">
          <div class="hero-copy">
            <div class="hero-badge">
              <span class="badge-dot"></span>
              TRAVELMATE ATLAS
            </div>
            <h1 class="hero-title">
              把远方，
              <span class="title-gradient">排成明天的路。</span>
            </h1>
            <p class="hero-sub">
              把真实笔记、季节、预算和兴趣放在同一张地图上。TravelMate 不替你赶路，只把散落的信息整理成一条可出发、也有余地的旅程。
            </p>
            <div class="hero-actions">
              <el-button type="primary" size="large" round @click="$router.push('/ai-plan')">
                开始整理路线
              </el-button>
              <el-button size="large" round class="hero-secondary-btn" @click="$router.push('/destinations')">
                翻阅目的地
              </el-button>
            </div>
          </div>

          <aside class="route-preview" aria-label="桂林山水慢行路线预览">
            <div class="route-preview-head">
              <span>ROUTE PREVIEW</span>
              <strong>桂林 · 3天</strong>
            </div>
            <h2>{{ recommendedTrip.title }}</h2>
            <button class="route-cover" type="button" @click="$router.push('/destination/guilin')">
              <SafeImage
                :src="routePreviewPhoto"
                :fallback="FALLBACK_IMAGE"
                alt="桂林山水路线"
                sizes="(max-width: 900px) 100vw, 34vw"
              />
              <span>漓江 · 阳朔 · 龙脊梯田</span>
            </button>
            <div class="route-days">
              <div v-for="item in recommendedTrip.days" :key="item.day" class="route-day">
                <span>{{ item.day }}</span>
                <strong>{{ item.title }}</strong>
              </div>
            </div>
            <el-button class="route-preview-action" type="primary" @click="$router.push('/ai-plan')">
              生成我的行程 <span aria-hidden="true">→</span>
            </el-button>
          </aside>
        </div>

        <div class="route-planner">
          <div class="planner-overview">
            <div class="planner-steps" aria-label="路线规划步骤">
              <div v-for="step in plannerSteps" :key="step.index" class="planner-step">
                <span class="planner-index">{{ step.index }}</span>
                <span class="planner-divider" aria-hidden="true"></span>
                <span class="planner-copy">
                  <strong>{{ step.title }}</strong>
                  <small>{{ step.subtitle }}</small>
                </span>
              </div>
            </div>
            <el-button class="planner-primary" type="primary" @click="$router.push('/ai-plan')">
              开始规划 <span aria-hidden="true">→</span>
            </el-button>
          </div>

          <el-tabs v-model="searchTab" class="hero-search-tabs">
            <el-tab-pane name="flight" label="机票" />
            <el-tab-pane name="train" label="火车票" />
            <el-tab-pane name="hotel" label="酒店" />
          </el-tabs>

          <div v-if="searchTab === 'flight'" class="search-row search-row-traffic">
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

          <div v-if="searchTab === 'train'" class="search-row search-row-traffic">
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

          <div v-if="searchTab === 'hotel'" class="search-row search-row-hotel">
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

          <div class="search-hints">
            <span class="hint-label">热门搜索：</span>
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

    <section class="stats-section">
      <div class="stats-grid">
        <div class="stat-item" v-for="stat in stats" :key="stat.label">
          <div class="stat-icon-wrap">
            <span class="stat-index">{{ stat.index }}</span>
          </div>
          <div class="stat-body">
            <CountUp :target="stat.num" class="stat-num" />
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <section v-if="recentHistory.length" class="history-section">
      <div class="section-header history-header">
        <div>
          <h2 class="section-title">最近浏览</h2>
          <p class="section-sub">快速回到刚看过的酒店、景点和游记</p>
        </div>
      </div>
      <div class="history-list">
        <button
          v-for="item in recentHistory"
          :key="`${item.type}-${item.id}`"
          class="history-card"
          type="button"
          @click="router.push(item.path)"
        >
          <span class="history-type">{{ historyTypeLabel(item.type) }}</span>
          <strong>{{ item.title }}</strong>
          <small>{{ item.subtitle }}</small>
        </button>
      </div>
    </section>

    <!-- ========== 热门城市 ========== -->
    <section class="dest-section editorial-section">
      <div class="section-header editorial-header">
        <div>
          <span class="section-kicker">DESTINATIONS</span>
          <h2 class="section-title">值得停留的城市</h2>
          <p class="section-sub">从山水、老街到海岸，把下一站交给真实风景。</p>
        </div>
        <el-button class="section-link-btn" text type="primary" @click="$router.push('/destinations')">
          查看全部城市 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <div class="dest-grid editorial-dest-grid">
        <button
          class="dest-card editorial-dest-card"
          type="button"
          v-for="(dest, idx) in hotDestinations"
          :key="dest.slug"
          :class="{ 'is-featured': idx === 0 }"
          @click="goDestination(dest)"
        >
          <div class="dest-img-wrap">
            <SafeImage
              :src="dest.img"
              :alt="dest.name"
              image-class="dest-img"
              sizes="(max-width: 768px) 100vw, 33vw"
            />
            <div class="dest-gradient"></div>
            <div class="dest-badge">{{ dest.tag }}</div>
            <div class="dest-img-info">
              <span class="dest-city">{{ dest.name }}</span>
              <span class="dest-country">{{ dest.country }}</span>
            </div>
          </div>
          <div class="dest-body">
            <div class="dest-copy-head">
              <span>{{ String(idx + 1).padStart(2, "0") }}</span>
              <strong>{{ dest.keywords?.[0] || dest.tag }}</strong>
            </div>
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
    <section class="feature-section editorial-section">
      <div class="section-header editorial-header">
        <div>
          <span class="section-kicker">SERVICES</span>
          <h2 class="section-title">出发前，把事情安排妥当</h2>
          <p class="section-sub">路线、交通、住宿和门票集中整理，少一点反复切换，多一点从容出发。</p>
        </div>
      </div>

      <div class="feature-grid editorial-feature-grid">
        <button
          v-for="card in serviceCards"
          :key="card.title"
          class="feature-card editorial-feature-card"
          type="button"
          @click="$router.push(card.path)"
        >
          <div class="feat-visual">
            <SafeImage
              :src="card.photo"
              :fallback="FALLBACK_IMAGE"
              image-class="feat-photo"
              :alt="card.title"
              sizes="(max-width: 768px) 100vw, 33vw"
            />
            <div class="feat-photo-overlay"></div>
            <span class="feat-number">{{ card.index }}</span>
          </div>
          <div class="feat-body">
            <span class="feat-label">{{ card.label }}</span>
            <h3 class="feat-title">{{ card.title }}</h3>
            <p class="feat-desc">{{ card.desc }}</p>
            <span class="feat-link">{{ card.action }} <el-icon><ArrowRight /></el-icon></span>
          </div>
        </button>
      </div>
    </section>

    <!-- ========== CTA Banner ========== -->
    <section class="cta-section editorial-section">
      <div class="cta-card editorial-cta-card">
        <div class="cta-content">
          <span class="section-kicker">PLAN YOUR NEXT TRIP</span>
          <h2 class="cta-title">下一次出发，从一条清楚的路线开始</h2>
          <p class="cta-sub">告诉 TravelMate 你的时间、预算和想看的风景，我们帮你整理成可出发的行程。</p>
          <div class="cta-actions">
            <el-button type="primary" size="large" round @click="$router.push('/ai-plan')">
              开始规划路线 <span aria-hidden="true">→</span>
            </el-button>
            <el-button size="large" round @click="$router.push('/community')">
              先看旅行笔记
            </el-button>
          </div>
        </div>
        <div class="cta-route-note" aria-label="路线整理方式">
          <div v-for="item in ctaNotes" :key="item.title" class="cta-note-item">
            <span>{{ item.index }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <small>{{ item.desc }}</small>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  Search,
  Right,
  LocationFilled,
  Calendar,
  ArrowRight,
} from "@element-plus/icons-vue";
import CountUp from "../components/CountUp.vue";
import SafeImage from "@/components/SafeImage.vue";
import { destinations } from "@/data/destinations";
import { FALLBACK_IMAGE } from "@/utils/image";
import { getBrowseHistory } from "@/utils/browseHistory";

const router = useRouter();
const searchTab = ref("flight");
const recentHistory = ref([]);

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
const heroPhoto = "/images/editorial/li-river-sunset-v82.jpg";
const routePreviewPhoto = "/images/editorial/route-card-v82.jpg";

const featurePhotos = {
  route: "/images/editorial/route-card-v82.jpg",
  community: "/images/editorial/street-card-v82.jpg",
  attraction: "/images/editorial/guilin-cinematic-v82.jpg",
  traffic: "/images/editorial/train-card-v82.jpg",
  hotel: "/images/editorial/coffee-card-v82.jpg",
  coupon: "/images/editorial/guilin-cinematic-v82.jpg",
};

const historyTypeLabel = (type) =>
  ({ hotel: "酒店", attraction: "景点", post: "游记" }[type] || "记录");

onMounted(() => {
  recentHistory.value = getBrowseHistory().slice(0, 4);
});

const stats = [
  {
    index: "01",
    num: 1280,
    label: "航线覆盖",
  },
  {
    index: "02",
    num: 523600,
    label: "用户信赖",
  },
  {
    index: "03",
    num: 99.9,
    label: "出票成功率 (%)",
    isDecimal: true,
  },
  {
    index: "04",
    num: 24,
    label: "小时客服在线",
  },
];

const hotDestinations = destinations.slice(0, 6);

const serviceCards = [
  {
    index: "01",
    label: "行程",
    title: "定制你的旅行路线",
    desc: "输入时间、预算和偏好，整理出更适合你的每日安排。",
    action: "开始规划",
    path: "/ai-plan",
    photo: featurePhotos.route,
  },
  {
    index: "02",
    label: "交通",
    title: "把路程接顺",
    desc: "机票、火车与本地交通统一查看，减少来回切换。",
    action: "查看交通",
    path: "/flight-search",
    photo: featurePhotos.traffic,
  },
  {
    index: "03",
    label: "住宿",
    title: "住在合适的位置",
    desc: "按路线节奏选择住宿区域，让每天少绕路。",
    action: "挑选住宿",
    path: "/hotel-search",
    photo: featurePhotos.hotel,
  },
  {
    index: "04",
    label: "门票",
    title: "提前安排想去的地方",
    desc: "把热门景点、开放时间和票务信息放进同一份行程。",
    action: "查看景点",
    path: "/attractions",
    photo: featurePhotos.attraction,
  },
  {
    index: "05",
    label: "笔记",
    title: "看看真实出发的人怎么走",
    desc: "从别人的路线、照片和避坑里找到自己的灵感。",
    action: "翻阅笔记",
    path: "/community",
    photo: featurePhotos.community,
  },
  {
    index: "06",
    label: "权益",
    title: "出发前看看可用优惠",
    desc: "把交通、住宿和门票优惠集中整理，能省则省。",
    action: "查看优惠",
    path: "/coupons",
    photo: featurePhotos.coupon,
  },
];

const ctaNotes = [
  { index: "01", title: "告诉我们时间", desc: "出发日期、天数和同行人数。" },
  { index: "02", title: "选择旅行偏好", desc: "美食、海岸、老街或轻徒步。" },
  { index: "03", title: "带着路线出发", desc: "交通、住宿和门票一起查看。" },
];

const recommendedTrip = {
  title: "桂林山水慢行",
  route: "桂林 -> 阳朔 -> 龙脊梯田",
  budget: "人均约 2800 起",
  tags: ["江畔住宿", "轻徒步", "清晨游船"],
  days: [
    { day: "D1", title: "抵达后慢慢进入城市", meta: "象鼻山 · 两江四湖夜景" },
    { day: "D2", title: "山水主线 + 老街补白", meta: "竹筏 · 西街 · 山景民宿" },
    { day: "D3", title: "轻徒步与从容返程", meta: "清晨梯田 · 高铁/航班衔接" },
  ],
};

const plannerSteps = [
  { index: "01", title: "从哪里出发", subtitle: "选择出发城市" },
  { index: "02", title: "想抵达哪里", subtitle: "输入目的地或灵感" },
  { index: "03", title: "什么时候出发", subtitle: "选择日期与天数" },
  { index: "04", title: "旅程偏好", subtitle: "主题 / 预算 / 节奏" },
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
  max-width: 560px;
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
  font-size: clamp(62px, 5.7vw, 86px);
  font-weight: 500;
  color: var(--tm-ink);
  margin-bottom: 30px;
  line-height: 0.98;
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
  margin-bottom: 34px;
  line-height: 1.9;
  animation: fadeInUp 0.65s ease 0.1s both;
}

.hero-chips {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  align-items: flex-start;
  gap: 0;
  padding-top: 20px;
  border-top: 1px solid oklch(0.78 0.018 100 / 0.62);
}

.hero-chips span {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  min-width: 0;
  padding: 0 12px;
  box-sizing: border-box;
  color: var(--tm-ink);
  background: transparent;
  border: 0;
  border-radius: 0;
  font-size: 19px;
  font-weight: 760;
  white-space: nowrap;
}

.hero-chips span::after {
  content: "";
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--tm-olive);
}

.hero-chips span + span::before {
  content: "";
  position: absolute;
  left: 0;
  top: 2px;
  width: 1px;
  height: 18px;
  background: oklch(0.74 0.018 100 / 0.72);
  transform: rotate(14deg);
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

.history-section {
  padding: 0 var(--tm-page-padding);
  margin-top: 52px;
  position: relative;
  z-index: 2;
}
.history-header {
  margin-bottom: 18px;
}
.history-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}
.history-card {
  min-height: 120px;
  padding: 18px;
  border: 1px solid var(--tm-line-soft);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(241, 253, 250, 0.92)),
    radial-gradient(circle at 92% 12%, rgba(20, 184, 166, 0.12), transparent 34%);
  box-shadow: 0 16px 36px rgba(36, 96, 92, 0.08);
  cursor: pointer;
  text-align: left;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}
.history-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 22px 44px rgba(36, 96, 92, 0.14);
}
.history-type {
  display: inline-flex;
  margin-bottom: 12px;
  padding: 4px 10px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}
.history-card strong {
  display: block;
  color: var(--tm-ink);
  font-size: 16px;
  line-height: 1.45;
  margin-bottom: 6px;
}
.history-card small {
  color: var(--tm-muted);
  font-size: 13px;
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
  display: flex;
  justify-content: center;
}

.itinerary-panel {
  width: min(100%, 1320px);
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(500px, 0.98fr);
  gap: clamp(40px, 4.8vw, 70px);
  align-items: center;
  padding: clamp(42px, 4vw, 58px);
  border: 1px solid var(--tm-line);
  min-height: 500px;
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
  padding: 34px var(--tm-page-padding) 18px;
  display: flex;
  justify-content: center;
}

.retreat-panel {
  width: min(100%, 1320px);
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(500px, 0.98fr);
  gap: clamp(40px, 4.8vw, 70px);
  align-items: center;
  padding: clamp(42px, 4vw, 58px);
  border: 1px solid var(--tm-line);
  min-height: 500px;
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
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 560px;
  min-height: 390px;
}

.retreat-heading {
  min-width: 0;
}

.section-kicker {
  display: inline-flex;
  margin-bottom: 16px;
  color: var(--tm-olive);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.retreat-copy h2 {
  max-width: 8.2em;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(36px, 3.5vw, 50px);
  font-weight: 500;
  line-height: 1.1;
  color: var(--tm-ink);
  margin: 0 0 22px;
}

.retreat-copy h2 span {
  display: block;
}

.retreat-copy p {
  max-width: 52ch;
  margin: 0;
  color: var(--tm-muted);
  font-size: 16px;
  line-height: 1.9;
}

.retreat-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 30px;
}

.retreat-actions button {
  min-width: 82px;
  padding: 9px 16px;
  border-radius: 999px;
  border: 1px solid var(--tm-line);
  background: oklch(0.985 0.002 248 / 0.82);
  color: var(--tm-bark);
  font-size: 14px;
  font-weight: 760;
  line-height: 1;
  cursor: pointer;
  box-shadow: 0 10px 24px oklch(0.24 0.026 82 / 0.06);
  transition: transform 0.22s ease, border-color 0.22s ease, color 0.22s ease, background 0.22s ease;
}

.retreat-actions button:hover {
  transform: translateY(-2px);
  border-color: oklch(0.70 0.060 180);
  background: oklch(0.955 0.022 180);
  color: var(--tm-olive);
}

.retreat-visual {
  position: relative;
  z-index: 1;
  min-height: 390px;
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
  filter: saturate(0.92) contrast(0.94) brightness(1.08);
  transition: transform 0.7s ease, filter 0.7s ease;
}

.retreat-panel:hover .retreat-photo {
  transform: scale(1.035);
  filter: saturate(0.98) contrast(0.96) brightness(1.1);
}

.retreat-image-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, oklch(0.985 0.002 248 / 0.18), oklch(0.34 0.050 160 / 0.26)),
    linear-gradient(135deg, oklch(0.930 0.040 160 / 0.34), oklch(0.985 0.002 248 / 0.10) 48%, transparent 78%);
}

.retreat-steps {
  position: absolute;
  inset: 24px 26px;
  width: auto;
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.retreat-step {
  width: 100%;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  min-height: 0;
  padding: 16px 20px;
  border: 1px solid var(--tm-line);
  border-radius: 8px;
  background: oklch(0.985 0.002 248 / 0.82);
  color: inherit;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 14px 30px oklch(0.239 0.006 180 / 0.065);
  backdrop-filter: blur(7px);
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
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--tm-primary-soft);
  color: var(--tm-olive);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 23px;
  line-height: 1;
}

.retreat-step strong {
  display: block;
  color: var(--tm-ink);
  font-size: 16px;
  font-weight: 760;
  margin-bottom: 5px;
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
  min-height: 3em;
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
  --feat-accent: var(--tm-olive);
  --feat-accent-soft: oklch(0.955 0.022 180);
  --feat-border: var(--tm-line-soft);
  background:
    linear-gradient(180deg, var(--tm-surface) 0%, var(--feat-accent-soft) 132%);
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--feat-border);
  padding: 0;
  font: inherit;
  color: inherit;
  text-align: left;
  appearance: none;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: var(--tm-shadow-card);
}

.feature-card:nth-child(1) {
  --feat-accent: oklch(0.55 0.095 188);
  --feat-accent-soft: oklch(0.965 0.028 188);
  --feat-border: oklch(0.84 0.045 188);
}

.feature-card:nth-child(2) {
  --feat-accent: oklch(0.64 0.120 70);
  --feat-accent-soft: oklch(0.970 0.032 78);
  --feat-border: oklch(0.86 0.070 78);
}

.feature-card:nth-child(3) {
  --feat-accent: oklch(0.56 0.110 235);
  --feat-accent-soft: oklch(0.965 0.028 230);
  --feat-border: oklch(0.84 0.052 230);
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
  border-color: var(--feat-accent);
  box-shadow:
    0 20px 48px oklch(0.24 0.026 82 / 0.12),
    0 0 0 1px color-mix(in oklch, var(--feat-accent) 26%, transparent);
}

.feat-visual {
  height: 180px;
  position: relative;
  overflow: hidden;
  background: oklch(0.964 0.008 197);
}

.feat-visual-ai {
  background: linear-gradient(135deg, oklch(0.93 0.055 188), oklch(0.965 0.024 205));
}
.feat-visual-community {
  background: linear-gradient(135deg, oklch(0.95 0.060 78), oklch(0.970 0.026 42));
}
.feat-visual-attraction {
  background: linear-gradient(135deg, oklch(0.94 0.050 230), oklch(0.965 0.025 185));
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
    linear-gradient(180deg, oklch(0.985 0.002 248 / 0.06), color-mix(in oklch, var(--feat-accent) 42%, oklch(0.18 0.050 180 / 0.34))),
    linear-gradient(135deg, color-mix(in oklch, var(--feat-accent) 36%, transparent), transparent 56%);
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
  background: color-mix(in oklch, var(--feat-accent) 34%, oklch(0.985 0.002 248 / 0.28));
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
  position: relative;
  padding: 24px;
  text-align: center;
}

.feat-body::before {
  content: "";
  display: block;
  width: 42px;
  height: 3px;
  border-radius: 999px;
  margin: 0 auto 16px;
  background: var(--feat-accent);
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
  color: var(--feat-accent);
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
  background:
    linear-gradient(90deg, oklch(0.986 0.006 205) 0%, oklch(0.975 0.030 80) 48%, oklch(0.962 0.026 190) 100%);
  border: 1px solid oklch(0.84 0.040 190);
  border-radius: 8px;
  padding: 56px 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  position: relative;
  overflow: hidden;
}

.cta-card::before {
  content: "";
  position: absolute;
  inset: 0;
  border-top: 5px solid oklch(0.62 0.115 72);
  pointer-events: none;
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
  background: var(--tm-olive);
  border-color: var(--tm-olive);
  color: #fff;
  box-shadow: 0 14px 28px oklch(0.38 0.050 112 / 0.14);
}

.cta-card :deep(.el-button--primary:hover) {
  background: oklch(0.42 0.072 180);
  border-color: oklch(0.42 0.072 180);
  color: #fff;
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
    padding-top: 28px;
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
  .retreat-copy {
    max-width: 680px;
    min-height: auto;
  }
  .retreat-visual {
    min-height: 430px;
  }
  .retreat-steps {
    inset: 24px;
    width: auto;
  }
  .retreat-copy h2 {
    max-width: 8.2em;
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
    inset: auto;
    transform: none;
    width: 100%;
    grid-template-rows: none;
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
  .history-list {
    grid-template-columns: 1fr;
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

/* ==================== 沉浸式首页首屏 ==================== */
.home-page {
  margin: 0;
  background: oklch(0.965 0.010 104);
}

.hero-section {
  position: relative;
  display: block;
  min-height: 860px;
  padding: 98px clamp(24px, 4vw, 64px) 24px;
  overflow: hidden;
  background: oklch(0.24 0.052 177);
}

.hero-bg {
  position: absolute;
  inset: 0;
  z-index: 0;
  display: block;
}

.hero-photo-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 52%;
  filter: saturate(0.9) contrast(1.07) brightness(0.82);
  animation: none;
  transform: none;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, oklch(0.16 0.060 177 / 0.96) 0%, oklch(0.20 0.055 177 / 0.84) 31%, oklch(0.28 0.042 122 / 0.38) 62%, oklch(0.29 0.035 70 / 0.20) 100%),
    linear-gradient(180deg, oklch(0.12 0.035 177 / 0.42) 0%, transparent 38%, oklch(0.12 0.035 177 / 0.52) 100%);
}

.hero-content {
  position: relative;
  z-index: 1;
  display: block;
  width: min(100%, 1510px);
  margin: 0 auto;
}

.hero-main {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(350px, 410px);
  gap: clamp(54px, 8vw, 132px);
  align-items: center;
  min-height: 455px;
}

.hero-copy {
  max-width: 690px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
  color: oklch(0.84 0.095 82);
  font-size: 13px;
  font-weight: 760;
  letter-spacing: 0.12em;
}

.badge-dot {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  background: oklch(0.76 0.115 74);
  animation: none;
}

.hero-title {
  margin: 0 0 26px;
  color: oklch(0.96 0.018 88);
  font-family: "Songti SC", "STSong", "SimSun", Georgia, serif;
  font-size: clamp(68px, 6.8vw, 112px);
  font-weight: 700;
  line-height: 0.98;
  letter-spacing: -0.04em;
  text-shadow: 0 8px 34px oklch(0.10 0.030 177 / 0.24);
}

.title-gradient {
  display: block;
  margin-top: 8px;
  color: oklch(0.80 0.105 78);
  font-style: normal;
}

.hero-sub {
  max-width: 42ch;
  margin: 0 0 30px;
  color: oklch(0.90 0.018 92 / 0.88);
  font-size: 17px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 0;
}

.hero-actions :deep(.el-button) {
  height: 52px;
  padding: 0 28px;
  border-radius: 999px;
  font-size: 15px;
  font-weight: 740;
}

.hero-actions :deep(.el-button--primary) {
  background: oklch(0.40 0.092 177);
  border-color: oklch(0.53 0.074 177);
  box-shadow: 0 16px 30px oklch(0.10 0.040 177 / 0.26);
}

.hero-secondary-btn {
  color: oklch(0.94 0.018 88);
  background: oklch(0.18 0.044 177 / 0.30);
  border-color: oklch(0.89 0.020 88 / 0.50);
}

.hero-secondary-btn:hover,
.hero-secondary-btn:focus-visible {
  color: oklch(0.98 0.014 88);
  background: oklch(0.26 0.052 177 / 0.68);
  border-color: oklch(0.89 0.020 88 / 0.82);
}

.route-preview {
  padding: 18px;
  border: 1px solid oklch(0.92 0.012 90 / 0.58);
  border-radius: 30px;
  background: rgba(244, 239, 229, 0.68);
  box-shadow: 0 30px 70px oklch(0.10 0.030 177 / 0.28);
  backdrop-filter: blur(16px) saturate(0.86);
  -webkit-backdrop-filter: blur(16px) saturate(0.86);
}

.route-preview-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.route-preview-head > span {
  color: oklch(0.57 0.082 76);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.route-preview-head > strong {
  padding: 7px 12px;
  border-radius: 999px;
  color: oklch(0.30 0.072 177);
  background: oklch(0.92 0.012 90 / 0.76);
  font-size: 13px;
}

.route-preview h2 {
  margin: 12px 0;
  color: oklch(0.27 0.060 177);
  font-family: "Songti SC", "STSong", "SimSun", Georgia, serif;
  font-size: 28px;
  line-height: 1.1;
}

.route-cover {
  position: relative;
  display: block;
  width: 100%;
  height: 142px;
  padding: 0;
  overflow: hidden;
  border: 0;
  border-radius: 20px;
  background: oklch(0.28 0.050 177);
  cursor: pointer;
}

.route-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 52%;
  filter: saturate(0.78) contrast(1.02) brightness(0.88);
  transition: none;
}

.route-cover::after {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent 34%, oklch(0.12 0.038 177 / 0.66));
}

.route-cover span {
  position: absolute;
  z-index: 1;
  left: 18px;
  bottom: 13px;
  color: oklch(0.96 0.018 88);
  font-size: 13px;
  font-weight: 740;
}

.route-cover:hover img,
.route-cover:focus-visible img {
  transform: none;
}

.route-days {
  display: grid;
  gap: 5px;
  margin: 8px 0;
}

.route-day {
  display: grid;
  grid-template-columns: 38px 1fr;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 5px 12px;
  border: 1px solid oklch(0.74 0.014 92 / 0.54);
  border-radius: 15px;
  background: oklch(0.88 0.010 92 / 0.62);
}

.route-day > span {
  color: oklch(0.63 0.095 74);
  font-family: Georgia, "Times New Roman", serif;
  font-weight: 800;
}

.route-day > strong {
  color: oklch(0.31 0.058 177);
  font-size: 14px;
}

.route-preview-action {
  width: 100%;
  height: 44px;
  margin-top: 2px;
  border-radius: 15px;
  background: oklch(0.34 0.084 177);
  border-color: oklch(0.34 0.084 177);
  font-size: 15px;
}

.route-planner {
  margin-top: 18px;
  padding: 18px 22px 16px;
  border: 1px solid oklch(0.92 0.012 90 / 0.74);
  border-radius: 30px;
  background: oklch(0.95 0.014 88 / 0.97);
  box-shadow: 0 28px 64px oklch(0.10 0.030 177 / 0.24);
}

.planner-overview {
  display: flex;
  align-items: center;
  gap: 24px;
}

.planner-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  flex: 1;
}

.planner-step {
  display: grid;
  grid-template-columns: auto 1px minmax(0, 1fr);
  align-items: center;
  gap: 15px;
  min-width: 0;
  padding: 0 22px;
}

.planner-step:first-child {
  padding-left: 2px;
}

.planner-index {
  color: oklch(0.64 0.104 74);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 25px;
  font-weight: 800;
}

.planner-divider {
  width: 1px;
  height: 42px;
  background: oklch(0.72 0.032 78 / 0.62);
}

.planner-copy {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.planner-copy strong {
  overflow: hidden;
  color: oklch(0.29 0.054 177);
  font-size: 15px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.planner-copy small {
  overflow: hidden;
  color: oklch(0.54 0.018 92);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.planner-primary {
  flex: 0 0 auto;
  height: 54px;
  padding: 0 24px;
  border-radius: 16px;
  background: oklch(0.34 0.084 177);
  border-color: oklch(0.34 0.084 177);
  font-size: 15px;
}

.hero-search-tabs {
  margin-top: 14px;
}

.hero-search-tabs :deep(.el-tabs__header) {
  margin: 0 0 8px;
}

.hero-search-tabs :deep(.el-tabs__nav) {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;
  padding: 3px;
  border: 1px solid oklch(0.82 0.016 88 / 0.72);
  border-radius: 14px;
  background: oklch(0.92 0.012 88 / 0.72);
}

.hero-search-tabs :deep(.el-tabs__item) {
  height: 36px;
  padding: 0 12px;
  border-radius: 11px;
  color: oklch(0.45 0.025 177);
  font-size: 13px;
}

.hero-search-tabs :deep(.el-tabs__item.is-active) {
  color: oklch(0.95 0.014 88);
  background: oklch(0.38 0.078 177);
  box-shadow: none;
}

.search-field {
  padding: 7px 10px 9px;
  border-color: oklch(0.80 0.018 88 / 0.74);
  border-radius: 13px;
  background: oklch(0.98 0.006 88 / 0.74);
}

.field-label {
  margin-bottom: 5px;
  color: oklch(0.48 0.020 92);
}

.search-input :deep(.el-input__wrapper),
.search-date :deep(.el-input__wrapper) {
  height: 38px;
  border: 0;
  background: transparent;
}

.search-btn {
  height: 58px;
  min-width: 112px;
  border-radius: 14px;
  background: oklch(0.34 0.084 177);
  border-color: oklch(0.34 0.084 177);
  box-shadow: none;
}

.search-hints {
  display: none;
}

.hint-tag {
  background: oklch(0.92 0.012 88 / 0.76);
  border-color: oklch(0.80 0.018 88 / 0.72);
}

.stats-section {
  width: min(100%, 1510px);
  margin: 0 auto;
  padding: 34px clamp(24px, 4vw, 64px) 0;
}

.stats-grid {
  padding-top: 0;
}

@media (max-width: 1200px) {
  .hero-main {
    grid-template-columns: minmax(0, 1fr) 360px;
    gap: 48px;
  }

  .planner-steps {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    row-gap: 18px;
  }

  .planner-step:nth-child(3) {
    padding-left: 2px;
  }
}

@media (max-width: 992px) {
  .hero-section {
    min-height: auto;
    padding-top: 102px;
  }

  .hero-main {
    grid-template-columns: 1fr;
    gap: 42px;
  }

  .hero-copy {
    max-width: 640px;
  }

  .route-preview {
    width: min(100%, 620px);
  }

  .planner-overview {
    align-items: stretch;
    flex-direction: column;
  }

  .planner-primary {
    width: 100%;
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
  }
}

@media (max-width: 640px) {
  .home-page {
    margin: 0;
  }

  .hero-section {
    padding: 88px 16px 22px;
  }

  .hero-overlay {
    background:
      linear-gradient(180deg, oklch(0.15 0.056 177 / 0.94) 0%, oklch(0.18 0.052 177 / 0.80) 48%, oklch(0.13 0.040 177 / 0.88) 100%),
      linear-gradient(90deg, oklch(0.14 0.050 177 / 0.62), transparent);
  }

  .hero-main {
    gap: 32px;
  }

  .hero-title {
    font-size: clamp(54px, 17vw, 76px);
  }

  .hero-sub {
    font-size: 15px;
  }

  .hero-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .hero-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .route-preview {
    width: 100%;
    padding: 18px;
    border-radius: 24px;
  }

  .route-planner {
    padding: 20px 16px 16px;
    border-radius: 24px;
  }

  .planner-steps {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .planner-step,
  .planner-step:nth-child(3) {
    padding: 0;
  }

  .planner-divider {
    height: 34px;
  }

  .planner-copy strong,
  .planner-copy small {
    white-space: normal;
  }

  .stats-section {
    padding: 24px 16px 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .hero-photo-img {
    animation: none;
  }
}

/* ==================== v82 editorial lower-home refresh ==================== */
.editorial-section {
  width: min(100%, 1510px);
  margin: 0 auto;
  padding: clamp(72px, 8vw, 118px) clamp(24px, 4vw, 64px) 0;
}

.editorial-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 34px;
  border-bottom: 1px solid color-mix(in srgb, var(--tm-deep) 13%, transparent);
  padding-bottom: 20px;
}

.section-kicker {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  color: var(--tm-gold);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

.section-kicker::before {
  content: "";
  width: 44px;
  height: 1px;
  background: currentColor;
}

.section-title {
  max-width: 760px;
  margin: 0;
  color: var(--tm-deep);
  font-family: var(--tm-font-serif);
  font-size: clamp(34px, 4vw, 58px);
  font-weight: 650;
  line-height: 1.08;
  letter-spacing: -0.045em;
}

.section-sub {
  max-width: 720px;
  margin: 14px 0 0;
  color: var(--tm-ink-soft);
  font-size: 16px;
  line-height: 1.8;
}

.section-link-btn {
  flex: 0 0 auto;
  color: var(--tm-deep);
  font-weight: 760;
}

.editorial-dest-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 20px;
  align-items: stretch;
}

.editorial-dest-card {
  grid-column: span 4;
  display: grid;
  grid-template-rows: auto 1fr;
  min-width: 0;
  border: 1px solid color-mix(in srgb, var(--tm-deep) 12%, transparent);
  border-radius: 24px;
  background: var(--tm-paper-2);
  box-shadow: 0 16px 44px rgba(18, 28, 23, 0.08);
  text-align: left;
}

.editorial-dest-card.is-featured {
  grid-column: span 6;
  grid-row: auto;
}

.editorial-dest-card:nth-child(2),
.editorial-dest-card:nth-child(3) {
  grid-column: span 3;
}

.editorial-dest-card:nth-child(n + 4) {
  grid-column: span 4;
}

.editorial-dest-card .dest-img-wrap {
  height: 230px;
  border-radius: 22px 22px 0 0;
}

.editorial-dest-card.is-featured .dest-img-wrap {
  height: 230px;
}

.editorial-dest-card .dest-img {
  filter: saturate(0.9) contrast(1.04) brightness(0.92);
}

.editorial-dest-card .dest-gradient {
  background:
    linear-gradient(180deg, rgba(6, 44, 38, 0.04), rgba(6, 44, 38, 0.72)),
    linear-gradient(90deg, rgba(6, 44, 38, 0.62), transparent 58%);
}

.editorial-dest-card .dest-badge {
  top: 18px;
  left: 18px;
  right: auto;
  padding: 7px 13px;
  border: 1px solid rgba(255, 250, 240, 0.5);
  border-radius: 999px;
  color: var(--tm-paper-2);
  background: rgba(6, 44, 38, 0.46);
  backdrop-filter: blur(6px);
}

.editorial-dest-card .dest-img-info {
  left: 20px;
  right: 20px;
  bottom: 18px;
  display: grid;
  gap: 4px;
}

.editorial-dest-card .dest-city {
  color: var(--tm-paper-2);
  font-family: var(--tm-font-serif);
  font-size: clamp(25px, 2.2vw, 34px);
  line-height: 1;
}

.editorial-dest-card.is-featured .dest-city {
  font-size: clamp(30px, 3vw, 42px);
}

.editorial-dest-card .dest-country {
  color: rgba(255, 250, 240, 0.76);
}

.editorial-dest-card .dest-body {
  display: grid;
  grid-template-rows: auto 1fr auto;
  padding: 20px 20px 22px;
}

.dest-copy-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 13px;
  color: var(--tm-gold);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.dest-copy-head span {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 22px;
  font-weight: 800;
}

.dest-copy-head strong {
  color: var(--tm-primary);
  font-weight: 760;
}

.editorial-dest-card .dest-desc {
  min-height: 76px;
  margin-bottom: 18px;
  color: var(--tm-ink-soft);
  font-size: 14px;
  line-height: 1.75;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.editorial-dest-card .dest-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.editorial-dest-card .el-tag {
  border-color: rgba(7, 89, 78, 0.16);
  color: var(--tm-primary);
  background: rgba(7, 89, 78, 0.06);
}

.editorial-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  align-items: stretch;
}

.editorial-feature-card {
  display: grid;
  grid-template-rows: auto 1fr;
  min-height: 0;
  padding: 0;
  border: 1px solid color-mix(in srgb, var(--tm-deep) 12%, transparent);
  border-radius: 26px;
  background: var(--tm-paper-2);
  box-shadow: 0 16px 44px rgba(18, 28, 23, 0.08);
  text-align: left;
}

.editorial-feature-card:first-child {
  grid-row: auto;
}

.editorial-feature-card:first-child .feat-visual {
  height: 220px;
}

.editorial-feature-card .feat-visual {
  height: 220px;
  min-height: 0;
  border-radius: 24px 24px 0 0;
}

.editorial-feature-card .feat-photo {
  filter: saturate(0.86) contrast(1.03) brightness(0.94);
}

.editorial-feature-card .feat-photo-overlay {
  background:
    linear-gradient(180deg, transparent 34%, rgba(6, 44, 38, 0.76)),
    linear-gradient(90deg, rgba(6, 44, 38, 0.45), transparent 62%);
}

.editorial-feature-card .feat-body {
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  align-content: start;
  padding: 24px 24px 26px;
  text-align: left;
}

.editorial-feature-card .feat-body::before {
  display: none;
}

.feat-label {
  margin-bottom: 10px;
  color: var(--tm-gold);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.editorial-feature-card .feat-title {
  margin-bottom: 10px;
  color: var(--tm-deep);
  font-family: var(--tm-font-serif);
  font-size: clamp(24px, 2.4vw, 36px);
  line-height: 1.12;
  letter-spacing: -0.035em;
}

.editorial-feature-card .feat-desc {
  min-height: 76px;
  color: var(--tm-ink-soft);
  font-size: 14px;
  line-height: 1.8;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.editorial-feature-card .feat-link {
  margin-top: 12px;
  color: var(--tm-primary);
  font-weight: 760;
}

.editorial-cta-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 0.52fr);
  align-items: center;
  gap: 36px;
  padding: clamp(34px, 5vw, 62px);
  border: 1px solid rgba(195, 148, 74, 0.28);
  border-radius: 30px;
  background: linear-gradient(135deg, #fffaf0 0%, #f3eadb 100%);
  box-shadow: 0 18px 50px rgba(18, 28, 23, 0.08);
}

.editorial-cta-card::before {
  border-top: 0;
  border-left: 5px solid var(--tm-gold);
}

.editorial-cta-card .cta-title {
  max-width: 760px;
  color: var(--tm-deep);
  font-family: var(--tm-font-serif);
  font-size: clamp(34px, 4vw, 56px);
  line-height: 1.12;
  letter-spacing: -0.045em;
}

.editorial-cta-card .cta-sub {
  max-width: 640px;
  color: var(--tm-ink-soft);
  line-height: 1.8;
}

.cta-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.cta-actions .el-button:not(.el-button--primary) {
  color: var(--tm-deep);
  background: rgba(255, 250, 240, 0.72);
}

.cta-route-note {
  display: grid;
  gap: 12px;
}

.cta-note-item {
  display: grid;
  grid-template-columns: 52px 1fr;
  gap: 16px;
  align-items: start;
  padding: 18px;
  border: 1px solid rgba(7, 89, 78, 0.12);
  border-radius: 18px;
  background: rgba(255, 250, 240, 0.72);
}

.cta-note-item > span {
  color: var(--tm-gold);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 28px;
  font-weight: 800;
  line-height: 1;
}

.cta-note-item strong {
  display: block;
  margin-bottom: 4px;
  color: var(--tm-deep);
  font-size: 16px;
}

.cta-note-item small {
  color: var(--tm-muted);
  line-height: 1.6;
}

@media (max-width: 1080px) {
  .editorial-dest-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .editorial-dest-card,
  .editorial-dest-card.is-featured,
  .editorial-dest-card:nth-child(2),
  .editorial-dest-card:nth-child(3),
  .editorial-dest-card:nth-child(n + 4) {
    grid-column: auto;
  }

  .editorial-feature-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .editorial-feature-card:first-child .feat-visual,
  .editorial-feature-card .feat-visual {
    height: 260px;
  }

  .editorial-cta-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .editorial-section {
    padding: 64px 16px 0;
  }

  .editorial-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .editorial-dest-grid,
  .editorial-feature-grid {
    grid-template-columns: 1fr;
  }

  .editorial-dest-card,
  .editorial-dest-card.is-featured,
  .editorial-dest-card:nth-child(2),
  .editorial-dest-card:nth-child(3),
  .editorial-dest-card:nth-child(n + 4) {
    grid-column: auto;
    grid-row: auto;
  }

  .editorial-dest-card.is-featured .dest-img-wrap,
  .editorial-dest-card .dest-img-wrap,
  .editorial-feature-card:first-child .feat-visual,
  .editorial-feature-card .feat-visual {
    height: 240px;
  }

  .editorial-dest-card.is-featured .dest-city {
    font-size: 34px;
  }

  .cta-actions .el-button {
    width: 100%;
    margin-left: 0;
  }
}
</style>
