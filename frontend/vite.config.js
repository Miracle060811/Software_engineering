import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
import { fileURLToPath } from "node:url";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const backendMode = env.VITE_BACKEND_MODE?.trim() || "monolith";
  const monolithTarget = env.VITE_DEV_BACKEND_URL?.trim()
    || env.VITE_MONOLITH_URL?.trim()
    || "http://localhost:8080";
  const proxyTarget = (target) => ({ target, changeOrigin: true });

  const microserviceProxy = {
    "/api/admin": proxyTarget(env.OPS_SERVICE_URL || "http://localhost:8086"),
    "/api/post": proxyTarget(env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
    "/api/comment": proxyTarget(env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
    "/api/like": proxyTarget(env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
    "/api/file": proxyTarget(env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
    "/uploads": proxyTarget(env.COMMUNITY_SERVICE_URL || "http://localhost:8085"),
    "/api/ai": proxyTarget(env.AI_SERVICE_URL || "http://localhost:8084"),
    "/api/notification": proxyTarget(env.AI_SERVICE_URL || "http://localhost:8084"),
    "/api/notifications": proxyTarget(env.AI_SERVICE_URL || "http://localhost:8084"),
    "/api/private-message": proxyTarget(env.AI_SERVICE_URL || "http://localhost:8084"),
    "/api/hotel": proxyTarget(env.LOCAL_SERVICE_URL || "http://localhost:8083"),
    "/api/attraction": proxyTarget(env.LOCAL_SERVICE_URL || "http://localhost:8083"),
    "/api/destinations": proxyTarget(env.LOCAL_SERVICE_URL || "http://localhost:8083"),
    "/api/tour": proxyTarget(env.LOCAL_SERVICE_URL || "http://localhost:8083"),
    "/api/review": proxyTarget(env.LOCAL_SERVICE_URL || "http://localhost:8083"),
    "/api/reply": proxyTarget(env.LOCAL_SERVICE_URL || "http://localhost:8083"),
    "/api/coupon": proxyTarget(env.LOCAL_SERVICE_URL || "http://localhost:8083"),
    "/api/flight": proxyTarget(env.TRAFFIC_SERVICE_URL || "http://localhost:8082"),
    "/api/train": proxyTarget(env.TRAFFIC_SERVICE_URL || "http://localhost:8082"),
    "/api/order": proxyTarget(env.TRAFFIC_SERVICE_URL || "http://localhost:8082"),
    "/api/price": proxyTarget(env.TRAFFIC_SERVICE_URL || "http://localhost:8082"),
    "/api/passenger": proxyTarget(env.IDENTITY_SERVICE_URL || "http://localhost:8081"),
    "/api/follow": proxyTarget(env.IDENTITY_SERVICE_URL || "http://localhost:8081"),
    "/api/user": proxyTarget(env.IDENTITY_SERVICE_URL || "http://localhost:8081"),
    "/user": proxyTarget(env.IDENTITY_SERVICE_URL || "http://localhost:8081"),
  };

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        "@": fileURLToPath(new URL("./src", import.meta.url)),
      },
    },
    server: {
      port: Number(env.VITE_DEV_PORT || 3000),
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
  };
});
