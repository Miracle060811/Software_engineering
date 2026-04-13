<template>
  <div class="flight-list-container">
    <el-card class="search-box">
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="出发城市">
          <el-input v-model="searchForm.depCity" placeholder="如: 北京" />
        </el-form-item>
        <el-form-item label="到达城市">
          <el-input v-model="searchForm.arrCity" placeholder="如: 上海" />
        </el-form-item>
        <el-form-item label="出发日期">
          <el-date-picker
            v-model="searchForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchFlights">搜索航班</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="list-content">
      <el-table :data="flights" style="width: 100%" v-loading="loading">
        <el-table-column prop="airline" label="航司" width="180">
          <template #default="scope">
            <strong>{{ scope.row.airline }}</strong> <br />
            <span style="color: #999; font-size: 12px">{{
              scope.row.flightNo
            }}</span>
          </template>
        </el-table-column>
        <el-table-column label="行程 (起飞 - 降落)">
          <template #default="scope">
            <div>
              {{ scope.row.departureCity }} - {{ scope.row.arrivalCity }}
            </div>
            <div style="font-size: 12px; color: #666">
              {{ formatTime(scope.row.departureTime) }} ~
              {{ formatTime(scope.row.arrivalTime) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="availableSeats" label="余票" width="100" />
        <el-table-column label="价格与预订" width="220" align="right">
          <template #default="scope">
            <div style="color: #ff4d4f; font-size: 20px; margin-bottom: 5px">
              ￥{{ scope.row.economyPrice }}
              <span style="font-size: 12px; color: #999">(经济舱)</span>
            </div>
            <el-button
              type="warning"
              size="small"
              @click="goCheckout(scope.row)"
              >选定并预订</el-button
            >
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "@/utils/request";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";

const router = useRouter();
const flights = ref([]);
const loading = ref(false);

const searchForm = ref({
  depCity: "",
  arrCity: "",
  date: "",
});

const fetchFlights = async () => {
  loading.value = true;
  try {
    const data = await request.get("/flight/search", {
      params: searchForm.value,
    });
    flights.value = data;
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  searchForm.value = { depCity: "", arrCity: "", date: "" };
  fetchFlights();
};

const formatTime = (isoString) => {
  if (!isoString) return "";
  const date = new Date(isoString);
  return date.toLocaleString("zh-CN", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

const goCheckout = (flight) => {
  // 这里简化演示，跳转到下单页并将 flightId 传过去
  ElMessage.success("选中航班：" + flight.flightNo);
  // TODO: router.push('/checkout/' + flight.id)
  // 我们下一步会写 Checkout.vue
};

onMounted(() => {
  fetchFlights();
});
</script>

<style scoped>
.flight-list-container {
  padding: 20px;
}
.search-box {
  margin-bottom: 20px;
}
.list-content {
  background: #fff;
  border-radius: 4px;
}
</style>
