# 微服务故障处理实验

## TRAFFIC→IDENTITY 故障隔离

`Invoke-IdentityOutageExperiment.ps1` 会创建一次临时身份与旅客数据，记录交通订单数和火车库存，主动停止 `identity-service`，再验证：

- 创建交通订单返回 HTTP/业务码 503 和固定提示；
- `traffic-service` 健康接口与火车查询仍可用；
- 订单没有落库，库存没有扣减；
- 实验结束后 `identity-service` 自动恢复并重新通过健康检查。

运行前先用 `microservices/compose.yml` 启动环境，然后在仓库根目录执行：

```powershell
.\04_tests\fault\Invoke-IdentityOutageExperiment.ps1
```

结构化结果与服务日志写入 `04_tests/fault/results/`。结果文件不包含密码、JWT、CSRF Token 或内部服务 Token。
