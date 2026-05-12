<template>
  <div class="hotel-detail-page" v-loading="loading">
    <div v-if="hotel">
      <!-- 酒店基本信息 -->
      <el-card class="hotel-header-card">
        <el-row :gutter="24">
          <el-col :span="10">
            <img
              :src="
                hotel.coverImage ||
                `https://picsum.photos/seed/hotel${hotel.id}/600/360`
              "
              class="hotel-cover"
              :alt="hotel.name"
            />
          </el-col>
          <el-col :span="14">
            <h1 class="hotel-name">{{ hotel.name }}</h1>
            <div class="hotel-star">
              <el-icon v-for="i in hotel.starRating" :key="i" class="star-icon"><StarFilled /></el-icon>
              <el-tag type="warning" style="margin-left: 12px; font-size: 16px">
                {{ hotel.rating || "—" }} 分
              </el-tag>
            </div>
            <div class="hotel-meta"><el-icon><LocationFilled /></el-icon> {{ hotel.address }}</div>
            <div class="hotel-desc">{{ hotel.description }}</div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 房型列表 -->
      <el-card class="section-card">
        <template #header>
          <span class="section-header">可选房型</span>
        </template>
        <el-empty v-if="rooms.length === 0" description="暂无可用房型" />
        <div v-for="room in rooms" :key="room.id" class="room-item">
          <div class="room-info">
            <div class="room-type">{{ room.roomType }}</div>
            <div class="room-meta">
              <el-tag size="small" type="info">{{ room.bedType }}</el-tag>
              <span class="room-area" v-if="room.area">{{ room.area }}㎡</span>
              <span class="room-facilities">{{ room.facilities }}</span>
            </div>
            <div class="room-stock">
              <el-tag
                :type="room.availableRooms > 5 ? 'success' : 'warning'"
                size="small"
              >
                剩余 {{ room.availableRooms }} 间
              </el-tag>
            </div>
          </div>
          <div class="room-price-col">
            <div class="room-price">¥{{ room.price }}<span>/晚</span></div>
            <el-button type="primary" @click="openBookDialog(room)"
              >立即预订</el-button
            >
          </div>
        </div>
      </el-card>

      <!-- 评价列表 -->
      <el-card class="section-card">
        <template #header>
          <span class="section-header">用户评价</span>
        </template>
        <el-empty v-if="reviews.length === 0" description="暂无评价" />
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-header">
            <span class="review-user">{{ review.username }}</span>
            <el-rate
              :model-value="review.rating"
              disabled
              style="margin-left: 12px"
            />
            <span class="review-date">{{ review.createTime }}</span>
          </div>
          <div class="review-content">{{ review.content }}</div>
        </div>
      </el-card>
    </div>

    <!-- 预订 Dialog -->
    <el-dialog v-model="bookDialogVisible" title="预订酒店" width="500px">
      <el-form :model="bookForm" label-width="80px">
        <el-form-item label="房型">
          <span>{{ selectedRoom?.roomType }}</span>
        </el-form-item>
        <el-form-item label="入住日期">
          <el-date-picker
            v-model="bookForm.checkIn"
            type="date"
            placeholder="选择入住日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="退房日期">
          <el-date-picker
            v-model="bookForm.checkOut"
            type="date"
            placeholder="选择退房日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="联系姓名">
          <el-input v-model="bookForm.guestName" placeholder="联系人姓名" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="bookForm.guestPhone" placeholder="联系人手机号" />
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="total-price">¥{{ calcTotalPrice }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="booking" @click="confirmBook"
          >确认预订</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { StarFilled, LocationFilled } from "@element-plus/icons-vue";
import request from "@/utils/request";

const route = useRoute();
const hotelId = route.params.id;
const hotel = ref(null);
const rooms = ref([]);
const reviews = ref([]);
const loading = ref(false);
const bookDialogVisible = ref(false);
const booking = ref(false);
const selectedRoom = ref(null);

const bookForm = ref({
  checkIn: "",
  checkOut: "",
  guestName: "",
  guestPhone: "",
});

const calcTotalPrice = computed(() => {
  if (
    !bookForm.value.checkIn ||
    !bookForm.value.checkOut ||
    !selectedRoom.value
  )
    return 0;
  const days = Math.ceil(
    (new Date(bookForm.value.checkOut) - new Date(bookForm.value.checkIn)) /
      86400000,
  );
  return days > 0 ? days * selectedRoom.value.price : 0;
});

const fetchHotelDetail = async () => {
  loading.value = true;
  try {
    const [hotelData, roomData, reviewData] = await Promise.allSettled([
      request.get(`/api/hotel/${hotelId}`),
      request.get(`/api/hotel/${hotelId}/rooms`),
      request.get("/api/review/list", {
        params: { targetId: hotelId, targetType: "hotel" },
      }),
    ]);
    hotel.value = hotelData.status === "fulfilled" ? hotelData.value : null;
    rooms.value =
      roomData.status === "fulfilled" && Array.isArray(roomData.value)
        ? roomData.value
        : [];
    reviews.value =
      reviewData.status === "fulfilled" && Array.isArray(reviewData.value)
        ? reviewData.value
        : [];
  } finally {
    loading.value = false;
  }
};

const openBookDialog = (room) => {
  selectedRoom.value = room;
  bookForm.value = {
    checkIn: "",
    checkOut: "",
    guestName: "",
    guestPhone: "",
  };
  bookDialogVisible.value = true;
};

const confirmBook = async () => {
  if (!bookForm.value.checkIn || !bookForm.value.checkOut) {
    ElMessage.warning("请选择入住和退房日期");
    return;
  }
  if (!bookForm.value.guestName || !bookForm.value.guestPhone) {
    ElMessage.warning("请填写联系人信息");
    return;
  }
  booking.value = true;
  try {
    await request.post("/api/hotel/order/create", {
      hotelId: hotel.value.id,
      roomId: selectedRoom.value.id,
      checkInDate: bookForm.value.checkIn,
      checkOutDate: bookForm.value.checkOut,
      guestName: bookForm.value.guestName,
      guestPhone: bookForm.value.guestPhone,
    });
    ElMessage.success("预订成功！请前往【我的订单】完成支付");
    window.dispatchEvent(new Event("notification-updated"));
    bookDialogVisible.value = false;
  } catch (e) {
  } finally {
    booking.value = false;
  }
};

onMounted(() => {
  fetchHotelDetail();
});
</script>

<style scoped>
.hotel-detail-page {
  max-width: 1100px;
  margin: 0 auto;
}
.hotel-header-card {
  margin-bottom: 20px;
}
.hotel-cover {
  width: 100%;
  height: 280px;
  object-fit: cover;
  border-radius: 8px;
}
.hotel-name {
  font-size: 26px;
  font-weight: 700;
  color: #222;
  margin-bottom: 12px;
}
.hotel-star {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.star-icon {
  color: #F59E0B;
  margin-right: 2px;
}
.hotel-meta {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}
.hotel-desc {
  font-size: 14px;
  color: #888;
  line-height: 1.7;
}
.section-card {
  margin-bottom: 20px;
}
.section-header {
  font-size: 18px;
  font-weight: 600;
}
.room-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}
.room-item:last-child {
  border-bottom: none;
}
.room-type {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}
.room-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.room-area,
.room-facilities {
  font-size: 13px;
  color: #999;
}
.room-price-col {
  text-align: right;
}
.room-price {
  font-size: 22px;
  font-weight: 700;
  color: #ef4444;
  margin-bottom: 8px;
}
.room-price span {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 400;
}
.review-item {
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
}
.review-item:last-child {
  border-bottom: none;
}
.review-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}
.review-user {
  font-weight: 600;
  color: #1e293b;
}
.review-date {
  margin-left: auto;
  font-size: 12px;
  color: #94a3b8;
}
.review-content {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
}
.total-price {
  font-size: 22px;
  font-weight: 700;
  color: #ef4444;
}
</style>
