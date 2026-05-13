<template>
  <div class="community-page">
    <PageHeader
      title="旅行社区"
      subtitle="分享旅途故事，发现精彩游记"
      :icon="Notebook"
      :breadcrumbs="[
        { label: '首页', to: '/' },
        { label: '旅行社区' }
      ]"
    >
      <template #extra>
        <div class="header-actions">
          <el-input
            v-model="keyword"
            placeholder="搜索游记..."
            style="width: 220px"
            clearable
            size="large"
            @keyup.enter="fetchPosts"
          />
          <el-button type="primary" size="large" @click="$router.push('/post/create')">
            <el-icon><Plus /></el-icon>发布游记
          </el-button>
        </div>
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" @tab-change="onTabChange" class="feed-tabs">
      <el-tab-pane label="推荐" name="recommend" />
      <el-tab-pane label="关注" name="following" />
    </el-tabs>

    <!-- 骨架屏 -->
    <div v-if="loading" class="post-grid">
      <SkeletonBox type="card" :count="4" />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="posts.length === 0"
      icon="document"
      :title="activeTab === 'following' ? '暂无关注的游记' : '暂无游记'"
      :description="activeTab === 'following' ? '关注更多用户，查看他们的旅行分享' : '还没有人分享游记，快来发布第一篇吧'"
      :action-text="activeTab === 'following' ? '' : '发布游记'"
      @action="$router.push('/post/create')"
    />

    <!-- 游记列表 -->
    <template v-else>
      <div class="post-grid">
        <el-card
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          :body-style="{ padding: 0 }"
          @click="$router.push(`/post/${post.id}`)"
        >
          <div class="post-img-wrap">
            <img
              :src="getFirstImage(post.images)"
              class="post-cover"
              :alt="post.title"
            />
            <div class="post-img-overlay"></div>
          </div>
          <div class="post-info">
            <div class="post-title">{{ post.title }}</div>
            <div class="post-meta">
              <span class="post-author">
                <el-icon :size="14"><UserFilled /></el-icon>
                {{ post.authorNickname || post.authorUsername }}
              </span>
              <span class="post-likes">
                <el-icon :size="14"><StarFilled /></el-icon>
                {{ post.likeCount || 0 }}
              </span>
            </div>
            <div class="post-location" v-if="post.destination">
              <el-icon :size="13"><LocationFilled /></el-icon>
              {{ post.destination }}
            </div>
          </div>
        </el-card>
      </div>

      <div class="load-more" v-if="hasMore">
        <el-button
          @click="loadMore"
          :loading="loadingMore"
          size="large"
          round
          class="load-more-btn"
        >
          加载更多
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { Plus, UserFilled, StarFilled, LocationFilled, Notebook } from "@element-plus/icons-vue";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";

const posts = ref([]);
const loading = ref(false);
const loadingMore = ref(false);
const keyword = ref("");
const page = ref(1);
const size = 10;
const hasMore = ref(true);
const activeTab = ref("recommend");

const fetchPosts = async () => {
  loading.value = true;
  page.value = 1;
  try {
    const url = activeTab.value === "following" ? "/api/post/following" : "/api/post/list";
    const data = await request.get(url, {
      params: { page: 1, size, keyword: keyword.value },
    });
    const list = Array.isArray(data) ? data : data?.records || [];
    posts.value = list;
    hasMore.value = list.length >= size;
  } catch (e) {
    posts.value = [];
  } finally {
    loading.value = false;
  }
};

const onTabChange = (tab) => {
  activeTab.value = tab;
  posts.value = [];
  fetchPosts();
};

const loadMore = async () => {
  loadingMore.value = true;
  page.value += 1;
  try {
    const url = activeTab.value === "following" ? "/api/post/following" : "/api/post/list";
    const data = await request.get(url, {
      params: { page: page.value, size, keyword: keyword.value },
    });
    const list = Array.isArray(data) ? data : data?.records || [];
    posts.value.push(...list);
    hasMore.value = list.length >= size;
  } catch (e) {
  } finally {
    loadingMore.value = false;
  }
};

const getFirstImage = (images) => {
  if (!images) return `https://picsum.photos/seed/${Math.random()}/400/260`;
  const arr = typeof images === "string" ? images.split(",") : images;
  return arr[0]?.trim() || `https://picsum.photos/seed/community/400/260`;
};

onMounted(() => {
  fetchPosts();
});
</script>

<style scoped>
.community-page {
  max-width: 1200px;
  margin: 0 auto;
}
.feed-tabs {
  margin-bottom: 16px;
}
.feed-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.post-grid {
  columns: 2;
  column-gap: 20px;
}

.post-card {
  break-inside: avoid;
  margin-bottom: 20px;
  cursor: pointer;
  border-radius: 16px;
  overflow: hidden;
  border: 1px solid #F0F2F5;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  display: inline-block;
  width: 100%;
}
.post-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.1);
}

.post-img-wrap {
  position: relative;
  overflow: hidden;
}
.post-img-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.3), transparent 40%);
  opacity: 0;
  transition: opacity 0.35s ease;
}
.post-card:hover .post-img-overlay {
  opacity: 1;
}

.post-cover {
  width: 100%;
  object-fit: cover;
  display: block;
  max-height: 280px;
  transition: transform 0.5s ease;
}
.post-card:hover .post-cover {
  transform: scale(1.05);
}

.post-info {
  padding: 16px 18px;
}

.post-title {
  font-size: 16px;
  font-weight: 700;
  color: #1A1A2E;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.post-author {
  font-size: 13px;
  color: #71718B;
  display: flex;
  align-items: center;
  gap: 4px;
}

.post-likes {
  font-size: 13px;
  color: #FB7185;
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
}

.post-location {
  font-size: 13px;
  color: #A0A0B8;
  display: flex;
  align-items: center;
  gap: 4px;
}

.load-more {
  text-align: center;
  margin-top: 32px;
  margin-bottom: 20px;
}

.load-more-btn {
  padding: 12px 40px;
}

@media (max-width: 768px) {
  .post-grid {
    columns: 1;
  }
  .header-actions {
    flex-direction: column;
    width: 100%;
  }
  .header-actions .el-input {
    width: 100% !important;
  }
}
</style>
