# 微服务分库脚本

本目录的六组 `*-schema.sql` / `*-seed.sql` 由仓库根目录的 `scripts/Generate-MicroserviceSchemas.ps1` 从规范源 `docs/sql/init.sql` 自动生成。schema 文件只包含建库与建表 DDL；seed 文件只抽取各服务本地集成与 E2E 所需的演示数据。

重新生成：

```powershell
.\scripts\Generate-MicroserviceSchemas.ps1
```

同步生成 `03_devops` 交付镜像：

```powershell
.\scripts\Generate-MicroserviceSchemas.ps1 -OutputDirectory .\03_devops\database\microservices-schema
```

生成器会校验：

- 声明的表必须存在于主初始化 SQL；
- 同一张表不能归属两个服务；
- 标记的定向 seed 必须指向已声明服务；
- 输出分别使用 `travelmate_identity`、`travelmate_traffic`、`travelmate_local`、`travelmate_ai`、`travelmate_community`、`travelmate_ops` 数据库。

local schema 当前包含 16 张表，其中 `tm_tour_schedule`、`tm_tour_order` 及其产品/班期外键、库存/金额约束、唯一键和索引均来自 `docs/sql/init.sql`。班期 seed 使用相对 `CURRENT_DATE` 的未来日期，并仅在对应产品没有未来可售班期时补充，因此同日重复导入不会新增重复班期，历史班期也不会被改写。

单体应用的存量库升级仍以 backend Flyway 迁移为准；`V10__tour_booking_workflow.sql` 与规范初始化源保持 UC08 结构和动态班期策略一致，但两者服务于“升级”和“全新初始化”两条不同路径。

当前脚本用于全新环境初始化，不包含旧单体数据库的历史数据迁移。正式迁移前还需要补充数据导出、双写/停机窗口、校验和回滚方案。
