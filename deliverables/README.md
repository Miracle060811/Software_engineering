# 课程交付归档

使用以下命令从当前物理 `01_source` ~ `06_defense` 目录生成提交压缩包：

```powershell
.\scripts\Build-CourseSubmissionArchive.ps1            # 完整版（含 PPT）
.\scripts\Build-CourseSubmissionArchive.ps1 -ExcludePpt # 不含 PPT 的草案
.\scripts\Build-CourseSubmissionArchive.ps1 -DryRun     # 仅校验白名单，不写文件
```

脚本只从六个交付目录中的 Git 跟踪文件及当前未忽略的最终新增文件取材，并按显式白名单和排除规则保留正式材料。白名单排除内部审阅、制作大纲、实施计划、交接记录、模板、旧稿、原始备份、历史中间材料、缓存、依赖目录、真实 `.env`、私钥、Token 和本机数据库。完整源代码以仓库为准（见 `01_source/README.md` 仓库清单）。

`06_defense/TravelMate云原生改造-最终答辩-第5组.pptx` 是唯一会进入归档的 PPT。包内 `SUBMISSION-MANIFEST.txt` 与包外 `archive-manifest.txt` 记录来源 HEAD、文件数、分类数量和 SHA-256；ZIP 与包外清单默认不进入 Git。

四名成员已确认个人权重；最终本地技术验证已 18/18 通过，证据位于 `04_tests/pipeline/results/final-local-validation-20260903-ee64dc34/`。9 月 3 日看板与确认记录均作为正式管理材料归档；现场演练属于提交前操作，不改变技术验收结论。
