<template>
  <div class="coupon-center-page">
    <PageHeader
      title="优惠券中心"
      subtitle="领取优惠券，下单更优惠"
      :icon="Present"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '优惠券中心' }
      ]"
    />

    <el-tabs v-model="activeTab" class="coupon-tabs">
      <el-tab-pane label="可领取优惠券" name="available">
        <div v-if="availableLoading">
          <SkeletonBox type="card" :count="3" />
        </div>

        <EmptyState
          v-else-if="availableCoupons.length === 0"
          icon="document"
          title="暂无可用优惠券"
          description="暂时没有可领取的优惠券，请稍后再来"
        />

        <el-row v-else :gutter="16">
          <el-col v-for="c in availableCoupons" :key="c.id" :xs="24" :sm="12" :md="8" style="margin-bottom:16px">
            <el-card class="coupon-card" shadow="hover">
              <div class="coupon-header">
                <span class="coupon-value">
                  <template v-if="c.discountType === 0">¥{{ c.discountValue }}</template>
                  <template v-else>{{ Math.round((1 - c.discountValue) * 100) }}%</template>
                </span>
                <span class="coupon-type">{{ c.discountType === 0 ? '满减券' : '折扣券' }}</span>
              </div>
              <div class="coupon-name">{{ c.name }}</div>
              <div class="coupon-desc">{{ c.description }}</div>
              <div class="coupon-meta">
                <span v-if="c.minAmount">满¥{{ c.minAmount }}可用</span>
                <span>剩余 {{ c.stock }} 张</span>
              </div>
              <div class="coupon-expire">有效期至 {{ formatDate(c.expireDate) }}</div>
              <el-button type="primary" plain size="small" round style="width:100%;margin-top:12px" :loading="claiming === c.id" @click="claimCoupon(c.id)">
                立即领取
              </el-button>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="我的优惠券" name="my">
        <div v-if="myLoading">
          <SkeletonBox type="card" :count="3" />
        </div>

        <EmptyState
          v-else-if="myCoupons.length === 0"
          icon="tickets"
          title="暂无优惠券"
          description="还没有领取优惠券，快去看看有哪些可领的吧"
        />

        <el-row v-else :gutter="16">
          <el-col v-for="c in myCoupons" :key="c.id" :xs="24" :sm="12" :md="8" style="margin-bottom:16px">
            <el-card class="coupon-card" :class="{ 'coupon-used': c.status !== 0 }" shadow="hover">
              <div class="coupon-header">
                <span class="coupon-value">
                  <template v-if="c.discountType === 0">¥{{ c.discountValue }}</template>
                  <template v-else>{{ Math.round((1 - c.discountValue) * 100) }}%</template>
                </span>
                <el-tag size="small" :type="c.status === 0 ? 'success' : c.status === 1 ? 'info' : 'danger'" round>
                  {{ c.status === 0 ? '未使用' : c.status === 1 ? '已使用' : '已过期' }}
                </el-tag>
              </div>
              <div class="coupon-name">{{ c.couponName }}</div>
              <div class="coupon-desc">{{ c.description }}</div>
              <div class="coupon-meta">
                <span v-if="c.minAmount">满¥{{ c.minAmount }}可用</span>
              </div>
              <div class="coupon-expire">有效期至 {{ formatDate(c.expireDate) }}</div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import { Present } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";

const activeTab = ref("available");
const availableCoupons = ref([]);
const myCoupons = ref([]);
const availableLoading = ref(false);
const myLoading = ref(false);
const claiming = ref(null);

const fetchAvailable = async () => {
  availableLoading.value = true;
  try {
    const data = await request.get("/api/coupon/list");
    availableCoupons.value = Array.isArray(data) ? data : [];
  } catch (e) {
    availableCoupons.value = [];
  } finally {
    availableLoading.value = false;
  }
};

const fetchMy = async () => {
  myLoading.value = true;
  try {
    const data = await request.get("/api/coupon/my");
    myCoupons.value = Array.isArray(data) ? data : [];
  } catch (e) {
    myCoupons.value = [];
  } finally {
    myLoading.value = false;
  }
};

const claimCoupon = async (id) => {
  claiming.value = id;
  try {
    const msg = await request.post(`/api/coupon/claim/${id}`);
    ElMessage.success(msg || "领取成功");
    await fetchAvailable();
    await fetchMy();
  } catch (e) {
  } finally {
    claiming.value = null;
  }
};

const formatDate = (iso) => {
  if (!iso) return "";
  return new Date(iso).toLocaleDateString("zh-CN");
};

onMounted(() => {
  fetchAvailable();
  fetchMy();
});
</script>

<style scoped>
.coupon-center-page {
  max-width: 960px;
  margin: 0 auto;
}
.coupon-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
}
.coupon-card {
  border-radius: 16px;
  border: 1px solid #F0F2F5;
  background: linear-gradient(135deg, #ECFDFA 0%, #FFF 50%);
}
.coupon-card.coupon-used {
  opacity: 0.55;
  filter: grayscale(0.3);
}
.coupon-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.coupon-value {
  font-size: 32px;
  font-weight: 800;
  color: #EF4444;
}
.coupon-type {
  font-size: 12px;
  color: #A0A0B8;
  background: #F0F2F5;
  padding: 2px 8px;
  border-radius: 10px;
}
.coupon-name {
  font-size: 16px;
  font-weight: 700;
  color: #1A1A2E;
  margin-bottom: 6px;
}
.coupon-desc {
  font-size: 13px;
  color: #71718B;
  margin-bottom: 8px;
}
.coupon-meta {
  font-size: 12px;
  color: #A0A0B8;
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
}
.coupon-expire {
  font-size: 12px;
  color: #F59E0B;
}
</style>
