<template>
  <div class="notification-page">
    <PageHeader
      title="通知中心"
      subtitle="查看路线、酒店订单和系统提醒"
      :icon="Bell"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '通知中心' }
      ]"
    >
      <template #extra>
        <div class="header-actions">
          <el-tag v-if="unreadCount > 0" type="danger" effect="dark" round>
            {{ unreadCount }} 条未读
          </el-tag>
          <el-button @click="fetchNotifications" :loading="loading" round>
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
          <el-button
            v-if="unreadCount > 0"
            type="primary"
            plain
            round
            @click="markAllRead"
          >
            全部已读
          </el-button>
          <el-button
            v-if="notifications.length > 0"
            type="danger"
            plain
            round
            @click="deleteAllNotifications"
          >
            批量删除
          </el-button>
        </div>
      </template>
    </PageHeader>

    <!-- 骨架屏 -->
    <div v-if="loading" class="notification-list">
      <SkeletonBox type="list" :count="5" />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="notifications.length === 0"
      icon="bell"
      title="暂无通知"
      description="当有新的路线、酒店订单或系统消息时，会在这里显示"
    />

    <!-- 通知列表 -->
    <div v-else class="notification-list">
      <div
        v-for="item in notifications"
        :key="item.id"
        class="notification-card"
        :class="{ unread: item.isRead === 0, clickable: !!item.actionUrl }"
        @click="openNotification(item)"
      >
        <div class="notif-top">
          <div class="notif-title-row">
            <el-tag :type="getTypeTag(item.type)" effect="dark" size="small" round>
              {{ getTypeLabel(item.type) }}
            </el-tag>
            <span class="notif-title">{{ item.title }}</span>
          </div>
          <span class="notif-time">{{ formatTime(item.createTime) }}</span>
        </div>

        <div class="notif-content">{{ item.content }}</div>

        <div class="notif-footer">
          <div class="notif-status">
            <span class="status-dot" :class="{ 'status-unread': item.isRead === 0 }"></span>
            {{ item.isRead === 0 ? "未读" : "已读" }}
            <span v-if="item.actionUrl" class="jump-hint">点击查看</span>
          </div>
          <div class="notif-actions" @click.stop>
            <el-button
              v-if="item.isRead === 0"
              type="primary"
              text
              size="small"
              @click="markRead(item.id)"
            >
              标记已读
            </el-button>
            <el-button type="danger" text size="small" @click="deleteNotification(item)">
              删除
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Refresh, Bell } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";

const loading = ref(false);
const notifications = ref([]);
const router = useRouter();

const unreadCount = computed(
  () => notifications.value.filter((item) => item.isRead === 0).length
);

const fetchNotifications = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/notification/list");
    notifications.value = Array.isArray(data) ? data : [];
  } catch (e) {
    notifications.value = [];
  } finally {
    loading.value = false;
  }
};

const markRead = async (id, options = {}) => {
  try {
    await request.post(`/api/notification/read/${id}`);
    notifications.value = notifications.value.map((item) =>
      item.id === id ? { ...item, isRead: 1 } : item
    );
    window.dispatchEvent(new Event("notification-updated"));
    if (!options.silent) {
      ElMessage.success("已标记为已读");
    }
  } catch (e) {
    if (!options.silent) {
      ElMessage.error("标记已读失败");
    }
  }
};

const deleteNotification = async (item) => {
  try {
    await ElMessageBox.confirm(`确认删除「${item.title}」吗？`, "删除通知", {
      type: "warning",
      confirmButtonText: "确认删除",
      cancelButtonText: "暂不删除",
    });
    await request.delete(`/api/notification/${item.id}`);
    notifications.value = notifications.value.filter((notice) => notice.id !== item.id);
    window.dispatchEvent(new Event("notification-updated"));
    ElMessage.success("通知已删除");
  } catch (e) {}
};

const deleteAllNotifications = async () => {
  try {
    await ElMessageBox.confirm("确认删除全部通知吗？该操作不可恢复。", "批量删除通知", {
      type: "warning",
      confirmButtonText: "全部删除",
      cancelButtonText: "暂不删除",
    });
    await request.delete("/api/notifications/clear-all");
    notifications.value = [];
    window.dispatchEvent(new Event("notification-updated"));
    ElMessage.success("通知已全部删除");
  } catch (e) {}
};

const openNotification = async (item) => {
  if (!item.actionUrl) return;
  if (item.isRead === 0) {
    await markRead(item.id, { silent: true });
  }
  router.push(item.actionUrl);
};

const markAllRead = async () => {
  const unreadItems = notifications.value.filter((item) => item.isRead === 0);
  if (unreadItems.length === 0) return;

  try {
    await Promise.all(
      unreadItems.map((item) =>
        request.post(`/api/notification/read/${item.id}`)
      )
    );
    notifications.value = notifications.value.map((item) => ({
      ...item,
      isRead: 1,
    }));
    window.dispatchEvent(new Event("notification-updated"));
    ElMessage.success("已全部标记为已读");
  } catch (e) {
    ElMessage.error("批量标记失败");
  }
};

const getTypeLabel = (type) => {
  const map = {
    ai_plan: "路线行程",
    hotel_order: "酒店订单",
    traffic_order: "大交通订单",
    attraction_order: "景点门票",
    post_audit: "游记审核",
    system: "系统通知",
  };
  return map[type] ?? "消息通知";
};

const getTypeTag = (type) => {
  const map = {
    ai_plan: "success",
    hotel_order: "warning",
    traffic_order: "primary",
    attraction_order: "success",
    post_audit: "danger",
    system: "info",
  };
  return map[type] ?? "info";
};

const formatTime = (value) => {
  if (!value) return "";
  return value.replace("T", " ");
};

onMounted(() => {
  fetchNotifications();
});
</script>

<style scoped>
.notification-page {
  max-width: 860px;
  margin: 0 auto;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notification-list {
  display: grid;
  gap: 14px;
}

.notification-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid #F0F2F5;
  transition: all 0.3s ease;
  cursor: default;
}
.notification-card.clickable {
  cursor: pointer;
}
.notification-card:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.05);
}
.notification-card.unread {
  border-left: 4px solid #0D9488;
  background: #FAFFFE;
}

.notif-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.notif-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.notif-title {
  font-size: 16px;
  font-weight: 700;
  color: #1A1A2E;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notif-time {
  font-size: 12px;
  color: #A0A0B8;
  white-space: nowrap;
  flex-shrink: 0;
}

.notif-content {
  color: #71718B;
  line-height: 1.7;
  font-size: 14px;
  margin-bottom: 14px;
}

.notif-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notif-status {
  font-size: 13px;
  color: #A0A0B8;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #C8C8D8;
}
.status-dot.status-unread {
  background: #0D9488;
  animation: pulse 2s ease infinite;
}
.jump-hint {
  color: #0D9488;
  margin-left: 8px;
}
.notif-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

@media (max-width: 768px) {
  .notification-card {
    padding: 16px;
  }
  .notif-top {
    flex-direction: column;
  }
}
</style>
