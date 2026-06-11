# 修改日志 — 2026-05-13

| 字段      | 内容       |
| --------- | ---------- |
| 提交者    | yfan945    |
| 提交 Hash | `d4653af3` |

## 今日概述

> 本次提交（`d4653af3`，"目前进展"）是项目迄今为止功能量最大的单次提交，新增了优惠券、文件上传、评价商家回复、评价举报、一日游产品五个全新子系统，前后端均同步实现。同时完成了 AI 服务的降级机制、敏感词过滤增强，以及基础设施层面的 `.gitignore`、`.env.example`、`setup.ps1` 一键启动脚本的补齐。前端新增优惠券中心页和 ECharts 价格趋势组件。

---

## 变更内容

### feat · 新功能

- **优惠券子系统**: 新增 `Coupon`、`UserCoupon` 实体及对应 Mapper/Service/Controller，支持满减/折扣类型优惠券的发放和核销；前端新建优惠券中心页 `CouponCenter.vue`（`CouponController.java`, `CouponServiceImpl.java`, `CouponCenter.vue`）

- **文件上传**: 新增 `FileController` 支持头像/游记封面图片上传，返回访问 URL（`FileController.java`）

- **评价 · 商家回复**: 新增 `Reply` 实体和 `ReplyController`，允许酒店/景点商家对评价进行官方回复（`ReplyController.java`, `Reply.java`）

- **评价 · 举报**: 新增 `ReviewReport` 实体和 `ReviewReportController`，用户可对违规评价发起举报，管理员可处理工单（`ReviewReportController.java`, `ReviewReport.java`）

- **一日游产品**: 新增 `TourProduct` 实体、Mapper、Service、Controller，支持景区周边一日游产品的增删查（`TourProductController.java`, `TourProductServiceImpl.java`）

- **Review · 标签字段**: `Review` 实体新增 `tags` 字段，支持评价打标签（逗号分隔）（`Review.java`）

- **价格趋势图**: 前端新增 `PriceTrend.vue` ECharts 弹窗组件，展示航班/火车价格历史趋势（`PriceTrend.vue`）

- **前端路由**: `router/index.js` 新增优惠券中心、一日游、文件上传等新页面路由（`router/index.js`）

### fix · Bug 修复

- **点赞 · 重复点赞**: `LikeServiceImpl` 修复重复点赞不报错的问题，改为幂等处理（`LikeServiceImpl.java`）

- **社区 · 发帖**: `PostServiceImpl` 修复创建游记时 `userId` 未从 `UserContext` 取而是从前端传入的安全问题（`PostServiceImpl.java`）

- **AI · 降级兜底**: `AiServiceImpl` 优化 DeepSeek API 超时/失败时的降级逻辑，改为返回预设模板而非抛异常（`AiServiceImpl.java`）

- **交通订单**: `TrafficOrderController` 修复下单时优惠券字段未传导致空指针（`TrafficOrderController.java`）

### chore · 配置与工程

- **环境变量规范**: 新增 `.env.example` 模板，明确 `DB_PASSWORD`、`DEEPSEEK_API_KEY` 等必填项（`.env.example`）

- **Git 忽略规则**: 新增 `.gitignore`，排除 `.env`、`application-local.yml`、`target/` 等敏感/生成文件（`.gitignore`）

- **一键启动脚本**: 新增 `setup.ps1`，支持自动加载 `.env`、初始化数据库、启动前后端（`setup.ps1`）

- **数据库 · 新表**: `init.sql` 新增 `tm_coupon`、`tm_user_coupon`、`tm_reply`、`tm_review_report`、`tm_tour_product` 五张表（`docs/sql/init.sql`）

### test · 测试

- **Security 测试**: 新增 `SecurityConfigTests.java`，覆盖新增公开接口的鉴权逻辑（`SecurityConfigTests.java`）

---

## 文件更改（关键源码）

| 文件                                                   | 说明             |
| ------------------------------------------------------ | ---------------- |
| `backend/.../controller/CouponController.java`         | 新建，优惠券接口 |
| `backend/.../controller/FileController.java`           | 新建，文件上传   |
| `backend/.../controller/ReplyController.java`          | 新建，商家回复   |
| `backend/.../controller/ReviewReportController.java`   | 新建，评价举报   |
| `backend/.../controller/TourProductController.java`    | 新建，一日游产品 |
| `backend/.../entity/Coupon.java`                       | 新建             |
| `backend/.../entity/UserCoupon.java`                   | 新建             |
| `backend/.../entity/Reply.java`                        | 新建             |
| `backend/.../entity/ReviewReport.java`                 | 新建             |
| `backend/.../entity/TourProduct.java`                  | 新建             |
| `backend/.../service/impl/CouponServiceImpl.java`      | 新建             |
| `backend/.../service/impl/TourProductServiceImpl.java` | 新建             |
| `frontend/src/views/order/CouponCenter.vue`            | 新建             |
| `frontend/src/components/PriceTrend.vue`               | 新建             |
| `.gitignore`                                           | 新建             |
| `.env.example`                                         | 新建             |
| `setup.ps1`                                            | 新建             |
| `docs/sql/init.sql`                                    | 新增五张表       |

---

## 未完成事项

- 优惠券核销在下单接口的调用路径未完全打通（Controller 层已接参数，Service 层待补）
- 一日游产品暂无前端展示页，仅有后端接口
- 文件上传服务仅支持本地存储，生产环境应对接 OSS

## 明日计划

1. 补充数据库种子数据，覆盖新增的五张表
2. 完善管理后台，接入举报工单处理功能
3. 修复已知的买票/评论 Bug
