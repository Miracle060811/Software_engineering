<template>
  <div class="order-list-container">
    <el-card>
      <h2>我的出行订单</h2>
      <el-table :data="orders" style="width: 100%" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" width="220" />
        <el-table-column prop="orderType" label="类型">
          <template #default="scope">
            <el-tag :type="scope.row.orderType === 0 ? 'primary' : 'success'">
              {{ scope.row.orderType === 0 ? "机票" : "火车票" }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="乘车人信息" width="200">
          <template #default="scope">
            <div>{{ scope.row.passengerName }}</div>
            <div style="font-size: 12px; color: #999">
              {{ scope.row.passengerIdCard }}
            </div>
            <div style="font-size: 12px; color: #999">
              座位: {{ scope.row.seatType }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="订单金额(元)" width="120">
          <template #default="scope">
            <span style="color: #ff4d4f; font-weight: bold"
              >￥{{ scope.row.amount }}</span
            >
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="right">
          <template #default="scope">
            <template v-if="scope.row.status === 0">
              <el-button
                type="danger"
                size="small"
                @click="handlePay(scope.row.orderNo)"
                >去支付</el-button
              >
              <el-button size="small" @click="handleCancel(scope.row.orderNo)"
                >取消</el-button
              >
            </template>
            <template
              v-else-if="scope.row.status === 1 || scope.row.status === 2"
            >
              <el-button type="info" size="small" plain disabled
                >申请退改</el-button
              >
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "@/utils/request";
import { ElMessage, ElMessageBox } from "element-plus";

const orders = ref([]);
const loading = ref(false);

const getStatusLabel = (status) => {
  const map = {
    0: "待支付",
    1: "出票中",
    2: "已出票",
    3: "已取消",
    4: "已退票",
  };
  return map[status] || "未知状态";
};

const getStatusType = (status) => {
  const map = {
    0: "warning",
    1: "primary",
    2: "success",
    3: "info",
    4: "info",
  };
  return map[status] || "info";
};

const fetchOrders = async () => {
  loading.value = true;
  try {
    const data = await request.get("/order/list");
    orders.value = data;
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

const handlePay = async (orderNo) => {
  try {
    await request.post(`/order/pay/${orderNo}`);
    ElMessage.success("支付成功，订单状态已更新为：出票中");
    fetchOrders();
  } catch (err) {
    console.error("支付失败", err);
  }
};

const handleCancel = async (orderNo) => {
  try {
    await ElMessageBox.confirm(
      "确认放弃支付并取消订单吗? 取消后将归还席位",
      "提示",
      { type: "warning" },
    );
    await request.post(`/order/cancel/${orderNo}`);
    ElMessage.success("订单已取消成功");
    fetchOrders();
  } catch (err) {
    if (err !== "cancel") console.error("取消订单异常");
  }
};

onMounted(() => {
  fetchOrders();
});
</script>

<style scoped>
.order-list-container {
  padding: 20px;
}
</style>
