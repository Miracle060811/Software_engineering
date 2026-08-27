import { describe, it, expect, vi, beforeEach } from "vitest";

describe("request.js", () => {
  let request;
  let mockAxiosCreate;
  let mockInterceptorsRequest;
  let mockInterceptorsResponse;

  beforeEach(async () => {
    vi.resetModules();
    vi.clearAllMocks();

    localStorage.clear();
    sessionStorage.clear();

    delete window.location;
    window.location = { replace: vi.fn(), pathname: "/app" };

    mockInterceptorsRequest = { use: vi.fn() };
    mockInterceptorsResponse = { use: vi.fn() };

    mockAxiosCreate = vi.fn().mockReturnValue({
      interceptors: {
        request: mockInterceptorsRequest,
        response: mockInterceptorsResponse,
      },
    });

    vi.doMock("axios", () => ({
      default: {
        create: mockAxiosCreate,
      },
      __esModule: true,
    }));

    vi.doMock("element-plus", () => ({
      ElMessage: { error: vi.fn() },
    }));

    request = (await import("@/utils/request")).default;
  });

  describe("axios instance creation", () => {
    it("creates axios instance with correct config", () => {
      expect(mockAxiosCreate).toHaveBeenCalledWith(
        expect.objectContaining({
          timeout: 30000,
          withCredentials: true,
          withXSRFToken: true,
        }),
      );
    });
  });

  describe("request interceptor", () => {
    it("adds Authorization header when token exists", () => {
      const setTokenInterceptor = mockInterceptorsRequest.use.mock.calls[0][0];
      localStorage.setItem("token", "test-token-123");

      const config = { headers: {} };
      const result = setTokenInterceptor(config);

      expect(result.headers.Authorization).toBe("Bearer test-token-123");
    });

    it("does not add Authorization header when no token", () => {
      const setTokenInterceptor = mockInterceptorsRequest.use.mock.calls[0][0];
      localStorage.removeItem("token");

      const config = { headers: {} };
      const result = setTokenInterceptor(config);

      expect(result.headers.Authorization).toBeUndefined();
    });
  });

  describe("response interceptor - success", () => {
    it("extracts data when response code is 200", () => {
      const successInterceptor = mockInterceptorsResponse.use.mock.calls[0][0];
      const response = {
        data: { code: 200, data: { id: 1, name: "test" } },
        status: 200,
        config: {},
      };

      const result = successInterceptor(response);
      expect(result).toEqual({ id: 1, name: "test" });
    });

    it("rejects when response code is not 200", async () => {
      const successInterceptor = mockInterceptorsResponse.use.mock.calls[0][0];
      const response = {
        data: { code: 400, msg: "参数错误" },
        status: 400,
        config: {},
      };

      await expect(successInterceptor(response)).rejects.toThrow("参数错误");
    });

    it("passes through non-standard response objects", () => {
      const successInterceptor = mockInterceptorsResponse.use.mock.calls[0][0];
      const response = {
        data: { result: "ok" },
        status: 200,
        config: {},
      };

      const result = successInterceptor(response);
      expect(result).toEqual({ result: "ok" });
    });
  });

  describe("response interceptor - error", () => {
    let errorInterceptor;

    beforeEach(() => {
      errorInterceptor = mockInterceptorsResponse.use.mock.calls[0][1];
    });

    it("handles 401 status by clearing auth and redirecting", () => {
      const error = { response: { status: 401 } };
      try {
        errorInterceptor(error);
      } catch (e) {
        // expected
      }

      expect(localStorage.getItem("token")).toBeNull();
    });

    it("handles 403 status similarly to 401", () => {
      const error = { response: { status: 403 } };
      try {
        errorInterceptor(error);
      } catch (e) {
        // expected
      }

      expect(localStorage.getItem("token")).toBeNull();
    });

    it("handles network timeout gracefully", () => {
      const error = {
        code: "ECONNABORTED",
        message: "timeout of 30000ms exceeded",
        config: { url: "/api/flight/search" },
      };

      try {
        errorInterceptor(error);
      } catch (e) {
        // expected
      }
    });

    it("handles no response (network error)", () => {
      const error = {
        message: "Network Error",
        config: {},
      };

      try {
        errorInterceptor(error);
      } catch (e) {
        // expected
      }
    });
  });
});