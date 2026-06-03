# 修改日志 - 2026-06-03

| 字段      | 内容                                |
| --------- | ----------------------------------- |
| 分支      | `sylphira_server`                   |
| 提交者    | Sylphira                            |
| 提交 Hash | `8f3a2c9d`                          |
| 提交信息  | `support deepseek env configuration` |

## 今日概述

今天的工作重点是把 `main` 的最新改动同步到 `sylphira_server`，并围绕服务器部署前的运行稳定性做了一轮完整检查。合并完成后，重点验证了前后端冒烟流程、后端启动状态、Maven 编译结果、数据库结构完整性、Redis 未启动时的降级表现，以及 AI 服务在 DeepSeek 配置缺失或不可用时的兜底逻辑。

本次调整的核心落点在 DeepSeek 配置读取方式：移除了运行时扫描 `.env` 文件的逻辑，改为完全依赖 Spring 环境变量与配置项读取 API Key，并新增 `DEEPSEEK_CHAT_COMPLETIONS_PATH` 以适配服务器环境中的路径配置。同步新增宝塔部署环境变量说明文档，为后续线上部署和同学接手排查提供更清晰的操作依据。

后续又围绕内置 DeepSeek 客服体验做了补充规范：加厚系统提示词但收紧输出格式，明确禁止 Markdown 大段落、星号加粗、表格和工具协议文本直出；同时给 AI 客服请求补充用户设备日期和时区，让“今天、明天、后天”等相对时间可以按用户本机日期换算，避免模型自行猜日期。

注：已将sylphira_server最新的更新同步到main

## 变更内容

### feat · New Feature

- **DeepSeek 环境变量配置支持**: 将 AI 服务的 API Key 获取方式改为通过 Spring `Environment` 读取，优先使用标准环境变量注入，避免服务运行时主动扫描项目根目录 `.env` 文件，更适合宝塔、systemd、Docker 或 CI/CD 等服务器部署场景（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **聊天补全路径独立配置**: 新增 `DEEPSEEK_CHAT_COMPLETIONS_PATH` 配置项，DeepSeek Chat Completions 接口路径不再硬编码在业务逻辑里，方便在不同网关、代理或兼容接口环境下单独调整（`backend/src/main/resources/application.yml`）。
- **服务器部署准备补齐**: 完成 `sylphira_server` 分支部署前检查，确认后端可启动、前端可完成基础访问、AI Key 可通过环境变量注入，并整理宝塔面板中配置 DeepSeek 变量的操作说明（`docs/deepseek-baota-env.md`）。
- **AI 客服提示词规范化**: 加厚 TravelMate 中文旅行助手提示词，补充路线、住宿、交通、预算、实时信息边界和安全拒绝规则，同时明确要求自然客服聊天风格，禁止星号、加粗、表格、Markdown 标题、分隔线和超长攻略式输出（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **用户设备日期上下文**: AI 客服请求新增 `clientDate` 与 `clientTimeZone`，前端从浏览器本地时间生成并传给后端，后端每轮动态注入 system prompt，用于处理“今天/明天/后天/下周”等相对日期（`backend/src/main/java/com/travelmate/dto/AiChatDTO.java`、`frontend/src/views/ai/AiPlan.vue`）。

### fix · Bug Fix

- **移除运行时 `.env` 扫描副作用**: 删除 AI 服务内部对本地 `.env` 文件的运行时查找与解析逻辑，避免部署目录变化、工作目录不一致或文件权限限制导致 API Key 读取失败，也减少敏感配置被业务代码重复处理的风险（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **AI 降级链路确认**: 检查 DeepSeek API Key 缺失、接口超时、响应异常等场景下的降级行为，确认 AI 行程规划与客服逻辑仍会回退到本地模板或兜底回复，不阻塞主要业务流程（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **数据库结构缺失排查**: 冒烟测试过程中发现本地库缺少 `tm_destination` 与 `tm_attraction_order` 两张表，已定位为历史库未重新执行初始化 SQL 导致的问题，并记录为部署前必须补齐的数据库迁移检查项。
- **Redis 未启动场景检查**: 验证 Redis 未启动时项目的启动与业务降级表现，确认 AI 服务不依赖 Redis，涉及限流或库存缓存的接口需要在正式服务器上提前启动 Redis 或明确降级策略。
- **DeepSeek 工具协议泄漏兜底**: 针对模型偶发输出 `<｜｜DSML｜｜tool_calls>`、`invoke name=` 等工具调用协议文本的问题，新增检测与拦截逻辑；若标准 `tool_calls` 解析失败且内容疑似协议泄漏，则丢弃该回复并改走不带工具的普通对话兜底，避免协议文本直出到前端。
- **相对日期误判修复**: AI 客服不再依赖模型自行猜测当前日期；当用户询问“后天机票”等场景时，优先使用前端传入的用户设备日期进行换算，前端未传时再使用服务器日期兜底。

### docs · Documentation

- **新增宝塔环境变量说明**: 新增 `docs/deepseek-baota-env.md`，记录在宝塔面板中配置 `DEEPSEEK_API_KEY` 与 `DEEPSEEK_CHAT_COMPLETIONS_PATH` 的方式，并说明配置完成后需要重启后端服务使环境变量生效。
- **补充 DeepSeek 配置说明**: 在文档中明确本项目不再依赖运行时扫描 `.env`，本地开发仍可通过启动脚本加载变量，服务器部署则推荐直接配置系统环境变量或进程环境变量。
- **记录部署前检查结果**: 将数据库缺表、Redis 状态、AI 降级、Maven 编译、项目启动等检查结论纳入本日日志，方便后续继续推进服务器上线。
- **补充 AI 客服行为边界**: 在日志中记录 AI 客服目前不具备真实联网查票能力，航班与酒店工具优先查询项目本地数据；没有工具明确结果时，不应声称已查询实时库存、票价或天气。

## 文件更改

| File                                                                  | Changes                                  |
| --------------------------------------------------------------------- | ---------------------------------------- |
| `backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java` | +128 -74，重构 DeepSeek 配置读取、提示词规范、工具协议兜底与日期上下文 |
| `backend/src/main/resources/application.yml`                          | +9 -2，新增 DeepSeek 路径环境变量配置     |
| `backend/src/main/java/com/travelmate/dto/AiChatDTO.java`              | +2，新增用户设备日期与时区字段             |
| `frontend/src/views/ai/AiPlan.vue`                                     | +5，发送浏览器本地日期与时区给 AI 客服      |
| `docs/deepseek-baota-env.md`                                          | new file, +76，新增宝塔环境变量部署说明   |

## 验证情况

- 已同步并合并 `main` 到 `sylphira_server`，当前分支用于服务器部署准备。
- 已检查项目整体运行状态，完成前后端基础冒烟测试，核心页面与后端接口可进入联调流程。
- 已执行 Maven 编译验证，后端编译通过。
- 已验证项目可正常启动，DeepSeek 配置缺失时不会阻断 Spring Boot 启动。
- 已检查 AI 服务降级逻辑，DeepSeek API 不可用时可返回本地兜底结果。
- 已验证 AI 客服提示词调整后后端可正常编译，新增字符串文本块和 JSON 拼接未破坏 Java 编译。
- 已验证前端生产构建通过，AI 客服请求新增的 `clientDate` 与 `clientTimeZone` 不影响页面构建。
- 已补充工具协议泄漏检测逻辑，避免 DeepSeek 非标准 DSML 工具调用文本直接进入用户聊天窗口。
- 已检查 Redis 未启动情况下的表现，确认需要在正式部署时补充 Redis 启动检查或明确跳过策略。
- 已排查数据库结构缺失问题，确认本地库缺少 `tm_destination` 与 `tm_attraction_order`，需要通过初始化 SQL 或迁移脚本补齐。

## 未完成事项

- 当前数据库缺少 `tm_destination` 与 `tm_attraction_order` 的问题尚未直接在本地库中修复，需要在部署数据库初始化或迁移环节补齐。
- Redis 未启动时，部分依赖限流或库存缓存的接口仍需要进一步确认业务表现，正式服务器建议优先保证 Redis 服务可用。
- DeepSeek 环境变量配置已完成代码与文档准备，但仍需在真实宝塔服务器上按文档执行一次完整验证。
- AI 客服已补充日期上下文和格式约束，但仍需要在真实 DeepSeek 返回下继续观察是否还会出现过长回复、Markdown 残留或非标准工具协议输出。

## 明日计划

- 在服务器上按 `docs/deepseek-baota-env.md` 配置 DeepSeek 环境变量，并重启后端验证 AI 接口实际调用结果。
- 对照 `docs/sql/init.sql` 检查线上数据库结构，补齐 `tm_destination`、`tm_attraction_order` 等新增表。
- 启动 Redis 后重新跑一轮下单、限流、库存预扣减相关接口，确认服务器环境与本地表现一致。
- 完成前端生产构建与后端部署联调，整理最终上线检查清单。
- 继续用“后天机票”“三天行程”“自我介绍”等典型客服输入做回归，确认相对日期、自然输出和工具兜底表现稳定。

## 备注

- 本日志基于 2026-06-03 在 `sylphira_server` 分支上的服务器部署准备工作整理。
- 本次提交信息为 `support deepseek env configuration`，提交 Hash 记录为 `8f3a2c9d`。
- 后续部署时应优先使用服务器环境变量管理敏感配置，不建议把真实 DeepSeek API Key 写入仓库文件。
- 当前 AI 客服可以调用 DeepSeek 生成自然语言，但不等同于真实联网查票；实时机票、酒店库存和天气仍需要正式外部数据源或可靠本地接口支撑。

## 追加记录：12306 公开余票页面读取演示

### feat · New Feature

- **12306 页面自动化余票读取演示**: 新增 Playwright 浏览器自动化同步模块，用户点击火车搜索后才打开 `https://kyfw.12306.cn/otn/leftTicket/init`，按用户输入的出发站、到达站和日期填表并点击查询，从动态渲染后的页面 DOM 读取公开展示的车次、站点、时间、历时和席别余票信息；不登录、不下单、不候补、不支付、不绕过验证码或风控。
- **自由路线一视同仁同步**: 移除后端白名单限制，前端不再展示五条快捷路线，用户可直接输入任意 12306 可识别站点；读取成功后统一 upsert 到 `tm_train`，后台管理可看到动态同步出来的车次。
- **同步状态接口**: 新增 `/api/train/live-sync-status`，返回最近一次同步路线、日期、是否成功、失败原因、数据来源、车次数量和同步时间。
- **请求频率控制与兜底**: 同一路线同一天 8 分钟内复用同步状态和内存结果，不重复打开 12306 页面；页面读取失败、无结果或环境无法访问 12306 时，前端继续展示本地数据库/演示数据，不让页面空白。
- **火车搜索页体验优化**: 前端展示数据来源状态条，删除快捷路线按钮，避免误导为只能查固定路线；用户未选择出发日期时自动使用今天并弹出提示。

### verification · 验证

- 后端 `mvnw.cmd clean compile` 通过。
- 后端 `mvnw.cmd test` 通过，9 tests。
- 前端 `npm run build` 通过，仅保留 Vite 常规 chunk 体积提示。
- 本地实测 `南京南 -> 合肥南 / 2026-06-04` 页面读取成功，返回并写入 `tm_train` 83 条；第二次同路线同日请求命中缓存，`syncedAt` 未变化。
