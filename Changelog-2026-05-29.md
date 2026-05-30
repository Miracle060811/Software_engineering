# 修改日志 - 2026-05-29

| 字段      | 内容                                                                               |
| --------- | ---------------------------------------------------------------------------------- |
| 分支      | `main`                                                                             |
| 提交者    | yfan945、Yinghao Xiang                                                             |
| 提交 Hash | `5bbaf414`、`ef2b9e22`、`3b7e08ca`、`e905f116`、`a3e74c0f`、`690abd35`、`8873df53` |

## 今日概述

今天的工作主线是把“城市资料 + 资源展示 + 图片兜底”这一整套体验串起来。后端新增热门城市资料表与公开接口，后台扩展了城市资源管理和 CSV 导入，前端则同步调整目的地页、搜索页、首页和登录页；当日后半段又继续重构图片加载逻辑，把部分景点图片切到本地真实资源，并收紧了图片兜底与展示策略。

## 变更内容

### feat · New Feature

- **城市资料体系落地**: 新增 `tm_destination` 表、`Destination` 实体、Mapper 与 `/api/destinations` 公开接口，支持按排序输出城市列表、按 `slug` 查询详情（`backend/src/main/java/com/travelmate/controller/DestinationController.java`、`docs/sql/init.sql`）。
- **后台城市资源管理**: 管理后台新增城市资源分页展示、状态控制和 CSV 导入，导入时支持 `slug` 重复覆盖更新，便于后续直接维护热门城市内容（`backend/src/main/java/com/travelmate/controller/AdminController.java`、`frontend/src/views/admin/AdminDashboard.vue`）。
- **前端目的地动态读取**: 目的地列表页和详情页改为优先请求后端城市资料，接口无数据时自动回退到静态数据，避免后端未初始化时页面空白（`frontend/src/utils/destinations.js`、`frontend/src/views/destination/DestinationList.vue`）。
- **管理员注册入口**: 新增管理员注册接口与登录页隐藏弹窗，允许用密钥创建管理员账号，减少直接改库创建账号的成本（`backend/src/main/java/com/travelmate/backend/controller/UserController.java`、`frontend/src/views/Login.vue`）。

### fix · Bug Fix

- **公开接口放行**: 安全配置补充放行城市资料接口和管理员注册接口，修复未登录状态下目的地页无法读取数据的问题（`backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java`）。
- **密码重置流程改顺**: 登录页将“修改密码”改为“忘记密码”，后端同步为按用户名直接重置新密码，减少原流程理解成本（`backend/src/main/java/com/travelmate/backend/service/UserService.java`、`frontend/src/views/Login.vue`）。
- **城市实体字段映射**: `Destination.desc` 增加数据库字段映射声明，规避保留字导致的查询或写入风险（`backend/src/main/java/com/travelmate/entity/Destination.java`）。
- **后台审核与操作区修整**: 审核按钮允许对已处理游记重新改判，同时移除重复的库存操作按钮，减少后台表格干扰（`frontend/src/views/admin/AdminDashboard.vue`、`backend/src/main/java/com/travelmate/controller/AdminController.java`）。

### refactor · Refactoring

- **图片工具与兜底逻辑重构**: 简化前端图片工具函数，统一景点、酒店、社区等模块的图片回退逻辑，并更新默认 fallback 资源，减少旧 seed 图与随机占位图混用（`frontend/src/utils/image.js`、`frontend/public/images/seed/fallback.svg`）。
- **图片素材逐步本地化**: 新增一批本地真实景点图片资源，并在首页、景点列表、酒店详情、社区列表等位置切换为新的图片判定与引用策略（`frontend/public/images/real/attractions`、`frontend/src/views/hotel/AttractionList.vue`）。

### style · UI / Visual Polish

- **首页旅行规划区重排**: 重新梳理首页 Hero、旅行规划区、功能卡片和 CTA 视觉层次，统一双栏区块宽度、入口节奏与配图风格（`frontend/src/views/Home.vue`）。
- **搜索页卡片统一调性**: 航班、火车和酒店搜索结果区改为更轻的卡片背景、边框和价格强调方式，提升资源类页面的一致性（`frontend/src/views/flight/FlightSearch.vue`、`frontend/src/views/train/TrainSearch.vue`）。
- **管理后台和登录页细节优化**: 管理后台从深色转为浅色蓝绿系风格，登录页补齐管理员注册和忘记密码弹窗体验，主按钮文字色也统一固定为白色（`frontend/src/views/admin/AdminDashboard.vue`、`frontend/src/styles/theme.css`）。

### docs · Documentation

- **CSV 与环境变量说明补齐**: README、CSV 导入文档和 `.env.example` 一并补充城市资源字段、`ADMIN_REGISTER_SECRET` 和导入示例，降低后续接手成本（`README.md`、`docs/admin-csv-import.md`、`.env.example`）。
- **日志回填**: 当天已有一次 0529 日志整理提交，后续又根据实际改动补充和修正归类（`Changelog-2026-05-29.md`）。

### chore · Generated Assets

- **生成产物仍被带入提交**: 当天提交仍包含 `frontend/dist/index.html` 和 `.codex-pet-runs/travelfox` 下的预览、验证产物，属于生成类文件，后续需要统一决定是否继续纳入版本库（`frontend/dist/index.html`、`.codex-pet-runs/travelfox/qa/review.json`）。

## 文件更改

| File                                                                          | Changes                                  |
| ----------------------------------------------------------------------------- | ---------------------------------------- |
| `backend/src/main/java/com/travelmate/controller/AdminController.java`        | 城市资源导入、内容审核与后台资源管理扩展 |
| `backend/src/main/java/com/travelmate/controller/DestinationController.java`  | 新增公开城市资料接口                     |
| `backend/src/main/java/com/travelmate/entity/Destination.java`                | 新增实体并补充保留字字段映射             |
| `backend/src/main/java/com/travelmate/mapper/DestinationMapper.java`          | 新增 Mapper                              |
| `backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java`     | 放行城市资料与管理员注册接口             |
| `backend/src/main/java/com/travelmate/backend/controller/UserController.java` | 管理员注册与认证流程微调                 |
| `backend/src/main/java/com/travelmate/backend/service/UserService.java`       | 忘记密码逻辑调整                         |
| `frontend/src/views/admin/AdminDashboard.vue`                                 | 城市资源管理、审核交互与后台视觉改版     |
| `frontend/src/views/Home.vue`                                                 | 首页旅行规划区与功能卡片连续打磨         |
| `frontend/src/views/Login.vue`                                                | 管理员注册与忘记密码弹窗增强             |
| `frontend/src/utils/destinations.js`                                          | 目的地接口读取与静态兜底封装             |
| `frontend/src/utils/image.js`                                                 | 图片兜底与资源判定逻辑重构               |
| `frontend/src/views/flight/FlightSearch.vue`                                  | 搜索结果卡片视觉优化                     |
| `frontend/src/views/train/TrainSearch.vue`                                    | 搜索结果卡片视觉优化                     |
| `frontend/src/views/hotel/HotelSearch.vue`                                    | 搜索结果与图片展示优化                   |
| `frontend/src/views/hotel/HotelDetail.vue`                                    | 酒店详情图片与视觉细节优化               |
| `frontend/public/images/real/attractions`                                     | 新增本地真实景点图片资源                 |
| `docs/sql/init.sql`                                                           | 新增城市资料表及配套 seed 数据           |
| `README.md`                                                                   | 城市资源与启动说明更新                   |

## 验证情况

- 根据当日提交记录，前端相关改动已至少进行过一次构建产物更新，说明 UI 调整后执行过前端构建流程。
- 当天日志与代码改动中未看到新增自动化测试记录；城市资料接口、管理员注册和图片逻辑主要依赖手动联调验证。

## 未完成事项

- 城市资源后台目前仍以导入和下线为主，缺少完整的新增、编辑表单能力。
- 管理员注册密钥虽然已支持环境变量，但生产环境是否显式配置仍需确认。
- 图片逻辑已开始去随机占位图，但酒店、景点、社区三块仍有不少外链图片没有完全本地化。
- `frontend/dist` 与 `.codex-pet-runs` 这类生成产物是否保留入库尚未形成统一规则。

## 明日计划

- 继续清理图片来源，把高频展示资源逐步迁到本地真实图片目录。
- 补足城市资源后台的编辑能力和基本操作闭环。
- 对目的地接口、管理员注册和忘记密码流程做一次更完整的手动验证记录。
- 梳理生成产物和临时文件的提交边界，避免后续日志和功能提交混杂。

## 备注

- 本次重写基于 2026-05-29 当天全部可见提交重新归纳，而不是只沿用原先日志中的前三个提交。
