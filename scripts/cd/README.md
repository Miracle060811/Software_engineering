# TravelMate 演示机部署脚本

这组脚本只面向 Docker Desktop Kubernetes 演示环境：

- `Configure-TravelMateGhcrCredential.ps1` 通过安全提示读取仅含 `read:packages` 的 GitHub Token，验证两个私有镜像可拉取，并配置 Docker 凭据存储与 Kubernetes `imagePullSecret`；Token 不写入命令行、仓库或日志。
- `Initialize-TravelMateKubernetes.ps1` 创建本地 Secret、MySQL 初始化 ConfigMap，并应用 `deploy/k8s`；已存在的 Secret 不会被自动轮换。
- `Deploy-TravelMate.ps1` 拉取固定 GHCR `deploy` 通道，确认前后端镜像携带同一个完整 commit，再按 digest 更新 Deployment；失败时恢复更新前镜像。
- `Install-TravelMateDeploymentTask.ps1` 将部署脚本复制到当前用户的 `%USERPROFILE%\TravelMateCD`，并注册每五分钟运行的 Windows 计划任务。
- 若 Docker Desktop Kind 节点继承了宿主机仅监听回环地址的代理，`Ensure-KindProxy.ps1` 会把该端口转接到 Docker Desktop 内置容器代理；初始化和每次部署都会自检，且不会改写宿主代理设置。

首次启用顺序：Docker Desktop 启用 Kubernetes → GHCR 首次发布 → 配置私有 GHCR 只读凭据 → 初始化 Kubernetes → 安装计划任务：

```powershell
.\scripts\cd\Configure-TravelMateGhcrCredential.ps1
.\scripts\cd\Initialize-TravelMateKubernetes.ps1
.\scripts\cd\Install-TravelMateDeploymentTask.ps1
```

GitHub Token 建议使用 classic PAT，仅授予 `read:packages` 并设置到期时间；不要把 Token 放入命令参数、文件或聊天记录。Token 到期或撤销后重新运行凭据配置脚本即可轮换。运行日志位于 `%USERPROFILE%\TravelMateCD\deploy.log`，不记录凭据正文。
