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
      <el-tab-pane label="我的" name="mine" />
    </el-tabs>

    <el-alert
      v-if="activeTab === 'mine'"
      class="audit-alert"
      type="info"
      show-icon
      :closable="false"
      title="发布后的游记会先进入待审核，管理员在后台审核通过后才会出现在推荐流。草稿只在这里和个人主页中显示。"
    />

    <div v-if="activeTab === 'mine'" class="mine-filters">
      <el-radio-group v-model="mineStatusFilter" size="small" @change="fetchPosts">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="draft">草稿</el-radio-button>
        <el-radio-button label="pending">待审核</el-radio-button>
        <el-radio-button label="published">已发布</el-radio-button>
        <el-radio-button label="rejected">已拒绝</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 骨架屏 -->
    <div v-if="loading" class="post-grid">
      <SkeletonBox type="card" :count="4" />
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="posts.length === 0"
      icon="document"
      :title="emptyTitle"
      :description="emptyDescription"
      :action-text="activeTab === 'following' ? '' : '发布游记'"
      @action="$router.push('/post/create')"
    />

    <!-- 游记列表 -->
    <template v-else>
      <div class="post-grid">
        <el-card
          v-for="post in posts"
          :key="post.id"
          :class="['post-card', { 'text-only-card': !hasImages(post.images), 'my-post-card': activeTab === 'mine' }]"
          :body-style="{ padding: 0 }"
          @click="openPost(post)"
        >
          <div v-if="hasImages(post.images)" class="post-img-wrap">
            <img
              :src="getFirstImage(post.images)"
              class="post-cover"
              :alt="post.title"
              referrerpolicy="no-referrer"
              @error="onImageError($event, post)"
            />
            <div class="post-img-overlay"></div>
          </div>
          <div class="post-info">
            <div v-if="activeTab === 'mine'" class="post-status-row">
              <el-tag size="small" :type="statusType(post.status)">
                {{ statusLabel(post.status) }}
              </el-tag>
              <div class="my-post-actions" @click.stop>
                <el-button
                  v-if="post.status === 3"
                  link
                  size="small"
                  type="primary"
                  @click="editDraft(post)"
                >
                  继续编辑
                </el-button>
                <el-button link size="small" type="danger" @click="deletePost(post)">
                  删除
                </el-button>
              </div>
            </div>
            <div class="post-title">{{ post.title }}</div>
            <div v-if="!hasImages(post.images)" class="post-excerpt">{{ post.content }}</div>
            <div class="post-meta">
              <span class="post-author">
                <el-icon :size="14"><UserFilled /></el-icon>
                {{ getAuthorName(post) }}
              </span>
              <span class="post-likes">
                <el-icon :size="14"><StarFilled /></el-icon>
                {{ getLikeCount(post) }}
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
import { computed, ref, onMounted } from "vue";
import { Plus, UserFilled, StarFilled, LocationFilled, Notebook } from "@element-plus/icons-vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import request from "@/utils/request";
import PageHeader from "@/components/PageHeader.vue";
import SkeletonBox from "@/components/SkeletonBox.vue";
import EmptyState from "@/components/EmptyState.vue";
import { useUserStore } from "@/stores/user";

const posts = ref([]);
const loading = ref(false);
const loadingMore = ref(false);
const keyword = ref("");
const page = ref(1);
const size = 10;
const hasMore = ref(true);
const activeTab = ref("recommend");
const mineStatusFilter = ref("all");
const userStore = useUserStore();
const router = useRouter();

const emptyTitle = computed(() => {
  if (activeTab.value === "following") return "暂无关注的游记";
  if (activeTab.value === "mine") return "还没有自己的游记";
  return "暂无游记";
});

const emptyDescription = computed(() => {
  if (activeTab.value === "following") return "关注更多用户，查看他们的旅行分享";
  if (activeTab.value === "mine") return "发布内容会先进入待审核，草稿会保存在这里";
  return "还没有人分享游记，快来发布第一篇吧";
});

const fetchPosts = async () => {
  if ((activeTab.value === "following" || activeTab.value === "mine") && !userStore.isLoggedIn) {
    posts.value = [];
    hasMore.value = false;
    loading.value = false;
    ElMessage.warning(activeTab.value === "mine" ? "请先登录后查看我的游记" : "请先登录后查看关注流");
    return;
  }
  loading.value = true;
  page.value = 1;
  try {
    const url = activeTab.value === "following"
      ? "/api/post/following"
      : activeTab.value === "mine"
        ? "/api/post/my"
        : "/api/post/list";
    const data = await request.get(url, {
      params: { page: 1, size, keyword: keyword.value },
    });
    const list = filterMinePosts(Array.isArray(data) ? data : data?.records || []);
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
  if ((activeTab.value === "following" || activeTab.value === "mine") && !userStore.isLoggedIn) {
    ElMessage.warning(activeTab.value === "mine" ? "请先登录后查看我的游记" : "请先登录后查看关注流");
    return;
  }
  if (activeTab.value === "mine") return;
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
  const arr = typeof images === "string" ? images.split(",") : images;
  return arr?.[0]?.trim() || "";
};

const hasImages = (images) => {
  return !!getFirstImage(images);
};

const getAuthorName = (post) =>
  post.authorNickname || post.authorUsername || post.nickname || post.username || "旅行用户";

const getLikeCount = (post) => post.likeCount ?? post.like_count ?? 0;

const onImageError = (event, post) => {
  event.target.style.display = "none";
};

const filterMinePosts = (list) => {
  if (activeTab.value !== "mine") return list;
  const statusMap = { draft: 3, pending: 0, published: 1, rejected: 2 };
  let result = mineStatusFilter.value === "all"
    ? list
    : list.filter((post) => post.status === statusMap[mineStatusFilter.value]);
  if (!keyword.value.trim()) return result;
  const kw = keyword.value.trim().toLowerCase();
  return result.filter((post) =>
    [post.title, post.content, post.tags, post.destination]
      .filter(Boolean)
      .some((field) => String(field).toLowerCase().includes(kw)),
  );
};

const statusLabel = (status) =>
  ({ 0: "待审核", 1: "已发布", 2: "已拒绝", 3: "草稿" })[status] || `状态${status}`;

const statusType = (status) =>
  status === 1 ? "success" : status === 2 ? "danger" : status === 3 ? "info" : "warning";

const openPost = (post) => {
  if (activeTab.value === "mine" && post.status === 3) {
    editDraft(post);
    return;
  }
  routerPushPost(post);
};

const routerPushPost = (post) => {
  router.push(`/post/${post.id}`);
};

const editDraft = (post) => {
  router.push(`/post/create?draftId=${post.id}`);
};

const deletePost = async (post) => {
  try {
    await ElMessageBox.confirm(`确认删除「${post.title}」吗？`, "删除游记", { type: "warning" });
    await request.delete(`/api/post/${post.id}`);
    ElMessage.success("删除成功");
    posts.value = posts.value.filter((item) => item.id !== post.id);
  } catch (e) {}
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
.audit-alert {
  margin-bottom: 16px;
}
.mine-filters {
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
.text-only-card .post-info {
  min-height: 128px;
}
.my-post-card {
  position: relative;
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
.post-status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.my-post-actions {
  display: flex;
  gap: 6px;
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
.post-excerpt {
  font-size: 13px;
  color: #71718B;
  line-height: 1.7;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
