<template>
  <div class="flight-search-page">
    <PageHeader
      title="机票搜索"
      subtitle="搜索并预订国内航班机票"
      :icon="Promotion"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '机票搜索' }
      ]"
    />

    <el-card class="search-box">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="出发城市">
          <el-input
            v-model="searchForm.depCity"
            placeholder="如：北京"
            clearable
            size="large"
          />
        </el-form-item>
        <el-form-item label="到达城市">
          <el-input
            v-model="searchForm.arrCity"
            placeholder="如：上海"
            clearable
            size="large"
          />
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
          <el-button type="primary" size="large" @click="fetchFlights">
            <el-icon><Search /></el-icon>搜索航班
          </el-button>
          <el-button size="large" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 骨架屏 -->
    <div v-if="loading">
      <SkeletonBox type="list" :count="4" />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="flights.length === 0"
      icon="search"
      title="暂无航班信息"
      description="没有找到匹配的航班，请调整搜索条件试试"
    />

    <template v-else>
      <el-card v-for="flight in flights" :key="flight.id" class="flight-card">
        <div class="flight-row">
          <div class="flight-airline">
            <div class="airline-name">{{ flight.airline }}</div>
            <div class="flight-no">{{ flight.flightNo }}</div>
          </div>
          <div class="flight-time">
            <div class="time-depart">
              {{ formatTime(flight.departureTime) }}
            </div>
            <div class="flight-route">
              {{ flight.departureCity }} → {{ flight.arrivalCity }}
            </div>
            <div class="time-arrive">{{ formatTime(flight.arrivalTime) }}</div>
          </div>
          <div class="flight-seats">
            <el-tag :type="flight.availableSeats > 10 ? 'success' : 'warning'">
              余票 {{ flight.availableSeats }}
            </el-tag>
          </div>
          <div class="flight-price-col">
            <div class="price-economy">
              <span class="price-num">¥{{ flight.economyPrice }}</span>
              <span class="price-label">经济舱</span>
            </div>
            <div class="price-business" v-if="flight.businessPrice">
              <span class="price-num-sm">¥{{ flight.businessPrice }}</span>
              <span class="price-label">商务舱</span>
            </div>
            <el-button
              type="primary"
              size="small"
              @click="openBookDialog(flight)"
            >
              预订
            </el-button>
            <el-button
              link
              type="info"
              size="small"
              @click="openPriceTrend(flight)"
              style="margin-left:4px"
            >
              价格趋势
            </el-button>
          </div>
        </div>
      </el-card>
    </template>

    <!-- 预订 Dialog -->
    <el-dialog v-model="bookDialogVisible" title="预订机票" width="520px">
      <div v-if="selectedFlight" class="book-flight-info">
        <p>
          <strong
            >{{ selectedFlight.airline }} {{ selectedFlight.flightNo }}</strong
          >
          {{ selectedFlight.departureCity }} → {{ selectedFlight.arrivalCity }}
        </p>
        <p>
          {{ formatTime(selectedFlight.departureTime) }} ~
          {{ formatTime(selectedFlight.arrivalTime) }}
        </p>
      </div>
      <el-divider />
      <el-form :model="bookForm" label-width="80px">
        <el-form-item label="乘客">
          <el-select
            v-model="bookForm.passengerId"
            placeholder="选择乘客"
            style="width: 100%"
            @click="fetchPassengers"
          >
            <el-option
              v-for="p in passengers"
              :key="p.id"
              :label="`${p.name}（${p.idCard}）`"
              :value="p.id"
            />
          </el-select>
          <el-button link type="primary" @click="passengerDrawerVisible = true">
            + 添加乘客
          </el-button>
        </el-form-item>
        <el-form-item label="舱位">
          <el-radio-group v-model="bookForm.seatType">
            <el-radio value="economy"
              >经济舱 ¥{{ selectedFlight?.economyPrice }}</el-radio
            >
            <el-radio
              value="business"
              :disabled="!selectedFlight?.businessPrice"
            >
              商务舱 ¥{{ selectedFlight?.businessPrice || "暂无" }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="bookForm.ticketCount" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="优惠券">
          <el-select
            v-model="bookForm.userCouponId"
            placeholder="不使用优惠券"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="coupon in usableCoupons"
              :key="coupon.id"
              :label="couponLabel(coupon)"
              :value="coupon.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="total-price">
            <span v-if="currentPayablePrice < currentOriginalPrice" class="origin-price">¥{{ currentOriginalPrice }}</span>
            ¥{{ currentPayablePrice }}
          </span>
        </el-form-item>
      </el-form>
      <el-collapse style="margin-top:12px">
        <el-collapse-item title="退改签规则">
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="经济舱">出发前24小时退票手续费5%，2小时内20%，出票后不可退</el-descriptions-item>
            <el-descriptions-item label="商务舱">出发前24小时退票手续费3%，2小时内15%，出票后不可退</el-descriptions-item>
            <el-descriptions-item label="改签">出发前2小时以上免费改签一次，之后不可改签</el-descriptions-item>
          </el-descriptions>
        </el-collapse-item>
      </el-collapse>
      <template #footer>
        <el-button @click="bookDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="booking" @click="confirmBook"
          >确认下单</el-button
        >
      </template>
    </el-dialog>

    <!-- 价格趋势 Dialog -->
    <PriceTrend
      v-model="priceTrendVisible"
      :ticket-id="priceTrendTicket?.id"
      :ticket-type="0"
      :ticket-name="priceTrendTicket?.flightNo"
    />

    <!-- 添加乘客 Drawer -->
    <el-drawer v-model="passengerDrawerVisible" title="添加乘客" size="400px">
      <el-form :model="newPassenger" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="newPassenger.name" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="newPassenger.idCard" placeholder="18位身份证号" />
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
import { Search, Promotion } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";
import PriceTrend from "@/components/PriceTrend.vue";

const route = useRoute();
const router = useRouter();
const flights = ref([]);
const loading = ref(false);
const bookDialogVisible = ref(false);
const passengerDrawerVisible = ref(false);
const booking = ref(false);
const selectedFlight = ref(null);
const passengers = ref([]);
const myCoupons = ref([]);
const priceTrendVisible = ref(false);
const priceTrendTicket = ref(null);

const searchForm = ref({
  depCity: route.query.depCity || "",
  arrCity: route.query.arrCity || "",
  date: "",
});

const bookForm = ref({ passengerId: null, seatType: "economy", ticketCount: 1, userCouponId: null });
const newPassenger = ref({ name: "", idCard: "", phone: "" });

const currentOriginalPrice = computed(() => {
  if (!selectedFlight.value) return 0;
  const unitPrice = Number(
    bookForm.value.seatType === "economy"
      ? selectedFlight.value.economyPrice
      : selectedFlight.value.businessPrice,
  ) || 0;
  return unitPrice * (bookForm.value.ticketCount || 1);
});

const usableCoupons = computed(() =>
  myCoupons.value.filter(
    (coupon) =>
      coupon.status === 0 &&
      isCouponUsableFor(coupon, "flight") &&
      currentOriginalPrice.value >= Number(coupon.minAmount || 0),
  ),
);

const currentPayablePrice = computed(() => {
  const coupon = usableCoupons.value.find((item) => item.id === bookForm.value.userCouponId);
  return calcCouponAmount(currentOriginalPrice.value, coupon);
});

const fetchFlights = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/flight/search", {
      params: searchForm.value,
    });
    flights.value = Array.isArray(data) ? data : [];
  } catch (e) {
    flights.value = [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  searchForm.value = { depCity: "", arrCity: "", date: "" };
  fetchFlights();
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

const openPriceTrend = (flight) => {
  priceTrendTicket.value = flight;
  priceTrendVisible.value = true;
};

const openBookDialog = async (flight) => {
  selectedFlight.value = flight;
  bookForm.value = { passengerId: null, seatType: "economy", ticketCount: 1, userCouponId: null };
  bookDialogVisible.value = true;
  await Promise.all([fetchPassengers(), fetchMyCoupons()]);
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
  booking.value = true;
  try {
    const passenger = passengers.value.find(
      (p) => p.id === bookForm.value.passengerId,
    );
    await request.post("/api/order/flight/create", {
      flightId: selectedFlight.value.id,
      passengerId: passenger?.id,
      seatType: bookForm.value.seatType === "business" ? "Business" : "Economy",
      ticketCount: bookForm.value.ticketCount,
      userCouponId: bookForm.value.userCouponId,
    });
    ElMessage.success("下单成功！请前往【我的订单】完成支付");
    bookDialogVisible.value = false;
    router.push({ path: "/my-orders", query: { tab: "traffic" } });
  } catch (e) {
  } finally {
    booking.value = false;
  }
};

const calcCouponAmount = (amount, coupon) => {
  if (!coupon) return Number(amount.toFixed(2));
  const discountValue = Number(coupon.discountValue || 0);
  const discounted =
    coupon.discountType === 1 ? amount * discountValue : amount - discountValue;
  return Number(Math.max(discounted, 0).toFixed(2));
};

const couponLabel = (coupon) => {
  const discount =
    coupon.discountType === 1
      ? `${Number(coupon.discountValue) * 10}折`
      : `减¥${coupon.discountValue}`;
  return `${coupon.couponName || coupon.name}（${couponCategoryLabel(coupon.category)}，${discount}，满¥${coupon.minAmount || 0}可用）`;
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

watch(currentOriginalPrice, () => {
  if (
    bookForm.value.userCouponId &&
    !usableCoupons.value.some((coupon) => coupon.id === bookForm.value.userCouponId)
  ) {
    bookForm.value.userCouponId = null;
  }
});

onMounted(() => {
  fetchFlights();
});
</script>

<style scoped>
.flight-search-page {
  max-width: 1040px;
  margin: 0 auto;
}
.search-box {
  margin-bottom: 22px;
  border-radius: 8px;
  border: 1px solid var(--tm-line-soft);
  background: linear-gradient(135deg, var(--tm-surface), var(--tm-primary-soft));
  box-shadow: var(--tm-shadow-card);
}
.flight-card {
  margin-bottom: 12px;
  border-radius: 8px;
  border: 1px solid var(--tm-line-soft);
  background: var(--tm-surface);
  transition: all 0.3s ease;
}
.flight-card:hover {
  border-color: oklch(0.82 0.055 190);
  box-shadow: var(--tm-shadow-hover);
  transform: translateY(-2px);
}
.flight-row {
  display: grid;
  grid-template-columns: 140px minmax(260px, 1fr) 92px 172px;
  align-items: center;
  gap: 22px;
}
.flight-airline {
  min-width: 0;
}
.airline-name {
  font-weight: 700;
  font-size: 16px;
  color: var(--tm-ink);
}
.flight-no {
  font-size: 12px;
  color: var(--tm-muted);
  margin-top: 2px;
  font-family: "SF Mono", "Menlo", monospace;
}
.flight-time {
  flex: 1;
  text-align: center;
}
.time-depart,
.time-arrive {
  font-size: 20px;
  font-weight: 700;
  color: var(--tm-ink);
}
.flight-route {
  font-size: 13px;
  color: var(--el-color-primary);
  margin: 6px 0;
}
.flight-seats {
  text-align: center;
}
.flight-price-col {
  text-align: right;
}
.price-num {
  font-size: 24px;
  font-weight: 800;
  color: var(--tm-accent);
}
.price-label {
  font-size: 11px;
  color: var(--tm-muted);
  margin-left: 4px;
}
.price-num-sm {
  font-size: 16px;
  font-weight: 700;
  color: oklch(0.54 0.12 165);
}
.price-business {
  margin-bottom: 8px;
}
.book-flight-info {
  padding: 14px;
  background: var(--tm-primary-soft);
  border: 1px solid var(--tm-line-soft);
  border-radius: 8px;
  margin-bottom: 4px;
}
.total-price {
  font-size: 24px;
  font-weight: 800;
  color: var(--tm-accent);
}
.origin-price {
  margin-right: 8px;
  font-size: 14px;
  color: var(--tm-muted);
  text-decoration: line-through;
}

@media (max-width: 768px) {
  .flight-row {
    grid-template-columns: 1fr;
    gap: 12px;
    align-items: flex-start;
  }
  .flight-price-col {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
