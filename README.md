# TravelMate (伴游) 出行旅游平台

## 快速启动

### 前置条件

- JDK 17+
- MySQL 8.0
- Redis 6.x+
- Node.js 18+

### 数据库初始化

```bash
mysql -u root -p < docs/sql/init.sql
```

### 后端启动

```bash
cd backend

# 设置环境变量 (Windows CMD)
set DB_PASSWORD=你的MySQL密码
set DEEPSEEK_API_KEY=你的DeepSeek密钥  # 可选，不设置则AI功能降级为模板

# 运行主启动类
mvn spring-boot:run
```

**重要**: 使用 `com.travelmate.TravelMateApplication` 作为启动类（非 `BackendApplication`）。

若不想设环境变量，可在 `backend/src/main/resources/` 新建 `application-local.yml`:

```yaml
spring:
  datasource:
    password: 你的MySQL密码
ai:
  deepseek:
    api-key: sk-你的key
```

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:3000

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
- 酒店预订（Redis 预减 + 数据库乐观锁防超卖）
- 景点搜索与门票购买
- 评价系统

### AI 智能规划与 Agent 服务（成员C - 陈一鸿）

- AI 行程规划（调用 DeepSeek API，强制 JSON 结构化输出）
- API 超时/失败时自动降级为模板方案
- AI 客服多轮对话（对话历史持久化）
- 站内通知系统

### 旅途社区与用户中心（成员D - 杜新诚）

- 用户注册/登录（BCrypt + JWT 认证）
- 游记发布（图文、目的地打卡、标签）
- 双列瀑布流社区浏览
- 点赞、收藏、评论（二级评论树）
- 关注/粉丝社交关系

### 管理后台与可观测性（成员E - 李科）

- RBAC 权限控制（role=1 管理员专属）
- 游记内容审核（通过/下架）
- 用户管理（启用/禁用）
- 资源管理（航班、酒店 CRUD）
- 系统数据统计（总用户/订单/待审核/今日新增）
- 敏感词管理

---

## 技术架构

| 层次   | 技术                                              |
| ------ | ------------------------------------------------- |
| 前端   | Vue 3 + Vite + Element Plus + Pinia + Vue Router  |
| 后端   | Java 17 + Spring Boot 3.x + MyBatis-Plus + Lombok |
| 数据库 | MySQL 8.0 + Redis                                 |
| 认证   | JWT (jjwt 0.11.5) + Spring Security               |
| AI     | DeepSeek API（OpenAI 兼容协议）                   |

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
