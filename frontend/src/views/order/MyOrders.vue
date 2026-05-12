<template>
  <div class="my-orders-page">
    <PageHeader
      title="我的订单"
      subtitle="管理大交通票务和酒店预订订单"
      :icon="Tickets"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '我的订单' }
      ]"
    />

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="order-tabs">
      <!-- 大交通订单 -->
      <el-tab-pane label="大交通订单" name="traffic">
        <!-- 骨架屏 -->
        <div v-if="trafficLoading">
          <SkeletonBox type="list" :count="3" />
        </div>

        <!-- 空状态 -->
        <EmptyState
          v-else-if="trafficOrders.length === 0"
          icon="tickets"
          title="暂无大交通订单"
          description="还没有机票或火车票订单，快去订票吧"
          action-text="去搜索机票"
          @action="$router.push('/flight-search')"
        />

        <el-card
          v-else
          v-for="order in trafficOrders"
          :key="order.orderNo"
          class="order-card"
        >
          <div class="order-header">
            <el-tag :type="order.orderType === 0 ? 'primary' : 'success'" effect="light" round>
              {{ order.orderType === 0 ? "机票" : "火车票" }}
            </el-tag>
            <span class="order-no">{{ order.orderNo }}</span>
            <el-tag :type="getStatusType(order.status)" round size="small">
              {{ getTrafficStatusLabel(order.status) }}
            </el-tag>
          </div>
          <div class="order-body">
            <div class="order-route">
              <span class="route-city">{{ order.departureCity || order.departureStation }}</span>
              <span class="route-arrow">→</span>
              <span class="route-city">{{ order.arrivalCity || order.arrivalStation }}</span>
            </div>
            <div class="order-detail">
              乘客：{{ order.passengerName }} | 证件：{{ order.passengerIdCard }} | 席位：{{ order.seatType }}
            </div>
          </div>
          <div class="order-footer">
            <span class="order-amount">¥{{ order.amount }}</span>
            <div class="order-actions">
              <template v-if="order.status === 0">
                <el-button type="primary" size="small" round @click="payOrder(order.orderNo, 'traffic')">
                  去支付
                </el-button>
                <el-button size="small" round @click="cancelOrder(order.orderNo, 'traffic')">
                  取消
                </el-button>
              </template>
              <el-tag v-else-if="order.status === 3 || order.status === 4" type="info" round>
                {{ getTrafficStatusLabel(order.status) }}
              </el-tag>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 酒店订单 -->
      <el-tab-pane label="酒店订单" name="hotel">
        <div v-if="hotelLoading">
          <SkeletonBox type="list" :count="3" />
        </div>

        <EmptyState
          v-else-if="hotelOrders.length === 0"
          icon="document"
          title="暂无酒店订单"
          description="还没有酒店预订记录，去看看心仪的酒店吧"
          action-text="去搜索酒店"
          @action="$router.push('/hotel-search')"
        />

        <el-card
          v-else
          v-for="order in hotelOrders"
          :key="order.orderNo"
          class="order-card"
        >
          <div class="order-header">
            <el-tag type="warning" effect="light" round><el-icon style="margin-right:4px"><House /></el-icon>酒店</el-tag>
            <span class="order-no">{{ order.orderNo }}</span>
            <el-tag :type="getStatusType(order.status)" round size="small">
              {{ getHotelStatusLabel(order.status) }}
            </el-tag>
          </div>
          <div class="order-body">
            <div class="hotel-name-row">{{ order.hotelName }}</div>
            <div class="order-detail">
              房型：{{ order.roomType }} | 入住：{{ order.checkInDate }} | 退房：{{ order.checkOutDate }}
            </div>
            <div class="order-detail">
              联系人：{{ order.guestName }} | 电话：{{ order.guestPhone }}
            </div>
          </div>
          <div class="order-footer">
            <span class="order-amount">¥{{ order.amount }}</span>
            <div class="order-actions">
              <template v-if="order.status === 0">
                <el-button type="primary" size="small" round @click="payOrder(order.orderNo, 'hotel')">
                  去支付
                </el-button>
                <el-button size="small" round @click="cancelOrder(order.orderNo, 'hotel')">
                  取消
                </el-button>
              </template>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Promotion, Tickets, House } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";

const activeTab = ref("traffic");
const trafficOrders = ref([]);
const hotelOrders = ref([]);
const trafficLoading = ref(false);
const hotelLoading = ref(false);

const getTrafficStatusLabel = (status) => {
  const map = { 0: "待支付", 1: "出票中", 2: "已出票", 3: "已取消", 4: "已退票" };
  return map[status] ?? "未知";
};

const getHotelStatusLabel = (status) => {
  const map = { 0: "待支付", 1: "已支付", 2: "已入住", 3: "已完成", 4: "已取消" };
  return map[status] ?? "未知";
};

const getStatusType = (status) => {
  const map = { 0: "warning", 1: "primary", 2: "success", 3: "info", 4: "info" };
  return map[status] ?? "info";
};

const fetchTrafficOrders = async () => {
  trafficLoading.value = true;
  try {
    const data = await request.get("/api/order/list");
    trafficOrders.value = Array.isArray(data) ? data : [];
  } catch (e) {
    trafficOrders.value = [];
  } finally {
    trafficLoading.value = false;
  }
};

const fetchHotelOrders = async () => {
  hotelLoading.value = true;
  try {
    const data = await request.get("/api/hotel/orders");
    hotelOrders.value = Array.isArray(data) ? data : [];
  } catch (e) {
    hotelOrders.value = [];
  } finally {
    hotelLoading.value = false;
  }
};

const handleTabChange = (tab) => {
  if (tab === "hotel" && hotelOrders.value.length === 0) {
    fetchHotelOrders();
  }
};

const payOrder = async (orderNo, type) => {
  try {
    if (type === "traffic") {
      await request.post(`/api/order/${orderNo}/pay`);
    } else {
      await request.post(`/api/hotel/order/${orderNo}/pay`);
    }
    ElMessage({
      message: "支付成功！",
      type: "success",
      duration: 2000,
    });
    window.dispatchEvent(new Event("notification-updated"));
    type === "traffic" ? fetchTrafficOrders() : fetchHotelOrders();
  } catch (e) {}
};

const cancelOrder = async (orderNo, type) => {
  await ElMessageBox.confirm("确认取消该订单？", "提示", { type: "warning" });
  try {
    if (type === "traffic") {
      await request.post(`/api/order/${orderNo}/cancel`);
    } else {
      await request.post(`/api/hotel/order/${orderNo}/cancel`);
    }
    ElMessage.success("订单已取消");
    window.dispatchEvent(new Event("notification-updated"));
    type === "traffic" ? fetchTrafficOrders() : fetchHotelOrders();
  } catch (e) {}
};

onMounted(() => {
  fetchTrafficOrders();
});
</script>

<style scoped>
.my-orders-page {
  max-width: 960px;
  margin: 0 auto;
}

.order-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
}

.order-card {
  margin-bottom: 16px;
  border-radius: 16px;
  border: 1px solid #F0F2F5;
  transition: all 0.3s ease;
}
.order-card:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
}

.order-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.order-no {
  font-size: 13px;
  color: #A0A0B8;
  flex: 1;
  font-family: "SF Mono", "Menlo", monospace;
}

.order-body {
  padding: 14px 0;
  border-top: 1px solid #F0F2F5;
  border-bottom: 1px solid #F0F2F5;
  margin-bottom: 14px;
}

.order-route {
  font-size: 20px;
  font-weight: 700;
  color: #1A1A2E;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.route-city {
  display: inline-block;
}

.route-arrow {
  color: #A0A0B8;
  font-weight: 400;
}

.order-detail,
.hotel-name-row {
  font-size: 13px;
  color: #71718B;
  margin-bottom: 4px;
  line-height: 1.6;
}

.hotel-name-row {
  font-size: 17px;
  font-weight: 700;
  color: #1A1A2E;
  margin-bottom: 8px;
}

.order-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.order-amount {
  font-size: 24px;
  font-weight: 800;
  color: #EF4444;
}

.order-actions {
  display: flex;
  gap: 8px;
}
</style>
