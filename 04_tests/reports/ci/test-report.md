# TravelMate 自动化测试补全报告

## 范围
补齐 UC09、UC13、UC15、UC16、UC17 五个业务场景的自动化测试证据，并修复相关缺陷。

## 变更摘要
- 新增后端单元/集成测试：
  - `UseCase09ReviewWorkflowTests.java`
  - `UseCase13MessagingWorkflowTests.java`
  - `UseCase15CommunityInteractionTests.java`
  - `UseCase16PassengerWorkflowTests.java`
  - `UseCase17FollowWorkflowTests.java`
- 修复 `SecurityConfig.java`：将 `/api/user/profile/**` 与 `/api/comment/list` 开放为公开 GET 接口，保证未登录用户可访问。
- 修复 `CommentServiceImpl.refreshPostCommentCount`：仅更新 `id` 与 `commentCount`，避免覆盖帖子其他字段。
- 修复真实后端 E2E 测试中的硬编码密码：使用 `randomUUID` 生成随机密码，消除 Gitleaks 告警。

## CI 结果
PR #205 全部检查通过：
- Secret scan：通过
- Maven / Node.js dependency audit：通过
- Backend integration tests and package：通过
- Frontend build and mocked smoke E2E：通过
- Real backend E2E：通过
- CodeQL quality gate：通过
- CI quality gate：通过

## 追溯矩阵更新
`use-case-test-matrix.json` 中 UC09、UC13、UC15、UC16、UC17 状态由 `partial` 升级为 `covered`。
当前基线：5 个 covered、14 个 partial、0 个 planned， evidence score = 5×2 + 14×1 = 24 ≥ 19（质量门禁）。

## 后续行动
- 合并 PR #205 到 `main`。
- 删除临时分支 `codex/uc09-17-automation` 与 `codex/uc09-17-automation-v2`。
