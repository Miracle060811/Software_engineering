<template>
  <div class="community-page">
    <div class="community-header">
      <h2 class="page-title">旅行社区</h2>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索游记..."
          style="width: 200px"
          clearable
          @keyup.enter="fetchPosts"
        />
        <el-button type="primary" @click="$router.push('/post/create')">
          + 发布游记
        </el-button>
      </div>
    </div>

    <div v-loading="loading">
      <el-empty
        v-if="!loading && posts.length === 0"
        description="暂无游记，快来发布第一篇吧"
      />
      <div class="post-grid">
        <el-card
          v-for="post in posts"
          :key="post.id"
          class="post-card"
          :body-style="{ padding: 0 }"
          @click="$router.push(`/post/${post.id}`)"
        >
          <img
            :src="getFirstImage(post.images)"
            class="post-cover"
            :alt="post.title"
          />
          <div class="post-info">
            <div class="post-title">{{ post.title }}</div>
            <div class="post-meta">
              <span class="post-author">
                {{ post.authorNickname || post.authorUsername }}
              </span>
              <span class="post-likes"> ❤️ {{ post.likeCount || 0 }} </span>
            </div>
            <div class="post-location" v-if="post.destination">
              📍 {{ post.destination }}
            </div>
          </div>
        </el-card>
      </div>

      <div class="load-more" v-if="hasMore">
        <el-button @click="loadMore" :loading="loadingMore">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import request from "@/utils/request";

const posts = ref([]);
const loading = ref(false);
const loadingMore = ref(false);
const keyword = ref("");
const page = ref(1);
const size = 10;
const hasMore = ref(true);

const fetchPosts = async () => {
  loading.value = true;
  page.value = 1;
  try {
    const data = await request.get("/api/post/list", {
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

const loadMore = async () => {
  loadingMore.value = true;
  page.value += 1;
  try {
    const data = await request.get("/api/post/list", {
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
.community-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #333;
}
.post-grid {
  columns: 2;
  column-gap: 20px;
}
.post-card {
  break-inside: avoid;
  margin-bottom: 20px;
  cursor: pointer;
  border-radius: 12px;
  overflow: hidden;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  display: inline-block;
  width: 100%;
}
.post-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
}
.post-cover {
  width: 100%;
  object-fit: cover;
  display: block;
  max-height: 300px;
}
.post-info {
  padding: 14px 16px;
}
.post-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}
.post-author {
  font-size: 13px;
  color: #666;
}
.post-likes {
  font-size: 13px;
  color: #EF4444;
}
.post-location {
  font-size: 12px;
  color: #999;
}
.load-more {
  text-align: center;
  margin-top: 24px;
}
</style>
