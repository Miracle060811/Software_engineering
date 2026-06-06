<template>
  <div class="hotel-search-page">
    <PageHeader
      title="酒店搜索"
      subtitle="搜索并预订全国热门酒店"
      :icon="House"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '酒店搜索' }
      ]"
    />

    <el-card class="search-box">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="城市">
          <el-input
            v-model="searchForm.city"
            placeholder="目的城市"
            clearable
            size="large"
          />
        </el-form-item>
        <el-form-item label="星级">
          <el-select
            v-model="searchForm.starRating"
            placeholder="不限"
            clearable
            style="width: 120px"
            size="large"
          >
            <el-option label="五星" :value="5" />
            <el-option label="四星" :value="4" />
            <el-option label="三星" :value="3" />
            <el-option label="二星" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格区间">
          <el-input
            v-model.number="searchForm.minPrice"
            type="number"
            placeholder="最低价"
            style="width: 100px"
            size="large"
          />
          <span style="margin: 0 8px; color: #999">~</span>
          <el-input
            v-model.number="searchForm.maxPrice"
            type="number"
            placeholder="最高价"
            style="width: 100px"
            size="large"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="fetchHotels">
            <el-icon><Search /></el-icon>搜索酒店
          </el-button>
          <el-button size="large" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 骨架屏 -->
    <div v-if="loading">
      <el-row :gutter="20">
        <el-col :span="8" v-for="i in 6" :key="i" style="margin-bottom: 20px">
          <SkeletonBox type="card" :count="1" />
        </el-col>
      </el-row>
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="hotels.length === 0"
      icon="search"
      title="暂无酒店信息"
      description="没有找到匹配的酒店，请调整搜索条件试试"
    />

    <el-row v-else :gutter="20">
        <el-col
          :span="8"
          v-for="hotel in hotels"
          :key="hotel.id"
          style="margin-bottom: 20px"
        >
          <el-card
            class="hotel-card"
            :body-style="{ padding: 0 }"
            @click="$router.push(`/hotel/${hotel.id}`)"
          >
            <SafeImage
              :src="resolveHotelCover(hotel)"
              image-class="hotel-img"
              :alt="hotel.name"
            />
            <div class="hotel-info">
              <div class="hotel-name">{{ hotel.name }}</div>
              <div class="hotel-star">
                <el-icon v-for="i in hotel.starRating" :key="i" class="star-icon"><StarFilled /></el-icon>
                <el-tag size="small" type="warning" style="margin-left: 8px">
                  {{ hotel.rating || hotel.score || "暂无" }} 分
                </el-tag>
              </div>
              <div class="hotel-location"><el-icon><LocationFilled /></el-icon> {{ hotel.address }}</div>
              <div class="hotel-price">
                <span class="price-from">起价</span>
                <span class="price-value"
                  >¥{{ hotel.minPrice || hotel.price || hotel.avgPrice || "—" }}</span
                >
                <span class="price-unit">/晚</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import { Search, House, StarFilled, LocationFilled } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";
import SafeImage from "@/components/SafeImage.vue";

const route = useRoute();
const hotels = ref([]);
const loading = ref(false);

const hotelCoverOverrides = {
  32: "/images/real/hotels/shanghai-treasury.webp",
  "上海人民广场南京东路珍宝酒店": "/images/real/hotels/shanghai-treasury.webp",
};

const resolveHotelCover = (hotel) => {
  if (!hotel) return "";
  return hotelCoverOverrides[hotel.id] || hotelCoverOverrides[hotel.name] || hotel.coverImg;
};

const searchForm = ref({
  city: route.query.city || "",
  starRating: null,
  minPrice: null,
  maxPrice: null,
});

const fetchHotels = async () => {
  loading.value = true;
  try {
    const params = {};
    if (searchForm.value.city) params.city = searchForm.value.city;
    if (searchForm.value.starRating)
      params.starRating = searchForm.value.starRating;
    if (searchForm.value.minPrice) params.minPrice = searchForm.value.minPrice;
    if (searchForm.value.maxPrice) params.maxPrice = searchForm.value.maxPrice;
    const data = await request.get("/api/hotel/search", { params });
    hotels.value = Array.isArray(data) ? data : [];
  } catch (e) {
    hotels.value = [];
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  searchForm.value = {
    city: "",
    starRating: null,
    minPrice: null,
    maxPrice: null,
  };
  fetchHotels();
};

onMounted(() => {
  fetchHotels();
});
</script>

<style scoped>
.hotel-search-page {
  max-width: 1200px;
  margin: 0 auto;
}
.search-box {
  margin-bottom: 24px;
  border-radius: 8px;
  border: 1px solid var(--tm-line-soft);
  background: linear-gradient(135deg, var(--tm-surface), var(--tm-primary-soft));
  box-shadow: var(--tm-shadow-card);
}
.hotel-card {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid oklch(0.93 0.018 190);
  background: linear-gradient(135deg, #ffffff 0%, #fbfffe 58%, #f2fbf8 100%);
  box-shadow: 0 10px 28px rgba(43, 96, 92, 0.06);
  transition: all 0.28s ease;
}
.hotel-card:hover {
  transform: translateY(-3px);
  border-color: oklch(0.83 0.072 184);
  box-shadow: 0 18px 42px rgba(43, 96, 92, 0.12);
}
.hotel-img {
  width: 100%;
  height: 210px;
  object-fit: cover;
  transition: transform 0.5s ease;
}
.hotel-card:hover .hotel-img {
  transform: scale(1.05);
}
.hotel-info {
  padding: 16px 18px;
}
.hotel-name {
  font-size: 17px;
  font-weight: 700;
  color: var(--tm-ink);
  margin-bottom: 8px;
}
.hotel-star {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}
.star-icon {
  color: oklch(0.72 0.145 82);
  margin-right: 2px;
}
.hotel-location {
  font-size: 13px;
  color: var(--tm-muted);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.hotel-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.price-from {
  font-size: 12px;
  color: var(--tm-muted);
}
.price-value {
  font-size: 26px;
  font-weight: 800;
  color: #ef4444;
  text-shadow: 0 8px 22px rgba(239, 68, 68, 0.12);
}
.price-unit {
  font-size: 12px;
  color: var(--tm-muted);
}

@media (max-width: 900px) {
  :deep(.el-col) {
    max-width: 50%;
    flex: 0 0 50%;
  }
}

@media (max-width: 640px) {
  :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
