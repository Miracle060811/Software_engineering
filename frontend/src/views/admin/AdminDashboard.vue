<template>
  <div class="admin-page">
    <el-container>
      <!-- 左侧菜单 -->
      <el-aside width="200px" class="admin-aside">
        <div class="admin-logo">管理后台</div>
        <el-menu
          :default-active="activeMenu"
          @select="activeMenu = $event"
          class="admin-menu"
        >
          <el-menu-item index="stats">
            <el-icon><DataLine /></el-icon>
            数据统计
          </el-menu-item>
          <el-menu-item index="flights">
            <el-icon><Promotion /></el-icon>
            航班管理
          </el-menu-item>
          <el-menu-item index="hotels">
            <el-icon><House /></el-icon>
            酒店管理
          </el-menu-item>
          <el-menu-item index="posts">
            <el-icon><Document /></el-icon>
            游记审核
          </el-menu-item>
          <el-menu-item index="users">
            <el-icon><User /></el-icon>
            用户管理
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 右侧内容 -->
      <el-main class="admin-main">
        <!-- 数据统计 -->
        <div v-if="activeMenu === 'stats'">
          <h2 class="section-title">数据统计</h2>
          <el-row :gutter="20" v-loading="statsLoading">
            <el-col :span="6" v-for="stat in statCards" :key="stat.label">
              <el-card class="stat-card">
                <div class="stat-icon">{{ stat.icon }}</div>
                <div class="stat-value">{{ stat.value }}</div>
                <div class="stat-label">{{ stat.label }}</div>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 航班管理 -->
        <div v-if="activeMenu === 'flights'">
          <h2 class="section-title">航班管理</h2>
          <el-table :data="flights" v-loading="flightLoading" stripe>
            <el-table-column prop="flightNo" label="航班号" width="120" />
            <el-table-column prop="airline" label="航司" width="120" />
            <el-table-column prop="departureCity" label="出发城市" />
            <el-table-column prop="arrivalCity" label="到达城市" />
            <el-table-column prop="economyPrice" label="经济舱价格" width="120">
              <template #default="scope"
                >¥{{ scope.row.economyPrice }}</template
              >
            </el-table-column>
            <el-table-column prop="availableSeats" label="余票" width="80" />
          </el-table>
        </div>

        <!-- 酒店管理 -->
        <div v-if="activeMenu === 'hotels'">
          <h2 class="section-title">酒店管理</h2>
          <el-table :data="hotels" v-loading="hotelLoading" stripe>
            <el-table-column prop="name" label="酒店名称" />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column prop="starRating" label="星级" width="80" />
            <el-table-column prop="rating" label="评分" width="80" />
            <el-table-column prop="address" label="地址" />
          </el-table>
        </div>

        <!-- 游记审核 -->
        <div v-if="activeMenu === 'posts'">
          <h2 class="section-title">游记审核</h2>
          <el-table :data="pendingPosts" v-loading="postLoading" stripe>
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="authorUsername" label="作者" width="120" />
            <el-table-column prop="destination" label="目的地" width="100" />
            <el-table-column prop="createTime" label="发布时间" width="160" />
            <el-table-column label="状态" width="80">
              <template #default="scope">
                <el-tag type="warning">待审核</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button
                  type="success"
                  size="small"
                  @click="approvePost(scope.row.id)"
                >
                  通过
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="rejectPost(scope.row.id)"
                >
                  拒绝
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 用户管理 -->
        <div v-if="activeMenu === 'users'">
          <h2 class="section-title">用户管理</h2>
          <el-table :data="users" v-loading="userLoading" stripe>
            <el-table-column prop="username" label="用户名" width="150" />
            <el-table-column prop="nickname" label="昵称" width="150" />
            <el-table-column label="角色" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.role === 1 ? 'danger' : ''">
                  {{ scope.row.role === 1 ? "管理员" : "普通用户" }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="注册时间" />
            <el-table-column label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === 0 ? 'success' : 'info'">
                  {{ scope.row.status === 0 ? "正常" : "禁用" }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from "vue";
import { ElMessage } from "element-plus";
import {
  DataLine,
  Promotion,
  House,
  Document,
  User,
} from "@element-plus/icons-vue";
import request from "@/utils/request";

const activeMenu = ref("stats");

// 数据统计
const statsLoading = ref(false);
const statsData = ref({});
const statCards = ref([
  { icon: "👥", label: "总用户数", value: "—" },
  { icon: "📋", label: "总订单数", value: "—" },
  { icon: "⏳", label: "待审核游记", value: "—" },
  { icon: "🆕", label: "今日新增", value: "—" },
]);

// 其他数据
const flights = ref([]);
const flightLoading = ref(false);
const hotels = ref([]);
const hotelLoading = ref(false);
const pendingPosts = ref([]);
const postLoading = ref(false);
const users = ref([]);
const userLoading = ref(false);

const fetchStats = async () => {
  statsLoading.value = true;
  try {
    const data = await request.get("/api/admin/stats");
    if (data) {
      statCards.value = [
        { icon: "👥", label: "总用户数", value: data.totalUsers ?? "—" },
        { icon: "📋", label: "总订单数", value: data.totalOrders ?? "—" },
        { icon: "⏳", label: "待审核游记", value: data.pendingPosts ?? "—" },
        { icon: "🆕", label: "今日新增", value: data.todayNew ?? "—" },
      ];
    }
  } catch (e) {
  } finally {
    statsLoading.value = false;
  }
};

const fetchFlights = async () => {
  flightLoading.value = true;
  try {
    const data = await request.get("/api/admin/flights");
    flights.value = Array.isArray(data) ? data : [];
  } catch (e) {
    flights.value = [];
  } finally {
    flightLoading.value = false;
  }
};

const fetchHotels = async () => {
  hotelLoading.value = true;
  try {
    const data = await request.get("/api/admin/hotels");
    hotels.value = Array.isArray(data) ? data : [];
  } catch (e) {
    hotels.value = [];
  } finally {
    hotelLoading.value = false;
  }
};

const fetchPendingPosts = async () => {
  postLoading.value = true;
  try {
    const data = await request.get("/api/admin/posts", {
      params: { status: 0 },
    });
    pendingPosts.value = Array.isArray(data) ? data : [];
  } catch (e) {
    pendingPosts.value = [];
  } finally {
    postLoading.value = false;
  }
};

const fetchUsers = async () => {
  userLoading.value = true;
  try {
    const data = await request.get("/api/admin/users");
    users.value = Array.isArray(data) ? data : [];
  } catch (e) {
    users.value = [];
  } finally {
    userLoading.value = false;
  }
};

const approvePost = async (id) => {
  try {
    await request.post(`/api/admin/posts/${id}/approve`);
    ElMessage.success("已通过");
    await fetchPendingPosts();
  } catch (e) {}
};

const rejectPost = async (id) => {
  try {
    await request.post(`/api/admin/posts/${id}/reject`);
    ElMessage.success("已拒绝");
    await fetchPendingPosts();
  } catch (e) {}
};

watch(activeMenu, (val) => {
  if (val === "stats") fetchStats();
  else if (val === "flights") fetchFlights();
  else if (val === "hotels") fetchHotels();
  else if (val === "posts") fetchPendingPosts();
  else if (val === "users") fetchUsers();
});

onMounted(() => {
  fetchStats();
});
</script>

<style scoped>
.admin-page {
  margin: -24px -40px;
  min-height: calc(100vh - 60px);
}
.admin-aside {
  background: #0F172A;
  min-height: calc(100vh - 60px);
}
.admin-logo {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  padding: 20px 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.admin-menu {
  background: #0F172A;
  border-right: none;
}
.admin-menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.55);
}
.admin-menu :deep(.el-menu-item:hover),
.admin-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, #0D9488, #10B981);
  color: #fff;
}
.admin-main {
  background: #F8FAFC;
  padding: 24px;
}
.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}
.stat-card {
  text-align: center;
  border-radius: 10px;
  padding: 8px;
}
.stat-icon {
  font-size: 36px;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(135deg, #0D9488, #10B981);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: 6px;
}
.stat-label {
  font-size: 14px;
  color: #999;
}
</style>
