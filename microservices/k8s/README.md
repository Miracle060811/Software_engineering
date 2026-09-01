# TravelMate 微服务 Kubernetes 与 HPA

本目录将四个业务服务部署到独立的 `travelmate-microservices` 命名空间，不修改现有 `deploy/k8s` 单体部署。

## 资源组成

| 类型 | 资源 | 副本与持久化 |
| --- | --- | --- |
| 业务服务 | `identity-service`、`traffic-service`、`local-service`、`ai-service` | 初始 2 副本；HPA 最少 2、最多 6，CPU 目标 60% |
| 数据库 | 四个 MySQL StatefulSet | 每套数据库独占 5Gi `ReadWriteOnce` PVC |
| 缓存 | Redis StatefulSet | 1Gi `ReadWriteOnce` PVC，开启 AOF |
| 配置 | ConfigMap、运行时 Secret、四套数据库初始化 ConfigMap | Secret 和 SQL ConfigMap 由部署脚本生成，不在仓库保存真实口令 |

所有 PVC 默认使用集群的默认 StorageClass。Docker Desktop 通常会动态供应本地持久卷；如现场集群没有默认 StorageClass，应先创建 StorageClass，或在 `volumeClaimTemplates` 中明确填写 `storageClassName`。

## 前置条件

1. Docker Desktop 已启用 Kubernetes，且存在 `docker-desktop` context。
2. `kubectl get nodes` 返回 Ready。
3. `microservices/.env` 已按 `.env.example` 配置真实本地值。
4. HPA 实验前安装 Metrics Server，并确保 `kubectl top nodes` 能返回指标。
5. HPA 实验机安装 k6。

不要直接应用 `secret.example.yaml` 中的占位符，也不要把 `microservices/.env` 加入 Git。

## 构建与部署

在仓库根目录执行：

```powershell
.\scripts\cd\Deploy-Microservices.ps1 -KubeContext docker-desktop
```

脚本会依次：

1. 运行微服务 Maven `clean verify`；
2. 使用当前完整 Git SHA 构建四个不可变本地镜像标签；
3. 在内存中生成 Kubernetes Secret，日志不输出明文；
4. 从 `microservices/sql` 生成四套数据库初始化 ConfigMap；
5. 应用 Kustomize 清单并等待数据库、Redis 和四个服务完成 rollout；
6. 输出 Pod、Service、PVC 与 HPA 状态。

已有对应版本镜像时可传入 `-SkipBuild -ImageTag <tag>`，但镜像必须已存在于 Docker Desktop 本地镜像库，且不能使用 `latest`。

## 验证

```powershell
kubectl get pods,svc,pvc,hpa -n travelmate-microservices -o wide
kubectl top pods -n travelmate-microservices
kubectl port-forward service/traffic-service 18082:8082 -n travelmate-microservices
```

端口转发后访问：

```text
http://127.0.0.1:18082/actuator/health/readiness
http://127.0.0.1:18082/api/flight/search?depCity=北京&arrCity=上海
```

## HPA 实验

```powershell
.\scripts\experiments\Invoke-MicroserviceHpaExperiment.ps1
```

脚本以 `traffic-service` 为目标，运行现有 `flight-search.js`，每 5 秒记录 HPA、Pod 和 CPU 状态，等待扩容后再观察回落。原始结果写入 `04_tests/stress/results/hpa-*`，验收报告填写 `04_tests/stress/HPA实验记录模板.md`。

通过条件：

- k6 返回成功且错误率满足脚本阈值；
- 压力升高后 `traffic-service` Ready 副本数大于初始副本数；
- 压力结束后副本数回落到初始值；
- 日志中保留扩容前、扩容中、缩容后的 CPU、HPA 与 Pod 证据。
