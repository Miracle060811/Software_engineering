# 修改日志 - 2026-05-29

| 字段      | 内容                               |
| --------- | ---------------------------------- |
| 分支      | `main`                             |
| 提交者    | yfan945、Yinghao Xiang             |
| 提交 Hash | `5bbaf414`、`ef2b9e22`、`3b7e08ca` |

## 今日概述

今天主要围绕管理后台、城市资料体系、登录认证体验和首页视觉做了一轮集中完善。后台新增城市资源管理与 CSV 导入链路，前台热门城市页面改为优先读取后端数据并保留静态兜底；同时优化登录页交互、管理员注册入口、后台视觉样式，以及首页多个旅行规划入口的图片、卡片宽度和双栏布局。

## 变更内容

### feat · New Feature

- **城市资料后端化**: 新增 `tm_destination` 表、`Destination` 实体、Mapper 和公开城市资料接口，支持按 `slug` 查询详情并按排序展示城市列表（`backend/src/main/java/com/travelmate/controller/DestinationController.java`、`docs/sql/init.sql`）。
- **管理后台城市资源**: 在管理后台加入“城市资源”菜单，支持城市资料列表、状态展示、下线操作和 CSV 批量导入，便于维护热门城市内容（`frontend/src/views/admin/AdminDashboard.vue`、`backend/src/main/java/com/travelmate/controller/AdminController.java`）。
- **目的地资料动态读取**: 前端目的地列表与详情页改为优先请求 `/api/destinations`，接口不可用或无数据时回退到原静态资料，降低后端数据缺失对展示页的影响（`frontend/src/utils/destinations.js`、`frontend/src/views/destination/DestinationList.vue`）。
- **管理员注册入口**: 新增 `/user/admin-register` 接口与登录页隐藏管理员注册弹窗，使用 `ADMIN_REGISTER_SECRET` 控制管理员账号创建（`backend/src/main/java/com/travelmate/backend/controller/UserController.java`、`frontend/src/views/Login.vue`）。

### fix · Bug Fix

- **航班资源操作重复**: 删除后台航班资源表格中与“编辑”功能重复的“改库存”按钮，并收窄操作列宽度，减少后台操作干扰（`frontend/src/views/admin/AdminDashboard.vue`）。
- **内容审核改判**: 管理后台游记审核按钮不再只对待审核内容显示，已处理内容也可以改为通过或拒绝，并同步刷新 `updateTime`（`frontend/src/views/admin/AdminDashboard.vue`、`backend/src/main/java/com/travelmate/controller/AdminController.java`）。
- **密码重置文案与流程**: 登录页将“修改密码”调整为“忘记密码”，后端接口同步改为按用户名重置新密码的流程（`frontend/src/views/Login.vue`、`backend/src/main/java/com/travelmate/backend/service/UserService.java`）。
- **公开路由访问**: 安全配置放行城市资料公开接口和管理员注册接口，避免目的地页面未登录时无法加载数据（`backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java`）。

### style · UI / Visual Polish

- **首页 Hero 微调**: 优化首页 Hero 标签、标题字号、行高和卡片文案高度，减少文字拥挤并强化入口分类（`frontend/src/views/Home.vue`）。
- **首页功能入口图片**: 将“发现更多精彩”三张功能卡片替换为独立旅行图片，避免与上方目的地图片重复；底部 CTA 头像由占位图替换为真实头像图（`frontend/src/views/Home.vue`）。
- **首页规划区布局统一**: 将 `ROUTE PREVIEW` 与 `CALM PLANNING` 两个左右双栏卡片统一为居中主容器、相同最大宽度和列宽比例，降低页面断裂感（`frontend/src/views/Home.vue`）。
- **首页安静规划区视觉**: 重排标题为三行中文节奏，入口按钮改为“查票价 / 选住宿 / 生成路线”，右侧图片换为更协调的自然绿色系，并让步骤说明铺满图片区域（`frontend/src/views/Home.vue`）。
- **管理后台视觉优化**: 将管理后台侧栏和内容区从深色后台风格调整为浅色蓝绿系，统一表格、按钮、卡片、阴影和 hover 状态（`frontend/src/views/admin/AdminDashboard.vue`）。
- **登录页体验优化**: 扩展登录页管理员注册弹窗和密码重置弹窗样式，补齐表单校验与成功后的登录回填（`frontend/src/views/Login.vue`）。
- **主题按钮文字**: 调整全局主按钮文字色变量，保证渐变按钮在 hover/active 状态下文字保持可读（`frontend/src/styles/theme.css`）。

### docs · Documentation

- **城市资源导入说明**: 更新 README 和管理后台 CSV 导入文档，补充城市资源字段、重复 `slug` 覆盖规则和示例 CSV（`README.md`、`docs/admin-csv-import.md`）。
- **环境变量模板**: 在 `.env.example` 中补充 `ADMIN_REGISTER_SECRET` 示例配置，说明未配置时后端会使用默认值（`.env.example`）。
- **每日日志更新**: 汇总 2026-05-29 当天后端、前端和首页视觉改动，记录验证情况和后续事项（`Changelog-2026-05-29.md`）。

### chore · Generated Assets

- **构建产物与素材记录**: 今日早前提交包含前端 `dist` 入口文件更新，以及 `.codex-pet-runs/travelfox` 下的素材、预览和校验记录；这些属于生成类产物，后续提交前建议确认是否继续纳入版本库（`frontend/dist/index.html`、`.codex-pet-runs/travelfox/qa/review.json`）。

## 文件更改

| File                                                                          | Changes            |
| ----------------------------------------------------------------------------- | ------------------ |
| `backend/src/main/java/com/travelmate/controller/AdminController.java`        | +85 -2             |
| `backend/src/main/java/com/travelmate/controller/DestinationController.java`  | +41 -0             |
| `backend/src/main/java/com/travelmate/entity/Destination.java`                | +33 -0             |
| `backend/src/main/java/com/travelmate/mapper/DestinationMapper.java`          | +9 -0              |
| `backend/src/main/java/com/travelmate/backend/controller/UserController.java` | +18 -3             |
| `backend/src/main/java/com/travelmate/backend/service/UserService.java`       | +3 -4              |
| `backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java`     | +3 -0              |
| `frontend/src/views/admin/AdminDashboard.vue`                                 | +403 -151          |
| `frontend/src/views/Login.vue`                                                | +142 -19           |
| `frontend/src/views/Home.vue`                                                 | +178 -83           |
| `frontend/src/utils/destinations.js`                                          | +40 -0             |
| `frontend/src/views/destination/DestinationList.vue`                          | +11 -3             |
| `frontend/src/views/destination/DestinationDetail.vue`                        | +10 -3             |
| `frontend/src/styles/theme.css`                                               | +5 -0              |
| `docs/sql/init.sql`                                                           | +26 -0             |
| `docs/admin-csv-import.md`                                                    | +10 -1             |
| `README.md`                                                                   | +5 -5              |
| `.env.example`                                                                | +3 -0              |
| `Changelog-2026-05-29.md`                                                     | 日志补充与结构更新 |

## 验证情况

- 已运行 `npm run build`，前端生产构建通过。
- 已确认首页本地开发地址 `http://127.0.0.1:3000/` 返回 `200`。
- 新增首页远程图片 URL 已用 `curl` 验证返回 `200`。
- 构建过程中仍有既有提示：`@vueuse/core` 的 PURE 注释提示，以及部分 chunk 超过 500 kB 的体积警告；本次未处理分包。

## 未完成事项

- 当前工作区仍有未上传的本地文件或生成产物：`Changelog-2026-05-28.md`、`backend/target/classes/com/travelmate/service/impl/TrainServiceImpl.class`、`backend-start.log`、`backend/src/main/java/com/travelmate/config/TomcatConfig.java`；本次提交未纳入这些与首页视觉无直接关系的改动。
- 管理员注册密钥已支持环境变量配置，后续部署时应确认生产环境显式设置 `ADMIN_REGISTER_SECRET`，避免使用默认回退值。
- 密码重置流程已改为无需旧密码，后续需要确认是否符合课程系统的安全预期，必要时补充验证码、密保或管理员重置流程。
- `.codex-pet-runs/travelfox` 与 `frontend/dist` 属于生成产物，后续提交前建议统一确认是否保留在版本库。

## 明日计划

- 继续检查首页各区块在移动端和不同桌面宽度下的视觉一致性。
- 补充城市资源管理的新增/编辑能力，避免后台只能导入和下线。
- 为城市资料接口、管理员注册和密码重置流程补充基本测试或手动验证记录。
- 梳理本地生成产物与未跟踪文件，明确哪些应提交、忽略或清理。

## 备注

- 本日志基于今日提交 `5bbaf414`、`ef2b9e22`、`3b7e08ca`，以及本次首页视觉优化后的构建验证生成。
- 本次上传刻意排除了 `backend/target`、Vite 缓存和本地运行日志，避免把生成产物混入首页 UI 提交。
