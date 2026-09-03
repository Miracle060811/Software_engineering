# 测试原始报告与 CI 证据（交付副本）

本目录汇总测试相关的**原始报告与 CI 证据**，多数为归档副本（原件保留在工程/文档原位以维持构建与追溯）：

| 路径 | 内容 | 原件位置 |
| --- | --- | --- |
| `ci/` | CI 用例覆盖矩阵、API 覆盖、测试质量策略与测试报告（`microservice-api-coverage.json`、`microservice-use-case-matrix.json`、`use-case-test-matrix.json`、`test-report.md`、`test-report-uc14.md`、`test-quality-policy.json`、`test-completion-baseline-2026-08-27.md`、`remaining-use-case-one-person-plan.md`） | `docs/ci/` |
| `ci/use-case-traceability.md` | CI 用例追溯检查报告：UC01–UC19 全部 covered（19/19）、自动化证据分 38/38；由追溯检查生成，留作追溯证据 | 追溯成果以 `02_docs/需求设计代码测试追溯表.md`（含 PDF）为准 |
| `playwright-report-real/` | 真实后端 E2E 的 Playwright HTML 报告（仅保留 `index.html` 报告，不含 `test-results` 缓存） | `frontend/playwright-report-real/` |
| `测试执行报告-2026-08-27 ~ 09-02.md` | 每日测试执行报告（5 篇） | `02_docs/测试执行报告-*.md` |
| `2026-09-01-microservices-maven-verify.log` | 六服务 Maven `verify` 原始日志（CI 构建证据）；按"本地日志不入库"约定保留在本机与 CI 流水线 Artifact，不随压缩包提交，构建结论见 `ci/*.md` | CI 流水线 Artifact |

压力测试原始结果见 `../stress/results/`，故障/迁移实验结果见 `../fault/results/`、`../migration/`；自动化测试源码快照见 `../automation/`。
