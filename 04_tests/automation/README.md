# 自动化测试源码快照（交付副本）

本目录为自动化测试代码的**交付归档副本**，原件保留在工程原位以维持 Maven/Playwright 构建与 CI：

| 副本路径 | 原件位置（仓库根目录 / GitHub） | 内容 |
| --- | --- | --- |
| `backend/src/test/` | `backend/src/test/` | 后端 JUnit 单元/集成测试（UC01–UC19 用例测试、MySQL 集成测试、拦截器/调度/服务测试，共 50 个文件） |
| `frontend/tests/` | `frontend/tests/` | 前端 Playwright 测试：`e2e/`（单体冒烟）、`e2e-real/`（真实后端 E2E）、`unit/`（CSRF/请求/路由/图片安全单测） |
| `playwright.config.js` | `frontend/playwright.config.js` | 单体 Playwright 配置 |
| `playwright.microservices.config.js` | `frontend/playwright.microservices.config.js` | 微服务 Playwright 配置 |
| `playwright.real.config.js` | `frontend/playwright.real.config.js` | 真实后端 E2E 配置 |
| `midterm-checkpoint/` | `中期检查/测试脚本/` | 中期检查时点后端 JUnit + 前端 Playwright 测试脚本**历史快照**（约 08-26），含 `run-tests.ps1`、`check-deployment-config.mjs`，用于对比改造中期测试基线 |

说明：

- 本目录仅为**证据快照**，不能脱离根目录工程独立编译/运行；可运行的测试以根目录 `backend/`、`frontend/` 工程（或 GitHub 仓库）为准。
- 压力测试（k6）、故障与迁移实验脚本见 `../stress/`、`../fault/`、`../migration/`；测试报告与每日执行记录见 `../reports/`。
- `midterm-checkpoint/` 为仓库根目录 `中期检查/测试脚本/` 的交付副本（原件保留原位，不随六目录归档也能在本目录内查看中期基线）。
