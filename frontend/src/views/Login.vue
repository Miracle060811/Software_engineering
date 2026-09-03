<template>
  <div class="login-page">
    <!-- 背景装饰 -->
    <div class="login-bg">
      <div class="bg-circle c1"></div>
      <div class="bg-circle c2"></div>
      <div class="bg-circle c3"></div>
    </div>

    <div class="login-box">
      <div class="login-brand">
        <span class="brand-icon"><el-icon :size="42"><Promotion /></el-icon></span>
        <h1 class="brand-name">伴游 TravelMate</h1>
        <p class="brand-slogan">探索世界，从这里开始</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-width="0"
            class="auth-form"
            @keyup.enter="handleLogin"
          >
            <el-form-item prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                size="large"
                :prefix-icon="User"
                class="auth-input"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="请输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                class="auth-input"
              />
            </el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="auth-btn"
              @click="handleLogin"
            >
              登 录
            </el-button>
            <div class="form-actions">
              <el-button link type="primary" @click="openResetDialog">
                忘记密码？
              </el-button>
            </div>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-width="0"
            class="auth-form"
          >
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名（4-20位字母数字）"
                size="large"
                :prefix-icon="User"
                class="auth-input"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码（至少6位）"
                size="large"
                :prefix-icon="Lock"
                show-password
                class="auth-input"
              />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                size="large"
                :prefix-icon="Lock"
                show-password
                class="auth-input"
              />
            </el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="auth-btn"
              @click="handleRegister"
            >
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <p class="login-footer">
        <el-button link type="primary" @click="$router.push('/')">← 返回首页</el-button>
      </p>
    </div>

    <el-dialog
      v-model="adminDialogVisible"
      title="管理员注册"
      width="420px"
      class="reset-dialog"
      :close-on-click-modal="false"
    >
      <el-form
        ref="adminFormRef"
        :model="adminForm"
        :rules="adminRules"
        label-width="0"
        class="auth-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="adminForm.username"
            placeholder="管理员用户名"
            size="large"
            :prefix-icon="User"
            class="auth-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="adminForm.password"
            type="password"
            placeholder="管理员密码（至少6位）"
            size="large"
            :prefix-icon="Lock"
            show-password
            class="auth-input"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="adminForm.confirmPassword"
            type="password"
            placeholder="请再次输入管理员密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            class="auth-input"
          />
        </el-form-item>
        <el-form-item prop="secret">
          <el-input
            v-model="adminForm.secret"
            type="password"
            placeholder="请输入管理员注册密钥"
            size="large"
            :prefix-icon="Lock"
            show-password
            class="auth-input"
            @keyup.enter="handleAdminRegister"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adminDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="adminLoading"
          @click="handleAdminRegister"
        >
          注册管理员
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resetDialogVisible"
      title="重置密码"
      width="420px"
      class="reset-dialog"
      :close-on-click-modal="false"
    >
      <el-form
        ref="resetFormRef"
        :model="resetForm"
        :rules="resetRules"
        label-width="0"
        class="auth-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="resetForm.username"
            placeholder="请输入用户名"
            size="large"
            :prefix-icon="User"
            class="auth-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="resetForm.password"
            type="password"
            placeholder="请输入新密码（至少6位）"
            size="large"
            :prefix-icon="Lock"
            show-password
            class="auth-input"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="resetForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            size="large"
            :prefix-icon="Lock"
            show-password
            class="auth-input"
            @keyup.enter="handleResetPassword"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="resetLoading"
          @click="handleResetPassword"
        >
          重置密码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { User, Lock, Promotion } from "@element-plus/icons-vue";
import { useUserStore } from "../stores/user";
import request from "../utils/request";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const activeTab = ref("login");
const loading = ref(false);
const adminLoading = ref(false);
const adminDialogVisible = ref(false);
const resetLoading = ref(false);
const resetDialogVisible = ref(false);
const blankRegisterClickCount = ref(0);

const loginFormRef = ref(null);
const registerFormRef = ref(null);
const adminFormRef = ref(null);
const resetFormRef = ref(null);

const loginForm = ref({ username: "", password: "" });
const registerForm = ref({ username: "", password: "", confirmPassword: "" });
const adminForm = ref({ username: "", password: "", confirmPassword: "", secret: "" });
const resetForm = ref({ username: "", password: "", confirmPassword: "" });

const loginRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

const registerRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 4, max: 20, message: "用户名长度4-20位", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码至少6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认密码", trigger: "blur" },
    {
      validator: (_rule, value, callback) => {
        if (value !== registerForm.value.password) {
          callback(new Error("两次密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
};

const adminRules = {
  username: [
    { required: true, message: "请输入管理员用户名", trigger: "blur" },
    { min: 4, max: 20, message: "用户名长度4-20位", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入管理员密码", trigger: "blur" },
    { min: 6, message: "密码至少6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认管理员密码", trigger: "blur" },
    {
      validator: (_rule, value, callback) => {
        if (value !== adminForm.value.password) {
          callback(new Error("两次密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
  secret: [{ required: true, message: "请输入管理员注册密钥", trigger: "blur" }],
};

const resetRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入新密码", trigger: "blur" },
    { min: 6, message: "密码至少6位", trigger: "blur" },
  ],
  confirmPassword: [
    { required: true, message: "请确认新密码", trigger: "blur" },
    {
      validator: (_rule, value, callback) => {
        if (value !== resetForm.value.password) {
          callback(new Error("两次密码不一致"));
        } else {
          callback();
        }
      },
      trigger: "blur",
    },
  ],
};

const openResetDialog = () => {
  resetForm.value = { username: "", password: "", confirmPassword: "" };
  resetDialogVisible.value = true;
};

const handleLogin = async () => {
  const valid = await loginFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await userStore.login(loginForm.value.username.trim(), loginForm.value.password);
    ElMessage({
      type: "success",
      message: "登录成功，欢迎回来！",
      duration: 1000,
    });
    const redirect = route.query.redirect;
    router.push(typeof redirect === "string" && redirect.startsWith("/") ? redirect : "/");
  } catch (e) {
    // error handled in request.js
  } finally {
    loading.value = false;
  }
};

const handleRegister = async () => {
  const isBlankRegisterForm = !registerForm.value.username
    && !registerForm.value.password
    && !registerForm.value.confirmPassword;
  if (isBlankRegisterForm) {
    await registerFormRef.value?.validate().catch(() => false);
    blankRegisterClickCount.value += 1;
    if (blankRegisterClickCount.value >= 3) {
      blankRegisterClickCount.value = 0;
      adminForm.value = { username: "", password: "", confirmPassword: "", secret: "" };
      adminDialogVisible.value = true;
    }
    return;
  }
  blankRegisterClickCount.value = 0;
  const valid = await registerFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await userStore.register(
      registerForm.value.username.trim(),
      registerForm.value.password,
    );
    ElMessage.success("注册成功！请登录");
    activeTab.value = "login";
    loginForm.value.username = registerForm.value.username.trim();
    registerForm.value = { username: "", password: "", confirmPassword: "" };
  } catch (e) {
    // error handled in request.js
  } finally {
    loading.value = false;
  }
};

const handleAdminRegister = async () => {
  const valid = await adminFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  adminLoading.value = true;
  try {
    await request.post("/user/admin-register", {
      username: adminForm.value.username,
      password: adminForm.value.password,
      secret: adminForm.value.secret,
    });
    ElMessage.success("管理员注册成功，请登录");
    loginForm.value.username = adminForm.value.username;
    loginForm.value.password = "";
    adminDialogVisible.value = false;
    activeTab.value = "login";
  } catch (e) {
    // error handled in request.js
  } finally {
    adminLoading.value = false;
  }
};

const handleResetPassword = async () => {
  const valid = await resetFormRef.value?.validate().catch(() => false);
  if (!valid) return;
  resetLoading.value = true;
  try {
    await request.post("/user/reset-password", null, {
      params: {
        username: resetForm.value.username.trim(),
        newPassword: resetForm.value.password,
      },
    });
    ElMessage.success("密码重置成功，请使用新密码登录");
    resetDialogVisible.value = false;
    resetForm.value = { username: "", password: "", confirmPassword: "" };
    activeTab.value = "login";
  } catch (e) {
    // error handled in request.js
  } finally {
    resetLoading.value = false;
  }
};

</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(160deg, #ECFDF5 0%, #EEF2FF 40%, #F5F3FF 100%);
}

/* 背景装饰圆 */
.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.c1 {
  width: 500px;
  height: 500px;
  background: rgba(13, 148, 136, 0.15);
  top: -15%;
  right: -10%;
}

.c2 {
  width: 400px;
  height: 400px;
  background: rgba(99, 102, 241, 0.12);
  bottom: -10%;
  left: -8%;
}

.c3 {
  width: 300px;
  height: 300px;
  background: rgba(245, 158, 11, 0.1);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

/* 登录卡片 */
.login-box {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border-radius: 24px;
  padding: 44px 42px 36px;
  width: 430px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 20px 60px rgba(13, 148, 136, 0.08);
}

.login-brand {
  text-align: center;
  margin-bottom: 32px;
}

.brand-icon {
  display: block;
  margin-bottom: 12px;
  color: var(--el-color-primary);
  animation: float 3s ease-in-out infinite;
}

.brand-name {
  font-size: 26px;
  font-weight: 800;
  color: #1E293B;
  margin-bottom: 6px;
  letter-spacing: 0.5px;
}

.brand-slogan {
  font-size: 14px;
  color: #94A3B8;
  font-weight: 400;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 28px;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #F1F5F9;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 600;
  padding: 0 28px;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.auth-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.25s ease;
}

.auth-input :deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.12);
}

.auth-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: 14px;
  margin-top: 8px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  min-height: 28px;
  margin-top: 4px;
}

.reset-dialog :deep(.el-dialog) {
  border-radius: 16px;
}

.login-footer {
  text-align: center;
  margin-top: 20px;
}
</style>
