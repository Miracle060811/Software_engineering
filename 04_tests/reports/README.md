# 测试原始报告与 CI 证据（交付副本）

本目录汇总测试相关的**原始报告与 CI 证据**，多数为归档副本（原件保留在工程/文档原位以维持构建与追溯）：

| 路径 | 内容 | 原件位置 |
| --- | --- | --- |
| `ci/` | CI 用例覆盖矩阵、API 覆盖、测试质量策略与测试报告（与 `docs/ci/` 当前 8 个文件同步） | `docs/ci/` |
| `ci/use-case-traceability.md` | CI 用例追溯检查报告：UC01–UC19 全部 covered（19/19）、自动化证据分 38/38；由追溯检查生成，留作追溯证据 | 追溯成果以 `02_docs/需求设计代码测试追溯表.md`（含 PDF）为准 |
| `playwright-report-real/` | 真实后端 E2E 的 Playwright HTML 报告（仅保留 `index.html` 报告，不含 `test-results` 缓存） | `frontend/playwright-report-real/` |
| `测试执行报告-2026-08-27 ~ 09-02.md` | 每日测试执行报告（5 篇）；历史记录集中保留在本目录，最终 09-02 报告及 PDF 同时位于 `02_docs/` | 本目录与 `02_docs/测试执行报告-2026-09-02.*` |
| `e2e/playwright-e2e-本机-2026-09-01.html` | 本机 Playwright E2E 原始 HTML 报告 | 本目录 |
| `pipeline/33582799133/backend-test-reports/` | 后端 Surefire、Failsafe、JaCoCo 与 SpotBugs 原始报告，共 320 个文件 | GitHub Actions run 33582799133：`backend-test-reports` |
| `pipeline/33582799133/real-backend-e2e-evidence/` | 真实后端 Playwright E2E 报告与测试结果，共 2 个文件 | GitHub Actions run 33582799133：`real-backend-e2e-evidence` |
| `pipeline/33582799133/microservice-e2e-evidence-4850f4805db158522bdd5a952b0be4efb827cf66/` | 微服务 E2E 原始证据，共 7 个文件 | GitHub Actions run 33582799133：同名 Artifact |
| `pipeline/33582799133/use-case-traceability-report/` | UC01—UC19 用例追溯原始报告 | GitHub Actions run 33582799133：`use-case-traceability-report` |

以上 4 个 Artifact 已从 [GitHub Actions run 33582799133](https://github.com/Miracle060811/Software_engineering/actions/runs/33582799133) 下载并解压，共 330 个原始文件；归档目录保留 Artifact 原名，便于与远程流水线逐项核对。

压力测试原始结果见 `../stress/results/`，故障/迁移实验结果见 `../fault/results/`、`../migration/`；自动化测试源码快照见 `../automation/`。
