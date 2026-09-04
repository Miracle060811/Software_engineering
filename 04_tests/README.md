# 04\_tests 自动化测试与实验数据索引

本目录收纳自动化测试脚本、压力测试脚本、原始报告和实验数据。单元/集成/E2E 测试代码随代码仓库分布在 `backend/`、`frontend/`、`microservices/`（见 01\_source），本表给出定位与运行方法。

## 自动化测试定位

| 测试类型          | 代码位置                        | 运行命令                                                | 规模与结果                                                                                                   |
| ------------- | --------------------------- | --------------------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| 后端单体单元 + 集成测试 | `backend/src/test/`         | `mvn verify`（Failsafe 在真实 MySQL 容器中跑集成测试）           | 47 个测试类 205 个测试方法；覆盖重复支付拒绝、库存竞态回滚、越权隔离等异常分支                                                             |
| 六微服务单元 + 契约测试 | `microservices/*/src/test/` | 在 `microservices/` 下 `mvn verify`（单次 Maven reactor） | 覆盖矩阵登记 119 个端点（100 公开 + 19 内部）；`npm run check:microservice-api` 已于 2026-09-04 通过                        |
| 前端单元测试        | `frontend/`                 | `npm run test:unit`（Vitest）                         | 26 个用例                                                                                                  |
| 端到端 E2E       | `frontend/tests/e2e-real/`  | `npm run test:e2e:microservices`（Playwright 真实浏览器）  | 17 个场景联合覆盖 UC01–UC19；微服务版于 2026-09-02 在流水线跑通 17/17（GitHub Actions run 33582799133），原始报告见 `reports/e2e/` |

## 实验与原始数据

| 目录                                | 内容                                                                                                                                                                                                     |
| --------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `stress/`                         | k6 压测脚本（航班查询、酒店查询、并发下单）；HPA 编排脚本位于 `scripts/experiments/Invoke-MicroserviceHpaExperiment.ps1`（见 `stress/README.md`），本目录仅保留其运行结果；`results/` 内含性能对比 18 次运行原始 JSON/日志、HPA 实验记录与全过程截图（`results/evidence/`） |
| `fault/`                          | identity-service 停机故障注入实验脚本（Docker 与 K8s 两版）；`results/` 内含 503 降级、零脏数据的原始 JSON/日志                                                                                                                      |
| `migration/`                      | 单体到六服务数据迁移验收脚本；`results/data-migration-2026-09-02/` 内含 31/31 表行数一致的 summary 与日志                                                                                                                        |
| `reports/ci/`                     | 用例追溯历史校验报告（19 covered / 0 待测；开发期由机器校验保障，交付目录重整后该门禁步骤已从流水线移除，报告保留）                                                                                                                                      |
| `reports/e2e/`                    | 17 场景 E2E 的 Playwright 本机原始报告（单文件 HTML，浏览器直接打开）                                                                                                                                                        |
| `reports/playwright-report-real/` | 真实后端 E2E 的 Playwright HTML 报告（仅保留 `index.html`，不含 `test-results` 缓存；原件位于 `frontend/playwright-report-real/`）                                                                                           |
| `reports/pipeline/33582799133/` | GitHub Actions 原始 Artifact：后端测试、真实后端 E2E、微服务 E2E、用例追溯报告，共 330 个文件（交付副本另见 `../03_devops/pipeline/run-33582799133/`） |                                                                                                                                     |

## 验收证据确认

1. GitHub Actions run 33582799133 的 4 个测试证据 Artifact 已归档到 `reports/pipeline/33582799133/`，详细目录与远程来源见 `reports/README.md`。
2. 旅游订单接口的正常、鉴权、参数/越权分支，以及管理仪表盘 JWT 管理员鉴权与下游故障降级分支，分别由 `TourBookingApiContractTests`、`OpsDashboardIntegrationTests` 覆盖；对应 API 映射已通过静态门禁。

## 说明

- 压测与实验脚本均带自动断言（如防超卖断言"成功订单数 ≤ 初始库存"），结果由机器判定。

- 性能对比口径：同一台机器、同批数据、同一脚本、每场景每版本 3 次取中位数；详见 `stress/README.md`。

