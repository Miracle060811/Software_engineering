# 修改日志 — 2026-05-23（下午至深夜）

| 字段      | 内容                                                                               |
| --------- | ---------------------------------------------------------------------------------- |
| 时间范围  | 2026-05-23 12:00 之后                                                              |
| 提交者    | yfan945                                                                            |
| 提交 Hash | `5af33846`（15:03）、`304aa9d7`（21:02）、`71587c29`（21:13）、`179650ac`（23:27） |

## 今日概述

> 今日下午到深夜共 4 次提交，完成了管理后台的全面扩展、通知中心的完整重建、AI 自动审核系统、热门城市详情页、订单多数量支持、本地 SVG 种子图片体系，以及多处社区/用户页面优化。管理后台新增近 600 行 API 与 550 行前端看板，是本次改动体量最大的模块。

## 变更内容

### feat · 新功能

- **管理后台全面扩展**: `AdminController` 新增用户管理、订单流水、库存房态、优惠券 CRUD、敏感词管理、操作日志查询、CSV 批量导入等 20 余个接口；`AdminDashboard.vue` 同步扩展为多标签仪表盘，支持实时数据展示与操作（`AdminController.java` +562, `AdminDashboard.vue` +546）。
- **通知中心重建**: `NotificationCenter.vue` 重写为完整通知列表页，支持分类过滤、已读/未读、删除操作；后端 `NotificationCenterServiceImpl` 补充批量读取与统计接口（`NotificationCenter.vue` +112, `NotificationCenterServiceImpl.java` +23）。
- **AI 自动审核**: 新增 `PostAuditScheduler` 定时扫帖任务、`PostAuditResult` DTO，`AiServiceImpl` 实现内容审核调用 DeepSeek 并输出通过/拒绝结论；后台可查看审核统计（`PostAuditScheduler.java` +74, `AiServiceImpl.java` +140）。
- **AI 接口扩展**: `AiController` 新增行程优化和问答接口，`AiService` 扩充审核方法签名（`AiController.java` +28, `AiService.java` +4）。
- **热门城市详情页**: 新增城市数据源（10 个热门城市）、列表页与详情页，支持从首页直接进入真实目的地资料、代表景点与出行建议（`destinations.js` +260, `DestinationList.vue` +193, `DestinationDetail.vue` +241）。
- **静态信息页**: 新增关于我们、隐私政策、服务条款、帮助中心等页面，接入路由（`infoPages.js` +90, `InfoPage.vue` +102, `router/index.js` +34）。
- **本地 SVG 种子图片体系**: 新增 20 余张城市/景点/酒店/头像主题 SVG 占位图，新增 `SafeImage.vue` 带兜底的图片组件和 `image.js` 图片工具函数，彻底替代依赖外网的 `picsum.photos`（`frontend/public/images/seed/` +20 files, `SafeImage.vue` +55, `image.js` +35）。
- **优惠券分类**: `Coupon` 实体新增业务类型字段，`CouponServiceImpl` 支持按交通/酒店/通用筛选可用券，前端优惠券中心同步适配（`Coupon.java`, `CouponServiceImpl.java` +25）。
- **多数量订单支持**: 机票/火车票 DTO 新增 `ticketCount`，酒店订单新增 `roomCount`，下单逻辑、库存预扣减、超时回补全部按数量处理（`FlightOrderCreateDTO.java`, `TrainOrderCreateDTO.java`, `HotelOrderCreateDTO.java`, `HotelOrder.java`, `TrafficOrder.java`）。
- **图片链接检查脚本**: 新增 `scripts/check-image-links.mjs`，可批量检测前端图片 URL 的可访问性（`check-image-links.mjs` +47）。
- **CSV 批量导入文档**: 新增管理后台 CSV 导入说明文档（`docs/admin-csv-import.md` +38）。

### fix · Bug 修复

- **景点购票**: 景点购票接口改为分别接收成人票/儿童票数量，前端购票表单同步改为分项提交（`AttractionController.java`, `AttractionServiceImpl.java` +12, `AttractionList.vue`）。
- **库存回补**: 超时订单释放库存时按实际票数/房间数回补，修复多张票或多间房取消后库存只恢复 1 的问题（`OrderTimeoutScheduler.java`, `HotelRoomStockServiceImpl.java` +24）。
- **酒店图片/房型**: 扩充酒店真实照片、补齐部分酒店房型展示字段，修复详情页空数据或图片错乱问题（`HotelDetail.vue` +24, `HotelSearch.vue` +13, `docs/sql/init.sql`）。
- **社区页面**: 修复社区列表帖子卡片展示异常，`PostDetail.vue` 补充评论区交互细节（`Community.vue` +30, `PostDetail.vue` +23）。
- **用户主页**: 修复 `UserProfile.vue` 部分字段未渲染、关注数统计不准确问题（`UserProfile.vue` +32）。
- **我的订单**: `MyOrders.vue` 补充票数/房间数展示，修复订单状态映射遗漏（`MyOrders.vue` +15）。

### refactor · 重构

- **库存接口统一**: `FlightMapper`、`TrainMapper`、`HotelRoomMapper` 库存预扣减方法统一按数量参数处理，语义更清晰（`FlightMapper.java` +8, `TrainMapper.java` +16, `HotelRoomMapper.java` +8）。
- **顶部导航重构**: `App.vue` 顶部导航与面包屑逻辑重写，修正页面标题与路由跳转不一致，删除冗余 `PageHeader.vue` 组件（`App.vue` +89, `PageHeader.vue` -23）。
- **首页结构优化**: 首页功能区和热门城市入口重排，减少纯装饰占位，突出可点击业务入口（`Home.vue`）。

### docs · 文档

- **AGENTS.md**: 补充各子系统负责人对照表、新增实体、新增页面等项目规范（`AGENTS.md` +42）。
- **README**: 更新项目说明，补充启动脚本参数说明、新增功能和图片数据说明（`README.md`）。
- **SQL 种子数据**: 大规模扩充景点、酒店、房型、热门城市、媒体资源 URL 和兼容迁移语句（`docs/sql/init.sql` +268）。

### chore · 构建与配置

- **启动脚本**: `start.bat` 同步更新帮助文本，与 `start.ps1` 参数保持一致（`start.bat` +28）。
- **package.json**: 新增 `check-image-links` 脚本入口（`package.json` +3）。
- **数据库启动验证**: `DatabaseStartupValidator` 补充更友好的启动检测提示（`DatabaseStartupValidator.java` +2）。

## 文件更改

| File                                                                                   | Changes     |
| -------------------------------------------------------------------------------------- | ----------- |
| `backend/src/main/java/com/travelmate/controller/AdminController.java`                 | +562 -21    |
| `frontend/src/views/admin/AdminDashboard.vue`                                          | +546 -33    |
| `docs/sql/init.sql`                                                                    | +268 -268   |
| `frontend/src/data/destinations.js`                                                    | +260 +22    |
| `frontend/src/views/destination/DestinationDetail.vue`                                 | +241 +3     |
| `frontend/src/views/destination/DestinationList.vue`                                   | +193 +5     |
| `frontend/src/views/Home.vue`                                                          | +190 -69    |
| `frontend/src/views/user/NotificationCenter.vue`                                       | +112        |
| `frontend/src/data/infoPages.js`                                                       | +90         |
| `frontend/src/App.vue`                                                                 | +89 +25     |
| `backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`                 | +140 +13    |
| `frontend/src/views/info/InfoPage.vue`                                                 | +102        |
| `frontend/src/views/hotel/AttractionList.vue`                                          | +66 +57 +17 |
| `frontend/src/views/hotel/HotelDetail.vue`                                             | +25 +24 +27 |
| `backend/src/main/java/com/travelmate/scheduler/PostAuditScheduler.java`               | +74 +19     |
| `frontend/src/components/SafeImage.vue`                                                | +55         |
| `frontend/src/utils/image.js`                                                          | +35         |
| `backend/src/main/java/com/travelmate/service/impl/TrafficOrderServiceImpl.java`       | +34 +11     |
| `frontend/src/router/index.js`                                                         | +34         |
| `frontend/src/views/community/Community.vue`                                           | +30 +8      |
| `frontend/src/views/user/UserProfile.vue`                                              | +32         |
| `frontend/src/views/order/MyOrders.vue`                                                | +15 +7      |
| `backend/src/main/java/com/travelmate/service/impl/HotelRoomStockServiceImpl.java`     | +24         |
| `backend/src/main/java/com/travelmate/service/impl/NotificationCenterServiceImpl.java` | +23         |
| `frontend/src/views/community/PostDetail.vue`                                          | +23 +13     |
| `frontend/src/views/hotel/HotelSearch.vue`                                             | +13 +6      |
| `frontend/src/controller/AiController.java`                                            | +28         |
| `scripts/check-image-links.mjs`                                                        | +47         |
| `docs/admin-csv-import.md`                                                             | +38         |
| `AGENTS.md`                                                                            | +42         |
| `frontend/public/images/seed/` (20+ SVG 文件)                                          | 新增        |

_(忽略 `dist/`、`target/`、`.class` 等生成物。同一文件跨多次提交的增量已合并展示。)_

## 未完成事项

- 管理后台新增接口需补充权限校验的集成测试，确认非管理员访问返回 403。
- SVG 种子图片体系已落地，但部分旧页面仍直接使用 `<img>` 而非 `SafeImage`，需逐步替换。
- 多张票/多间房+优惠券联动的端到端下单回归测试尚未执行。
- 当前仓库历史仍包含 `frontend/dist/` 生成物，后续应确认是否从版本管理中清理。

## 明日计划

1. 对管理后台新增接口做权限边界人工验收，重点：非管理员调用返回 403、CSV 导入边界校验。
2. 对景点、酒店、热门城市、通知中心页面做完整人工验收，重点检查图片、空状态和移动端排版。
3. 补充多数量订单+库存回补+优惠券分类的端到端手动测试记录。
4. 评估并推进 `dist/`、`target/` 从 git 跟踪中移除的清理工作。

## 备注

- 提交时间顺序：`5af33846`（15:03）→ `304aa9d7`（21:02）→ `71587c29`（21:13，纯文档/日志）→ `179650ac`（23:27，晚上主提交）。
- 晚上提交 `179650ac` 是本日体量最大的单次提交（60 个文件，+2125 -332），包含管理后台、通知中心、种子图片和社区优化的绝大部分内容。
- 景点图片已同步修正前端映射、SQL 种子数据和当前 MySQL 数据库，避免只改种子数据但旧库不生效。
