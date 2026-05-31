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
              乘客：{{ order.passengerName }} | 证件：{{ order.passengerIdCard }} | 席位：{{ order.seatType }} | 数量：{{ order.ticketCount || 1 }}
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
              <el-button v-if="order.status === 1 || order.status === 2" size="small" round @click="requestRefund(order.orderNo, 'traffic')">
                申请退票
              </el-button>
              <el-tag v-if="order.status === 3 || order.status === 4 || order.status === 5" type="info" round>
                {{ getTrafficStatusLabel(order.status) }}
              </el-tag>
              <el-button v-if="order.status === 1 || order.status === 2" size="small" round @click="downloadReceipt(order, 'traffic')">
                下载行程单
              </el-button>
              <el-button size="small" round @click="openOrderDetail(order.orderNo, 'traffic')">
                详情
              </el-button>
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
              房型：{{ order.roomType }} | 房间数：{{ order.roomCount || 1 }} | 入住：{{ order.checkInDate }} | 退房：{{ order.checkOutDate }}
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
              <el-button v-if="order.status === 1 || order.status === 2" size="small" round @click="showQrCode(order)">
                出示校验码
              </el-button>
              <el-button v-if="order.status === 1" size="small" round @click="requestRefund(order.orderNo, 'hotel')">
                申请退款
              </el-button>
              <el-button size="small" round @click="openOrderDetail(order.orderNo, 'hotel')">
                详情
              </el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="景点门票" name="attraction">
        <div v-if="attractionLoading">
          <SkeletonBox type="list" :count="3" />
        </div>

        <EmptyState
          v-else-if="attractionOrders.length === 0"
          icon="location"
          title="暂无景点门票订单"
          description="还没有购买景点门票，去看看热门景点吧"
          action-text="去买门票"
          @action="$router.push('/attractions')"
        />

        <el-card
          v-else
          v-for="order in attractionOrders"
          :key="order.orderNo"
          class="order-card"
        >
          <div class="order-header">
            <el-tag type="success" effect="light" round>景点门票</el-tag>
            <span class="order-no">{{ order.orderNo }}</span>
            <el-tag :type="getStatusType(order.status)" round size="small">
              {{ getAttractionStatusLabel(order.status) }}
            </el-tag>
          </div>
          <div class="order-body">
            <div class="hotel-name-row">{{ order.attractionName }}</div>
            <div class="order-detail">
              城市：{{ order.city || "未知" }} | 成人票：{{ order.adultCount || 0 }} | 儿童票：{{ order.childCount || 0 }} | 总数：{{ order.ticketCount || 1 }}
            </div>
            <div class="order-detail">
              联系人：{{ order.guestName }} | 电话：{{ order.guestPhone }}
            </div>
          </div>
          <div class="order-footer">
            <span class="order-amount">¥{{ order.amount }}</span>
            <div class="order-actions">
              <el-button v-if="order.status === 1 || order.status === 2" size="small" round @click="showQrCode(order)">
                出示核销码
              </el-button>
              <el-button size="small" round @click="openOrderDetail(order.orderNo, 'attraction')">
                详情
              </el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 校验码弹窗 -->
    <el-dialog v-model="qrVisible" title="扫码核销" width="360px" center>
      <div class="qr-code-box">
        <img
          v-if="qrOrderNo"
          :src="mockCheckinQr"
          alt="核销二维码"
          class="qr-img"
        />
        <p class="qr-label">向工作人员出示此二维码核销</p>
        <el-tag type="success" size="large" effect="dark" style="font-size:18px;padding:8px 20px;letter-spacing:4px">
          {{ qrOrderNo ? qrOrderNo.slice(-8) : "" }}
        </el-tag>
      </div>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="订单详情" width="520px">
      <el-descriptions v-if="detailOrder" :column="1" border>
        <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单类型">
          {{ detailType === "traffic" ? (detailOrder.orderType === 0 ? "机票" : "火车票") : detailType === "hotel" ? "酒店" : "景点门票" }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detailType === "traffic" ? getTrafficStatusLabel(detailOrder.status) : detailType === "hotel" ? getHotelStatusLabel(detailOrder.status) : getAttractionStatusLabel(detailOrder.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ detailOrder.amount }}</el-descriptions-item>
        <el-descriptions-item v-if="detailType === 'traffic'" label="路线">
          {{ detailOrder.departureCity || detailOrder.departureStation }} → {{ detailOrder.arrivalCity || detailOrder.arrivalStation }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailType === 'traffic'" label="乘客">
          {{ detailOrder.passengerName }}，{{ detailOrder.seatType }} x {{ detailOrder.ticketCount || 1 }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailType === 'hotel'" label="酒店">
          {{ detailOrder.hotelName }}，{{ detailOrder.roomType }} x {{ detailOrder.roomCount || 1 }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailType === 'hotel'" label="入住">
          {{ detailOrder.checkInDate }} 至 {{ detailOrder.checkOutDate }}，{{ detailOrder.nights }} 晚
        </el-descriptions-item>
        <el-descriptions-item v-if="detailType === 'attraction'" label="景点">
          {{ detailOrder.attractionName }}，{{ detailOrder.city || "未知城市" }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailType === 'attraction'" label="门票">
          成人票 {{ detailOrder.adultCount || 0 }}，儿童票 {{ detailOrder.childCount || 0 }}，共 {{ detailOrder.ticketCount || 1 }} 张
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailOrder.createTime }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ detailOrder.payTime || "未支付" }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { Promotion, Tickets, House } from "@element-plus/icons-vue";
import request from "@/utils/request";
import mockCheckinQr from "@/assets/mock-checkin-qr.png";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";

const route = useRoute();
const initialTab = ["traffic", "hotel", "attraction"].includes(route.query.tab)
  ? route.query.tab
  : "traffic";
const activeTab = ref(initialTab);
const trafficOrders = ref([]);
const hotelOrders = ref([]);
const attractionOrders = ref([]);
const trafficLoading = ref(false);
const hotelLoading = ref(false);
const attractionLoading = ref(false);
const qrVisible = ref(false);
const qrOrderNo = ref("");
const detailVisible = ref(false);
const detailOrder = ref(null);
const detailType = ref("traffic");

const getTrafficStatusLabel = (status) => {
  const map = { 0: "待支付", 1: "出票中", 2: "已出票", 3: "已取消", 4: "已退票", 5: "退票申请中" };
  return map[status] ?? "未知";
};

const getHotelStatusLabel = (status) => {
  const map = { 0: "待支付", 1: "已支付", 2: "已入住", 3: "已完成", 4: "已取消/已退款", 5: "退款申请中" };
  return map[status] ?? "未知";
};

const getAttractionStatusLabel = (status) => {
  const map = { 1: "待核销", 2: "已核销", 4: "已取消/已退款" };
  return map[status] ?? "未知";
};

const getStatusType = (status) => {
  const map = { 0: "warning", 1: "primary", 2: "success", 3: "info", 4: "info", 5: "danger" };
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

const fetchAttractionOrders = async () => {
  attractionLoading.value = true;
  try {
    const data = await request.get("/api/attraction/orders");
    attractionOrders.value = Array.isArray(data) ? data : [];
  } catch (e) {
    attractionOrders.value = [];
  } finally {
    attractionLoading.value = false;
  }
};

const handleTabChange = (tab) => {
  if (tab === "hotel" && hotelOrders.value.length === 0) {
    fetchHotelOrders();
  }
  if (tab === "attraction" && attractionOrders.value.length === 0) {
    fetchAttractionOrders();
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

const downloadReceipt = async (order, type) => {
  const url = type === "traffic"
    ? `/api/order/${order.orderNo}/receipt`
    : `/api/hotel/order/${order.orderNo}/receipt`;
  try {
    const data = await request.get(url);
    const content = [
      `========================================`,
      `          TravelMate 行程单`,
      `========================================`,
      `订单编号：${data.orderNo || order.orderNo}`,
      `订单类型：${type === "traffic" ? (order.orderType === 0 ? "机票" : "火车票") : "酒店"}`,
      type === "traffic"
        ? `路线：${data.departureCity || data.departureStation || order.departureCity || order.departureStation} → ${data.arrivalCity || data.arrivalStation || order.arrivalCity || order.arrivalStation}`
        : `酒店：${data.hotelName || order.hotelName}`,
      type === "traffic"
        ? `票数：${data.ticketCount || order.ticketCount || 1}`
        : `房型：${data.roomType || order.roomType}，房间数：${data.roomCount || order.roomCount || 1}`,
      `乘客/住客：${data.passengerName || data.guestName || order.passengerName || order.guestName}`,
      `金额：¥${data.amount || order.amount}`,
      `========================================`,
      `        TravelMate 伴游平台`,
      `========================================`,
    ].join("\n");
    const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
    const el = document.createElement("a");
    el.href = URL.createObjectURL(blob);
    el.download = `TravelMate_${order.orderNo}.txt`;
    el.click();
    URL.revokeObjectURL(el.href);
    ElMessage.success("行程单已下载");
  } catch (e) {
    // fallback: download from local order data
    const content = [
      `========================================`,
      `          TravelMate 行程单`,
      `========================================`,
      `订单编号：${order.orderNo}`,
      `金额：¥${order.amount}`,
      `========================================`,
      `        TravelMate 伴游平台`,
      `========================================`,
    ].join("\n");
    const blob = new Blob([content], { type: "text/plain;charset=utf-8" });
    const el = document.createElement("a");
    el.href = URL.createObjectURL(blob);
    el.download = `TravelMate_${order.orderNo}.txt`;
    el.click();
    URL.revokeObjectURL(el.href);
    ElMessage.success("行程单已下载");
  }
};

const showQrCode = (order) => {
  qrOrderNo.value = order.orderNo;
  qrVisible.value = true;
};

const openOrderDetail = async (orderNo, type) => {
  detailType.value = type;
  try {
    detailOrder.value = await request.get(
      type === "traffic"
        ? `/api/order/${orderNo}/receipt`
        : type === "hotel"
          ? `/api/hotel/order/${orderNo}/receipt`
          : `/api/attraction/order/${orderNo}/receipt`,
    );
    detailVisible.value = true;
  } catch (e) {}
};

const requestRefund = async (orderNo, type) => {
  await ElMessageBox.confirm(
    type === "traffic" ? "确认提交退票申请？管理员审核通过后会归还库存。" : "确认提交退款申请？管理员审核通过后会归还房间库存。",
    "退款申请",
    {
      type: "warning",
      confirmButtonText: "提交申请",
      cancelButtonText: "暂不申请",
    },
  );
  try {
    if (type === "traffic") {
      await request.post(`/api/order/${orderNo}/refund`);
    } else {
      await request.post(`/api/hotel/order/${orderNo}/refund`);
    }
    ElMessage.success("申请已提交，请等待管理员处理");
    window.dispatchEvent(new Event("notification-updated"));
    type === "traffic" ? fetchTrafficOrders() : fetchHotelOrders();
  } catch (e) {}
};

const cancelOrder = async (orderNo, type) => {
  await ElMessageBox.confirm("确认取消该订单？", "提示", {
    type: "warning",
    confirmButtonText: "确认取消",
    cancelButtonText: "暂不取消",
  });
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
  if (activeTab.value === "hotel") {
    fetchHotelOrders();
  } else if (activeTab.value === "attraction") {
    fetchAttractionOrders();
  } else {
    fetchTrafficOrders();
  }
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
.qr-code-box {
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.qr-img {
  width: 200px;
  height: 200px;
  margin-bottom: 12px;
  border-radius: 12px;
  display: block;
  object-fit: contain;
}
.qr-label {
  font-size: 13px;
  color: #A0A0B8;
  margin-bottom: 16px;
}
</style>
