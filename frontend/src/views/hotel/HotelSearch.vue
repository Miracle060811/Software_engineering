<template>
  <div class="hotel-search-page">
    <el-card class="search-box">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="城市">
          <el-input
            v-model="searchForm.city"
            placeholder="目的城市"
            clearable
          />
        </el-form-item>
        <el-form-item label="星级">
          <el-select
            v-model="searchForm.starRating"
            placeholder="不限"
            clearable
            style="width: 120px"
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
          />
          <span style="margin: 0 8px; color: #999">~</span>
          <el-input
            v-model.number="searchForm.maxPrice"
            type="number"
            placeholder="最高价"
            style="width: 100px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchHotels">搜索酒店</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading">
      <el-empty
        v-if="!loading && hotels.length === 0"
        description="暂无酒店信息，请调整搜索条件"
      />
      <el-row :gutter="20">
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
            <img
              :src="
                hotel.coverImage ||
                `https://picsum.photos/seed/hotel${hotel.id}/400/220`
              "
              class="hotel-img"
              :alt="hotel.name"
            />
            <div class="hotel-info">
              <div class="hotel-name">{{ hotel.name }}</div>
              <div class="hotel-star">
                <span v-for="i in hotel.starRating" :key="i">⭐</span>
                <el-tag size="small" type="warning" style="margin-left: 8px">
                  {{ hotel.rating || "暂无" }} 分
                </el-tag>
              </div>
              <div class="hotel-location">📍 {{ hotel.address }}</div>
              <div class="hotel-price">
                <span class="price-from">起价</span>
                <span class="price-value"
                  >¥{{ hotel.minPrice || hotel.price || "—" }}</span
                >
                <span class="price-unit">/晚</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRoute } from "vue-router";
import request from "@/utils/request";

const route = useRoute();
const hotels = ref([]);
const loading = ref(false);

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
}
.hotel-card {
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.hotel-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}
.hotel-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}
.hotel-info {
  padding: 14px 16px;
}
.hotel-name {
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
  margin-bottom: 6px;
}
.hotel-star {
  margin-bottom: 6px;
  display: flex;
  align-items: center;
}
.hotel-location {
  font-size: 13px;
  color: #94A3B8;
  margin-bottom: 10px;
}
.hotel-price {
  display: flex;
  align-items: baseline;
  gap: 4px;
}
.price-from {
  font-size: 12px;
  color: #94A3B8;
}
.price-value {
  font-size: 22px;
  font-weight: 700;
  color: #EF4444;
}
.price-unit {
  font-size: 12px;
  color: #94A3B8;
}
</style>
