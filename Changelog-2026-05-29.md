# 修改日志 - 2026-05-29

| 字段      | 内容       |
| --------- | ---------- |
| 分支      | `main`     |
| 提交者    | yfan945    |
| 提交 Hash | `5bbaf414` |

## 今日概述

今天主要围绕管理后台、城市资料体系和登录认证体验做了一轮功能补充与问题修复。后台新增城市资源管理与 CSV 导入链路，前台热门城市页面改为优先读取后端数据并保留静态兜底；同时优化登录页交互、管理员注册入口、后台视觉样式和航班资源操作按钮。

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

- **管理后台视觉优化**: 将管理后台侧栏和内容区从深色后台风格调整为浅色蓝绿系，统一表格、按钮、卡片、阴影和 hover 状态（`frontend/src/views/admin/AdminDashboard.vue`）。
- **首页 Hero 微调**: 优化首页 Hero 标签、标题字号、行高和卡片文案高度，减少文字拥挤并强化入口分类（`frontend/src/views/Home.vue`）。
- **登录页体验优化**: 扩展登录页管理员注册弹窗和密码重置弹窗样式，补齐表单校验与成功后的登录回填（`frontend/src/views/Login.vue`）。
- **主题按钮文字**: 调整全局主按钮文字色变量，保证渐变按钮在 hover/active 状态下文字保持可读（`frontend/src/styles/theme.css`）。

### docs · Documentation

- **城市资源导入说明**: 更新 README 和管理后台 CSV 导入文档，补充城市资源字段、重复 `slug` 覆盖规则和示例 CSV（`README.md`、`docs/admin-csv-import.md`）。
- **环境变量模板**: 在 `.env.example` 中补充 `ADMIN_REGISTER_SECRET` 示例配置，说明未配置时后端会使用默认值（`.env.example`）。

### chore · Generated Assets

- **构建产物与素材记录**: 提交中包含前端 `dist` 入口文件更新，以及 `.codex-pet-runs/travelfox` 下的素材、预览和校验记录；这些属于生成类产物，后续提交前建议确认是否应继续纳入版本库（`frontend/dist/index.html`、`.codex-pet-runs/travelfox/qa/review.json`）。

## 文件更改

| File                                                                          | Changes                                      |
| ----------------------------------------------------------------------------- | -------------------------------------------- |
| `backend/src/main/java/com/travelmate/controller/AdminController.java`        | +85 -2                                       |
| `backend/src/main/java/com/travelmate/controller/DestinationController.java`  | +41 -0                                       |
| `backend/src/main/java/com/travelmate/entity/Destination.java`                | +33 -0                                       |
| `backend/src/main/java/com/travelmate/mapper/DestinationMapper.java`          | +9 -0                                        |
| `backend/src/main/java/com/travelmate/backend/controller/UserController.java` | +18 -3                                       |
| `backend/src/main/java/com/travelmate/backend/service/UserService.java`       | +3 -4                                        |
| `backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java`     | +3 -0                                        |
| `frontend/src/views/admin/AdminDashboard.vue`                                 | +139 -25，含当前未提交的“删除改库存按钮”修补 |
| `frontend/src/views/Login.vue`                                                | +142 -19                                     |
| `frontend/src/views/Home.vue`                                                 | +49 -19                                      |
| `frontend/src/utils/destinations.js`                                          | +40 -0                                       |
| `frontend/src/views/destination/DestinationList.vue`                          | +11 -3                                       |
| `frontend/src/views/destination/DestinationDetail.vue`                        | +10 -3                                       |
| `frontend/src/styles/theme.css`                                               | +5 -0                                        |
| `docs/sql/init.sql`                                                           | +26 -0                                       |
| `docs/admin-csv-import.md`                                                    | +10 -1                                       |
| `README.md`                                                                   | +5 -5                                        |
| `.env.example`                                                                | +3 -0                                        |
| `frontend/dist/index.html`                                                    | +3 -3，含构建后入口 chunk 名变化             |

## 验证情况

- 已运行 `npm run build`，前端生产构建通过。
- 构建过程中仍有既有提示：`@vueuse/core` 的 PURE 注释提示，以及部分 chunk 超过 500 kB 的体积警告；本次未处理分包。

## 未完成事项

- 当前工作区仍有未提交改动：`frontend/src/views/admin/AdminDashboard.vue` 的航班资源按钮修补、`frontend/dist/index.html` 的构建入口更新，以及本日志文件。
- 管理员注册密钥已支持环境变量配置，后续部署时应确认生产环境显式设置 `ADMIN_REGISTER_SECRET`，避免使用默认回退值。
- 密码重置流程已改为无需旧密码，后续需要确认是否符合课程系统的安全预期，必要时补充验证码、密保或管理员重置流程。
- `.codex-pet-runs/travelfox` 与 `frontend/dist` 属于生成产物，后续提交前建议统一确认是否保留在版本库。

## 明日计划

- 提交并同步今天未提交的航班资源按钮修补和日志文件，保持工作区干净。
- 补充城市资源管理的新增/编辑能力，避免后台只能导入和下线。
- 为城市资料接口、管理员注册和密码重置流程补充基本测试或手动验证记录。
- 检查生成产物纳入版本库的策略，必要时更新 `.gitignore` 或清理历史生成文件。

## 备注

- `git log --since="today"` 在本次环境中没有返回结果，已改用明确日期 `2026-05-29 00:00` 和最近提交列表确认今日提交。
- 本日志基于今日提交 `5bbaf414`、当前工作区 diff 和已完成的前端构建验证生成。
