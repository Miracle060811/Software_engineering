# TravelMate 测试运行手册

## 1. 环境准备

### 1.1 必需软件

| 软件 | 最低版本 | 用途 |
| --- | --- | --- |
| Java JDK | 21 (Temurin) | 后端编译与测试 |
| Maven | 3.9+ | 后端构建与测试执行 |
| Node.js | 24 | 前端构建与测试 |
| npm | 11 | 前端依赖管理 |
| MySQL | 8.0 | 集成测试与真实 E2E |
| Redis | 7 | 集成测试与真实 E2E |

### 1.2 初始化

```powershell
# 克隆仓库
git clone https://github.com/Miracle060811/Software_engineering.git
cd Software_engineering

# 安装前端依赖
cd frontend
npm ci
cd ..

# 初始化数据库（需要 MySQL 运行中）
mysql -uroot -p < docs/sql/init.sql
```

## 2. 快速运行

### 2.1 一键全量回归

```powershell
.\scripts\run-tests.ps1 -SkipInstall
```

此脚本按顺序执行：后端测试 → 前端 lint → 前端构建 → Mock E2E → 追溯门禁。任一步失败立即停止。

### 2.2 分步运行

```powershell
# 后端单元测试（H2 内存数据库，无需 MySQL）
cd backend
.\mvnw.cmd --batch-mode test

# 后端 MySQL 集成测试（需要 MySQL 运行）
.\mvnw.cmd --batch-mode failsafe:integration-test failsafe:verify -Dspring.profiles.active=mysql-test

# 前端单元测试
cd frontend
npx vitest run

# 前端 lint
npm run lint

# 前端构建
npm run build

# Mock E2E（无需后端运行）
npx playwright test --workers=1

# 真实后端 E2E（需要后端运行在 localhost:8080）
npx playwright test --config=playwright.real.config.js --workers=1
```

## 3. 测试层级说明

| 层级 | 工具 | 数据库 | 启动方式 | 耗时 |
| --- | --- | --- | --- | --- |
| 后端单元测试 | JUnit 5 + Mockito | H2 内存 | `mvn test` | ~30s |
| 后端集成测试 | JUnit 5 + MySQL | MySQL 8.0 | `mvn verify -Dspring.profiles.active=mysql-test` | ~60s |
| 前端单元测试 | Vitest + jsdom | 无 | `npx vitest run` | ~8s |
| Mock E2E | Playwright | 无 | `npx playwright test` | ~30s |
| 真实 E2E | Playwright | MySQL + Redis | `npx playwright test --config=playwright.real.config.js` | ~120s |

## 4. 预期结果

### 4.1 后端测试

- 总数：161
- 通过：161
- 失败：0
- 跳过：0
- 构建：BUILD SUCCESS

### 4.2 前端单元测试

- 测试文件：3 个（csrf、request、router）
- 测试用例：24 个
- 全部通过

### 4.3 Mock E2E

- 测试用例：6 个
- 全部通过

### 4.4 真实 E2E

- 测试用例：16 个
- PR CI 中执行（需要 MySQL + Redis + 后端运行）

### 4.5 追溯门禁

- covered：19 个
- partial：0 个
- planned：0 个
- 证据分：38（门槛 38）
- 结构错误：0

## 5. 失败排查

### 5.1 后端测试失败

```powershell
# 查看失败详情
cd backend
.\mvnw.cmd --batch-mode test 2>&1 | Select-String "FAIL|ERROR|Failed"

# 单独运行失败的测试类
.\mvnw.cmd --batch-mode -Dtest=测试类名 test
```

常见原因：
- H2 数据库文件损坏：删除 `backend/data/` 目录重新运行
- 端口占用：检查 8080 端口是否被占用

### 5.2 前端测试失败

```powershell
# 查看失败详情
cd frontend
npx vitest run --reporter=verbose
```

常见原因：
- 依赖未安装：运行 `npm ci`
- Node 版本不匹配：需要 Node.js 24+

### 5.3 E2E 测试失败

```powershell
# 查看 Playwright 报告
cd frontend
npx playwright show-report

# 调试模式运行单个测试
npx playwright test --debug --workers=1
```

常见原因：
- 后端未启动：确保 `java -jar backend/target/*.jar` 在 localhost:8080 运行
- MySQL/Redis 未运行：检查 Docker 或本地服务
- 测试数据残留：重新执行 `docs/sql/init.sql`

## 6. CI 运行

### 6.1 触发条件

- 推送到任意分支
- 创建/更新 PR（目标 main）
- 手动触发（workflow_dispatch）

### 6.2 CI 流程

1. `validation` — 仓库结构、微服务 E2E 结构与部署配置检查
2. `backend` — 后端单元测试 + MySQL 集成测试 + 打包
3. `frontend` — lint + audit + build + 单元测试 + Mock E2E
4. `real-e2e` — 真实 MySQL/Redis E2E（仅 PR/main 触发）
5. `ci-gate` — 汇总所有阶段，全部通过才能通过

### 6.3 镜像发布

CI 全部通过 + main 分支 push → 自动触发 `publish-images.yml` → 推送到 GHCR

## 7. 测试数据

### 7.1 测试用户

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| admin | 123456 | 管理员（首次登录后请修改默认密码） |
| testuser | 123456 | 普通用户 |
| testuser2 | 123456 | 普通用户 |

### 7.2 测试数据库

- 单元测试：H2 内存数据库，每次运行自动重建
- 集成测试：MySQL `travelmate` 数据库，由 `docs/sql/init.sql` 初始化
- E2E 测试：使用 MySQL，测试数据由各测试用例自行创建和清理
