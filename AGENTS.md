# AGENTS.md

This file provides guidance to coding assistants when working with code in this repository.

## 技术栈版本

- 后端：Spring Boot 3.5.13 + Java 21 + MyBatis-Plus 3.5.7 + MySQL + Redis
- 前端：Vue 3 + Vite + Element Plus + Pinia + Axios
- 认证：Spring Security + jjwt 0.11.5（HS256 对称密钥，24h 过期）

## 常用命令

### 一键启动（推荐）

```powershell
# Windows: 启动前后端（推荐）
.\start.ps1

# CMD/双击入口，参数会透传给 start.ps1
.\start.bat

# 首次初始化数据库
.\setup.ps1 -InitDb

# 中文种子数据已被导成 ? 时重建数据库
.\setup.ps1 -InitDb -ResetDb
```

### 后端

```bash
cd backend

# 手动加载环境变量（从根目录 .env 文件）
# Windows: .\setup.ps1
# Linux/Mac: source ../.env

# 启动（需要 DB_PASSWORD 和可选的 DEEPSEEK_API_KEY 环境变量）
.\mvnw.cmd clean spring-boot:run

# 编译
.\mvnw.cmd clean compile

# 运行测试
.\mvnw.cmd test

# 运行单个测试
.\mvnw.cmd test -Dtest="BackendApplicationTests"
```

### 前端

```bash
cd frontend
npm install
npm run dev        # 开发服务器，端口 3000
npm run build      # 生产构建
```

### 启动脚本参数

`start.ps1` 和 `start.bat` 支持同一组参数，`start.bat` 只是 CMD/双击入口：

```powershell
.\start.ps1 -DbPassword 你的MySQL密码
.\start.ps1 -DeepseekApiKey 你的DeepSeek密钥
.\start.ps1 -BackendOnly
.\start.ps1 -FrontendOnly
.\start.ps1 -SkipRedis
.\start.ps1 -SkipFrontendInstall
.\start.ps1 -DryRun
```

`start.bat /?` 可查看帮助。改动启动逻辑时优先维护 `start.ps1`，再同步 `start.bat` 的帮助文本；不要在 `start.bat` 里复制一套独立启动逻辑。

### 数据库

```powershell
# 首次导库
.\setup.ps1 -InitDb

# 中文已被导成 ? 时重建数据库
.\setup.ps1 -InitDb -ResetDb
```

```bash
# 手动导入
mysql --default-character-set=utf8mb4 -u root -p < docs/sql/init.sql
```

不要使用 PowerShell 的 `Get-Content | mysql` 管道导入；这会把中文种子数据写成 `?`。
`setup.ps1` 会自动查找 `mysql.exe` 并直接执行 `SOURCE` 导入，不依赖 `cmd.exe`。
景点、酒店、热门城市等种子数据已尽量使用真实图片 URL；不要再引入 `picsum.photos` 这类随机占位图。若外链图片在页面无法渲染，需要同步更新：

1. `docs/sql/init.sql` 中对应 `cover_img` 或媒体资源 URL
2. 已初始化本地库中的对应记录（否则旧库不会自动变化）
3. 前端兜底映射（如 `AttractionList.vue` 中按景点名覆盖的图片映射）

## 架构概览

### 启动类

**必须使用 `com.travelmate.TravelMateApplication`** 作为主类，而非 `backend.BackendApplication`。前者 `@SpringBootApplication` 会自动扫描 `com.travelmate` 下所有子包（包括 `backend` 认证模块和其余业务模块）。

### 包结构

- `com.travelmate.backend` — 用户认证系统（User 实体、JWT、Spring Security）
- `com.travelmate.common` — 统一响应体 `Result<T>`（code/msg/data）+ `GlobalExceptionHandler` + `UserContext`
- `com.travelmate.controller` — 所有业务接口（Flight/Train/Hotel/AI/Post/Comment/Like/Follow/Admin 等）
- `com.travelmate.entity` — 实体类，使用 MyBatis-Plus 注解，表名前缀 `tm_`
- `com.travelmate.mapper` — MyBatis-Plus Mapper 接口
- `com.travelmate.service` — 业务接口 + `impl/` 实现类
- `com.travelmate.dto` — 请求 DTO（如 `FlightOrderCreateDTO`、`AiChatDTO`）
- `com.travelmate.config` — WebConfig、RedisConfig
- `com.travelmate.interceptor` — RateLimiterInterceptor（基于 Redis 的接口限流）
- `com.travelmate.aspect` — SysLogAspect（AOP 自动记录 Controller 调用日志）
- `com.travelmate.annotation` — RateLimiter 注解定义

### 密钥与密码配置

所有敏感配置集中放在根目录 `.env` 文件中（已在 `.gitignore` 中忽略）：

```
DB_PASSWORD=你的数据库密码
DEEPSEEK_API_KEY=你的DeepSeek密钥
```

**加载 .env 到环境变量:**

```powershell
# Windows PowerShell：只加载 .env
.\setup.ps1

# 初始化数据库
.\setup.ps1 -InitDb

# 启动前后端
.\start.ps1
```

`application.yml` 通过占位符读取环境变量，优先级：`SPRING_DATASOURCE_PASSWORD` > `DB_PASSWORD`。本地开发时也可以继续使用 `backend/application-local.yml`（已在 `.gitignore` 中）。

### 认证流程

1. `/user/register` 和 `/user/login` 公开访问，注册时 BCrypt 加密密码
2. 登录返回 JWT token（jjwt 0.11.5，每次重启生成新密钥，旧 token 全部失效），前端存入 localStorage
3. 前端 `utils/request.js` 的 Axios 拦截器自动注入 `Authorization: Bearer <token>`
4. `JwtFilter` 从 Header 提取 token 并设置 `SecurityContext`
5. `SecurityConfig` 仅公开 `/user/register`、`/user/login`，以及航班/火车搜索、酒店搜索/详情/房型、景点搜索/详情、游记列表/详情、评价列表等只读 GET 接口；社区写接口、酒店订单、我的内容和其他私有接口都需要登录
6. 需要登录但未带 token 的请求返回 403

### UserContext 工具

在 Controller/Service 中通过 `UserContext.getCurrentUserId()` 获取当前登录用户 ID，通过 `UserContext.getCurrentUser()` 获取完整 User 对象。内部从 `SecurityContextHolder` 取 username 再查库。

### 接口限流（RateLimiter）

`@RateLimiter(maxRequests = 5, timeWindowSeconds = 1)` 注解用于 Controller 方法，基于 Redis 计数器 + IP 实现。`RateLimiterInterceptor` 拦截后超限返回 429。常用于下单接口（FlightController、TrafficOrderController 等）。

### 操作日志（SysLogAspect）

AOP 环绕通知自动拦截 `com.travelmate.controller..*.*` 所有方法，记录方法名、参数、耗时、操作用户、IP、成功/失败状态到 `tm_sys_log` 表。日志落库失败不影响业务。

### 全局异常处理

`GlobalExceptionHandler` 统一捕获异常并返回 `Result.error()`。特殊处理：

- 数据库连接失败 → 提示检查 `application-local.yml` 或 `DB_PASSWORD`
- 表不存在 → 提示执行 `docs/sql/init.sql`
- 其他异常 → 返回根因 message

### 防超卖机制

订单创建使用**乐观锁**（version 字段 + Redis 预减库存），适用于航班、火车票、酒店房型。
交通订单支持 `ticketCount`，酒店订单支持 `roomCount`；库存预扣减、订单金额、超时取消回补都必须按实际数量处理，不能再默认只扣 1。

### AI 降级

AI 行程规划和客服调用 DeepSeek API（OpenAI 兼容协议，默认模型为 `deepseek-v4-flash`），API 超时或失败时自动降级为预设模板/兜底回复，不会报错。
社区游记支持 AI 审核：`PostAuditScheduler` 定时扫描待审核内容，`AiServiceImpl` 输出通过/拒绝与原因。AI 失败时应保留人工审核或兜底路径，不能阻塞发帖主流程。

### 敏感词过滤

`SensitiveWordService` 提供敏感词检测，用于社区发帖/评论的审核。

### 前端关键模式

#### Axios 响应拦截器行为

`utils/request.js` 中，`response.data.code === 200` 时只返回 `data` 字段（调用方直接拿到业务数据）；非 200 抛出 Error（调用方需 catch）。401/403 时自动清除 token 并跳转 `/login`。

#### 路由守卫

Vue Router 通过 `meta.requiresAuth` 和 `meta.requiresAdmin` 控制访问：

- `requiresAuth`：检查 localStorage 中的 token，无 token 跳转 `/login`
- `requiresAdmin`：检查 `userInfo.role === 1`，非管理员跳转首页

#### Pinia Store

`stores/user.js` 的 `useUserStore` 管理登录状态：`login()` 获取 token 后自动调 `fetchUserInfo()` 拉取用户详情并存入 localStorage。

#### 公共组件

`skeletonBox`（骨架屏）、`EmptyState`（空状态占位）、`CountUp`（数字滚动动画）、`PageHeader`（页面标题栏）。

### 前端代理

Vite dev server 将 `/api` 和 `/user` 代理到 `http://localhost:8080`，开发时无需跨域配置。

### 热门城市与静态信息页

- `frontend/src/data/destinations.js` — 热门城市资料源
- `frontend/src/views/destination/DestinationList.vue` — 热门城市列表
- `frontend/src/views/destination/DestinationDetail.vue` — 城市详情页
- `frontend/src/data/infoPages.js` — 关于/条款/隐私/帮助文案
- `frontend/src/views/info/InfoPage.vue` — 静态信息页渲染

修改首页、导航或页脚入口时，要同步检查这些路由与 `App.vue` 面包屑配置。

### User 实体映射

`User` 实体的 `@TableName` 注解值为 `"tm_user"`（非默认表名 `"user"`），这是之前踩过的坑。

### 各子系统对应关系

| 子系统         | 负责同学 | 关键 Controller                                                                                                            |
| -------------- | -------- | -------------------------------------------------------------------------------------------------------------------------- |
| 大交通票务     | 邹林利   | FlightController, TrainController, TrafficOrderController, PassengerController, PriceHistoryController, CouponController   |
| 住宿与本地生活 | 莫谨瑞   | HotelController, AttractionController, ReviewController, TourProductController, ReplyController, ReviewReportController    |
| AI 智能规划    | 陈一鸿   | AiController                                                                                                               |
| 社区与用户中心 | 杜新诚   | PostController, CommentController, LikeController, FollowController, UserProfileController, UserController, FileController |
| 管理后台       | 李科     | AdminController                                                                                                            |

### 新增实体和表

- `Coupon` / `tm_coupon` — 优惠券表（满减/折扣、限量领取）
- `UserCoupon` / `tm_user_coupon` — 用户已领优惠券关联表
- `Reply` / `tm_reply` — 评价商家回复表
- `ReviewReport` / `tm_review_report` — 评价举报表
- `TourProduct` / `tm_tour_product` — 一日游/周边游产品表
- `Review` 新增 `tags` 字段（评价标签，逗号分隔）
- `TrafficOrder` 新增 `ticket_count` 字段（交通票数量）
- `HotelOrder` 新增 `room_count` 字段（酒店房间数）
- `Coupon` 新增业务类型/分类字段，用于交通、酒店、通用优惠券筛选

### 新增前端页面/组件

- `views/order/CouponCenter.vue` — 优惠券中心（可领取 + 我的优惠券）
- `views/admin/AdminDashboard.vue` — 管理后台（仪表盘、房态库存、优惠券、订单流水、举报工单、敏感词、日志、用户管理）
- `components/PriceTrend.vue` — ECharts 价格趋势图弹窗组件
- `views/destination/DestinationList.vue` — 热门城市列表
- `views/destination/DestinationDetail.vue` — 热门城市详情
- `views/info/InfoPage.vue` — 关于、条款、隐私、帮助等静态信息页
