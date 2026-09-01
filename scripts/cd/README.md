# TravelMate 演示机部署脚本

这组脚本只面向 Docker Desktop Kubernetes 演示环境：

- `Configure-TravelMateGhcrCredential.ps1` 通过安全提示读取仅含 `read:packages` 的 GitHub Token，验证两个私有镜像可拉取，并配置 Docker 凭据存储与 Kubernetes `imagePullSecret`；Token 不写入命令行、仓库或日志。
- `Initialize-TravelMateKubernetes.ps1` 创建本地 Secret、MySQL 初始化 ConfigMap，并默认应用 local overlay；可通过 `-Environment base|local|server` 选择环境，已存在的 Secret 不会被自动轮换。
- `Apply-TravelMateConfiguration.ps1` 应用 local/server overlay，计算配置 hash 并触发后端滚动更新。
- `Update-TravelMateSecret.ps1` 通过安全提示更新单个应用 Secret，并默认滚动重启后端；数据库密码不由该脚本处理。
- `Rotate-TravelMateDatabasePassword.ps1` 按“核对现状 → 修改 MySQL 用户 → 验证 → 更新 Secret → 滚动后端”的顺序轮换应用数据库密码，并在失败时尽可能恢复原状态；可交互输入或使用 `-Generate`。
- `Backup-TravelMateKubernetes.ps1` 将 MySQL 逻辑备份、本地上传目录或 S3 bucket、仓库清单和不含 Secret 值的运行时清单保存到宿主机，并生成 SHA-256 校验清单。S3 模式要求宿主机安装 MinIO Client `mc`。
- `Restore-TravelMateKubernetes.ps1` 在备份校验通过后恢复 MySQL 和对应文件存储；该操作会覆盖目标数据库和目标 bucket，必须显式传入 `-ConfirmDataOverwrite`。
- `Deploy-TravelMate.ps1` 拉取固定 GHCR `deploy` 通道，确认前后端镜像携带同一个完整 commit，再按 digest 更新 Deployment；失败时恢复更新前镜像。
- `.github/workflows/ci.yml` 的 `deploy` job 通过带 `travelmate-deploy` 标签的 Windows self-hosted Runner 调用部署脚本，并上传 Kubernetes 与健康检查证据。
- `Install-TravelMateDeploymentTask.ps1` 是可选的本机轮询备用方案，将部署脚本复制到当前用户的 `%USERPROFILE%\TravelMateCD`，并注册每五分钟运行的 Windows 计划任务。
- 若 Docker Desktop Kind 节点继承了宿主机仅监听回环地址的代理，`Ensure-KindProxy.ps1` 会把该端口转接到 Docker Desktop 内置容器代理；初始化和每次部署都会自检，且不会改写宿主代理设置。

正式流水线首次启用顺序：Docker Desktop 启用 Kubernetes → 注册带 `travelmate-deploy` 标签的 Windows self-hosted Runner → GHCR 首次发布 → 配置私有 GHCR 只读凭据 → 初始化 Kubernetes：

```powershell
.\scripts\cd\Configure-TravelMateGhcrCredential.ps1
.\scripts\cd\Initialize-TravelMateKubernetes.ps1
```

Runner 先用 `run.cmd` 交互运行完成验收，确认其账号可以访问 Docker Desktop、Docker credential store 和 `%USERPROFILE%\.kube\config` 后，再决定是否配置为同一用户身份的 Windows 服务。GitHub Token 建议使用 classic PAT，仅授予 `read:packages` 并设置到期时间；不要把 Token 放入命令参数、文件或聊天记录。Token 到期或撤销后重新运行凭据配置脚本即可轮换。运行日志位于 `%USERPROFILE%\TravelMateCD\deploy.log`，不记录凭据正文。

## 集群重建前备份与恢复

修改 Docker Desktop Kubernetes 节点数、重置集群或删除 `travelmate` Namespace 前，先执行：

```powershell
.\scripts\cd\Backup-TravelMateKubernetes.ps1
```

默认备份目录为 `backups/kubernetes/travelmate-<时间戳>`，已被 `.gitignore` 排除。`STORAGE_TYPE=local` 时复制旧 uploads 目录；`STORAGE_TYPE=s3` 时通过 `mc mirror` 导出 bucket 到 `objects/`。本地 MinIO 默认使用宿主机入口 `http://127.0.0.1:30900`；外部 S3 需要传入 `-ObjectStorageEndpoint`。备份不会导出 `travelmate-secrets` 或 GHCR Token 明文。

新集群按以下顺序恢复：

```powershell
.\scripts\cd\Configure-TravelMateGhcrCredential.ps1
.\scripts\cd\Initialize-TravelMateKubernetes.ps1
.\scripts\cd\Restore-TravelMateKubernetes.ps1 `
  -BackupDirectory .\backups\kubernetes\travelmate-<时间戳> `
  -ConfirmDataOverwrite
```

恢复脚本会先校验全部备份文件的 SHA-256，再恢复 `travelmate` 数据库和对应文件存储，并等待后端滚动重启完成。S3 恢复使用 `mc mirror --remove`，会让目标 bucket 与备份完全一致；不要对错误的集群、数据库或 bucket 执行恢复。

## 数据库密码轮换

不要单独修改 `travelmate-secrets` 中的 `mysql-password`。在维护窗口执行：

```powershell
.\scripts\cd\Rotate-TravelMateDatabasePassword.ps1 -Generate
kubectl delete pod travelmate-mysql-0 -n travelmate
kubectl wait --for=condition=Ready pod/travelmate-mysql-0 -n travelmate --timeout=180s
```

脚本不会打印新旧密码。MySQL Pod 重启只刷新环境变量快照，不会删除 PVC；完成后仍需检查后端 readiness 和业务数据。
