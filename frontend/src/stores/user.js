import { defineStore } from "pinia";
import { ref, computed } from "vue";
import request, { ensureAccessToken } from "../utils/request";
import { clearAccessToken, setAccessToken } from "../utils/authToken";

export const useUserStore = defineStore("user", () => {
  const token = ref("");
  const userInfo = ref(null);
  let restorePromise = null;

  async function login(username, password) {
    const res = await request.post("/user/login", null, {
      params: { username: username.trim(), password },
    });
    token.value = res;
    setAccessToken(res);
    try {
      await fetchUserInfo();
    } catch (e) {
      logout();
      throw e;
    }
  }

  async function register(username, password) {
    await request.post("/user/register", null, {
      params: { username: username.trim(), password },
    });
  }

  async function fetchUserInfo() {
    try {
      const res = await request.get("/user/me");
      userInfo.value = res;
    } catch (e) {
      console.error("获取用户信息失败", e);
      throw e;
    }
  }

  async function restoreSession() {
    if (token.value && userInfo.value) return true;
    if (!restorePromise) {
      restorePromise = (async () => {
        const restoredToken = await ensureAccessToken();
        token.value = restoredToken;
        await fetchUserInfo();
        return true;
      })().finally(() => {
        restorePromise = null;
      });
    }
    return restorePromise;
  }

  function logout() {
    const logoutRequest = request.post("/user/logout", null, {
      silent: true,
      skipErrorMessage: true,
    }).catch(() => undefined);
    token.value = "";
    userInfo.value = null;
    clearAccessToken();
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
    return logoutRequest;
  }

  const isLoggedIn = computed(() => !!token.value);

  return {
    token,
    userInfo,
    login,
    register,
    logout,
    fetchUserInfo,
    restoreSession,
    isLoggedIn,
  };
});
