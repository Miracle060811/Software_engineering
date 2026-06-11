# 修改日志 — 2026-05-11

| 字段      | 内容                   |
| --------- | ---------------------- |
| 提交者    | yfan945                |
| 提交 Hash | `a3606772`、`b58b3d58` |

## 今日概述

> 今日共两个提交（`a3606772` "version 1.0" / `b58b3d58` "version 1.0.1"）。`version 1.0` 是项目里程碑提交，完成了住宿（酒店/房型/评价）、景点、AI 行程规划、社区游记、管理后台（含日志/敏感词）的首次完整实现，前端完成了登录注册、全部核心页面和 Pinia 用户状态管理。`version 1.0.1` 随即对 AI 规划、酒店详情、景点列表、航班列表、订单列表等页面做了小幅修复。

---

## 变更内容

### feat · 新功能（version 1.0）

#### 后端 · 全量业务模块首次实现

- **住宿子系统**: 新增酒店（`Hotel`）、房型（`HotelRoom`）、酒店订单（`HotelOrder`）实体及完整 CRUD；`HotelController`/`HotelOrderController` 提供搜索、详情、预订、评价接口（`HotelController.java`, `HotelOrderServiceImpl.java`）

- **景点子系统**: 新增 `Attraction` 实体和 `AttractionController`，支持城市筛选和详情查看（`AttractionController.java`）

- **评价子系统**: 新增 `Review` 实体、`ReviewMapper`、`ReviewService`/`ReviewServiceImpl`/`ReviewController`，支持对酒店/景点发布评价（`ReviewController.java`, `ReviewServiceImpl.java`）

- **AI 行程规划**: 新增 `AiController`、`AiService`/`AiServiceImpl`，对接 DeepSeek API，支持多轮聊天和行程规划生成；`AiChat`/`AiPlan` 实体持久化对话记录（`AiServiceImpl.java`, `AiController.java`）

- **社区 · 游记（骨架）**: 新增 `Post` 实体、`PostMapper`、`PostController`，游记发布/查看首次上线（`PostController.java`）

- **社区 · 评论/点赞/关注（骨架）**: 新增 `Comment`、`Like`、`Follow` 实体及对应 Mapper，相关 Controller 骨架实现（`CommentController.java`, `LikeController.java`, `FollowController.java`）

- **管理后台**: 新增 `AdminController`，提供用户管理、日志查看、敏感词管理接口；`SysLog`、`SysSensitiveWord` 实体落库（`AdminController.java`）

- **全局异常处理**: 新增 `GlobalExceptionHandler`，统一返回 `Result<T>` 格式错误信息（`GlobalExceptionHandler.java`）

- **CORS 配置**: 新增 `CorsConfig` 解决前后端跨域（后续被 `WebConfig` 接管）（`CorsConfig.java`）

- **通知实体**: 新增 `Notification` 实体和 `NotificationMapper`（`Notification.java`）

#### 前端 · 全量页面首次实现

- **登录/注册**: 新增 `Login.vue`，支持表单校验和跳转（`Login.vue`）

- **Pinia 用户状态**: 新增 `stores/user.js`，管理 token 和 userInfo，实现持久化登录（`user.js`）

- **全局主题**: 新增 `styles/theme.css`，建立 CSS 变量体系（`theme.css`）

- **社区页面**: 新增 `Community.vue`、`PostCreate.vue`、`PostDetail.vue`（`community/`）

- **酒店**: 新增 `HotelSearch.vue`、`HotelDetail.vue`（`hotel/`）

- **景点**: 新增 `AttractionList.vue`（`hotel/`）

- **我的订单**: 新增 `MyOrders.vue`（`order/`）

- **个人主页**: 新增 `UserProfile.vue`（`user/`）

- **管理后台**: 新增 `AdminDashboard.vue`（`admin/`）

- **AI 规划**: 新增 `AiPlan.vue`（`admin/ai/`，后续迁移）

- **路由扩展**: `router/index.js` 注册所有新页面路由，添加 `requiresAuth` 路由守卫（`router/index.js`）

- **Vite 代理**: `vite.config.js` 配置 `/api`、`/user` 代理到后端 8080 端口（`vite.config.js`）

#### 文档

- **设计文档**: 新增《5 组-软件详细设计说明.md/pdf》和《5 组-软件需求规格说明.md》（文档初版）

- **大作业说明**: 新增《大作业说明.md》、《软件概要设计说明书.md/pdf》、《详细实施计划.md》

### fix · Bug 修复（version 1.0.1）

- **AI 规划页**: 修复 `AiPlan.vue` 对话框展示异常（`AiPlan.vue`）

- **酒店详情页**: 修复 `HotelDetail.vue` 房型列表加载失败（`HotelDetail.vue`）

- **景点列表页**: 修复 `AttractionList.vue` 图片加载 404（`AttractionList.vue`）

- **航班/订单列表**: `FlightList.vue`、`OrderList.vue` 修复表格数据绑定错误（后续版本已重构）

---

## 文件更改（关键源码，version 1.0 全量新建）

| 模块       | 关键新建文件                                                                                                                                                                                        |
| ---------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 酒店子系统 | `Hotel/HotelRoom/HotelOrder.java`, `HotelController.java`, `HotelServiceImpl.java`, `HotelOrderServiceImpl.java`                                                                                    |
| 景点子系统 | `Attraction.java`, `AttractionController.java`, `AttractionServiceImpl.java`                                                                                                                        |
| 评价子系统 | `Review.java`, `ReviewController.java`, `ReviewServiceImpl.java`                                                                                                                                    |
| AI 规划    | `AiChat/AiPlan.java`, `AiController.java`, `AiServiceImpl.java`                                                                                                                                     |
| 社区骨架   | `Post/Comment/Like/Follow.java`, 对应 Controller                                                                                                                                                    |
| 管理后台   | `AdminController.java`, `SysLog/SysSensitiveWord.java`                                                                                                                                              |
| 前端页面   | `Login.vue`, `Community.vue`, `PostCreate.vue`, `PostDetail.vue`, `HotelSearch.vue`, `HotelDetail.vue`, `AttractionList.vue`, `MyOrders.vue`, `UserProfile.vue`, `AdminDashboard.vue`, `AiPlan.vue` |
| 前端基础   | `stores/user.js`, `styles/theme.css`                                                                                                                                                                |

---

## 备注

- `version 1.0` 为项目第一个可运行版本里程碑，前后端主要功能均已打通。
- `FlightList.vue` 和 `OrderList.vue` 在后续版本（05-12）中被重构或删除。
