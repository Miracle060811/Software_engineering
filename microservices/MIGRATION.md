# 单体数据库迁移到四服务库

迁移工具：`scripts/Migrate-MicroserviceData.ps1`。

## 安全边界

- 默认只输出计划，不连接数据库、不写数据。
- 实际执行要求显式提供 `-Execute -ConfirmationToken MIGRATE_TO_EMPTY_TARGETS`。
- 目标 Schema 必须已初始化，且所有待迁移目标表必须为空；发现任一非空表立即停止。
- 迁移只读取源库，不删除、不更新源库。
- 每张表迁移后比较源库与目标库行数，不一致即失败。
- Outbox 表是新服务运行时数据，不从旧单体库迁移。

## 推荐步骤

1. 停止单体后端写流量，记录切换时间。
2. 备份源库，并用 `microservices/sql/*-schema.sql` 初始化四个空目标库。
3. 先运行 DryRun：

```powershell
.\scripts\Migrate-MicroserviceData.ps1
```

4. 配置 `DB_PASSWORD`、`IDENTITY_DB_PASSWORD`、`TRAFFIC_DB_PASSWORD`、`LOCAL_DB_PASSWORD`、`AI_DB_PASSWORD`，再执行：

```powershell
.\scripts\Migrate-MicroserviceData.ps1 `
  -Execute `
  -ConfirmationToken MIGRATE_TO_EMPTY_TARGETS
```

5. 检查逐表 `verified` 输出，启动四个服务，完成登录、交通下单、酒店下单、景点购票和通知消费冒烟测试。

## 回滚

迁移脚本不会修改源库。切换验证失败时，停止四个微服务并重新启动单体后端即可回到原数据源。目标库保留用于排查；确认不再需要后再通过受控数据库运维流程清理，不由迁移脚本自动删除。
