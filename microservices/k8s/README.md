# TravelMate 微服务独立数据环境（实验）

本目录保留六服务、六套独立 MySQL 和 Redis 的物理隔离实验环境，部署到 `travelmate-microservices` 命名空间。它不是正式部署入口；正式 Kubernetes 清单、HPA 和 CI/CD 统一使用 `deploy/k8s/` 与 `travelmate` 命名空间。

## 资源组成

| 类型 | 资源 | 副本与持久化 |
| --- | --- | --- |
| 业务服务 | `identity-service`、`traffic-service`、`local-service`、`ai-service`、`community-service`、`ops-service` | 固定 2 副本 |
| 数据库 | 六个 MySQL StatefulSet | 每套数据库独占 5Gi `ReadWriteOnce` PVC |
| 缓存 | Redis StatefulSet | 1Gi `ReadWriteOnce` PVC，开启 AOF |
| 配置 | ConfigMap、运行时 Secret、六套数据库初始化 ConfigMap | Secret 和 SQL ConfigMap 由部署脚本生成，不在仓库保存真实口令 |

所有 PVC 默认使用集群的默认 StorageClass。Docker Desktop 通常会动态供应本地持久卷；如现场集群没有默认 StorageClass，应先创建 StorageClass，或在 `volumeClaimTemplates` 中明确填写 `storageClassName`。

## 前置条件

1. Docker Desktop 已启用 Kubernetes，且存在 `docker-desktop` context。
2. `kubectl get nodes` 返回 Ready。
3. `microservices/.env` 已按 `.env.example` 配置真实本地值。

不要直接应用 `secret.example.yaml` 中的占位符，也不要把 `microservices/.env` 加入 Git。

## 构建与部署

在仓库根目录执行：

```powershell
.\scripts\cd\Deploy-Microservices.ps1 -KubeContext docker-desktop
```

脚本会依次：

1. 运行微服务 Maven `clean verify`；
2. 使用当前完整 Git SHA 构建六个不可变本地镜像标签；
3. 在内存中生成 Kubernetes Secret，日志不输出明文；
4. 从 `microservices/sql` 生成六套数据库初始化 ConfigMap；
5. 应用 Kustomize 清单并等待数据库、Redis 和六个服务完成 rollout；
6. 输出 Pod、Service 与 PVC 状态。

已有对应版本镜像时可传入 `-SkipBuild -ImageTag <tag>`，但镜像必须已存在于 Docker Desktop 本地镜像库，且不能使用 `latest`。

## 验证

```powershell
kubectl get pods,svc,pvc -n travelmate-microservices -o wide
kubectl top pods -n travelmate-microservices
kubectl port-forward service/traffic-service 18082:8082 -n travelmate-microservices
```

端口转发后访问：

```text
http://127.0.0.1:18082/actuator/health/readiness
http://127.0.0.1:18082/api/flight/search?depCity=北京&arrCity=上海
```

## 与正式部署的边界

本目录只用于验证六套数据库 StatefulSet、PVC 和服务数据边界，不再维护 HPA。正式 HPA 位于 `deploy/k8s/hpa.yaml`，实验命令和验收标准见 `04_tests/stress/README.md`。
