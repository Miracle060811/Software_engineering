<template>
  <div class="my-orders-page">
    <h2 class="page-title">我的订单</h2>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 大交通订单 -->
      <el-tab-pane label="大交通订单" name="traffic">
        <div v-loading="trafficLoading">
          <el-empty
            v-if="!trafficLoading && trafficOrders.length === 0"
            description="暂无大交通订单"
          />
          <el-card
            v-for="order in trafficOrders"
            :key="order.orderNo"
            class="order-card"
          >
            <div class="order-header">
              <el-tag :type="order.orderType === 0 ? 'primary' : 'success'">
                {{ order.orderType === 0 ? "机票" : "火车票" }}
              </el-tag>
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <el-tag :type="getStatusType(order.status)" class="order-status">
                {{ getTrafficStatusLabel(order.status) }}
              </el-tag>
            </div>
            <div class="order-body">
              <div class="order-route">
                <span class="route-from">{{
                  order.departureCity || order.departureStation
                }}</span>
                <span class="route-arrow">→</span>
                <span class="route-to">{{
                  order.arrivalCity || order.arrivalStation
                }}</span>
              </div>
              <div class="order-passenger">
                乘客：{{ order.passengerName }} | 证件：{{
                  order.passengerIdCard
                }}
                | 席位：{{ order.seatType }}
              </div>
            </div>
            <div class="order-footer">
              <span class="order-amount">¥{{ order.amount }}</span>
              <div class="order-actions">
                <template v-if="order.status === 0">
                  <el-button
                    type="primary"
                    size="small"
                    @click="payOrder(order.orderNo, 'traffic')"
                  >
                    去支付
                  </el-button>
                  <el-button
                    size="small"
                    @click="cancelOrder(order.orderNo, 'traffic')"
                  >
                    取消
                  </el-button>
                </template>
                <el-tag
                  v-else-if="order.status === 3 || order.status === 4"
                  type="info"
                >
                  {{ getTrafficStatusLabel(order.status) }}
                </el-tag>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 酒店订单 -->
      <el-tab-pane label="酒店订单" name="hotel">
        <div v-loading="hotelLoading">
          <el-empty
            v-if="!hotelLoading && hotelOrders.length === 0"
            description="暂无酒店订单"
          />
          <el-card
            v-for="order in hotelOrders"
            :key="order.orderNo"
            class="order-card"
          >
            <div class="order-header">
              <el-tag type="warning">酒店</el-tag>
              <span class="order-no">订单号：{{ order.orderNo }}</span>
              <el-tag :type="getStatusType(order.status)" class="order-status">
                {{ getHotelStatusLabel(order.status) }}
              </el-tag>
            </div>
            <div class="order-body">
              <div class="hotel-name-row">{{ order.hotelName }}</div>
              <div class="room-info-row">
                房型：{{ order.roomType }} | 入住：{{ order.checkInDate }} |
                退房：{{ order.checkOutDate }}
              </div>
              <div class="contact-row">
                联系人：{{ order.contactName }} | 电话：{{ order.contactPhone }}
              </div>
            </div>
            <div class="order-footer">
              <span class="order-amount">¥{{ order.amount }}</span>
              <div class="order-actions">
                <template v-if="order.status === 0">
                  <el-button
                    type="primary"
                    size="small"
                    @click="payOrder(order.orderNo, 'hotel')"
                  >
                    去支付
                  </el-button>
                  <el-button
                    size="small"
                    @click="cancelOrder(order.orderNo, 'hotel')"
                  >
                    取消
                  </el-button>
                </template>
              </div>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import request from "@/utils/request";

const activeTab = ref("traffic");
const trafficOrders = ref([]);
const hotelOrders = ref([]);
const trafficLoading = ref(false);
const hotelLoading = ref(false);

const getTrafficStatusLabel = (status) => {
  const map = {
    0: "待支付",
    1: "出票中",
    2: "已出票",
    3: "已取消",
    4: "已退票",
  };
  return map[status] ?? "未知";
};

const getHotelStatusLabel = (status) => {
  const map = {
    0: "待支付",
    1: "已确认",
    2: "已入住",
    3: "已取消",
    4: "已退款",
  };
  return map[status] ?? "未知";
};

const getStatusType = (status) => {
  const map = {
    0: "warning",
    1: "primary",
    2: "success",
    3: "info",
    4: "info",
  };
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
    ElMessage.success("支付成功！");
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
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 20px;
}
.order-card {
  margin-bottom: 16px;
  border-radius: 10px;
}
.order-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.order-no {
  font-size: 13px;
  color: #999;
  flex: 1;
}
.order-body {
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 12px;
}
.order-route {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}
.route-arrow {
  margin: 0 8px;
  color: #999;
}
.order-passenger,
.room-info-row,
.contact-row,
.hotel-name-row {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}
.hotel-name-row {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.order-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.order-amount {
  font-size: 22px;
  font-weight: 700;
  color: #EF4444;
}
.order-actions {
  display: flex;
  gap: 8px;
}
</style>
