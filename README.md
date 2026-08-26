# TravelMate（伴游）出行旅游平台

TravelMate 是 Miracle 小组的软件工程课程项目，围绕“行前规划—资源预订—行中服务—行后分享”提供一体化旅行体验。当前仓库是一套可本地运行、可自动化验证的前后端单体应用，不是接入真实支付、出票或商用库存的生产 OTA。

## 当前能力

| 领域 | 已实现能力 |
| --- | --- |
| 出行资源 | 航班搜索、火车票搜索与中转建议、12306 公开余票读取、酒店/房型、景点门票、一日游与目的地资料 |
| 订单履约 | 多乘客/多间房下单、模拟支付、取消、退款申请与审批、交通订单人工出票、库存扣减与回补、订单详情与凭证 |
| AI 服务 | DeepSeek 行程规划与多轮客服、地点有效性核验、天气工具调用；外部服务不可用时回退本地模板 |
| 社区与用户 | 注册登录、个人主页、游记草稿与审核、点赞/收藏/评论、关注关系、私信、通知、优惠券与浏览记录 |
| 管理后台 | RBAC、用户/资源/订单/优惠券管理、CSV 导入、内容与评价审核、系统日志和轻量运行指标 |
| 工程质量 | 后端 JUnit/MockMvc、前端 ESLint/构建、Mock 与真实后端 Playwright E2E、JaCoCo、SpotBugs、依赖/密钥扫描及 CodeQL |

系统目前处于“核心主链路可运行，测试覆盖持续补齐”的阶段。UC01—UC19 的证据基线由 [`docs/ci/use-case-test-matrix.json`](docs/ci/use-case-test-matrix.json) 与 [`docs/ci/test-quality-policy.json`](docs/ci/test-quality-policy.json) 管理，不能把“代码已存在”直接视为“场景已被完整自动化覆盖”。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite 8、JavaScript、Element Plus、Pinia、Vue Router、Axios、ECharts |
| 后端 | Java 21、Spring Boot 3.5.13、Spring Security、MyBatis-Plus 3.5.7、Maven Wrapper |
| 数据 | MySQL 8.0、Redis 6/7（缓存、限流与库存辅助） |
| 认证 | JWT（jjwt 0.11.5）+ BCrypt + 基于角色的访问控制 |
| 外部能力 | DeepSeek API、12306 公开余票接口、OpenStreetMap Nominatim、Open-Meteo |
| CI/CD | GitHub Actions、Playwright、JaCoCo、SpotBugs、Gitleaks、Trivy、CodeQL |

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

编辑 `.env`，至少正确填写 MySQL 密码：

```dotenv
DB_PASSWORD="你的 MySQL root 密码"
DEEPSEEK_API_KEY=""
ADMIN_REGISTER_SECRET="请替换为本地强随机值"
```

- `DEEPSEEK_API_KEY` 可留空；行程规划和客服会使用本地降级结果。需要真实 AI 响应时再填入有效密钥。
- `ADMIN_REGISTER_SECRET` 仅用于创建管理员账号，应替换模板值且不得提交。
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

前端开发服务器固定使用 3000 端口，并将 `/api`、`/user` 和 `/uploads` 代理到 `http://localhost:8080`。

## 账号与权限

- 普通用户可直接在登录页注册，后端始终按普通用户角色创建账号。
- 管理员需在登录页的管理员注册入口中提供 `.env` 配置的 `ADMIN_REGISTER_SECRET`。
- 管理端 `/admin` 同时受前端路由守卫与后端 `/api/admin/**` 的 `ROLE_ADMIN` 校验保护。
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

# 用例追溯门禁（回到仓库根目录）
Set-Location ..
npm run check:traceability
```

正式 CI 位于 [`.github/workflows/ci.yml`](.github/workflows/ci.yml)：

- 所有提交都执行仓库清洁度与 UC01—UC19 追溯校验；
- 代码、SQL、脚本或工作流变更执行后端 `verify`、前端 lint/审计/构建和 Mock E2E；
- `main`、面向 `main` 的 Pull Request 和手动运行还执行真实后端 E2E；
- Markdown 等纯文档改动跳过无关构建，但仍经过总质量门禁。

安全流水线位于 [`.github/workflows/security.yml`](.github/workflows/security.yml) 与 [`.github/workflows/codeql.yml`](.github/workflows/codeql.yml)，覆盖密钥、依赖漏洞和 Java/JavaScript 静态安全分析。流水线证据记录规则见 [`05_management/pipeline-records/README.md`](05_management/pipeline-records/README.md)。

图片或种子数据有改动时，额外运行：

```powershell
npm run check:images
```

## 已知边界

- 当前是模块化单体，不是微服务架构，也没有生产级容器编排与发布流程。
- Redis 不可用时支持降级，但缓存、限流和高并发库存场景不能按完整状态验收。
- 管理后台的 QPS、延迟和告警来自本地 `sys_log` 的轻量统计，尚未接入真实 APM 或分布式追踪。
- 用例追溯矩阵目前仍包含 `partial` 与 `planned` 项；新增功能应同步补测试并收紧质量策略。
- SQL 包含演示数据和部分外部图片/资料来源；对外发布前仍需复核授权、时效性与稳定性。

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
├── document/                           # 详细设计与业务用例文档
├── scripts/                            # 测试、追溯与图片检查脚本
├── 05_management/pipeline-records/     # CI/CD 运行证据
├── .github/workflows/                  # CI、安全与 CodeQL
├── .env.example                        # 本地配置模板
├── setup.ps1                           # 数据库初始化
└── start.ps1 / start.bat               # Windows 一键启动
```

## 相关文档

- [软件需求规格说明书](5组-软件需求规格说明书.md)
- [软件详细设计说明](document/5组-软件详细设计说明.md)
- [业务场景清单与用例说明](document/业务场景清单与用例说明.md)
- [版本日志](CHANGELOG.md)
- [用例测试矩阵](docs/ci/use-case-test-matrix.json)
- [CI 质量策略](docs/ci/test-quality-policy.json)

---

本项目用于“软件工程基础 2026 春”课程实践，由 Miracle 小组开发与维护。
