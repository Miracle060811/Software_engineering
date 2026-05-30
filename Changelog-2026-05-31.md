# 修改日志 - 2026-05-31

| 字段      | 内容 |
| --------- | ---- |
| 分支      | `main` |
| 提交者    | Yinghao Xiang |
| 提交 Hash | 本日志随本次提交一起生成，最终 Hash 见 Git 历史 |
| 推送状态  | 随本次提交推送到 `origin/main` |

## 今日概述

今天重点完善了 TravelMate 的 AI 行程规划、AI 降级体验、管理后台资源 CSV 导入和后台接入能力。改动覆盖后端接口、前端管理台、AI 行程页和使用文档，让无 API Key、CSV 格式复杂、导入前预检等场景都有更稳的兜底路径。

## 变更内容

### feat · New Feature

- **AI 行程规划**: 新增旅行节奏、必去地点、避开项、交通偏好和住宿偏好输入，并在生成结果中输出交通建议、住宿建议、行前清单、风险提醒、每日餐饮安排、当天备选和 Plan B（`backend/src/main/java/com/travelmate/dto/AiPlanCreateDTO.java`、`frontend/src/views/ai/AiPlan.vue`）。
- **AI 服务兜底**: DeepSeek API Key 缺失、调用失败或返回结构不完整时，自动生成本地结构化行程模板；AI 客服也补充空消息、超长消息校验和本地兜底答复（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **后台 CSV 导入**: 管理后台支持航班、火车、酒店、房型、景点、城市资料 CSV 批量导入，新增模板下载、dryRun 预检、insert/upsert 模式、UTF-8 BOM、引号、多行字段和失败行汇总（`backend/src/main/java/com/travelmate/controller/AdminController.java`、`frontend/src/views/admin/AdminDashboard.vue`）。
- **后端启动配置**: 增加 Tomcat NIO2 协议配置，提升本地启动兼容性（`backend/src/main/java/com/travelmate/config/TomcatConfig.java`）。

### fix · Bug Fix

- **导入校验**: 补齐座位/库存不能超过总量、酒店评分范围、火车运行时长自动计算等后台资源校验，减少脏数据进入库内（`backend/src/main/java/com/travelmate/controller/AdminController.java`）。
- **AI 工具调用**: 补齐工具调用结果中的 `tool_call_id`，避免 Function Calling 二次请求上下文不完整（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。

### docs · Documentation

- **README**: 更新 AI 行程、AI 客服、后台 CSV 导入和技术栈说明，补充 Apache Commons CSV 依赖记录（`README.md`）。
- **CSV 文档**: 完善导入格式、模式参数、模板下载、预检和常见问题说明（`docs/admin-csv-import.md`）。
- **历史日志**: 补充 2026-05-28 日志的推送状态和遗留说明（`Changelog-2026-05-28.md`）。

### chore · Build & Dependency

- **依赖**: 后端新增 `org.apache.commons:commons-csv:1.11.0`，替换手写 CSV 解析逻辑（`backend/pom.xml`）。

## 文件更改

| File | Changes |
| ---- | ------- |
| `backend/src/main/java/com/travelmate/controller/AdminController.java` | +373 -70 |
| `frontend/src/views/admin/AdminDashboard.vue` | +459 -35 |
| `frontend/src/views/ai/AiPlan.vue` | +320 -4 |
| `backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java` | +229 -10 |
| `docs/admin-csv-import.md` | +51 -10 |
| `README.md` | +5 -3 |
| `backend/pom.xml` | +5 -0 |
| `backend/src/main/java/com/travelmate/dto/AiPlanCreateDTO.java` | +5 -0 |
| `backend/src/main/java/com/travelmate/config/TomcatConfig.java` | new file |
| `Changelog-2026-05-28.md` | +8 -0 |

## 未完成事项

- 图片本地化任务因网络访问 Wikimedia 超时中断，本次提交未纳入未引用的半成品图片；后续需要继续时，应统一下载完整图片并同步 `docs/sql/init.sql`、前端静态数据和本地库。
- 前端构建仍存在大 chunk 警告，后续可对 ECharts、管理后台或大页面做懒加载拆分。

## 明日计划

- 继续完成种子数据和前端静态展示图的本地化，并补一条只更新图片字段的 SQL 脚本。
- 用真实 CSV 样例覆盖导入预检、重复更新和失败行展示，补充后端单元测试。
- 继续收敛 AI 行程结果展示，联动订单、酒店和景点数据做更真实的推荐。

## 备注

- 本日志由 `D:\Skill\daily-changelog` skill 生成，并基于当前未提交 diff 汇总。
