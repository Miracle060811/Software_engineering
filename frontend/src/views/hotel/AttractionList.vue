<template>
  <div class="attraction-page">
    <PageHeader
      title="景点门票"
      subtitle="搜索并预订景点门票"
      :icon="Clock"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '景点门票' }
      ]"
    />

    <el-tabs v-model="activeTab" class="tab-nav">
      <el-tab-pane label="热门景点" name="attraction" />
      <el-tab-pane label="一日游" name="daytour" />
      <el-tab-pane label="周边游" name="neartour" />
    </el-tabs>

    <!-- 景点门票 -->
    <template v-if="activeTab === 'attraction'">
      <el-card class="search-box">
        <el-form :inline="true" :model="searchForm">
          <el-form-item label="城市">
            <el-input v-model="searchForm.city" placeholder="如：北京" clearable size="large" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" @click="fetchAttractions"><el-icon><Search /></el-icon>搜索景点</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <div v-if="loading">
        <SkeletonBox type="card" :count="6" />
      </div>

      <EmptyState
        v-else-if="attractions.length === 0"
        icon="search"
        title="暂无景点数据"
        description="没有找到匹配的景点，请尝试其他城市"
      />

      <el-row v-else :gutter="20">
        <el-col :span="8" v-for="attr in attractions" :key="attr.id" style="margin-bottom:20px">
          <el-card class="attr-card" :body-style="{ padding: 0 }">
            <SafeImage :src="resolveAttractionCover(attr)" image-class="attr-img" :alt="attr.name" />
            <div class="attr-info">
              <div class="attr-name">{{ attr.name }}</div>
              <div class="attr-desc">{{ attr.description }}</div>
              <div class="attr-meta">
                <span class="attr-hours" v-if="attr.openTime">
                  <el-icon><Clock /></el-icon> {{ attr.openTime }}
                </span>
                <span class="attr-stock" v-if="attr.availableTickets != null">
                  余票 {{ attr.availableTickets }}
                </span>
              </div>
              <div class="attr-footer">
                <span class="attr-price">¥{{ attr.adultPrice || 0 }}</span>
                <el-button type="primary" size="small" :disabled="attr.availableTickets === 0" @click="openBookDialog(attr)">
                  {{ attr.availableTickets === 0 ? "已售罄" : "购票" }}
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 一日游 -->
    <template v-if="activeTab === 'daytour'">
      <EmptyState
        v-if="dayTours.length === 0"
        icon="location"
        title="一日游推荐"
        description="精选一日游产品即将上线，敬请期待"
      />
      <el-row v-else :gutter="20">
        <el-col :span="8" v-for="t in dayTours" :key="t.id" style="margin-bottom:20px">
          <el-card class="tour-card" shadow="hover">
            <div class="tour-name">{{ t.name }}</div>
            <div class="tour-desc">{{ t.description }}</div>
            <div class="tour-meta">行程：{{ t.duration || "1天" }} | 出发地：{{ t.departureCity }}</div>
            <div class="tour-meta">可售班期：{{ t.schedules?.length || 0 }} 个</div>
            <div class="tour-footer">
              <span class="tour-price">¥{{ t.price }}</span>
              <el-button
                type="primary"
                size="small"
                round
                :disabled="!t.schedules?.some((schedule) => schedule.availableStock > 0)"
                @click="openTourDialog(t)"
              >
                {{ t.schedules?.some((schedule) => schedule.availableStock > 0) ? "预订" : "暂无班期" }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 周边游 -->
    <template v-if="activeTab === 'neartour'">
      <EmptyState
        v-if="nearTours.length === 0"
        icon="location"
        title="周边游推荐"
        description="精选周边游产品即将上线，敬请期待"
      />
      <el-row v-else :gutter="20">
        <el-col :span="8" v-for="t in nearTours" :key="t.id" style="margin-bottom:20px">
          <el-card class="tour-card" shadow="hover">
            <div class="tour-name">{{ t.name }}</div>
            <div class="tour-desc">{{ t.description }}</div>
            <div class="tour-meta">行程：{{ t.duration || "2天1晚" }} | 集合：{{ t.departureCity }}</div>
            <div class="tour-meta">可售班期：{{ t.schedules?.length || 0 }} 个</div>
            <div class="tour-footer">
              <span class="tour-price">¥{{ t.price }}</span>
              <el-button
                type="primary"
                size="small"
                round
                :disabled="!t.schedules?.some((schedule) => schedule.availableStock > 0)"
                @click="openTourDialog(t)"
              >
                {{ t.schedules?.some((schedule) => schedule.availableStock > 0) ? "预订" : "暂无班期" }}
              </el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 购票弹窗 -->
    <el-dialog v-model="bookDialogVisible" title="预订景点门票" width="480px">
      <div v-if="selectedAttraction" class="book-info">
        <p><strong>{{ selectedAttraction.name }}</strong></p>
        <p class="book-desc">{{ selectedAttraction.description }}</p>
      </div>
      <el-divider />
      <el-form :model="bookForm" label-width="80px">
        <el-form-item label="成人票">
          <el-input-number v-model="bookForm.adultCount" :min="0" :max="10" size="small" /> ¥{{ selectedAttraction?.adultPrice }}/张
        </el-form-item>
        <el-form-item label="儿童票" v-if="selectedAttraction?.childPrice">
          <el-input-number v-model="bookForm.childCount" :min="0" :max="10" size="small" /> ¥{{ selectedAttraction?.childPrice }}/张
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="bookForm.guestName" placeholder="姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="bookForm.guestPhone" placeholder="手机号" />
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="total-amount">¥{{ totalTicketPrice }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="booking" @click="confirmBook">确认购买</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tourDialogVisible" title="预订本地游产品" width="560px">
      <div v-if="selectedTour" class="book-info">
        <p><strong>{{ selectedTour.name }}</strong></p>
        <p class="book-desc">{{ selectedTour.description }}</p>
        <div v-if="selectedTour.steps?.length" class="tour-steps">
          <span v-for="step in selectedTour.steps" :key="step.id">
            第{{ step.dayNo }}天 · {{ step.placeName }}
          </span>
        </div>
      </div>
      <el-divider />
      <el-form :model="tourForm" label-width="90px">
        <el-form-item label="出行班期">
          <el-select v-model="tourForm.scheduleId" placeholder="请选择可售日期" style="width:100%">
            <el-option
              v-for="schedule in availableTourSchedules"
              :key="schedule.id"
              :label="`${schedule.travelDate} · ¥${schedule.unitPrice}/人 · 余${schedule.availableStock}`"
              :value="schedule.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出行人数">
          <el-input-number v-model="tourForm.participantCount" :min="1" :max="10" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="tourForm.contactName" placeholder="请输入联系人姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="tourForm.contactPhone" placeholder="请输入11位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="订单金额">
          <span class="total-amount">¥{{ totalTourPrice }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tourDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="tourBooking" @click="confirmTourBook">确认预订</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Clock, Search } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";
import SafeImage from "@/components/SafeImage.vue";
import { addBrowseHistory } from "@/utils/browseHistory";

const route = useRoute();
const router = useRouter();
const activeTab = ref("attraction");
const attractions = ref([]);
const loading = ref(false);
const searchForm = ref({ city: "" });
const bookDialogVisible = ref(false);
const booking = ref(false);
const selectedAttraction = ref(null);
const bookForm = ref({ adultCount: 1, childCount: 0, guestName: "", guestPhone: "" });
const dayTours = ref([]);
const nearTours = ref([]);
const tourDialogVisible = ref(false);
const tourBooking = ref(false);
const selectedTour = ref(null);
const tourForm = ref({
  scheduleId: null,
  participantCount: 1,
  contactName: "",
  contactPhone: "",
  idempotencyKey: "",
});

const resolveAttractionCover = (attr) => {
  return attr?.coverImg || "";
};

const totalTicketPrice = computed(() => {
  if (!selectedAttraction.value) return 0;
  let total = 0;
  if (bookForm.value.adultCount > 0) {
    total += (selectedAttraction.value.adultPrice || 0) * bookForm.value.adultCount;
  }
  if (bookForm.value.childCount > 0 && selectedAttraction.value.childPrice) {
    total += selectedAttraction.value.childPrice * bookForm.value.childCount;
  }
  return total;
});

const availableTourSchedules = computed(() =>
  (selectedTour.value?.schedules || []).filter((schedule) => Number(schedule.availableStock) > 0),
);

const selectedTourSchedule = computed(() =>
  availableTourSchedules.value.find((schedule) => schedule.id === tourForm.value.scheduleId),
);

const totalTourPrice = computed(() => {
  const unitPrice = Number(selectedTourSchedule.value?.unitPrice || 0);
  return (unitPrice * tourForm.value.participantCount).toFixed(2);
});

const newIdempotencyKey = () => {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID();
  return `tour-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const fetchTours = async (type) => {
  try {
    const data = await request.get("/api/tour/list", { params: { type } });
    if (type === 0) dayTours.value = Array.isArray(data) ? data : [];
    else nearTours.value = Array.isArray(data) ? data : [];
  } catch (e) {}
};

const fetchAttractions = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/attraction/search", { params: searchForm.value });
    attractions.value = Array.isArray(data) ? data : [];
  } catch (e) {
    attractions.value = [];
  } finally {
    loading.value = false;
  }
};

const openBookDialog = (attr) => {
  selectedAttraction.value = attr;
  addBrowseHistory({
    type: "attraction",
    id: attr.id,
    title: attr.name,
    subtitle: attr.city || "景点门票",
    path: `/attractions?city=${encodeURIComponent(attr.city || "")}`,
  });
  bookForm.value = { adultCount: 1, childCount: 0, guestName: "", guestPhone: "" };
  bookDialogVisible.value = true;
};

const openTourDialog = (tour) => {
  selectedTour.value = tour;
  const firstSchedule = (tour.schedules || []).find((schedule) => Number(schedule.availableStock) > 0);
  tourForm.value = {
    scheduleId: firstSchedule?.id || null,
    participantCount: 1,
    contactName: "",
    contactPhone: "",
    idempotencyKey: newIdempotencyKey(),
  };
  addBrowseHistory({
    type: "tour",
    id: tour.id,
    title: tour.name,
    subtitle: tour.tourType === 0 ? "一日游" : "周边游",
    path: `/attractions`,
  });
  tourDialogVisible.value = true;
};

const confirmTourBook = async () => {
  if (!selectedTourSchedule.value) {
    ElMessage.warning("请选择可售班期");
    return;
  }
  const name = tourForm.value.contactName.trim();
  const phone = tourForm.value.contactPhone.trim();
  if (name.length < 2) {
    ElMessage.warning("请填写有效联系人姓名");
    return;
  }
  if (!/^1\d{10}$/.test(phone)) {
    ElMessage.warning("请填写有效的11位手机号");
    return;
  }
  tourBooking.value = true;
  try {
    const order = await request.post("/api/tour/orders", {
      productId: selectedTour.value.id,
      scheduleId: selectedTourSchedule.value.id,
      participantCount: tourForm.value.participantCount,
      contactName: name,
      contactPhone: phone,
      expectedUnitPrice: selectedTourSchedule.value.unitPrice,
      idempotencyKey: tourForm.value.idempotencyKey,
    }, {
      headers: { "Idempotency-Key": tourForm.value.idempotencyKey },
    });
    ElMessage.success(`预订成功！订单号：${order.orderNo}`);
    tourDialogVisible.value = false;
    router.push({ path: "/my-orders", query: { tab: "tour" } });
  } catch (e) {
    // request interceptor displays the server-side validation result.
  } finally {
    tourBooking.value = false;
  }
};

const confirmBook = async () => {
  if (!bookForm.value.guestName || !bookForm.value.guestPhone) {
    ElMessage.warning("请填写联系人和电话");
    return;
  }
  const total = bookForm.value.adultCount + bookForm.value.childCount;
  if (total <= 0) {
    ElMessage.warning("请选择至少1张票");
    return;
  }
  booking.value = true;
  try {
    const resp = await request.post(`/api/attraction/${selectedAttraction.value.id}/ticket`, {
      adultCount: bookForm.value.adultCount,
      childCount: bookForm.value.childCount,
      guestName: bookForm.value.guestName,
      guestPhone: bookForm.value.guestPhone,
    });
    ElMessage.success(`购票成功！订单号：${resp}`);
    bookDialogVisible.value = false;
    window.dispatchEvent(new Event("notification-updated"));
    router.push({ path: "/my-orders", query: { tab: "attraction" } });
  } catch (e) {
  } finally {
    booking.value = false;
  }
};

watch(activeTab, (val) => {
  if (val === "daytour" && dayTours.value.length === 0) fetchTours(0);
  if (val === "neartour" && nearTours.value.length === 0) fetchTours(1);
});

watch(
  () => route.query.city,
  (city) => {
    searchForm.value.city = typeof city === "string" ? city : "";
    fetchAttractions();
  },
);

onMounted(() => {
  searchForm.value.city = typeof route.query.city === "string" ? route.query.city : "";
  fetchAttractions();
});
</script>

<style scoped>
.attraction-page { max-width: 1200px; margin: 0 auto; }
.tab-nav { margin-bottom: 20px; }
.tab-nav :deep(.el-tabs__item) { font-size: 15px; font-weight: 600; }
.search-box { margin-bottom: 24px; border-radius: 16px; border: 1px solid #F0F2F5; }
.attr-card {
  border-radius: 12px; overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}
.attr-card:hover { transform: translateY(-4px); box-shadow: 0 12px 32px rgba(0,0,0,0.12); }
.attr-img { width: 100%; height: 180px; object-fit: cover; }
.attr-info { padding: 14px 16px; }
.attr-name { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 6px; }
.attr-desc { font-size: 13px; color: #888; margin-bottom: 10px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.attr-meta { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; font-size: 12px; color: #999; }
.attr-footer { display: flex; justify-content: space-between; align-items: center; }
.attr-price { font-size: 20px; font-weight: 700; color: #EF4444; }
.book-info { padding: 14px; background: #ECFDFA; border-radius: 12px; }
.book-desc { font-size: 13px; color: #71718B; margin-top: 4px; }
.total-amount { font-size: 24px; font-weight: 800; color: #EF4444; }
.tour-card {
  border-radius: 16px; border: 1px solid #F0F2F5;
  background: linear-gradient(135deg, #FFF7ED 0%, #FFF 50%);
}
.tour-card:hover { box-shadow: 0 4px 20px rgba(0,0,0,0.06); transform: translateY(-2px); }
.tour-name { font-size: 18px; font-weight: 700; color: #1A1A2E; margin-bottom: 8px; }
.tour-desc { font-size: 13px; color: #71718B; margin-bottom: 10px; }
.tour-meta { font-size: 12px; color: #A0A0B8; margin-bottom: 12px; }
.tour-footer { display: flex; justify-content: space-between; align-items: center; }
.tour-price { font-size: 22px; font-weight: 800; color: #EF4444; }
.tour-steps { display: flex; flex-direction: column; gap: 4px; margin-top: 10px; font-size: 12px; color: #71718B; }
</style>
