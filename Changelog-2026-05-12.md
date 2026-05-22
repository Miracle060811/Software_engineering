# 修改日志 — 2026-05-12

| 字段      | 内容                   |
| --------- | ---------------------- |
| 提交者    | yfan945                |
| 提交 Hash | `27e85115`、`56575d28` |

## 今日概述

> 今日共两个提交（`27e85115` "前端" / `56575d28` "看起来能用了"）。`27e85115` 是一次大型基础设施和功能扩展提交，引入了 Redis 基础设施、接口限流（RateLimiter）、AOP 操作日志（SysLogAspect）、`UserContext`、数据库启动校验等后端基础能力，并同步新增了社区（点赞/关注/评论/游记）、通知中心等核心业务模块，前端也添加了 CountUp/EmptyState/PageHeader/SkeletonBox 四个公共组件。`56575d28` 在此基础上对所有前端页面做了全面 UI 打磨，并新增了 404 页面。

---

## 变更内容

### feat · 新功能

#### 后端基础设施

- **Redis 基础设施**: 新增 `RedisConfig`，配置 RedisTemplate 和序列化方式（`RedisConfig.java`）

- **接口限流**: 新增 `@RateLimiter` 注解和 `RateLimiterInterceptor`，基于 Redis 计数器 + IP 实现接口限流，防刷保护（`RateLimiter.java`, `RateLimiterInterceptor.java`）

- **操作日志 AOP**: 新增 `SysLogAspect`，环绕拦截所有 Controller 方法，自动记录操作用户、IP、耗时、结果到 `tm_sys_log` 表（`SysLogAspect.java`）

- **用户上下文工具**: 新增 `UserContext`，从 `SecurityContextHolder` 获取当前登录用户 ID 和对象，统一供 Controller/Service 使用（`UserContext.java`）

- **数据库启动校验**: 新增 `DatabaseStartupValidator`，启动时检测数据库连通性和核心表是否存在，失败给出友好提示（`DatabaseStartupValidator.java`）

- **WebConfig**: 新增 `WebConfig`，注册 `RateLimiterInterceptor`（`WebConfig.java`）

- **订单超时调度器**: 新增 `OrderTimeoutScheduler`，定时处理超时未支付订单（`OrderTimeoutScheduler.java`）

#### 后端业务模块（全新）

- **社区 · 游记**: 新增 `PostService`/`PostServiceImpl`，支持发布、查看、审核游记（`PostServiceImpl.java`）

- **社区 · 评论**: 新增 `CommentService`/`CommentServiceImpl`，支持树形评论结构（`CommentServiceImpl.java`）

- **社区 · 点赞**: 新增 `LikeService`/`LikeServiceImpl`（`LikeServiceImpl.java`）

- **社区 · 关注**: 新增 `FollowService`/`FollowServiceImpl`（`FollowServiceImpl.java`）

- **通知中心**: 新增 `NotificationCenterService`/`NotificationCenterServiceImpl`（`NotificationCenterServiceImpl.java`）

- **酒店库存**: 新增 `HotelRoomStockService`/`HotelRoomStockServiceImpl`，独立管理房间库存防超卖（`HotelRoomStockServiceImpl.java`）

- **敏感词过滤**: 新增 `SensitiveWordService`/`SensitiveWordServiceImpl`，发帖/评论时自动检测（`SensitiveWordServiceImpl.java`）

#### 前端公共组件

- **公共组件**: 新增 `CountUp.vue`（数字滚动动画）、`EmptyState.vue`（空状态占位）、`PageHeader.vue`（页面标题栏）、`SkeletonBox.vue`（骨架屏）（`components/`）

- **通知中心页**: 新增 `NotificationCenter.vue`（`NotificationCenter.vue`）

- **AI 规划页迁移**: `AiPlan.vue` 从 `admin/ai/` 迁移至 `views/ai/`，路由同步更新（`AiPlan.vue`）

- **404 页面**: 新增 `NotFound.vue`，路由未匹配时展示（`NotFound.vue`）

#### 文档

- **技术文档套件**: 新增用户手册、测试报告、部署文档、大模型使用说明四份文档（`docs/`）

### chore · 配置与工程

- **依赖 · pom.xml**: 引入 Redis、AOP、Scheduler 相关依赖（`pom.xml`）

- **一键启动**: 新增 `start.bat`、`start.ps1`、`package.json`/`package-lock.json`（项目根）用于跨平台快速启动前后端（`start.bat`, `start.ps1`）

- **主题样式**: `theme.css` 初步建立全局 CSS 变量体系（`theme.css`）

- **路由扩展**: `router/index.js` 注册通知中心、AI 规划等新路由，添加 404 fallback（`router/index.js`）

- **数据库备份**: 提交 `travelmate_backup.sql`（`56575d28` 阶段性备份）

### style · 前端 UI 打磨（56575d28）

- **全页面打磨**: 首页、社区、航班/火车搜索、酒店搜索/详情、景点列表、AI 规划、管理后台、个人主页、我的订单、通知中心全面 UI 优化（各对应 `.vue` 文件）

---

## 文件更改（关键源码）

| 文件                                                     | 说明               |
| -------------------------------------------------------- | ------------------ |
| `backend/.../annotation/RateLimiter.java`                | 新建，限流注解     |
| `backend/.../aspect/SysLogAspect.java`                   | 新建，操作日志 AOP |
| `backend/.../common/UserContext.java`                    | 新建，用户上下文   |
| `backend/.../config/DatabaseStartupValidator.java`       | 新建，启动校验     |
| `backend/.../config/RedisConfig.java`                    | 新建，Redis 配置   |
| `backend/.../config/WebConfig.java`                      | 新建，Web 配置     |
| `backend/.../interceptor/RateLimiterInterceptor.java`    | 新建，限流拦截器   |
| `backend/.../scheduler/OrderTimeoutScheduler.java`       | 新建，超时调度     |
| `backend/.../service/impl/PostServiceImpl.java`          | 新建，游记         |
| `backend/.../service/impl/CommentServiceImpl.java`       | 新建，评论         |
| `backend/.../service/impl/LikeServiceImpl.java`          | 新建，点赞         |
| `backend/.../service/impl/FollowServiceImpl.java`        | 新建，关注         |
| `backend/.../service/impl/SensitiveWordServiceImpl.java` | 新建，敏感词       |
| `frontend/src/components/CountUp.vue`                    | 新建               |
| `frontend/src/components/EmptyState.vue`                 | 新建               |
| `frontend/src/components/PageHeader.vue`                 | 新建               |
| `frontend/src/components/SkeletonBox.vue`                | 新建               |
| `frontend/src/views/user/NotificationCenter.vue`         | 新建               |
| `frontend/src/views/NotFound.vue`                        | 新建               |

---

## 未完成事项

- 新增模块均缺乏单元测试和集成测试
- 通知中心仅有 UI，通知生成逻辑（点赞/关注触发）未完整实现
- `travelmate_backup.sql` 不应进入版本库，应在 `.gitignore` 中排除

## 明日计划

1. 完善优惠券、评价回复等新子系统（见 05-13 提交）
2. 补充数据库种子数据
3. 整理文档，准备阶段性汇报
