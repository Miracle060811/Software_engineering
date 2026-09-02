import fs from "node:fs"
import path from "node:path"
import process from "node:process"

const root = process.cwd()
const migrationDir = path.join(root, "backend", "src", "main", "resources", "db", "migration")
const k8sDir = path.join(root, "deploy", "k8s")
const overlayDir = path.join(root, "deploy", "k8s-overlays")
const microserviceK8sDir = path.join(root, "microservices", "k8s")
const failures = []

function fail(message) {
  failures.push(message)
}

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8")
}

if (!fs.existsSync(migrationDir)) {
  fail("缺少 Flyway 迁移目录 backend/src/main/resources/db/migration")
} else {
  const sqlFiles = fs.readdirSync(migrationDir).filter((name) => name.endsWith(".sql")).sort()
  const versions = new Map()

  if (sqlFiles.length === 0) fail("Flyway 迁移目录中没有 SQL 文件")

  for (const file of sqlFiles) {
    const match = /^V([1-9]\d*)__([A-Za-z0-9_]+)\.sql$/.exec(file)
    if (!match) {
      fail(`迁移文件名不符合 V<版本>__<说明>.sql：${file}`)
      continue
    }

    const version = Number(match[1])
    if (versions.has(version)) {
      fail(`Flyway 版本 V${version} 重复：${versions.get(version)}、${file}`)
    }
    versions.set(version, file)

    const sql = fs.readFileSync(path.join(migrationDir, file), "utf8")
    if (/\bCREATE\s+DATABASE\b|\bUSE\s+[`\w-]+\s*;/i.test(sql)) {
      fail(`${file} 不应创建或切换数据库，数据库应由运行环境提供`)
    }
  }

  const ordered = [...versions.keys()].sort((a, b) => a - b)
  if (ordered[0] !== 1) fail("Flyway 迁移必须从 V1 开始")
  for (let index = 1; index < ordered.length; index += 1) {
    if (ordered[index] !== ordered[index - 1] + 1) {
      fail(`Flyway 版本不连续：V${ordered[index - 1]} 后直接出现 V${ordered[index]}`)
    }
  }
}

const requiredK8sFiles = [
  "namespace.yaml",
  "configmap.yaml",
  "storage.yaml",
  "mysql.yaml",
  "redis.yaml",
  "backend.yaml",
  "frontend.yaml",
]
const kustomization = read("deploy/k8s/kustomization.yaml")
for (const file of requiredK8sFiles) {
  if (!fs.existsSync(path.join(k8sDir, file))) fail(`缺少 Kubernetes 清单：deploy/k8s/${file}`)
  if (!kustomization.includes(`- ${file}`)) fail(`kustomization.yaml 未包含 ${file}`)
}

const microserviceK8sFiles = [
  ["identity-service", 8081],
  ["traffic-service", 8082],
  ["local-service", 8083],
  ["ai-service", 8084],
  ["community-service", 8085],
  ["ops-service", 8086],
]
for (const [service, port] of microserviceK8sFiles) {
  const relativePath = `deploy/k8s/${service}.yaml`
  if (!fs.existsSync(path.join(root, relativePath))) {
    fail(`缺少微服务 Kubernetes 清单：${relativePath}`)
    continue
  }
  const manifest = read(relativePath)
  if (!/kind:\s*Service\b/.test(manifest)) fail(`${service}.yaml 缺少 Service`)
  if (!/kind:\s*Deployment\b/.test(manifest)) fail(`${service}.yaml 缺少 Deployment`)
  if (!new RegExp(`\\bport:\\s*${port}\\b`).test(manifest)) fail(`${service}.yaml 未暴露端口 ${port}`)
  if (!/\breadinessProbe\s*:/.test(manifest)) fail(`${service}.yaml 缺少 readinessProbe`)
  if (!/\blivenessProbe\s*:/.test(manifest)) fail(`${service}.yaml 缺少 livenessProbe`)
  if (!/\brequests\s*:[\s\S]*?\bcpu\s*:[\s\S]*?\bmemory\s*:/.test(manifest)) {
    fail(`${service}.yaml 缺少 CPU/内存 requests`)
  }
  if (!/\blimits\s*:[\s\S]*?\bcpu\s*:[\s\S]*?\bmemory\s*:/.test(manifest)) {
    fail(`${service}.yaml 缺少 CPU/内存 limits`)
  }
  if (!/\bconfigMapRef\s*:|\bconfigMapKeyRef\s*:/.test(manifest)) fail(`${service}.yaml 未引用 ConfigMap`)
  if (!/\bsecretKeyRef\s*:/.test(manifest)) fail(`${service}.yaml 未引用 Secret`)
}

for (const file of ["microservices-configmap.yaml", "microservice-database-bootstrap.yaml"]) {
  if (!fs.existsSync(path.join(k8sDir, file))) fail(`缺少微服务 Kubernetes 清单：deploy/k8s/${file}`)
}

const microserviceDeployScript = read("scripts/cd/Deploy-TravelMateMicroservices.ps1")
for (const service of microserviceK8sFiles.map(([name]) => name)) {
  if (!microserviceDeployScript.includes(`"${service}"`)) {
    fail(`微服务部署脚本未包含 ${service}`)
  }
}
if (!/ReleaseEvidencePath/.test(microserviceDeployScript) || !/sha256:/.test(microserviceDeployScript)) {
  fail("微服务部署脚本未强制使用发布证据中的镜像 digest")
}

const ciWorkflow = read(".github/workflows/ci.yml")
if (!/runs-on:[\s\S]*?travelmate-deploy[\s\S]*?timeout-minutes:\s*10/.test(ciWorkflow)) {
  fail("部署流水线未设置 YFan_deploy Runner 的 10 分钟超时")
}
for (const port of [8081, 8082, 8083, 8084, 8085, 8086]) {
  if (!ciWorkflow.includes(`ServicePort = ${port}`)) fail(`部署后健康检查未覆盖端口 ${port}`)
}

for (const service of ["backend", "frontend", "mysql", "redis"]) {
  const manifest = read(`deploy/k8s/${service}.yaml`)
  if (!/\breadinessProbe\s*:/.test(manifest)) fail(`${service}.yaml 缺少 readinessProbe`)
  if (!/\blivenessProbe\s*:/.test(manifest)) fail(`${service}.yaml 缺少 livenessProbe`)
}

const requiredMicroserviceK8sFiles = [
  "namespace.yaml",
  "configmap.yaml",
  "databases.yaml",
  "redis.yaml",
  "identity-service.yaml",
  "traffic-service.yaml",
  "local-service.yaml",
  "ai-service.yaml",
  "community-service.yaml",
  "ops-service.yaml",
]
if (!fs.existsSync(microserviceK8sDir)) {
  fail("缺少微服务 Kubernetes 目录：microservices/k8s")
} else {
  const microserviceKustomization = read("microservices/k8s/kustomization.yaml")
  for (const file of requiredMicroserviceK8sFiles) {
    if (!fs.existsSync(path.join(microserviceK8sDir, file))) {
      fail(`缺少微服务 Kubernetes 清单：microservices/k8s/${file}`)
    }
    if (!microserviceKustomization.includes(`- ${file}`)) {
      fail(`microservices/k8s/kustomization.yaml 未包含 ${file}`)
    }
  }

  for (const service of ["identity", "traffic", "local", "ai", "community", "ops"]) {
    const manifest = read(`microservices/k8s/${service}-service.yaml`)
    if (!/\bkind:\s*Deployment\b/.test(manifest)) fail(`${service}-service.yaml 缺少 Deployment`)
    if (!/\bstartupProbe\s*:/.test(manifest)) fail(`${service}-service.yaml 缺少 startupProbe`)
    if (!/\breadinessProbe\s*:/.test(manifest)) fail(`${service}-service.yaml 缺少 readinessProbe`)
    if (!/\blivenessProbe\s*:/.test(manifest)) fail(`${service}-service.yaml 缺少 livenessProbe`)
    if (!/\brequests\s*:/.test(manifest) || !/\blimits\s*:/.test(manifest)) {
      fail(`${service}-service.yaml 缺少 HPA 所需的资源 requests/limits`)
    }
  }

  const databases = read("microservices/k8s/databases.yaml")
  const redis = read("microservices/k8s/redis.yaml")
  const hpa = read("deploy/k8s/hpa.yaml")
  if ((databases.match(/\bvolumeClaimTemplates\s*:/g) || []).length !== 6) {
    fail("databases.yaml 必须为六套 MySQL 分别声明 PVC 模板")
  }
  if ((redis.match(/\bvolumeClaimTemplates\s*:/g) || []).length !== 1) {
    fail("redis.yaml 必须声明持久化 PVC 模板")
  }
  if ((hpa.match(/\bkind:\s*HorizontalPodAutoscaler\b/g) || []).length !== 6) {
    fail("hpa.yaml 必须包含六个业务服务的 HPA")
  }
  for (const [field, expected] of [["minReplicas", "2"], ["maxReplicas", "6"], ["averageUtilization", "60"]]) {
    const matches = hpa.match(new RegExp(`\\b${field}:\\s*${expected}\\b`, "g")) || []
    if (matches.length !== 6) fail(`hpa.yaml 中 ${field}: ${expected} 应出现 6 次`)
  }
}

const deploymentKustomization = read("deploy/k8s/kustomization.yaml")
if (!deploymentKustomization.includes("- hpa.yaml")) {
  fail("deploy/k8s/kustomization.yaml 未包含正式 HPA 清单")
}
if (!/Join-Path\s+\$kubernetesDirectory\s+"hpa\.yaml"/.test(microserviceDeployScript)) {
  fail("微服务部署脚本未从 deploy/k8s 应用正式 HPA 清单")
}

const configMap = read("deploy/k8s/configmap.yaml")
if (/^\s{2,}(?:[^#\n]*(?:PASSWORD|SECRET|TOKEN|API_KEY))\s*:/gim.test(configMap)) {
  fail("ConfigMap 中发现疑似敏感配置键，请改用 Kubernetes Secret")
}

for (const directory of [k8sDir, microserviceK8sDir].filter((candidate) => fs.existsSync(candidate))) {
  for (const file of fs.readdirSync(directory).filter((name) => name.endsWith(".yaml"))) {
    const manifest = fs.readFileSync(path.join(directory, file), "utf8")
    for (const match of manifest.matchAll(/^\s*image:\s*([^\s#]+)/gim)) {
      if (/:latest$/i.test(match[1])) fail(`${path.relative(root, path.join(directory, file))} 使用了不可追踪的 latest 镜像标签：${match[1]}`)
    }
  }
}

for (const file of [".env.example", ".env.server.example", "compose.yml", "compose.server.yml"]) {
  if (!fs.existsSync(path.join(root, file))) fail(`缺少部署配置：${file}`)
}

for (const environment of ["local", "server"]) {
  const directory = path.join(overlayDir, environment)
  const overlay = path.join(directory, "kustomization.yaml")
  if (!fs.existsSync(overlay)) {
    fail(`缺少 Kubernetes ${environment} overlay：deploy/k8s-overlays/${environment}/kustomization.yaml`)
    continue
  }

  const content = fs.readFileSync(overlay, "utf8")
  if (!content.includes("../../k8s")) {
    fail(`${environment} overlay 未引用 deploy/k8s 基础清单`)
  }
}

if (fs.existsSync(path.join(root, ".env.server.example"))) {
  const serverExample = read(".env.server.example")
  for (const key of ["JWT_SECRET", "DB_PASSWORD", "S3_ENDPOINT", "S3_BUCKET", "S3_ACCESS_KEY", "S3_SECRET_KEY"]) {
    if (!new RegExp(`^${key}=`, "m").test(serverExample)) {
      fail(`.env.server.example 缺少 ${key}`)
    }
  }
}

if (failures.length > 0) {
  console.error("数据库与部署配置检查失败：")
  failures.forEach((message) => console.error(`- ${message}`))
  process.exit(1)
}

console.log("数据库与部署配置检查通过：Flyway 版本、Kubernetes 资源、健康探针和敏感配置均符合要求。")
