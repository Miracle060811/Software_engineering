import { defineConfig, devices } from "@playwright/test";

const frontendPort = process.env.MICROSERVICE_E2E_PORT || "3100";

export default defineConfig({
  testDir: "./tests/e2e-real",
  outputDir: "./test-results-microservices",
  timeout: 45000,
  expect: {
    timeout: 10000,
  },
  reporter: [["list"], ["html", { outputFolder: "playwright-report-microservices", open: "never" }]],
  use: {
    baseURL: process.env.PLAYWRIGHT_BASE_URL || `http://127.0.0.1:${frontendPort}`,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: {
    command: `npm run dev -- --host 127.0.0.1 --port ${frontendPort}`,
    url: `http://127.0.0.1:${frontendPort}`,
    reuseExistingServer: false,
    timeout: 90000,
    env: {
      VITE_BACKEND_MODE: "microservices",
      VITE_DEV_PORT: frontendPort,
      IDENTITY_SERVICE_URL: process.env.IDENTITY_SERVICE_URL || "http://127.0.0.1:8081",
      TRAFFIC_SERVICE_URL: process.env.TRAFFIC_SERVICE_URL || "http://127.0.0.1:8082",
      LOCAL_SERVICE_URL: process.env.LOCAL_SERVICE_URL || "http://127.0.0.1:8083",
      AI_SERVICE_URL: process.env.AI_SERVICE_URL || "http://127.0.0.1:8084",
      COMMUNITY_SERVICE_URL: process.env.COMMUNITY_SERVICE_URL || "http://127.0.0.1:8085",
      OPS_SERVICE_URL: process.env.OPS_SERVICE_URL || "http://127.0.0.1:8086",
    },
  },
  projects: [
    {
      name: "chromium-microservices",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
});
