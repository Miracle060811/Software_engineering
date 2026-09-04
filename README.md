# TravelMate（伴游）出行旅游平台

TravelMate 是 Miracle 小组的软件工程课程项目，围绕“行前规划—资源预订—行中服务—行后分享”提供一体化旅行体验。当前仓库以可本地运行、可自动化验证的前后端单体作为回归基线，并已建立六个独立微服务；它不是接入真实支付、出票或商用库存的生产 OTA。

## 当前能力

| 领域 | 已实现能力 |
| --- | --- |
| 出行资源 | 航班搜索、火车票搜索与中转建议、12306 公开余票读取、酒店/房型、景点门票、一日游与目的地资料 |
| 订单履约 | 多乘客/多间房下单、模拟支付、取消、退款申请与审批、交通订单人工出票、库存扣减与回补、订单详情与凭证 |
| AI 服务 | DeepSeek 行程规划与多轮客服、地点有效性核验、天气工具调用；外部服务不可用时回退本地模板 |
| 社区与用户 | 注册登录、个人主页、游记草稿与审核、点赞/收藏/评论、关注关系、私信、通知、优惠券与浏览记录 |
| 管理后台 | RBAC、用户/资源/订单/优惠券管理、CSV 导入、内容与评价审核、系统日志和轻量运行指标 |
| 工程质量 | 后端 JUnit/MockMvc、前端 ESLint/构建、Mock 与真实后端 Playwright E2E、JaCoCo、SpotBugs、依赖/密钥扫描及 CodeQL |

系统核心主链路可运行，课程验收证据已按源码、文档、DevOps、测试、管理和答辩六类归档。UC01—UC19 的证据基线由 [`docs/ci/use-case-test-matrix.json`](docs/ci/use-case-test-matrix.json) 与 [`docs/ci/test-quality-policy.json`](docs/ci/test-quality-policy.json) 管理；六微服务的 119 个 HTTP 端点（100 个公开、19 个内部）测试映射由 [`docs/ci/microservice-api-coverage.json`](docs/ci/microservice-api-coverage.json) 管理，不能把“代码已存在”直接视为“场景已被完整自动化覆盖”。

## 微服务迁移状态

`identity-service`、`traffic-service`、`local-service`、`ai-service`、`community-service`、`ops-service` 已在 [`microservices`](microservices/README.md) 下建立独立 Maven 模块、配置、健康检查和 Dockerfile。跨域读取改用内部 HTTP 接口，AI 服务覆盖通知、行程、对话和私信；单体 `backend` 继续保留，便于迁移期间做功能回归。

六服务分库 DDL、本地 Compose、事务 Outbox 写入/重试投递、AI 通知幂等消费和历史数据迁移验收均已形成证据；Kubernetes 部署统一使用 [`deploy/k8s`](deploy/k8s)，六服务接入同一 `travelmate` 命名空间，HPA 清单与扩缩容实验也以该环境为准。`microservices/k8s` 保存六套独立 MySQL/PVC 的物理隔离实验方案。本课程交付边界是 Docker Desktop Kubernetes 单集群部署，不采用 API Gateway、注册中心或生产多集群编排；中期设计历史基线见 [`02_docs/TravelMate中期验收基线.md`](02_docs/TravelMate中期验收基线.md)。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite 8、JavaScript、Element Plus、Pinia、Vue Router、Axios、ECharts |
| 后端 | Java 21、Spring Boot 3.5.15、Spring Security、MyBatis-Plus 3.5.7、Maven Wrapper |
| 数据 | MySQL 8.0、Redis 6/7（缓存、限流与库存辅助） |
| 认证 | JWT（jjwt 0.11.5）+ BCrypt + 基于角色的访问控制 |
| 外部能力 | DeepSeek API、12306 公开余票接口、OpenStreetMap Nominatim、Open-Meteo |
| CI/CD | GitHub Actions、Playwright、JaCoCo、SpotBugs、Gitleaks、Trivy、CodeQL |

## 运行与验收速览（正式部署：Kubernetes）

> 项目的正式演示与持续部署环境是 **Docker Desktop Kubernetes**：context 为 `docker-desktop`，namespace 为 `travelmate`，资源清单统一使用 [`deploy/k8s`](deploy/k8s) 及其 overlay。根目录 `compose.yml` 和 `microservices/compose.yml` 仅用于本地联调，不是正式部署方式。

### 环境版本

| 组件 | 项目要求 / 已验收版本 |
| --- | --- |
| 操作系统 | Windows 10/11 + PowerShell |
| Docker Desktop / Engine | 4.88.1 / 29.7.2，必须启用 Kubernetes |
| Kubernetes / kubectl | 1.36.1 / 1.36.1；context `docker-desktop` |
| JDK / Maven | JDK 21；Maven Wrapper 3.9.14 |
| Node.js / npm | Node.js 22.12+；CI 与验收机使用 Node.js 24（验收机 npm 11.12.1） |
| 数据组件 | MySQL 8.0、Redis 7 Alpine、MinIO `RELEASE.2025-04-22T22-12-26Z` |

### 端口

| 环境 | 服务 | 端口与访问范围 |
| --- | --- | --- |
| Kubernetes | 前端 `travelmate-frontend` | `30080`，local overlay 下由 `LoadBalancer` 暴露，演示机访问 <http://localhost:30080> |
| Kubernetes | 单体后端 / 六个微服务 | `8080` / `8081`–`8086`，均为集群内 `ClusterIP`，不直接暴露到宿主机 |
| Kubernetes | MySQL / Redis / MinIO | `3306` / `6379` / `9000`，均为集群内端口；local overlay 另将 MinIO 暴露为 `30900` |
| 可选本地开发 | 前端 / 单体后端 / MySQL / Redis | `3000` / `8080` / `3306` / `6379` |
| 可选微服务 Compose | 六服务 / 六套 MySQL | `8081`–`8086` / `3307`–`3312` |

`server` overlay 会把前端改为 `ClusterIP` 并通过 Ingress 提供入口，因此服务器地址与端口以实际 Ingress 配置为准。本地开发和 Compose 端口可通过对应 `.env` 调整；Kubernetes Pod 不读取根目录 `.env`。

### 启动方法

首次配置演示机时，在仓库根目录执行：

```powershell
.\scripts\cd\Configure-TravelMateGhcrCredential.ps1
.\scripts\cd\Initialize-TravelMateKubernetes.ps1 -Environment local
```

初始化脚本负责创建 `travelmate` namespace、Secret、数据库初始化 ConfigMap，并应用 local overlay。随后合并到 `main` 或手动运行 GitHub Actions 的 `TravelMate CI/CD`，由带 `travelmate-deploy` 标签的 self-hosted Runner 部署前后端和六个微服务。已经完成初始化与部署的演示机，只需启动 Docker Desktop，等待下列资源恢复 Ready：

```powershell
kubectl config use-context docker-desktop
kubectl -n travelmate get deploy,pod,hpa,pvc
```

本地开发备用启动命令为 `.\start.bat`（或 `.\start.ps1`）；它不代表正式 Kubernetes 部署。

### 健康检查地址

| 检查对象 | 命令 / 地址 |
| --- | --- |
| Kubernetes 前端 | <http://localhost:30080/healthz> |
| Kubernetes 后端 readiness | `kubectl get --raw "/api/v1/namespaces/travelmate/services/http:travelmate-backend:8080/proxy/actuator/health/readiness"` |
| Kubernetes 六微服务 | 将下面命令中的 `<service>` 与 `<port>` 分别替换为 `identity-service:8081`、`traffic-service:8082`、`local-service:8083`、`ai-service:8084`、`community-service:8085` 或 `ops-service:8086`：`kubectl get --raw "/api/v1/namespaces/travelmate/services/http:<service>:<port>/proxy/actuator/health"` |

健康响应应为前端 `ok`、后端及六微服务 `{"status":"UP"}`。本地开发模式的单体后端健康地址为 <http://localhost:8080/actuator/health>。

### 测试账号与初始数据

- 管理员：`admin` / `123456`；普通用户：`test` / `123456`、`alice` / `123456`、`bob` / `123456`。这些账号只用于课程演示，首次登录后应修改默认密码，详见 [账号与权限](#账号与权限)。
- Kubernetes 首次初始化会把 [`docs/sql/init.sql`](docs/sql/init.sql) 挂载到 MySQL 的 `/docker-entrypoint-initdb.d/`；**仅当 MySQL PVC 为空时**自动建立 `travelmate` 库并写入测试账号、航班、火车、酒店、景点、游记等演示数据，已有 PVC 不会被重复导入或清空。
- 六微服务首次整体部署时，`Deploy-TravelMateMicroservices.ps1` 会通过数据库 bootstrap Job 建立 `travelmate_identity`、`travelmate_traffic`、`travelmate_local`、`travelmate_ai`、`travelmate_community`、`travelmate_ops` 六个逻辑库；对应 schema 与 seed 位于 [`microservices/sql`](microservices/sql)，只对首次创建的库导入 seed。

## 快速开始

### 1. 准备环境

推荐使用 Windows 10/11 与 PowerShell，并安装：

- JDK 21；
- MySQL 8.0，同时确保 `mysql` 客户端可用；
- Node.js 22.12+ 与 npm（Vite 8 要求；CI 使用 Node.js 24）；
- Redis 6.x 或 7.x（推荐，默认监听 `127.0.0.1:6379`）。

MySQL 是必需依赖。Redis 未运行时后端仍可启动，但限流、缓存和部分库存能力会降级。首次安装依赖、读取 12306 余票或调用 AI/天气/地点服务时还需要网络连接。

### 2. 配置本地环境

在仓库根目录执行：

```powershell
Copy-Item .env.example .env
```

编辑 `.env`，至少正确填写 MySQL 密码与随机 JWT 密钥：

```dotenv
DB_PASSWORD="你的 MySQL root 密码"
JWT_SECRET="至少 32 个随机字节的 Base64 文本"
DEEPSEEK_API_KEY=""
ADMIN_REGISTER_SECRET="请替换为本地强随机值"
```

- `DEEPSEEK_API_KEY` 可留空；行程规划和客服会使用本地降级结果。需要真实 AI 响应时再填入有效密钥。
- `JWT_SECRET` 在不同实例间必须保持一致，否则登录令牌会随机失效；模板包含 PowerShell 生成命令。
- `RATE_LIMIT_ENABLED` 正式环境必须保持 `true`；仅可在隔离的自动化验收环境临时设为 `false`，避免整套 E2E 共用单一 Runner IP 时互相限流。
- Flyway V7 会禁用仍使用公开默认密码哈希的历史 `admin` 种子账号；已修改密码的管理员不受影响。没有有效管理员时，通过限时、一次性的管理员初始化入口创建首个管理员。
- `ADMIN_REGISTER_SECRET` 仅用于创建管理员账号；留空会关闭管理员注册入口。
- 已提交模板不提供 `S3_SECRET_KEY` 示例值；本地运行 `Initialize-TravelMateLocalEnv.ps1` 自动生成，服务器必须通过安全渠道填写实际值。
- `.env` 已被 Git 忽略，不要把密码、Token 或 API Key 写进源码、README 或提交记录。

DeepSeek 的网关、模型和推理选项可继续使用 [`.env.example`](.env.example) 中的默认配置。

### 3. 初始化数据库

先启动 MySQL，再在仓库根目录执行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1 -InitDb
```

脚本会读取 `.env`，查找 `mysql.exe`，并按 `utf8mb4` 导入 [`docs/sql/init.sql`](docs/sql/init.sql)。不要使用 `Get-Content ... | mysql` 导入 SQL，否则 Windows PowerShell 的编码转换可能把中文写成 `?`。

如本地数据库已经损坏且数据可以全部丢弃，可显式重建：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\setup.ps1 -InitDb -ResetDb
```

> `-ResetDb` 会删除并重建整个本地 `travelmate` 数据库。日常启动或需要保留本地数据时不要使用。

### 4. 启动系统

推荐从仓库根目录运行：

```powershell
.\start.bat
```

`start.bat` 会调用 [`start.ps1`](start.ps1)：读取 `.env`、检查或尝试启动 Redis、启动后端、等待后端可访问，再启动前端；若缺少 `frontend/node_modules`，还会先安装前端依赖。

启动后可访问：

- 前端：<http://localhost:3000>
- 后端健康检查：<http://localhost:8080/actuator/health>

启动脚本会打开独立终端窗口。停止系统时，在前端、后端窗口中分别按 `Ctrl+C`；由脚本单独启动的 Redis 也应在对应窗口停止。重新启动前先确认 3000、8080 和 6379 端口没有被旧进程占用。

## 启动选项

仅启动一侧服务：

```powershell
.\start-backend.ps1
.\start-frontend.ps1
```

常用参数：

| 参数 | 作用 |
| --- | --- |
| `-DbPassword <密码>` | 临时指定后端 MySQL 密码，优先于 `.env` 和本地配置 |
| `-DeepseekApiKey <密钥>` | 临时指定 DeepSeek API Key |
| `-BackendOnly` / `-FrontendOnly` | 只启动后端或前端；不能同时使用 |
| `-SkipRedis` | 跳过 Redis 检查与自动启动 |
| `-SkipFrontendInstall` | 缺少依赖时也不执行 `npm install` |
| `-DryRun` | 只检查并打印启动命令，不创建服务进程 |

例如：

```powershell
.\start.ps1 -BackendOnly -SkipRedis
.\start.ps1 -FrontendOnly -SkipFrontendInstall
.\start.ps1 -DryRun
```

首次使用或 `.env` 缺少 JWT/对象存储变量时执行：

```powershell
.\scripts\Initialize-TravelMateLocalEnv.ps1
```

脚本只填写空白配置项，不覆盖已有本机配置，也不会打印生成的密钥。根目录 `.env` 始终只用于本机开发和 Docker Compose，并由 Git 忽略。

手工启动时，后端主类是 `com.travelmate.TravelMateApplication`：

```powershell
Set-Location backend
.\mvnw.cmd spring-boot:run
```

```powershell
Set-Location frontend
npm install
npm run dev
```

前端开发服务器默认使用 3000 端口，并将 `/api`、`/user` 和 `/uploads` 代理到 `http://localhost:8080`。需要调整时使用 `.env` 中的 `VITE_DEV_PORT` 和 `VITE_DEV_BACKEND_URL`。

### Docker Compose 本地联调

根目录 [`compose.yml`](compose.yml) 包含前端、后端、MySQL、Redis 和 MinIO。它复用本机 `.env`，图片进入 MinIO，MySQL 与 MinIO 数据分别保存在 named volume：

```powershell
.\scripts\Initialize-TravelMateLocalEnv.ps1
docker compose config
docker compose up -d --build
docker compose ps
```

默认入口为 <http://localhost:3000>，MinIO API 为 <http://localhost:9000>，管理控制台只绑定本机 `127.0.0.1:19001`。端口可通过 `MINIO_API_PORT`、`MINIO_CONSOLE_PORT` 调整。`docker compose down` 保留数据；只有明确执行 `docker compose down -v` 才会删除本地数据卷。

将旧本地 `uploads` 文件迁移到 S3/MinIO 前，先生成计划；确认映射后再执行上传与逐文件 SHA-256 回读校验：

```powershell
.\scripts\Migrate-TravelMateUploadsToS3.ps1
.\scripts\Migrate-TravelMateUploadsToS3.ps1 -Execute
```

报告输出到已忽略的 `backups/storage-migrations/`。脚本不会自动改写数据库字段；`mapping.csv` 同时记录旧 URL 和稳定 object key，数据库引用必须在明确对应业务字段后单独迁移。

服务器模板为 [`compose.server.yml`](compose.server.yml)，配置模板为 [`.env.server.example`](.env.server.example)。服务器必须使用实际 HTTPS 域名、外部 S3 兼容存储和前后端镜像 digest；数据库、Redis 与管理端口不得直接暴露公网。

## 账号与权限

### 测试账号（本地演示用）

按下方任一方式完成数据库初始化后，可直接使用以下种子账号登录验证：

| 用户名 | 密码 | 角色 | 说明 |
| --- | --- | --- | --- |
| `admin` | `123456` | 管理员 | Flyway V8 迁移恢复的默认管理员，可访问 `/admin` 管理后台 |
| `test` / `alice` / `bob` | `123456` | 普通用户 | 演示账号，附带游记、评论等社区种子数据 |

- 上述密码均为本地演示专用；首次登录后建议立即修改，不要在任何环境共用。
- 普通用户也可以直接在登录页注册新账号，后端始终按普通用户角色创建。

### 账号与安全机制

- 普通用户可直接在登录页注册，后端始终按普通用户角色创建账号。
- 管理员初始化默认关闭。只有 `ADMIN_REGISTER_ENABLED=true`、`ADMIN_REGISTER_EXPIRES_AT` 处于有效期内、系统中不存在有效管理员且密钥正确时，初始化入口才允许创建首个管理员。密钥只保存指纹、成功后只能使用一次，所有尝试都会写入审计表；完成后应立即关闭入口。
- 管理端 `/admin` 同时受前端路由守卫与后端 `/api/admin/**` 的 `ROLE_ADMIN` 校验保护。
- access token 默认有效 30 分钟，并携带 token 版本；改密、注销或管理员禁用账号后旧 token 失效，后端以数据库当前角色为准。
- refresh token 默认有效 14 天，只通过 `HttpOnly`、`SameSite=Lax`、`Path=/user` Cookie 传输，数据库仅保存 SHA-256 指纹；每次刷新都会轮换，旧值重放和退出登录后的值都会失效。服务器 HTTPS 环境必须设置 `REFRESH_COOKIE_SECURE=true`，刷新和退出接口继续受 CSRF 校验保护。
- 前端 access token 只保存在页面运行内存中，不写入 `localStorage`；页面刷新后由 refresh cookie 静默恢复，并重新从 `/user/me` 获取角色与账号状态。
- 无身份核验的自助密码重置已关闭；用户仍可在登录后通过旧密码修改自己的密码。
- 不要依赖 SQL 种子账号的注释密码作为稳定凭据；如果本地种子账号无法登录，请注册新的本地账号，不要在 README 中共享真实密码。

## 外部数据与降级行为

- 火车搜索会在条件满足时读取 12306 公开余票，并把结果更新到本地车次表；只支持当天至未来 15 天。12306 不可达或没有匹配结果时，页面会说明状态并回退本地数据。
- 12306 集成只用于读取公开余票，不包含 12306 登录、真实购票、支付或退票。
- 航班、酒店、景点和旅游产品主要来自本地演示库，不代表实时商用库存或价格。
- DeepSeek 未配置、超时或响应无效时，AI 行程和客服会降级；天气与地点核验也会在外部 API 失败时返回可解释的兜底结果。
- 支付、出票、核销和退款是课程演示流程，不连接真实支付机构、航空公司、铁路账户或酒店 PMS。

## 测试与质量门禁

常用本地回归入口：

```powershell
.\scripts\run-tests.ps1
```

该脚本依次运行后端测试、前端依赖安装、生产构建、Mock Playwright 冒烟测试和启动脚本 DryRun。已有依赖或只想做较快检查时可使用：

```powershell
.\scripts\run-tests.ps1 -SkipInstall -SkipE2E
```

按模块单独验证：

```powershell
# 后端：测试、JaCoCo、SpotBugs 与打包门禁
Set-Location backend
.\mvnw.cmd verify

# 前端：锁定依赖、静态检查、生产审计与构建
Set-Location ..\frontend
npm ci
npm run lint
npm run audit:prod
npm run build
npx playwright test --reporter=list --workers=1

# 微服务接口覆盖门禁（回到仓库根目录）
Set-Location ..
npm run check:microservice-api

# 数据库迁移与 Kubernetes 部署配置门禁
npm run check:deployment
kubectl kustomize deploy/k8s | Out-Null
kubectl kustomize deploy/k8s-overlays/local | Out-Null
kubectl kustomize deploy/k8s-overlays/server | Out-Null
```

正式 CI/CD 位于 [`.github/workflows/ci.yml`](.github/workflows/ci.yml)：

- 所有提交都执行仓库清洁度、UC01—UC19 追溯、Flyway 迁移规则、Kubernetes 清单和部署脚本校验；
- Pull Request 按 backend、frontend、microservices 变更范围选择性执行构建与 E2E；合并到 `main` 后仍执行完整发布验收；
- 六个微服务使用一次 Maven reactor 完成测试与打包，避免在矩阵 Job 中重复构建共享 contract；PR 验证 Dockerfile，main 直接构建并发布最终镜像；
- 后端 CI 从空的 `travelmate` 数据库启动，由 Flyway 自动执行全部迁移，并核对迁移历史表的最新版本；
- 功能分支不再同时触发 push 与 Pull Request 两套重复流水线；手动运行仍执行完整验证；
- `main` 的代码 push 在质量与安全门禁成功后，继续制作镜像、部署 Kubernetes、执行健康检查并上传证据；
- Markdown 等纯文档改动跳过无关构建和部署，但仍经过总质量门禁。

安全流水线位于 [`.github/workflows/security.yml`](.github/workflows/security.yml) 与 [`.github/workflows/codeql.yml`](.github/workflows/codeql.yml)，覆盖密钥、Node.js 依赖、backend/六微服务 Maven 依赖，以及 backend、六微服务和 JavaScript/TypeScript 静态安全分析。CodeQL 仅在源码、依赖、部署脚本或工作流变化时触发，纯文档/证据更新不再重复分析。正式发布只等待一个统一安全门禁；main 的发布与部署不会因后续提交而在 rollout 中途取消。流水线证据记录规则见 [`05_management/pipeline-records/README.md`](05_management/pipeline-records/README.md)。

### 数据库版本迁移

数据库结构与种子数据由 Flyway 管理，迁移文件位于 [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration)。新迁移只能新增文件，命名格式为 `V<连续版本号>__<英文说明>.sql`；已经进入共享分支的迁移文件不要修改或删除。应用启动时默认自动迁移，可通过 `.env` 中的 `FLYWAY_ENABLED=false` 临时关闭，仅建议用于故障排查。

`V1__baseline_schema.sql` 用于新数据库初始化；已有旧数据库首次接入时会登记为 V1 基线，再执行后续增量迁移。原有 [`docs/sql/init.sql`](docs/sql/init.sql) 暂时保留给旧版初始化脚本和演示环境兼容使用，后续数据库变更以 Flyway 文件为准。

## 持续部署

合并到 `main` 的代码在同一个 [`.github/workflows/ci.yml`](.github/workflows/ci.yml) 中完成构建、测试、镜像制作、Kubernetes 部署和部署后健康检查。流水线使用 CI 已测试的 JAR 与前端 `dist` 构建镜像；镜像先发布为不可变的 `sha-<完整 commit>`，通过 Trivy 高危/严重漏洞扫描后，才推进 `main` 与 `deploy` 通道。自托管 Windows Runner 随后校验前后端 OCI commit 一致性，按仓库 digest 更新 Deployment，等待 rollout，并检查前端 `/healthz` 与后端 `/actuator/health/readiness`。失败时部署脚本恢复更新前镜像，流水线保留镜像扫描、digest、Kubernetes 状态和健康检查证据。

演示机使用 Docker Desktop Kubernetes，并运行带有 `self-hosted`、`Windows`、`X64`、`travelmate-deploy` 标签的 GitHub Actions Runner。正式资源清单（包括六个 HPA）统一位于 [`deploy/k8s`](deploy/k8s)，本机部署脚本说明位于 [`scripts/cd/README.md`](scripts/cd/README.md)。首次启用：

```powershell
.\scripts\cd\Configure-TravelMateGhcrCredential.ps1
.\scripts\cd\Initialize-TravelMateKubernetes.ps1
```

GHCR 包可保持私有：凭据脚本通过安全提示读取仅含 `read:packages` 的 classic PAT，验证镜像后配置 Docker 与 Kubernetes 拉取凭据，不把 Token 写入命令行、仓库或日志。初始化脚本会在本机生成并复用应用 Secret；自托管 Runner 必须以能够访问 Docker Desktop、Docker credential store 和 `docker-desktop` Kubernetes context 的当前用户运行。应用入口为 <http://localhost:30080>，部署日志位于 `%USERPROFILE%\TravelMateCD\deploy.log`。`Install-TravelMateDeploymentTask.ps1` 仅保留为 GitHub Runner 不在线时的可选本机轮询方案，不属于正式流水线。

### 初学者 CI/CD、Docker 与 Kubernetes 维护指南

#### 基本概念与发布流程

- **CI（持续集成）**：提交代码后自动构建并运行测试，阻止未通过质量门禁的代码进入部署阶段。
- **Docker**：把前端、后端及其运行环境制作成可重复部署的镜像。
- **Kubernetes**：根据镜像创建和管理 Pod、Deployment、Service、PVC 等运行资源。
- **CD（持续部署）**：CI 通过后自动发布镜像、更新 Kubernetes，并执行部署后健康检查。

本项目的标准发布路径是：

```text
创建开发分支
  → 修改代码并完成本地测试
  → 推送分支并创建 Pull Request
  → PR 执行 CI、依赖检查和安全扫描
  → 检查全部通过后合并到 main
  → 构建前后端 Docker 镜像
  → Trivy 扫描并推送 GHCR
  → self-hosted Runner 部署 Docker Desktop Kubernetes
  → 校验镜像 digest、Pod 状态和前后端健康接口
```

#### 日常开发与发布步骤

不要直接在 `main` 上开发。开始工作前先同步主分支并创建功能分支：

```powershell
git switch main
git pull
git switch -c feature/功能名称
```

修改完成后先检查改动，并按涉及的模块执行本地测试：

```powershell
git status
git diff

# 仓库级常用回归
.\scripts\run-tests.ps1

# Kubernetes 清单和部署配置检查
npm run check:deployment
kubectl kustomize deploy/k8s | Out-Null
```

确认无误后只暂存本次相关文件，再提交和推送：

```powershell
git add <本次修改的文件或目录>
git diff --cached
git commit -m "feat: 简要说明修改内容"
git push -u origin feature/功能名称
```

随后在 GitHub 创建 Pull Request。PR 检查全部通过后再合并到 `main`，不要为了部署而跳过失败的测试或安全门禁。

#### 什么情况下会自动部署

所有 push 都会触发 [TravelMate CI/CD](.github/workflows/ci.yml)，但只有合并到 `main` 且包含代码或部署相关改动时，才会制作镜像并部署。以下路径会被视为需要完整构建和测试：

```text
backend/
frontend/
microservices/
deploy/
docs/sql/
scripts/
.github/workflows/
package.json
package-lock.json
```

纯 Markdown 等文档改动会跳过镜像构建和 Kubernetes 部署，这是正常行为。需要验证完整流水线时，可以在 GitHub Actions 页面手动运行 `TravelMate CI/CD`；`workflow_dispatch` 会按代码改动处理并执行完整质量检查。

#### 部署电脑需要保持的状态

合并代码前确认部署电脑满足以下条件：

1. Docker Desktop 已启动并启用 Kubernetes；
2. `kubectl config current-context` 返回 `docker-desktop`；
3. GitHub Actions Runner 显示 Online，并具有 `self-hosted`、`Windows`、`X64`、`travelmate-deploy` 标签；
4. Runner 账号能够访问 Docker Desktop、Docker credential store 和 `%USERPROFILE%\.kube\config`；
5. GHCR 的只读拉取凭据处于有效期内。

首次使用按以下顺序配置：

```powershell
# 在 C:\actions-runner 中交互启动 Runner，窗口关闭后 Runner 会离线
.\run.cmd

# 在项目根目录配置 GHCR 和初始化 Kubernetes
.\scripts\cd\Configure-TravelMateGhcrCredential.ps1 -GitHubUsername <GitHub用户名>
.\scripts\cd\Initialize-TravelMateKubernetes.ps1
```

若部署机通过本机 HTTP 代理访问 GitHub，建议在 `C:\actions-runner\start-runner.ps1` 中统一设置代理后再启动 Runner，避免每次手动输入环境变量：

```powershell
$env:HTTP_PROXY = "http://127.0.0.1:7897"
$env:HTTPS_PROXY = "http://127.0.0.1:7897"
$env:NO_PROXY = "localhost,127.0.0.1"

& "$PSScriptRoot\run.cmd"
```

此时使用 `C:\actions-runner\start-runner.ps1` 启动；代理程序必须先运行，端口不同则按实际代理地址调整。这样 GitHub Actions Runner 下载 `codeload.github.com` 依赖和 workflow 中的 HTTPS Git 拉取都会使用代理。部署源码拉取已使用 HTTPS，并配置为 HTTP/1.1、低速中断、最多 3 次重试和 5 分钟步骤上限，以避免 SSH 443 传输停滞时长期占用部署任务。不要将该 Runner 改为 Windows 服务：Docker Desktop、`docker-desktop` Kubernetes context 和本机代理通常依赖当前用户会话。

GHCR Token 建议使用仅含 `read:packages`、设置了到期时间的 classic PAT。不要把 Token 放入命令参数、仓库文件、聊天记录或截图；Token 到期后重新运行凭据配置脚本。

#### 本地检查 Docker 与 Kubernetes 改动

修改 Dockerfile 后，可先在本地验证镜像是否能够构建：

```powershell
docker build -t travelmate-backend:local backend
docker build -t travelmate-frontend:local frontend
```

修改 `deploy/k8s` 后，应先渲染并检查清单：

```powershell
npm run check:deployment
kubectl kustomize deploy/k8s | Out-Null
```

不要长期使用 `kubectl edit` 直接修改集群中的 Deployment。正式配置必须写回 `deploy/k8s/`，否则下一次自动部署会覆盖手工修改。

正常情况下不需要手动部署。确需重新部署已经通过审批的 GHCR `deploy` 镜像时，可执行：

```powershell
.\scripts\cd\Deploy-TravelMate.ps1
```

该脚本会核对前后端镜像 commit，按不可变 digest 更新 Deployment，并在 rollout 或健康检查失败时尝试恢复更新前镜像。

#### 部署后验证

```powershell
kubectl --context docker-desktop -n travelmate get deployments
kubectl --context docker-desktop -n travelmate get pods

Invoke-WebRequest -UseBasicParsing http://127.0.0.1:30080/healthz |
  Select-Object StatusCode, Content

kubectl --context docker-desktop get --raw `
  "/api/v1/namespaces/travelmate/services/http:travelmate-backend:8080/proxy/actuator/health/readiness"
```

正常结果应满足：前后端 Deployment 达到期望副本数、Pod 为 `Running`、前端返回 `200 / ok`、后端返回 `{"status":"UP"}`。

#### 常见故障排查

| 现象 | 常见原因 | 处理方式 |
| --- | --- | --- |
| Deploy job 一直排队 | self-hosted Runner 离线或标签不匹配 | 启动 Runner，并在 GitHub 检查 `travelmate-deploy` 标签 |
| `ImagePullBackOff` | GHCR Token 过期、被撤销或无包读取权限 | 重新运行 `Configure-TravelMateGhcrCredential.ps1` |
| `CrashLoopBackOff` | 应用配置、数据库连接或启动过程失败 | 使用 `kubectl logs` 和 `kubectl describe pod` 查看原因 |
| Deployment 长时间未 Ready | readiness probe、镜像、数据库或资源不足 | 查看 rollout、Pod events 和容器日志 |
| Docker 镜像构建失败 | Dockerfile、依赖或构建上下文错误 | 先使用本地 `docker build` 复现 |
| Trivy 阶段失败 | 基础镜像或应用依赖存在高危漏洞 | 升级基础镜像或依赖后重新提交 |
| 文档提交没有执行部署 | 路径检测判定为纯文档变更 | 正常，无需处理 |

常用诊断命令：

```powershell
kubectl -n travelmate get pods
kubectl -n travelmate get events --sort-by=.lastTimestamp
kubectl -n travelmate describe pod <Pod名称>
kubectl -n travelmate logs deployment/travelmate-backend --tail=200
kubectl -n travelmate logs deployment/travelmate-frontend --tail=200
kubectl -n travelmate rollout status deployment/travelmate-backend
kubectl -n travelmate rollout status deployment/travelmate-frontend
```

处理故障时不要删除 PVC、Secret 或整个 `travelmate` namespace，除非已经确认数据可以丢失并明确执行重建。更完整的首次配置、回滚和部署脚本说明见 [`scripts/cd/README.md`](scripts/cd/README.md)。

### Kubernetes 数据备份与集群重建

Docker Desktop 的 `Reset cluster`、修改 Kind 节点数量、删除 `travelmate` Namespace 或删除 PVC，均可能永久删除 MySQL 与本地 MinIO 中的图片。Pod 重启和 Deployment 滚动更新通常不会删除 PVC，但不能把 PVC 当成唯一备份。

当前持久化数据包括：

| PVC | 用途 | 默认容量 | 风险 |
| --- | --- | ---: | --- |
| `mysql-data-travelmate-mysql-0` | MySQL 数据目录 | 5 GiB | 集群或 PV 被删除时可能丢失全部业务数据 |
| `minio-data-travelmate-minio-0`（local overlay） | MinIO 图片对象 | 5 GiB | 集群或 PV 被删除时可能丢失用户上传内容 |

基础清单中的 `travelmate-uploads` 只为旧部署兼容保留；local/server overlay 均已解除后端对该共享 PVC 的依赖。服务器环境应使用带版本控制、生命周期和独立备份策略的外部 S3，而不是跨节点共享 uploads 目录。

仓库中的 `deploy/k8s/*.yaml`、初始化脚本和部署脚本是环境的“重建说明书”，不包含 PVC 中的真实数据。因此，修改节点数量或重置集群前必须执行宿主机备份：

```powershell
.\scripts\cd\Backup-TravelMateKubernetes.ps1
```

默认输出目录为 `backups/kubernetes/travelmate-<时间戳>/`，并已通过 `.gitignore` 排除，不会进入 Git。每次完整备份包含：

- `mysql/travelmate.sql`：使用 `mysqldump` 生成的 MySQL 逻辑备份；
- `uploads/`：仅旧 `STORAGE_TYPE=local` 部署的上传文件；
- `objects/`：`STORAGE_TYPE=s3` 时通过 MinIO Client `mc mirror` 导出的 bucket 内容；
- `manifests/repository/`：执行备份时仓库中的 Kubernetes YAML；
- `manifests/live-resources-without-secrets.yaml`：不含 Secret 值的集群运行时资源快照；
- `metadata.json`：备份时间、Context、Namespace 和来源 Pod；
- `checksums.sha256`：全部备份文件的 SHA-256 完整性校验。

备份不会导出 `travelmate-secrets`、GHCR Token 或其他凭据明文。重建集群后需要重新配置 GHCR 凭据；`Initialize-TravelMateKubernetes.ps1` 会创建新的 MySQL、JWT 和管理员注册密钥。若需要 DeepSeek 功能，应通过安全渠道重新提供 API Key，不要写入仓库或备份目录。

新集群按以下顺序恢复：

```powershell
# 1. 重新配置私有 GHCR 镜像拉取凭据
.\scripts\cd\Configure-TravelMateGhcrCredential.ps1

# 2. 创建 Namespace、Secret，并应用对应环境 overlay
.\scripts\cd\Initialize-TravelMateKubernetes.ps1 -Environment local

# 3. 校验备份并恢复 MySQL 与对应文件存储
.\scripts\cd\Restore-TravelMateKubernetes.ps1 `
  -BackupDirectory .\backups\kubernetes\travelmate-<时间戳> `
  -ConfirmDataOverwrite
```

`Restore-TravelMateKubernetes.ps1` 会先校验 `checksums.sha256`，然后恢复 `travelmate` 数据库和对应文件存储，最后滚动重启后端。S3 模式要求宿主机安装 MinIO Client `mc`；外部 S3 需要给备份和恢复脚本传入宿主机可访问的 `-ObjectStorageEndpoint`。`-ConfirmDataOverwrite` 是强制保护开关，且 S3 恢复会删除目标 bucket 中备份不存在的对象；执行前必须确认 Context、Namespace、备份目录和 bucket 均正确。

恢复完成后执行：

```powershell
kubectl --context docker-desktop -n travelmate get pods
kubectl --context docker-desktop -n travelmate get pvc

Invoke-WebRequest -UseBasicParsing http://127.0.0.1:30080/healthz |
  Select-Object StatusCode, Content

kubectl --context docker-desktop get --raw `
  "/api/v1/namespaces/travelmate/services/http:travelmate-backend:8080/proxy/actuator/health/readiness"
```

验收标准：所有 Pod 为 `Running`、MySQL 与本地 MinIO PVC 为 `Bound`、前端 `/healthz` 返回 HTTP 200、后端 readiness 返回 `UP`，并抽查数据库业务记录和图片对象能够正常访问。更完整的脚本参数和注意事项见 [`scripts/cd/README.md`](scripts/cd/README.md)。

### Kubernetes 运行配置与敏感信息更新

> 本节只适用于 Docker Desktop Kubernetes 的 `travelmate` 命名空间。仓库根目录 `.env` 仅供 `start.ps1` 等本地开发脚本使用，**不会被已部署的 Kubernetes Pod 读取**。

运行配置分为两类：

| 配置位置 | 内容 | 修改方式 |
| --- | --- | --- |
| [`deploy/k8s/configmap.yaml`](deploy/k8s/configmap.yaml) → `travelmate-config` | 非敏感参数，如数据库地址、Redis 地址、对象存储类型、登录时效和 CORS 来源 | 修改 overlay 后通过配置应用脚本滚动更新 |
| Kubernetes Secret `travelmate-secrets` | `mysql-root-password`、`mysql-password`、`jwt-secret`、`admin-register-secret`、`deepseek-api-key` | 使用 `kubectl patch` 更新指定键，避免把值写入仓库 |

`travelmate-config` 会整体注入后端容器环境变量；Secret 中的 `mysql-password` 被注入后端的 `SPRING_DATASOURCE_PASSWORD`，其余密钥分别注入 `JWT_SECRET`、`ADMIN_REGISTER_SECRET` 与 `DEEPSEEK_API_KEY`。可安全检查 Secret 是否存在（命令不会打印真实值）：

Kubernetes 环境已分层：`deploy/k8s` 是兼容旧部署的基础清单，`deploy/k8s-overlays/local` 使用本地 MinIO 并解除后端对共享 uploads PVC 的挂载，`deploy/k8s-overlays/server` 使用外部 S3、ClusterIP、Ingress 和 PodDisruptionBudget。修改非敏感配置后使用以下脚本，脚本会计算配置 hash 并触发后端滚动更新：

```powershell
.\scripts\cd\Apply-TravelMateConfiguration.ps1 -Environment local
```

```powershell
kubectl describe secret travelmate-secrets -n travelmate
```

#### 修改 DeepSeek API Key（PowerShell）

以下流程通过交互方式输入密钥，并使用自动删除的临时 JSON 文件传给 `kubectl`，避免 PowerShell 破坏 JSON 格式，也避免密钥出现在命令历史中。不要把真实密钥写入 `.env.example`、部署 YAML、Git 提交、聊天记录或截图。

```powershell
# 1. 交互输入新 Key；输入内容不会显示在终端。
$secretInput = Read-Host '输入新的 DeepSeek API Key' -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secretInput)
$tempPatch = Join-Path $env:TEMP ("travelmate-deepseek-" + [guid]::NewGuid().ToString() + ".json")

try {
  $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
  @{ stringData = @{ 'deepseek-api-key' = $plain } } |
    ConvertTo-Json -Compress |
    Set-Content -LiteralPath $tempPatch -Encoding utf8 -NoNewline

  kubectl patch secret travelmate-secrets -n travelmate `
    --type=merge `
    --patch-file=$tempPatch

  if ($LASTEXITCODE -ne 0) {
    throw "Secret 更新失败；请勿重启后端。"
  }
}
finally {
  if ($bstr -ne [IntPtr]::Zero) {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
  }
  Remove-Item -LiteralPath $tempPatch -Force -ErrorAction SilentlyContinue
  Remove-Variable plain,secretInput,bstr,tempPatch -ErrorAction SilentlyContinue
}

# 2. Pod 仅在启动时读取环境变量，更新 Secret 后必须滚动重启后端。
kubectl rollout restart deployment/travelmate-backend -n travelmate
kubectl rollout status deployment/travelmate-backend -n travelmate
kubectl get pods -n travelmate
```

出现 `secret/travelmate-secrets patched`，并且 rollout 显示 `successfully rolled out` 后，新 Key 才已生效；两个 `travelmate-backend` Pod 应均为 `1/1 Running`。中间的 “new replicas have been updated” 和 “old replicas are pending termination” 是正常的滚动更新过程。

修改管理员注册密钥或 JWT 密钥时复用同一流程，只把脚本中的 `deepseek-api-key` 分别改为 `admin-register-secret` 或 `jwt-secret`。修改 `jwt-secret` 会让所有现有登录 Token 失效，用户需要重新登录。

启用管理员初始化时，`ADMIN_REGISTER_EXPIRES_AT` 必须填写 ISO-8601 UTC 时间，例如 `2026-09-01T03:00:00Z`；不要设置长期有效窗口。创建成功、过期或失败结果可在 `admin_bootstrap_audit` 中审计，但表中不会记录原始密钥。

> **数据库密码例外：** 不要仅更新 `mysql-password` 或 `mysql-root-password`。MySQL 数据卷中已保存 `travelmate` 和 `root` 用户的实际认证密码；只更新 Secret 会导致容器配置与数据库账号不一致，后端将无法连接数据库。

应用数据库密码使用专用脚本轮换。脚本先核对数据库与 Secret 是否一致，再修改 MySQL 用户、验证新密码、更新 Secret 并滚动两个后端副本；任一步失败会尽可能恢复原状态，且不会打印密码：

```powershell
# 交互输入新密码
.\scripts\cd\Rotate-TravelMateDatabasePassword.ps1

# 或生成新的强随机密码
.\scripts\cd\Rotate-TravelMateDatabasePassword.ps1 -Generate
```

应在维护窗口执行。完成后重启 MySQL Pod，使容器环境变量快照与 Secret 一致；该操作不会删除 PVC：

```powershell
kubectl delete pod travelmate-mysql-0 -n travelmate
kubectl wait --for=condition=Ready pod/travelmate-mysql-0 -n travelmate --timeout=180s
kubectl rollout status deployment/travelmate-backend -n travelmate
```

图片或种子数据有改动时，额外运行：

```powershell
npm run check:images
```

## 已知边界

- 当前是模块化单体；现有 Kubernetes/CD 面向单机 Docker Desktop 演示环境，不等同于生产级多节点容灾、备份和可观测平台。
- Redis 不可用时支持降级，但缓存、限流和高并发库存场景不能按完整状态验收。
- 管理后台的 QPS、延迟和告警数据源为本地 `sys_log` 轻量统计，不采用外部 APM 或分布式追踪数据。
- 用例追溯矩阵的 19 个场景均已具备至少一项自动化证据，当前均为 `partial`；新增功能应同步补测试，并逐步将场景提升为完整 `covered`。
- SQL 包含演示数据和部分外部图片/资料来源；课程验收结论不将这些外部素材视为生产授权或长期可用性保证。

## 项目结构

```text
Software_engineering/
├── backend/
│   ├── pom.xml                         # Spring Boot 依赖与质量门禁
│   └── src/
│       ├── main/java/com/travelmate/   # 控制器、服务、实体、Mapper、安全配置
│       └── test/java/com/travelmate/   # JUnit / MockMvc 测试
├── frontend/
│   ├── src/                            # Vue 页面、组件、路由、状态与请求封装
│   ├── tests/e2e/                      # Mock Playwright 测试
│   └── tests/e2e-real/                 # 真实后端 Playwright 测试
├── docs/
│   ├── sql/init.sql                    # 数据库结构与演示数据
│   └── ci/                             # 用例测试矩阵与质量策略
├── 02_docs/                           # 需求、用例、系统级/对象级模型与详细设计
├── scripts/                            # 测试、追溯与图片检查脚本
├── 05_management/pipeline-records/     # CI/CD 运行证据
├── .github/workflows/                  # CI、安全与 CodeQL
├── .env.example                        # 本地配置模板
├── setup.ps1                           # 数据库初始化
└── start.ps1 / start.bat               # Windows 一键启动
```

## 相关文档

- [软件需求规格说明书](02_docs/5组-软件需求规格说明.md)
- [软件详细设计说明](02_docs/5组-软件详细设计说明.md)
- [微服务改造中期验收基线](02_docs/TravelMate中期验收基线.md)
- [业务场景清单与用例说明](02_docs/业务场景清单与用例说明.md)
- [版本日志](CHANGELOG.md)
- [用例测试矩阵](docs/ci/use-case-test-matrix.json)
- [CI 质量策略](docs/ci/test-quality-policy.json)

---

本项目用于“软件工程基础 2026 春”课程实践，由 Miracle 小组开发与维护。
