# 修改日志 — 2026-05-21

| 字段      | 内容                   |
| --------- | ---------------------- |
| 提交者    | yfan945                |
| 提交 Hash | `3105158d`、`2232b78a` |

## 今日概述

> 今日共两个提交（`3105158d` "debug 上次" / `2232b78a` "数据库改进"）。主要工作是清理昨日遗留的临时 SQL 脚本、改进数据库 `init.sql`，以及新增 `AGENTS.md` 协作规范文件。社区首页和 `Community.vue` 进行了小幅前端调整，`PostMapper` 修复了一处查询映射问题。

---

## 变更内容

### fix · Bug 修复

- **社区 · PostMapper 查询**: 修复 `PostMapper.java` 中联表查询字段映射错误，导致游记列表部分字段为空（`PostMapper.java`）

- **数据库 · 临时脚本清理**: 删除 `data_supplement.sql` 和 `update_images_correct.sql` 两个一次性修复脚本，避免下次初始化被误执行（`docs/sql/`）

### chore · 配置与工程

- **协作规范 · AGENTS.md**: 新增 `AGENTS.md`，记录项目架构、包结构、命名规范、各子系统负责人，供 AI 辅助编码时参考（`AGENTS.md`）

- **数据库建议 · 文档**: 新增《数据库建议.md》，记录当前数据库设计的优化建议和待讨论事项（`数据库建议.md`）

- **数据库 · init.sql**: 调整初始化脚本内容，修正测试数据或结构定义（`docs/sql/init.sql`）

### style · 前端调整

- **首页 · Home.vue**: 小幅布局和样式调整（`Home.vue`）

- **社区 · Community.vue**: 社区列表页交互细节调整（`Community.vue`）

---

## 文件更改（关键源码）

| 文件                                         | 说明                 |
| -------------------------------------------- | -------------------- |
| `AGENTS.md`                                  | 新建，协作与架构规范 |
| `backend/.../mapper/PostMapper.java`         | 查询映射修复         |
| `docs/sql/init.sql`                          | 数据库脚本修正       |
| `docs/sql/data_supplement.sql`               | 已删除（临时脚本）   |
| `docs/sql/update_images_correct.sql`         | 已删除（临时脚本）   |
| `frontend/src/views/Home.vue`                | 样式微调             |
| `frontend/src/views/community/Community.vue` | 交互细节调整         |

---

## 未完成事项

- 社区评论回填用户信息（N+1 问题）仍存在，需专项重构

## 明日计划

1. 继续修复买票流程中的库存校验 Bug
2. 补充数据库种子数据，完善测试场景覆盖
