import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath } from "node:url";

const backendMode = process.env.VITE_BACKEND_MODE || "monolith";
const monolithTarget = process.env.VITE_MONOLITH_URL || "http://localhost:8080";

const proxyTarget = (target) => ({ target, changeOrigin: true });

const microserviceProxy = {
  "/api/admin": proxyTarget(process.env.OPS_SERVICE_URL || "http://localhost:8086"),
  "/api/post": proxyTarget(process.env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
  "/api/comment": proxyTarget(process.env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
  "/api/like": proxyTarget(process.env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
  "/api/file": proxyTarget(process.env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
  "/uploads": proxyTarget(process.env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
  "/api/ai": proxyTarget(process.env.AI_SERVICE_URL || "http://localhost:8084"),
  "/api/notification": proxyTarget(process.env.AI_SERVICE_URL || "http://localhost:8084"),
  "/api/notifications": proxyTarget(process.env.AI_SERVICE_URL || "http://localhost:8084"),
  "/api/private-message": proxyTarget(process.env.AI_SERVICE_URL || "http://localhost:8084"),
  "/api/hotel": proxyTarget(process.env.LOCAL_SERVICE_URL || "http://localhost:8083"),
  "/api/attraction": proxyTarget(process.env.LOCAL_SERVICE_URL || "http://localhost:8083"),
  "/api/destinations": proxyTarget(process.env.LOCAL_SERVICE_URL || "http://localhost:8083"),
  "/api/tour": proxyTarget(process.env.LOCAL_SERVICE_URL || "http://localhost:8083"),
  "/api/review": proxyTarget(process.env.LOCAL_SERVICE_URL || "http://localhost:8083"),
  "/api/reply": proxyTarget(process.env.LOCAL_SERVICE_URL || "http://localhost:8083"),
  "/api/coupon": proxyTarget(process.env.LOCAL_SERVICE_URL || "http://localhost:8083"),
  "/api/flight": proxyTarget(process.env.TRAFFIC_SERVICE_URL || "http://localhost:8082"),
  "/api/train": proxyTarget(process.env.TRAFFIC_SERVICE_URL || "http://localhost:8082"),
  "/api/order": proxyTarget(process.env.TRAFFIC_SERVICE_URL || "http://localhost:8082"),
  "/api/price": proxyTarget(process.env.TRAFFIC_SERVICE_URL || "http://localhost:8082"),
  "/api/passenger": proxyTarget(process.env.IDENTITY_SERVICE_URL || "http://localhost:8081"),
  "/api/follow": proxyTarget(process.env.IDENTITY_SERVICE_URL || "http://localhost:8081"),
  "/api/user": proxyTarget(process.env.IDENTITY_SERVICE_URL || "http://localhost:8081"),
  "/user": proxyTarget(process.env.IDENTITY_SERVICE_URL || "http://localhost:8081"),
};

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    port: Number(process.env.VITE_DEV_PORT || 3000),
    open: backendMode !== "microservices",
    proxy: backendMode === "microservices"
      ? microserviceProxy
      : {
          "/api": proxyTarget(monolithTarget),
          "/user": proxyTarget(monolithTarget),
          "/uploads": proxyTarget(monolithTarget),
        },
  },
  test: {
    environment: "jsdom",
    globals: true,
    include: ["tests/unit/**/*.test.js"],
  },
});
