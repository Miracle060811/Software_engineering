# 03_devops Docker、流水线、Kubernetes 与数据库脚本

本目录为课程交付归档副本，与仓库运行位置保持一致（原件保留原位以维持 CI/CD 与 Docker 构建上下文）。

## 目录内容

| 子目录 | 内容 | 原件位置 |
| --- | --- | --- |
| `docker/` | 前后端与六微服务 Dockerfile、单体/服务器/微服务 Compose 编排 | `backend/Dockerfile*`、`frontend/Dockerfile*`、`compose*.yml`、`microservices/services/*/Dockerfile` |
| `pipeline/` | GitHub Actions 流水线配置（CI/CD 主流水线、安全扫描、CodeQL） | `.github/workflows/` |
| `kubernetes/` | 单体前后端 K8s 部署清单（含 HPA、MySQL、RBAC） | `deploy/k8s/` |
| `kubernetes-overlays/` | 本地/服务器 Kustomize 覆盖层 | `deploy/k8s-overlays/` |
| `kubernetes-microservices/` | 六微服务 K8s 部署清单 | `microservices/k8s/` |
| `database/init-and-seed/` | 单体数据库建表与种子数据脚本 | `docs/sql/` |
| `database/flyway-migration/` | 单体 Flyway 迁移 V1–V9 | `backend/src/main/resources/db/migration/` |
| `database/microservices-schema/` | 六微服务分库 Schema 与种子脚本 | `microservices/sql/` |
| `scripts/` | 部署、备份、恢复、回滚与密钥轮换脚本（PowerShell） | `scripts/cd/` |

## 部署与回滚入口

- 单体部署/回滚：`scripts/Deploy-TravelMate.ps1`（含 rollout 状态检测与失败回滚）
- 微服务部署：`scripts/Deploy-TravelMateMicroservices.ps1`（支持 `-Service` 单服务定向更新）
- 备份/恢复：`scripts/Backup-TravelMateKubernetes.ps1`、`scripts/Restore-TravelMateKubernetes.ps1`
- 密钥轮换：`scripts/Rotate-TravelMateDatabasePassword.ps1`

镜像发布使用不可变 digest 并带 `sha-<commit>` 版本号，不使用 `latest`；流水线任一步失败即阻断镜像发布与部署。
