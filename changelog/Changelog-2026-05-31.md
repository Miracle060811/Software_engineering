# 修改日志 - 2026-05-31

| 字段      | 内容                   |
| --------- | ---------------------- |
| 分支      | `main`                 |
| 提交者    | Yinghao Xiang、yfan945 |
| 提交 Hash | `3cc07753`、`bbdb7f25` |
| 推送状态  | 已推送到 `origin/main` |

## 今日概述

今天的工作分成两块：凌晨先把 AI 行程规划和管理后台 CSV 导入能力补齐到可演示状态，中午继续补订单详情、退票退款、景点购票和收藏链路。整体结果是 TravelMate 的 AI、后台管理、订单中心和用户内容链路都更完整，README 与数据库种子也同步跟进了新能力。

## 变更内容

### feat · New Feature

- **AI 行程规划增强**: 新增旅行节奏、必去地点、避开项、交通偏好和住宿偏好输入，生成结果补充交通建议、住宿建议、清单提醒、风险提示和每日备选方案，AI 页面交互也一起扩展（`backend/src/main/java/com/travelmate/dto/AiPlanCreateDTO.java`、`frontend/src/views/ai/AiPlan.vue`）。
- **AI 降级与客服兜底**: DeepSeek API Key 缺失、接口失败或返回结构不完整时，后端会回退到本地结构化行程模板；AI 客服也补了空消息、超长消息校验和兜底答复（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **后台 CSV 导入**: 管理后台新增航班、火车、酒店、房型、景点、城市资料的 CSV 批量导入，支持模板下载、dryRun 预检、insert 或 upsert 模式、UTF-8 BOM、多行字段和失败行汇总（`backend/src/main/java/com/travelmate/controller/AdminController.java`、`frontend/src/views/admin/AdminDashboard.vue`）。
- **订单与购票链路补全**: 新增景点订单实体和 Mapper，打通景点门票下单、酒店订单详情、交通订单详情与退票退款处理，订单中心同步展示更多明细信息（`backend/src/main/java/com/travelmate/entity/AttractionOrder.java`、`frontend/src/views/order/MyOrders.vue`）。
- **收藏与浏览历史**: 新增我的收藏页面，并引入前端浏览历史工具，把酒店、景点、社区详情等访问行为回流到首页最近浏览区（`frontend/src/views/user/MyCollections.vue`、`frontend/src/utils/browseHistory.js`）。

### fix · Bug Fix

- **后台资源校验**: 补齐座位或库存不能超过总量、酒店评分范围校验、火车运行时长自动计算等逻辑，减少管理台导入脏数据（`backend/src/main/java/com/travelmate/controller/AdminController.java`）。
- **AI 工具调用上下文**: 补齐工具调用结果里的 `tool_call_id`，避免 Function Calling 二次请求时上下文丢失（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **订单与图片显示细节**: 调整社区详情、景点列表、酒店详情、图片工具和订单页，修正部分详情展示、收藏入口和兜底图行为（`frontend/src/views/community/PostDetail.vue`、`frontend/src/utils/image.js`）。

### docs · Documentation

- **README 更新**: 补充 AI 行程、AI 客服、后台 CSV 导入、收藏页和订单链路相关说明，让当前能力和启动方式保持一致（`README.md`）。
- **CSV 使用文档**: 完善导入格式、模式参数、模板下载、预检和常见问题说明，方便管理后台联调（`docs/admin-csv-import.md`）。
- **历史日志补录**: 回填 5 月 28 日日志的推送状态与遗留事项说明（`Changelog-2026-05-28.md`）。

### chore · Build & Dependency

- **CSV 解析依赖**: 后端新增 `org.apache.commons:commons-csv:1.11.0`，替换原先偏手工的 CSV 解析方式。
- **Tomcat 兼容配置**: 新增 NIO2 协议配置，改善本地环境下的启动兼容性（`backend/src/main/java/com/travelmate/config/TomcatConfig.java`）。

## 文件更改

| File                                                                   | Changes        |
| ---------------------------------------------------------------------- | -------------- |
| `frontend/src/views/admin/AdminDashboard.vue`                          | +483 -46       |
| `backend/src/main/java/com/travelmate/controller/AdminController.java` | +423 -80       |
| `frontend/src/views/ai/AiPlan.vue`                                     | +320 -4        |
| `backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java` | +229 -10       |
| `frontend/src/views/order/MyOrders.vue`                                | +166 -6        |
| `frontend/src/views/user/MyCollections.vue`                            | new file, +184 |
| `docs/admin-csv-import.md`                                             | +51 -10        |
| `frontend/src/views/Home.vue`                                          | +88 -1         |
| `frontend/src/views/community/Community.vue`                           | +66 -4         |
| `docs/sql/init.sql`                                                    | +23 -2         |
| `backend/src/main/java/com/travelmate/entity/AttractionOrder.java`     | new file, +31  |
| `frontend/src/utils/browseHistory.js`                                  | new file, +20  |

## 验证情况

- 已核对今天共有两笔提交，分支为 `main`，当前提交已推送到远端。
- 本次日志按 Git 提交与文件统计生成，文件更改表已排除 `backend/target` 下的编译产物。
- 今日提交记录里未看到新的测试命令或自动化测试结果，当前验证仍以代码变更和提交范围核对为主。

## 未完成事项

- 后台 CSV 导入功能已上线，但还缺少更系统的真实样例回归和自动化测试覆盖。
- 订单、收藏和浏览历史链路已经接通，但前端几个大页面的体积还偏大，后续仍可以继续拆分懒加载。
- 景点订单相关能力已入库，老数据库若没重跑初始化脚本，可能还缺 `tm_attraction_order` 等新表结构。

## 明日计划

- 用真实 CSV 样例把预检、重复更新、失败行回显完整跑一遍，并补后端测试。
- 继续联调景点订单、交通退票和酒店退款链路，确认管理后台与用户侧状态一致。
- 评估订单中心、管理后台和社区页面的按路由拆包，降低构建 chunk 体积。

## 备注

- 本日志基于 2026-05-31 当天可见 Git 提交与文件统计重写，修正了旧版本只覆盖凌晨一笔提交的问题。
