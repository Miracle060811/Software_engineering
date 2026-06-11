# 修改日志 — 2026-06-06

| 字段 | 内容 |
| --- | --- |
| 提交者 | yfan945、Sylphira-ovo |
| 提交 Hash | `70c62776`、`ebb890bb`、`980c38c9` |

## 今日概述

今天主要完成 TravelMate 的测试体系补齐、云端部署问题修复和测试报告整理。重点把后端 MockMvc、前端 Playwright、构建和启动脚本 DryRun 串成可重复执行的回归流程，同时修复前端 history 路由直接访问失败和景点订单表兼容问题。

## 变更内容

### feat · New Feature

- **自动化测试入口**: 新增统一测试脚本，串联后端测试、前端依赖安装、生产构建、Playwright E2E 和启动脚本 DryRun，方便一次性回归验证（`scripts/run-tests.ps1`）。
- **前端 E2E 测试**: 引入 Playwright 配置和 smoke 用例，覆盖公开页面渲染、匿名路由守卫、非管理员访问后台拦截和登录 token 写入（`frontend/playwright.config.js`、`frontend/tests/e2e/travelmate-smoke.spec.js`）。
- **后端测试覆盖**: 新增公开接口统一响应、安全边界、登录失败业务错误、库存预扣减状态和 `Result<T>` 响应结构测试（`backend/src/test/java/com/travelmate/PublicApiSmokeTests.java`、`backend/src/test/java/com/travelmate/ResultAndStockBoundaryTests.java`）。

### fix · Bug Fix

- **SPA 路由回退**: 新增 `SpaForwardController`，并放行主要前端 GET 路由，修复云端直接访问 `/login`、`/ai-plan`、`/coupons` 等 history 路由时返回 403 或空白页的问题（`backend/src/main/java/com/travelmate/config/SpaForwardController.java`、`backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java`）。
- **安全配置测试**: 为前端 history 路由免登录转发增加 MockMvc 断言，避免后续改安全规则时再次破坏前端直达路由（`backend/src/test/java/com/travelmate/SecurityConfigTests.java`）。
- **景点订单表兼容**: 在初始化 SQL 中补齐旧库 `tm_attraction_order` 表和字段、索引的兼容修复，解决云端景点购票链路因表缺失失败的问题（`docs/sql/init.sql`）。
- **演示图片数据**: 调整部分酒店、景点、社区图片映射，减少 seed 占位图依赖，并记录仍存在的外链图片加载风险（`docs/sql/init.sql`、`frontend/src/views/hotel/AttractionList.vue`）。

### test · Tests

- **完整回归记录**: 测试报告补充 AI 正常/降级、真实浏览器兼容性、普通用户登录、管理员后台入口、内容审核闭环、真实下单、酒店 50 并发和 Java 21 环境测试结果（`测试报告.md`）。
- **测试运行文档**: 新增测试 runbook，说明一键运行、单独运行、覆盖范围和未自动覆盖内容（`docs/test-runbook.md`）。

### docs · Documentation

- **README 测试说明**: 在 README 中加入自动化测试命令和测试报告入口，便于交付和复测（`README.md`）。
- **部署与课程文档**: 更新部署文档、开发计划、需求/设计说明等课程交付材料，补充当前系统状态和测试结果（`docs/部署文档.md`、`开发计划书.md`、`5组-软件需求规格说明.md`、`5组-软件详细设计说明.md`）。
- **测试报告口径整理**: 将已经完成的测试内容归入“测试结论”，未覆盖表只保留真实剩余风险，统一“已测试/验证”表述（`测试报告.md`）。

### chore · Maintenance

- **依赖与配置**: 增加 `@playwright/test` 依赖和 `test:e2e` 脚本，生成对应 lockfile 更新（`frontend/package.json`、`frontend/package-lock.json`）。
- **知识图谱文件**: 更新 `.understand-anything` 相关图谱、指纹和 dashboard 运行文件；这些属于辅助分析/生成产物，本日志不展开逐项说明。

## 文件更改

| File | Changes |
| --- | --- |
| `backend/src/main/java/com/travelmate/backend/config/SecurityConfig.java` | 放行前端 history GET 路由 |
| `backend/src/main/java/com/travelmate/config/SpaForwardController.java` | 新增 SPA 前端路由转发 |
| `backend/src/test/java/com/travelmate/SecurityConfigTests.java` | 增加 history 路由转发测试 |
| `backend/src/test/java/com/travelmate/PublicApiSmokeTests.java` | 新增公开接口和鉴权 smoke 测试 |
| `backend/src/test/java/com/travelmate/ResultAndStockBoundaryTests.java` | 新增统一响应和库存状态边界测试 |
| `frontend/playwright.config.js` | 新增 Playwright 配置 |
| `frontend/tests/e2e/travelmate-smoke.spec.js` | 新增前端 E2E smoke 用例 |
| `frontend/package.json` | 增加 Playwright 依赖和测试脚本 |
| `scripts/run-tests.ps1` | 新增一键回归脚本 |
| `docs/sql/init.sql` | 修复景点订单表兼容和演示图片数据 |
| `docs/test-runbook.md` | 新增测试运行说明 |
| `README.md` | 增加自动化测试入口说明 |
| `测试报告.md` | 更新测试执行结果、风险和最终结论 |

## 未完成事项

- 管理后台完整 CRUD 尚未全部自动化覆盖，仍需要按管理员角色继续人工回归新增、编辑、删除、分页和筛选。
- 文件上传、头像更新、本地 `/uploads/**` 访问和外链图片替换仍需要继续验证。
- AI 长时间、多轮、大量请求、额度异常和超时场景尚未形成稳定性报告。
- 并发压测目前覆盖酒店单房型 50 并发，航班、火车、景点门票和更长时间阶梯负载还未扩展。

## 明日计划

- 优先回归管理后台各业务表的 CRUD、分页和筛选，补齐验收演示风险点。
- 验证社区图片上传、头像更新和本地静态资源访问，并替换容易被浏览器拦截的 Wikimedia 外链。
- 扩展库存压测到航班、火车和景点门票，保留原始压测输出。
- 根据今天新增测试体系，清理不应入库的构建产物和辅助运行文件。

## 备注

- 今日提交中包含部分 `backend/target`、`frontend/dist`、`frontend/node_modules`、`.understand-anything` 和 PDF 产物变更，日志摘要已按源码、测试、脚本和关键文档优先整理。
