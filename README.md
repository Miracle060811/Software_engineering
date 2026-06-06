# TravelMate (伴游) 出行旅游平台

## 快速启动

### 前置条件

- JDK 21
- MySQL 8.0
- Redis 6.x+
- Node.js 18+

### 数据库初始化

不要用 PowerShell 的 `Get-Content | mysql` 管道导入，中文种子数据会被写成 `?`。请直接让 `mysql` 客户端按 `utf8mb4` 读取脚本：

Windows 下优先推荐直接使用仓库脚本，脚本会自动加载 `.env`、查找 `mysql.exe` 并用 `SOURCE` 导入：

```powershell
.\setup.ps1 -InitDb
```

`setup.ps1` 现在会用 PowerShell 参数数组调用 `mysql.exe`，用于数据库重建和 `SOURCE docs/sql/init.sql` 导入，避免命令参数中的空格、引号或分号被 PowerShell 错误拆分。

```bat
mysql --default-character-set=utf8mb4 -u root -p < docs\sql\init.sql
```

如果你当前就在 PowerShell 里，直接执行下面这条命令即可：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -u root -p < docs\sql\init.sql"
```

如果你不想使用脚本，或者需要手工排查导入问题，PowerShell 提示找不到 `mysql` 命令时可以改用 MySQL 安装目录下的可执行文件：

```powershell
cmd /c '"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 -u root -p < docs\sql\init.sql'
```

如果页面里已经出现 `????`，说明这些中文已经在导入阶段被写坏，单纯改前后端配置无法恢复，需要删掉 `travelmate` 数据库后按上面的命令重新导入。

如果中文已经被导成 `?`，推荐直接使用脚本重建（已在当前仓库实测恢复中文）：

```powershell
.\setup.ps1 -InitDb -ResetDb
```

### 数据库重建（中文已经变成 ? 时）

先关闭后端服务，然后在**项目根目录**执行下面这组命令。这个流程已经在当前仓库下实际验证过，可以恢复 `tm_user.nickname` 和 `tm_post.title` 里的中文。

如果你使用的是 CMD：

```bat
cd /d E:\SoftEngneeringHomework\Software_engineering
mysql --default-character-set=utf8mb4 -u root -p -e "DROP DATABASE IF EXISTS travelmate; CREATE DATABASE travelmate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql --default-character-set=utf8mb4 -u root -p travelmate -e "SOURCE docs/sql/init.sql;"
mysql --default-character-set=utf8mb4 -u root -p travelmate -e "SELECT id, nickname FROM tm_user WHERE id IN (1,2,3,4); SELECT id, title FROM tm_post LIMIT 3;"
```

如果你使用的是 PowerShell，推荐仍然调用 `mysql` 客户端本身，不要再用 `Get-Content | mysql`：

```powershell
Set-Location -LiteralPath "E:\SoftEngneeringHomework\Software_engineering"
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 -u root -p -e "DROP DATABASE IF EXISTS travelmate; CREATE DATABASE travelmate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 -u root -p travelmate -e "SOURCE E:/SoftEngneeringHomework/Software_engineering/docs/sql/init.sql;"
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 -u root -p travelmate -e "SELECT id, nickname FROM tm_user WHERE id IN (1,2,3,4); SELECT id, title FROM tm_post LIMIT 3;"
```

最后一条校验命令如果能看到“超级管理员”“测试用户”“北京三日游｜故宫+长城+颐和园完美攻略”等正常中文，就说明导入成功。

初始化完成后，建议执行一次校验：

```powershell
mysql -u root -p -e "USE travelmate; SHOW TABLES LIKE 'tm_user'; SHOW TABLES LIKE 'tm_notification'; SELECT COUNT(*) AS user_count FROM tm_user;"
```

### 一键启动（Windows）

在项目根目录可直接使用：

```powershell
.\start.ps1
.\start-backend.ps1
.\start-frontend.ps1
```

脚本会先启动后端，并在检测到 `http://127.0.0.1:8080/api/post/list` 可访问后再启动前端，避免开发模式下前端一打开就因为后端尚未完成启动而出现 Vite `http proxy error` / `ECONNREFUSED`。

如果你只想分别启动单个服务，也可以直接使用两个独立入口：`start-backend.ps1` 只启动后端，`start-frontend.ps1` 只启动前端。它们都会复用现有 `start.ps1` 的环境变量加载、参数校验和启动逻辑。

也可以双击或命令行运行：

```bat
start.bat
start.bat -DbPassword 你的MySQL密码
start.bat -BackendOnly
start.bat -FrontendOnly
start.bat -SkipRedis
start.bat -SkipFrontendInstall
start.bat -DryRun
```

常用参数：

```powershell
.\start.ps1 -DbPassword 你的MySQL密码
.\start.ps1 -DeepseekApiKey 你的DeepSeek密钥
.\start.ps1 -BackendOnly
.\start.ps1 -FrontendOnly
.\start.ps1 -SkipRedis
.\start.ps1 -SkipFrontendInstall
.\start.ps1 -DryRun

.\start-backend.ps1 -DbPassword 你的MySQL密码
.\start-backend.ps1 -DeepseekApiKey 你的DeepSeek密钥
.\start-backend.ps1 -SkipRedis

.\start-frontend.ps1 -SkipFrontendInstall
.\start-frontend.ps1 -DryRun
```

参数说明：

- `-DbPassword`：仅后端启动相关。显式指定 MySQL 密码，优先于 `.env`、`SPRING_DATASOURCE_PASSWORD`、`DB_PASSWORD` 和 `backend/application-local.yml` 中的配置。
- `-DeepseekApiKey`：仅后端启动相关。显式指定 DeepSeek API Key；未提供时会继续尝试读取当前环境变量或根目录 `.env`。
- `-BackendOnly`：只启动后端服务，不启动前端。通常由 `start-backend.ps1` 内部使用。
- `-FrontendOnly`：只启动前端服务，不启动后端。通常由 `start-frontend.ps1` 内部使用。
- `-SkipRedis`：跳过 Redis 自动检测和自动启动。适合本机未装 Redis、或你只想先验证基础后端是否能起来的场景。
- `-SkipFrontendInstall`：启动前端前不自动执行 `npm install`。当 `frontend/node_modules` 已经存在时建议使用；如果依赖还没装，前端可能启动失败。
- `-DryRun`：只打印即将执行的命令，不真正启动服务。适合检查脚本会如何解析参数和调用命令。

独立脚本对应关系：

- `start-backend.ps1` 会固定附带 `-BackendOnly`，并额外支持 `-DbPassword`、`-DeepseekApiKey`、`-SkipRedis`、`-DryRun`。
- `start-frontend.ps1` 会固定附带 `-FrontendOnly`，并额外支持 `-SkipFrontendInstall`、`-DryRun`。

`start.bat` 是 `start.ps1` 的 CMD/双击入口，会自动寻找 PowerShell 7 或 Windows PowerShell，并把命令行参数原样传给 `start.ps1`。需要查看参数时可运行 `start.bat /?`。

`start.ps1` 会先检查 `127.0.0.1:6379`。如果 Redis 已运行则直接复用；如果检测到 Windows Redis 服务或 `redis-server.exe`，会尝试自动启动。若本机未安装 Redis，会给出警告，后端仍会启动，但 Redis 限流/酒店房态缓存会降级。

最近一次脚本调整没有改变 `start.ps1` 的启动流程；前后端、Redis 检查、参数透传和等待后端可访问后再启动前端的行为保持不变。

### 前端视觉说明

当前前端采用浅色自然旅行风格：以米绿色/浅沙色背景、克制蓝绿色强调色、低圆角卡片和大留白为主。首页重点展示分屏 Hero、快速搜索、路线预览、三段式行程准备、热门城市和功能入口；AI 行程规划页已调整为工作台式布局，包含推荐起点、行程生成表单、空状态路线预览、历史行程和生成结果时间线。

### 社区模块说明

社区游记发布默认进入 `AI审核中` 状态，审核通过后才会出现在推荐流和用户公开主页。用户可以在社区页 `我的` 标签和自己的个人主页中查看全部笔记，按 `草稿 / AI审核中 / 已发布 / 已拒绝` 筛选；草稿可继续编辑，自己的笔记可删除。AI 或人工审核未通过时，会在已拒绝笔记上展示拒绝原因，并同步发送站内通知。个人主页、关注/粉丝、帖子详情点赞、评论与回复会依赖最新后端接口；修改后如果仍看到 `undefined`、`post` 或 `No static resource api/user/profile/.../posts`，请重启后端和前端开发服务。

已覆盖的社区交互：

- 游记列表支持关键词搜索，推荐流按热度和时间衰减排序。
- 游记发布后进入 AI 审核中，后端可通过 AI 审核任务自动判断通过/拒绝；失败时保留人工审核兜底。
- 关注流需要登录；关注/取消关注后会同步刷新关注状态和粉丝数。
- 社区页增加 `我的` 标签，展示草稿、AI 审核中、已发布、已拒绝内容；标签可点击进入同标签筛选页。
- 个人主页支持查看关注/粉丝列表，并可跳转到用户主页。
- 无配图游记使用纯文字卡片，不再随机渲染占位图片。
- 帖子详情点赞/收藏使用数字 `targetType`，避免字符串类型导致的后端解析错误。
- 我的收藏页集中展示已收藏游记，可从用户菜单进入。
- 评论、回复、删除评论后会刷新评论树和评论数量；无头像用户显示首字母默认头像。

### 后端启动

```powershell
cd backend

# PowerShell
$env:DEEPSEEK_API_KEY="你的DeepSeek密钥"  # 可选，不设置则AI功能降级为模板
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password=你的MySQL密码"
```

后端启动时也会自动读取项目根目录 `.env`（兼容 `DB_PASSWORD="..."`），所以根目录已有 `.env` 时可以直接运行：

```powershell
cd backend
.\mvnw.cmd clean spring-boot:run
```

如果你用的是 CMD：

```bat
cd backend
set DEEPSEEK_API_KEY=你的DeepSeek密钥
mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password=你的MySQL密码"
```

**重要**: 使用 `com.travelmate.TravelMateApplication` 作为启动类（非 `BackendApplication`）。

若不想设环境变量，可直接编辑项目根下的 `backend/application-local.yml`：

```yaml
spring:
  datasource:
    password: "你的MySQL密码"
```

如果密码是纯数字，或者带前导 `0`，必须加引号；否则 YAML 可能把它当成数字解析，导致实际传给 MySQL 的密码被改写。

如果使用 `start.ps1` 启动，脚本会自动读取 `backend/application-local.yml`，并通过继承的 `SPRING_DATASOURCE_PASSWORD` 环境变量把密码传给后端。

如果启动后前端出现 Vite 的 `http proxy error` 或 `ECONNREFUSED`，先看后端窗口是否已经退出。当前后端会在启动期校验数据库连接；若 MySQL 密码不匹配，会直接输出“数据库连接失败，请检查 backend/application-local.yml 或 DB_PASSWORD 配置”。

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:3000

### 自动化测试

当前仓库提供统一测试入口，用于执行后端 JUnit/MockMvc、前端依赖检查、生产构建、Playwright E2E smoke 测试和启动脚本 DryRun：

```powershell
.\scripts\run-tests.ps1
```

也可以按需单独运行：

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm install
npm run build
npx playwright test --reporter=list --workers=1

cd ..
.\start.ps1 -DryRun
```

测试说明见 [docs/test-runbook.md](docs/test-runbook.md)，正式测试结果见根目录 [测试报告.md](测试报告.md)。

---

## 当前实现状态

当前仓库已经完成基础业务主链路，但整体仍处于“可运行 + 持续联调完善”阶段，不应再按早期文档理解为“酒店/AI/社区/后台尚未开始”。

- 后端已具备：用户认证、航班/火车、交通订单、酒店/景点、AI 行程、AI 聊天、社区、管理后台等基础接口。
- 前端已具备：[frontend/src/views/Home.vue](frontend/src/views/Home.vue)、[frontend/src/views/Login.vue](frontend/src/views/Login.vue)、[frontend/src/views/destination/DestinationList.vue](frontend/src/views/destination/DestinationList.vue)、[frontend/src/views/destination/DestinationDetail.vue](frontend/src/views/destination/DestinationDetail.vue)、[frontend/src/views/info/InfoPage.vue](frontend/src/views/info/InfoPage.vue)、[frontend/src/views/flight/FlightSearch.vue](frontend/src/views/flight/FlightSearch.vue)、[frontend/src/views/train/TrainSearch.vue](frontend/src/views/train/TrainSearch.vue)、[frontend/src/views/hotel/HotelSearch.vue](frontend/src/views/hotel/HotelSearch.vue)、[frontend/src/views/hotel/HotelDetail.vue](frontend/src/views/hotel/HotelDetail.vue)、[frontend/src/views/hotel/AttractionList.vue](frontend/src/views/hotel/AttractionList.vue)、[frontend/src/views/ai/AiPlan.vue](frontend/src/views/ai/AiPlan.vue)、[frontend/src/views/community/Community.vue](frontend/src/views/community/Community.vue)、[frontend/src/views/community/PostCreate.vue](frontend/src/views/community/PostCreate.vue)、[frontend/src/views/community/PostDetail.vue](frontend/src/views/community/PostDetail.vue)、[frontend/src/views/order/CouponCenter.vue](frontend/src/views/order/CouponCenter.vue)、[frontend/src/views/order/MyOrders.vue](frontend/src/views/order/MyOrders.vue)、[frontend/src/views/user/NotificationCenter.vue](frontend/src/views/user/NotificationCenter.vue)、[frontend/src/views/user/MyCollections.vue](frontend/src/views/user/MyCollections.vue)、[frontend/src/views/user/UserProfile.vue](frontend/src/views/user/UserProfile.vue)、[frontend/src/views/admin/AdminDashboard.vue](frontend/src/views/admin/AdminDashboard.vue) 等基础页面。
- Windows 根目录已提供一键启动脚本：[start.ps1](start.ps1) 和 [start.bat](start.bat)。
- 数据库种子已改用本地静态图片路径，热门城市资料、一日游/周边游、优惠券、订单、日志、评价等演示数据不再依赖随机占位图。
- 订单链路已支持机票/火车票多张购买、酒店多间房预订、景点门票购买、详情查看、用户退款申请、后台退款审批，库存预扣减和取消/退款会按实际数量回补。
- 首页会展示本地最近浏览记录，当前覆盖酒店详情、景点购票入口和游记详情。

## 图片资源策略

酒店和景点封面优先使用真实图片 URL 或真实本地图片路径（如 `/uploads/...`、`/images/real/...`）。课程演示用的 `/images/seed/...` SVG 仍允许作为稳定兜底素材；业务图片为空或加载失败时，前端会统一回退到 `/images/seed/fallback.svg`。

新增或修改本地图片后建议执行：

```powershell
npm run check:images
```

该命令会检查 `docs/sql/init.sql` 和主要前端入口，避免重新引入随机占位图，并确认本地图片文件存在。

## 当前待完善项

- Redis 限流已覆盖大部分关键写接口，仍有少量非核心接口待补齐。
- 管理后台中的 QPS、延迟和告警已基于本地 `sys_log` 做轻量统计，尚未接入真实 APM / 链路追踪系统。
- AI 行程与订单的更深度联动、同行人共享行程等扩展能力待后续补充。
- 若后续需要更真实的照片效果，可把同名 seed SVG 替换为自建 CDN 或对象存储中的稳定图片，并保持数据库路径由项目方控制。
- 更深层的并发压测与端到端自动化回归仍需继续完善。
- 已初始化过的本地数据库若缺少 `tm_attraction_order`，需要重新执行 `docs/sql/init.sql` 或手工补建该表后再使用景点购票订单功能。

---

## 默认账号说明

> SQL 中预置账号密码哈希对应 "admin123"。若无法登录，请手动注册：

```bash
# 注册管理员
curl -X POST "http://localhost:8080/user/register?username=admin&password=admin123&role=1"

# 注册普通用户
curl -X POST "http://localhost:8080/user/register?username=test&password=test123&role=0"
```

---

## 系统功能

### 大交通票务（成员 A - 邹林利）

- 航班搜索（出发城市、到达城市、日期，多维度筛选）
- 火车票搜索（出发站、到达站、日期、中转方案推荐）
- 基础订单管理（多张票下单、模拟支付、取消、订单详情、用户退票申请、后台退款处理、状态机流转）
- 库存管控（机票、酒店等资源的 Redis 预扣减 + MySQL 原子更新防超卖）
- 常用旅客管理
- 历史价格趋势（ECharts 折线图，近 7 天模拟数据）
- 退改签规则展示（各舱位退改费用说明）
- 行程单下载（文本格式订单回执）和订单详情弹窗

### 目的地住宿与本地生活（成员 B - 莫谨瑞）

- 酒店多条件搜索（城市、星级、价格区间）
- 酒店详情与房型展示（提供房型与基础库存数据）
- 景点搜索与详情展示
- 景点门票购买入口、真实订单落库、我的订单景点门票标签页与凭证展示
- 一日游 / 周边游产品推荐
- 基础评价系统（星级评分、图片上传、标签选择、评价列表）
- 酒店订单扫码核销、订单详情、用户退款申请与后台审批
- 热门城市资料页（真实目的地介绍、代表景点、出行建议），优先读取后端城市资料表，后端为空时回退到前端静态资料

### AI 智能规划与 Agent 服务（成员 C - 陈一鸿）

- AI 行程规划（调用 DeepSeek API，强制 JSON 结构化输出）
- 行程生成支持旅行节奏、必去地点、避开项、交通偏好和住宿偏好，并输出交通住宿建议、行前清单、风险提醒、每日备选方案
- API Key 缺失、超时或失败时自动降级为本地模板方案，避免前端长时间等待失败请求
- AI 客服多轮对话（Function Calling Tools: 天气/航班/酒店查询），支持空消息/超长消息校验和本地兜底答复
- 站内通知查询 / 已读 / 删除 / 一键删除 / 未读数接口，支持点击通知跳转业务页面并自动清除未读红点
- 航班延误预警模拟（定时任务随机推送通知）
- 行程导出（文本格式，含每日路线和费用明细）

### 旅途社区与用户中心（成员 D - 杜新诚）

- 用户注册/登录（BCrypt + JWT 认证）
- 游记发布（图片上传、标签、可见范围、草稿箱）
- 游记审核状态流转（审核中、已发布、已拒绝）
- 双列瀑布流社区浏览（推荐 + 关注信息流 + 标签筛选）
- 点赞、收藏、我的收藏页、评论（二级评论树）
- 关注/粉丝社交关系
- 个人资料编辑 + 密码修改

### 管理后台与可观测性（成员 E - 李科）

- RBAC 权限控制（后端 `/api/admin/**` 要求 `ROLE_ADMIN`，前端 `requiresAdmin` 二次拦截）
- ECharts 可观测仪表盘（真实订单趋势、类型分布、热门目的地、用户增长、今日 GMV、近 15 分钟活跃用户、本地请求量、接口延迟、报错日志告警）
- 资源管理（航班 CRUD、火车 CRUD、酒店 CRUD、房型库存/价格/上下架干预、景点 CRUD、城市资料下线）
- 资源批量导入（航班、火车、酒店、房型、景点、城市资料 CSV 导入；支持 UTF-8/BOM、引号、多行字段、预检、仅新增/重复更新和模板下载，格式见 [docs/admin-csv-import.md](docs/admin-csv-import.md)）
- 优惠券配置（满减券/折扣券新增、编辑、删除，支持业务类型分类和用户领券记录查看）
- 内容与安全审核（敏感词新增/编辑/删除、游记人工审核、AI 审核建议复核、通过 / 拒绝原因记录、审核完成后可随时改判、一键封禁作者）
- 商家回复与评价举报工单处理（驳回举报、删除被举报评价、查看/新增/删除商家回复）
- 用户管理（启用/禁用、用户画像侧边查看，含订单、发帖、评论、评价、举报和最近操作统计）
- 全平台订单流水（按类型/状态筛选、分页查看、交通与酒店退款审批闭环）
- 系统操作日志（AOP 自动记录 Controller 调用）
- 敏感词管理

---

## 技术架构

| 层次   | 技术                                                          |
| ------ | ------------------------------------------------------------- |
| 前端   | Vue 3 + Vite + JavaScript + Element Plus + Pinia + Vue Router |
| 后端   | Java 21 + Spring Boot 3.5.x + MyBatis-Plus + Lombok           |
| 数据库 | MySQL 8.0 + Redis                                             |
| 认证   | JWT (jjwt 0.11.5) + Spring Security                           |
| AI     | DeepSeek API（OpenAI 兼容协议）                               |
| CSV    | Apache Commons CSV                                            |

---

## 项目结构

```
Software_engineering/
├── backend/
│   └── src/main/java/com/travelmate/
│       ├── TravelMateApplication.java   ← 主启动类（请用此类启动）
│       ├── backend/                     ← 用户认证（成员D）
│       │   └── config/                 ← JWT + Spring Security + CORS
│       ├── common/                      ← Result<T> 统一响应 + 全局异常
│       ├── controller/                  ← 所有业务接口
│       ├── entity/                      ← 实体类
│       ├── mapper/                      ← MyBatis-Plus Mapper
│       └── service/                     ← 业务逻辑
├── frontend/
│   └── src/
│       ├── views/
│       │   ├── Home.vue                ← 首页（三合一搜索）
│       │   ├── Login.vue               ← 登录/注册
│       │   ├── destination/            ← 热门城市列表 + 城市详情
│       │   ├── info/                   ← 关于/条款/隐私/帮助
│       │   ├── flight/FlightSearch.vue ← 航班搜索 + 价格趋势
│       │   ├── train/TrainSearch.vue   ← 火车票搜索 + 价格趋势
│       │   ├── hotel/                  ← 酒店搜索/详情 + 景点 + 评价
│       │   ├── ai/AiPlan.vue          ← AI行程规划 + 客服对话
│       │   ├── community/             ← 社区瀑布流 + 发帖 + 帖子详情
│       │   ├── order/                 ← MyOrders + CouponCenter
│       │   ├── user/                  ← 个人主页 + 通知中心
│       │   └── admin/AdminDashboard.vue ← ECharts 仪表盘 + 资源/审核/订单管理
│       ├── components/                ← PageHeader, SkeletonBox, EmptyState, CountUp, PriceTrend
│       ├── stores/user.js             ← Pinia 用户状态
│       ├── data/                      ← destinations 兜底资料 + infoPages 静态资料
│       ├── router/index.js            ← 路由配置（23 路由）
│       └── utils/request.js           ← Axios 封装（含 JWT 自动注入）
└── docs/
    └── sql/init.sql                   ← 数据库初始化（含 Mock 数据）
```

---

> 本项目为软件工程基础 2026 春课程大作业，Miracle 开发小组。
