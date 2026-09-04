import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const specPath = path.join(
  root,
  "frontend",
  "tests",
  "e2e-real",
  "travelmate-real.spec.js",
);
const matrixPath = path.join(
  root,
  "docs",
  "ci",
  "microservice-use-case-matrix.json",
);
const apiCoveragePath = path.join(
  root,
  "docs",
  "ci",
  "microservice-api-coverage.json",
);
const vitePath = path.join(root, "frontend", "vite.config.js");
const configPath = path.join(
  root,
  "frontend",
  "playwright.microservices.config.js",
);
const nginxPath = path.join(root, "frontend", "nginx.conf");
const workflowPath = path.join(root, ".github", "workflows", "ci.yml");

const spec = fs.readFileSync(specPath, "utf8");
const vite = fs.readFileSync(vitePath, "utf8");
const playwrightConfig = fs.readFileSync(configPath, "utf8");
const nginx = fs.readFileSync(nginxPath, "utf8");
const workflow = fs.readFileSync(workflowPath, "utf8");
const matrix = JSON.parse(fs.readFileSync(matrixPath, "utf8"));
const apiCoverage = JSON.parse(fs.readFileSync(apiCoveragePath, "utf8"));
const expected = Array.from(
  { length: 19 },
  (_, index) => `UC${String(index + 1).padStart(2, "0")}`,
);
const failures = [];

for (const uc of expected) {
  if (!new RegExp(`\\b${uc}\\b`).test(spec))
    failures.push(`${uc}: E2E spec 中没有场景`);
  if (!matrix[uc]) failures.push(`${uc}: 微服务矩阵中没有归属`);
  if (
    matrix[uc] &&
    (!Array.isArray(matrix[uc].services) || matrix[uc].services.length === 0)
  ) {
    failures.push(`${uc}: 未声明负责服务`);
  }
  if (
    matrix[uc] &&
    (!Array.isArray(matrix[uc].routes) || matrix[uc].routes.length === 0)
  ) {
    failures.push(`${uc}: 未声明验证路由`);
  }
}

for (const service of [
  "IDENTITY",
  "TRAFFIC",
  "LOCAL",
  "AI",
  "COMMUNITY",
  "OPS",
]) {
  if (!vite.includes(`${service}_SERVICE_URL`))
    failures.push(`Vite 未配置 ${service}_SERVICE_URL`);
}

if (!playwrightConfig.includes('VITE_BACKEND_MODE: "microservices"')) {
  failures.push("Playwright 微服务配置未启用 microservices 路由模式");
}

const nginxUpstreams = [
  ["identity_api", "identity-service", 8081],
  ["traffic_api", "traffic-service", 8082],
  ["local_api", "local-service", 8083],
  ["ai_api", "ai-service", 8084],
  ["community_api", "community-service", 8085],
  ["ops_api", "ops-service", 8086],
];
for (const [upstream, service, port] of nginxUpstreams) {
  if (
    !nginx.includes(`upstream ${upstream} {`) ||
    !nginx.includes(`server ${service}:${port};`)
  ) {
    failures.push(
      `生产 Nginx 未声明 ${service}:${port} 的 ${upstream} upstream`,
    );
  }
}
if (nginx.includes("server travelmate-backend:8080;")) {
  failures.push("生产 Nginx 仍将前端业务请求发送到单体 travelmate-backend");
}

const nginxRoutes = [
  [
    "location ~ ^/(?:api/(?:passenger|follow|user)(?:/|$)|user(?:/|$))",
    "identity_api",
  ],
  ["location ~ ^/api/(?:flight|train|order|price)(?:/|$)", "traffic_api"],
  [
    "location ~ ^/api/(?:hotel|attraction|destinations|tour|review|reply|coupon)(?:/|$)",
    "local_api",
  ],
  [
    "location ~ ^/api/(?:ai|notification|notifications|private-message)(?:/|$)",
    "ai_api",
  ],
  [
    "location ~ ^/(?:api/(?:post|comment|like|file)(?:/|$)|uploads(?:/|$))",
    "community_api",
  ],
  ["location ~ ^/api/admin(?:/|$)", "ops_api"],
];
for (const [location, upstream] of nginxRoutes) {
  const start = nginx.indexOf(location);
  const end = start < 0 ? -1 : nginx.indexOf("\n        }", start);
  const block = start < 0 || end < 0 ? "" : nginx.slice(start, end);
  if (!block.includes(`proxy_pass http://${upstream};`)) {
    failures.push(`生产 Nginx 路由缺失或目标错误：${location} -> ${upstream}`);
  }
}

const publicRouteOwnership = [
  [
    "identity-service",
    /^\/(?:user(?:\/|$)|api\/(?:passenger|follow|user)(?:\/|$))/,
  ],
  ["traffic-service", /^\/api\/(?:flight|train|order|price)(?:\/|$)/],
  [
    "local-service",
    /^\/api\/(?:hotel|attraction|destinations|tour|review|reply|coupon)(?:\/|$)/,
  ],
  [
    "ai-service",
    /^\/api\/(?:ai|notification|notifications|private-message)(?:\/|$)/,
  ],
  [
    "community-service",
    /^\/(?:uploads(?:\/|$)|api\/(?:post|comment|like|file)(?:\/|$))/,
  ],
  ["ops-service", /^\/api\/admin(?:\/|$)/],
];
for (const [operation, endpoint] of Object.entries(apiCoverage.endpoints)) {
  if (endpoint.visibility !== "public") continue;
  const requestPath = operation.slice(operation.indexOf(" ") + 1);
  const owners = publicRouteOwnership.filter(([, routePattern]) =>
    routePattern.test(requestPath),
  );
  if (owners.length !== 1 || owners[0][0] !== endpoint.service) {
    failures.push(
      `公开端点没有唯一且正确的生产路由：${operation}，登记服务=${endpoint.service}，路由服务=${owners.map(([service]) => service).join(",") || "无"}`,
    );
  }
}

for (const required of [
  "  microservice-e2e:",
  "name: Real microservice E2E",
  "npm run test:e2e:microservices",
  "microservice-e2e-evidence-${{ github.sha }}",
  "${{ needs.microservice-e2e.result }}",
  "MICROSERVICE_E2E_EXTERNAL_FRONTEND",
  "Build and start production frontend gateway",
]) {
  if (!workflow.includes(required))
    failures.push(`CI 未接入微服务真实 E2E：${required}`);
}

if (
  !playwrightConfig.includes('MICROSERVICE_E2E_EXTERNAL_FRONTEND === "true"')
) {
  failures.push("Playwright 微服务配置不支持直接验收生产前端网关");
}

if (failures.length > 0) {
  console.error("Microservice E2E coverage check failed:");
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

const blocked = expected.filter((uc) => matrix[uc].status !== "runnable");
console.log(
  `Microservice E2E structure covers ${expected.length}/19 use cases.`,
);
console.log(
  `Runnable now: ${expected.length - blocked.length}; blocked by service migration: ${blocked.length}.`,
);
if (blocked.length > 0) console.log(`Blocked: ${blocked.join(", ")}`);
