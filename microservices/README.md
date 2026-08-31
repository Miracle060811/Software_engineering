# TravelMate 微服务迁移（第一阶段）

本目录是从现有 `backend` 模块化单体向微服务迁移的第一批可运行工程。单体仍保留，作为功能回归基线；新工程当前包含四个可独立构建、启动和制作镜像的业务服务。

| 服务 | 默认端口 | 数据所有权 | 当前跨服务调用 |
| --- | ---: | --- | --- |
| `identity-service` | 8081 | 用户、关注、常用旅客 | 提供旅客归属与必要快照内部接口 |
| `traffic-service` | 8082 | 航班、火车、候补、交通订单、价格历史、交通 Outbox | HTTP 调用身份服务校验旅客；HTTP 调用本地生活服务核销优惠券 |
| `local-service` | 8083 | 酒店、景点、目的地、评价、优惠券、本地游、本地生活 Outbox | 提供优惠券核销内部接口 |
| `ai-service` | 8084 | AI 行程、AI 对话、通知、私信、事件消费记录 | 幂等消费交通与本地生活服务的通知事件；提供通知查询与状态操作 |

内部接口使用 `X-Internal-Token`，四个服务必须配置同一个 `INTERNAL_SERVICE_TOKEN`。`/internal/**` 不参与面向浏览器的 CSRF 校验，但仍由各内部控制器校验服务 Token。JWT 密钥也必须一致，且 `JWT_SECRET` 为解码后至少 32 字节的 Base64 文本。

`traffic-service` 调用 IDENTITY 默认使用 1 秒连接超时和 2 秒读取超时，可通过 `IDENTITY_CONNECT_TIMEOUT_MS`、`IDENTITY_READ_TIMEOUT_MS` 调整。连接失败、读取超时或 IDENTITY 5xx 会返回 HTTP 503 和“身份服务暂不可用，请稍后重试”，订单事务不会进入库存扣减阶段。

## 构建

在本目录执行：

```powershell
..\backend\mvnw.cmd "-Dmaven.test.skip=true" clean package
```

构建成功后，四个可执行 JAR 分别位于 `services/<service>/target/`。每个服务目录均有独立 `Dockerfile`。

## API 与 UC01—UC19 测试

四个已实现服务均有独立的 public API MockMvc 契约测试；运行完整 Reactor 会同时执行 API、跨服务 Outbox 和幂等消费测试：

```powershell
mvn --batch-mode --no-transfer-progress verify
```

微服务 E2E 复用真实数据库版 UC01—UC19 场景，但通过 Vite 按路由转发到 8081—8086 的对应服务，不会访问 8080 单体后端：

```powershell
cd ..\frontend
npm run test:e2e:microservices
```

结构门禁 `npm run check:microservice-e2e` 会检查 19 个 UC、服务归属和路由均已登记。当前可执行状态以 `docs/ci/microservice-use-case-matrix.json` 为准；社区与运营服务及 AI 行程/对话/私信迁移完成前，完整 E2E 应失败，不能作为“19/19 已通过”的验收证据。

## 启动示例

数据库已按服务初始化后，先设置环境变量，再分别启动四个 JAR：

```powershell
$env:JWT_SECRET = "<Base64 密钥>"
$env:INTERNAL_SERVICE_TOKEN = "<随机内部服务密钥>"
$env:DB_PASSWORD = "<MySQL 密码>"

java -jar .\services\identity-service\target\identity-service-0.1.0-SNAPSHOT.jar
java -jar .\services\traffic-service\target\traffic-service-0.1.0-SNAPSHOT.jar
java -jar .\services\local-service\target\local-service-0.1.0-SNAPSHOT.jar
java -jar .\services\ai-service\target\ai-service-0.1.0-SNAPSHOT.jar
```

健康检查：

- `http://localhost:8081/actuator/health/liveness`
- `http://localhost:8082/actuator/health/liveness`
- `http://localhost:8083/actuator/health/liveness`
- `http://localhost:8084/actuator/health/liveness`

## 本地 Compose 联调

先构建四个 JAR、生成分库脚本，再复制环境变量模板并启动：

```powershell
..\backend\mvnw.cmd "-Dmaven.test.skip=true" package
..\scripts\Generate-MicroserviceSchemas.ps1
Copy-Item .env.example .env
docker compose --env-file .env -f compose.yml up --build
```

Compose 会创建四套独立 MySQL 数据卷、四个独立应用账号和一个 Redis，并加载按表归属拆分的本地 E2E 演示数据。默认主机端口为 3307、3308、3309、3310；这是本地联调配置，不包含生产级 Secret 管理，生产环境不得执行 `*-seed.sql`。

旧单体库的数据迁移步骤、空目标库保护、逐表行数校验和回滚方式见 [MIGRATION.md](MIGRATION.md)。迁移脚本默认只做 DryRun。

## 当前边界

- 四个 JAR 已按表归属排除跨域 Mapper；交通服务不会直接编译或打包乘车人、优惠券、通知 Mapper。
- 现阶段通过 Maven 的选择性源码编译复用单体中的领域代码，下一轮再把代码物理移动到各服务目录。
- 订单通知在订单事务内写入服务自己的 Outbox 表；定时投递器使用 `eventId` 作为 `Idempotency-Key` 调用 AI 服务 `/internal/notifications/events`，支持并发认领、指数退避、卡住认领恢复和死信。AI 消费端用 `tm_ai_consumed_event` 去重，并在同一事务内写通知。
- 默认数据库名分别为 `travelmate_identity`、`travelmate_traffic`、`travelmate_local`、`travelmate_ai`；DDL 与本地 E2E 种子数据可由 `scripts/Generate-MicroserviceSchemas.ps1` 按表归属从事实源自动生成。历史数据迁移工具已提供，但尚未对真实目标库执行迁移验收。
- AI 服务当前只完成通知消费、查询和状态操作切片；AI 行程、对话和私信接口仍需继续物理迁移。
- API Gateway、注册中心、配置中心及镜像编排属于后续阶段。

### Outbox 状态

| 状态 | 含义 | 后续动作 |
| ---: | --- | --- |
| 0 | 待投递 | 到达 `next_retry_time` 后重新认领 |
| 1 | 已投递 | 保留审计记录 |
| 2 | 死信 | 人工排查 AI 服务或负载后再重放 |
| 3 | 投递中 | 超过认领超时会自动恢复为待投递 |

独立 JAR 默认关闭投递器，避免未初始化 Outbox 表时误轮询；Compose 通过 `OUTBOX_DISPATCHER_ENABLED=true` 启用。AI 服务未启动时投递会退避重试，订单主事务不受影响。
