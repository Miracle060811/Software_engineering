import { createRouter, createWebHistory } from "vue-router";

const routes = [
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/Login.vue"),
  },
  {
    path: "/",
    name: "Home",
    component: () => import("../views/Home.vue"),
  },
  {
    path: "/destinations",
    name: "DestinationList",
    component: () => import("../views/destination/DestinationList.vue"),
  },
  {
    path: "/destination/:slug",
    name: "DestinationDetail",
    component: () => import("../views/destination/DestinationDetail.vue"),
  },
  {
    path: "/about",
    name: "About",
    component: () => import("../views/info/InfoPage.vue"),
    meta: { pageKey: "about" },
  },
  {
    path: "/terms",
    name: "Terms",
    component: () => import("../views/info/InfoPage.vue"),
    meta: { pageKey: "terms" },
  },
  {
    path: "/privacy",
    name: "Privacy",
    component: () => import("../views/info/InfoPage.vue"),
    meta: { pageKey: "privacy" },
  },
  {
    path: "/help",
    name: "Help",
    component: () => import("../views/info/InfoPage.vue"),
    meta: { pageKey: "help" },
  },
  {
    path: "/flight-search",
    name: "FlightSearch",
    component: () => import("../views/flight/FlightSearch.vue"),
  },
  {
    path: "/train-search",
    name: "TrainSearch",
    component: () => import("../views/train/TrainSearch.vue"),
  },
  {
    path: "/hotel-search",
    name: "HotelSearch",
    component: () => import("../views/hotel/HotelSearch.vue"),
  },
  {
    path: "/hotel/:id",
    name: "HotelDetail",
    component: () => import("../views/hotel/HotelDetail.vue"),
  },
  {
    path: "/attractions",
    name: "AttractionList",
    component: () => import("../views/hotel/AttractionList.vue"),
  },
  {
    path: "/ai-plan",
    name: "AiPlan",
    component: () => import("../views/ai/AiPlan.vue"),
  },
  {
    path: "/community",
    name: "Community",
    component: () => import("../views/community/Community.vue"),
  },
  {
    path: "/post/create",
    name: "PostCreate",
    component: () => import("../views/community/PostCreate.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/post/:id",
    name: "PostDetail",
    component: () => import("../views/community/PostDetail.vue"),
  },
  {
    path: "/my-orders",
    name: "MyOrders",
    component: () => import("../views/order/MyOrders.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/coupons",
    name: "CouponCenter",
    component: () => import("../views/order/CouponCenter.vue"),
  },
  {
    path: "/notifications",
    name: "NotificationCenter",
    component: () => import("../views/user/NotificationCenter.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/profile/:username",
    name: "UserProfile",
    component: () => import("../views/user/UserProfile.vue"),
  },
  {
    path: "/admin",
    name: "AdminDashboard",
    component: () => import("../views/admin/AdminDashboard.vue"),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: () => import("../views/NotFound.vue"),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem("token");
  if (to.meta.requiresAuth && !token) {
    next("/login");
  } else if (to.meta.requiresAdmin) {
    const userInfo = JSON.parse(localStorage.getItem("userInfo") || "null");
    if (!userInfo || userInfo.role !== 1) {
      next("/");
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
