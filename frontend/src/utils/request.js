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

const shouldNotify = (config) => !config?.silent && !config?.skipErrorMessage;

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

      if (shouldNotify(response.config)) {
        ElMessage.error(res.msg || "请求失败");
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
      if (shouldNotify(error.config)) {
        const timedOut = error.code === "ECONNABORTED" || /timeout/i.test(error.message || "");
        const requestUrl = error.config?.url || "";
        const timeoutMessage = requestUrl.includes("/api/train")
          ? "查询超时，12306 当前响应较慢，请稍后重试"
          : requestUrl.includes("/api/ai")
            ? "AI 服务响应较慢，请稍后重试"
            : "请求超时，请稍后重试";
        ElMessage.error(
          timedOut
            ? timeoutMessage
            : "无法连接后端服务，请确认前后端都已启动",
        );
      }
      return Promise.reject(error);
    }

    sessionStorage.removeItem(AUTH_REDIRECT_FLAG);
    if (shouldNotify(error.config)) {
      ElMessage.error(error.response.data?.msg || error.message || "请求失败");
    }
    return Promise.reject(error);
  },
);

export default request;
