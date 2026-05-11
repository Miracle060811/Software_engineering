import { defineStore } from "pinia";
import { ref, computed } from "vue";
import request from "../utils/request";

export const useUserStore = defineStore("user", () => {
  const token = ref(localStorage.getItem("token") || "");
  const userInfo = ref(JSON.parse(localStorage.getItem("userInfo") || "null"));

  async function login(username, password) {
    const res = await request.post("/user/login", null, {
      params: { username, password },
    });
    token.value = res;
    localStorage.setItem("token", res);
    await fetchUserInfo();
  }

  async function register(username, password, role = 0) {
    await request.post("/user/register", null, {
      params: { username, password, role },
    });
  }

  async function fetchUserInfo() {
    try {
      const res = await request.get("/user/me");
      userInfo.value = res;
      localStorage.setItem("userInfo", JSON.stringify(res));
    } catch (e) {
      console.error("获取用户信息失败", e);
    }
  }

  function logout() {
    token.value = "";
    userInfo.value = null;
    localStorage.removeItem("token");
    localStorage.removeItem("userInfo");
  }

  const isLoggedIn = computed(() => !!token.value);

  return {
    token,
    userInfo,
    login,
    register,
    logout,
    fetchUserInfo,
    isLoggedIn,
  };
});
