<template>
  <div class="coupon-center-page">
    <PageHeader
      :title="pageTitle"
      :subtitle="pageSubtitle"
      :icon="Present"
      :breadcrumbs="breadcrumbs"
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
          <el-col v-for="c in availableCoupons" :key="c.id" :xs="24" :sm="12" :md="8" class="coupon-col">
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
                <span>{{ couponCategoryLabel(c.category) }}</span>
                <span v-if="c.minAmount">满¥{{ c.minAmount }}可用</span>
                <span>剩余 {{ c.stock }} 张</span>
              </div>
              <div class="coupon-expire">有效期至 {{ formatDate(c.expireDate) }}</div>
              <el-button class="coupon-action" type="primary" plain size="small" round :loading="claiming === c.id" @click="claimCoupon(c.id)">
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
          <el-col v-for="c in myCoupons" :key="c.id" :xs="24" :sm="12" :md="8" class="coupon-col">
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
                <span>{{ couponCategoryLabel(c.category) }}</span>
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
import { computed, ref, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Present } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";

const activeTab = ref("available");
const route = useRoute();
const router = useRouter();
const availableCoupons = ref([]);
const myCoupons = ref([]);
const availableLoading = ref(false);
const myLoading = ref(false);
const claiming = ref(null);

const pageTitle = computed(() =>
  activeTab.value === "my" ? "我的优惠券" : "优惠券中心",
);
const pageSubtitle = computed(() =>
  activeTab.value === "my"
    ? "查看已领取优惠券，付款前别忘了抵扣"
    : "领取优惠券，下单更优惠",
);
const breadcrumbs = computed(() => [
  { label: "首页", to: "/" },
  { label: activeTab.value === "my" ? "我的优惠券" : "优惠券中心" },
]);

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
  if (!localStorage.getItem("token")) {
    myCoupons.value = [];
    return;
  }
  myLoading.value = true;
  try {
    const data = await request.get("/api/coupon/my", { silent: true });
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
    const claimed = availableCoupons.value.find((coupon) => coupon.id === id);
    const msg = await request.post(`/api/coupon/claim/${id}`);
    ElMessage.success(msg || "领取成功");
    availableCoupons.value = availableCoupons.value.filter(
      (coupon) => coupon.id !== id && couponRuleKey(coupon) !== couponRuleKey(claimed),
    );
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

const normalizeDecimal = (value) => {
  if (value === null || value === undefined || value === "") return "";
  return Number(value).toString();
};

const normalizeCouponCategory = (category) => {
  const value = String(category || "all").toLowerCase();
  return ["all", "flight", "train", "hotel"].includes(value) ? value : "all";
};

const couponCategoryLabel = (category) =>
  ({
    all: "全部通用",
    flight: "机票",
    train: "火车票",
    hotel: "酒店",
  }[normalizeCouponCategory(category)]);

const couponRuleKey = (coupon) => {
  if (!coupon) return "";
  return [
    coupon.name || coupon.couponName || "",
    coupon.description || "",
    normalizeCouponCategory(coupon.category),
    coupon.discountType ?? "",
    normalizeDecimal(coupon.discountValue),
    normalizeDecimal(coupon.minAmount),
  ].join("|");
};

onMounted(() => {
  activeTab.value = route.query.tab === "my" ? "my" : "available";
  fetchAvailable();
  fetchMy();
});

watch(
  () => route.query.tab,
  (tab) => {
    activeTab.value = tab === "my" ? "my" : "available";
  },
);

watch(activeTab, (tab) => {
  const nextTab = tab === "my" ? "my" : undefined;
  if (route.query.tab === nextTab) return;
  router.replace({
    path: "/coupons",
    query: nextTab ? { tab: nextTab } : {},
  });
});
</script>

<style scoped>
.coupon-center-page {
  max-width: 1120px;
  margin: 0 auto;
}
.coupon-tabs {
  padding: 6px 0 0;
}
.coupon-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #e9edf3;
}
.coupon-tabs :deep(.el-tabs__item) {
  height: 52px;
  padding: 0 22px;
  font-size: 16px;
  font-weight: 800;
  color: #3d3d5c;
}
.coupon-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
}
.coupon-tabs :deep(.el-tabs__active-bar) {
  height: 4px;
  border-radius: 4px;
}
.coupon-col {
  margin-bottom: 18px;
}
.coupon-card {
  position: relative;
  min-height: 214px;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid #e7f4f2;
  background:
    radial-gradient(circle at 92% 12%, rgba(245, 158, 11, 0.16), transparent 26%),
    linear-gradient(135deg, #ecfdfa 0%, #ffffff 54%, #fff8ed 100%);
}
.coupon-card::before,
.coupon-card::after {
  content: "";
  position: absolute;
  top: 50%;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f7f8fa;
  border: 1px solid #e7f4f2;
  transform: translateY(-50%);
}
.coupon-card::before {
  left: -10px;
}
.coupon-card::after {
  right: -10px;
}
.coupon-card.coupon-used {
  opacity: 0.68;
  filter: grayscale(0.2);
  background: linear-gradient(135deg, #f7f8fa 0%, #fff 100%);
}
.coupon-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.coupon-value {
  font-size: 36px;
  line-height: 1;
  font-weight: 800;
  color: #ef4444;
}
.coupon-type {
  font-size: 12px;
  font-weight: 700;
  color: #0d9488;
  background: rgba(13, 148, 136, 0.1);
  padding: 3px 9px;
  border-radius: 999px;
}
.coupon-name {
  font-size: 17px;
  font-weight: 800;
  color: #1a1a2e;
  margin-bottom: 6px;
}
.coupon-desc {
  font-size: 13px;
  color: #71718b;
  min-height: 40px;
  margin-bottom: 10px;
}
.coupon-meta {
  font-size: 12px;
  color: #71718b;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 6px;
}
.coupon-expire {
  font-size: 12px;
  font-weight: 700;
  color: #d97706;
}
.coupon-action {
  width: 100%;
  margin-top: 14px;
  font-weight: 800;
}

@media (max-width: 768px) {
  .coupon-tabs :deep(.el-tabs__item) {
    padding: 0 14px;
    font-size: 15px;
  }

  .coupon-card {
    min-height: auto;
  }
}
</style>
