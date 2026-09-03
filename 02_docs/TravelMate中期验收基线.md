# TravelMate 微服务改造中期验收基线

> 验收目标：微服务划分图、服务接口清单、数据表归属方案已经完成。  
> 基线日期：2026-08-27

## 1. 当前情况与中期结论

TravelMate 以单个 `backend` Maven 工程作为现有功能回归基线；在中期设计冻结后，已经新增 `microservices` 多模块工程，第一批 `SVC-IDENTITY`、`SVC-TRAFFIC`、`SVC-LOCAL`、`SVC-AI` 可独立构建、启动和制作镜像。中期时点仍是渐进式迁移：四服务分库 DDL、本地 Compose、事务 Outbox 写入/重试投递、AI 通知幂等消费和数据迁移工具已完成，新服务仍选择性复用单体领域源码；当时真实数据切换、AI 其余业务、API Gateway 与生产部署编排仍在后续阶段，最终验收状态以 9 月 3 日材料为准。

本次中期阶段已经形成以下三项可验收成果：

| 验收项 | 状态 | 交付证据 |
| :--- | :---: | :--- |
| 微服务划分图 | 已完成 | [TravelMate 服务划分图](./中期验收/TravelMate服务划分图.svg)及其 [Mermaid 源文件](./中期验收/TravelMate服务划分图.mmd) |
| 服务接口清单 | 已完成 | 本文第 3 节；完整现有端点见《软件详细设计说明书》4.1.3 |
| 数据表归属方案 | 已完成 | 本文第 4 节；以 `docs/sql/init.sql` 为数据库事实源 |

中期三项设计交付已经完成，并已开始下一阶段实现。当前已有四个独立可执行 JAR 和四个 Dockerfile；六服务全部拆分、真实数据库权限验收、API Gateway、注册/配置中心及容器化流水线仍属于后续工作。

### 1.1 第一批实现进度

| 实施项 | 状态 | 证据 |
| :--- | :---: | :--- |
| 四服务独立 Maven 构建 | 已完成 | `microservices/pom.xml` 及四个服务 `pom.xml` |
| 独立健康检查与镜像入口 | 已完成 | 四个服务的 `application.yml`、`Dockerfile` |
| 旅客跨域访问 | 已完成 | TRAFFIC 通过 IDENTITY 内部 HTTP 接口取得归属与订单所需快照 |
| 优惠券跨域访问 | 已完成 | TRAFFIC 通过 LOCAL 内部 HTTP 接口核销，不读取优惠券表 |
| 通知失败策略 | 已完成（代码级） | 同事务写 Outbox，投递器支持认领、指数退避和死信；AI 消费端按事件 ID 幂等并以事务写通知 |
| 独立 Schema 脚本 | 已完成 | 从 `docs/sql/init.sql` 自动生成四份 DDL，并校验缺表和重复归属 |
| 历史数据迁移工具 | 已完成 | 默认 DryRun、空目标库保护、逐表行数校验且不修改源库 |
| 真实数据切换验收 | 待完成 | 尚未在正式目标库执行迁移和业务冒烟测试 |

## 2. 服务划分方案

不按用例数量机械拆分，而按业务能力、事务边界、数据所有权和变化原因划分六个业务服务。API Gateway、注册中心、配置中心、前端和数据库不计入业务微服务数量。

![TravelMate 中期目标服务划分图](./中期验收/TravelMate服务划分图.svg)

| 服务编号 | 服务名称 | 核心职责 | 当前实现入口 | 拆分理由 |
| :--- | :--- | :--- | :--- | :--- |
| `SVC-IDENTITY` | 身份与用户服务 | 注册登录、账户安全、用户资料、关注关系、常用旅客 | `UserController`、`UserProfileController`、`FollowController`、`PassengerController` | 身份数据敏感且被多域复用，应形成稳定的认证与公开资料边界 |
| `SVC-TRAFFIC` | 交通与订单服务 | 航班、火车查询，候补，交通下单、支付、取消、退款和票据 | `FlightController`、`TrainController`、`TrafficOrderController`、`PriceHistoryController` | 交通资源与交通订单共享库存和状态机，属于同一强事务域 |
| `SVC-LOCAL` | 酒店景点与权益服务 | 酒店、房型库存、景点门票、目的地、评价、优惠券和本地游 | `HotelController`、`AttractionController`、`ReviewController`、`CouponController`、`TourProductController` | 本地资源、库存、订单和权益核销联动频繁，应保持本地事务 |
| `SVC-AI` | AI 与消息服务 | AI 行程、AI 对话、通知、私信 | `AiController`、`PrivateMessageController` | 外部 AI 依赖可降级，消息具有最终一致性，扩缩容特征与交易服务不同 |
| `SVC-COMMUNITY` | 社区内容服务 | 帖子、评论、点赞、收藏、图片素材 | `PostController`、`CommentController`、`LikeController`、`FileController` | 内容读多写少、审核异步化，数据模型和流量特征独立 |
| `SVC-OPS` | 运营管理服务 | 审核编排、跨域管理命令、敏感词、操作日志和运营指标 | `AdminController`、`SysLogAspect`、`SensitiveWordService` | 管理操作需要统一授权和审计，但不能因此取得其他服务数据表写权限 |

## 3. 服务接口清单

### 3.1 对外 REST API

下表是中期按服务归档后的接口清单。路径以当前 Controller 注解为准；`/**` 表示同一路由族下的详情或状态操作。

| 所属服务 | 方法 | 路径/路由族 | 主要能力 | 认证 |
| :--- | :--- | :--- | :--- | :--- |
| SVC-IDENTITY | POST/GET/DELETE | `/user/register`、`/user/login`、`/user/me`、`/user/password`、`/user/account` | 账号注册、登录、查询与安全操作 | 混合 |
| SVC-IDENTITY | GET/PUT | `/api/user/profile/**` | 用户公开资料查询与本人资料修改 | 需登录 |
| SVC-IDENTITY | GET/POST/DELETE | `/api/passenger/**` | 常用旅客管理 | 需登录 |
| SVC-IDENTITY | GET/POST | `/api/follow/**` | 关注、取关、粉丝与关注列表 | 需登录 |
| SVC-TRAFFIC | GET | `/api/flight/search`、`/api/flight/{id}` | 航班查询 | 公开 |
| SVC-TRAFFIC | GET/POST | `/api/train/search`、`/api/train/{id}`、`/api/train/transfer`、`/api/train/waitlist` | 火车查询、中转和候补 | 混合 |
| SVC-TRAFFIC | POST/GET | `/api/order/**` | 交通下单、支付、取消、退款、列表与凭证 | 需登录 |
| SVC-TRAFFIC | GET | `/api/price/trend` | 价格趋势 | 需登录 |
| SVC-LOCAL | GET/POST | `/api/hotel/**` | 酒店、房型、酒店订单、支付退款与凭证 | 混合 |
| SVC-LOCAL | GET/POST | `/api/attraction/**` | 景点查询、购票、订单与凭证 | 混合 |
| SVC-LOCAL | GET | `/api/destinations/**`、`/api/tour/list` | 目的地和本地游产品 | 混合 |
| SVC-LOCAL | GET/POST | `/api/review/**`、`/api/reply/**` | 评价、举报与商家回复 | 混合 |
| SVC-LOCAL | GET/POST | `/api/coupon/**` | 优惠券列表、领取与本人权益 | 混合 |
| SVC-AI | GET/POST | `/api/ai/plan/**`、`/api/ai/chat` | 行程生成、保存和 AI 对话 | 需登录 |
| SVC-AI | GET/POST/DELETE | `/api/notification/**` | 通知查询、已读、删除和清空 | 需登录 |
| SVC-AI | GET/POST | `/api/private-message/**` | 联系人、会话、发送和未读数 | 需登录 |
| SVC-COMMUNITY | GET/POST/PUT/DELETE | `/api/post/**` | 内容流、发布、编辑、删除和关注流 | 混合 |
| SVC-COMMUNITY | GET/POST/DELETE | `/api/comment/**`、`/api/like/**` | 评论、点赞和收藏 | 需登录 |
| SVC-COMMUNITY | POST | `/api/file/upload` | 图片上传并返回素材引用 | 需登录 |
| SVC-OPS | GET/POST/PUT/DELETE | `/api/admin/**` | 资源管理、订单审核、内容审核、用户管理、日志与敏感词 | 管理员 |

### 3.2 拆分后内部接口与事件

| 契约编号 | 调用方 → 被调用方 | 接口或事件 | 一致性与失败处理 |
| :--- | :--- | :--- | :--- |
| `INT-01` | TRAFFIC/LOCAL → IDENTITY | `GET /internal/users/{id}/status`、`GET /internal/users/{id}/passengers/{passengerId}` | 只读强校验；失败即拒绝下单，不扣库存 |
| `INT-02` | AI → TRAFFIC/LOCAL | 候选航班、车次、酒店和景点查询接口 | 2 秒超时、最多重试 1 次；失败时降级为不承诺实时库存的行程 |
| `EVT-01` | TRAFFIC/LOCAL → AI | `NotificationRequested` | 与业务事务同库写 Outbox；投递器携带 `Idempotency-Key` 重试，达到上限进入死信；AI 消费端按 `eventId` 幂等写通知 |
| `EVT-02` | COMMUNITY → OPS | `PostSubmitted` | 按 `postId` 幂等；审核失败时保持待审核并重试 |
| `INT-03` | OPS → COMMUNITY | 内容审核通过/拒绝命令 | 仅允许从待审核状态迁移；重复命令返回当前终态 |
| `INT-04` | OPS → TRAFFIC/LOCAL | 退款审核、资源管理命令 | 携带 `Idempotency-Key`；超时后先查状态再决定是否重试 |
| `INT-05` | COMMUNITY → IDENTITY | 用户公开资料、关注状态查询 | 隐私字段裁剪；失败时允许匿名化展示内容 |

统一约束：所有写请求携带 `X-Request-Id`；可重试写请求携带 `Idempotency-Key`；服务间不传递密码、手机号等无关敏感字段；统一使用 `Result<T>` 或在网关处转换为等价错误模型。

## 4. 数据表归属方案

采用“一表一主”的唯一写入责任。现阶段可在同一 MySQL 实例中用不同 Schema 或独立数据库账号隔离；不得跨服务直接联表或写表。

| 唯一写入责任服务 | 归属表 | 其他服务访问方式 |
| :--- | :--- | :--- |
| SVC-IDENTITY | `tm_user`、`tm_follow`、`tm_passenger` | JWT、用户公开资料接口、旅客归属校验接口 |
| SVC-TRAFFIC | `tm_flight`、`tm_train`、`tm_traffic_order`、`tm_train_waitlist`、`tm_price_history`、`tm_traffic_outbox_event` | 交通资源查询、订单查询与状态命令、可靠事件投递 |
| SVC-LOCAL | `tm_hotel`、`tm_hotel_room`、`tm_hotel_order`、`tm_attraction`、`tm_attraction_order`、`tm_destination`、`tm_review`、`tm_reply`、`tm_review_report`、`tm_tour_product`、`tm_tour_product_step`、`tm_coupon`、`tm_user_coupon`、`tm_local_outbox_event` | 本地资源查询、库存与订单命令、评价/权益接口、可靠事件投递 |
| SVC-AI | `tm_ai_plan`、`tm_ai_chat`、`tm_notification`、`tm_private_message`、`tm_private_contact`、`tm_ai_consumed_event` | 本人行程/会话接口、通知事件与查询接口 |
| SVC-COMMUNITY | `tm_post`、`tm_comment`、`tm_like`、`tm_media_asset` | 内容查询、审核状态命令、素材引用接口 |
| SVC-OPS | `sys_log`、`sys_sensitive_word` | 审计日志只读接口、敏感词校验接口 |

特别说明：

1. `tm_passenger` 归 SVC-IDENTITY，因为它是用户维护的身份资料；交通服务仅通过归属校验接口使用。
2. `tm_private_message`、`tm_private_contact` 归 SVC-AI，因为通知与私信共用消息域能力；社区服务不得直接读写。
3. `tm_review_report` 归 SVC-LOCAL，因为它与 `tm_review` 的举报处理构成同一业务聚合；SVC-OPS 只负责审核编排。
4. SVC-OPS 查询跨域运营数据时调用业务服务管理接口或消费统计事件，不能直接连接其他 Schema。
5. 历史订单保存必要资源快照，资源名称或价格变化不反向修改历史订单。

## 5. 中期验收演示顺序

1. 先说明现状：单体作为回归基线保留，四个服务模块已落地；中期设计已冻结，但不把尚未执行的真实分库切换和生产部署说成完成。
2. 展示服务划分图，说明六个服务均可形成独立 Maven 工程，且 API Gateway、数据库不计入业务服务数量。
3. 按第 3 节抽查 Controller 路由，证明每组接口已有明确归属。
4. 按第 4 节抽查 `docs/sql/init.sql`，证明每张表只有一个写入责任服务。
5. 用 `INT-01` 或 `EVT-01` 说明跨服务调用及失败策略，强调不能跨服务联表。

## 6. 中期之后的实施顺序

1. 先抽取公共契约：`Result<T>`、鉴权 Claims、Request-Id 和错误码；公共包不包含业务 Entity/Mapper。
2. 第一批拆出 SVC-IDENTITY、SVC-TRAFFIC、SVC-LOCAL，优先满足“至少 3 个业务微服务”。
3. 为每个服务建立独立 `pom.xml`、独立测试入口、独立 Dockerfile 和独立数据库账号/Schema。
4. 将原 Java 进程内调用替换为内部 REST 或事件，补齐超时、幂等、重试与补偿测试。
5. 引入 API Gateway，再补注册发现与配置管理；完成独立构建、测试、镜像和部署流水线。
6. 最后拆分 AI、社区与运营服务，并进行端到端回归和部署失败定位演示。

## 7. 事实源与一致性规则

- 接口事实源：`backend/src/main/java/com/travelmate/**/controller/*.java`。
- 表结构事实源：`docs/sql/init.sql`，Entity 的 `@TableName` 仅作交叉验证。
- 当前架构事实源：`backend/pom.xml` 和仓库目录结构。
- 完整接口、数据库、失败补偿设计：`02_docs/5组-软件详细设计说明.md` 第 4 章。
- 后续若 Controller 或 SQL 发生变化，必须同步更新本基线和服务划分图。
