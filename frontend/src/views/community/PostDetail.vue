<template>
  <div class="post-detail-page" v-loading="loading">
    <div v-if="post">
      <!-- 游记头部 -->
      <el-card class="post-header-card">
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta-row">
          <div
            class="author-info"
            @click="goAuthorProfile(post)"
          >
            <el-avatar :size="40" :src="post.authorAvatar || ''" class="fallback-avatar">
              {{ avatarInitial(post) }}
            </el-avatar>
            <div class="author-text">
              <div class="author-name">
                {{ displayName(post) }}
              </div>
              <div class="post-date">{{ post.createTime }}</div>
            </div>
          </div>
          <div class="header-actions">
            <template v-if="isSelf">
              <el-button
                v-if="post.status === 3"
                size="small"
                @click="router.push(`/post/create?draftId=${post.id}`)"
              >
                继续编辑
              </el-button>
              <el-button size="small" type="danger" @click="deletePost">
                删除
              </el-button>
            </template>
            <el-button
              v-else
              :type="isFollowing ? '' : 'primary'"
              size="small"
              @click="toggleFollow"
            >
              {{ isFollowing ? "已关注" : "+ 关注" }}
            </el-button>
          </div>
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
        >
          <template #error>
            <img :src="FALLBACK_IMAGE" class="post-image-fallback" alt="图片暂不可用" />
          </template>
        </el-image>
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
          <span class="section-header">评论 ({{ commentTotal }})</span>
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
          <el-avatar :size="36" :src="comment.authorAvatar || ''" class="fallback-avatar">
            {{ avatarInitial(comment) }}
          </el-avatar>
          <div class="comment-body">
            <div class="comment-author">
              {{ displayName(comment) }}
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
                <span class="sub-author">{{ displayName(sub) }}</span>
                <span v-if="replyTargetName(sub)" class="reply-target">
                  回复 @{{ replyTargetName(sub) }}
                </span>
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
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { LocationFilled, StarFilled, Star } from "@element-plus/icons-vue";
import request from "@/utils/request";
import { useUserStore } from "@/stores/user";
import { FALLBACK_IMAGE, parseImageList } from "@/utils/image";

const route = useRoute();
const router = useRouter();
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
const POST_LIKE_TARGET_TYPE = 0;

const isSelf = computed(() => {
  const current = userStore.userInfo;
  const authorId = post.value?.authorId || post.value?.userId;
  return !!current && (current.username === post.value?.authorUsername || current.id === authorId || current.userId === authorId);
});

const commentTotal = computed(() => {
  if (post.value?.commentCount != null) return post.value.commentCount;
  return countComments(comments.value);
});

const parseImages = (images) => {
  return parseImageList(images);
};

const parseTags = (tags) => {
  if (!tags) return [];
  if (Array.isArray(tags)) return tags;
  return tags
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
};

const displayName = (item) => {
  return item?.authorNickname || item?.authorUsername || item?.nickname || item?.username || "匿名用户";
};

const avatarInitial = (item) => {
  const name = displayName(item);
  return name.trim().charAt(0).toUpperCase() || "旅";
};

const goAuthorProfile = (item) => {
  const username = item?.authorUsername || item?.username;
  if (username) {
    router.push(`/profile/${username}`);
  }
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
    await Promise.allSettled([fetchComments(), fetchLikeStatus(), fetchFollowStatus()]);
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
    if (post.value) {
      post.value.commentCount = countComments(comments.value);
    }
  } catch (e) {
    comments.value = [];
  }
};

const countComments = (list) => {
  return (list || []).reduce((sum, item) => sum + 1 + countComments(item.children || []), 0);
};

const fetchLikeStatus = async () => {
  if (!userStore.isLoggedIn) return;
  try {
    const data = await request.get("/api/like/status", {
      params: { targetId: postId, targetType: POST_LIKE_TARGET_TYPE },
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
    const data = await request.post("/api/like/toggle", {
      targetId: postId,
      targetType: POST_LIKE_TARGET_TYPE,
    });
    isLiked.value = !!data?.liked;
    likeCount.value = data?.count ?? Math.max(0, likeCount.value + (isLiked.value ? 1 : -1));
  } catch (e) {}
};

const fetchFollowStatus = async () => {
  if (!userStore.isLoggedIn || isSelf.value) return;
  const authorId = post.value?.authorId || post.value?.userId;
  if (!authorId) return;
  try {
    const data = await request.get(`/api/follow/status/${authorId}`);
    isFollowing.value = !!data;
  } catch (e) {}
};

const toggleFollow = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning("请先登录");
    return;
  }
  try {
    const authorId = post.value?.authorId || post.value?.userId;
    if (!authorId) {
      ElMessage.error("无法获取作者信息，请刷新后重试");
      return;
    }
    const data = await request.post(`/api/follow/${authorId}`);
    isFollowing.value = !!data?.followed;
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

const replyTargetName = (comment) => {
  return comment?.replyNickname || comment?.replyUsername || "";
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
      replyUserId: replyingTo.value?.userId,
    });
    replyingTo.value = null;
    replyContent.value = "";
    ElMessage.success("回复成功");
    await fetchComments();
  } catch (e) {}
};

const deleteComment = async (commentId) => {
  await ElMessageBox.confirm("确认删除该评论吗？", "提示", {
    type: "warning",
    confirmButtonText: "确认删除",
    cancelButtonText: "暂不删除",
  });
  try {
    await request.delete(`/api/comment/${commentId}`);
    ElMessage.success("删除成功");
    await fetchComments();
  } catch (e) {}
};

const deletePost = async () => {
  try {
    await ElMessageBox.confirm("确认删除这篇游记吗？", "删除游记", {
      type: "warning",
      confirmButtonText: "确认删除",
      cancelButtonText: "暂不删除",
    });
    await request.delete(`/api/post/${postId}`);
    ElMessage.success("删除成功");
    router.push("/community");
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
.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.fallback-avatar {
  background: linear-gradient(135deg, #0d9488, #0ea5e9);
  color: #fff;
  font-weight: 700;
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
.post-image-fallback {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
.reply-target {
  color: #71718B;
  margin-right: 6px;
}
.reply-input {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
}
</style>
