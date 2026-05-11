import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/",
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
    const res = response.data;
    if (res.code === 200) {
      return res.data;
    } else {
      ElMessage.error(res.msg || "请求失败");
      return Promise.reject(new Error(res.msg));
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem("token");
      localStorage.removeItem("userInfo");
      window.location.href = "/login";
    }
    ElMessage.error(error.response?.data?.msg || "网络请求失败");
    return Promise.reject(error);
  },
);

export default request;
