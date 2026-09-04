<template>
  <div class="ai-plan-page">
    <section class="planner-hero">
      <div class="planner-hero-copy">
        <span class="section-kicker">AI ROUTE PLANNING DESK</span>
        <h1>把旅行想法整理成每天都能照着走的路线</h1>
        <p>
          输入出发地、目的地、人数、预算和偏好，TravelMate 会先核验城市，再把每日节奏、交通衔接、住宿建议和费用估算放进一份清晰行程。
        </p>
      </div>
      <div class="hero-route-card" aria-label="规划流程">
        <div class="route-node active">
          <span>01</span>
          <strong>输入偏好</strong>
        </div>
        <div class="route-line"></div>
        <div class="route-node">
          <span>02</span>
          <strong>生成路线</strong>
        </div>
        <div class="route-line"></div>
        <div class="route-node">
          <span>03</span>
          <strong>导出执行</strong>
        </div>
      </div>
    </section>

    <div class="planner-layout">
      <aside class="planner-sidebar">
        <section class="input-card">
          <div class="panel-head">
            <span><el-icon><Cpu /></el-icon> AI 路线整理</span>
            <strong>先定旅行轮廓</strong>
          </div>
          <el-form :model="planForm" label-position="top" label-width="auto">
            <el-form-item label="出发地">
              <el-select
                v-model="planForm.origin"
                filterable
                allow-create
                default-first-option
                clearable
                placeholder="选择或输入真实城市"
              >
                <el-option
                  v-for="city in commonOriginOptions"
                  :key="city"
                  :label="city"
                  :value="city"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="目的地">
              <el-input
                v-model="planForm.destination"
                placeholder="如：云南大理"
                clearable
              />
            </el-form-item>
            <el-form-item label="出行天数">
              <el-input-number
                v-model="planForm.days"
                :min="1"
                :max="15"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="出行人数">
              <el-input-number
                v-model="planForm.people"
                :min="1"
                :max="20"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="预算（元）">
              <el-input
                v-model="planForm.budget"
                placeholder="如：5000"
                type="number"
              />
            </el-form-item>
            <el-form-item label="出发日期">
              <el-date-picker
                v-model="planForm.startDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="出行偏好">
              <el-checkbox-group v-model="planForm.preferences" class="preference-grid">
                <el-checkbox
                  v-for="item in preferenceOptions"
                  :key="item"
                  :value="item"
                >
                  {{ item }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="旅行节奏">
              <el-radio-group v-model="planForm.travelStyle" class="style-group">
                <el-radio-button
                  v-for="item in travelStyleOptions"
                  :key="item"
                  :label="item"
                />
              </el-radio-group>
            </el-form-item>
            <el-form-item label="交通偏好">
              <el-select v-model="planForm.transportPreference">
                <el-option
                  v-for="item in transportOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="住宿偏好">
              <el-select v-model="planForm.accommodationPreference">
                <el-option
                  v-for="item in accommodationOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="必去地点">
              <el-input
                v-model="planForm.mustVisit"
                type="textarea"
                :rows="2"
                maxlength="120"
                show-word-limit
                placeholder="如：洱海骑行、苍山、当地夜市"
              />
            </el-form-item>
            <el-form-item label="避开项">
              <el-input
                v-model="planForm.avoidPlaces"
                type="textarea"
                :rows="2"
                maxlength="120"
                show-word-limit
                placeholder="如：高强度爬山、排队太久、太晚返程"
              />
            </el-form-item>
            <el-button
              type="primary"
              :loading="generating"
              class="generate-btn"
              @click="generatePlan"
            >
              <el-icon><MagicStick /></el-icon>
              {{ generating ? "正在生成..." : "生成行程" }}
            </el-button>
            <p class="verification-note">
              出发地和目的地会先经过城市核验；无法确认或不可作为城市前往时，将停止生成。
              地点数据 © OpenStreetMap contributors，天气数据由 Open-Meteo 提供。
            </p>
          </el-form>
        </section>

        <section class="preset-panel">
          <div class="panel-head compact">
            <span>推荐目的地</span>
            <strong>快速填充</strong>
          </div>
          <div class="preset-list">
            <button
              v-for="preset in destinationPresets"
              :key="preset.name"
              type="button"
              class="preset-chip"
              @click="applyDestinationPreset(preset)"
            >
              <span>{{ preset.name }}</span>
              <small>{{ preset.days }}天 · ¥{{ preset.budget }}</small>
            </button>
          </div>
        </section>

        <section class="history-card">
          <div class="panel-head compact">
            <span><el-icon><Tickets /></el-icon> 历史行程</span>
            <strong>{{ historyPlans.length }} 条</strong>
          </div>
          <el-empty
            v-if="historyPlans.length === 0"
            description="暂无历史行程"
            :image-size="60"
          />
          <button
            v-for="plan in historyPlans"
            :key="plan.id"
            type="button"
            class="history-item"
            :class="{ active: selectedPlanId === Number(plan.id) }"
            @click="viewHistoryPlan(plan)"
          >
            <div class="history-title">
              {{ plan.title || plan.destination }}
            </div>
            <div class="history-meta">
              {{ plan.days }}天 · {{ plan.createTime }}
              <span v-if="plan.startDate" class="countdown">
                {{ getCountdown(plan.startDate) }}
              </span>
            </div>
          </button>
        </section>
      </aside>

      <main class="planner-main">
        <div v-if="generating" class="loading-area">
          <el-skeleton :rows="8" animated />
        </div>
        <section v-else-if="!currentPlan" class="empty-plan">
          <div class="empty-plan-copy">
            <span class="section-kicker">ROUTE BUILDER</span>
            <h2>先填左侧偏好，这里会生成每日路线</h2>
            <p>
              生成后会按天展示主题、区域、时间安排、交通衔接和预算，方便直接导出或继续订票订酒店。
            </p>
          </div>
          <div class="sample-route">
            <div class="sample-route-head">
              <span>示例结构</span>
              <strong>4 Days</strong>
            </div>
            <div
              v-for="item in sampleDays"
              :key="item.day"
              class="sample-day"
            >
              <span>{{ item.day }}</span>
              <div>
                <strong>{{ item.title }}</strong>
                <small>{{ item.meta }}</small>
              </div>
            </div>
          </div>
        </section>
        <div v-else class="plan-result">
          <section class="plan-summary-card">
            <div class="plan-summary-head">
              <div>
                <span class="section-kicker">GENERATED PLAN</span>
                <h2 class="plan-title">{{ currentPlan.title }}</h2>
              </div>
              <div class="summary-actions">
                <el-button type="primary" plain @click="exportPlan">
                  <el-icon><Download /></el-icon>导出行程
                </el-button>
                <el-button plain @click="$router.push('/my-orders')">
                  <el-icon><Tickets /></el-icon>关联订单
                </el-button>
              </div>
            </div>
            <p class="plan-summary">{{ currentPlan.summary }}</p>
            <div class="summary-tags">
              <el-tag v-if="currentPlan.origin && currentPlan.destination" effect="plain">
                {{ currentPlan.origin }} → {{ currentPlan.destination }}
              </el-tag>
              <el-tag v-if="currentPlan.pace" type="primary" effect="plain">
                节奏：{{ currentPlan.pace }}
              </el-tag>
              <el-tag type="success" size="large">
                总预估费用：¥{{ currentPlan.totalEstimatedCost }}
              </el-tag>
            </div>
            <p v-if="currentPlan.budgetNote" class="budget-note">
              {{ currentPlan.budgetNote }}
            </p>
          </section>

          <section v-if="hasPlanInsights" class="plan-insight-section">
            <article v-if="currentPlan.transportAdvice" class="insight-card">
              <span>交通建议</span>
              <p>{{ currentPlan.transportAdvice }}</p>
            </article>
            <article v-if="currentPlan.hotelAdvice" class="insight-card">
              <span>住宿建议</span>
              <p>{{ currentPlan.hotelAdvice }}</p>
            </article>
            <article v-if="beforeTripChecklist.length" class="insight-card">
              <span>行前清单</span>
              <ul>
                <li v-for="item in beforeTripChecklist" :key="item">{{ item }}</li>
              </ul>
            </article>
            <article v-if="riskNotes.length" class="insight-card">
              <span>风险提醒</span>
              <ul>
                <li v-for="item in riskNotes" :key="item">{{ item }}</li>
              </ul>
            </article>
          </section>

          <section
            v-for="dayPlan in currentPlan.days || []"
            :key="dayPlan.day"
            class="day-card"
          >
            <div class="day-header">
              <div>
                <span class="day-num">第 {{ dayPlan.day }} 天</span>
                <span class="day-theme">{{ dayPlan.theme }}</span>
              </div>
              <div class="day-meta">
                <span v-if="dayPlan.date">{{ dayPlan.date }}</span>
                <span v-if="dayPlan.area">{{ dayPlan.area }}</span>
                <span v-if="dayPlan.dayEstimatedCost">
                  约 ¥{{ dayPlan.dayEstimatedCost }}
                </span>
              </div>
            </div>
            <div v-if="dayPlan.tips" class="day-tip">{{ dayPlan.tips }}</div>
            <div
              v-if="dayPlan.mealHint || dayPlan.backupPlan"
              class="day-support-grid"
            >
              <div v-if="dayPlan.mealHint">
                <span>餐饮安排</span>
                <p>{{ dayPlan.mealHint }}</p>
              </div>
              <div v-if="dayPlan.backupPlan">
                <span>当天备选</span>
                <p>{{ dayPlan.backupPlan }}</p>
              </div>
            </div>
            <el-timeline>
              <el-timeline-item
                v-for="(activity, idx) in dayPlan.activities"
                :key="idx"
                :timestamp="activity.time"
                placement="top"
                type="primary"
              >
                <div class="activity-card">
                  <div class="activity-heading">
                    <div class="activity-name">{{ activity.name }}</div>
                    <el-tag v-if="activity.type" size="small" effect="plain">
                      {{ activity.type }}
                    </el-tag>
                  </div>
                  <div class="activity-desc">{{ activity.description }}</div>
                  <div class="activity-meta">
                    <span v-if="activity.duration">{{ activity.duration }}</span>
                    <span v-if="activity.transfer">{{ activity.transfer }}</span>
                    <span v-if="activity.bookingTip">{{ activity.bookingTip }}</span>
                  </div>
                  <div class="activity-cost" v-if="activity.cost">
                    预估费用：¥{{ activity.cost }}
                  </div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </section>

          <section v-if="planAlternatives.length" class="alternative-section">
            <div class="alternative-head">
              <span class="section-kicker">PLAN B</span>
              <h3>备选方案</h3>
            </div>
            <div class="alternative-list">
              <article
                v-for="item in planAlternatives"
                :key="item.title + item.whenToUse"
                class="alternative-item"
              >
                <strong>{{ item.title }}</strong>
                <p v-if="item.whenToUse">{{ item.whenToUse }}</p>
                <small v-if="item.changes">{{ item.changes }}</small>
              </article>
            </div>
          </section>
        </div>
      </main>
    </div>

    <!-- 旅行助手浮窗按钮 -->
    <button
      class="chat-fab"
      type="button"
      aria-label="打开旅行助手"
      @click="chatDrawerVisible = true"
    >
      <el-icon :size="24"><ChatDotSquare /></el-icon>
    </button>

    <!-- 旅行助手抽屉 -->
    <el-drawer
      v-model="chatDrawerVisible"
      title="旅行助手"
      direction="rtl"
      size="420px"
    >
      <div class="chat-container">
        <div class="chat-messages" ref="chatMessagesRef">
          <div
            v-for="(msg, idx) in chatMessages"
            :key="idx"
            :class="['chat-bubble', msg.role]"
          >
            <div class="bubble-content">{{ msg.content }}</div>
          </div>
          <div v-if="chatLoading" class="chat-bubble assistant">
            <div class="bubble-content typing">正在生成回复...</div>
          </div>
        </div>
        <div class="chat-input-area">
          <el-input
            v-model="chatInput"
            placeholder="问问旅行助手..."
            @keyup.enter="sendChat"
          />
          <el-button type="primary" :loading="chatLoading" @click="sendChat"
            >发送</el-button
          >
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, ref, nextTick, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  Cpu,
  MagicStick,
  Tickets,
  ChatDotSquare,
  Download,
} from "@element-plus/icons-vue";
import request from "@/utils/request";
import { getAccessToken } from "@/utils/authToken";

const route = useRoute();
const router = useRouter();

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

const preferenceOptions = [
  "文化历史",
  "自然风光",
  "美食体验",
  "购物娱乐",
  "亲子游",
  "轻松慢行",
];

const travelStyleOptions = ["轻松", "适中", "紧凑"];

const commonOriginOptions = [
  "北京", "上海", "广州", "深圳", "杭州", "南京", "成都", "重庆", "西安", "武汉",
];

const transportOptions = [
  "公共交通优先，必要时打车",
  "打车优先，减少换乘",
  "自驾优先，控制停车成本",
  "步行友好，少跨区移动",
];

const accommodationOptions = [
  "交通便利，预算均衡",
  "亲子友好，早餐稳定",
  "安静舒适，避开夜市",
  "靠近核心景区，减少通勤",
];

const destinationPresets = [
  {
    name: "云南大理",
    days: 4,
    people: 2,
    budget: "4200",
    preferences: ["自然风光", "美食体验", "轻松慢行"],
    travelStyle: "轻松",
    transportPreference: "公共交通优先，必要时打车",
    accommodationPreference: "安静舒适，避开夜市",
    mustVisit: "洱海骑行、古城夜游",
    avoidPlaces: "连续高强度爬山",
  },
  {
    name: "杭州",
    days: 3,
    people: 2,
    budget: "2600",
    preferences: ["文化历史", "美食体验"],
    travelStyle: "适中",
    transportPreference: "步行友好，少跨区移动",
    accommodationPreference: "靠近核心景区，减少通勤",
    mustVisit: "西湖、灵隐寺",
    avoidPlaces: "太晚返程",
  },
  {
    name: "成都",
    days: 5,
    people: 3,
    budget: "5200",
    preferences: ["美食体验", "亲子游", "文化历史"],
    travelStyle: "轻松",
    transportPreference: "打车优先，减少换乘",
    accommodationPreference: "亲子友好，早餐稳定",
    mustVisit: "大熊猫基地、茶馆、火锅",
    avoidPlaces: "午后长时间排队",
  },
];

const sampleDays = [
  { day: "D1", title: "抵达与城市散步", meta: "机场/车站衔接 · 老街区 · 晚餐" },
  { day: "D2", title: "核心景点深度游", meta: "上午景点 · 下午咖啡/博物馆" },
  { day: "D3", title: "周边自然路线", meta: "轻徒步 · 当地餐厅 · 返程准备" },
];

const planForm = ref({
  origin: "",
  destination: "",
  days: 3,
  people: 2,
  budget: "",
  startDate: addDays(1),
  preferences: [],
  travelStyle: "适中",
  transportPreference: "公共交通优先，必要时打车",
  accommodationPreference: "交通便利，预算均衡",
  mustVisit: "",
  avoidPlaces: "",
});

const generating = ref(false);
const currentPlan = ref(null);
const historyPlans = ref([]);
const selectedPlanId = ref(null);

const chatDrawerVisible = ref(false);
const chatMessages = ref([
  {
    role: "assistant",
    content: "你好，我是 TravelMate 智能旅行助手。有任何路线、交通或住宿问题都可以问我。",
  },
]);
const chatInput = ref("");
const chatLoading = ref(false);
const chatMessagesRef = ref(null);
const sessionId = ref(`session_${Date.now()}`);

const normalizeList = (value) => {
  if (Array.isArray(value)) {
    return value.filter((item) => item !== null && item !== undefined && String(item).trim());
  }
  return [];
};

const beforeTripChecklist = computed(() =>
  normalizeList(currentPlan.value?.beforeTripChecklist),
);
const riskNotes = computed(() => normalizeList(currentPlan.value?.riskNotes));
const planAlternatives = computed(() =>
  normalizeList(currentPlan.value?.alternatives),
);
const hasPlanInsights = computed(
  () =>
    Boolean(currentPlan.value?.transportAdvice) ||
    Boolean(currentPlan.value?.hotelAdvice) ||
    beforeTripChecklist.value.length > 0 ||
    riskNotes.value.length > 0,
);

const applyDestinationPreset = (preset) => {
  planForm.value.destination = preset.name;
  planForm.value.days = preset.days;
  planForm.value.people = preset.people;
  planForm.value.budget = preset.budget;
  planForm.value.preferences = [...preset.preferences];
  planForm.value.travelStyle = preset.travelStyle || "适中";
  planForm.value.transportPreference =
    preset.transportPreference || "公共交通优先，必要时打车";
  planForm.value.accommodationPreference =
    preset.accommodationPreference || "交通便利，预算均衡";
  planForm.value.mustVisit = preset.mustVisit || "";
  planForm.value.avoidPlaces = preset.avoidPlaces || "";
  if (!planForm.value.startDate) {
    planForm.value.startDate = addDays(1);
  }
};

const generatePlan = async () => {
  if (!planForm.value.origin) {
    ElMessage.warning("请选择或输入出发地");
    return;
  }
  if (!planForm.value.destination) {
    ElMessage.warning("请输入目的地");
    return;
  }
  if (planForm.value.origin.trim() === planForm.value.destination.trim()) {
    ElMessage.warning("出发地和目的地不能相同");
    return;
  }
  if (Number(planForm.value.budget) <= 0) {
    ElMessage.warning("请输入大于 0 的预算");
    return;
  }
  generating.value = true;
  currentPlan.value = null;
  try {
    const res = await request.post("/api/ai/plan/generate", {
      origin: planForm.value.origin,
      destination: planForm.value.destination,
      days: planForm.value.days,
      people: planForm.value.people,
      peopleCount: planForm.value.people,
      budget: planForm.value.budget,
      startDate: planForm.value.startDate,
      preferences: planForm.value.preferences.join(","),
      travelStyle: planForm.value.travelStyle,
      transportPreference: planForm.value.transportPreference,
      accommodationPreference: planForm.value.accommodationPreference,
      mustVisit: planForm.value.mustVisit,
      avoidPlaces: planForm.value.avoidPlaces,
    }, {
      timeout: 120000,
      skipErrorMessage: true,
    });
    // 后端返回 planContent 为 JSON 字符串
    if (res && res.planContent) {
      try {
        currentPlan.value = JSON.parse(res.planContent);
      } catch (e) {
        currentPlan.value = res;
      }
    } else if (res) {
      currentPlan.value = res;
    }
    await fetchHistoryPlans();
    window.dispatchEvent(new Event("notification-updated"));
  } catch (e) {
    ElMessage.error(e?.response?.data?.msg || e?.message || "行程生成失败，请稍后重试");
  } finally {
    generating.value = false;
  }
};

const fetchHistoryPlans = async () => {
  if (!getAccessToken()) {
    historyPlans.value = [];
    return;
  }
  try {
    const data = await request.get("/api/ai/plan/list", { silent: true });
    historyPlans.value = Array.isArray(data) ? data : [];
  } catch (e) {
    historyPlans.value = [];
  }
};

const getCountdown = (startDate) => {
  if (!startDate) return "";
  const now = new Date();
  const target = new Date(startDate);
  const diff = target.getTime() - now.getTime();
  if (diff <= 0) return "已出发";
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  if (days === 0) return "今天出发!";
  return `倒计时${days}天`;
};

const exportPlan = () => {
  if (!currentPlan.value) return;
  let text = "========================================\n";
  text += `  ${currentPlan.value.title || "行程计划"}\n`;
  text += `  ${currentPlan.value.summary || ""}\n`;
  if (currentPlan.value.origin && currentPlan.value.destination) {
    text += `  路线：${currentPlan.value.origin} → ${currentPlan.value.destination}\n`;
  }
  text += `  总预估费用：¥${currentPlan.value.totalEstimatedCost || 0}\n`;
  text += "========================================\n\n";
  if (currentPlan.value.transportAdvice) {
    text += `交通建议：${currentPlan.value.transportAdvice}\n`;
  }
  if (currentPlan.value.hotelAdvice) {
    text += `住宿建议：${currentPlan.value.hotelAdvice}\n`;
  }
  if (beforeTripChecklist.value.length) {
    text += "\n行前清单：\n";
    for (const item of beforeTripChecklist.value) {
      text += `- ${item}\n`;
    }
  }
  if (riskNotes.value.length) {
    text += "\n风险提醒：\n";
    for (const item of riskNotes.value) {
      text += `- ${item}\n`;
    }
  }
  text += "\n";
  if (currentPlan.value.days) {
    for (const day of currentPlan.value.days) {
      text += `第 ${day.day} 天 · ${day.theme || ""}\n`;
      text += `${"-".repeat(40)}\n`;
      if (day.mealHint) text += `餐饮安排：${day.mealHint}\n`;
      if (day.backupPlan) text += `当天备选：${day.backupPlan}\n`;
      if (day.activities) {
        for (const act of day.activities) {
          text += `  ${act.time || ""} | ${act.name || ""} [${
            act.type || ""
          }]\n`;
          text += `  ${act.description || ""}\n`;
          if (act.cost) text += `  费用：¥${act.cost}\n`;
          text += "\n";
        }
      }
    }
  }
  if (planAlternatives.value.length) {
    text += "\n备选方案：\n";
    for (const item of planAlternatives.value) {
      text += `- ${item.title || "备选"}：${item.whenToUse || ""} ${item.changes || ""}\n`;
    }
  }
  text += "========================================\n";
  text += "      由 TravelMate 伴游平台生成\n";
  text += "========================================\n";
  const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
  const el = document.createElement("a");
  el.href = URL.createObjectURL(blob);
  el.download = `TravelMate_行程_${Date.now()}.txt`;
  el.click();
  URL.revokeObjectURL(el.href);
  ElMessage.success("行程已导出");
};

const parsePlanId = (value) => {
  const rawValue = Array.isArray(value) ? value[0] : value;
  const planId = Number(rawValue);
  return Number.isSafeInteger(planId) && planId > 0 ? planId : null;
};

const viewHistoryPlan = (plan, options = {}) => {
  try {
    currentPlan.value = plan.planContent ? JSON.parse(plan.planContent) : plan;
  } catch (e) {
    currentPlan.value = plan;
  }
  selectedPlanId.value = parsePlanId(plan.id);
  if (options.syncRoute === false || !selectedPlanId.value) {
    return;
  }
  if (parsePlanId(route.query.planId) === selectedPlanId.value) {
    return;
  }
  router.replace({
    path: "/ai-plan",
    query: { ...route.query, planId: String(selectedPlanId.value) },
  });
};

const openPlanById = async (value) => {
  const planId = parsePlanId(value);
  if (!planId) return;

  const historyPlan = historyPlans.value.find(
    (plan) => Number(plan.id) === planId,
  );
  if (historyPlan) {
    viewHistoryPlan(historyPlan, { syncRoute: false });
    return;
  }

  try {
    const plan = await request.get(`/api/ai/plan/${planId}`, { silent: true });
    if (plan) {
      viewHistoryPlan(plan, { syncRoute: false });
    }
  } catch (e) {
    currentPlan.value = null;
    selectedPlanId.value = null;
    ElMessage.error("对应的 AI 行程不存在或已无法访问");
  }
};

const sendChat = async () => {
  const content = chatInput.value.trim();
  if (!content) return;
  const now = new Date();
  const clientDate = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
  chatMessages.value.push({ role: "user", content });
  chatInput.value = "";
  chatLoading.value = true;
  await nextTick(() => {
    if (chatMessagesRef.value) {
      chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight;
    }
  });
  try {
    const res = await request.post("/api/ai/chat", {
      message: content,
      sessionId: sessionId.value,
      clientDate,
      clientTimeZone: Intl.DateTimeFormat().resolvedOptions().timeZone || "",
    }, {
      timeout: 90000,
      skipErrorMessage: true,
    });
    const reply =
      res?.reply || res?.content || res || "收到您的问题，正在处理中...";
    chatMessages.value.push({ role: "assistant", content: reply });
  } catch (e) {
    chatMessages.value.push({
      role: "assistant",
      content: e?.response?.data?.msg || e?.message || "抱歉，暂时无法回答，请稍后再试。",
    });
  } finally {
    chatLoading.value = false;
    await nextTick(() => {
      if (chatMessagesRef.value) {
        chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight;
      }
    });
  }
};

onMounted(async () => {
  await fetchHistoryPlans();
  await openPlanById(route.query.planId);
});

watch(
  () => route.query.planId,
  async (planId, previousPlanId) => {
    if (planId === previousPlanId) return;
    if (!planId) {
      currentPlan.value = null;
      selectedPlanId.value = null;
      return;
    }
    await openPlanById(planId);
  },
);
</script>

<style scoped>
.ai-plan-page {
  max-width: 1240px;
  margin: 0 auto;
  padding-bottom: 28px;
}

.planner-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(330px, 0.55fr);
  gap: 36px;
  align-items: end;
  margin-bottom: 22px;
  padding: 42px;
  border: 1px solid var(--tm-line);
  border-radius: 8px;
  background:
    linear-gradient(135deg, oklch(0.985 0.002 248), oklch(0.962 0.010 197));
  box-shadow: var(--tm-shadow-card);
}

.section-kicker {
  display: inline-flex;
  margin-bottom: 12px;
  color: var(--tm-olive);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.planner-hero h1 {
  max-width: 10em;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 48px;
  font-weight: 500;
  line-height: 1.06;
  color: var(--tm-ink);
  margin-bottom: 16px;
}

.planner-hero p {
  max-width: 62ch;
  color: var(--tm-muted);
  font-size: 16px;
  line-height: 1.9;
}

.hero-route-card {
  display: grid;
  grid-template-columns: auto 1fr auto 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 22px;
  border: 1px solid var(--tm-line);
  border-radius: 8px;
  background: oklch(0.985 0.002 248 / 0.76);
}

.route-node {
  display: grid;
  gap: 8px;
  min-width: 74px;
}

.route-node span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: oklch(0.935 0.030 180);
  color: var(--tm-olive);
  font-weight: 800;
}

.route-node strong {
  color: var(--tm-ink);
  font-size: 13px;
}

.route-node.active span {
  background: var(--tm-olive);
  color: oklch(0.985 0.002 248);
}

.route-line {
  height: 1px;
  background: var(--tm-line);
}

.planner-layout {
  display: grid;
  grid-template-columns: minmax(300px, 360px) minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.planner-sidebar {
  display: grid;
  gap: 16px;
  position: sticky;
  top: 84px;
}

.input-card,
.preset-panel,
.history-card,
.loading-area,
.empty-plan,
.plan-summary-card,
.day-card {
  border-radius: 8px;
  border: 1px solid var(--tm-line-soft);
  background: var(--tm-surface);
  box-shadow: var(--tm-shadow-card);
}

.input-card,
.preset-panel,
.history-card {
  padding: 20px;
}
.card-header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--tm-ink);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 18px;
}

.panel-head span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--tm-olive);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.panel-head strong {
  color: var(--tm-ink);
  font-size: 18px;
  font-weight: 760;
}

.panel-head.compact {
  align-items: center;
  margin-bottom: 12px;
}

.panel-head.compact strong {
  font-size: 13px;
  color: var(--tm-muted);
}

.input-card :deep(.el-form-item) {
  margin-bottom: 16px;
}

.input-card :deep(.el-form-item__label) {
  color: var(--tm-ink-soft);
  font-weight: 700;
}

.verification-note {
  margin: 10px 2px 0;
  color: var(--tm-text-muted);
  font-size: 12px;
  line-height: 1.6;
}

.input-card :deep(.el-input__wrapper),
.input-card :deep(.el-input-number),
.input-card :deep(.el-date-editor) {
  width: 100%;
}

.preference-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  width: 100%;
}

.preference-grid :deep(.el-checkbox) {
  height: 38px;
  margin-right: 0;
  padding: 0 10px;
  border: 1px solid var(--tm-line);
  border-radius: 8px;
  background: oklch(0.985 0.002 248);
}

.preference-grid :deep(.el-checkbox__label) {
  color: var(--tm-ink-soft);
  font-size: 13px;
  font-weight: 650;
}

.style-group {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  width: 100%;
}

.style-group :deep(.el-radio-button__inner) {
  width: 100%;
}

.generate-btn {
  width: 100%;
  height: 44px;
  font-weight: 760;
}

.preset-list {
  display: grid;
  gap: 8px;
}

.preset-chip,
.history-item {
  width: 100%;
  border: 1px solid var(--tm-line);
  border-radius: 8px;
  padding: 12px;
  font: inherit;
  color: inherit;
  text-align: left;
  appearance: none;
  background: oklch(0.985 0.002 248);
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.preset-chip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.preset-chip span,
.history-title {
  color: var(--tm-ink);
  font-size: 14px;
  font-weight: 760;
}

.preset-chip small,
.history-meta {
  color: var(--tm-muted);
  font-size: 12px;
}

.preset-chip:hover,
.history-item:hover,
.preset-chip:focus-visible,
.history-item:focus-visible {
  transform: translateY(-1px);
  border-color: var(--tm-olive);
  background: oklch(0.965 0.008 197);
  outline: none;
}
.history-item.active {
  border-color: var(--tm-olive);
  background: oklch(0.95 0.018 197);
  box-shadow: inset 3px 0 0 var(--tm-olive);
}
.history-item {
  margin-top: 8px;
}

.countdown {
  display: inline-flex;
  margin-left: 6px;
  color: var(--tm-olive);
  font-weight: 700;
}

.planner-main {
  min-width: 0;
}

.loading-area {
  padding: 24px;
}

.empty-plan {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(320px, 0.7fr);
  gap: 36px;
  align-items: center;
  min-height: 560px;
  padding: 42px;
  background:
    linear-gradient(135deg, oklch(0.985 0.002 248), oklch(0.964 0.008 197));
}

.empty-plan h2 {
  max-width: 11em;
  font-family: Georgia, "Times New Roman", serif;
  font-size: 42px;
  font-weight: 500;
  line-height: 1.08;
  color: var(--tm-ink);
  margin-bottom: 14px;
}

.empty-plan p {
  max-width: 54ch;
  color: var(--tm-muted);
  line-height: 1.9;
}

.sample-route {
  padding: 24px;
  border-radius: 8px;
  border: 1px solid var(--tm-line);
  background: oklch(0.985 0.002 248 / 0.82);
}

.sample-route-head,
.plan-summary-head,
.day-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.sample-route-head {
  padding-bottom: 16px;
  margin-bottom: 8px;
  border-bottom: 1px solid var(--tm-line);
}

.sample-route-head span {
  color: var(--tm-muted);
  font-size: 13px;
  font-weight: 760;
}

.sample-route-head strong {
  color: var(--tm-olive);
  font-family: Georgia, "Times New Roman", serif;
  font-size: 30px;
  font-weight: 500;
}

.sample-day {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--tm-line-soft);
}

.sample-day:last-child {
  border-bottom: 0;
}

.sample-day > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--tm-primary-soft);
  color: var(--tm-olive);
  font-weight: 800;
}

.sample-day strong {
  display: block;
  color: var(--tm-ink);
  margin-bottom: 4px;
}

.sample-day small {
  color: var(--tm-muted);
}

.plan-summary-card {
  margin-bottom: 16px;
  padding: 26px;
}

.plan-title {
  font-family: Georgia, "Times New Roman", serif;
  font-size: 32px;
  font-weight: 500;
  color: var(--tm-ink);
  margin-bottom: 0;
}

.summary-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.plan-summary {
  max-width: 66ch;
  font-size: 15px;
  color: var(--el-text-color-regular);
  line-height: 1.8;
  margin: 18px 0 14px;
}

.summary-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}
.budget-note {
  margin: 14px 0 0;
  padding: 12px 14px;
  border: 1px solid var(--tm-line);
  border-radius: 8px;
  background: oklch(0.965 0.008 197);
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.plan-insight-section {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.insight-card,
.alternative-section {
  border-radius: 8px;
  border: 1px solid var(--tm-line-soft);
  background: var(--tm-surface);
  box-shadow: var(--tm-shadow-card);
}

.insight-card {
  padding: 18px;
}

.insight-card span,
.day-support-grid span {
  display: block;
  margin-bottom: 8px;
  color: var(--tm-olive);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.insight-card p,
.day-support-grid p {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.7;
}

.insight-card ul {
  margin: 0;
  padding-left: 18px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.7;
}

.day-card {
  margin-bottom: 16px;
  padding: 24px 24px 8px;
}

.day-num {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--tm-primary-soft);
  color: var(--tm-olive);
  font-size: 13px;
  font-weight: 800;
}

.day-theme {
  margin-left: 10px;
  font-size: 17px;
  font-weight: 760;
  color: var(--el-text-color-regular);
}

.day-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.day-meta span {
  padding: 6px 10px;
  border-radius: 999px;
  background: oklch(0.965 0.008 197);
}

.day-tip {
  margin: 16px 0;
  padding: 12px 14px;
  background: var(--tm-primary-soft);
  border: 1px solid oklch(0.84 0.034 180);
  border-radius: 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.6;
}

.day-support-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.day-support-grid > div {
  padding: 12px 14px;
  border-radius: 8px;
  border: 1px solid var(--tm-line-soft);
  background: oklch(0.985 0.002 248);
}

.day-card :deep(.el-timeline) {
  padding-left: 3px;
  margin-top: 18px;
}

.day-card :deep(.el-timeline-item__timestamp) {
  color: var(--tm-olive);
  font-weight: 760;
}

.activity-card {
  border: 1px solid var(--tm-line-soft);
  border-radius: 8px;
  background: oklch(0.985 0.002 248);
  padding: 15px;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.activity-card:hover {
  border-color: var(--tm-line);
  background: oklch(0.995 0.004 197);
}

.activity-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 6px;
}
.activity-name {
  font-size: 15px;
  font-weight: 760;
  color: var(--el-text-color-primary);
}

.activity-desc {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.activity-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.activity-meta span {
  padding: 5px 9px;
  border-radius: 999px;
  background: oklch(0.965 0.008 197);
}

.activity-cost {
  font-size: 12px;
  color: var(--tm-accent);
  margin-top: 6px;
  font-weight: 700;
}

.alternative-section {
  padding: 22px;
}

.alternative-head {
  margin-bottom: 12px;
}

.alternative-head h3 {
  margin: 0;
  color: var(--tm-ink);
  font-size: 20px;
}

.alternative-list {
  display: grid;
  gap: 10px;
}

.alternative-item {
  padding: 14px;
  border: 1px solid var(--tm-line-soft);
  border-radius: 8px;
  background: oklch(0.985 0.002 248);
}

.alternative-item strong {
  color: var(--tm-ink);
}

.alternative-item p {
  margin: 6px 0;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.alternative-item small {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

.chat-fab {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 56px;
  height: 56px;
  background: var(--tm-gradient-brand);
  border-radius: 50%;
  border: 0;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: oklch(0.996 0.004 205);
  cursor: pointer;
  box-shadow: 0 10px 28px oklch(0.55 0.12 190 / 0.32);
  z-index: 999;
  transition: all 0.3s ease;
}
.chat-fab:hover,
.chat-fab:focus-visible {
  transform: translateY(-2px);
  box-shadow: 0 14px 34px oklch(0.55 0.12 190 / 0.38);
  outline: none;
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-bubble {
  display: flex;
}

.chat-bubble.user {
  justify-content: flex-end;
}

.chat-bubble.assistant {
  justify-content: flex-start;
}

.bubble-content {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.6;
}

.chat-bubble.user .bubble-content {
  background: var(--tm-gradient-brand);
  color: oklch(0.996 0.004 205);
  border-bottom-right-radius: 4px;
}

.chat-bubble.assistant .bubble-content {
  background: var(--tm-primary-soft);
  color: var(--tm-ink);
  border-bottom-left-radius: 4px;
}

.typing {
  color: var(--el-text-color-secondary);
  font-style: italic;
}

.chat-input-area {
  padding: 16px;
  border-top: 1px solid var(--tm-line-soft);
  display: flex;
  gap: 8px;
}

@media (max-width: 900px) {
  .planner-hero,
  .planner-layout,
  .empty-plan,
  .plan-insight-section,
  .day-support-grid {
    grid-template-columns: 1fr;
  }

  .planner-sidebar {
    position: static;
  }

  .chat-fab {
    right: 22px;
    bottom: 24px;
  }
}

@media (max-width: 640px) {
  .planner-hero {
    padding: 28px 20px;
  }

  .planner-hero h1,
  .empty-plan h2 {
    font-size: 34px;
  }

  .hero-route-card {
    grid-template-columns: 1fr;
  }

  .route-line {
    width: 1px;
    height: 18px;
    margin-left: 18px;
  }

  .empty-plan,
  .plan-summary-card,
  .day-card {
    padding: 20px;
  }

  .sample-route {
    padding: 18px;
  }

  .plan-summary-head,
  .day-header {
    flex-direction: column;
  }

  .summary-actions {
    width: 100%;
  }

  .summary-actions :deep(.el-button) {
    flex: 1;
  }

  .preference-grid {
    grid-template-columns: 1fr;
  }
}
</style>
