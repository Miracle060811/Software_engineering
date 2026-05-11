<template>
  <div class="attraction-page">
    <el-card class="search-box">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="城市">
          <el-input
            v-model="searchForm.city"
            placeholder="如：北京"
            clearable
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchAttractions"
            >搜索景点</el-button
          >
        </el-form-item>
      </el-form>
    </el-card>

    <div v-loading="loading">
      <el-empty
        v-if="!loading && attractions.length === 0"
        description="暂无景点数据"
      />
      <el-row :gutter="20">
        <el-col
          :span="8"
          v-for="attr in attractions"
          :key="attr.id"
          style="margin-bottom: 20px"
        >
          <el-card class="attr-card" :body-style="{ padding: 0 }">
            <img
              :src="
                attr.image ||
                `https://picsum.photos/seed/attr${attr.id}/400/220`
              "
              class="attr-img"
              :alt="attr.name"
            />
            <div class="attr-info">
              <div class="attr-name">{{ attr.name }}</div>
              <div class="attr-desc">{{ attr.description }}</div>
              <div class="attr-meta">
                <span class="attr-hours" v-if="attr.openingHours">
                  🕐 {{ attr.openingHours }}
                </span>
                <span class="attr-stock" v-if="attr.availableTickets != null">
                  余票 {{ attr.availableTickets }}
                </span>
              </div>
              <div class="attr-footer">
                <span class="attr-price">
                  {{ attr.ticketPrice ? `¥${attr.ticketPrice}` : "免费" }}
                </span>
                <el-button
                  type="primary"
                  size="small"
                  :disabled="attr.availableTickets === 0"
                  @click="handleBook(attr)"
                >
                  {{ attr.availableTickets === 0 ? "已售罄" : "购票" }}
                </el-button>
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
import { ElMessage } from "element-plus";
import request from "@/utils/request";

const attractions = ref([]);
const loading = ref(false);
const searchForm = ref({ city: "" });

const fetchAttractions = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/attraction/search", {
      params: searchForm.value,
    });
    attractions.value = Array.isArray(data) ? data : [];
  } catch (e) {
    attractions.value = [];
  } finally {
    loading.value = false;
  }
};

const handleBook = (attr) => {
  ElMessage.info(`景点购票功能即将上线，请先收藏「${attr.name}」`);
};

onMounted(() => {
  fetchAttractions();
});
</script>

<style scoped>
.attraction-page {
  max-width: 1200px;
  margin: 0 auto;
}
.search-box {
  margin-bottom: 24px;
}
.attr-card {
  border-radius: 12px;
  overflow: hidden;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.attr-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}
.attr-img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}
.attr-info {
  padding: 14px 16px;
}
.attr-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}
.attr-desc {
  font-size: 13px;
  color: #888;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.attr-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  font-size: 12px;
  color: #999;
}
.attr-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.attr-price {
  font-size: 20px;
  font-weight: 700;
  color: #EF4444;
}
</style>
