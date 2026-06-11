# 修改日志 — 2026-05-22

| 字段      | 内容       |
| --------- | ---------- |
| 提交者    | yfan945    |
| 提交 Hash | `ea1e5aad` |

## 今日概述

> 本次提交（`ea1e5aad`，"功能修复-买票，评论"）以修复线上 Bug 为主。重点解决了酒店房间库存并发超卖问题、JWT 鉴权边界场景、限流器误拦截，以及游记草稿状态泄露等问题。管理后台（`AdminDashboard`）经过大幅重构，新增了订单流水、举报工单、敏感词等多个管理模块。同步更新了软件详细设计说明文档。

---

## 变更内容

### feat · 新功能

- **管理后台 · 多模块扩展**: `AdminDashboard.vue` 新增订单流水查看、评价举报工单处理、敏感词管理等功能面板（`AdminDashboard.vue`）

- **启动脚本 · 增强**: `start.ps1` 新增环境检测、自动加载 `.env`、分颜色日志输出等能力，开发体验改善（`start.ps1`）

### fix · Bug 修复

- **酒店 · 库存超卖**: `HotelRoomStockServiceImpl` 修复并发场景下房间库存判断逻辑，引入乐观锁防超卖（`HotelRoomStockServiceImpl.java`）

- **认证 · JWT 边界**: `JwtFilter` 修复 Bearer Token 解析异常时未正确返回 401 的问题；`SecurityConfig` 补充部分接口的公开访问白名单（`JwtFilter.java`, `SecurityConfig.java`）

- **限流 · 误拦截**: `RateLimiterInterceptor` 修复 IP 提取逻辑在反代场景下获取到代理 IP 导致误限流的问题（`RateLimiterInterceptor.java`）

- **社区 · 草稿泄露**: `PostServiceImpl` 修复草稿状态的游记在社区列表中对外可见的问题（`PostServiceImpl.java`）

- **数据库 · 启动校验**: `DatabaseStartupValidator` 优化启动时数据库连接失败的错误提示，区分"表不存在"和"连接失败"两种场景（`DatabaseStartupValidator.java`）

- **社区 · 评价举报**: `ReviewReport` 实体补全缺失字段；`PostDetail.vue` 修复举报逻辑调用错误（`ReviewReport.java`, `PostDetail.vue`）

### docs · 文档

- **详细设计说明**: 更新《5 组-软件详细设计说明.md/pdf》，补充新增模块的接口和数据流描述

- **数据库脚本**: `init.sql` 新增评价举报和初始化数据语句（`docs/sql/init.sql`）

### test · 测试

- **安全测试**: `SecurityConfigTests` 补充对新增公开接口的测试用例（`SecurityConfigTests.java`）

---

## 文件更改（关键源码）

| 文件                                                      | 说明                |
| --------------------------------------------------------- | ------------------- |
| `frontend/src/views/admin/AdminDashboard.vue`             | 多模块大幅扩展 +290 |
| `backend/.../service/impl/HotelRoomStockServiceImpl.java` | 库存并发修复 +56    |
| `backend/.../controller/AdminController.java`             | 接口重构 ~713 行    |
| `backend/.../backend/config/JwtFilter.java`               | JWT 边界修复 +18    |
| `backend/.../interceptor/RateLimiterInterceptor.java`     | 限流逻辑 +17        |
| `start.ps1`                                               | 启动脚本增强 +103   |
| `docs/sql/init.sql`                                       | 新增初始化数据 +10  |

---

## 未完成事项

- 管理后台各模块仅完成 UI 框架，部分功能（如批量操作、导出）待后续接入

## 明日计划

1. 全面联调本次 Bug 修复，确认线上超卖和 JWT 问题不再复现
2. 推进优惠券下单核销链路端到端验证
3. 社区评论/点赞模块 N+1 查询优化
