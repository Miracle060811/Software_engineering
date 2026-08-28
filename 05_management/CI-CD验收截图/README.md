# CI/CD 验收截图说明

已自动生成：

1. `01-流水线总览成功.png`：同一流水线的完整执行图和最终 Success 状态。
2. `02-Docker镜像构建与Trivy扫描成功.png`：镜像构建、GHCR 推送、前后端 Trivy 扫描和 digest 记录。
3. `03-Kubernetes部署与健康检查成功.png`：实际部署、回滚保护、digest 核对和健康检查。
4. `04-流水线Artifacts验收证据.png`：发布与 Kubernetes 部署证据制品。
5. `05-PR216已合并.png`：修复 PR 已合并到 `main`。
6. `06-GHCR前后端镜像包.png`：GHCR 中的 `travelmate-backend` 和 `travelmate-frontend` 镜像包。

## 已补充的现场截图

7. `07-Kubernetes工作负载与Pod状态.png`：前后端 Deployment 副本全部可用，6 个 Pod 全部 `Running`，重启次数为 `0`。
8. `08-前后端健康检查.png`：前端返回 `200 / ok`，后端 readiness 返回 `{"status":"UP"}`。

以下命令为两张现场截图的复现方法。

打开 PowerShell，执行以下命令并使用 `Win + Shift + S` 截图：

```powershell
Clear-Host
kubectl --context docker-desktop -n travelmate get deployments
kubectl --context docker-desktop -n travelmate get pods
```

截图中应看到前后端 Deployment 为 `2/2`，所有 Pod 为 `Running` 和 `1/1`。

然后执行：

```powershell
Clear-Host
Invoke-WebRequest -UseBasicParsing http://127.0.0.1:30080/healthz | Select-Object StatusCode, Content
kubectl --context docker-desktop get --raw "/api/v1/namespaces/travelmate/services/http:travelmate-backend:8080/proxy/actuator/health/readiness"
```

截图中应看到前端 `200 / ok`，后端 `{"status":"UP"}`。

流水线地址：<https://github.com/Miracle060811/Software_engineering/actions/runs/33154114496>

PR 地址：<https://github.com/Miracle060811/Software_engineering/pull/216>
