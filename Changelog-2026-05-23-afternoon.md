# 修改日志 — 2026-05-23（下午）

| 字段      | 内容                   |
| --------- | ---------------------- |
| 时间范围  | 2026-05-23 12:00 之后  |
| 提交者    | yfan945                |
| 提交 Hash | `5af33846`、`304aa9d7` |

## 今日概述

> 下午主要围绕 AI 审核、优惠券类型、订单数量/库存、目的地内容页和景点/酒店图片数据做集中完善。后端补齐了自动审核、优惠券分类、多人/多房下单和库存回补逻辑；前端新增热门城市详情、信息页和多处入口优化，并修正景点封面图与种子数据，使演示内容更完整。

## 变更内容

### feat · 新功能

- **AI 审核**: 新增 `PostAuditScheduler` 定时审核任务、`PostAuditResult` DTO 和 `AiService` 审核能力，支持帖子自动审核与后台审核统计展示（`PostAuditScheduler.java`, `AiServiceImpl.java`）。
- **优惠券分类**: `Coupon` 新增分类字段，优惠券服务支持按订单类型筛选可用券，前端优惠券中心和下单页同步适配（`Coupon.java`, `CouponServiceImpl.java`）。
- **订单数量**: 机票/火车票 DTO 新增 `ticketCount`，酒店订单新增 `roomCount`，下单、库存预扣减和超时回补逻辑支持多张票/多间房（`TrafficOrderServiceImpl.java`, `HotelOrderServiceImpl.java`）。
- **热门城市**: 新增城市数据源、城市列表页和城市详情页，支持从首页进入真实目的地资料、代表景点和出行建议（`destinations.js`, `DestinationList.vue`, `DestinationDetail.vue`）。
- **信息页**: 新增关于、隐私、服务条款等静态信息页，并接入路由（`infoPages.js`, `InfoPage.vue`, `router/index.js`）。
- **后台概览**: 管理后台补充 AI 审核、业务数据和展示字段，增强课程演示可观测性（`AdminController.java`, `AdminDashboard.vue`）。

### fix · Bug 修复

- **景点图片**: 替换热门景点中随机 `picsum` 图和失效 Wikimedia 文件名，补齐 48 个景点真实封面图，并同步更新当前数据库（`AttractionList.vue`, `docs/sql/init.sql`）。
- **酒店图片/房型**: 扩充酒店真实照片、补齐部分酒店房型和详情页展示字段，避免详情页空数据或图片不匹配（`HotelDetail.vue`, `HotelSearch.vue`, `docs/sql/init.sql`）。
- **库存回补**: 超时订单释放库存时按实际票数/房间数回补，避免多张票或多间房订单取消后库存恢复不准确（`OrderTimeoutScheduler.java`, `HotelRoomStockServiceImpl.java`）。
- **景点购票**: 景点购票接口改为接收成人票/儿童票数量，前端购票表单同步提交分项数量（`AttractionController.java`, `AttractionServiceImpl.java`, `AttractionList.vue`）。
- **前端顶部 UI**: 调整顶部导航、面包屑和首页入口，修正部分页面标题与导航跳转不一致的问题（`App.vue`, `Home.vue`）。

### refactor · 重构

- **库存接口**: Flight/Train/Hotel 库存预扣减方法统一按数量处理，接口语义更清晰（`FlightMapper.java`, `TrainMapper.java`, `HotelRoomMapper.java`）。
- **首页结构**: 首页功能区和热门城市入口重排，减少纯装饰内容，突出实际可点击业务入口（`Home.vue`）。
- **页面公共逻辑**: 部分页面标题、路由和展示字段统一命名，降低前端模块间不一致（`router/index.js`, `App.vue`）。

### docs · 文档

- **README**: 更新项目说明，补充下午新增的数据和功能说明（`README.md`）。
- **SQL 种子数据**: 扩充景点、酒店、房型、媒体来源和兼容迁移语句，便于重建库后保留完整演示数据（`docs/sql/init.sql`）。

### chore · 构建与数据

- **前端构建**: 多次执行 `npm.cmd run build` 验证新增页面和景点图片映射可通过生产构建。
- **数据库同步**: 直接更新本地 `travelmate.tm_attraction.cover_img`，使已初始化数据库无需重建即可看到新图片。

## 文件更改

| File                                                       | Changes |
| ---------------------------------------------------------- | ------- |
| `frontend/src/data/destinations.js`                        | +260    |
| `frontend/src/views/destination/DestinationDetail.vue`     | +241    |
| `frontend/src/views/destination/DestinationList.vue`       | +193    |
| `frontend/src/views/Home.vue`                              | +230 -69 |
| `docs/sql/init.sql`                                       | +258 -70 |
| `backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java` | +140 |
| `frontend/src/views/info/InfoPage.vue`                     | +102    |
| `frontend/src/data/infoPages.js`                           | +90     |
| `frontend/src/App.vue`                                     | +114 -16 |
| `backend/src/main/java/com/travelmate/scheduler/PostAuditScheduler.java` | +74 |
| `frontend/src/views/hotel/AttractionList.vue`              | +83     |
| `backend/src/main/java/com/travelmate/service/impl/TrafficOrderServiceImpl.java` | +38 |
| `frontend/src/router/index.js`                             | +34     |
| `backend/src/main/java/com/travelmate/service/impl/HotelRoomStockServiceImpl.java` | +24 |
| `frontend/src/views/admin/AdminDashboard.vue`              | +33     |

_(文件表只列关键源码与文档，忽略 `dist/`、`target/` 等生成物。)_

## 未完成事项

- 仍需在浏览器端逐页检查所有外链图片在校园/本地网络环境下的加载稳定性。
- 当前仓库历史中仍包含 `frontend/dist/` 和 `backend/target/` 生成物变更，后续应确认是否要从版本管理中清理。
- 多张票、多间房、优惠券联动已经打通主要字段，但仍建议补一次端到端下单回归。

## 明日计划

1. 对景点、酒店、热门城市三个展示页做完整人工验收，重点检查图片、空状态和移动端排版。
2. 补充订单数量、库存回补和优惠券分类的后端测试或手工测试记录。
3. 清理生成物跟踪策略，确认 `dist/`、`target/` 是否应继续入库。
4. 复查 README 与 SQL 初始化脚本，确保新同学重建环境后数据一致。

## 备注

- 下午提交 `5af33846` 时间为 15:03，提交 `304aa9d7` 时间为 21:02。
- 景点图片问题已同步修正前端映射、SQL 种子数据和当前 MySQL 数据库，避免只改种子数据但旧库不生效。
