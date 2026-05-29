<template>
  <div class="hotel-detail-page" v-loading="loading">
    <div v-if="hotel">
      <!-- 酒店基本信息 -->
      <el-card class="hotel-header-card">
        <el-row :gutter="24">
          <el-col :span="10">
            <SafeImage
              :src="hotel.coverImg"
              image-class="hotel-cover"
              :alt="hotel.name"
            />
          </el-col>
          <el-col :span="14">
            <h1 class="hotel-name">{{ hotel.name }}</h1>
            <div class="hotel-star">
              <el-icon v-for="i in hotel.starRating" :key="i" class="star-icon"><StarFilled /></el-icon>
              <el-tag type="warning" style="margin-left: 12px; font-size: 16px">
                {{ hotel.score || hotel.rating || "暂无" }} 分
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
          <el-button type="primary" link size="small" style="float:right" @click="showReviewForm = !showReviewForm">
            {{ showReviewForm ? '收起' : '写评价' }}
          </el-button>
        </template>

        <!-- 写评价表单 -->
        <el-form v-if="showReviewForm" :model="newReview" label-width="80px" class="review-form">
          <el-form-item label="评分">
            <el-rate v-model="newReview.rating" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="newReview.content" type="textarea" :rows="3" placeholder="分享您的入住体验..." />
          </el-form-item>
          <el-form-item label="标签">
            <el-checkbox-group v-model="newReview.tags">
              <el-checkbox v-for="t in reviewTagOptions" :key="t" :label="t" :value="t" />
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="图片">
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              list-type="picture-card"
              :on-success="onUploadSuccess"
              :on-remove="onUploadRemove"
              :file-list="uploadFiles"
              :limit="6"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submittingReview" @click="submitReview">提交评价</el-button>
          </el-form-item>
        </el-form>

        <el-empty v-if="reviews.length === 0" description="暂无评价" />
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-header">
            <span class="review-user">{{ review.username || '匿名用户' }}</span>
            <el-rate :model-value="review.rating" disabled style="margin-left: 12px" />
            <span class="review-date">{{ review.createTime }}</span>
            <el-button link type="danger" size="small" style="margin-left:auto" @click="reportReview(review.id)">举报</el-button>
          </div>
          <div class="review-content">{{ review.content }}</div>
          <div v-if="review.tags" class="review-tags">
            <el-tag v-for="t in review.tags.split(',')" :key="t" size="small" round effect="plain" style="margin-right:6px">
              {{ t }}
            </el-tag>
          </div>
          <div v-if="review.images" class="review-images">
            <el-image
              v-for="(img, i) in parseImages(review.images)"
              :key="i"
              :src="img"
              :preview-src-list="parseImages(review.images)"
              style="width:80px;height:80px;border-radius:8px;margin-right:8px;object-fit:cover"
            >
              <template #error>
                <img :src="FALLBACK_IMAGE" class="review-image-fallback" alt="图片暂不可用" />
              </template>
            </el-image>
          </div>
          <el-divider v-if="review.replies && review.replies.length" style="margin:10px 0" />
          <div v-for="reply in review.replies" :key="reply.id" class="reply-item">
            <span class="reply-label">商家回复：</span>{{ reply.content }}
          </div>
        </div>
      </el-card>
    </div>

    <!-- 预订 Dialog -->
    <el-dialog v-model="bookDialogVisible" title="预订酒店" width="500px">
      <el-form :model="bookForm" label-width="80px">
        <el-form-item label="房型">
          <span>{{ selectedRoom?.roomType }}</span>
        </el-form-item>
        <el-form-item label="房间数">
          <el-input-number
            v-model="bookForm.roomCount"
            :min="1"
            :max="selectedRoom?.availableRooms || 1"
          />
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
            <span v-if="currentPayablePrice < calcTotalPrice" class="origin-price">¥{{ calcTotalPrice }}</span>
            ¥{{ currentPayablePrice }}
          </span>
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
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { StarFilled, LocationFilled, Plus } from "@element-plus/icons-vue";
import request from "@/utils/request";
import SafeImage from "@/components/SafeImage.vue";
import { FALLBACK_IMAGE, parseImageList } from "@/utils/image";

const route = useRoute();
const router = useRouter();
const hotelId = route.params.id;
const HOTEL_REVIEW_TARGET_TYPE = 0;
const hotel = ref(null);
const rooms = ref([]);
const reviews = ref([]);
const myCoupons = ref([]);
const loading = ref(false);
const bookDialogVisible = ref(false);
const booking = ref(false);
const selectedRoom = ref(null);
const showReviewForm = ref(false);
const submittingReview = ref(false);

const reviewTagOptions = ["干净卫生", "性价比高", "服务好", "位置便利", "设施齐全", "环境安静", "早餐丰富", "景观好"];

const newReview = ref({ rating: 5, content: "", tags: [] });
const uploadFiles = ref([]);
const uploadedImageUrls = ref([]);

const uploadUrl = "/api/file/upload";
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem("token")}`,
}));

const bookForm = ref({
  checkIn: "",
  checkOut: "",
  roomCount: 1,
  guestName: "",
  guestPhone: "",
  userCouponId: null,
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
  return days > 0 ? days * selectedRoom.value.price * (bookForm.value.roomCount || 1) : 0;
});

const usableCoupons = computed(() =>
  myCoupons.value.filter(
    (coupon) =>
      coupon.status === 0 &&
      isCouponUsableFor(coupon, "hotel") &&
      calcTotalPrice.value >= Number(coupon.minAmount || 0),
  ),
);

const currentPayablePrice = computed(() => {
  const coupon = usableCoupons.value.find((item) => item.id === bookForm.value.userCouponId);
  return calcCouponAmount(calcTotalPrice.value, coupon);
});

const fetchHotelDetail = async () => {
  loading.value = true;
  try {
    const [hotelData, roomData, reviewData] = await Promise.allSettled([
      request.get(`/api/hotel/${hotelId}`),
      request.get(`/api/hotel/${hotelId}/rooms`),
      request.get("/api/review/list", {
        params: { targetId: hotelId, targetType: HOTEL_REVIEW_TARGET_TYPE },
        skipErrorMessage: true,
      }),
    ]);
    const detail = hotelData.status === "fulfilled" ? hotelData.value : null;
    hotel.value = detail?.hotel || detail || null;
    rooms.value =
      roomData.status === "fulfilled" && Array.isArray(roomData.value)
        ? roomData.value
        : Array.isArray(detail?.rooms)
          ? detail.rooms
        : [];
    reviews.value =
      reviewData.status === "fulfilled" && Array.isArray(reviewData.value)
        ? reviewData.value
        : [];
  } finally {
    loading.value = false;
  }
};

const openBookDialog = async (room) => {
  selectedRoom.value = room;
  bookForm.value = {
    checkIn: "",
    checkOut: "",
    roomCount: 1,
    guestName: "",
    guestPhone: "",
    userCouponId: null,
  };
  bookDialogVisible.value = true;
  await fetchMyCoupons();
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
      roomCount: bookForm.value.roomCount,
      checkInDate: bookForm.value.checkIn,
      checkOutDate: bookForm.value.checkOut,
      guestName: bookForm.value.guestName,
      guestPhone: bookForm.value.guestPhone,
      userCouponId: bookForm.value.userCouponId,
    });
    ElMessage.success("预订成功！请前往【我的订单】完成支付");
    window.dispatchEvent(new Event("notification-updated"));
    bookDialogVisible.value = false;
    router.push({ path: "/my-orders", query: { tab: "hotel" } });
  } catch (e) {
  } finally {
    booking.value = false;
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

watch(calcTotalPrice, () => {
  if (
    bookForm.value.userCouponId &&
    !usableCoupons.value.some((coupon) => coupon.id === bookForm.value.userCouponId)
  ) {
    bookForm.value.userCouponId = null;
  }
});

const onUploadSuccess = (res) => {
  const url = res?.url || res?.data?.url || (typeof res?.data === "string" ? res.data : "");
  if (url) uploadedImageUrls.value.push(url);
};

const onUploadRemove = (file) => {
  const url = file.response?.url || file.response?.data?.url || file.response?.data || file.url;
  uploadedImageUrls.value = uploadedImageUrls.value.filter((u) => u !== url);
};

const submitReview = async () => {
  if (!newReview.value.content.trim()) {
    ElMessage.warning("请输入评价内容");
    return;
  }
  submittingReview.value = true;
  try {
    const body = {
      targetId: hotelId,
      targetType: HOTEL_REVIEW_TARGET_TYPE,
      rating: newReview.value.rating,
      content: newReview.value.content,
      tags: newReview.value.tags.join(","),
      images: uploadedImageUrls.value.length > 0 ? JSON.stringify(uploadedImageUrls.value) : null,
    };
    await request.post("/api/review/add", body);
    ElMessage.success("评价发表成功");
    newReview.value = { rating: 5, content: "", tags: [] };
    uploadFiles.value = [];
    uploadedImageUrls.value = [];
    showReviewForm.value = false;
    await fetchReviews();
  } catch (e) {
  } finally {
    submittingReview.value = false;
  }
};

const reportReview = async (reviewId) => {
  try {
    await request.post("/api/review/report", { reviewId, reason: "内容不当" });
    ElMessage.success("举报已提交");
  } catch (e) {
  }
};

const fetchReviews = async () => {
  try {
    const data = await request.get("/api/review/list", {
      params: { targetId: hotelId, targetType: HOTEL_REVIEW_TARGET_TYPE },
      skipErrorMessage: true,
    });
    const revs = Array.isArray(data) ? data : [];
    for (const rev of revs) {
      try {
        const replies = await request.get("/api/reply/list", { params: { reviewId: rev.id } });
        rev.replies = Array.isArray(replies) ? replies : [];
      } catch (e) {
        rev.replies = [];
      }
    }
    reviews.value = revs;
  } catch (e) {
    reviews.value = [];
  }
};

const parseImages = (images) => parseImageList(images);

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
  font-size: 26px;
  font-weight: 800;
  color: #ef4444;
  text-shadow: 0 8px 22px rgba(239, 68, 68, 0.12);
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
.review-tags {
  margin-top: 8px;
}
.review-images {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
}
.review-image-fallback {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.review-form {
  padding: 16px;
  background: #f8fafc;
  border-radius: 12px;
  margin-bottom: 20px;
}
.reply-item {
  padding: 8px 14px;
  background: #FFF7ED;
  border-radius: 8px;
  font-size: 13px;
  color: #71718B;
}
.reply-label {
  color: #F59E0B;
  font-weight: 600;
}
.total-price {
  font-size: 24px;
  font-weight: 800;
  color: #ef4444;
}
.origin-price {
  margin-right: 8px;
  font-size: 14px;
  color: #94a3b8;
  text-decoration: line-through;
}
</style>
