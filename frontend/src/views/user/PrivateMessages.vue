<template>
  <div class="messages-page">
    <PageHeader
      title="私信"
      subtitle="和站内用户进行一对一交流"
      :icon="ChatDotRound"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '私信' }
      ]"
    />

    <div class="message-shell">
      <aside class="contact-panel">
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索用户名或昵称"
            clearable
            @keyup.enter="searchUsers"
            @clear="searchResults = []"
          >
            <template #append>
              <el-button :icon="Search" @click="searchUsers" />
            </template>
          </el-input>
        </div>

        <div v-if="searchResults.length" class="search-results">
          <div class="panel-title">搜索结果</div>
          <div
            v-for="user in searchResults"
            :key="user.userId"
            class="contact-item"
            @click="selectContact(user)"
          >
            <el-avatar :size="40" :src="user.avatar || ''" class="contact-avatar">
              {{ avatarText(user) }}
            </el-avatar>
            <div class="contact-main">
              <div class="contact-name">{{ displayName(user) }}</div>
              <div class="contact-sub">@{{ user.username }}</div>
            </div>
          </div>
        </div>

        <div class="panel-title">最近私信</div>
        <el-empty v-if="!contacts.length && !loadingContacts" description="暂无私信" />
        <div
          v-for="contact in contacts"
          :key="contact.userId"
          :class="['contact-item', { active: activeUserId === contact.userId }]"
          @click="selectContact(contact)"
        >
          <el-badge :value="contact.unreadCount" :hidden="!contact.unreadCount">
            <el-avatar :size="42" :src="contact.avatar || ''" class="contact-avatar">
              {{ avatarText(contact) }}
            </el-avatar>
          </el-badge>
          <div class="contact-main">
            <div class="contact-row">
              <span class="contact-name">{{ displayName(contact) }}</span>
              <span class="contact-time">{{ formatTime(contact.lastMessageTime) }}</span>
            </div>
            <div class="last-message">{{ contact.lastMessage || "点击开始聊天" }}</div>
          </div>
        </div>
      </aside>

      <section class="chat-panel">
        <template v-if="activeContact">
          <header class="chat-header">
            <el-avatar :size="44" :src="activeContact.avatar || ''" class="chat-avatar">
              {{ avatarText(activeContact) }}
            </el-avatar>
            <div>
              <div class="chat-name">{{ displayName(activeContact) }}</div>
              <div class="chat-sub">@{{ activeContact.username }}</div>
            </div>
          </header>

          <div ref="messageBoxRef" class="message-list" v-loading="loadingMessages">
            <div
              v-for="message in messages"
              :key="message.id"
              :class="['message-row', isMine(message) ? 'mine' : 'theirs']"
            >
              <el-avatar :size="34" :src="isMine(message) ? userStore.userInfo?.avatar : activeContact.avatar" class="bubble-avatar">
                {{ isMine(message) ? selfAvatarText : avatarText(activeContact) }}
              </el-avatar>
              <div class="bubble-wrap">
                <div class="bubble">{{ message.content }}</div>
                <div class="message-time">{{ formatTime(message.createTime) }}</div>
              </div>
            </div>
            <el-empty v-if="!messages.length && !loadingMessages" description="还没有消息" />
          </div>

          <footer class="composer">
            <el-input
              v-model="draft"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              resize="none"
              placeholder="输入消息，按 Ctrl + Enter 发送"
              @keydown.ctrl.enter.prevent="sendMessage"
            />
            <div class="composer-actions">
              <el-button :icon="Refresh" @click="loadConversation(activeUserId)">刷新</el-button>
              <el-button type="primary" :icon="Promotion" :loading="sending" :disabled="!draft.trim()" @click="sendMessage">
                发送
              </el-button>
            </div>
          </footer>
        </template>

        <div v-else class="empty-chat">
          <el-icon :size="54"><ChatDotRound /></el-icon>
          <p>选择联系人开始私信</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute } from "vue-router";
import { ChatDotRound, Promotion, Refresh, Search } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const userStore = useUserStore();

const contacts = ref([]);
const searchResults = ref([]);
const messages = ref([]);
const activeContact = ref(null);
const keyword = ref("");
const draft = ref("");
const loadingContacts = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const messageBoxRef = ref(null);
const pollTimer = ref(null);
const polling = ref(false);

const currentUserId = computed(() => userStore.userInfo?.id);
const activeUserId = computed(() => activeContact.value?.userId || null);
const selfAvatarText = computed(() => (userStore.userInfo?.nickname || userStore.userInfo?.username || "?").charAt(0).toUpperCase());

const displayName = (user) => user?.nickname || user?.username || "未知用户";
const avatarText = (user) => displayName(user).charAt(0).toUpperCase();
const isMine = (message) => Number(message?.senderId) === Number(currentUserId.value);

const formatTime = (value) => {
  if (!value) return "";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const today = new Date();
  const sameDay = date.toDateString() === today.toDateString();
  return sameDay
    ? date.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" })
    : date.toLocaleDateString("zh-CN", { month: "2-digit", day: "2-digit" });
};

const scrollBottom = async () => {
  await nextTick();
  if (messageBoxRef.value) {
    messageBoxRef.value.scrollTop = messageBoxRef.value.scrollHeight;
  }
};

const loadContacts = async (options = {}) => {
  if (!options.silent) {
    loadingContacts.value = true;
  }
  try {
    const data = await request.get("/api/private-message/contacts", { silent: true });
    contacts.value = Array.isArray(data) ? data : [];
  } finally {
    if (!options.silent) {
      loadingContacts.value = false;
    }
  }
};

const searchUsers = async () => {
  const kw = keyword.value.trim();
  if (!kw) {
    searchResults.value = [];
    return;
  }
  const data = await request.get("/api/private-message/users", { params: { keyword: kw } });
  searchResults.value = Array.isArray(data) ? data : [];
};

const selectContact = async (contact) => {
  activeContact.value = contact;
  searchResults.value = [];
  await loadConversation(contact.userId);
};

const loadConversation = async (userId, options = {}) => {
  if (!userId) return;
  if (!options.silent) {
    loadingMessages.value = true;
  }
  try {
    const data = await request.get(`/api/private-message/conversation/${userId}`, { silent: true });
    const nextMessages = Array.isArray(data) ? data : [];
    const lastOldId = messages.value[messages.value.length - 1]?.id;
    const lastNewId = nextMessages[nextMessages.length - 1]?.id;
    messages.value = nextMessages;
    await loadContacts({ silent: options.silent });
    if (!options.silent || lastOldId !== lastNewId) {
      await scrollBottom();
    }
  } finally {
    if (!options.silent) {
      loadingMessages.value = false;
    }
  }
};

const pollMessages = async () => {
  if (polling.value) return;
  polling.value = true;
  try {
    await loadContacts({ silent: true });
    if (activeUserId.value) {
      await loadConversation(activeUserId.value, { silent: true });
    }
  } catch (e) {
  } finally {
    polling.value = false;
  }
};

const startPolling = () => {
  if (pollTimer.value) return;
  pollTimer.value = window.setInterval(pollMessages, 3000);
};

const stopPolling = () => {
  if (!pollTimer.value) return;
  window.clearInterval(pollTimer.value);
  pollTimer.value = null;
};

const sendMessage = async () => {
  if (!activeUserId.value || !draft.value.trim()) return;
  sending.value = true;
  try {
    const message = await request.post("/api/private-message/send", {
      receiverId: activeUserId.value,
      content: draft.value.trim(),
    });
    messages.value.push(message);
    draft.value = "";
    await loadContacts();
    await scrollBottom();
  } finally {
    sending.value = false;
  }
};

const openFromRoute = async () => {
  const targetId = Number(route.query.userId);
  if (!targetId) return;
  const existing = contacts.value.find((item) => item.userId === targetId);
  if (existing) {
    await selectContact(existing);
    return;
  }
  const username = route.query.username;
  activeContact.value = {
    userId: targetId,
    username: typeof username === "string" ? username : "",
    nickname: typeof route.query.nickname === "string" ? route.query.nickname : "",
    avatar: typeof route.query.avatar === "string" ? route.query.avatar : "",
  };
  await loadConversation(targetId);
};

watch(
  () => route.query.userId,
  () => openFromRoute(),
);

onMounted(async () => {
  await userStore.fetchUserInfo();
  await loadContacts();
  await openFromRoute();
  startPolling();
});

onUnmounted(stopPolling);
</script>

<style scoped>
.messages-page {
  max-width: 1120px;
  margin: 0 auto;
}

.message-shell {
  display: grid;
  grid-template-columns: 310px 1fr;
  min-height: 680px;
  border: 1px solid var(--tm-line-soft);
  border-radius: 8px;
  background: var(--tm-surface);
  overflow: hidden;
}

.contact-panel {
  border-right: 1px solid var(--tm-line-soft);
  background: oklch(0.985 0.002 248);
  overflow-y: auto;
}

.search-box {
  padding: 16px;
  border-bottom: 1px solid var(--tm-line-soft);
}

.panel-title {
  padding: 14px 16px 8px;
  font-size: 13px;
  font-weight: 700;
  color: var(--tm-muted);
}

.search-results {
  border-bottom: 1px solid var(--tm-line-soft);
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.18s ease;
}

.contact-item:hover,
.contact-item.active {
  background: oklch(0.935 0.030 180);
}

.contact-avatar,
.chat-avatar,
.bubble-avatar {
  background: var(--tm-gradient-brand);
  color: #fff;
  font-weight: 700;
  flex-shrink: 0;
}

.contact-main {
  min-width: 0;
  flex: 1;
}

.contact-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.contact-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--tm-ink);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-sub,
.last-message,
.contact-time,
.chat-sub,
.message-time {
  font-size: 12px;
  color: var(--tm-muted);
}

.last-message {
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #f8fafc;
}

.chat-header {
  height: 74px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid var(--tm-line-soft);
}

.chat-name {
  font-size: 18px;
  font-weight: 800;
  color: var(--tm-ink);
}

.message-list {
  flex: 1;
  padding: 22px;
  overflow-y: auto;
}

.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}

.message-row.mine {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.bubble-wrap {
  max-width: min(68%, 560px);
}

.bubble {
  padding: 10px 13px;
  border-radius: 8px;
  background: #fff;
  color: var(--tm-ink);
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 1px 2px oklch(0.239 0.006 180 / 0.08);
}

.mine .bubble {
  background: var(--tm-olive);
  color: #fff;
}

.mine .message-time {
  text-align: right;
}

.message-time {
  margin-top: 5px;
}

.composer {
  padding: 16px;
  background: #fff;
  border-top: 1px solid var(--tm-line-soft);
}

.composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.empty-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: var(--tm-muted);
}

@media (max-width: 820px) {
  .message-shell {
    grid-template-columns: 1fr;
  }

  .contact-panel {
    max-height: 340px;
    border-right: 0;
    border-bottom: 1px solid var(--tm-line-soft);
  }

  .bubble-wrap {
    max-width: 78%;
  }
}
</style>
