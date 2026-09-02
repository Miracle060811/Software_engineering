# 课程交付归档

使用以下命令生成不包含 PPT 的当前交付草案：

```powershell
.\scripts\Build-CourseSubmissionArchive.ps1 -ExcludePpt
```

归档只包含 Git 已跟踪文件和未被 `.gitignore` 排除的交付文件，不包含 `.env`、构建缓存、依赖目录和本机数据库。ZIP 默认不进入 Git；`archive-manifest.txt` 记录文件数、字节数和 SHA-256。

在四名成员确认个人权重、补齐 9 月 3 日管理材料以及组内 PPT 前，生成物只能标记为草案。
