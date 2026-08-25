import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import NProgress from "nprogress";
import "nprogress/nprogress.css";

import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import "./styles/theme.css";
import "./styles/editorial.css";

const app = createApp(App);

app.use(router);
app.use(createPinia());
app.use(ElementPlus);

// NProgress 配置
NProgress.configure({
  showSpinner: false,
  speed: 400,
  minimum: 0.15,
  easing: "ease",
  trickleSpeed: 200,
});

// 路由加载进度条
router.beforeEach((to, from, next) => {
  if (from.name) {
    NProgress.start();
  }
  next();
});

router.afterEach(() => {
  NProgress.done();
});

router.onError(() => {
  NProgress.done();
});

app.mount("#app");
