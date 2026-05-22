# 修改日志 — 2026-05-19

| 字段   | 内容                                                                       |
| ------ | -------------------------------------------------------------------------- |
| 提交者 | Zyy_mie（`d70caf0f`）、Yinghao Xiang（`fb4cdf86`、`963dbc19`、`5330c9b4`） |

## 今日概述

> 今日共四个提交，集中在数据库内容完善和订单系统 Bug 修复两个方向。新增了景点/酒店补充数据和图片 URL 修正脚本；修复了航班、火车购票时的库存扣减和订单超时自动取消的 Bug；`Attraction` 实体补全了必要字段；前端购票搜索页也同步修复了相关交互问题。

---

## 变更内容

### feat · 新功能

- **订单 · 超时自动取消调度器**: `OrderTimeoutScheduler` 新增/改进超时订单自动取消逻辑，定时扫描未支付超时订单并释放库存（`OrderTimeoutScheduler.java`）

### fix · Bug 修复

- **交通票 · 购票流程**: 修复航班（`FlightController`、`FlightMapper`）和火车（`TrainMapper`）在高并发下库存查询不准确的问题；`TrafficOrderController` 修复下单时乘客信息校验遗漏（`TrafficOrderServiceImpl.java`, `TrafficOrderController.java`）

- **酒店 · 订单创建**: `HotelOrderServiceImpl` 修复酒店下单时房间库存扣减失败但订单仍然创建的问题（`HotelOrderServiceImpl.java`）

- **实体 · Attraction 字段**: `Attraction.java` 补全 `province`、`imageUrl` 等缺失字段，解决景点列表展示异常（`Attraction.java`）

- **前端 · 航班/火车搜索**: `FlightSearch.vue` 和 `TrainSearch.vue` 修复搜索条件不合法时仍发请求、以及下单按钮加载态未重置的问题（`FlightSearch.vue`, `TrainSearch.vue`）

- **用户上下文**: `UserContext` 修复在未登录场景调用 `getCurrentUserId()` 抛空指针而非返回 null 的问题（`UserContext.java`）

### chore · 数据维护

- **数据库 · 补充种子数据**: 新增 `data_supplement.sql`（景点、酒店房型等补充测试数据）和 `update_images_correct.sql`（批量修正图片 URL 错误）（`docs/sql/`）

- **数据库 · init.sql 多次更新**: 三个提交（`fb4cdf86`, `963dbc19`）累计多次修正初始化脚本，补全新表结构或修正字段类型（`docs/sql/init.sql`）

- **景点 · 前端列表**: `AttractionList.vue` 同步调整字段绑定，适配实体字段变化（`AttractionList.vue`）

---

## 文件更改（关键源码）

| 文件                                                    | 说明                  |
| ------------------------------------------------------- | --------------------- |
| `backend/.../scheduler/OrderTimeoutScheduler.java`      | 超时调度器改进        |
| `backend/.../service/impl/TrafficOrderServiceImpl.java` | 购票流程修复          |
| `backend/.../service/impl/HotelOrderServiceImpl.java`   | 酒店订单修复          |
| `backend/.../entity/Attraction.java`                    | 补全字段              |
| `backend/.../mapper/FlightMapper.java`                  | 库存查询修复          |
| `backend/.../mapper/TrainMapper.java`                   | 库存查询修复          |
| `frontend/src/views/flight/FlightSearch.vue`            | 搜索交互修复          |
| `frontend/src/views/train/TrainSearch.vue`              | 搜索交互修复          |
| `docs/sql/init.sql`                                     | 数据库脚本多次更新    |
| `docs/sql/data_supplement.sql`                          | 新增（种子数据）      |
| `docs/sql/update_images_correct.sql`                    | 新增（图片 URL 修正） |

---

## 未完成事项

- 超时调度器的边界测试（多实例部署场景下是否会重复取消）待验证
- 航班/火车的价格历史数据缺失，搜索结果展示不完整

## 明日计划

1. 验证超时订单取消逻辑，确认库存归还正确
2. 联调管理后台，修复已知 UI 异常
3. 完善购票前端的错误提示，让用户明确知道失败原因
