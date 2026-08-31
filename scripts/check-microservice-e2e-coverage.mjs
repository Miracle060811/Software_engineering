import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const specPath = path.join(root, "frontend", "tests", "e2e-real", "travelmate-real.spec.js");
const matrixPath = path.join(root, "docs", "ci", "microservice-use-case-matrix.json");
const vitePath = path.join(root, "frontend", "vite.config.js");
const configPath = path.join(root, "frontend", "playwright.microservices.config.js");

const spec = fs.readFileSync(specPath, "utf8");
const vite = fs.readFileSync(vitePath, "utf8");
const playwrightConfig = fs.readFileSync(configPath, "utf8");
const matrix = JSON.parse(fs.readFileSync(matrixPath, "utf8"));
const expected = Array.from({ length: 19 }, (_, index) => `UC${String(index + 1).padStart(2, "0")}`);
const failures = [];

for (const uc of expected) {
  if (!new RegExp(`\\b${uc}\\b`).test(spec)) failures.push(`${uc}: E2E spec 中没有场景`);
  if (!matrix[uc]) failures.push(`${uc}: 微服务矩阵中没有归属`);
  if (matrix[uc] && (!Array.isArray(matrix[uc].services) || matrix[uc].services.length === 0)) {
    failures.push(`${uc}: 未声明负责服务`);
  }
  if (matrix[uc] && (!Array.isArray(matrix[uc].routes) || matrix[uc].routes.length === 0)) {
    failures.push(`${uc}: 未声明验证路由`);
  }
}

for (const service of ["IDENTITY", "TRAFFIC", "LOCAL", "AI", "COMMUNITY", "OPS"]) {
  if (!vite.includes(`${service}_SERVICE_URL`)) failures.push(`Vite 未配置 ${service}_SERVICE_URL`);
}

if (!playwrightConfig.includes('VITE_BACKEND_MODE: "microservices"')) {
  failures.push("Playwright 微服务配置未启用 microservices 路由模式");
}

if (failures.length > 0) {
  console.error("Microservice E2E coverage check failed:");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

const blocked = expected.filter((uc) => matrix[uc].status !== "runnable");
console.log(`Microservice E2E structure covers ${expected.length}/19 use cases.`);
console.log(`Runnable now: ${expected.length - blocked.length}; blocked by service migration: ${blocked.length}.`);
if (blocked.length > 0) console.log(`Blocked: ${blocked.join(", ")}`);
