import { describe, it, expect, vi, beforeEach } from "vitest";

const stub = { template: "<div></div>" };

vi.mock("@/views/Login.vue", () => ({ default: stub }));
vi.mock("@/views/Home.vue", () => ({ default: stub }));
vi.mock("@/views/NotFound.vue", () => ({ default: stub }));
vi.mock("@/views/destination/DestinationList.vue", () => ({ default: stub }));
vi.mock("@/views/destination/DestinationDetail.vue", () => ({ default: stub }));
vi.mock("@/views/info/InfoPage.vue", () => ({ default: stub }));
vi.mock("@/views/flight/FlightSearch.vue", () => ({ default: stub }));
vi.mock("@/views/train/TrainSearch.vue", () => ({ default: stub }));
vi.mock("@/views/hotel/HotelSearch.vue", () => ({ default: stub }));
vi.mock("@/views/hotel/HotelDetail.vue", () => ({ default: stub }));
vi.mock("@/views/hotel/AttractionList.vue", () => ({ default: stub }));
vi.mock("@/views/ai/AiPlan.vue", () => ({ default: stub }));
vi.mock("@/views/community/Community.vue", () => ({ default: stub }));
vi.mock("@/views/community/PostCreate.vue", () => ({ default: stub }));
vi.mock("@/views/community/PostDetail.vue", () => ({ default: stub }));
vi.mock("@/views/order/MyOrders.vue", () => ({ default: stub }));
vi.mock("@/views/order/CouponCenter.vue", () => ({ default: stub }));
vi.mock("@/views/user/NotificationCenter.vue", () => ({ default: stub }));
vi.mock("@/views/user/PrivateMessages.vue", () => ({ default: stub }));
vi.mock("@/views/user/MyCollections.vue", () => ({ default: stub }));
vi.mock("@/views/user/UserProfile.vue", () => ({ default: stub }));
vi.mock("@/views/admin/AdminDashboard.vue", () => ({ default: stub }));

import router from "@/router/index";

describe("router/index.js", () => {
  beforeEach(async () => {
    localStorage.clear();
    sessionStorage.clear();
    await router.replace("/");
    await router.isReady();
  });

  describe("route definitions", () => {
    it("has all expected routes", () => {
      const routeNames = router.getRoutes().map((r) => r.name);
      expect(routeNames).toContain("Login");
      expect(routeNames).toContain("Home");
      expect(routeNames).toContain("FlightSearch");
      expect(routeNames).toContain("TrainSearch");
      expect(routeNames).toContain("HotelSearch");
      expect(routeNames).toContain("MyOrders");
      expect(routeNames).toContain("CouponCenter");
      expect(routeNames).toContain("AiPlan");
      expect(routeNames).toContain("Community");
      expect(routeNames).toContain("AdminDashboard");
      expect(routeNames).toContain("NotFound");
    });

    it("marks auth-required routes correctly", () => {
      const routes = router.getRoutes();
      const authRoutes = routes.filter((r) => r.meta?.requiresAuth);
      const authNames = authRoutes.map((r) => r.name);

      expect(authNames).toContain("MyOrders");
      expect(authNames).toContain("PostCreate");
      expect(authNames).toContain("NotificationCenter");
      expect(authNames).toContain("PrivateMessages");
      expect(authNames).toContain("AdminDashboard");
    });

    it("marks admin routes correctly", () => {
      const routes = router.getRoutes();
      const adminRoutes = routes.filter((r) => r.meta?.requiresAdmin);
      const adminNames = adminRoutes.map((r) => r.name);

      expect(adminNames).toContain("AdminDashboard");
      expect(adminNames).toHaveLength(1);
    });
  });

  describe("beforeEach guard", () => {
    it("redirects to login when auth required and no token", async () => {
      localStorage.removeItem("token");

      await router.push("/my-orders");
      await router.isReady();

      expect(router.currentRoute.value.path).toBe("/login");
    });

    it("allows access when auth required and token exists", async () => {
      localStorage.setItem("token", "valid-token");

      await router.push("/my-orders");
      await router.isReady();

      expect(router.currentRoute.value.path).toBe("/my-orders");
    });

    it("redirects to home when admin required but user is not admin", async () => {
      localStorage.setItem("token", "valid-token");
      localStorage.setItem("userInfo", JSON.stringify({ role: 0 }));

      await router.push("/admin");
      await router.isReady();

      expect(router.currentRoute.value.path).toBe("/");
    });

    it("allows admin access when user is admin", async () => {
      localStorage.setItem("token", "valid-token");
      localStorage.setItem("userInfo", JSON.stringify({ role: 1 }));

      await router.push("/admin");
      await router.isReady();

      expect(router.currentRoute.value.path).toBe("/admin");
    });

    it("allows public routes without token", async () => {
      localStorage.removeItem("token");

      await router.push("/about");
      await router.isReady();

      expect(router.currentRoute.value.path).toBe("/about");
    });

    it("handles 404 route", async () => {
      await router.push("/nonexistent-path");
      await router.isReady();

      expect(router.currentRoute.value.name).toBe("NotFound");
    });
  });
});