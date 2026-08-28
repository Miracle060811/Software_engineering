# 微服务分库脚本

本目录的四个 `*-schema.sql` 由仓库根目录的 `scripts/Generate-MicroserviceSchemas.ps1` 从 `docs/sql/init.sql` 自动生成，只包含建库与建表 DDL，不复制演示种子数据。

重新生成：

```powershell
.\scripts\Generate-MicroserviceSchemas.ps1
```

生成器会校验：

- 声明的表必须存在于主初始化 SQL；
- 同一张表不能归属两个服务；
- 输出分别使用 `travelmate_identity`、`travelmate_traffic`、`travelmate_local`、`travelmate_ai` 数据库。

当前脚本用于全新环境初始化，不包含旧单体数据库的历史数据迁移。正式迁移前还需要补充数据导出、双写/停机窗口、校验和回滚方案。
