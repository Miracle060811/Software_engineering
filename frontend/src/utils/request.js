import axios from "axios";
import { ElMessage } from "element-plus";

const AUTH_REDIRECT_FLAG = "travelmate-auth-redirecting";
const apiBaseURL = import.meta.env.VITE_API_BASE_URL?.trim() || "/";

const clearAuthState = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("userInfo");
};

const handleAuthExpired = () => {
  clearAuthState();
  if (window.location.pathname === "/login") {
    sessionStorage.removeItem(AUTH_REDIRECT_FLAG);
    return;
  }
  if (sessionStorage.getItem(AUTH_REDIRECT_FLAG) === "1") {
    return;
  }
  sessionStorage.setItem(AUTH_REDIRECT_FLAG, "1");
  ElMessage.error("登录状态已失效，请重新登录");
  window.location.replace("/login");
};

const request = axios.create({
  baseURL: apiBaseURL,
  timeout: 30000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers["Authorization"] = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    sessionStorage.removeItem(AUTH_REDIRECT_FLAG);
    const res = response.data;
    if (res && typeof res === "object" && "code" in res) {
      if (res.code === 200) {
        return res.data;
      }

      const businessError = new Error(res.msg || "请求失败");
      businessError.response = {
        status: response.status,
        data: res,
      };
      return Promise.reject(businessError);
    }

    return res;
  },
  (error) => {
    const status = error.response?.status;

    if (status === 401 || status === 403) {
      handleAuthExpired();
      return Promise.reject(error);
    }

    if (!error.response) {
      ElMessage.error("无法连接后端服务，请确认前后端都已启动");
      return Promise.reject(error);
    }

    sessionStorage.removeItem(AUTH_REDIRECT_FLAG);
    ElMessage.error(error.response.data?.msg || error.message || "请求失败");
    return Promise.reject(error);
  },
);

export default request;
