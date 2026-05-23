<template>
  <div id="app">
    <!-- 导航栏 -->
    <header class="navbar" v-if="!isAuthPage">
      <div class="nav-inner">
        <!-- Logo -->
        <div class="nav-logo" @click="$router.push('/')">
          <span class="logo-icon"
            ><el-icon :size="26"><Promotion /></el-icon
          ></span>
          <span class="logo-text">伴游 <strong>TravelMate</strong></span>
        </div>

        <!-- 桌面端导航链接 -->
        <nav class="nav-links desktop-only">
          <template v-for="link in navLinks" :key="link.path || link.label">
            <el-dropdown
              v-if="link.children"
              trigger="click"
              class="nav-dropdown"
              @command="handleNavCommand"
            >
              <el-button
                :type="isNavActive(link) ? 'primary' : ''"
                :text="!isNavActive(link)"
                class="nav-link-btn"
              >
                {{ link.label }}
                <el-icon class="nav-link-arrow"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="child in link.children"
                    :key="child.path"
                    :command="child.path"
                  >
                    {{ child.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button
              v-else
              :type="isNavActive(link) ? 'primary' : ''"
              :text="!isNavActive(link)"
              class="nav-link-btn"
              @click="$router.push(link.path)"
            >
              {{ link.label }}
            </el-button>
          </template>

          <!-- 全局搜索按钮 -->
          <el-button
            text
            circle
            class="nav-search-btn"
            @click="showSearch = true"
          >
            <el-icon :size="18"><Search /></el-icon>
          </el-button>

          <template v-if="userStore.isLoggedIn">
            <el-badge
              :value="unreadCount"
              :hidden="unreadCount === 0"
              class="nav-badge"
            >
              <el-button text @click="$router.push('/notifications')">
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-badge>
            <el-button
              text
              :type="$route.path === '/my-orders' ? 'primary' : ''"
              @click="$router.push('/my-orders')"
              >我的订单</el-button
            >
            <el-dropdown @command="handleCommand" trigger="click">
              <span class="user-chip">
                <el-avatar
                  :size="32"
                  :src="userStore.userInfo?.avatar"
                  class="user-avatar"
                >
                  {{
                    (userStore.userInfo?.nickname ||
                      userStore.userInfo?.username)?.[0]
                  }}
                </el-avatar>
                <span class="user-name">
                  {{
                    userStore.userInfo?.nickname || userStore.userInfo?.username
                  }}
                </span>
                <el-icon class="user-arrow"><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人主页
                  </el-dropdown-item>
                  <el-dropdown-item
                    v-if="userStore.userInfo?.role === 1"
                    command="admin"
                  >
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
            <el-button type="primary" round @click="$router.push('/login')"
              >登录 / 注册</el-button
            >
          </template>
        </nav>

        <!-- 移动端菜单按钮 -->
        <div class="mobile-only mobile-nav-actions">
          <el-button text circle @click="showSearch = true">
            <el-icon :size="20"><Search /></el-icon>
          </el-button>
          <el-badge
            v-if="userStore.isLoggedIn"
            :value="unreadCount"
            :hidden="unreadCount === 0"
          >
            <el-button text circle @click="$router.push('/notifications')">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-button text circle @click="showMobileMenu = true">
            <el-icon :size="22"><Expand /></el-icon>
          </el-button>
        </div>
      </div>
    </header>

    <!-- 移动端抽屉菜单 -->
    <el-drawer
      v-model="showMobileMenu"
      title=""
      direction="ltr"
      size="280px"
      :show-close="false"
      class="mobile-drawer"
    >
      <template #header>
        <div class="mobile-drawer-header">
          <span class="mobile-drawer-brand"
            ><el-icon style="margin-right: 6px"><Promotion /></el-icon>伴游
            TravelMate</span
          >
          <el-button text circle @click="showMobileMenu = false">
            <el-icon :size="20"><Close /></el-icon>
          </el-button>
        </div>
      </template>
      <div class="mobile-menu">
        <div
          class="mobile-menu-item"
          v-for="link in mobileNavLinks"
          :key="link.path"
          :class="{ active: isNavActive(link) }"
          @click="mobileNav(link.path)"
        >
          {{ link.label }}
        </div>
        <div class="mobile-menu-divider"></div>
        <template v-if="userStore.isLoggedIn">
          <div class="mobile-menu-item" @click="mobileNav('/my-orders')">
            <el-icon style="margin-right: 8px"><Tickets /></el-icon>我的订单
          </div>
          <div class="mobile-menu-item" @click="mobileNav('/notifications')">
            <el-icon style="margin-right: 8px"><Bell /></el-icon>通知中心
          </div>
          <div
            class="mobile-menu-item"
            @click="mobileNav(`/profile/${userStore.userInfo?.username}`)"
          >
            <el-icon style="margin-right: 8px"><User /></el-icon>个人主页
          </div>
          <div
            v-if="userStore.userInfo?.role === 1"
            class="mobile-menu-item"
            @click="mobileNav('/admin')"
          >
            <el-icon style="margin-right: 8px"><Setting /></el-icon>管理后台
          </div>
          <div class="mobile-menu-divider"></div>
          <div class="mobile-menu-item logout" @click="handleLogout">
            退出登录
          </div>
        </template>
        <template v-else>
          <div class="mobile-menu-item" @click="mobileNav('/login')">
            登录 / 注册
          </div>
        </template>
      </div>
    </el-drawer>

    <!-- 全局搜索弹窗 -->
    <el-dialog
      v-model="showSearch"
      title=""
      width="680px"
      :show-close="false"
      class="search-dialog"
      @opened="searchInputRef?.focus()"
    >
      <template #header>
        <div class="search-dialog-header">
          <el-icon :size="20" class="search-header-icon"><Search /></el-icon>
          <span class="search-dialog-title">快速搜索</span>
        </div>
      </template>

      <el-tabs v-model="searchTab" class="search-tabs">
        <el-tab-pane name="flight">
          <template #label
            ><el-icon style="margin-right: 4px"><Promotion /></el-icon
            >机票</template
          >
          <div class="search-fields">
            <el-input
              v-model="gsFlight.depCity"
              placeholder="出发城市"
              size="large"
            />
            <el-icon class="search-swap-icon"><Right /></el-icon>
            <el-input
              v-model="gsFlight.arrCity"
              placeholder="到达城市"
              size="large"
            />
            <el-date-picker
              v-model="gsFlight.date"
              type="date"
              placeholder="出发日期"
              size="large"
              value-format="YYYY-MM-DD"
              style="width: 160px"
            />
            <el-button
              type="primary"
              size="large"
              @click="doSearch('flight')"
              round
            >
              搜索机票
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane name="train">
          <template #label
            ><el-icon style="margin-right: 4px"><Tickets /></el-icon
            >火车票</template
          >
          <div class="search-fields">
            <el-input
              v-model="gsTrain.depStation"
              placeholder="出发站"
              size="large"
            />
            <el-icon class="search-swap-icon"><Right /></el-icon>
            <el-input
              v-model="gsTrain.arrStation"
              placeholder="到达站"
              size="large"
            />
            <el-date-picker
              v-model="gsTrain.date"
              type="date"
              placeholder="出发日期"
              size="large"
              value-format="YYYY-MM-DD"
              style="width: 160px"
            />
            <el-button
              type="primary"
              size="large"
              @click="doSearch('train')"
              round
            >
              搜索火车票
            </el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane name="hotel">
          <template #label
            ><el-icon style="margin-right: 4px"><House /></el-icon
            >酒店</template
          >
          <div class="search-fields">
            <el-input
              v-model="gsHotel.city"
              placeholder="目的城市"
              size="large"
            />
            <el-button
              type="primary"
              size="large"
              @click="doSearch('hotel')"
              round
            >
              搜索酒店
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- 面包屑导航 -->
    <div class="breadcrumb-bar" v-if="breadcrumbItems.length && !isAuthPage">
      <div class="breadcrumb-inner">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item
            v-for="(crumb, idx) in breadcrumbItems"
            :key="idx"
            :to="idx < breadcrumbItems.length - 1 ? crumb.to : undefined"
          >
            {{ crumb.label }}
          </el-breadcrumb-item>
        </el-breadcrumb>
      </div>
    </div>

    <!-- 主内容区 -->
    <main class="main-content" :class="{ 'no-header': isAuthPage }">
      <router-view v-slot="{ Component, route }">
        <transition name="page" mode="out-in">
          <component :is="Component" :key="route.fullPath" />
        </transition>
      </router-view>
    </main>

    <!-- 底部 Footer -->
    <footer class="app-footer" v-if="!isAuthPage">
      <div class="footer-inner">
        <div class="footer-grid">
          <div class="footer-col footer-brand">
            <h3>
              <el-icon style="margin-right: 4px"><Promotion /></el-icon>伴游
              TravelMate
            </h3>
            <p>一站式智慧出行平台，让你的每次旅行都精彩</p>
            <div class="footer-social">
              <button
                v-for="member in teamMembers"
                :key="member.name"
                class="team-avatar-btn"
                type="button"
                :title="`${member.name} · ${member.moduleCode}模块`"
                @click="selectedTeamMember = member"
              >
                <el-avatar
                  :size="36"
                  :src="member.avatar"
                  class="team-avatar"
                  :style="{ background: member.gradient }"
                >
                  {{ member.initial }}
                </el-avatar>
              </button>
            </div>
          </div>
          <div class="footer-col">
            <h4>出行服务</h4>
            <a @click="footerNav('/flight-search')">机票预订</a>
            <a @click="footerNav('/train-search')">火车票预订</a>
            <a @click="footerNav('/hotel-search')">酒店预订</a>
            <a @click="footerNav('/attractions')">热门景点</a>
          </div>
          <div class="footer-col">
            <h4>发现更多</h4>
            <a @click="footerNav('/ai-plan')">AI 行程规划</a>
            <a @click="footerNav('/community')">旅行社区</a>
            <a @click="footerNav('/my-orders')">我的订单</a>
          </div>
          <div class="footer-col">
            <h4>关于我们</h4>
            <a>关于 TravelMate</a>
            <a>用户协议</a>
            <a>隐私政策</a>
            <a>帮助中心</a>
          </div>
        </div>
        <div class="footer-bottom">
          <span>&copy; 2026 TravelMate 伴游 — 软件工程课程项目</span>
          <span class="footer-credit"
            >Made with passion by TravelMate Team</span
          >
        </div>
      </div>
    </footer>

    <el-dialog
      v-model="teamDialogVisible"
      width="420px"
      class="team-dialog"
      :show-close="true"
    >
      <template #header>
        <div class="team-dialog-header" v-if="selectedTeamMember">
          <el-avatar
            :size="56"
            :src="selectedTeamMember.avatar"
            class="team-dialog-avatar"
            :style="{ background: selectedTeamMember.gradient }"
          >
            {{ selectedTeamMember.initial }}
          </el-avatar>
          <div>
            <div class="team-dialog-name">{{ selectedTeamMember.name }}</div>
            <div class="team-dialog-role">
              {{ selectedTeamMember.moduleCode }} · {{ selectedTeamMember.module }}
            </div>
          </div>
        </div>
      </template>
      <div v-if="selectedTeamMember" class="team-dialog-body">
        <div class="team-info-row">
          <span class="team-info-label">负责模块</span>
          <span>{{ selectedTeamMember.module }}</span>
        </div>
        <div class="team-info-row">
          <span class="team-info-label">个人简介</span>
          <span>{{ selectedTeamMember.bio }}</span>
        </div>
        <div class="team-info-row">
          <span class="team-info-label">联系方式</span>
          <span>{{ selectedTeamMember.contact }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  ArrowDown,
  User,
  Setting,
  SwitchButton,
  Search,
  Bell,
  Expand,
  Close,
  Right,
  Promotion,
  Notebook,
  Cpu,
  Tickets,
  House,
  HomeFilled,
  Present,
} from "@element-plus/icons-vue";
import { useUserStore } from "./stores/user";
import request from "@/utils/request";
import yfanAvatar from "@/assets/team/YFan.jpg";
import yangYouthAvatar from "@/assets/team/YangYouth.jpg";
import sylphiraAvatar from "@/assets/team/Sylphira.jpg";
import mojireeAvatar from "@/assets/team/Mojiree.jpg";
import dxcAvatar from "@/assets/team/DXC.jpg";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const unreadCount = ref(0);
const showSearch = ref(false);
const showMobileMenu = ref(false);
const searchTab = ref("flight");
const searchInputRef = ref(null);
const selectedTeamMember = ref(null);

const teamMembers = [
  {
    name: "YFan",
    initial: "Y",
    moduleCode: "E",
    module: "管理后台与可观测性",
    bio: "负责后台运营管理、内容审核、系统日志与数据看板相关设计。",
    contact: "微信号待补充",
    avatar: yfanAvatar,
    gradient: "linear-gradient(135deg, #0f766e, #2563eb)",
  },
  {
    name: "YangYouth",
    initial: "Y",
    moduleCode: "A",
    module: "大交通票务 / 订单库存",
    bio: "负责机票、火车票、库存防超卖和订单预占流程相关设计。",
    contact: "微信号待补充",
    avatar: yangYouthAvatar,
    gradient: "linear-gradient(135deg, #dc2626, #f59e0b)",
  },
  {
    name: "Sylphira",
    initial: "S",
    moduleCode: "C",
    module: "AI 智能规划",
    bio: "负责 AI 行程生成、智能客服、Prompt 设计和工具调用相关能力。",
    contact: "微信号待补充",
    avatar: sylphiraAvatar,
    gradient: "linear-gradient(135deg, #7c3aed, #db2777)",
  },
  {
    name: "Mojiree",
    initial: "M",
    moduleCode: "B",
    module: "住宿与本地生活",
    bio: "负责酒店、房型、景点、本地玩乐和评价体系相关设计。",
    contact: "微信号待补充",
    avatar: mojireeAvatar,
    gradient: "linear-gradient(135deg, #16a34a, #0ea5e9)",
  },
  {
    name: "DXC",
    initial: "D",
    moduleCode: "D",
    module: "社区与用户中心",
    bio: "负责旅行社区、互动关系、评论收藏和用户中心相关设计。",
    contact: "微信号待补充",
    avatar: dxcAvatar,
    gradient: "linear-gradient(135deg, #334155, #0891b2)",
  },
];

const teamDialogVisible = computed({
  get: () => !!selectedTeamMember.value,
  set: (visible) => {
    if (!visible) selectedTeamMember.value = null;
  },
});

// ---------- 全局搜索表单 ----------
const gsFlight = ref({ depCity: "", arrCity: "", date: "" });
const gsTrain = ref({ depStation: "", arrStation: "", date: "" });
const gsHotel = ref({ city: "" });

const navLinks = [
  { path: "/", label: "首页" },
  {
    label: "交通",
    children: [
      { path: "/flight-search", label: "机票" },
      { path: "/train-search", label: "火车票" },
    ],
  },
  {
    label: "出游",
    children: [
      { path: "/hotel-search", label: "酒店" },
      { path: "/attractions", label: "热门景点" },
    ],
  },
  { path: "/community", label: "社区", activePaths: ["/community", "/post"] },
  { path: "/ai-plan", label: "AI规划" },
  { path: "/coupons", label: "优惠券" },
];

const mobileNavLinks = computed(() =>
  navLinks.flatMap((link) => link.children || [link]),
);

const isAuthPage = computed(() => route.path === "/login");

// ---------- 面包屑 ----------
const breadcrumbRouteMap = {
  Home: [{ label: "首页", to: "/" }],
  FlightSearch: [{ label: "首页", to: "/" }, { label: "机票搜索" }],
  TrainSearch: [{ label: "首页", to: "/" }, { label: "火车票搜索" }],
  HotelSearch: [{ label: "首页", to: "/" }, { label: "酒店搜索" }],
  HotelDetail: [
    { label: "首页", to: "/" },
    { label: "酒店搜索", to: "/hotel-search" },
    { label: "酒店详情" },
  ],
  AttractionList: [{ label: "首页", to: "/" }, { label: "热门景点" }],
  AiPlan: [{ label: "首页", to: "/" }, { label: "AI 行程规划" }],
  Community: [{ label: "首页", to: "/" }, { label: "旅行社区" }],
  PostCreate: [
    { label: "首页", to: "/" },
    { label: "旅行社区", to: "/community" },
    { label: "发布游记" },
  ],
  PostDetail: [
    { label: "首页", to: "/" },
    { label: "旅行社区", to: "/community" },
    { label: "游记详情" },
  ],
  MyOrders: [{ label: "首页", to: "/" }, { label: "我的订单" }],
  CouponCenter: [{ label: "首页", to: "/" }, { label: "优惠券中心" }],
  NotificationCenter: [{ label: "首页", to: "/" }, { label: "通知中心" }],
  UserProfile: [{ label: "首页", to: "/" }, { label: "用户主页" }],
  AdminDashboard: [{ label: "首页", to: "/" }, { label: "管理后台" }],
};

const breadcrumbItems = computed(() => {
  const routeName = route.name;
  return breadcrumbRouteMap[routeName] || [];
});

// ---------- 通知未读数 ----------
const fetchUnreadCount = async () => {
  if (!userStore.isLoggedIn) {
    unreadCount.value = 0;
    return;
  }
  try {
    const data = await request.get("/api/notification/unread-count");
    unreadCount.value = data || 0;
  } catch (e) {
    // 忽略
  }
};

const handleNotificationUpdated = () => fetchUnreadCount();

// ---------- 导航操作 ----------
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

const handleLogout = () => {
  userStore.logout();
  router.push("/login");
  showMobileMenu.value = false;
};

const mobileNav = (path) => {
  router.push(path);
  showMobileMenu.value = false;
};

const handleNavCommand = (path) => {
  if (path) router.push(path);
};

const isNavActive = (link) => {
  if (link.children) return link.children.some((child) => isNavActive(child));
  if (link.path === "/") return route.path === "/";
  if (link.activePaths) {
    return link.activePaths.some((path) => route.path.startsWith(path));
  }
  return route.path === link.path;
};

const footerNav = (path) => {
  if (path) router.push(path);
};

// ---------- 全局搜索 ----------
const doSearch = (type) => {
  showSearch.value = false;
  if (type === "flight") {
    router.push({
      path: "/flight-search",
      query: {
        depCity: gsFlight.value.depCity,
        arrCity: gsFlight.value.arrCity,
        date: gsFlight.value.date,
      },
    });
  } else if (type === "train") {
    router.push({
      path: "/train-search",
      query: {
        depStation: gsTrain.value.depStation,
        arrStation: gsTrain.value.arrStation,
        date: gsTrain.value.date,
      },
    });
  } else if (type === "hotel") {
    router.push({
      path: "/hotel-search",
      query: { city: gsHotel.value.city },
    });
  }
};

// ---------- 生命周期 ----------
onMounted(() => {
  document.documentElement.classList.remove("dark");
  localStorage.removeItem("theme");
  window.addEventListener("notification-updated", handleNotificationUpdated);
  if (userStore.isLoggedIn) {
    userStore.fetchUserInfo();
    fetchUnreadCount();
  }
});

onUnmounted(() => {
  window.removeEventListener("notification-updated", handleNotificationUpdated);
});

watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      userStore.fetchUserInfo();
      fetchUnreadCount();
    } else {
      unreadCount.value = 0;
    }
  },
);

watch(
  () => route.fullPath,
  () => {
    if (userStore.isLoggedIn) fetchUnreadCount();
  },
);
</script>

<style scoped>
#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color-page);
}

/* ==================== 导航栏 ==================== */
.navbar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
}

.nav-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: var(--tm-page-max-width);
  margin: 0 auto;
  padding: 0 var(--tm-page-padding);
  height: 64px;
}

/* Logo */
.nav-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
}

.logo-icon {
  display: inline-flex;
  align-items: center;
  color: var(--el-color-primary);
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.nav-logo:hover .logo-icon {
  transform: scale(1.15) rotate(-8deg);
}

.logo-text {
  font-size: 20px;
  color: var(--el-text-color-primary);
  letter-spacing: 0.2px;
}
.logo-text strong {
  font-weight: 800;
  background: var(--tm-gradient-brand);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 4s ease infinite;
}

/* 桌面端导航 */
.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nav-link-btn {
  min-width: 64px;
  font-weight: 700;
}

.nav-dropdown {
  display: inline-flex;
}

.nav-link-arrow {
  margin-left: 4px;
  font-size: 12px;
}

.nav-badge {
  margin-right: 2px;
}

.nav-search-btn {
  margin: 0 6px;
  color: var(--el-text-color-secondary);
  transition: all 0.3s ease;
}
.nav-search-btn:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

/* 用户头像区 */
.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 4px 14px 4px 4px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
  margin-left: 4px;
}
.user-chip:hover {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-8);
}

.user-avatar {
  background: linear-gradient(135deg, #0d9488, #0ea5e9);
  color: #fff;
  font-weight: 700;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-arrow {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  transition: transform 0.3s ease;
}
.user-chip:hover .user-arrow {
  transform: rotate(180deg);
}

/* ==================== 移动端 ==================== */
.mobile-only {
  display: none;
}
.desktop-only {
  display: flex;
}

@media (max-width: 768px) {
  .mobile-only {
    display: flex;
  }
  .desktop-only {
    display: none;
  }

  .nav-inner {
    height: 56px;
    padding: 0 12px;
  }

  .logo-text {
    font-size: 16px;
  }

  .mobile-nav-actions {
    gap: 2px;
  }
}

/* 移动端抽屉 */
.mobile-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mobile-drawer-brand {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.mobile-menu {
  padding: 4px 0;
}

.mobile-menu-item {
  padding: 14px 20px;
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-regular);
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 10px;
  margin: 2px 12px;
}
.mobile-menu-item:hover {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.mobile-menu-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 700;
}
.mobile-menu-item.logout {
  color: var(--el-color-danger);
}

.mobile-menu-divider {
  height: 1px;
  background: var(--el-border-color-light);
  margin: 8px 20px;
}

/* ==================== 全局搜索弹窗 ==================== */
.search-dialog :deep(.el-dialog__header) {
  padding: 24px 28px 0;
  border-bottom: none;
}
.search-dialog :deep(.el-dialog__body) {
  padding: 20px 28px 32px;
}

.search-dialog-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-header-icon {
  color: var(--el-color-primary);
}

.search-dialog-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.search-fields {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-fields .el-input {
  flex: 1;
  min-width: 140px;
}

.search-swap-icon {
  color: var(--el-text-color-placeholder);
  flex-shrink: 0;
}

.search-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

/* ==================== 面包屑 ==================== */
.breadcrumb-bar {
  border-bottom: 1px solid var(--el-border-color-light);
  background: #fff;
}

.breadcrumb-inner {
  max-width: var(--tm-page-max-width);
  margin: 0 auto;
  padding: 12px var(--tm-page-padding);
}

/* ==================== 主内容区 ==================== */
.main-content {
  flex: 1;
  padding: 28px var(--tm-page-padding);
  max-width: var(--tm-page-max-width);
  margin: 0 auto;
  width: 100%;
}
.main-content.no-header {
  padding: 0;
  max-width: 100%;
}

@media (max-width: 768px) {
  .main-content {
    padding: 16px var(--tm-page-padding);
  }
}

/* 页面过渡动画 */
.page-enter-active {
  animation: fadeInUp 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}
.page-leave-active {
  animation: fadeIn 0.2s cubic-bezier(0.4, 0, 0.2, 1) reverse;
}

/* ==================== Footer ==================== */
.app-footer {
  background: #fff;
  border-top: 1px solid var(--el-border-color-light);
  margin-top: auto;
}

.footer-inner {
  max-width: var(--tm-page-max-width);
  margin: 0 auto;
  padding: 48px var(--tm-page-padding) 24px;
}

.footer-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 40px;
  margin-bottom: 40px;
}

.footer-col h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  margin-bottom: 12px;
}

.footer-col h4 {
  font-size: 14px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 16px;
}

.footer-col p {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  margin-bottom: 16px;
}

.footer-col a {
  display: block;
  font-size: 14px;
  color: var(--el-text-color-secondary);
  padding: 5px 0;
  cursor: pointer;
  transition: color 0.2s ease;
}
.footer-col a:hover {
  color: var(--el-color-primary);
}

.footer-social {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.team-avatar-btn {
  width: 38px;
  height: 38px;
  padding: 0;
  border-radius: 50%;
  border: 1px solid #f0f2f5;
  background: #fff;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;
}

.team-avatar-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.12);
  border-color: var(--el-color-primary-light-5);
}

.team-avatar {
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  box-shadow: inset 0 0 0 2px rgba(255, 255, 255, 0.72);
}

.team-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.team-dialog-header {
  display: flex;
  align-items: center;
  gap: 14px;
}

.team-dialog-avatar {
  color: #fff;
  font-size: 20px;
  font-weight: 800;
}

.team-dialog-name {
  font-size: 20px;
  font-weight: 800;
  color: var(--el-text-color-primary);
}

.team-dialog-role {
  margin-top: 3px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.team-dialog-body {
  display: grid;
  gap: 14px;
  padding-top: 4px;
}

.team-info-row {
  display: grid;
  grid-template-columns: 76px 1fr;
  gap: 12px;
  align-items: start;
  font-size: 14px;
  line-height: 1.7;
  color: var(--el-text-color-regular);
}

.team-info-label {
  color: var(--el-text-color-secondary);
  font-weight: 700;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid var(--el-border-color-light);
  font-size: 13px;
  color: var(--el-text-color-placeholder);
}

.footer-credit {
  font-size: 12px;
}

@media (max-width: 768px) {
  .footer-grid {
    grid-template-columns: 1fr 1fr;
    gap: 28px 20px;
  }
  .footer-brand {
    grid-column: 1 / -1;
  }
  .footer-bottom {
    flex-direction: column;
    gap: 8px;
    text-align: center;
  }
}
</style>
