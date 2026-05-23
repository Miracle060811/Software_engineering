<template>
  <div class="train-search-page">
    <PageHeader
      title="火车票搜索"
      subtitle="搜索并预订全国高铁动车车次"
      :icon="Tickets"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '火车票搜索' }
      ]"
    />

    <el-card class="search-box">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="出发站">
          <el-input
            v-model="searchForm.depStation"
            placeholder="如：北京南"
            clearable
            size="large"
          />
        </el-form-item>
        <el-form-item label="到达站">
          <el-input
            v-model="searchForm.arrStation"
            placeholder="如：上海虹桥"
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
          <el-button type="primary" size="large" @click="fetchTrains">
            <el-icon><Search /></el-icon>搜索车次
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
      v-else-if="trains.length === 0"
      icon="search"
      title="暂无车次信息"
      description="没有找到匹配的车次，请调整搜索条件试试"
    />

    <template v-else>
      <el-card v-for="train in trains" :key="train.id" class="train-card">
        <div class="train-row">
          <div class="train-no-col">
            <div class="train-no">{{ train.trainNo }}</div>
            <el-tag size="small" :type="getTrainTypeColor(train.trainType)">
              {{ train.trainType }}
            </el-tag>
          </div>
          <div class="train-time-col">
            <div class="depart-time">{{ formatTime(train.departureTime) }}</div>
            <div class="train-route-info">
              {{ train.departureStation }} → {{ train.arrivalStation }}
            </div>
            <div class="arrive-time">{{ formatTime(train.arrivalTime) }}</div>
          </div>
          <div class="train-duration-col">
            <div class="duration">
              {{ calcDuration(train.departureTime, train.arrivalTime) }}
            </div>
            <div class="duration-label">历时</div>
          </div>
          <div class="train-price-col">
            <div v-if="train.secondClassPrice" class="seat-price">
              <span class="price-red">¥{{ train.secondClassPrice }}</span>
              <span class="seat-label">二等座</span>
            </div>
            <div v-if="train.firstClassPrice" class="seat-price">
              <span class="price-orange">¥{{ train.firstClassPrice }}</span>
              <span class="seat-label">一等座</span>
            </div>
            <el-button
              type="primary"
              size="small"
              @click="openBookDialog(train)"
            >
              预订
            </el-button>
            <el-button
              link
              type="info"
              size="small"
              @click="openPriceTrend(train)"
              style="margin-left:4px"
            >
              价格趋势
            </el-button>
          </div>
        </div>
      </el-card>
    </template>

    <!-- 预订 Dialog -->
    <el-dialog v-model="bookDialogVisible" title="预订火车票" width="520px">
      <div v-if="selectedTrain" class="book-train-info">
        <p>
          <strong>{{ selectedTrain.trainNo }}</strong>
          {{ selectedTrain.departureStation }} →
          {{ selectedTrain.arrivalStation }}
        </p>
        <p>
          {{ formatTime(selectedTrain.departureTime) }} ~
          {{ formatTime(selectedTrain.arrivalTime) }}
        </p>
      </div>
      <el-divider />
      <el-form :model="bookForm" label-width="80px">
        <el-form-item label="乘客">
          <el-select
            v-model="bookForm.passengerId"
            placeholder="选择乘客"
            style="width: 100%"
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
        <el-form-item label="席别">
          <el-radio-group v-model="bookForm.seatType">
            <el-radio
              value="secondClass"
              :disabled="!selectedTrain?.secondClassPrice"
            >
              二等座 ¥{{ selectedTrain?.secondClassPrice || "暂无" }}
            </el-radio>
            <el-radio
              value="firstClass"
              :disabled="!selectedTrain?.firstClassPrice"
            >
              一等座 ¥{{ selectedTrain?.firstClassPrice || "暂无" }}
            </el-radio>
            <el-radio
              value="businessClass"
              :disabled="!selectedTrain?.businessClassPrice"
            >
              商务座 ¥{{ selectedTrain?.businessClassPrice || "暂无" }}
            </el-radio>
          </el-radio-group>
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
            <span v-if="currentPayablePrice < currentSeatPrice" class="origin-price">¥{{ currentSeatPrice }}</span>
            ¥{{ currentPayablePrice }}
          </span>
        </el-form-item>
      </el-form>
      <el-collapse style="margin-top:12px">
        <el-collapse-item title="退改签规则">
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="二等座">出发前24小时退票手续费5%，2小时内20%，出票后不可退</el-descriptions-item>
            <el-descriptions-item label="一等座/商务座">出发前24小时退票手续费3%，2小时内15%，出票后不可退</el-descriptions-item>
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
      :ticket-type="1"
      :ticket-name="priceTrendTicket?.trainNo"
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
const bookDialogVisible = ref(false);
const passengerDrawerVisible = ref(false);
const booking = ref(false);
const selectedTrain = ref(null);
const passengers = ref([]);
const myCoupons = ref([]);
const priceTrendVisible = ref(false);
const priceTrendTicket = ref(null);

const searchForm = ref({
  depStation: route.query.depStation || "",
  arrStation: route.query.arrStation || "",
  date: "",
});

const bookForm = ref({ passengerId: null, seatType: "secondClass", userCouponId: null });
const newPassenger = ref({ name: "", idCard: "", phone: "" });

const currentSeatPrice = computed(() => {
  if (!selectedTrain.value) return 0;
  const map = {
    secondClass: selectedTrain.value.secondClassPrice,
    firstClass: selectedTrain.value.firstClassPrice,
    businessClass: selectedTrain.value.businessClassPrice,
  };
  return map[bookForm.value.seatType] || 0;
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

const fetchTrains = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/train/search", {
      params: searchForm.value,
    });
    trains.value = Array.isArray(data) ? data : [];
  } catch (e) {
    trains.value = [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  searchForm.value = { depStation: "", arrStation: "", date: "" };
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
  const diff = (new Date(arr) - new Date(dep)) / 60000;
  const h = Math.floor(diff / 60);
  const m = diff % 60;
  return `${h}小时${m}分`;
};

const getTrainTypeColor = (type) => {
  const map = { G: "danger", D: "warning", Z: "success", T: "info", K: "" };
  return map[type?.[0]] || "";
};

const openPriceTrend = (train) => {
  priceTrendTicket.value = train;
  priceTrendVisible.value = true;
};

const openBookDialog = async (train) => {
  selectedTrain.value = train;
  bookForm.value = { passengerId: null, seatType: "secondClass", userCouponId: null };
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
    await request.post("/api/order/train/create", {
      trainId: selectedTrain.value.id,
      passengerId: passenger?.id,
      seatType: bookForm.value.seatType === "firstClass" ? "FirstClass" : "SecondClass",
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
  max-width: 960px;
  margin: 0 auto;
}
.search-box {
  margin-bottom: 20px;
  border-radius: 16px;
  border: 1px solid #F0F2F5;
}
.train-card {
  margin-bottom: 12px;
  border-radius: 16px;
  border: 1px solid #F0F2F5;
  transition: all 0.3s ease;
}
.train-card:hover {
  box-shadow: 0 4px 20px rgba(0,0,0,0.06);
  transform: translateY(-2px);
}
.train-row {
  display: flex;
  align-items: center;
  gap: 24px;
}
.train-no-col {
  width: 100px;
}
.train-no {
  font-size: 20px;
  font-weight: 800;
  color: #0D9488;
  margin-bottom: 4px;
  font-family: "SF Mono", "Menlo", monospace;
}
.train-time-col {
  flex: 1;
  text-align: center;
}
.depart-time,
.arrive-time {
  font-size: 20px;
  font-weight: 700;
  color: #1A1A2E;
}
.train-route-info {
  font-size: 13px;
  color: #A0A0B8;
  margin: 4px 0;
}
.train-duration-col {
  width: 80px;
  text-align: center;
}
.duration {
  font-size: 16px;
  font-weight: 600;
  color: #71718B;
}
.duration-label {
  font-size: 12px;
  color: #A0A0B8;
}
.train-price-col {
  width: 150px;
  text-align: right;
}
.seat-price {
  margin-bottom: 6px;
}
.price-red {
  font-size: 20px;
  font-weight: 800;
  color: #EF4444;
}
.price-orange {
  font-size: 16px;
  font-weight: 700;
  color: #F59E0B;
}
.seat-label {
  font-size: 11px;
  color: #A0A0B8;
  margin-left: 4px;
}
.book-train-info {
  padding: 14px;
  background: #ECFDFA;
  border-radius: 12px;
}
.total-price {
  font-size: 24px;
  font-weight: 800;
  color: #EF4444;
}
.origin-price {
  margin-right: 8px;
  font-size: 14px;
  color: #a0a0b8;
  text-decoration: line-through;
}

@media (max-width: 768px) {
  .train-row {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  .train-price-col {
    width: 100%;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
