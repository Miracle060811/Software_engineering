# 历史数据迁移验收

`Invoke-IsolatedDataMigrationAcceptance.ps1` 使用 7 个临时 MySQL 8.4 容器验证单体库向六服务数据库的真实迁移，不连接或修改开发者现有的 `3306` 数据库。

脚本执行以下检查：

1. 使用 `docs/sql/init.sql` 初始化临时单体源库；
2. 使用六份 `*-schema.sql` 初始化空目标库；
3. 调用正式迁移脚本并携带显式确认令牌；
4. 校验 31 张需要迁移的业务表在源库和目标库中的行数一致；
5. 无论成功或失败，都删除本次创建的临时容器。

在仓库根目录运行：

```powershell
.\04_tests\migration\Invoke-IsolatedDataMigrationAcceptance.ps1
```

默认使用 `35306`—`35312` 端口；任一端口被占用时脚本会在启动迁移前停止。结构化结果和脱敏日志保存在 `04_tests/migration/results/data-migration-2026-09-02/`。

该实验验证迁移脚本对课程基线数据的完整性和源库只读边界，不等同于生产数据库切换，也不会删除任何既有数据库。

2026-09-02 正式结果：31/31 张迁移表行数一致，源库未修改，7 个临时容器已清理，详见 [`results/data-migration-2026-09-02/summary.json`](results/data-migration-2026-09-02/summary.json) 和同目录 `migration.log`。
