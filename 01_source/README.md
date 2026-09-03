# 01_source 代码与仓库清单

> 本目录采用"仓库地址 + 文件清单"方式交付源码：代码本体不在此重复存放（避免与工程/CI 双份维护），请通过下方 Git 地址与标签获取完整可构建工程。

## 仓库地址

- **远程仓库**：<https://github.com/Miracle060811/Software_engineering>
- **默认分支**：`main`（完整提交历史，含改造前后全部记录）
- **源码文件清单**：[`source-file-list.txt`](source-file-list.txt)（由 `git ls-files backend frontend microservices` 导出，共 918 个被 Git 跟踪的源码文件，已自动排除 `node_modules/`、`target/`、`dist/` 等构建产物）

## 版本基线（性能对比与验收口径）

| 版本 | Git 标签 | 提交 | 说明 |
| --- | --- | --- | --- |
| 改造前单体基线 | `monolith-start` | `7258cd2c` | 上学期原系统，作为回归与性能对比基线，冻结不改 |
| 微服务阶段标签 | `microservices-phase1` | — | 六业务微服务拆分完成标记 |
| 最终交付 | `main` HEAD | 见仓库 | 最终验收版本 |

## 代码结构

| 目录 | 内容 |
| --- | --- |
| `backend/` | 单体后端（Spring Boot，改造前基线同时作为回归对照保留） |
| `frontend/` | Vue 3 前端 |
| `microservices/` | 六个业务微服务（identity / traffic / local / ai / community / ops）+ travelmate-contract 共享契约 + 各服务 SQL 与 K8s 配置 |
| `deploy/` | Kubernetes 部署清单（见 03_devops） |

## 获取代码

```powershell
git clone https://github.com/Miracle060811/Software_engineering.git
cd Software_engineering

# 查看改造前单体版本
git checkout monolith-start

# 查看最终微服务版本
git checkout main
```

启动方法、环境版本、端口、健康检查地址与测试账号见仓库根目录 `README.md`。
