<template>
  <div class="collections-page">
    <PageHeader
      title="我的收藏"
      subtitle="集中查看收藏过的旅行游记"
      :icon="StarFilled"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '我的收藏' }
      ]"
    />

    <div v-if="loading" class="collection-grid">
      <SkeletonBox type="card" :count="4" />
    </div>

    <EmptyState
      v-else-if="collections.length === 0"
      icon="document"
      title="还没有收藏"
      description="在游记详情页点击收藏后，会集中显示在这里"
      action-text="去社区看看"
      @action="router.push('/community')"
    />

    <div v-else class="collection-grid">
      <el-card
        v-for="post in collections"
        :key="post.id"
        class="collection-card"
        :body-style="{ padding: 0 }"
        shadow="hover"
        @click="router.push(`/post/${post.id}`)"
      >
        <SafeImage
          :src="getFirstImage(post)"
          :fallback="FALLBACK_IMAGE"
          image-class="collection-cover"
          :alt="post.title"
        />
        <div class="collection-body">
          <div class="collection-title">{{ post.title }}</div>
          <div class="collection-desc">{{ post.content }}</div>
          <div class="collection-meta">
            <span v-if="post.destination">{{ post.destination }}</span>
            <span>
              <el-icon><StarFilled /></el-icon>
              {{ post.likeCount || 0 }}
            </span>
            <span>评论 {{ post.commentCount || 0 }}</span>
          </div>
          <div v-if="post.tags" class="collection-tags" @click.stop>
            <el-tag
              v-for="tag in parseTags(post.tags)"
              :key="tag"
              size="small"
              round
              effect="plain"
              @click="router.push({ path: '/community', query: { tag } })"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { StarFilled } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";
import SafeImage from "@/components/SafeImage.vue";
import { FALLBACK_IMAGE, normalizeImageUrl, parseImageList } from "@/utils/image";

const router = useRouter();
const collections = ref([]);
const loading = ref(false);

const fetchCollections = async () => {
  loading.value = true;
  try {
    const data = await request.get("/api/like/my/collects");
    collections.value = Array.isArray(data) ? data : [];
  } catch (e) {
    collections.value = [];
    ElMessage.error(e.message || "收藏列表加载失败");
  } finally {
    loading.value = false;
  }
};

const getFirstImage = (post) => {
  const first = parseImageList(post?.images)
    .map((item) => String(item || "").trim())
    .find(Boolean);
  return normalizeImageUrl(first, FALLBACK_IMAGE);
};

const parseTags = (tags) => {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  return String(tags)
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
};

onMounted(fetchCollections);
</script>

<style scoped>
.collections-page {
  max-width: 1180px;
  margin: 0 auto;
}

.collection-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px;
}

.collection-card {
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--tm-line-soft);
}

.collection-card :deep(.collection-cover) {
  width: 100%;
  height: 170px;
  display: block;
  object-fit: cover;
}

.collection-body {
  padding: 16px;
}

.collection-title {
  font-size: 17px;
  font-weight: 700;
  color: #172554;
  margin-bottom: 8px;
}

.collection-desc {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
  height: 42px;
  overflow: hidden;
}

.collection-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
  color: #64748b;
  font-size: 13px;
}

.collection-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.collection-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 12px;
}
</style>
