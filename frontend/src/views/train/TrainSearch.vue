<template>
  <div class="train-search-page">
    <PageHeader
      title="火车票搜索"
      subtitle="搜索并预订全国高铁动车车次"
      :icon="Tickets"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '火车票搜索' },
      ]"
    />

    <el-card class="search-box">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="出发站">
          <el-input v-model="searchForm.depStation" placeholder="如：北京 / 北京西" clearable size="large" />
        </el-form-item>
        <el-form-item label="到达站">
          <el-input v-model="searchForm.arrStation" placeholder="如：南昌 / 南昌西" clearable size="large" />
        </el-form-item>
        <el-form-item label="出发日期">
          <el-date-picker
            v-model="searchForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="fetchTrains">
            <el-icon><Search /></el-icon>搜索车次
          </el-button>
          <el-button size="large" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
      <div class="live-sync-panel">
        <div class="live-sync-copy">
          <strong>12306 公开余票同步</strong>
          <span>{{ syncStatusText }}</span>
          <el-tag v-if="syncStatus" size="small" :type="syncSourceTagType">
            {{ syncSourceLabel }}
          </el-tag>
        </div>
      </div>
    </el-card>

    <div v-if="loading">
      <SkeletonBox type="list" :count="4" />
    </div>

    <EmptyState
      v-else-if="trains.length === 0"
      icon="search"
      title="暂无车次信息"
      description="没有找到匹配的车次，请调整搜索条件试试"
    />

    <template v-else>
      <el-card v-for="train in trains" :key="trainKey(train)" class="train-card">
        <div class="train-row">
          <div class="train-no-col">
            <div class="train-no">{{ train.trainNo }}</div>
            <el-tag size="small" :type="getTrainTypeColor(train.trainType)">
              {{ train.trainType }}
            </el-tag>
          </div>
          <div class="train-time-col">
            <div class="depart-time">{{ formatTime(train.departureTime) }}</div>
            <div class="train-route-info">{{ train.departureStation }} -> {{ train.arrivalStation }}</div>
            <div class="arrive-time">{{ formatTime(train.arrivalTime) }}</div>
          </div>
          <div class="train-duration-col">
            <div class="duration">{{ calcDuration(train.departureTime, train.arrivalTime) }}</div>
            <div class="duration-label">历时</div>
          </div>
          <div class="train-price-col">
            <div class="seat-status-list">
              <div class="seat-status">
                <span class="seat-name">二等座</span>
                <span class="seat-value" :class="seatStatusClass(train, 'secondClass')">
                  {{ seatStatusText(train, "secondClass") }}
                </span>
                <span v-if="train.secondClassPrice" class="seat-fare">¥{{ train.secondClassPrice }}</span>
              </div>
              <div class="seat-status">
                <span class="seat-name">一等座</span>
                <span class="seat-value" :class="seatStatusClass(train, 'firstClass')">
                  {{ seatStatusText(train, "firstClass") }}
                </span>
                <span v-if="train.firstClassPrice" class="seat-fare">¥{{ train.firstClassPrice }}</span>
              </div>
              <div v-if="hasExtraSeatInfo(train)" class="seat-extra">
                {{ extraSeatSummary(train) }}
              </div>
            </div>
            <div class="train-actions">
              <el-button
                v-if="hasBookableSeat(train)"
                type="primary"
                size="small"
                :disabled="isLiveDemoTrain(train)"
                @click="openBookDialog(train)"
              >
                预订
              </el-button>
              <el-button v-else type="warning" size="small" @click="openWaitlistDialog(train)">
                提交候补
              </el-button>
              <el-button link type="info" size="small" @click="openPriceTrend(train)">
                价格趋势
              </el-button>
            </div>
          </div>
        </div>
      </el-card>
      <div v-if="trains.length > 0" class="load-more-row">
        <el-button
          v-if="hasMoreTrains"
          :loading="loadingMore"
          type="primary"
          plain
          @click="loadMoreTrains"
        >
          读取更多
        </el-button>
        <span v-else class="load-more-finished">已读取当前可展示车次</span>
      </div>
    </template>

    <el-dialog v-model="bookDialogVisible" title="预订火车票" width="520px">
      <div v-if="selectedTrain" class="book-train-info">
        <p>
          <strong>{{ selectedTrain.trainNo }}</strong>
          {{ selectedTrain.departureStation }} -> {{ selectedTrain.arrivalStation }}
        </p>
        <p>{{ formatTime(selectedTrain.departureTime) }} ~ {{ formatTime(selectedTrain.arrivalTime) }}</p>
      </div>
      <el-divider />
      <el-form :model="bookForm" label-width="80px">
        <el-form-item label="乘客">
          <el-select v-model="bookForm.passengerId" placeholder="选择乘客" style="width: 100%">
            <el-option v-for="p in passengers" :key="p.id" :label="`${p.name} ${p.idCard}`" :value="p.id" />
          </el-select>
          <el-button link type="primary" @click="passengerDrawerVisible = true">+ 添加乘客</el-button>
        </el-form-item>
        <el-form-item label="席别">
          <el-radio-group v-model="bookForm.seatType">
            <el-radio value="secondClass" :disabled="!seatAvailable(selectedTrain, 'secondClass')">
              二等座 ¥{{ selectedTrain?.secondClassPrice || "暂无" }}
            </el-radio>
            <el-radio value="firstClass" :disabled="!seatAvailable(selectedTrain, 'firstClass')">
              一等座 ¥{{ selectedTrain?.firstClassPrice || "暂无" }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="bookForm.ticketCount" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="优惠券">
          <el-select v-model="bookForm.userCouponId" placeholder="不使用优惠券" clearable style="width: 100%">
            <el-option v-for="coupon in usableCoupons" :key="coupon.id" :label="couponLabel(coupon)" :value="coupon.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="total-price">
            <span v-if="currentPayablePrice < currentSeatPrice" class="origin-price">¥{{ currentSeatPrice }}</span>
            ¥{{ currentPayablePrice }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="booking" @click="confirmBook">确认下单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="waitlistDialogVisible" title="提交候补" width="480px">
      <div v-if="waitlistTrain" class="book-train-info">
        <p>
          <strong>{{ waitlistTrain.trainNo }}</strong>
          {{ waitlistTrain.departureStation }} -> {{ waitlistTrain.arrivalStation }}
        </p>
        <p>{{ formatTime(waitlistTrain.departureTime) }} ~ {{ formatTime(waitlistTrain.arrivalTime) }}</p>
      </div>
      <el-divider />
      <el-form :model="waitlistForm" label-width="80px">
        <el-form-item label="乘客">
          <el-select v-model="waitlistForm.passengerId" placeholder="选择乘客" style="width: 100%">
            <el-option v-for="p in passengers" :key="p.id" :label="`${p.name} ${p.idCard}`" :value="p.id" />
          </el-select>
          <el-button link type="primary" @click="passengerDrawerVisible = true">+ 添加乘客</el-button>
        </el-form-item>
        <el-form-item label="席别">
          <el-select v-model="waitlistForm.seatType" style="width: 100%">
            <el-option label="二等座" value="SecondClass" />
            <el-option label="一等座" value="FirstClass" />
          </el-select>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="waitlistForm.ticketCount" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="waitlistDialogVisible = false">取消</el-button>
        <el-button type="warning" :loading="waitlisting" @click="confirmWaitlist">提交候补</el-button>
      </template>
    </el-dialog>

    <PriceTrend
      v-model="priceTrendVisible"
      :ticket-id="priceTrendTicket?.id"
      :ticket-type="1"
      :ticket-name="priceTrendTicket?.trainNo"
    />

    <el-drawer v-model="passengerDrawerVisible" title="添加乘客" size="400px">
      <el-form :model="newPassenger" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="newPassenger.name" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="证件号">
          <el-input v-model="newPassenger.idCard" placeholder="身份证号/护照号" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="newPassenger.phone" placeholder="联系电话" />
        </el-form-item>
        <el-button type="primary" @click="addPassenger">添加</el-button>
      </el-form>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Search, Tickets } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";
import PriceTrend from "@/components/PriceTrend.vue";

const route = useRoute();
const router = useRouter();
const trains = ref([]);
const loading = ref(false);
const loadingMore = ref(false);
const hasMoreTrains = ref(false);
const syncStatus = ref(null);
const bookDialogVisible = ref(false);
const waitlistDialogVisible = ref(false);
const passengerDrawerVisible = ref(false);
const booking = ref(false);
const waitlisting = ref(false);
const selectedTrain = ref(null);
const waitlistTrain = ref(null);
const passengers = ref([]);
const myCoupons = ref([]);
const priceTrendVisible = ref(false);
const priceTrendTicket = ref(null);

const searchForm = ref({
  depStation: route.query.depStation || "",
  arrStation: route.query.arrStation || "",
  date: route.query.date || "",
});

const today = () => new Date().toISOString().slice(0, 10);
const PAGE_SIZE = 10;

const bookForm = ref({ passengerId: null, seatType: "secondClass", ticketCount: 1, userCouponId: null });
const waitlistForm = ref({ passengerId: null, seatType: "SecondClass", ticketCount: 1 });
const newPassenger = ref({ name: "", idCard: "", phone: "" });

const currentSeatPrice = computed(() => {
  if (!selectedTrain.value) return 0;
  const map = {
    secondClass: selectedTrain.value.secondClassPrice,
    firstClass: selectedTrain.value.firstClassPrice,
  };
  return Number(map[bookForm.value.seatType] || 0) * (bookForm.value.ticketCount || 1);
});

const usableCoupons = computed(() =>
  myCoupons.value.filter(
    (coupon) =>
      coupon.status === 0 &&
      isCouponUsableFor(coupon, "train") &&
      currentSeatPrice.value >= Number(coupon.minAmount || 0),
  ),
);

const currentPayablePrice = computed(() => {
  const coupon = usableCoupons.value.find((item) => item.id === bookForm.value.userCouponId);
  return calcCouponAmount(currentSeatPrice.value, coupon);
});

const syncStatusText = computed(() => {
  if (!syncStatus.value) return "输入出发站、到达站和日期后，搜索时会尝试同步 12306 公开余票。";
  const status = syncStatus.value;
  if (status.synced) {
    return `${status.route || "当前路线"} ${status.date || ""} 已同步 ${status.trainCount || 0} 趟车`;
  }
  return status.message || "实时同步未返回结果，当前展示本地数据。";
});

const syncSourceLabel = computed(() => {
  const source = syncStatus.value?.dataSource;
  if (source === "12306_PAGE") return "12306 实时读取";
  if (source === "LOCAL_DEMO_CACHE") return "本地演示缓存";
  return "本地数据库";
});

const syncSourceTagType = computed(() => {
  const source = syncStatus.value?.dataSource;
  if (source === "12306_PAGE") return "success";
  if (source === "LOCAL_DEMO_CACHE") return "warning";
  return "info";
});

const fetchTrains = async (options = {}) => {
  const append = options?.append === true;
  if (!searchForm.value.date) {
    searchForm.value.date = today();
    ElMessage.info("未选择出发日期，已默认使用今天");
  }
  if (append) {
    loadingMore.value = true;
  } else {
    loading.value = true;
    hasMoreTrains.value = false;
  }
  try {
    const data = await request.get("/api/train/search", {
      timeout: 45000,
      params: {
        ...searchForm.value,
        offset: append ? trains.value.length : 0,
        limit: PAGE_SIZE,
      },
    });
    const next = Array.isArray(data) ? data : [];
    trains.value = append ? [...trains.value, ...next] : next;
    hasMoreTrains.value = next.length === PAGE_SIZE;
    await fetchSyncStatus();
  } catch (e) {
    if (!append) {
      trains.value = [];
      hasMoreTrains.value = false;
    }
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

const loadMoreTrains = () => fetchTrains({ append: true });

const fetchSyncStatus = async () => {
  try {
    syncStatus.value = await request.get("/api/train/live-sync-status", { skipErrorMessage: true });
  } catch (e) {
    syncStatus.value = null;
  }
};

const resetForm = () => {
  searchForm.value = { depStation: "", arrStation: "", date: "" };
  hasMoreTrains.value = false;
  fetchTrains();
};

const formatTime = (iso) => {
  if (!iso) return "";
  return new Date(iso).toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const calcDuration = (dep, arr) => {
  if (!dep || !arr) return "";
  const diff = Math.max(0, (new Date(arr) - new Date(dep)) / 60000);
  const h = Math.floor(diff / 60);
  const m = Math.floor(diff % 60);
  return `${h}小时${m}分`;
};

const trainKey = (train) =>
  `${train.id || ""}-${train.trainNo}-${train.departureStation}-${train.arrivalStation}-${train.departureTime}`;

const getTrainTypeColor = (type) => {
  const map = { G: "danger", D: "warning", C: "primary", Z: "success", T: "info", K: "info" };
  return map[type?.[0]] || "info";
};

const normalizeSeatText = (value, seats) => {
  if (value !== null && value !== undefined && String(value).trim() !== "") return String(value).trim();
  if (seats === null || seats === undefined) return "--";
  if (Number(seats) > 0) return String(seats);
  return "候补";
};

const seatInfo = (train, seatType) => {
  if (!train) return { text: "--", seats: 0 };
  if (seatType === "firstClass") {
    return { text: normalizeSeatText(train.firstClassSeatText, train.firstClassSeats), seats: Number(train.firstClassSeats || 0) };
  }
  return { text: normalizeSeatText(train.secondClassSeatText, train.secondClassSeats), seats: Number(train.secondClassSeats || 0) };
};

const seatAvailable = (train, seatType) => {
  const info = seatInfo(train, seatType);
  return info.seats > 0 || info.text === "有";
};

const hasBookableSeat = (train) => seatAvailable(train, "secondClass") || seatAvailable(train, "firstClass");

const seatStatusText = (train, seatType) => {
  const info = seatInfo(train, seatType);
  if (info.text === "有") return "有票";
  if (info.text === "--" || info.text === "*") return "--";
  if (info.text === "候补" || info.text === "无") return "候补";
  return info.text;
};

const seatStatusClass = (train, seatType) => {
  if (seatAvailable(train, seatType)) return "seat-ok";
  const text = seatStatusText(train, seatType);
  if (text === "候补") return "seat-wait";
  return "seat-empty";
};

const hasExtraSeatInfo = (train) =>
  Boolean(train?.businessSeatText || train?.hardSeatText || train?.noSeatText);

const extraSeatSummary = (train) => {
  const parts = [];
  if (train.businessSeatText) parts.push(`商务座 ${train.businessSeatText}`);
  if (train.hardSeatText) parts.push(`硬座 ${train.hardSeatText}`);
  if (train.noSeatText) parts.push(`无座 ${train.noSeatText}`);
  return parts.join(" / ");
};

const openPriceTrend = (train) => {
  priceTrendTicket.value = train;
  priceTrendVisible.value = true;
};

const isLiveDemoTrain = (train) => train?.liveOnly === true;

const defaultSeatType = (train) => (seatAvailable(train, "secondClass") ? "secondClass" : "firstClass");

const openBookDialog = async (train) => {
  if (isLiveDemoTrain(train)) return;
  selectedTrain.value = train;
  bookForm.value = { passengerId: null, seatType: defaultSeatType(train), ticketCount: 1, userCouponId: null };
  bookDialogVisible.value = true;
  await Promise.all([fetchPassengers(), fetchMyCoupons()]);
};

const openWaitlistDialog = async (train) => {
  waitlistTrain.value = train;
  waitlistForm.value = { passengerId: null, seatType: "SecondClass", ticketCount: 1 };
  waitlistDialogVisible.value = true;
  await fetchPassengers();
};

const fetchPassengers = async () => {
  try {
    const data = await request.get("/api/passenger/list");
    passengers.value = Array.isArray(data) ? data : [];
  } catch (e) {
    passengers.value = [];
  }
};

const fetchMyCoupons = async () => {
  try {
    const data = await request.get("/api/coupon/my");
    myCoupons.value = Array.isArray(data) ? data : [];
  } catch (e) {
    myCoupons.value = [];
  }
};

const addPassenger = async () => {
  try {
    await request.post("/api/passenger/add", newPassenger.value);
    ElMessage.success("添加乘客成功");
    newPassenger.value = { name: "", idCard: "", phone: "" };
    passengerDrawerVisible.value = false;
    await fetchPassengers();
  } catch (e) {}
};

const confirmBook = async () => {
  if (!bookForm.value.passengerId) {
    ElMessage.warning("请选择乘客");
    return;
  }
  if (!seatAvailable(selectedTrain.value, bookForm.value.seatType)) {
    ElMessage.warning("所选席别暂无可预订余票");
    return;
  }
  booking.value = true;
  try {
    await request.post("/api/order/train/create", {
      trainId: selectedTrain.value.id,
      passengerId: bookForm.value.passengerId,
      seatType: bookForm.value.seatType === "firstClass" ? "FirstClass" : "SecondClass",
      ticketCount: bookForm.value.ticketCount,
      userCouponId: bookForm.value.userCouponId,
    });
    ElMessage.success("下单成功，请前往我的订单完成支付");
    bookDialogVisible.value = false;
    router.push({ path: "/my-orders", query: { tab: "traffic" } });
  } catch (e) {
  } finally {
    booking.value = false;
  }
};

const confirmWaitlist = async () => {
  if (!waitlistForm.value.passengerId) {
    ElMessage.warning("请选择乘客");
    return;
  }
  waitlisting.value = true;
  try {
    await request.post("/api/train/waitlist", {
      trainId: waitlistTrain.value.id,
      trainNo: waitlistTrain.value.trainNo,
      departureStation: waitlistTrain.value.departureStation,
      arrivalStation: waitlistTrain.value.arrivalStation,
      departureTime: waitlistTrain.value.departureTime,
      seatType: waitlistForm.value.seatType,
      ticketCount: waitlistForm.value.ticketCount,
      passengerId: waitlistForm.value.passengerId,
    });
    ElMessage.success("候补申请已提交");
    waitlistDialogVisible.value = false;
  } catch (e) {
  } finally {
    waitlisting.value = false;
  }
};

const calcCouponAmount = (amount, coupon) => {
  if (!coupon) return Number(amount.toFixed(2));
  const discountValue = Number(coupon.discountValue || 0);
  const discounted = coupon.discountType === 1 ? amount * discountValue : amount - discountValue;
  return Number(Math.max(discounted, 0).toFixed(2));
};

const couponLabel = (coupon) => {
  const discount = coupon.discountType === 1
    ? `${Number(coupon.discountValue) * 10}折`
    : `减¥${coupon.discountValue}`;
  return `${coupon.couponName || coupon.name} ${couponCategoryLabel(coupon.category)} ${discount} 满¥${coupon.minAmount || 0}可用`;
};

const normalizeCouponCategory = (category) => {
  const value = String(category || "all").toLowerCase();
  return ["all", "flight", "train", "hotel"].includes(value) ? value : "all";
};

const isCouponUsableFor = (coupon, category) => {
  const couponCategory = normalizeCouponCategory(coupon?.category);
  return couponCategory === "all" || couponCategory === category;
};

const couponCategoryLabel = (category) =>
  ({
    all: "通用",
    flight: "机票",
    train: "火车票",
    hotel: "酒店",
  }[normalizeCouponCategory(category)]);

watch(currentSeatPrice, () => {
  if (
    bookForm.value.userCouponId &&
    !usableCoupons.value.some((coupon) => coupon.id === bookForm.value.userCouponId)
  ) {
    bookForm.value.userCouponId = null;
  }
});

onMounted(() => {
  fetchTrains();
});
</script>

<style scoped>
.train-search-page {
  max-width: 1040px;
  margin: 0 auto;
}
.search-box {
  margin-bottom: 22px;
  border-radius: 8px;
  border: 1px solid oklch(0.91 0.028 185);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(238, 253, 249, 0.92));
  box-shadow: 0 14px 36px rgba(36, 96, 92, 0.08);
}
.live-sync-panel {
  margin-top: 8px;
  padding: 12px 14px;
  border: 1px solid oklch(0.91 0.028 185);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}
.live-sync-copy {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  color: var(--tm-ink-soft);
}
.live-sync-copy strong {
  color: oklch(0.43 0.09 182);
}
.live-sync-copy span {
  font-size: 13px;
  color: var(--tm-muted);
}
.train-card {
  margin-bottom: 12px;
  border-radius: 8px;
  border: 1px solid oklch(0.93 0.018 190);
  background: linear-gradient(135deg, #ffffff 0%, #fbfffe 58%, #f2fbf8 100%);
  box-shadow: 0 10px 28px rgba(43, 96, 92, 0.06);
}
.train-row {
  display: grid;
  grid-template-columns: 112px minmax(260px, 1fr) 96px 220px;
  align-items: center;
  gap: 22px;
}
.train-no {
  font-size: 20px;
  font-weight: 800;
  color: oklch(0.43 0.09 182);
  margin-bottom: 4px;
  font-family: "SF Mono", "Menlo", monospace;
}
.train-time-col,
.train-duration-col {
  text-align: center;
}
.depart-time,
.arrive-time {
  font-size: 20px;
  font-weight: 700;
  color: var(--tm-ink);
}
.train-route-info {
  font-size: 13px;
  color: oklch(0.43 0.09 182);
  margin: 6px 0;
}
.duration {
  font-size: 16px;
  font-weight: 600;
  color: var(--tm-ink-soft);
}
.duration-label,
.seat-extra {
  font-size: 12px;
  color: var(--tm-muted);
}
.train-price-col {
  text-align: right;
}
.seat-status-list {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}
.seat-status {
  display: grid;
  grid-template-columns: 54px 54px 1fr;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}
.seat-name {
  color: var(--tm-ink-soft);
}
.seat-value {
  font-weight: 800;
}
.seat-ok {
  color: #16a34a;
}
.seat-wait {
  color: #d97706;
}
.seat-empty {
  color: var(--tm-muted);
}
.seat-fare {
  font-weight: 800;
  color: #ef4444;
}
.train-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 4px;
}
.load-more-row {
  display: flex;
  justify-content: center;
  padding: 8px 0 24px;
}
.load-more-finished {
  font-size: 13px;
  color: var(--tm-muted);
}
.book-train-info {
  padding: 14px;
  background: #f0fdfa;
  border: 1px solid oklch(0.9 0.04 185);
  border-radius: 8px;
}
.total-price {
  font-size: 24px;
  font-weight: 800;
  color: #ef4444;
}
.origin-price {
  margin-right: 8px;
  font-size: 14px;
  color: var(--tm-muted);
  text-decoration: line-through;
}
@media (max-width: 768px) {
  .train-row {
    grid-template-columns: 1fr;
    gap: 12px;
    align-items: flex-start;
  }
  .train-time-col,
  .train-duration-col,
  .train-price-col {
    width: 100%;
    text-align: left;
  }
  .train-actions {
    justify-content: flex-start;
  }
}
</style>
