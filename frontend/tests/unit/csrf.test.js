import { describe, it, expect, vi, beforeEach } from "vitest";

describe("csrf.js", () => {
  let csrf;

  beforeEach(async () => {
    vi.resetModules();
    vi.clearAllMocks();
    localStorage.clear();

    csrf = await import("@/utils/csrf");
  });

  describe("CSRF_COOKIE_NAME and CSRF_HEADER_NAME", () => {
    it("exports CSRF_COOKIE_NAME", () => {
      expect(csrf.CSRF_COOKIE_NAME).toBe("XSRF-TOKEN");
    });

    it("exports CSRF_HEADER_NAME", () => {
      expect(csrf.CSRF_HEADER_NAME).toBe("X-XSRF-TOKEN");
    });
  });

  describe("buildUploadHeaders", () => {
    it("builds headers with token and csrf token", () => {
      localStorage.setItem("token", "test-token");
      document.cookie = "XSRF-TOKEN=csrf-token-value";

      const headers = csrf.buildUploadHeaders();

      expect(headers.Authorization).toBe("Bearer test-token");
      expect(headers["X-XSRF-TOKEN"]).toBe("csrf-token-value");
    });

    it("builds headers without Authorization when no token", () => {
      localStorage.clear();

      const headers = csrf.buildUploadHeaders();

      expect(headers.Authorization).toBeUndefined();
    });

    it("includes Authorization header when token exists", () => {
      localStorage.setItem("token", "test-token");

      const headers = csrf.buildUploadHeaders();

      expect(headers.Authorization).toBe("Bearer test-token");
    });
  });
});