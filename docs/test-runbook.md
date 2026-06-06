# TravelMate 自动化测试运行说明

本文档记录本仓库新增的可重复测试入口。正式测试结果以根目录 `测试报告.md` 中的执行日志为准。

## 一键运行

```powershell
.\scripts\run-tests.ps1
```

可选参数：

```powershell
.\scripts\run-tests.ps1 -SkipInstall
.\scripts\run-tests.ps1 -SkipE2E
```

## 单独运行

```powershell
cd backend
.\mvnw.cmd test
```

```powershell
cd frontend
npm install
npm run build
npx playwright test --reporter=list --workers=1
```

```powershell
.\start.ps1 -DryRun
```

## 覆盖范围

- 后端 JUnit / MockMvc：启动上下文、统一响应、安全拦截、公开只读接口、部分库存状态边界。
- 前端 Playwright：主要公开页面、登录流程、匿名路由守卫、非管理员路由守卫。
- 构建和脚本：前端生产构建、根目录启动脚本 DryRun。

## 未自动覆盖

- 高并发压测需要独立 JMeter 或压力脚本执行。
- DeepSeek 外部服务稳定性受 API Key、网络和服务状态影响，需要单独人工复核。
- 管理后台完整 CRUD 和人工审核闭环仍建议最终验收前补一轮人工端到端测试。
