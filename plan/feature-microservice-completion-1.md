---
goal: Complete TravelMate UC01-UC19 microservice migration and real acceptance
version: 1.0
date_created: 2026-09-01
last_updated: 2026-09-01
owner: TravelMate Team
status: 'Partially completed'
tags: [feature, microservices, migration, testing, kubernetes]
---

# Introduction

![Status: Partially completed](https://img.shields.io/badge/status-Partially_completed-yellow)

本计划将原有 4 个可运行服务扩展为 6 个业务服务，补齐 `community-service`、`ops-service` 以及 `ai-service` 的剩余切片，使 `docs/ci/microservice-use-case-matrix.json` 中 UC01—UC19 全部达到 `runnable`，并用真实 Compose、Playwright、数据迁移与 Kubernetes/HPA 结果验收。

## 1. Requirements & Constraints

- **REQ-001**: 保留 `backend` 模块化单体作为回归基线，不删除或改写现有单体接口。
- **REQ-002**: `community-service` 使用 8085，拥有帖子、评论、点赞/收藏数据及相关社区行为。
- **REQ-003**: `ops-service` 使用 8086，提供管理员用户、订单、内容审核、敏感词和操作日志接口；业务表继续由原业务服务唯一拥有，运营服务必须通过内部 HTTP 契约访问，不得跨库 Mapper 查询。
- **REQ-004**: `ai-service` 补齐 AI 行程、AI 对话和私信接口，继续拥有 AI 与消息数据。
- **REQ-005**: `docs/ci/microservice-use-case-matrix.json` 的 19 个 UC 最终全部设为 `runnable`，且结构门禁通过。
- **SEC-001**: 所有 `/internal/**` 接口校验 `X-Internal-Token`；浏览器接口继续校验共享 JWT、角色与资源归属。
- **SEC-002**: Docker 基础镜像必须合入 `origin/main` 的固定版本与 digest，不使用 `latest`。
- **CON-001**: 所有服务源码必须位于 `microservices/services/<service>`，不得通过 Maven 引入 `backend/src/main/java`。
- **CON-002**: 每张表只能被一个服务的 Mapper 直接访问，跨服务操作必须使用内部 API 或事件。
- **CON-003**: 不把 `check:microservice-e2e` 的结构覆盖当作真实 E2E 通过。
- **CON-004**: 不重置现有 Docker 数据卷；真实验收使用独立 Compose 数据卷和服务本地种子数据。
- **GUD-001**: 先迁移单体中已有 Controller、Service、Entity、Mapper，再做必要的服务边界重构。
- **PAT-001**: 复用现有 `identity-service`、`traffic-service`、`local-service`、`ai-service` 的配置、安全、异常处理、POM、Dockerfile 和 MockMvc 测试模式。

## 2. Implementation Steps

### Implementation Phase 1

- GOAL-001: 同步分支并冻结服务、表和接口边界。

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-001 | 将 `origin/main` 合入 `codex/feature/microservice-k8s-hpa`，保留 HPA 实现并合入四个 Dockerfile 的 digest 修复；运行 `git diff --check`。 | ✅ | 2026-09-01 |
| TASK-002 | 更新 `microservices/README.md` 与 `microservices/MIGRATION.md`，冻结 COMMUNITY、OPS、AI 剩余数据所有权和内部 HTTP 接口。 | ✅ | 2026-09-01 |
| TASK-003 | 更新 `scripts/check-microservice-boundaries.mjs`，使门禁识别 6 个服务且拒绝 OPS 跨库 Mapper。 | ✅ | 2026-09-01 |

### Implementation Phase 2

- GOAL-002: 实现 COMMUNITY 服务并解除社区相关 UC 阻塞。

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-004 | 新建 `microservices/services/community-service`，迁移 `PostController`、`CommentController`、`LikeController` 及对应 Service、Entity、Mapper，启动端口为 8085。 | ✅ | 2026-09-01 |
| TASK-005 | 为用户公开资料与帖子归属增加 COMMUNITY→IDENTITY 内部契约；不得直接读取 `tm_user` 或 `tm_follow`。 | ✅ | 2026-09-01 |
| TASK-006 | 增加 `CommunityPublicApiTests` 和内部调用测试，覆盖 UC14、UC15、UC17、UC19 社区侧接口与越权场景。 | ✅ | 2026-09-01 |

### Implementation Phase 3

- GOAL-003: 实现 OPS 服务并解除运营相关 UC 阻塞。

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-007 | 新建 `microservices/services/ops-service`，迁移管理员用户、订单聚合、内容审核、敏感词和操作日志接口，启动端口为 8086。 | ✅ | 2026-09-01 |
| TASK-008 | 在 IDENTITY、TRAFFIC、LOCAL、COMMUNITY 增加最小管理员内部接口；OPS 仅聚合响应并发起审核命令，不直接引用其他服务 Mapper。 | ✅ | 2026-09-01 |
| TASK-009 | 增加 `OpsPublicApiTests`、RBAC 测试及跨服务失败隔离测试，覆盖 UC09、UC14、UC18、UC19。 | ✅ | 2026-09-01 |

### Implementation Phase 4

- GOAL-004: 补齐 AI 行程、对话和私信。

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-010 | 向 `microservices/services/ai-service` 迁移 `AiPlan`、`AiChat`、`PrivateMessage` 的 Controller、Service、Entity、Mapper 与 DTO。 | ✅ | 2026-09-01 |
| TASK-011 | 在无外部 AI 密钥时保持现有确定性本地生成策略，保证 UC11/UC12 可重复测试；私信写入和未读状态必须校验用户身份。 | ✅ | 2026-09-01 |
| TASK-012 | 扩展 AI MockMvc 测试，覆盖 UC11、UC12、UC13 的持久化、多轮对话、私信与越权场景。 | ✅ | 2026-09-01 |

### Implementation Phase 5

- GOAL-005: 完成 6 服务运行、数据与交付编排。

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-013 | 在 `microservices/pom.xml`、`microservices/compose.yml`、`.env.example` 中加入 COMMUNITY/OPS 服务及独立 MySQL，默认数据库端口为 3311/3312。 | ✅ | 2026-09-01 |
| TASK-014 | 扩展 `scripts/Generate-MicroserviceSchemas.ps1` 和 `microservices/sql/*`，生成 6 套唯一归属 DDL/seed，并扩展迁移 DryRun/行数校验。 | ✅ | 2026-09-01 |
| TASK-015 | 更新 Vite 微服务代理、Playwright 配置和 `docs/ci/microservice-use-case-matrix.json`，将验证通过的 UC 改为 `runnable`。 | ✅ | 2026-09-01 |
| TASK-016 | 扩展 `.github/workflows/ci.yml`、GHCR 镜像矩阵、`microservices/k8s/`、部署脚本和 HPA 清单到 6 服务。 | ✅ | 2026-09-01 |

### Implementation Phase 6

- GOAL-006: 完成自动化和真实运行验收。

| Task | Description | Completed | Date |
|------|-------------|-----------|------|
| TASK-017 | 运行 `mvn -f microservices/pom.xml clean verify`、`check:microservice-boundaries`、`check:microservice-e2e`、`check:deployment` 和 `git diff --check`。 | ✅ | 2026-09-01 |
| TASK-018 | 在独立 Compose 环境启动 6 服务、6 个 MySQL 和 Redis，逐个验证 liveness/readiness 与跨服务失败隔离。 | ✅ | 2026-09-01 |
| TASK-019 | 运行 `npm run test:e2e:microservices`，记录 17 个 Playwright 测试的通过数、失败标题和日志，目标为 17/17。 | ✅ | 2026-09-01 |
| TASK-020 | 对临时目标库执行迁移 DryRun、实际迁移、逐表行数核对和核心业务冒烟测试，不接触现有本地业务库。 | ⚠️ DryRun 已通过；Docker daemon 重启后临时源/目标容器不可稳定监听端口，实际迁移未完成 | 2026-09-01 |
| TASK-021 | 在可用 Kubernetes context 与 Metrics Server 上部署 6 服务并运行 HPA 实验，保存 `04_tests/stress/results/hpa-*` 原始证据。 | ⛔ 当前没有 kubectl context；Metrics API 不可连接，未执行实机 HPA | 2026-09-01 |

## 3. Alternatives

- **ALT-001**: 保持 4 服务并让 AI/LOCAL 承担社区与运营接口；拒绝，因为会破坏已冻结的 8085/8086 路由和数据边界。
- **ALT-002**: OPS 直接连接所有业务数据库；拒绝，因为会破坏表唯一所有权并放大故障范围。
- **ALT-003**: 只修改用例矩阵为 runnable；拒绝，因为结构登记不等于 Controller、数据库和真实 E2E 已完成。

## 4. Dependencies

- **DEP-001**: JDK 21、Maven Wrapper、Spring Boot 3.5.15、MyBatis-Plus 3.5.7。
- **DEP-002**: Docker Desktop、MySQL 8.4、Redis 7.4。
- **DEP-003**: Node.js、Playwright Chromium。
- **DEP-004**: Kubernetes context、Metrics Server 与 k6，仅 TASK-021 需要。

## 5. Files

- **FILE-001**: `microservices/services/community-service/**`，新增社区服务。
- **FILE-002**: `microservices/services/ops-service/**`，新增运营服务。
- **FILE-003**: `microservices/services/ai-service/**`，补齐 AI/私信切片。
- **FILE-004**: `microservices/pom.xml`、`microservices/compose.yml`、`microservices/.env.example`，6 服务运行编排。
- **FILE-005**: `microservices/sql/**`、`scripts/Generate-MicroserviceSchemas.ps1`、`microservices/MIGRATION.md`，分库与迁移。
- **FILE-006**: `frontend/vite.config.js`、`frontend/playwright.microservices.config.js`、`docs/ci/microservice-use-case-matrix.json`，路由和 E2E 状态。
- **FILE-007**: `.github/workflows/ci.yml`、`microservices/k8s/**`、`scripts/cd/Deploy-Microservices.ps1`，交付编排。
- **FILE-008**: `microservices/README.md`、根 `README.md`，完成状态和验收命令。

## 6. Testing

- **TEST-001**: 每个服务至少包含 public API MockMvc 契约测试、鉴权/越权测试和内部 Token 测试。
- **TEST-002**: COMMUNITY→IDENTITY、OPS→各业务服务、TRAFFIC→IDENTITY/LOCAL 的跨服务失败统一返回 503，且不得产生部分写入。
- **TEST-003**: Maven Reactor 所有模块测试 0 failures、0 errors。
- **TEST-004**: `check:microservice-e2e` 输出 `Runnable now: 19; blocked by service migration: 0`。
- **TEST-005**: Playwright 微服务真实 E2E 17/17 通过，并保存 HTML/report/artifact。
- **TEST-006**: Kubernetes 所有 Deployment 可用、Pod 重启数为 0，HPA 产生扩容和回落时间序列。

## 7. Risks & Assumptions

- **RISK-001**: `AdminController` 同时访问多个业务域；迁移时必须拆成 OPS 聚合器与各服务内部适配器，不能整文件复制。
- **RISK-002**: 当前分支与 `origin/main` 各有独立提交；合并冲突必须保留 Docker digest 修复和 HPA/部署实现。
- **RISK-003**: 真实迁移和 HPA 需要外部运行环境；缺少 context 或 Metrics Server 时只能标记为阻塞，不能报告通过。
- **RISK-004**: Compose 全量 E2E 会创建独立数据卷；不得复用项目现有 MySQL 密码或数据库。
- **ASSUMPTION-001**: 单体行为是迁移的功能事实源，UC 定义以 `document/业务场景清单与用例说明.md` 为准。

## 8. Related Specifications / Further Reading

- `document/业务场景清单与用例说明.md`
- `docs/ci/microservice-use-case-matrix.json`
- `microservices/README.md`
- `microservices/MIGRATION.md`
- `microservices/k8s/README.md`
