<template>
  <div class="post-detail-page" v-loading="loading">
    <div v-if="post">
      <!-- 游记头部 -->
      <el-card class="post-header-card">
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta-row">
          <div
            class="author-info"
            @click="$router.push(`/profile/${post.authorUsername}`)"
          >
            <el-avatar :size="40" :src="post.authorAvatar" />
            <div class="author-text">
              <div class="author-name">
                {{ post.authorNickname || post.authorUsername }}
              </div>
              <div class="post-date">{{ post.createTime }}</div>
            </div>
          </div>
          <el-button
            v-if="!isSelf"
            :type="isFollowing ? '' : 'primary'"
            size="small"
            @click="toggleFollow"
          >
            {{ isFollowing ? "已关注" : "+ 关注" }}
          </el-button>
        </div>

        <div class="post-tags" v-if="post.tags">
          <el-tag
            v-for="tag in parseTags(post.tags)"
            :key="tag"
            size="small"
            style="margin-right: 6px"
          >
            {{ tag }}
          </el-tag>
        </div>

        <div class="post-destination" v-if="post.destination">
          <el-icon><LocationFilled /></el-icon> {{ post.destination }}
        </div>
      </el-card>

      <!-- 图片展示 -->
      <el-card v-if="post.images" class="images-card">
        <el-image
          v-for="(img, idx) in parseImages(post.images)"
          :key="idx"
          :src="img"
          class="post-image"
          fit="cover"
          :preview-src-list="parseImages(post.images)"
          :initial-index="idx"
        />
      </el-card>

      <!-- 游记内容 -->
      <el-card class="content-card">
        <div class="post-content">{{ post.content }}</div>
      </el-card>

      <!-- 点赞 -->
      <div class="action-bar">
        <el-button
          :type="isLiked ? 'danger' : ''"
          circle
          size="large"
          @click="toggleLike"
        >
          <el-icon size="18"><component :is="isLiked ? StarFilled : Star" /></el-icon>
        </el-button>
        <span class="like-count">{{ likeCount }}</span>
      </div>

      <!-- 评论区 -->
      <el-card class="comment-card">
        <template #header>
          <span class="section-header">评论 ({{ comments.length }})</span>
        </template>

        <!-- 发表评论 -->
        <div class="comment-input-area">
          <el-input
            v-model="commentContent"
            type="textarea"
            :rows="2"
            placeholder="写下你的评论..."
          />
          <el-button
            type="primary"
            size="small"
            @click="submitComment"
            style="margin-top: 8px"
          >
            发表评论
          </el-button>
        </div>

        <el-divider />

        <el-empty
          v-if="comments.length === 0"
          description="暂无评论，快来发表第一条吧"
        />
        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <el-avatar :size="36" :src="comment.authorAvatar" />
          <div class="comment-body">
            <div class="comment-author">
              {{ comment.authorNickname || comment.authorUsername }}
            </div>
            <div class="comment-content">{{ comment.content }}</div>
            <div class="comment-footer">
              <span class="comment-date">{{ comment.createTime }}</span>
              <el-button link size="small" @click="replyTo(comment)"
                >回复</el-button
              >
              <el-button
                link
                size="small"
                type="danger"
                v-if="isMyComment(comment)"
                @click="deleteComment(comment.id)"
              >
                删除
              </el-button>
            </div>
            <!-- 子评论 -->
            <div v-if="comment.children?.length" class="sub-comments">
              <div
                v-for="sub in comment.children"
                :key="sub.id"
                class="sub-comment-item"
              >
                <span class="sub-author">{{
                  sub.authorNickname || sub.authorUsername
                }}</span>
                <span class="sub-content">{{ sub.content }}</span>
              </div>
            </div>
            <!-- 回复输入框 -->
            <div v-if="replyingTo?.id === comment.id" class="reply-input">
              <el-input
                v-model="replyContent"
                placeholder="回复评论..."
                size="small"
              />
              <el-button size="small" @click="submitReply(comment.id)"
                >发送</el-button
              >
              <el-button size="small" @click="replyingTo = null"
                >取消</el-button
              >
            </div>
          </div>
        </div>
      </el-card>
    </div>
    <el-empty v-else-if="!loading" description="游记不存在或暂时无法打开" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { LocationFilled, StarFilled, Star } from "@element-plus/icons-vue";
import request from "@/utils/request";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const postId = route.params.id;
const userStore = useUserStore();
const post = ref(null);
const loading = ref(false);
const comments = ref([]);
const commentContent = ref("");
const replyContent = ref("");
const replyingTo = ref(null);
const isLiked = ref(false);
const likeCount = ref(0);
const isFollowing = ref(false);

const isSelf = computed(() => {
  return userStore.userInfo?.username === post.value?.authorUsername;
});

const parseImages = (images) => {
  if (!images) return [];
  if (Array.isArray(images)) return images;
  return images
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
};

const parseTags = (tags) => {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  return tags
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
};

const isMyComment = (comment) => {
  return userStore.userInfo?.username === comment.authorUsername;
};

const fetchPost = async () => {
  loading.value = true;
  try {
    const data = await request.get(`/api/post/${postId}`);
    post.value = data;
    likeCount.value = data.likeCount || 0;
    await Promise.allSettled([fetchComments(), fetchLikeStatus()]);
  } catch (e) {
    ElMessage.error(e.message || "游记详情加载失败");
  } finally {
    loading.value = false;
  }
};

const fetchComments = async () => {
  try {
    const data = await request.get("/api/comment/list", { params: { postId } });
    comments.value = Array.isArray(data) ? data : [];
  } catch (e) {
    comments.value = [];
  }
};

const fetchLikeStatus = async () => {
  if (!userStore.isLoggedIn) return;
  try {
    const data = await request.get("/api/like/status", {
      params: { targetId: postId, targetType: "post" },
    });
    isLiked.value = !!data;
  } catch (e) {}
};

const toggleLike = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning("请先登录");
    return;
  }
  try {
    await request.post("/api/like/toggle", {
      targetId: postId,
      targetType: "post",
    });
    isLiked.value = !isLiked.value;
    likeCount.value += isLiked.value ? 1 : -1;
  } catch (e) {}
};

const toggleFollow = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning("请先登录");
    return;
  }
  try {
    await request.post(`/api/follow/${post.value.authorId}`);
    isFollowing.value = !isFollowing.value;
    ElMessage.success(isFollowing.value ? "关注成功" : "已取消关注");
  } catch (e) {}
};

const submitComment = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning("请先登录");
    return;
  }
  if (!commentContent.value.trim()) {
    ElMessage.warning("请输入评论内容");
    return;
  }
  try {
    await request.post("/api/comment/add", {
      postId,
      content: commentContent.value,
    });
    commentContent.value = "";
    ElMessage.success("评论成功");
    await fetchComments();
  } catch (e) {}
};

const replyTo = (comment) => {
  replyingTo.value = comment;
  replyContent.value = "";
};

const submitReply = async (parentId) => {
  if (!replyContent.value.trim()) {
    ElMessage.warning("请输入回复内容");
    return;
  }
  try {
    await request.post("/api/comment/add", {
      postId,
      content: replyContent.value,
      parentId,
    });
    replyingTo.value = null;
    replyContent.value = "";
    ElMessage.success("回复成功");
    await fetchComments();
  } catch (e) {}
};

const deleteComment = async (commentId) => {
  await ElMessageBox.confirm("确认删除该评论吗？", "提示", { type: "warning" });
  try {
    await request.delete(`/api/comment/${commentId}`);
    ElMessage.success("删除成功");
    await fetchComments();
  } catch (e) {}
};

onMounted(() => {
  fetchPost();
});
</script>

<style scoped>
.post-detail-page {
  max-width: 860px;
  margin: 0 auto;
}
.post-header-card,
.images-card,
.content-card,
.comment-card {
  margin-bottom: 20px;
  border-radius: 12px;
}
.post-title {
  font-size: 26px;
  font-weight: 700;
  color: #222;
  margin-bottom: 16px;
}
.post-meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.author-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.post-date {
  font-size: 12px;
  color: #999;
}
.post-tags {
  margin-bottom: 8px;
}
.post-destination {
  font-size: 13px;
  color: #666;
}
.post-image {
  width: 200px;
  height: 150px;
  border-radius: 8px;
  margin-right: 12px;
  margin-bottom: 8px;
}
.post-content {
  font-size: 15px;
  line-height: 1.8;
  color: #444;
  white-space: pre-wrap;
}
.action-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  justify-content: center;
}
.like-count {
  font-size: 16px;
  color: #EF4444;
  font-weight: 600;
}
.section-header {
  font-size: 16px;
  font-weight: 600;
}
.comment-input-area {
  margin-bottom: 4px;
}
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-body {
  flex: 1;
}
.comment-author {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.comment-content {
  font-size: 14px;
  color: #555;
  margin-bottom: 6px;
}
.comment-footer {
  display: flex;
  align-items: center;
  gap: 8px;
}
.comment-date {
  font-size: 12px;
  color: #999;
}
.sub-comments {
  background: #F8FAFC;
  border-radius: 6px;
  padding: 8px 12px;
  margin-top: 8px;
}
.sub-comment-item {
  font-size: 13px;
  color: #555;
  margin-bottom: 4px;
}
.sub-author {
  font-weight: 600;
  color: #0D9488;
  margin-right: 6px;
}
.reply-input {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
}
</style>
