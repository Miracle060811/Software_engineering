# TravelMate 测试补全阶段 1 基线

## 1. 基线信息

| 项目 | 结果 |
| --- | --- |
| 记录时间 | 2026-08-27 14:44:47 +08:00 |
| 工作分支 | `codex/test-completion` |
| 基线提交 | `28d641b4dbe41e5e1eaa74782ccc949cfdba8f25` |
| 基线内容 | 已包含 UC14 后端测试和真实 E2E 测试代码 |
| 阶段范围 | 仅保护工作区、建立分支、验证并记录测试基线；未补新测试 |

## 2. 已保护的既有工作区修改

以下内容在创建分支前已经存在，本阶段未暂存、未覆盖、未删除：

- `document/详细设计说明/01_系统总体架构图.png`
- `document/详细设计说明/02_五大子系统协同关系图.png`
- `document/详细设计说明/11_项目测试计划闭环流程图.png`
- `document/详细设计说明/12_TravelMate完整系统用例图.png`
- `document/需求设计代码测试追溯表.md`
- `docs/杨任宇班-5组-TravelMate.md`

## 3. 自动化测试基线

| 检查项 | 命令 | 实际结果 |
| --- | --- | --- |
| 后端 JUnit/MockMvc | `backend/mvnw.cmd --batch-mode --no-transfer-progress test` | 77/77 通过，失败 0，错误 0，跳过 0 |
| Playwright Mock E2E 测试发现 | `npx playwright test --list` | 6 条测试，解析成功 |
| Playwright 真实后端 E2E 测试发现 | `npx playwright test --config=playwright.real.config.js --list` | 10 条测试，解析成功；本阶段未启动真实 MySQL/Redis 执行 |
| 用例追溯门禁 | `npm run check:traceability` | 6 个 `covered`、13 个 `partial`、0 个 `planned`；证据分 25，门槛 19；结构错误 0 |
| 格式检查 | `git diff --check` | 无格式错误；仅有 Windows LF/CRLF 转换提示 |

## 4. 已知基线缺口

- UC01—UC08、UC10—UC12、UC18、UC19 仍为 `partial`。
- UC14 的真实后端 E2E 已存在，但还需要在阶段 2 的 MySQL/Redis CI 中获得实际通过证据。
- 当前测试发现只证明 E2E 脚本可以被 Playwright 解析，不能替代真实运行结果。
- 当前本地 `main` 的 UC14 提交尚未进入 `origin/main`，后续只能从 `codex/test-completion` 推送并通过 PR/CI 合入。

## 5. 阶段 1 退出条件

- [x] 当前未提交文件得到保留。
- [x] 已建立独立测试补全分支。
- [x] 后端基线测试全部通过。
- [x] 两套 Playwright 测试数量和语法得到确认。
- [x] 追溯门禁和格式检查通过。
- [x] 基线提交、时间、命令、结果和缺口已记录。

阶段 2 从 UC14 的真实 MySQL/Redis E2E CI 验证开始。
