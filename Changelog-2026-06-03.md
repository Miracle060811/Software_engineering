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

注：已将sylphira_server最新的更新同步到main

## 变更内容

### feat · New Feature

- **DeepSeek 环境变量配置支持**: 将 AI 服务的 API Key 获取方式改为通过 Spring `Environment` 读取，优先使用标准环境变量注入，避免服务运行时主动扫描项目根目录 `.env` 文件，更适合宝塔、systemd、Docker 或 CI/CD 等服务器部署场景（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **聊天补全路径独立配置**: 新增 `DEEPSEEK_CHAT_COMPLETIONS_PATH` 配置项，DeepSeek Chat Completions 接口路径不再硬编码在业务逻辑里，方便在不同网关、代理或兼容接口环境下单独调整（`backend/src/main/resources/application.yml`）。
- **服务器部署准备补齐**: 完成 `sylphira_server` 分支部署前检查，确认后端可启动、前端可完成基础访问、AI Key 可通过环境变量注入，并整理宝塔面板中配置 DeepSeek 变量的操作说明（`docs/deepseek-baota-env.md`）。

### fix · Bug Fix

- **移除运行时 `.env` 扫描副作用**: 删除 AI 服务内部对本地 `.env` 文件的运行时查找与解析逻辑，避免部署目录变化、工作目录不一致或文件权限限制导致 API Key 读取失败，也减少敏感配置被业务代码重复处理的风险（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **AI 降级链路确认**: 检查 DeepSeek API Key 缺失、接口超时、响应异常等场景下的降级行为，确认 AI 行程规划与客服逻辑仍会回退到本地模板或兜底回复，不阻塞主要业务流程（`backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java`）。
- **数据库结构缺失排查**: 冒烟测试过程中发现本地库缺少 `tm_destination` 与 `tm_attraction_order` 两张表，已定位为历史库未重新执行初始化 SQL 导致的问题，并记录为部署前必须补齐的数据库迁移检查项。
- **Redis 未启动场景检查**: 验证 Redis 未启动时项目的启动与业务降级表现，确认 AI 服务不依赖 Redis，涉及限流或库存缓存的接口需要在正式服务器上提前启动 Redis 或明确降级策略。

### docs · Documentation

- **新增宝塔环境变量说明**: 新增 `docs/deepseek-baota-env.md`，记录在宝塔面板中配置 `DEEPSEEK_API_KEY` 与 `DEEPSEEK_CHAT_COMPLETIONS_PATH` 的方式，并说明配置完成后需要重启后端服务使环境变量生效。
- **补充 DeepSeek 配置说明**: 在文档中明确本项目不再依赖运行时扫描 `.env`，本地开发仍可通过启动脚本加载变量，服务器部署则推荐直接配置系统环境变量或进程环境变量。
- **记录部署前检查结果**: 将数据库缺表、Redis 状态、AI 降级、Maven 编译、项目启动等检查结论纳入本日日志，方便后续继续推进服务器上线。

## 文件更改

| File                                                                  | Changes                                  |
| --------------------------------------------------------------------- | ---------------------------------------- |
| `backend/src/main/java/com/travelmate/service/impl/AiServiceImpl.java` | +42 -68，重构 DeepSeek 配置读取与降级检查 |
| `backend/src/main/resources/application.yml`                          | +9 -2，新增 DeepSeek 路径环境变量配置     |
| `docs/deepseek-baota-env.md`                                          | new file, +76，新增宝塔环境变量部署说明   |

## 验证情况

- 已同步并合并 `main` 到 `sylphira_server`，当前分支用于服务器部署准备。
- 已检查项目整体运行状态，完成前后端基础冒烟测试，核心页面与后端接口可进入联调流程。
- 已执行 Maven 编译验证，后端编译通过。
- 已验证项目可正常启动，DeepSeek 配置缺失时不会阻断 Spring Boot 启动。
- 已检查 AI 服务降级逻辑，DeepSeek API 不可用时可返回本地兜底结果。
- 已检查 Redis 未启动情况下的表现，确认需要在正式部署时补充 Redis 启动检查或明确跳过策略。
- 已排查数据库结构缺失问题，确认本地库缺少 `tm_destination` 与 `tm_attraction_order`，需要通过初始化 SQL 或迁移脚本补齐。

## 未完成事项

- 当前数据库缺少 `tm_destination` 与 `tm_attraction_order` 的问题尚未直接在本地库中修复，需要在部署数据库初始化或迁移环节补齐。
- Redis 未启动时，部分依赖限流或库存缓存的接口仍需要进一步确认业务表现，正式服务器建议优先保证 Redis 服务可用。
- DeepSeek 环境变量配置已完成代码与文档准备，但仍需在真实宝塔服务器上按文档执行一次完整验证。

## 明日计划

- 在服务器上按 `docs/deepseek-baota-env.md` 配置 DeepSeek 环境变量，并重启后端验证 AI 接口实际调用结果。
- 对照 `docs/sql/init.sql` 检查线上数据库结构，补齐 `tm_destination`、`tm_attraction_order` 等新增表。
- 启动 Redis 后重新跑一轮下单、限流、库存预扣减相关接口，确认服务器环境与本地表现一致。
- 完成前端生产构建与后端部署联调，整理最终上线检查清单。

## 备注

- 本日志基于 2026-06-03 在 `sylphira_server` 分支上的服务器部署准备工作整理。
- 本次提交信息为 `support deepseek env configuration`，提交 Hash 记录为 `8f3a2c9d`。
- 后续部署时应优先使用服务器环境变量管理敏感配置，不建议把真实 DeepSeek API Key 写入仓库文件。
