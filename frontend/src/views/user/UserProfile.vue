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
              <div class="stat-item">
                <span class="stat-num">{{ followingCount }}</span>
                <span class="stat-label">关注</span>
              </div>
              <div class="stat-item">
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
          <span class="section-title">Ta 的游记</span>
        </template>
        <el-empty v-if="posts.length === 0" description="暂无游记" />
        <el-row :gutter="16">
          <el-col
            :span="8"
            v-for="post in posts"
            :key="post.id"
            style="margin-bottom: 16px"
          >
            <el-card
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import { ElMessage } from "element-plus";
import { StarFilled } from "@element-plus/icons-vue";
import request from "@/utils/request";
import { useUserStore } from "@/stores/user";

const route = useRoute();
const userStore = useUserStore();
const username = route.params.username;

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

const editForm = ref({ nickname: "", avatar: "", bio: "" });
const pwdForm = ref({ oldPassword: "", newPassword: "", confirmPassword: "" });

const isSelf = computed(() => {
  return userStore.userInfo?.username === username;
});

const fetchProfile = async () => {
  loading.value = true;
  try {
    const data = await request.get(`/api/user/profile/${username}`);
    profile.value = data;
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
    const data = await request.get("/api/post/my");
    posts.value = Array.isArray(data) ? data : [];
  } catch (e) {
    posts.value = [];
  }
};

const fetchFollowInfo = async () => {
  try {
    const [fans, following] = await Promise.allSettled([
      request.get(`/api/follow/fans/${profile.value?.id}`),
      request.get(`/api/follow/following/${profile.value?.id}`),
    ]);
    fansCount.value =
      fans.status === "fulfilled" ? (fans.value?.length ?? 0) : 0;
    followingCount.value =
      following.status === "fulfilled" ? (following.value?.length ?? 0) : 0;
  } catch (e) {}
};

const toggleFollow = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning("请先登录");
    return;
  }
  try {
    await request.post(`/api/follow/${profile.value.id}`);
    isFollowing.value = !isFollowing.value;
    fansCount.value += isFollowing.value ? 1 : -1;
    ElMessage.success(isFollowing.value ? "关注成功" : "已取消关注");
  } catch (e) {}
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

const getFirstImage = (images) => {
  if (!images) return `https://picsum.photos/seed/post${Math.random()}/400/260`;
  const arr = typeof images === "string" ? images.split(",") : images;
  return arr[0]?.trim() || `https://picsum.photos/seed/community/400/260`;
};

onMounted(() => {
  fetchProfile();
});
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
.post-info {
  padding: 10px 12px;
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
</style>
