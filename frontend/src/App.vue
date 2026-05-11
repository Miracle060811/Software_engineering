<template>
  <div id="app">
    <header class="navbar" v-if="!isAuthPage">
      <div class="nav-inner">
        <div class="nav-logo" @click="$router.push('/')">
          <span class="logo-icon">✈️</span>
          <span class="logo-text">伴游 <strong>TravelMate</strong></span>
        </div>
        <nav class="nav-links">
          <el-button
            v-for="link in navLinks"
            :key="link.path"
            :type="$route.path === link.path ? 'primary' : ''"
            :text="$route.path !== link.path"
            @click="$router.push(link.path)"
          >
            {{ link.label }}
          </el-button>
          <template v-if="userStore.isLoggedIn">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="nav-badge">
              <el-button text @click="$router.push('/my-orders')">我的订单</el-button>
            </el-badge>
            <el-dropdown @command="handleCommand" trigger="click">
              <span class="user-chip">
                <el-avatar
                  :size="32"
                  :src="userStore.userInfo?.avatar"
                  class="user-avatar"
                >
                  {{ (userStore.userInfo?.nickname || userStore.userInfo?.username)?.[0] }}
                </el-avatar>
                <span class="user-name">
                  {{ userStore.userInfo?.nickname || userStore.userInfo?.username }}
                </span>
                <el-icon class="user-arrow"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人主页
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.userInfo?.role === 1" command="admin">
                    <el-icon><Setting /></el-icon>管理后台
                  </el-dropdown-item>
                  <el-dropdown-item command="logout" divided>
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" round @click="$router.push('/login')">登录 / 注册</el-button>
          </template>
        </nav>
      </div>
    </header>
    <main class="main-content" :class="{ 'no-header': isAuthPage }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ArrowDown, User, Setting, SwitchButton } from "@element-plus/icons-vue";
import { useUserStore } from "./stores/user";
import request from "./utils/request";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const unreadCount = ref(0);

const navLinks = [
  { path: "/", label: "🏠 首页" },
  { path: "/community", label: "📖 社区" },
  { path: "/ai-plan", label: "🤖 AI规划" },
];

const isAuthPage = computed(() => route.path === "/login");

const fetchUnreadCount = async () => {
  if (!userStore.isLoggedIn) return;
  try {
    const data = await request.get("/api/notification/unread-count");
    unreadCount.value = data || 0;
  } catch (e) {
    // 忽略
  }
};

const handleCommand = (cmd) => {
  if (cmd === "logout") {
    userStore.logout();
    router.push("/login");
  } else if (cmd === "profile") {
    const username = userStore.userInfo?.username;
    if (username) router.push(`/profile/${username}`);
  } else if (cmd === "admin") {
    router.push("/admin");
  }
};

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUserInfo();
    fetchUnreadCount();
  }
});
</script>

<style scoped>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

/* ---- 导航栏 ---- */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 40px;
  height: 64px;
}

.nav-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.logo-icon {
  font-size: 26px;
  transition: transform 0.3s ease;
}
.nav-logo:hover .logo-icon {
  transform: scale(1.15) rotate(-5deg);
}

.logo-text {
  font-size: 20px;
  color: #1E293B;
  letter-spacing: 0.5px;
}
.logo-text strong {
  font-weight: 700;
  background: linear-gradient(135deg, #0D9488 0%, #10B981 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-badge {
  margin-right: 4px;
}

/* 用户头像区域 */
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}
.user-chip:hover {
  background: #F0FDFA;
  border-color: #CCFBF1;
}

.user-avatar {
  background: linear-gradient(135deg, #0D9488, #10B981);
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  font-size: 12px;
  color: #94A3B8;
  transition: transform 0.2s;
}
.user-chip:hover .user-arrow {
  transform: rotate(180deg);
}

/* ---- 主内容区 ---- */
.main-content {
  flex: 1;
  padding: 28px 40px;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}
.main-content.no-header {
  padding: 0;
  max-width: 100%;
}
</style>
