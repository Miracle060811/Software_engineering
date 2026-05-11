<template>
  <div class="ai-plan-page">
    <el-row :gutter="24">
      <!-- 左侧输入区 -->
      <el-col :span="8">
        <el-card class="input-card">
          <template #header>
            <span class="card-header-title">🤖 AI 行程规划</span>
          </template>
          <el-form :model="planForm" label-position="top" label-width="auto">
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
                :max="30"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="出行人数">
              <el-input-number
                v-model="planForm.people"
                :min="1"
                :max="50"
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
              <el-checkbox-group v-model="planForm.preferences">
                <el-checkbox value="文化历史">文化历史</el-checkbox>
                <el-checkbox value="自然风光">自然风光</el-checkbox>
                <el-checkbox value="美食体验">美食体验</el-checkbox>
                <el-checkbox value="购物娱乐">购物娱乐</el-checkbox>
                <el-checkbox value="亲子游">亲子游</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-button
              type="primary"
              :loading="generating"
              style="width: 100%"
              @click="generatePlan"
            >
              {{ generating ? "正在生成..." : "✨ 生成行程" }}
            </el-button>
          </el-form>
        </el-card>

        <!-- 历史行程 -->
        <el-card class="history-card" style="margin-top: 20px">
          <template #header>
            <span class="card-header-title">📋 历史行程</span>
          </template>
          <el-empty
            v-if="historyPlans.length === 0"
            description="暂无历史行程"
            :image-size="60"
          />
          <div
            v-for="plan in historyPlans"
            :key="plan.id"
            class="history-item"
            @click="viewHistoryPlan(plan)"
          >
            <div class="history-title">
              {{ plan.title || plan.destination }}
            </div>
            <div class="history-meta">
              {{ plan.days }}天 · {{ plan.createTime }}
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧行程展示区 -->
      <el-col :span="16">
        <div v-if="generating" class="loading-area">
          <el-skeleton :rows="8" animated />
        </div>
        <el-empty
          v-else-if="!currentPlan"
          description="输入行程信息，点击「生成行程」开始规划"
          :image-size="120"
        />
        <div v-else class="plan-result">
          <el-card class="plan-summary-card">
            <h2 class="plan-title">{{ currentPlan.title }}</h2>
            <p class="plan-summary">{{ currentPlan.summary }}</p>
            <el-tag type="success" size="large">
              总预估费用：¥{{ currentPlan.totalEstimatedCost }}
            </el-tag>
          </el-card>

          <el-card
            v-for="dayPlan in currentPlan.days"
            :key="dayPlan.day"
            class="day-card"
          >
            <template #header>
              <div class="day-header">
                <span class="day-num">第 {{ dayPlan.day }} 天</span>
                <span class="day-theme">{{ dayPlan.theme }}</span>
              </div>
            </template>
            <el-timeline>
              <el-timeline-item
                v-for="(activity, idx) in dayPlan.activities"
                :key="idx"
                :timestamp="activity.time"
                placement="top"
                type="primary"
              >
                <el-card class="activity-card" shadow="never">
                  <div class="activity-name">{{ activity.name }}</div>
                  <div class="activity-desc">{{ activity.description }}</div>
                  <div class="activity-cost" v-if="activity.cost">
                    预估费用：¥{{ activity.cost }}
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </div>
      </el-col>
    </el-row>

    <!-- AI 客服浮窗按钮 -->
    <div class="chat-fab" @click="chatDrawerVisible = true">💬</div>

    <!-- AI 客服抽屉 -->
    <el-drawer
      v-model="chatDrawerVisible"
      title="AI 旅行助手"
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
            <div class="bubble-content typing">正在思考...</div>
          </div>
        </div>
        <div class="chat-input-area">
          <el-input
            v-model="chatInput"
            placeholder="问问AI旅行助手..."
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
import { ref, nextTick, onMounted } from "vue";
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const planForm = ref({
  destination: "",
  days: 3,
  people: 2,
  budget: "",
  startDate: "",
  preferences: [],
});

const generating = ref(false);
const currentPlan = ref(null);
const historyPlans = ref([]);

const chatDrawerVisible = ref(false);
const chatMessages = ref([
  {
    role: "assistant",
    content: "你好！我是AI旅行助手，有任何旅行问题都可以问我 ✈️",
  },
]);
const chatInput = ref("");
const chatLoading = ref(false);
const chatMessagesRef = ref(null);
const sessionId = ref(`session_${Date.now()}`);

const generatePlan = async () => {
  if (!planForm.value.destination) {
    ElMessage.warning("请输入目的地");
    return;
  }
  generating.value = true;
  currentPlan.value = null;
  try {
    const res = await request.post("/api/ai/plan/generate", {
      destination: planForm.value.destination,
      days: planForm.value.days,
      people: planForm.value.people,
      budget: planForm.value.budget,
      startDate: planForm.value.startDate,
      preferences: planForm.value.preferences.join(","),
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
  } catch (e) {
    ElMessage.error("行程生成失败，请稍后重试");
  } finally {
    generating.value = false;
  }
};

const fetchHistoryPlans = async () => {
  try {
    const data = await request.get("/api/ai/plan/list");
    historyPlans.value = Array.isArray(data) ? data : [];
  } catch (e) {
    historyPlans.value = [];
  }
};

const viewHistoryPlan = (plan) => {
  try {
    currentPlan.value = plan.planContent ? JSON.parse(plan.planContent) : plan;
  } catch (e) {
    currentPlan.value = plan;
  }
};

const sendChat = async () => {
  const content = chatInput.value.trim();
  if (!content) return;
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
    });
    const reply =
      res?.reply || res?.content || res || "收到您的问题，正在处理中...";
    chatMessages.value.push({ role: "assistant", content: reply });
  } catch (e) {
    chatMessages.value.push({
      role: "assistant",
      content: "抱歉，暂时无法回答，请稍后再试。",
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

onMounted(() => {
  fetchHistoryPlans();
});
</script>

<style scoped>
.ai-plan-page {
  max-width: 1200px;
  margin: 0 auto;
}
.card-header-title {
  font-size: 16px;
  font-weight: 600;
}
.input-card,
.history-card {
  border-radius: 12px;
}
.history-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: color 0.2s;
}
.history-item:hover {
  color: #409eff;
}
.history-item:last-child {
  border-bottom: none;
}
.history-title {
  font-size: 14px;
  font-weight: 500;
}
.history-meta {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.loading-area {
  padding: 20px;
}
.plan-summary-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.plan-title {
  font-size: 22px;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
}
.plan-summary {
  font-size: 14px;
  color: #666;
  line-height: 1.7;
  margin-bottom: 12px;
}
.day-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.day-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.day-num {
  font-size: 16px;
  font-weight: 700;
  color: #409eff;
}
.day-theme {
  font-size: 14px;
  color: #666;
}
.activity-card {
  border: 1px solid #e8e8e8;
}
.activity-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}
.activity-desc {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}
.activity-cost {
  font-size: 12px;
  color: #ff8c00;
  margin-top: 6px;
}
.chat-fab {
  position: fixed;
  bottom: 40px;
  right: 40px;
  width: 56px;
  height: 56px;
  background: #409eff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.4);
  z-index: 999;
  transition: transform 0.2s;
}
.chat-fab:hover {
  transform: scale(1.1);
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
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
}
.chat-bubble.user .bubble-content {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.chat-bubble.assistant .bubble-content {
  background: #f5f7fa;
  color: #333;
  border-bottom-left-radius: 4px;
}
.typing {
  color: #999;
  font-style: italic;
}
.chat-input-area {
  padding: 16px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 8px;
}
</style>
