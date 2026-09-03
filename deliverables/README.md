# 课程交付归档

使用以下命令按课程任务书建议的 `01_source` ~ `06_defense` 目录结构生成提交压缩包：

```powershell
.\scripts\Build-CourseSubmissionArchive.ps1            # 完整版（含 PPT）
.\scripts\Build-CourseSubmissionArchive.ps1 -ExcludePpt # 不含 PPT 的草案
```

归档范围：六个交付目录（`01_source/`、`02_docs/`、`03_devops/`、`04_tests/`、`05_management/`、`06_defense/`）与根目录 `README.md`，仅含 Git 已跟踪文件，不包含 `.env`、构建缓存、依赖目录和本机数据库。完整源代码以仓库为准（见 `01_source/README.md` 仓库清单）。ZIP 默认不进入 Git；`archive-manifest.txt` 记录文件数、字节数和 SHA-256。

在四名成员确认个人权重、补齐 9 月 3 日看板截图之后，生成物才可标记为最终版。
