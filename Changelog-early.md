# 修改日志 — 2026-04-19 ~ 2026-03-10（项目启动阶段）

| 日期       | 提交者                                        | 提交 Hash                          |
| ---------- | --------------------------------------------- | ---------------------------------- |
| 2026-04-19 | Zyy_mie                                       | `a34dd951`                         |
| 2026-04-16 | Yinghao Xiang                                 | `fb727b6e`                         |
| 2026-04-13 | YFan                                          | `6ac80e33`、`b23d11e6`、`1bf08489` |
| 2026-03-10 | YFan（多次）、Yinghao Xiang（Initial commit） | 多个                               |

## 概述

> 本文件汇总了项目启动到首个里程碑（version 1.0，2026-05-11）之前的所有提交。这一阶段以框架搭建、文档准备和用户认证基础功能为主。

---

## 2026-04-19 · 实现了用户注册、登录功能

**提交**: `a34dd951`

### feat · 新功能

- **后端 · Spring Boot 项目初始化**: 创建 `com.travelmate.backend` 包，配置 Spring Boot + Spring Security + MyBatis-Plus 基础依赖（`pom.xml`, `application.yml`）

- **用户认证**: 实现完整的注册/登录流程，密码 BCrypt 加密；基于 jjwt 生成 JWT token；`JwtFilter` 和 `JwtUtil` 负责 token 的签发与校验（`JwtFilter.java`, `JwtUtil.java`, `SecurityConfig.java`）

- **用户实体**: 新增 `User` 实体（映射 `tm_user` 表）和 `UserMapper`；`UserController` 提供 `/user/register`、`/user/login` 两个公开接口（`User.java`, `UserController.java`）

- **测试**: 新增 `BackendApplicationTests`（`BackendApplicationTests.java`）

### 文件变更

| 文件                                            | 说明             |
| ----------------------------------------------- | ---------------- |
| `backend/pom.xml`                               | 项目依赖初始化   |
| `backend/src/main/java/com/travelmate/backend/` | 认证模块全量新建 |
| `backend/src/main/resources/application.yml`    | 数据源/JWT 配置  |

---

## 2026-04-16 · 上传了软件开发计划书

**提交**: `fb727b6e`

### docs · 文档

- **软件开发计划书**: 上传《软件开发计划书.pdf》（后续重命名为《5 组-软件开发计划书.pdf》）

---

## 2026-04-13 · 框架与初期代码

**提交**: `1bf08489` "框架" / `b23d11e6` "提交代码到 main 分支" / `6ac80e33` "like\_成员 A"

> 项目仓库初始化阶段，各成员开始熟悉 Git 协作流程，提交内容主要为前期框架探索和个人分支代码试验。具体文件内容已在后续版本中被完整替换，不作详细记录。

---

## 2026-03-10 · 仓库初始化与 Git 学习

**提交**: `6357765d` "Initial commit" / `1fe446b3` "自己的说明" / `558efe4a` "suiban" / `bbe63d17` / `c2a22995` / `93ba3249`（均为"使用 git 仓库心得"/"使用 git 的一些思路"）

> 课程初期 Git 练习阶段，成员通过多次提交熟悉 git add/commit/push 流程，提交内容为个人学习笔记和 README 说明，无业务代码。

---

## 阶段总结

| 时间段     | 主要成果                                 |
| ---------- | ---------------------------------------- |
| 2026-03-10 | 仓库初始化，Git 协作熟悉                 |
| 2026-04-13 | 项目框架探索，各成员分支试验             |
| 2026-04-16 | 上传软件开发计划书                       |
| 2026-04-19 | Spring Boot 后端初始化，JWT 用户认证完成 |
| 2026-05-11 | **version 1.0** — 全量业务模块首次上线   |
