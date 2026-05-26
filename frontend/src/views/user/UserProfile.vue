<template>
  <div class="user-profile-page" v-loading="loading">
    <div v-if="profile">
      <!-- 用户信息区 -->
      <el-card class="profile-card">
        <div class="profile-header">
          <el-avatar :size="80" :src="profile.avatar" class="profile-avatar">
            {{ profile.nickname?.[0] || profile.username?.[0] }}
          </el-avatar>
          <div class="profile-info">
            <h2 class="profile-name">
              {{ profile.nickname || profile.username }}
            </h2>
            <div class="profile-bio">
              {{ profile.bio || "这位用户很懒，什么都没写~" }}
            </div>
            <div class="profile-stats">
              <div class="stat-item stat-clickable" @click="openFollowDialog('following')">
                <span class="stat-num">{{ followingCount }}</span>
                <span class="stat-label">关注</span>
              </div>
              <div class="stat-item stat-clickable" @click="openFollowDialog('fans')">
                <span class="stat-num">{{ fansCount }}</span>
                <span class="stat-label">粉丝</span>
              </div>
              <div class="stat-item">
                <span class="stat-num">{{ posts.length }}</span>
                <span class="stat-label">游记</span>
              </div>
            </div>
          </div>
          <div class="profile-actions">
            <template v-if="isSelf">
              <el-button type="primary" @click="editDialogVisible = true"
                >编辑资料</el-button
              >
              <el-button @click="pwdDialogVisible = true">修改密码</el-button>
              <el-button type="danger" :loading="deletingAccount" @click="deleteAccount"
                >注销账户</el-button
              >
            </template>
            <template v-else>
              <el-button
                :type="isFollowing ? '' : 'primary'"
                @click="toggleFollow"
              >
                {{ isFollowing ? "已关注" : "+ 关注" }}
              </el-button>
            </template>
          </div>
        </div>
      </el-card>

      <!-- 游记列表 -->
      <el-card class="posts-card">
        <template #header>
          <div class="posts-header">
            <span class="section-title">{{ isSelf ? "我的游记" : "Ta 的游记" }}</span>
            <el-radio-group v-if="isSelf" v-model="postStatusFilter" size="small">
              <el-radio-button label="all">全部</el-radio-button>
              <el-radio-button label="draft">草稿</el-radio-button>
              <el-radio-button label="pending">AI审核中</el-radio-button>
              <el-radio-button label="published">已发布</el-radio-button>
              <el-radio-button label="rejected">已拒绝</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <el-alert
          v-if="isSelf"
          class="audit-alert"
          type="info"
          show-icon
          :closable="false"
          title="普通发布会进入 AI审核中 状态；审核通过后才会出现在社区推荐流。草稿只对自己可见。"
        />
        <el-empty v-if="visiblePosts.length === 0" description="暂无游记" />
        <el-row :gutter="16">
          <el-col
            :span="8"
            v-for="post in visiblePosts"
            :key="post.id"
            style="margin-bottom: 16px"
          >
            <el-card
              :class="['post-card', { 'text-only-card': !hasImages(post.images) }]"
              :body-style="{ padding: 0 }"
              @click="openPost(post)"
            >
              <SafeImage
                v-if="hasImages(post.images)"
                :src="getFirstImage(post.images)"
                image-class="post-cover"
                :alt="post.title"
              />
              <div class="post-info">
                <div v-if="isSelf" class="post-status-row">
                  <el-tag size="small" :type="statusType(post.status)">
                    {{ statusLabel(post.status) }}
                  </el-tag>
                  <div class="post-actions" @click.stop>
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
                <div v-if="post.status === 2 && post.rejectReason" class="reject-reason">
                  未通过原因：{{ post.rejectReason }}
                </div>
                <div class="post-likes"><el-icon><StarFilled /></el-icon> {{ post.likeCount || 0 }}</div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-card>
    </div>

    <!-- 编辑资料 Dialog -->
    <el-dialog v-model="editDialogVisible" title="编辑个人资料" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="editForm.avatar" placeholder="请输入头像图片URL" />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input
            v-model="editForm.bio"
            type="textarea"
            :rows="3"
            placeholder="介绍一下自己..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile"
          >保存</el-button
        >
      </template>
    </el-dialog>

    <!-- 修改密码 Dialog -->
    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="420px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="changingPwd" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="followDialogVisible" :title="followDialogTitle" width="420px">
      <el-empty v-if="followDialogUsers.length === 0" description="暂无用户" />
      <div v-else class="follow-list">
        <div
          v-for="user in followDialogUsers"
          :key="user.userId"
          class="follow-user"
          @click="goProfile(user)"
        >
          <el-avatar :size="40" :src="user.avatar || ''" class="follow-avatar">
            {{ (user.nickname || user.username || "旅").charAt(0).toUpperCase() }}
          </el-avatar>
          <div>
            <div class="follow-name">{{ user.nickname || user.username }}</div>
            <div class="follow-username">@{{ user.username }}</div>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { StarFilled } from "@element-plus/icons-vue";
import request from "@/utils/request";
import { useUserStore } from "@/stores/user";
import SafeImage from "@/components/SafeImage.vue";
import { parseImageList } from "@/utils/image";

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const profile = ref(null);
const posts = ref([]);
const loading = ref(false);
const fansCount = ref(0);
const followingCount = ref(0);
const isFollowing = ref(false);
const editDialogVisible = ref(false);
const pwdDialogVisible = ref(false);
const saving = ref(false);
const changingPwd = ref(false);
const deletingAccount = ref(false);
const followDialogVisible = ref(false);
const followDialogType = ref("fans");
const followDialogUsers = ref([]);
const postStatusFilter = ref("all");

const editForm = ref({ nickname: "", avatar: "", bio: "" });
const pwdForm = ref({ oldPassword: "", newPassword: "", confirmPassword: "" });

const isSelf = computed(() => {
  return userStore.userInfo?.username === route.params.username;
});

const profileUserId = computed(() => profile.value?.userId || profile.value?.id);

const followDialogTitle = computed(() => (followDialogType.value === "fans" ? "粉丝" : "关注"));

const visiblePosts = computed(() => {
  if (!isSelf.value || postStatusFilter.value === "all") return posts.value;
  const statusMap = { draft: 3, pending: 0, published: 1, rejected: 2 };
  return posts.value.filter((post) => post.status === statusMap[postStatusFilter.value]);
});

const fetchProfile = async () => {
  loading.value = true;
  try {
    const data = await request.get(`/api/user/profile/${route.params.username}`);
    profile.value = data;
    followingCount.value = data.followingCount || 0;
    fansCount.value = data.fansCount || 0;
    editForm.value = {
      nickname: data.nickname || "",
      avatar: data.avatar || "",
      bio: data.bio || "",
    };
    await Promise.allSettled([fetchPosts(), fetchFollowInfo()]);
  } catch (e) {
  } finally {
    loading.value = false;
  }
};

const fetchPosts = async () => {
  try {
    const data = isSelf.value
      ? await request.get("/api/post/my")
      : await request.get(`/api/user/profile/${route.params.username}/posts`, { silent: true });
    posts.value = Array.isArray(data) ? data : [];
  } catch (e) {
    if (isSelf.value) {
      posts.value = [];
      return;
    }
    try {
      const data = await request.get(`/api/user/profile/${route.params.username}/posts`);
      posts.value = Array.isArray(data) ? data : [];
    } catch (err) {
      posts.value = [];
    }
  }
};

const fetchFollowInfo = async () => {
  if (!profileUserId.value) {
    return;
  }
  try {
    const requests = [
      request.get(`/api/follow/fans/${profileUserId.value}`),
      request.get(`/api/follow/following/${profileUserId.value}`),
    ];
    if (userStore.isLoggedIn && !isSelf.value) {
      requests.push(request.get(`/api/follow/status/${profileUserId.value}`));
    }
    const [fans, following, status] = await Promise.allSettled(requests);
    fansCount.value =
      fans.status === "fulfilled" ? (fans.value?.length ?? 0) : fansCount.value;
    followingCount.value =
      following.status === "fulfilled"
        ? (following.value?.length ?? 0)
        : followingCount.value;
    if (status) {
      isFollowing.value = status.status === "fulfilled" ? !!status.value : false;
    }
  } catch (e) {}
};

const toggleFollow = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning("请先登录");
    return;
  }
  if (!profileUserId.value) {
    ElMessage.error("无法获取用户信息，请刷新后重试");
    return;
  }
  try {
    const data = await request.post(`/api/follow/${profileUserId.value}`);
    isFollowing.value = data?.followed ?? !isFollowing.value;
    ElMessage.success(isFollowing.value ? "关注成功" : "已取消关注");
    await fetchFollowInfo();
  } catch (e) {}
};

const openFollowDialog = async (type) => {
  if (!profileUserId.value) return;
  followDialogType.value = type;
  followDialogVisible.value = true;
  try {
    const url = type === "fans"
      ? `/api/follow/fans/${profileUserId.value}`
      : `/api/follow/following/${profileUserId.value}`;
    const data = await request.get(url);
    followDialogUsers.value = Array.isArray(data) ? data : [];
  } catch (e) {
    followDialogUsers.value = [];
  }
};

const goProfile = (user) => {
  if (!user?.username) return;
  followDialogVisible.value = false;
  router.push(`/profile/${user.username}`);
};

const saveProfile = async () => {
  saving.value = true;
  try {
    await request.put("/api/user/profile/update", editForm.value);
    ElMessage.success("资料更新成功");
    editDialogVisible.value = false;
    await userStore.fetchUserInfo();
    await fetchProfile();
  } catch (e) {
  } finally {
    saving.value = false;
  }
};

const changePassword = async () => {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.warning("请填写旧密码和新密码");
    return;
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.warning("两次输入的新密码不一致");
    return;
  }
  if (pwdForm.value.newPassword.length < 6) {
    ElMessage.warning("新密码长度至少6位");
    return;
  }
  changingPwd.value = true;
  try {
    await request.post("/user/password", null, {
      params: {
        oldPassword: pwdForm.value.oldPassword,
        newPassword: pwdForm.value.newPassword,
      },
    });
    ElMessage.success("密码修改成功，请重新登录");
    pwdDialogVisible.value = false;
    pwdForm.value = { oldPassword: "", newPassword: "", confirmPassword: "" };
    userStore.logout();
  } catch (e) {
  } finally {
    changingPwd.value = false;
  }
};

const deleteAccount = async () => {
  try {
    const { value } = await ElMessageBox.prompt(
      "账户注销后将无法登录，历史订单和游记会保留用于记录展示。请输入当前密码确认注销。",
      "注销账户",
      {
        type: "warning",
        inputType: "password",
        inputPlaceholder: "当前密码",
        confirmButtonText: "确认注销",
        cancelButtonText: "暂不注销",
        inputValidator: (value) => !!value || "请输入当前密码",
      },
    );
    deletingAccount.value = true;
    await request.delete("/user/account", {
      params: { password: value },
    });
    ElMessage.success("账户已注销");
    userStore.logout();
    router.push("/login");
  } catch (e) {
  } finally {
    deletingAccount.value = false;
  }
};

const getFirstImage = (images) => {
  return parseImageList(images)[0] || "";
};

const hasImages = (images) => !!getFirstImage(images);

const statusLabel = (status) =>
  ({ 0: "AI审核中", 1: "已发布", 2: "已拒绝", 3: "草稿" })[status] || `状态${status}`;

const statusType = (status) =>
  status === 1 ? "success" : status === 2 ? "danger" : status === 3 ? "info" : "warning";

const openPost = (post) => {
  if (isSelf.value && post.status === 3) {
    editDraft(post);
    return;
  }
  router.push(`/post/${post.id}`);
};

const editDraft = (post) => {
  router.push(`/post/create?draftId=${post.id}`);
};

const deletePost = async (post) => {
  try {
    await ElMessageBox.confirm(`确认删除「${post.title}」吗？`, "删除游记", {
      type: "warning",
      confirmButtonText: "确认删除",
      cancelButtonText: "暂不删除",
    });
    await request.delete(`/api/post/${post.id}`);
    ElMessage.success("删除成功");
    posts.value = posts.value.filter((item) => item.id !== post.id);
  } catch (e) {}
};

watch(
  () => route.params.username,
  () => {
    profile.value = null;
    posts.value = [];
    isFollowing.value = false;
    followDialogVisible.value = false;
    fetchProfile();
  },
);

onMounted(fetchProfile);
</script>

<style scoped>
.user-profile-page {
  max-width: 1100px;
  margin: 0 auto;
}
.profile-card {
  margin-bottom: 20px;
  border-radius: 12px;
}
.profile-header {
  display: flex;
  align-items: flex-start;
  gap: 24px;
}
.profile-avatar {
  flex-shrink: 0;
  font-size: 32px;
  background: #409eff;
  color: #fff;
}
.profile-info {
  flex: 1;
}
.profile-name {
  font-size: 22px;
  font-weight: 700;
  color: #222;
  margin-bottom: 8px;
}
.profile-bio {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
}
.profile-stats {
  display: flex;
  gap: 32px;
}
.stat-item {
  text-align: center;
}
.stat-clickable {
  cursor: pointer;
  border-radius: 8px;
  padding: 4px 8px;
  transition: background 0.2s;
}
.stat-clickable:hover {
  background: #F3F4F6;
}
.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: var(--el-text-color-primary);
  display: block;
}
.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.posts-card {
  border-radius: 12px;
}
.posts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.audit-alert {
  margin-bottom: 16px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
}
.post-card {
  cursor: pointer;
  border-radius: 10px;
  overflow: hidden;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.post-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}
.post-cover {
  width: 100%;
  height: 150px;
  object-fit: cover;
}
.text-only-card .post-info {
  min-height: 128px;
}
.post-info {
  padding: 10px 12px;
}
.post-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.post-actions {
  display: flex;
  gap: 6px;
}
.post-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.post-likes {
  font-size: 12px;
  color: #EF4444;
}
.post-excerpt {
  font-size: 13px;
  color: #71718B;
  line-height: 1.7;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.reject-reason {
  font-size: 12px;
  color: #B91C1C;
  background: #FEF2F2;
  border: 1px solid #FECACA;
  border-radius: 8px;
  padding: 7px 9px;
  line-height: 1.5;
  margin: 8px 0;
}
.follow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.follow-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
}
.follow-user:hover {
  background: #F8FAFC;
}
.follow-avatar {
  background: linear-gradient(135deg, #0d9488, #0ea5e9);
  color: #fff;
  font-weight: 700;
}
.follow-name {
  font-weight: 600;
  color: #1A1A2E;
}
.follow-username {
  font-size: 12px;
  color: #71718B;
}
</style>
