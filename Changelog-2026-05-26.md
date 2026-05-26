# 修改日志 — 2026-05-24 至 2026-05-26

| 字段      | 内容                         |
| --------- | ---------------------------- |
| 提交者    | yfan945、Yinghao Xiang        |
| 提交 Hash | `5d34c487`、`f9532961`        |
| 分支      | `main`                       |

## 今日概述

这两次提交主要围绕资源加载、管理员后台、AI 行程规划和数据库演示数据做修复与增强。5 月 24 日的提交修复了静态资源访问和图片兜底问题；5 月 26 日的提交进一步完善管理员退款、优惠券处理、AI 行程输出结构，并补充了酒店、房型和媒体资源数据。

## 变更内容

### feat · New Feature

- **AI 行程规划**: 增强行程生成结构，新增 `pace`、`budgetNote`、每日区域、每日费用、活动时长、换乘说明和预约提示等字段，让生成结果更适合直接展示和执行。相关文件：`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`、`frontend/src/views/ai/AiPlan.vue`
- **本地景点参考**: AI 规划引入本地景点库作为参考，生成行程时优先使用真实景点信息，减少跨区域折返和虚构地点。相关文件：`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`
- **数据库资源**: 补充酒店、房型、媒体资源和真实图片 URL，并修复旧占位图数据，提升初始化后的演示效果。相关文件：`docs/sql/init.sql`
- **项目文档**: 新增入门文档，更新产品需求文档，补充项目理解辅助资料。相关文件：`docs/ONBOARDING.md`、`docs/Product_Requirements_Document.md`

### fix · Bug Fix

- **静态资源访问**: 放开 `/favicon.ico`、`/assets/**`、`/images/**`、`/vite.svg` 等公开资源路径，并增加 `/images/**` 资源映射，修复前端图片和静态资源被鉴权拦截的问题。相关文件：`backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java`、`backend/src/main/java/com/travelmate/config/WebConfig.java`
- **管理员退款**: 修正交通订单和酒店订单的退款状态判断，避免重复退款、取消订单重复返还库存，以及已完成酒店订单被直接退款。相关文件：`backend/src/main/java/com/travelmate/controller/AdminController.java`
- **优惠券删除**: 已领取的优惠券不再物理删除，改为下线并清空库存，避免破坏用户领券记录。相关文件：`backend/src/main/java/com/travelmate/controller/AdminController.java`
- **修改密码**: 登录页将“重置密码”改为“修改密码”，新增原密码参数和校验入口，降低未验证旧密码直接修改的风险。相关文件：`frontend/src/views/Login.vue`、`backend/src/main/java/com/travelmate/backend/controller/UserController.java`
- **图片加载**: 调整 `SafeImage` 的 `referrerpolicy`，增强外链图片、本地种子图、上传图片和 Wikimedia 图片地址的兼容处理。相关文件：`frontend/src/components/SafeImage.vue`、`frontend/src/utils/image.js`
- **酒店封面图**: 优化酒店列表和详情页图片选择逻辑，优先使用可用真实图片，失败时再回退到本地种子图。相关文件：`frontend/src/views/hotel/HotelSearch.vue`、`frontend/src/views/hotel/HotelDetail.vue`
- **CSV 解析**: 修复后台导入解析中双引号转义内容的处理，提升包含逗号或引号字段的兼容性。相关文件：`backend/src/main/java/com/travelmate/controller/AdminController.java`

### refactor · Refactoring

- **图片工具**: 统一种子图路径生成、图片地址规范化和图片列表解析逻辑，减少页面内重复兜底判断。相关文件：`frontend/src/utils/image.js`
- **AI 参数处理**: 对目的地、天数、人数、预算、偏好和出发日期做统一规范化，提升异常输入下的稳定性。相关文件：`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`
- **城市展示**: 热门城市列表首屏背景改为从数据源动态读取，避免硬编码单一图片路径。相关文件：`frontend/src/views/destination/DestinationList.vue`

### test · Tests

- **安全配置测试**: 补充资源访问相关安全配置测试，覆盖静态资源放行场景。相关文件：`backend/src/test/java/com/travelmate/SecurityConfigTests.java`

### chore · Build / Config

- **环境配置**: 更新 `.env.example`、`.gitignore`、`vite.config.js`、`setup.ps1` 和 `start.ps1`，完善本地启动、代理、环境变量和初始化流程。相关文件：`.env.example`、`setup.ps1`、`start.ps1`
- **工具配置**: 新增 `.understand-anything` 索引和 `.claude` 配置，用于项目理解和辅助分析。相关文件：`.understand-anything/config.json`、`.claude/settings.json`

## 文件更改

| File | Changes |
| ---- | ------- |
| `.env.example` | +14 -13 |
| `.gitignore` | +7 |
| `backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java` | +6 -1 |
| `backend/src/main/java/com/travelmate/config/WebConfig.java` | +23 |
| `backend/src/main/java/com/travelmate/controller/AdminController.java` | +44 -15 |
| `backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java` | +275 -59 |
| `backend/src/test/java/com/travelmate/SecurityConfigTests.java` | +6 |
| `docs/ONBOARDING.md` | +219 |
| `docs/Product_Requirements_Document.md` | +34 -31 |
| `docs/sql/init.sql` | +191 -19 |
| `frontend/src/components/SafeImage.vue` | +4 -3 |
| `frontend/src/data/destinations.js` | +11 -11 |
| `frontend/src/utils/image.js` | +96 -5 |
| `frontend/src/views/Login.vue` | +19 -5 |
| `frontend/src/views/admin/AdminDashboard.vue` | +17 -24 |
| `frontend/src/views/ai/AiPlan.vue` | +81 -7 |
| `frontend/src/views/community/Community.vue` | +21 -13 |
| `frontend/src/views/destination/DestinationList.vue` | +9 -4 |
| `frontend/src/views/hotel/HotelDetail.vue` | +12 -8 |
| `frontend/src/views/hotel/HotelSearch.vue` | +3 -2 |
| `frontend/vite.config.js` | +4 |
| `setup.ps1` | +17 -3 |
| `start.ps1` | +1 -1 |

说明：`.understand-anything/*` 为工具生成的大体量索引文件，`backend/target/classes/*` 为编译产物，未在上表展开。

## 未完成事项

- 需要确认 `backend/target/classes/com/travelmate/service/impl/TrafficOrderServiceImpl.class` 是否应继续保留在版本库中；编译产物通常不建议提交。
- 需要在真实数据库初始化后验证 `docs/sql/init.sql` 新增图片和媒体资源是否都能正常加载。
- 需要回归管理员退款、优惠券删除、修改密码和 AI 行程规划等关键流程。

## 明日计划

- 运行后端测试和前端构建，确认安全配置、管理员接口和图片工具没有引入回归。
- 使用 `setup.ps1 -InitDb -ResetDb` 在本地重建数据库，验证新增酒店、房型和媒体资源数据。
- 手动检查 AI 行程规划页面，确认新增字段在移动端和桌面端展示正常。
- 清理或确认是否忽略编译产物和工具生成索引，减少后续提交噪音。

## 备注

- 本日志根据 `git log --since="2 days ago"`、`git show --stat` 和关键文件 diff 自动整理。
- 当前工作区存在未提交改动：`.claude/settings.local.json` 和 `backend/target/classes/com/travelmate/service/impl/TrafficOrderServiceImpl.class`。
