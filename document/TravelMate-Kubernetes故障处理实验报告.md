# TravelMate Kubernetes 故障处理实验报告

## 一、实验基本信息

| 项目 | 内容 |
|---|---|
| 实验名称 | Kubernetes 环境下身份服务停机故障处理测试 |
| 实验系统 | TravelMate 微服务系统 |
| 实验时间 | 2026年9月2日 |
| 正式执行时间 | 2026年9月2日 22:23 |
| 故障类型 | 依赖服务不可用 |
| 部署方式 | Docker Desktop Kubernetes |
| 实验结果 | **通过** |

## 二、实验背景与目的

TravelMate 的 `traffic-service` 在创建火车订单前，需要调用 `identity-service` 校验旅客是否属于当前用户。如果身份服务不可用，而交通服务没有正确处理该异常，可能造成请求长时间等待、错误信息不明确、订单或库存数据异常，甚至出现级联故障。

本实验在当前 Kubernetes 测试集群中主动将 `identity-service` 缩容到 0，验证系统是否满足以下要求：

1. 依赖服务不可用时，订单接口能够及时返回预先设计的降级提示。
2. 故障请求不会生成异常订单，也不会错误扣减库存。
3. `traffic-service` 及其他无关微服务不会跟随身份服务一起崩溃。
4. 身份服务恢复后，整个系统能够重新恢复正常状态。
5. 实验结束后恢复原 Deployment 副本数和 HPA，并清理临时业务数据与端口转发。

## 三、测试对象与范围

### 3.1 调用链

```text
火车订单创建接口
    ↓
traffic-service
    ↓ 旅客归属校验
identity-service
```

- 请求入口：`POST /api/order/train/create`
- 被注入故障的服务：`identity-service`
- 调用方：`traffic-service`
- 故障注入方法：临时删除 `identity-service` HPA 后，将其 Deployment 缩容到 0
- 访问方式：通过脚本自动建立的 `kubectl port-forward` 调用 ClusterIP 服务

### 3.2 测试范围

实验验证接口降级、订单与库存一致性、其他 5 个微服务的健康状态、身份服务恢复及 HPA 恢复。实验仅在本机 Docker Desktop Kubernetes 测试集群中执行，不访问生产环境，不修改业务代码，不删除数据库或 PVC。

## 四、实验环境

| 环境项 | 实际配置 |
|---|---|
| 操作系统 | Windows 本地环境 |
| Kubernetes context | `docker-desktop` |
| Kubernetes 版本 | `v1.36.1` |
| 集群节点 | 5 个，全部 `Ready` |
| 命名空间 | `travelmate` |
| 代码提交 | `900c5c8f9f1bd849e9c428f5bdd25621febb6b74` |
| 身份服务镜像 | `sha256:1bc4b37d3290157c7d73c86f4258cbdb095d6ce3f84066c298e7ae568320cec2` |
| 身份服务原副本数 | 2 |
| 身份服务 HPA | 最小 2、最大 6、CPU 目标 60% |
| 测试脚本 | `04_tests/fault/Invoke-IdentityOutageExperiment-k8s.ps1` |
| 结构化结果 | `04_tests/fault/results/identity-outage-k8s-20260902-222303.json` |
| 服务日志 | `04_tests/fault/results/identity-outage-k8s-20260902-222303.log` |

`traffic-service` 调用身份服务时配置了 `1000 ms` 连接超时和 `2000 ms` 读取超时。网络异常、超时或身份服务返回 5xx 时，系统会将错误转换为 HTTP 503，并返回固定提示“身份服务暂不可用，请稍后重试”。

## 五、实验前置条件

正式注入故障前完成了以下检查：

1. 当前 `kubectl` context 为 `docker-desktop`，`travelmate` 命名空间可访问。
2. `identity-service` 与 `traffic-service` Deployment 均存在且服务健康。
3. `identity-service` 故障前为 2 个 Pod，均处于 `Ready` 状态。
4. 使用临时用户名、旅客证件号和固定测试密码创建隔离测试数据，没有输出 JWT、CSRF Token 或集群 Secret。
5. 从当前 Kubernetes 数据库自动选择一趟二等座库存大于 1 的车次。
6. 记录故障前订单数、库存、Pod 名称和相关服务日志。
7. 保存 `identity-service` HPA 的实际配置，保证实验完成或异常退出时能够恢复。

## 六、实验步骤

1. 检查 PowerShell 7、`kubectl`、当前 context、命名空间、Deployment 和本地端口。
2. 为 `identity-service` 和 `traffic-service` 自动建立本地端口转发。
3. 调用身份服务创建临时用户和旅客，并取得本次请求所需的认证信息。
4. 查询可用车次，记录故障前订单数量和二等座库存。
5. 导出 `identity-service` HPA 的实际配置并临时删除该 HPA，防止 HPA 将 0 副本自动拉回到最小 2 副本。
6. 将 `identity-service` Deployment 缩容到 0。
7. 等待 Deployment 期望副本数变为 0，且 `identity-service` 的就绪 EndpointSlice 端点数变为 0。
8. 调用火车订单创建接口，记录 HTTP 状态码、业务码和提示信息。
9. 检查交通搜索接口、订单数量、库存以及其余 5 个微服务的健康状态。
10. 将 `identity-service` 恢复到原来的 2 个副本，并等待 rollout 和健康检查通过。
11. 重新应用实验前保存的 HPA 配置。
12. 删除临时旅客和用户，停止端口转发，保存结构化结果和脱敏日志。

在仓库根目录可使用以下命令复现实验：

```powershell
kubectl config current-context
kubectl get deployment,hpa -n travelmate
pwsh -NoProfile -File .\04_tests\fault\Invoke-IdentityOutageExperiment-k8s.ps1
```

## 七、预期结果与实际结果

| 检查项 | 预期结果 | 实际结果 | 判定 |
|---|---|---|---|
| 故障前身份服务 | 2 个副本正常运行 | `identityPodsBefore=2` | 通过 |
| 故障注入 | Deployment 期望副本为 0 | `identityDesiredReplicasDuringOutage=0` | 通过 |
| 服务端点 | 身份服务无可用后端 | `identityReadyEndpointsDuringOutage=0` | 通过 |
| 下单接口状态 | 身份服务不可用时返回 503 | HTTP 状态码和业务码均为 `503` | 通过 |
| 降级提示 | 返回明确、固定的错误提示 | “身份服务暂不可用，请稍后重试” | 通过 |
| 订单数据 | 失败请求不得生成订单 | 订单数量 `0 → 0` | 通过 |
| 库存数据 | 失败请求不得扣减库存 | 二等座库存 `320 → 320` | 通过 |
| 搜索功能 | 不依赖故障路径的功能保持可用 | 搜索接口业务码为 `200` | 通过 |
| 交通服务状态 | 故障期间仍保持存活 | `trafficHealthDuringOutage=UP` | 通过 |
| 其他服务隔离 | 其他服务不发生级联故障 | `traffic/local/ai/community/ops` 均为 `UP` | 通过 |
| 身份服务恢复 | 故障解除后恢复健康 | `identityRestored=true` | 通过 |
| HPA 恢复 | 恢复实验前的自动扩缩容配置 | `identityHpaRestored=true` | 通过 |
| 清理过程 | 不出现清理错误 | `cleanupErrors=[]` | 通过 |

正式结果文件中的 `passed=true`，所有核心断言均满足预期。

## 八、结果分析

身份服务的就绪端点降为 0 后，`traffic-service` 没有无限等待或崩溃，而是将依赖调用失败转换为结构化的 HTTP 503 响应。错误信息明确指出身份服务暂不可用并建议稍后重试，符合故障降级要求。

身份校验发生在库存扣减和订单写入之前，因此本次失败请求没有创建订单，也没有扣减库存。订单数量保持 `0`，二等座库存保持 `320`，说明当前调用顺序和事务边界能够保护业务数据一致性。

故障期间，交通服务健康检查为 `UP`，搜索接口返回业务码 200；`local-service`、`ai-service`、`community-service` 和 `ops-service` 也均为 `UP`。这说明身份服务故障被限制在当前依赖调用范围内，没有造成已检查服务的级联崩溃。

## 九、Kubernetes 专项说明

当前 `identity-service` 由 HPA 管理，HPA 的 `minReplicas=2`。如果只执行 `kubectl scale deployment identity-service --replicas=0`，HPA 会把副本数重新拉回，故障窗口不稳定。因此脚本只临时删除 `identity-service` 这一项 HPA，完成故障验证后再从实验前导出的实际配置恢复。其他 5 个 HPA 不受影响。

缩容过程中，旧 Pod 对象可能短暂处于 `Terminating` 状态。实验以 Deployment 期望副本数为 0、Service 就绪端点数为 0 作为“身份服务不可用”的准确判据，而不是只看 Pod 对象是否已经从 API 中完全消失。

## 十、恢复与清理结果

实验完成后已完成以下恢复与复核：

- `identity-service`：恢复为 `2/2 Ready`
- `identity-service` 健康接口：`UP`
- `identity-service` HPA：已恢复，最小 2、最大 6
- 临时旅客和临时用户：已执行删除
- 端口转发：已停止
- 数据库和 PVC：未删除、未重建
- 其他 5 个微服务：实验期间均为 `UP`
- 清理错误：0

身份服务 Pod 在缩容恢复后被 Kubernetes 重新创建，Pod 名称和运行时长发生变化属于本实验的预期现象。HPA 被重新创建后 `AGE` 也会重新计时，但其 `spec` 已按实验前配置恢复。

## 十一、证据边界与适用范围

本次实验是在本机 Docker Desktop Kubernetes 集群中完成，属于真实 Kubernetes 工作负载故障测试，但不是公有云生产环境测试。结论只覆盖 `traffic-service → identity-service` 停机场景，不代表已经验证网络分区、延迟注入、节点宕机、数据库故障或所有微服务调用链。

当前实现主要依靠连接超时、读取超时、统一 503 异常映射和事务顺序完成故障处理。本实验不证明系统已经配置完整的熔断器、舱壁或限流机制。

## 十二、实验结论

本次实验成功在 Kubernetes 环境中模拟了 `identity-service` 停机。系统能够返回预先设计的 HTTP 503 提示，没有生成异常订单或错误扣减库存，其他已检查微服务未发生级联崩溃；故障解除后身份服务、原副本数和 HPA 均恢复正常，实验数据与端口转发也完成清理。

综上，TravelMate 当前针对 `traffic-service → identity-service` 依赖停机场景的超时、降级、数据保护、服务隔离和恢复机制有效，本次 Kubernetes 故障处理实验判定为**通过**。

## 十三、实验依据

- 自动化脚本：`04_tests/fault/Invoke-IdentityOutageExperiment-k8s.ps1`
- 操作说明：`04_tests/fault/README.md`
- 结构化执行结果：`04_tests/fault/results/identity-outage-k8s-20260902-222303.json`
- 脱敏服务日志：`04_tests/fault/results/identity-outage-k8s-20260902-222303.log`
- Kubernetes 部署清单：`deploy/k8s/`
