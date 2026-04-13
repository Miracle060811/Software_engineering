import axios from "axios";
import { ElMessage } from "element-plus";

const request = axios.create({
  baseURL: "/api",
  timeout: 50000,
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 这里以后可以加上 token: config.headers['Authorization'] = `Bearer ${token}`
    return config;
  },
  (error) => {
    return Promise.reject(error);
  },
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code === 200) {
      return res.data;
    } else {
      ElMessage.error(res.msg || "系统异常");
      return Promise.reject(new Error(res.msg || "Error"));
    }
  },
  (error) => {
    ElMessage.error("网络请求失败");
    return Promise.reject(error);
  },
);

export default request;
