# TravelMate (伴游) 出行旅游平台

## 快速启动

### 前置条件

- JDK 21
- MySQL 8.0
- Redis 6.x+
- Node.js 18+

### 数据库初始化

不要用 PowerShell 的 `Get-Content | mysql` 管道导入，中文种子数据会被写成 `?`。请直接让 `mysql` 客户端按 `utf8mb4` 读取脚本：

```bat
mysql --default-character-set=utf8mb4 -u root -p < docs\sql\init.sql
```

如果你当前就在 PowerShell 里，直接执行下面这条命令即可：

```powershell
cmd /c "mysql --default-character-set=utf8mb4 -u root -p < docs\sql\init.sql"
```

如果 PowerShell 提示找不到 `mysql` 命令，可以改用 MySQL 安装目录下的可执行文件：

```powershell
cmd /c '"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 -u root -p < docs\sql\init.sql'
```

如果页面里已经出现 `????`，说明这些中文已经在导入阶段被写坏，单纯改前后端配置无法恢复，需要删掉 `travelmate` 数据库后按上面的命令重新导入。

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
```

脚本会先启动后端，并在检测到 `http://127.0.0.1:8080/api/post/list` 可访问后再启动前端，避免开发模式下前端一打开就因为后端尚未完成启动而出现 Vite `http proxy error` / `ECONNREFUSED`。

也可以双击或命令行运行：

```bat
start.bat
```

常用参数：

```powershell
.\start.ps1 -DbPassword 你的MySQL密码
.\start.ps1 -BackendOnly
.\start.ps1 -FrontendOnly
```

### 后端启动

```powershell
cd backend

# PowerShell
$env:DEEPSEEK_API_KEY="你的DeepSeek密钥"  # 可选，不设置则AI功能降级为模板
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.password=你的MySQL密码"
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

---

## 当前实现状态

当前仓库已经完成基础业务主链路，但整体仍处于“可运行 + 持续联调完善”阶段，不应再按早期文档理解为“酒店/AI/社区/后台尚未开始”。

- 后端已具备：用户认证、航班/火车、交通订单、酒店/景点、AI 行程、AI 聊天、社区、管理后台等基础接口。
- 前端已具备：[frontend/src/views/Home.vue](frontend/src/views/Home.vue)、[frontend/src/views/Login.vue](frontend/src/views/Login.vue)、[frontend/src/views/flight/FlightSearch.vue](frontend/src/views/flight/FlightSearch.vue)、[frontend/src/views/train/TrainSearch.vue](frontend/src/views/train/TrainSearch.vue)、[frontend/src/views/hotel/HotelSearch.vue](frontend/src/views/hotel/HotelSearch.vue)、[frontend/src/views/hotel/HotelDetail.vue](frontend/src/views/hotel/HotelDetail.vue)、[frontend/src/views/hotel/AttractionList.vue](frontend/src/views/hotel/AttractionList.vue)、[frontend/src/views/ai/AiPlan.vue](frontend/src/views/ai/AiPlan.vue)、[frontend/src/views/community/Community.vue](frontend/src/views/community/Community.vue)、[frontend/src/views/community/PostCreate.vue](frontend/src/views/community/PostCreate.vue)、[frontend/src/views/community/PostDetail.vue](frontend/src/views/community/PostDetail.vue)、[frontend/src/views/order/MyOrders.vue](frontend/src/views/order/MyOrders.vue)、[frontend/src/views/user/UserProfile.vue](frontend/src/views/user/UserProfile.vue)、[frontend/src/views/admin/AdminDashboard.vue](frontend/src/views/admin/AdminDashboard.vue) 等基础页面。
- Windows 根目录已提供一键启动脚本：[start.ps1](start.ps1) 和 [start.bat](start.bat)。

## 当前待完善项

- 仍缺独立的通知中心、订单详情页、后台资源/内容独立页面。
- Redis 目前主要用于限流，订单侧“Redis 预减库存”尚未完全按设计文档落地。
- AI 聊天已可用，但“Skills/工具调用真实站内数据”尚未完成。
- 社区审核流、图片上传、位置打卡等能力仍待完善。
- 当前存在若干联调问题，例如后台统计字段不一致、酒店订单字段与状态映射不一致。

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

### 大交通票务（成员A - 邹林利）

- 航班搜索（出发城市、到达城市、日期）
- 火车票搜索（出发站、到达站、日期）
- 订单管理（下单、模拟支付、取消，乐观锁防超卖）
- 常用旅客管理
- 历史价格趋势

### 目的地住宿与本地生活（成员B - 莫谨瑞）

- 酒店多条件搜索（城市、星级、价格区间）
- 酒店详情与房型展示
- 酒店预订（数据库原子扣减已实现，Redis 预减待补齐）
- 景点搜索与门票购买
- 评价系统

### AI 智能规划与 Agent 服务（成员C - 陈一鸿）

- AI 行程规划（调用 DeepSeek API，强制 JSON 结构化输出）
- API 超时/失败时自动降级为模板方案
- AI 客服多轮对话（基础版，对话历史持久化）
- 站内通知查询 / 已读 / 未读数接口

### 旅途社区与用户中心（成员D - 杜新诚）

- 用户注册/登录（BCrypt + JWT 认证）
- 游记发布（标题、正文、标签等基础能力）
- 双列瀑布流社区浏览
- 点赞、收藏、评论（二级评论树）
- 关注/粉丝社交关系

### 管理后台与可观测性（成员E - 李科）

- RBAC 权限控制（role=1 管理员专属）
- 游记内容审核（接口已具备，待与发帖审核流完全闭环）
- 用户管理（启用/禁用）
- 资源管理（航班、酒店 CRUD）
- 系统数据统计（基础版：总用户/订单/待审核/今日新增）
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
│       │   ├── flight/FlightSearch.vue ← 航班搜索
│       │   ├── train/TrainSearch.vue   ← 火车票搜索
│       │   ├── hotel/                  ← 酒店 + 景点
│       │   ├── ai/AiPlan.vue          ← AI行程规划
│       │   ├── community/             ← 旅行社区
│       │   ├── order/MyOrders.vue     ← 订单管理
│       │   ├── user/UserProfile.vue   ← 个人主页
│       │   └── admin/AdminDashboard.vue ← 管理后台
│       ├── stores/user.js             ← Pinia 用户状态
│       ├── router/index.js            ← 路由配置
│       └── utils/request.js           ← Axios 封装（含 JWT 自动注入）
└── docs/
    └── sql/init.sql                   ← 数据库初始化（含 Mock 数据）
```

---

> 本项目为软件工程基础2026春课程大作业，Miracle 开发小组。
