# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 常用命令

### 后端

```bash
cd backend

# 启动（需要 DB_PASSWORD 和可选的 DEEPSEEK_API_KEY 环境变量）
mvn spring-boot:run

# 编译
mvn compile

# 运行测试
mvn test

# 运行单个测试
mvn test -Dtest="BackendApplicationTests"
```

### 前端

```bash
cd frontend
npm install
npm run dev        # 开发服务器，端口 3000
npm run build      # 生产构建
```

### 数据库

```bash
mysql -u root -p < docs/sql/init.sql
```

## 架构概览

### 启动类

**必须使用 `com.travelmate.TravelMateApplication`** 作为主类，而非 `backend.BackendApplication`。前者 `@SpringBootApplication` 会自动扫描 `com.travelmate` 下所有子包（包括 `backend` 认证模块和其余业务模块）。

### 包结构

- `com.travelmate.backend` — 用户认证系统（User 实体、JWT、Spring Security）
- `com.travelmate.common` — 统一响应体 `Result<T>`（code/msg/data）+ 全局异常处理
- `com.travelmate.controller` — 所有业务接口（Flight/Train/Hotel/AI/Post/Comment/Like/Follow/Admin 等）
- `com.travelmate.entity` — 实体类，使用 MyBatis-Plus 注解，表名前缀 `tm_`
- `com.travelmate.mapper` — MyBatis-Plus Mapper 接口
- `com.travelmate.service` — 业务接口 + `impl/` 实现类
- `com.travelmate.dto` — 请求 DTO（如 `FlightOrderCreateDTO`、`AiChatDTO`）

### 认证流程

1. `/user/register` 和 `/user/login` 公开访问，注册时 BCrypt 加密密码
2. 登录返回 JWT token（jjwt 0.11.5），前端存入 localStorage
3. 前端 `utils/request.js` 的 Axios 拦截器自动注入 `Authorization: Bearer <token>`
4. `JwtFilter` 从 Header 提取 token 并设置 `SecurityContext`
5. `SecurityConfig` 放行 `/user/**`、`/api/flight/**`、`/api/train/**`、`/api/hotel/**`、`/api/attraction/**`、`/api/post/list`、`/api/review/list`，其余需要登录
6. 需要登录但未带 token 的请求返回 403

### 前端路由守卫

Vue Router 通过 `meta.requiresAuth` 和 `meta.requiresAdmin` 控制访问：
- `requiresAuth`：检查 localStorage 中的 token，无 token 跳转 `/login`
- `requiresAdmin`：检查 `userInfo.role === 1`，非管理员跳转首页

### 防超卖机制

订单创建使用**乐观锁**（version 字段 + Redis 预减库存），适用于航班、火车票、酒店房型。

### AI 降级

AI 行程规划和客服调用 DeepSeek API（OpenAI 兼容协议），API 超时或失败时自动降级为预设模板，不会报错。

### 前端代理

Vite dev server 将 `/api` 和 `/user` 代理到 `http://localhost:8080`，开发时无需跨域配置。

### User 实体映射

`User` 实体的 `@TableName` 注解值为 `"tm_user"`（非默认表名 `"user"`），这是之前踩过的坑。

### 各子系统对应关系

| 子系统 | 负责同学 | 关键 Controller |
|--------|----------|-----------------|
| 大交通票务 | 邹林利 | FlightController, TrainController, TrafficOrderController, PassengerController |
| 住宿与本地生活 | 莫谨瑞 | HotelController, AttractionController, ReviewController |
| AI 智能规划 | 陈一鸿 | AiController |
| 社区与用户中心 | 杜新诚 | PostController, CommentController, LikeController, FollowController, UserProfileController, UserController |
| 管理后台 | 李科 | AdminController |
