# 微服务故障处理实验

## 验证目标

两套脚本都会创建一次临时身份与旅客数据，记录交通订单数和火车库存，主动停止 `identity-service`，再验证：

- 创建交通订单返回 HTTP/业务码 503 和固定提示；
- `traffic-service` 健康接口与火车查询仍可用；
- 订单没有落库，库存没有扣减；
- 实验结束后 `identity-service` 自动恢复并重新通过健康检查。

## Docker Compose 版本

原有 `Invoke-IdentityOutageExperiment.ps1` 保留不变。运行前先用 `microservices/compose.yml` 启动环境，然后在仓库根目录执行：

```powershell
.\04_tests\fault\Invoke-IdentityOutageExperiment.ps1
```

## Kubernetes 版本

`Invoke-IdentityOutageExperiment-k8s.ps1` 适配仓库的 `deploy/k8s/` 部署，默认使用当前 `kubectl` context 和 `travelmate` 命名空间。脚本会：

1. 检查 `identity-service`、`traffic-service` 与本地转发端口；
2. 自动建立 `18081/18082` 端口转发；
3. 保存并临时删除 `identity-service` 的 HPA，避免 HPA 把 0 副本自动拉回；
4. 将 `identity-service` 缩容到 0，验证 503 降级、订单/库存不变以及其余 5 个微服务仍健康；
5. 在 `finally` 清理阶段恢复原副本数、原 HPA、临时用户和端口转发。

前置条件：PowerShell 7、`kubectl` 可用、当前 context 指向测试集群，并且调用者有读取、缩放 Deployment 及删除/恢复 HPA 的权限。执行前先确认目标集群：

```powershell
kubectl config current-context
kubectl get deployment,hpa -n travelmate
pwsh -File .\04_tests\fault\Invoke-IdentityOutageExperiment-k8s.ps1
```

如默认本地端口被占用，可以改用其他端口：

```powershell
pwsh -File .\04_tests\fault\Invoke-IdentityOutageExperiment-k8s.ps1 `
  -IdentityLocalPort 28081 -TrafficLocalPort 28082
```

> 该脚本会造成 `identity-service` 短暂停机，只能在允许故障注入的测试集群执行，不能直接用于生产环境。MySQL、PVC 和其他服务不会被删除。

结构化结果与脱敏服务日志写入 `04_tests/fault/results/`。结果文件不包含密码、JWT、CSRF Token 或内部服务 Token。
