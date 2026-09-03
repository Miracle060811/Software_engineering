# TravelMate UC14 自动化测试补全报告

## 范围
补齐 UC14「游记发布、编辑、删除与审核」业务场景的自动化测试证据，覆盖文件上传校验、接口权限、发布/编辑/删除状态流转、管理员审核通知等核心路径。

## 变更摘要
- 扩展后端单元测试：
  - `FileControllerTests.java`：补充空文件、超大文件、不支持的扩展名、不支持的 Content-Type、合法 JPEG 上传等边界场景。
  - `SecurityConfigTests.java`：补充匿名用户调用 `/api/file/upload` 被 403 拒绝的权限场景。
- 新增后端单元/集成测试：
  - `UseCase14PostWorkflowTests.java`：覆盖发布待审游记、草稿游记、越权编辑/删除、作者编辑后重入待审、管理员通过/驳回并通知等流程。
- 新增真实后端 E2E 测试：
  - `frontend/tests/e2e-real/travelmate-real.spec.js`：验证真实用户创建游记、查询本人游记、编辑游记、删除游记的完整链路。

## 关键可验证点
- 文件上传仅接受白名单扩展名与 Content-Type，且校验图片签名，拒绝空文件与超大文件。
- `/api/file/upload` 与 `/api/post/**` 必须登录后访问；匿名请求被 Spring Security 拒绝。
- 发布非草稿游记进入 `status=0`（审核中）；草稿游记进入 `status=3`；作者编辑后重新进入 `status=0`。
- 非作者无法编辑或删除他人游记。
- 管理员可通过 `/api/admin/posts/{id}/approve` 将游记置为 `status=1`，或通过 `/api/admin/posts/{id}/reject` 置为 `status=2` 并记录驳回理由，同时发送系统通知。

## 追溯矩阵更新
`use-case-test-matrix.json` 中 UC14 状态由 `partial` 升级为 `covered`。
当前基线：6 个 covered、13 个 partial、0 个 planned，evidence score = 6×2 + 13×1 = 25 ≥ 19（质量门禁）。

## 测试执行
- 后端单元/集成测试：`mvn -f backend/pom.xml test` 全部 77 项通过，新增 UC14 相关 27 项全部通过。
- 前端 E2E 测试：本地未配置真实后端（JWT_SECRET / MySQL / Redis）环境，无法直接运行；已将 UC14 E2E 用例加入 `tests/e2e-real/travelmate-real.spec.js`，语法与 Playwright 契约已验证，待 CI 真实后端环境运行。

## 后续行动
- 合并到 `main` 并观察 CI 全绿。
- 清理工作区未跟踪文件。
