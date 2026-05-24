# TravelMate（伴游）入职指南

## 项目概览

TravelMate 是一个全栈出行旅游平台，涵盖机票、火车票、酒店住宿、景点门票、AI 行程规划、社区论坛和管理后台七大功能模块。

| 维度 | 说明 |
|------|------|
| 项目名称 | TravelMate（伴游） |
| 技术栈 | Spring Boot 3.5 + Java 21 + MyBatis-Plus 3.5.7 + MySQL + Redis |
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Axios |
| 认证 | Spring Security + JWT（jjwt 0.11.5，HS256，24h 过期） |
| AI | DeepSeek API（OpenAI 兼容协议），超时自动降级兜底 |
| 分析版本 | commit `06cfe1ca`，2026-05-24 |

## 快速开始

```powershell
# 一键启动前后端
.\start.ps1

# 首次初始化数据库
.\setup.ps1 -InitDb

# 中文被导成?时重建
.\setup.ps1 -InitDb -ResetDb
```

- 后端：`http://localhost:8080`
- 前端：`http://localhost:3000`

---

## 架构层

### 1. API 层（21 个文件）

后端 REST API 控制器层。所有业务请求入口，委托给服务层处理。

**关键文件：**
- `FlightController.java` — 航班搜索、预订、价格趋势
- `TrainController.java` — 火车票搜索、预订
- `HotelController.java` — 酒店搜索、详情
- `AiController.java` — AI 行程规划生成、多轮对话
- `PostController.java` — 社区帖子 CRUD
- `AdminController.java` — 管理后台 REST 接口（仪表盘、资源 CRUD、CSV 导入、审核、举报工单、商家回复）
- `TrafficOrderController.java` — 订单创建、支付、取消

### 2. 服务层（43 个文件）

核心业务逻辑层，含 Service 接口、实现类和定时任务。

| 子系统 | 关键实现 |
|--------|----------|
| 大交通票务与订单 | `TrafficOrderServiceImpl`（大交通下单/支付/取消/退款）、`FlightServiceImpl`、`TrainServiceImpl`；订单库存管控策略统一协同机票、火车票、酒店等资源 |
| 目的地住宿与本地生活 | `HotelServiceImpl`、`HotelOrderServiceImpl`、`HotelRoomStockServiceImpl`、`AttractionServiceImpl`、`ReviewServiceImpl`；用户侧负责酒店/景点浏览和基础评价 |
| AI 智能规划与通知 | `AiServiceImpl`（DeepSeek 集成+Function Calling+JSON Output+降级兜底）、`NotificationCenterServiceImpl` |
| 社区与用户中心 | `PostServiceImpl`（帖子 CRUD+草稿+关注流+审核状态）、`CommentServiceImpl`、`LikeServiceImpl`、`FollowServiceImpl` |
| 管理后台与运营 | `AdminController` 聚合资源维护、CSV 导入、优惠券、审核、举报工单、商家回复、用户画像、可观测数据 |

**定时任务：**
- `OrderTimeoutScheduler` — 每 30 秒扫描超 15 分钟未支付订单，自动取消并归还库存
- `PostAuditScheduler` — 定时审核待审游记

### 3. 数据层（84 个文件）

实体类、Mapper 接口、DTO 和数据库表定义。

**核心实体（表前缀 `tm_`）：**
- 用户：`tm_user` — 用户认证与个人信息
- 大交通：`tm_flight`、`tm_train`、`tm_traffic_order`、`tm_passenger`、`tm_price_history`
- 目的地住宿与本地生活：`tm_hotel`、`tm_hotel_room`、`tm_hotel_order`、`tm_attraction`、`tm_review`
- 社区与用户中心：`tm_post`、`tm_comment`、`tm_like`、`tm_follow`
- AI：`tm_ai_plan`、`tm_ai_chat`
- 管理后台与运营：`tm_coupon`、`tm_user_coupon`、`tm_reply`、`tm_review_report`、`tm_notification`、`sys_log`、`sys_sensitive_word`

**防超卖机制：** 由 A 的订单库存管控模块统一设计，采用 Redis 预减 + 数据库原子更新/乐观校验 + 超时回滚，协同保障机票、火车票和酒店房型不超卖。

### 4. 基础设施与配置层（37 个文件）

| 组件 | 文件 | 职责 |
|------|------|------|
| 入口 | `TravelMateApplication.java` | Spring Boot 启动类 |
| 安全 | `SecurityConfig.java`、`JwtFilter.java`、`JwtUtil.java` | JWT 认证 + 路由权限 |
| 限流 | `RateLimiter.java`、`RateLimiterInterceptor.java` | Redis 接口限流（下单接口 5次/秒） |
| 日志 | `SysLogAspect.java` | AOP 自动记录 Controller 调用日志 |
| 异常 | `GlobalExceptionHandler.java` | 统一异常处理，中文错误提示 |
| 响应 | `Result.java` | 统一响应体 `code/msg/data` |
| 上下文 | `UserContext.java` | 从 SecurityContext 获取当前用户 |
| 配置 | `WebConfig.java`、`RedisConfig.java`、`application.yml` | 跨域、Redis、数据源 |
| 环境 | `DotenvEnvironmentPostProcessor.java` | 加载 `.env` 到环境变量 |

### 5. 前端 UI 层（39 个文件）

Vue 3 + Vite + Element Plus + Pinia。

**页面路由（21 条）：**
| 路由 | 页面 | 功能 |
|------|------|------|
| `/` | `Home.vue` | 首页三合一搜索+热门目的地 |
| `/login` | `Login.vue` | 登录/注册/找回密码 |
| `/flights` | `FlightSearch.vue` | 机票搜索预订 |
| `/trains` | `TrainSearch.vue` | 火车票搜索预订 |
| `/hotels` | `HotelSearch.vue` / `HotelDetail.vue` | 酒店搜索 |
| `/ai-plan` | `AiPlan.vue` | AI 行程规划 |
| `/community` | `Community.vue` / `PostCreate.vue` / `PostDetail.vue` | 社区 |
| `/admin` | `AdminDashboard.vue` | 管理后台 |
| `/profile` | `UserProfile.vue` | 个人主页 |
| `/orders` | `MyOrders.vue` | 我的订单 |
| `/coupons` | `CouponCenter.vue` | 优惠券中心 |

**公共组件：** `SkeletonBox`（骨架屏）、`EmptyState`（空状态）、`CountUp`（数字动画）、`PageHeader`（标题栏）、`PriceTrend`（价格趋势 ECharts 弹窗）、`SafeImage`（图片安全加载）

**关键模式：**
- Axios 拦截器自动注入 `Authorization` header，401/403 自动跳转登录
- Pinia `useUserStore` 管理登录状态
- Vue Router 守卫 `meta.requiresAuth` / `meta.requiresAdmin`
- 路由守卫检查 token 和 admin 角色

### 6. 文档层（30 个文件）

| 文档 | 说明 |
|------|------|
| `README.md` | 项目概述与快速启动 |
| `CLAUDE.md` | AI 编码助手项目指南 |
| `5组-软件需求规格说明.md` | 需求规格说明书 |
| `软件概要设计说明书.md` | 概要设计 |
| `5组-软件详细设计说明.md` | 详细设计 |
| `docs/用户手册.md` | 用户操作手册 |
| `docs/部署文档.md` | 部署说明 |
| `docs/测试报告.md` | 测试报告 |
| `docs/大模型使用说明.md` | AI 大模型使用说明 |

### 7. 脚本与部署层（5 个文件）

- `start.ps1` — 一键启动前后端（检测 Redis/Java/Node.js 依赖）
- `start.bat` — CMD 启动入口
- `setup.ps1` — 环境配置与数据库初始化（自动加载 `.env`、SOURCE 导入中文）
- `pptgenerate.js` — 答辩 PPT 自动生成（pptxgenjs，15 页暖陶土风格）
- `scripts/check-image-links.mjs` — 图片链接健康检查

---

## 关键概念

### 认证流程
1. 注册/登录公开访问，密码 BCrypt 加密
2. 登录返回 JWT（jjwt，每次重启新密钥，旧 token 全部失效）
3. 前端 `request.js` 拦截器自动注入 `Authorization: Bearer <token>`
4. `JwtFilter` 提取 token 设置 `SecurityContext`
5. `UserContext.getCurrentUserId()` 获取当前登录用户

### 防超卖机制
```
用户下单 → Redis 预扣库存（原子操作） → 数据库乐观锁（version 字段）
  → 成功：创建订单 → 发送通知
  → 失败：Redis 回滚库存 → 返回失败
```
`OrderTimeoutScheduler` 每 30 秒扫描超时未支付订单并归还库存。

### AI 降级
AI 行程规划和客服调用 DeepSeek API，超时/失败时自动降级为预设模板/兜底回复，保证系统稳定性。

### 操作日志
`SysLogAspect` AOP 环绕通知自动记录所有 Controller 调用（方法名、参数、耗时、用户、IP），落库失败不影响业务。

### 接口限流
`@RateLimiter(maxRequests=5, timeWindowSeconds=1)` 基于 Redis 计数器 + IP。下单接口标配。

---

## 复杂度热点

以下区域代码复杂度高，新开发者应仔细阅读：

| 文件 | 复杂度 | 说明 |
|------|--------|------|
| `TrafficOrderServiceImpl.java` | 复杂 | 防超卖下单核心逻辑，含乐观锁+Redis+优惠券 |
| `AiServiceImpl.java` | 复杂 | DeepSeek 集成、Function Calling、降级兜底 |
| `AdminController.java` | 复杂 | 管理后台资源 CRUD、仪表盘聚合、CSV 导入、举报闭环、商家回复 |
| `CouponServiceImpl.java` | 复杂 | 优惠券领取去重/品类校验/过期检测 |
| `PostServiceImpl.java` | 复杂 | 帖子 CRUD + 权限控制 + 关注流 + AI 审核 |
| `HotelOrderServiceImpl.java` | 复杂 | 订单创建、Redis 预扣库存、优惠券抵扣 |
| `SecurityConfig.java` | 复杂 | HTTP 安全过滤器链配置 |
| `frontend/App.vue` | 复杂 | 导航栏+搜索+面包屑+Footer+路由过渡 |
| `frontend/AdminDashboard.vue` | 复杂 | ECharts 仪表盘+多模块管理 |
| `frontend/AiPlan.vue` | 复杂 | 表单+AI展示+历史+客服对话 |

---

## 子系统负责人

| 子系统 | 负责人 | 职责边界 | 关键 Controller |
|--------|--------|----------|-----------------|
| 大交通票务与订单 | 邹林利 | 大交通查询预订、基础订单状态机、模拟支付/退款、库存管控协同 | `FlightController`, `TrainController`, `TrafficOrderController` |
| 目的地住宿与本地生活 | 莫谨瑞 | 酒店/景点用户侧浏览、门票入口、基础评价提交与展示 | `HotelController`, `AttractionController`, `ReviewController` |
| AI 智能规划与通知 | 陈一鸿 | DeepSeek 行程规划、AI 客服 Agent、AI 降级、站内通知触达 | `AiController`（含通知接口） |
| 旅途社区与用户中心 | 杜新诚 | JWT 用户体系、社区发布、推荐/关注流、点赞收藏评论、关注粉丝 | `PostController`, `CommentController`, `LikeController`, `FollowController`, `UserProfileController` |
| 管理后台与可观测性 | 李科 | RBAC 管理后台、资源维护、优惠券、内容审核、商家回复、评价举报、系统日志和仪表盘 | `AdminController` |

---

## 导览路径

推荐按以下顺序阅读代码：

1. **项目概览** → `README.md`
2. **后端启动入口** → `TravelMateApplication.java` + `pom.xml` + `application.yml`
3. **认证与安全** → `SecurityConfig.java` → `JwtFilter.java` → `JwtUtil.java`
4. **用户认证** → `UserController.java` → `UserService.java` → `User.java`
5. **数据模型** → `docs/sql/init.sql` → Entity 类
6. **防超卖** → `TrafficOrderServiceImpl.java`（核心业务逻辑）
7. **AI 规划** → `AiController.java` → `AiServiceImpl.java`
8. **前端架构** → `main.js` → `router/index.js` → `stores/user.js` → `utils/request.js`
9. **前端页面** → `App.vue` → `Home.vue` → `vite.config.js`

---

*本指南由 `/understand-onboard` 基于项目知识图谱自动生成。*
