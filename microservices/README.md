# TravelMate 微服务迁移（第一阶段）

本目录是从现有 `backend` 模块化单体向微服务迁移的可运行工程。单体仍保留，作为功能回归基线；当前工程包含六个可独立构建、启动和制作镜像的业务服务。

| 服务 | 默认端口 | 数据所有权 | 当前跨服务调用 |
| --- | ---: | --- | --- |
| `identity-service` | 8081 | 用户、关注、常用旅客 | 提供旅客归属与必要快照内部接口 |
| `traffic-service` | 8082 | 航班、火车、候补、交通订单、价格历史、交通 Outbox | HTTP 调用身份服务校验旅客；HTTP 调用本地生活服务核销优惠券 |
| `local-service` | 8083 | 酒店、景点、目的地、评价、优惠券、本地游、本地生活 Outbox | 提供优惠券核销内部接口 |
| `ai-service` | 8084 | AI 行程、AI 对话、通知、私信、事件消费记录 | 幂等消费交通与本地生活服务的通知事件；提供通知查询与状态操作 |
| `community-service` | 8085 | 帖子、评论、点赞/收藏 | 调用身份服务读取必要用户快照；调用运营服务执行敏感内容检查 |
| `ops-service` | 8086 | 敏感词、操作日志 | 通过各业务服务内部接口聚合管理列表并下发审核、退款等命令 |

内部接口使用 `X-Internal-Token`，六个服务必须配置同一个 `INTERNAL_SERVICE_TOKEN`。`/internal/**` 不参与面向浏览器的 CSRF 校验，但仍由各内部控制器校验服务 Token。JWT 密钥也必须一致，且 `JWT_SECRET` 为解码后至少 32 字节的 Base64 文本。

`traffic-service` 调用 IDENTITY 和 LOCAL 默认均使用 1 秒连接超时和 2 秒读取超时。可分别通过 `IDENTITY_CONNECT_TIMEOUT_MS`、`IDENTITY_READ_TIMEOUT_MS`、`LOCAL_CONNECT_TIMEOUT_MS`、`LOCAL_READ_TIMEOUT_MS` 调整。网络失败、读取超时、无法解析响应或依赖服务 5xx 会统一返回 HTTP 503；依赖服务返回的业务 4xx 保持原状态。IDENTITY 失败时订单事务不会进入库存扣减阶段，LOCAL 核销失败时本地订单事务回滚且不会写入订单。

### 冻结的内部接口与数据边界

| 调用方 | 被调用方 | 内部接口/事件 | 失败处理 |
| --- | --- | --- | --- |
| TRAFFIC | IDENTITY | `GET /internal/identity/passengers/{id}/ownership` | 连接、超时或 5xx 转 503；不扣交通库存 |
| TRAFFIC | LOCAL | `POST /internal/local/coupons/redeem` | 连接、超时或 5xx 转 503；交通订单事务回滚 |
| TRAFFIC、LOCAL | AI | `POST /internal/notifications/events`，`Idempotency-Key=eventId` | Outbox 保留并指数退避；达到上限进入死信 |
| COMMUNITY | IDENTITY | `GET /internal/identity/users/{id}/summary` | 连接、超时或 5xx 转 503；不直接读取 IDENTITY 数据库 |
| COMMUNITY | OPS | `POST /internal/ops/content/check` | OPS 不可用时拒绝发布或修改，不绕过敏感内容检查 |
| OPS | IDENTITY、TRAFFIC、LOCAL、COMMUNITY | `/internal/admin/**` 管理查询与命令 | 连接、超时或 5xx 转 503；OPS 不直接访问业务服务数据库 |

表归属保持唯一：IDENTITY 管理 `tm_user`、`tm_passenger`、`tm_follow`；TRAFFIC 管理航班、火车、交通订单、候补、价格历史和交通 Outbox 共 6 张表；LOCAL 管理酒店、景点、目的地、评价、举报、优惠券、本地游和本地 Outbox 共 14 张表；AI 管理行程、对话、通知、私信及消费去重共 6 张表；COMMUNITY 管理 `tm_post`、`tm_comment`、`tm_like`；OPS 管理 `sys_sensitive_word`、`sys_log`。完整表名以 `sql/*-schema.sql` 为准，六服务门禁应识别 35 张表中的 34 张；`tm_media_asset` 暂留单体文件域，待独立文件服务阶段处理。

## 构建

在仓库根目录执行：

```powershell
.\backend\mvnw.cmd -f .\microservices\pom.xml --batch-mode --no-transfer-progress clean verify
```

构建成功后，六个可执行 JAR 分别位于 `microservices/services/<service>/target/`。每个服务目录均有独立 `Dockerfile`。六个服务和 `travelmate-contract` 的源码均位于 `microservices` 内，POM 不再通过 `build-helper` 引入 `backend/src/main/java`；在完全不包含 `backend` 的临时目录中也可以完成同一条构建和测试命令。

## API 与 UC01—UC19 测试

六个服务均有独立的 public API MockMvc 契约测试；运行完整 Reactor 会同时执行 API、跨服务 Outbox 和幂等消费测试：

```powershell
mvn --batch-mode --no-transfer-progress verify
```

微服务 E2E 复用真实数据库版 UC01—UC19 场景，但通过 Vite 按路由转发到 8081—8086 的对应服务，不会访问 8080 单体后端：

```powershell
cd ..\frontend
npm run test:e2e:microservices
```

本地完整复现应先在仓库根目录构建并启动隔离的六服务 Compose，再在另一个终端运行 Playwright：

```powershell
.\backend\mvnw.cmd -f .\microservices\pom.xml --batch-mode --no-transfer-progress package -DskipTests
docker compose --env-file .\microservices\.env -f .\microservices\compose.yml up --build -d
Set-Location .\frontend
npm run test:e2e:microservices
```

2026-09-01 本地真实数据库结果为 17/17；CI 的 `Real microservice E2E` job 会下载同一提交中六个已测试 JAR，初始化六库与 Redis，保存 Playwright HTML、trace 和六服务日志到 `microservice-e2e-evidence-<commit>` Artifact。结构门禁 `npm run check:microservice-e2e` 同时检查 19 个 UC、服务归属、路由及 CI 接线；矩阵中的 CI 状态只有在远端 job 实跑后才能由 `configured_pending_run` 更新为通过。

服务边界门禁用于检查 POM 单体源码依赖、共享契约内容、跨服务 Mapper/表引用、34 张表唯一归属和六套数据库配置：

```powershell
npm run check:microservice-boundaries
```

## 启动示例

数据库已按服务初始化后，先设置环境变量，再分别启动六个 JAR：

```powershell
$env:JWT_SECRET = "<Base64 密钥>"
$env:INTERNAL_SERVICE_TOKEN = "<随机内部服务密钥>"
$env:DB_PASSWORD = "<MySQL 密码>"

java -jar .\services\identity-service\target\identity-service-0.1.0-SNAPSHOT.jar
java -jar .\services\traffic-service\target\traffic-service-0.1.0-SNAPSHOT.jar
java -jar .\services\local-service\target\local-service-0.1.0-SNAPSHOT.jar
java -jar .\services\ai-service\target\ai-service-0.1.0-SNAPSHOT.jar
java -jar .\services\community-service\target\community-service-0.1.0-SNAPSHOT.jar
java -jar .\services\ops-service\target\ops-service-0.1.0-SNAPSHOT.jar
```

健康检查：

- `http://localhost:8081/actuator/health/liveness`
- `http://localhost:8082/actuator/health/liveness`
- `http://localhost:8083/actuator/health/liveness`
- `http://localhost:8084/actuator/health/liveness`
- `http://localhost:8085/actuator/health/liveness`
- `http://localhost:8086/actuator/health/liveness`

## 本地 Compose 联调

先构建六个 JAR、生成分库脚本，再复制环境变量模板并启动：

```powershell
..\backend\mvnw.cmd "-Dmaven.test.skip=true" package
..\scripts\Generate-MicroserviceSchemas.ps1
Copy-Item .env.example .env
docker compose --env-file .env -f compose.yml up --build
```

Compose 会创建六套独立 MySQL 数据卷、六个独立应用账号和一个 Redis，并加载按表归属拆分的本地 E2E 演示数据。默认数据库主机端口为 3307—3312；这是本地联调配置，不包含生产级 Secret 管理，生产环境不得执行 `*-seed.sql`。

旧单体库的数据迁移步骤、空目标库保护、逐表行数校验和回滚方式见 [MIGRATION.md](MIGRATION.md)。迁移脚本默认只做 DryRun。

## 当前边界

- 六个 JAR 已完成当前业务切片的源码物理拆分；各服务只编译自己目录中的 Controller、Service、Entity 和 Mapper，不再从单体源码目录选择性编译。
- `travelmate-contract` 只保留 `Result`、`AuthenticatedUser` 以及旅客、优惠券、通知三个跨服务接口契约，不包含业务 Controller、Service 或 Mapper。
- 订单通知在订单事务内写入服务自己的 Outbox 表；定时投递器使用 `eventId` 作为 `Idempotency-Key` 调用 AI 服务 `/internal/notifications/events`，支持并发认领、指数退避、卡住认领恢复和死信。AI 消费端用 `tm_ai_consumed_event` 去重，并在同一事务内写通知。
- 默认数据库名分别为 `travelmate_identity`、`travelmate_traffic`、`travelmate_local`、`travelmate_ai`、`travelmate_community`、`travelmate_ops`；DDL 与本地 E2E 种子数据可由 `scripts/Generate-MicroserviceSchemas.ps1` 按表归属从事实源自动生成。历史数据迁移工具已提供，但尚未对真实目标库执行迁移验收。
- AI 服务已覆盖通知、行程、对话和私信切片；COMMUNITY 与 OPS 已按冻结边界完成源码、Compose、CI 与 Kubernetes 编排。
- API Gateway、注册中心、配置中心属于六服务业务迁移完成后的后续阶段。

### Outbox 状态

| 状态 | 含义 | 后续动作 |
| ---: | --- | --- |
| 0 | 待投递 | 到达 `next_retry_time` 后重新认领 |
| 1 | 已投递 | 保留审计记录 |
| 2 | 死信 | 人工排查 AI 服务或负载后再重放 |
| 3 | 投递中 | 超过认领超时会自动恢复为待投递 |

独立 JAR 默认关闭投递器，避免未初始化 Outbox 表时误轮询；Compose 通过 `OUTBOX_DISPATCHER_ENABLED=true` 启用。AI 服务未启动时投递会退避重试，订单主事务不受影响。

## Kubernetes、PVC 与 HPA

六个业务服务的 Kubernetes 清单位于 `microservices/k8s/`，与现有单体 `deploy/k8s/` 使用不同命名空间。配置包括：

- 六个业务服务的 Deployment、Service、启动/就绪/存活探针和 CPU/内存 requests/limits；
- 六套 MySQL StatefulSet，每套使用独立 5Gi PVC；
- Redis StatefulSet 与 1Gi PVC；
- 六个 `autoscaling/v2` HPA：`minReplicas=2`、`maxReplicas=6`、CPU 平均利用率 60%；
- 不落盘真实密钥的本地部署脚本和 HPA 实验脚本。

部署、验证和实验步骤见 `microservices/k8s/README.md`。
