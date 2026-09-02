import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const servicesRoot = path.join(root, "microservices", "services");
const matrixPath = path.join(root, "docs", "ci", "microservice-api-coverage.json");
const serviceNames = ["identity", "traffic", "local", "ai", "community", "ops"];

function walk(directory, predicate) {
  if (!fs.existsSync(directory)) return [];
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const fullPath = path.join(directory, entry.name);
    return entry.isDirectory() ? walk(fullPath, predicate) : (predicate(fullPath) ? [fullPath] : []);
  });
}

function normalizeRoute(route) {
  const normalized = `/${route}`.replaceAll(/\/{2,}/g, "/");
  return normalized.length > 1 && normalized.endsWith("/") ? normalized.slice(0, -1) : normalized;
}

function firstQuotedValue(argumentsText = "") {
  return argumentsText.match(/["']([^"']*)["']/)?.[1] ?? "";
}

function discoverEndpoints() {
  const endpoints = [];
  for (const service of serviceNames) {
    const javaRoot = path.join(servicesRoot, `${service}-service`, "src", "main", "java");
    for (const controllerFile of walk(javaRoot, (file) => file.endsWith("Controller.java"))) {
      const source = fs.readFileSync(controllerFile, "utf8");
      if (!source.includes("@RestController")) continue;
      const baseRoute = firstQuotedValue(source.match(/@RequestMapping\s*\(([^)]*)\)/s)?.[1]);
      const mappingPattern = /@(Get|Post|Put|Delete|Patch)Mapping(?:\s*\(([^)]*)\))?/g;
      for (const match of source.matchAll(mappingPattern)) {
        const method = match[1].toUpperCase();
        const route = normalizeRoute(`${baseRoute}/${firstQuotedValue(match[2])}`);
        endpoints.push({
          id: `${method} ${route}`,
          service: `${service}-service`,
          visibility: route.startsWith("/internal/") ? "internal" : "public",
          controller: path.basename(controllerFile, ".java"),
          source: path.relative(root, controllerFile).replaceAll("\\", "/"),
        });
      }
    }
  }
  return endpoints.sort((left, right) => left.id.localeCompare(right.id));
}

function loadTestSources() {
  const result = new Map();
  for (const service of serviceNames) {
    const testRoot = path.join(servicesRoot, `${service}-service`, "src", "test", "java");
    for (const file of walk(testRoot, (candidate) => candidate.endsWith(".java"))) {
      result.set(path.basename(file, ".java"), fs.readFileSync(file, "utf8"));
    }
  }
  return result;
}

function validateAnchor(anchor, endpoint, testSources) {
  if (typeof anchor !== "string" || !anchor.includes("#")) return "测试锚点格式必须为 ClassName#methodName";
  const [className, methodName] = anchor.split("#", 2);
  const source = testSources.get(className);
  if (!source) return `找不到测试类 ${className}`;
  if (!new RegExp(`\\b${methodName}\\s*\\(`).test(source)) return `找不到测试方法 ${anchor}`;
  const route = endpoint.id.split(" ", 2)[1];
  const routePattern = route.split(/(\{[^}]+\})/).map((fragment) => {
    if (/^\{[^}]+\}$/.test(fragment)) return '[^"\\s/]+';
    return fragment.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  }).join("");
  if (route !== "/" && !new RegExp(routePattern).test(source)) {
    return `${anchor} 未包含路由 ${route}`;
  }
  return null;
}

const discovered = discoverEndpoints();
const publicCount = discovered.filter((endpoint) => endpoint.visibility === "public").length;
const internalCount = discovered.length - publicCount;

if (process.argv.includes("--inventory")) {
  console.log(JSON.stringify({
    generatedFrom: "microservices/services/*/src/main/java/**/*Controller.java",
    endpoints: Object.fromEntries(discovered.map((endpoint) => [endpoint.id, {
      service: endpoint.service,
      visibility: endpoint.visibility,
      controller: endpoint.controller,
      source: endpoint.source,
      tests: { normal: [], auth: [], validation: [] },
    }])),
  }, null, 2));
  process.exit(0);
}

const failures = [];
if (!fs.existsSync(matrixPath)) {
  failures.push("缺少 docs/ci/microservice-api-coverage.json；先运行 npm run inventory:microservice-api 生成盘点骨架");
} else {
  const matrix = JSON.parse(fs.readFileSync(matrixPath, "utf8"));
  const registered = matrix.endpoints ?? {};
  const testSources = loadTestSources();
  const discoveredIds = new Set(discovered.map((endpoint) => endpoint.id));

  for (const endpoint of discovered) {
    const entry = registered[endpoint.id];
    if (!entry) {
      failures.push(`${endpoint.id}: 未登记`);
      continue;
    }
    for (const field of ["service", "visibility", "controller", "source"]) {
      if (entry[field] !== endpoint[field]) failures.push(`${endpoint.id}: ${field} 与代码盘点不一致`);
    }
    for (const category of ["normal", "auth", "validation"]) {
      const anchors = entry.tests?.[category];
      if (!Array.isArray(anchors) || anchors.length === 0) {
        failures.push(`${endpoint.id}: 缺少 ${category} 测试锚点`);
        continue;
      }
      for (const anchor of anchors) {
        const error = validateAnchor(anchor, endpoint, testSources);
        if (error) failures.push(`${endpoint.id}: ${category}: ${error}`);
      }
    }
    for (const anchor of entry.tests?.dependencyFailure ?? []) {
      const error = validateAnchor(anchor, endpoint, testSources);
      if (error) failures.push(`${endpoint.id}: dependencyFailure: ${error}`);
    }
  }

  for (const id of Object.keys(registered)) {
    if (!discoveredIds.has(id)) failures.push(`${id}: 清单存在但代码中没有对应端点`);
  }
}

if (failures.length > 0) {
  console.error(`Microservice API coverage check failed: ${discovered.length} endpoints (${publicCount} public, ${internalCount} internal).`);
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(`Microservice API coverage check passed: ${discovered.length} endpoints (${publicCount} public, ${internalCount} internal).`);
