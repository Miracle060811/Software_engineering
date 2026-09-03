# TravelMate 容器化与 CI/CD 两人分工实施计划

## 1. 文档信息

| 项目 | 内容 |
| --- | --- |
| 项目 | TravelMate（伴游）出行旅游平台 |
| 计划主题 | 容器化、数据库脚本、CI/CD、Kubernetes 自动部署与验收证据 |
| 计划基线 | `main@80394a83720f648205210bbec2e3a3b1f6b5a701` |
| 编制日期 | 2026-08-27 |
| 执行人数 | 2 人 |
| 当前状态 | 远程 CI、安全检查、CodeQL 和镜像发布已通过；本机 Kubernetes 与自动部署尚未启用 |

> 本计划只包含容器化与 CI/CD 剩余工作，不再纳入已经由其他成员完成的独立专项。计划中的“成员1、成员2”是本次工作的角色编号，执行前填写真实姓名和 GitHub 账号。

## 2. 人数判断

### 2.1 结论

剩余工作由两人完成更合适，不建议再拆成三人。

原因如下：

1. 前后端 Dockerfile 已经存在，不需要重新设计。
2. GitHub Actions 主流水线、安全扫描、CodeQL 和镜像发布已经运行成功。
3. Kubernetes 资源清单和 CD 脚本已经基本完成，主要缺少真实环境启用和闭环验收。
4. 剩余代码修改集中在数据库迁移、部署配置、流水线补强和文档证据，三个成员同时修改容易发生文件边界重叠。
5. 两人可以按“运行环境与部署”和“数据库、流水线与交付”分开，责任清楚且工作量相对均衡。

如果以后增加生产级多节点部署、HPA、监控、云服务器或独立运维平台，再增加第三人更合理；这些内容不属于当前课程截图要求。

### 2.2 剩余工作量

剩余工作可以归并为 6 个工作包：

| 编号 | 工作包 | 当前情况 | 主责 |
| --- | --- | --- | --- |
| WP1 | Kubernetes 实机初始化与手工部署 | 清单和脚本已有，本机尚未启用 Kubernetes | 成员1 |
| WP2 | 自动 CD 与冷启动恢复 | 部署脚本已有，计划任务尚未安装和验收 | 成员1 |
| WP3 | 失败阻断、回滚和换机部署 | 有回滚逻辑，缺真实演练与换机证据 | 成员1 |
| WP4 | 数据库建表、迁移和数据脚本规范化 | 只有 `init.sql` 与 `update_images.sql`，缺版本迁移链 | 成员2 |
| WP5 | 流水线、镜像和部署门禁收尾 | 主流程已成功，缺迁移和部署配置校验 | 成员2 |
| WP6 | README、流水线记录与最终证据 | 文档已有基础，缺最新部署、失败和换机记录 | 成员2 |

其中 WP1—WP3 主要是本机和 Kubernetes 环境操作，WP4—WP6 主要是仓库代码、流水线和文档修改，适合两人并行。

## 3. 最终目标

形成以下完整闭环：

```text
向 main 推送代码
  → 自动获取代码、安装依赖和编译
  → 执行现有质量门禁和安全检查
  → 质量门禁全部通过
  → 构建前后端不可变 commit 镜像
  → Trivy 镜像扫描
  → 推进 GHCR deploy 通道
  → 演示机自动部署到 Kubernetes
  → 滚动更新、健康检查和失败回滚
  → 保存成功、失败、修复和部署证据
```

最终应满足：

1. 前端、后端、MySQL 和 Redis 分别运行在容器中。
2. 数据库具有建表、迁移、演示数据和独立验收数据脚本。
3. 任一必要门禁失败时，不发布镜像、不推进部署。
4. 前后端镜像使用同一完整 commit，并最终按 digest 部署。
5. 换一台符合前置条件的 Windows 机器，按照 README 可以完成部署。
6. 成功、失败、回滚、健康检查和换机验收均有可追溯证据。

## 4. 范围与非目标

### 4.1 本次范围

- Dockerfile 与镜像构建结果复核；
- MySQL、Redis、前端、后端 Kubernetes 部署；
- 数据库建表、版本迁移、演示数据和验收数据脚本；
- GitHub Actions 质量门禁、安全门禁和镜像发布；
- Docker Desktop Kubernetes 演示机自动部署；
- 自动部署失败阻断和回滚演练；
- 换机复现、README和流水线证据；
- 修正仓库文档中与实际部署不一致的描述。

### 4.2 本次非目标

- 不改造成微服务架构；
- 不建设生产级多节点高可用、异地容灾和真实 APM；
- 不接入真实支付、出票或商用库存；
- 不为了扩充材料而实现截图要求之外的 HPA 或 Compose；
- 不大规模重构业务代码；
- 不重复展开已经由其他成员负责的独立专项。

## 5. 当前基线

### 5.1 已完成

- `backend/Dockerfile` 和 `frontend/Dockerfile` 已提交，容器以非 root 用户运行；
- MySQL 8.0 和 Redis 7 使用固定镜像 digest；
- `deploy/k8s` 已包含 Namespace、ConfigMap、PVC、MySQL、Redis、backend 和 frontend；
- GitHub Actions 主流水线和统一质量门禁已经建立；
- Gitleaks、依赖审计、CodeQL 和 Trivy 已接入；
- 镜像使用 `sha-<完整 commit>`、`main`、`deploy` 三类 tag，并生成镜像 digest 证据；
- 最新 `80394a83` 的 CI、安全检查、CodeQL 和镜像发布均成功；
- CD 脚本已具备冷启动等待、Kind 代理修复、依赖就绪检查、健康检查和失败恢复能力；
- Kubernetes 清单可以通过 `kubectl kustomize deploy/k8s` 渲染；
- 所有 `scripts/cd/*.ps1` 均通过 PowerShell 语法解析。

### 5.2 当时缺口或证据不足

- 当前演示机没有 Kubernetes context；
- 尚未安装 `TravelMate-CD` 计划任务；
- 尚无 `%USERPROFILE%\TravelMateCD\deploy.log`；
- 最新 `80394a83` 尚未形成自动任务部署成功的本机证据；
- 数据库没有规范的版本迁移链；
- 数据库建表、迁移、演示数据和验收数据职责尚未完全分离；
- 尚缺空库初始化和已有库升级两条验证路径；
- 流水线尚未对数据库迁移和 Kubernetes 清单实施完整门禁；
- `05_management/pipeline-records` 尚无 2026-08-27 的最新部署记录；
- 尚缺隔离环境中的失败阻断和回滚演练；
- 尚缺独立机器或全新用户环境的换机部署证据；
- README、详细设计和实际实现仍有少量状态不一致。

## 6. 两人责任总表

| 人员 | 主责方向 | 主要产出 | 交叉复核 |
| --- | --- | --- | --- |
| 成员1：容器与部署负责人 | Docker Desktop、GHCR、Kubernetes、手工部署、自动任务、冷启动、回滚、换机部署 | 可运行集群、部署日志、镜像与 commit 核对、回滚和换机记录 | 成员2复核配置、记录和最终结果 |
| 成员2：数据库与流水线负责人 | 数据库脚本、版本迁移、MySQL/Redis配置、GitHub Actions、镜像门禁、README和证据 | 迁移脚本、流水线配置、数据验证记录、文档和最终验收清单 | 成员1复核运行命令和部署可用性 |

### 6.1 边界原则

- 成员1主导所有会改变本机 Docker Desktop、Kubernetes和计划任务状态的操作。
- 成员2主导所有数据库迁移、GitHub Actions和仓库交付文档修改。
- 成员1不直接修改已经发布的数据库迁移文件。
- 成员2不在未通知成员1的情况下操作演示机 Secret、镜像通道和计划任务。
- 每个成员负责的结果必须由另一人复核，不能自行宣布最终通过。
- 共享文件由主责人统一修改，另一人通过审查意见提出调整，避免同文件并行冲突。

## 7. 成员1：容器与部署负责人计划

### 7.1 职责范围

- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `deploy/k8s/**`
- `scripts/cd/**`
- Docker Desktop、kubectl、GHCR、Windows计划任务
- Kubernetes资源、镜像、探针、滚动更新和回滚

### 7.2 任务清单

#### A1：复核容器镜像

1. 核对前后端 Dockerfile 使用固定基础镜像 digest。
2. 核对前后端以非 root 用户运行。
3. 核对 OCI source、revision 和 created 标签。
4. 核对前端 Nginx `/healthz`、SPA回退和后端反向代理。
5. 核对镜像中没有复制 `.env`、凭据、缓存和无关开发文件。

验收：前后端镜像构建成功、容器能够启动、健康检查正常、镜像标签与 commit 一致。

#### A2：初始化 Docker Desktop Kubernetes

1. 确认 Docker Engine 正常。
2. 在 Docker Desktop 启用 Kubernetes。
3. 等待 `docker-desktop` context、Node和CoreDNS Ready。
4. 检查 Kind 节点代理，必要时使用现有 `Ensure-KindProxy.ps1` 修复。
5. 记录 Docker、kubectl 和 Kubernetes 版本。

验收：

- `kubectl config current-context` 为 `docker-desktop`；
- 所有 Node 为 `Ready`；
- CoreDNS Pod 为 `Running/Ready`；
- 不修改用户宿主机代理配置。

#### A3：配置 GHCR 和 Kubernetes Secret

1. 使用仅含 `read:packages` 的 GitHub PAT。
2. 运行 `Configure-TravelMateGhcrCredential.ps1`。
3. 验证前后端 `deploy` 镜像均可拉取。
4. 验证 `travelmate-ghcr` Secret存在。
5. 禁止在命令行参数、仓库、聊天记录和日志中保存 Token。

验收：Docker和Kubernetes均可拉取私有镜像，Git diff与日志中没有凭据正文。

#### A4：初始化并手工部署

按顺序执行：

```powershell
.\scripts\cd\Configure-TravelMateGhcrCredential.ps1
.\scripts\cd\Initialize-TravelMateKubernetes.ps1
.\scripts\cd\Deploy-TravelMate.ps1
```

核对：

- MySQL StatefulSet `1/1`；
- Redis Deployment `1/1`；
- backend Deployment `2/2`；
- frontend Deployment `2/2`；
- PVC为 `Bound`；
- 前后端镜像携带同一commit；
- Deployment固定到具体digest；
- 前端 `/healthz` 返回HTTP 200；
- 后端readiness和liveness探针通过。

#### A5：启用自动部署任务

1. 手工部署验收通过后运行 `Install-TravelMateDeploymentTask.ps1`。
2. 检查计划任务按照既定轮询规则运行。
3. 确认安装目录中的脚本是仓库最新版本。
4. 记录安装脚本SHA256和仓库commit。
5. 验证重复运行只确认当前版本健康，不重复更新Deployment。
6. 验证Docker Desktop冷启动后任务能够恢复集群和应用。

验收：

- `TravelMate-CD`任务存在且最近执行结果成功；
- `%USERPROFILE%\TravelMateCD\deploy.log`正常生成；
- 日志不包含敏感值；
- 脚本更新后重新安装计划任务副本。

#### A6：失败阻断与回滚演练

1. 在隔离Namespace或专用演练环境部署健康版本。
2. 记录原前后端digest和commit。
3. 使用错误镜像、错误探针或专用配置触发失败。
4. 验证失败版本没有成为可用版本。
5. 验证恢复原镜像后健康接口仍正常。
6. 保存失败原因、回滚过程和恢复结果。

验收：

- 失败后不继续后续部署；
- 前后端不会出现commit不一致；
- 数据库和上传PVC不受影响；
- 回滚后Deployment恢复原digest；
- 成功与失败日志均可追溯。

#### A7：换机部署复现

优先选择第二台Windows机器、新建Windows用户或清理过旧TravelMate状态的环境：

1. 全新clone仓库；
2. 检查Docker Desktop、PowerShell和kubectl；
3. 启用Kubernetes；
4. 配置GHCR；
5. 初始化并部署；
6. 安装自动任务；
7. 重启Docker Desktop；
8. 验证应用恢复和数据持久化。

成员1必须仅按README操作；如果需要口头补充隐藏步骤，应先由成员2修正文档，然后重新复现。

### 7.3 成员1交付物

- 容器镜像复核记录；
- Kubernetes资源状态清单；
- 前后端镜像commit/digest核对结果；
- 手工部署日志；
- 自动任务安装与运行记录；
- 冷启动恢复记录；
- 失败阻断与回滚记录；
- 换机部署记录；
- 部署相关截图。

## 8. 成员2：数据库与流水线负责人计划

### 8.1 职责范围

- `docs/sql/**`
- `backend/src/main/resources/db/migration/**`
- 数据库迁移相关后端配置
- `deploy/k8s/configmap.yaml`、`deploy/k8s/mysql.yaml`、`deploy/k8s/redis.yaml`中的数据配置建议
- `.github/workflows/**`
- `05_management/pipeline-records/**`
- `README.md`
- `scripts/cd/README.md`

### 8.2 任务清单

#### B1：盘点当前数据库脚本

将 `docs/sql/init.sql` 分类为：

- 数据库与表结构；
- 索引和约束；
- 基础字典数据；
- 演示业务数据；
- 独立验收数据；
- 历史兼容修复。

同时确认 `update_images.sql` 的依赖、幂等性和适用版本，形成迁移映射表。

#### B2：建立版本化迁移

目标结构：

```text
backend/src/main/resources/db/migration/
├── V1__baseline_schema.sql
├── V2__image_data_update.sql
└── 后续 V3、V4...

docs/sql/
├── init.sql
├── demo-data.sql
├── test-data.sql
└── migration-guide.md
```

实施要求：

1. 引入Flyway或等价的版本管理机制。
2. 空数据库执行完整基线。
3. 已有非空数据库使用明确的baseline策略后继续升级。
4. 已发布的迁移文件不得再修改，只能追加新版本。
5. 演示数据使用幂等插入或明确记录执行限制。
6. 独立验收数据不能进入正式部署。
7. 迁移失败时后端启动失败。
8. 迁移完全验证前保留现有 `init.sql`兼容入口，避免破坏 `setup.ps1`和Kubernetes初始化流程。

#### B3：验证空库初始化和旧库升级

空库路径：

- 创建空MySQL数据库；
- 启动迁移；
- 验证关键表、字段、索引和基础数据；
- 再次启动，确认不会重复破坏数据。

旧库路径：

- 从兼容基线建立旧版数据库；
- 插入保留性核对数据；
- 执行新迁移；
- 验证数据未丢失、结构升级成功；
- 再次执行迁移，确认迁移幂等或正确跳过。

#### B4：补齐数据库和部署静态门禁

在GitHub Actions中增加或复核：

- 数据库迁移文件顺序和命名；
- 空数据库能够完成迁移；
- `kubectl kustomize deploy/k8s`能够渲染；
- 前后端存在readiness/liveness探针；
- ConfigMap中没有明文敏感信息；
- 应用镜像不只使用 `latest`；
- `scripts/cd/*.ps1`通过PowerShell语法解析；
- 迁移或配置检查失败时不推进镜像发布。

#### B5：复核镜像发布与部署门禁

确认：

1. 只有同一commit的全部必要门禁通过后才能构建镜像。
2. 镜像先发布为不可变 `sha-<完整commit>`。
3. Trivy未通过时不能推进 `main` 和 `deploy` 通道。
4. 前后端镜像必须来自同一commit。
5. 发布证据包含commit、源Run ID和两个镜像digest。
6. 文档改动不会错误推进空镜像发布。

#### B6：维护流水线与部署记录

在 `05_management/pipeline-records`新增实际运行记录，至少包括：

- Commit SHA；
- CI、安全、CodeQL和镜像发布Run ID及链接；
- 前后端镜像tag和digest；
- Kubernetes Namespace、Pod和Deployment状态；
- 数据库迁移版本；
- 自动任务运行结果；
- 健康检查结果；
- 失败、原因、回滚和修复后成功记录；
- 截图和原始日志位置。

#### B7：更新交付文档

1. 更新README的全容器部署和换机步骤。
2. 补充停止、重启、升级、回滚、卸载计划任务和清理资源说明。
3. 明确Docker Desktop Kubernetes是单机演示环境，不冒充生产高可用。
4. 核对详细设计中Compose、HPA等描述与实际实现；未实现内容必须删除、标为后续规划或明确写“尚未实现”。
5. 确保README中所有路径、命令、端口和健康检查地址真实可用。
6. 根据成员1换机部署发现的问题修改文档，并要求成员1重新复现对应步骤。

### 8.3 成员2交付物

- SQL分类和迁移映射表；
- 版本化迁移文件；
- 演示数据和独立验收数据文件；
- 空库初始化核对记录；
- 旧库升级核对记录；
- 数据保留性和幂等性记录；
- 更新后的GitHub Actions门禁；
- Kubernetes和PowerShell静态检查；
- 流水线、镜像和部署证据索引；
- 更新后的README和CD使用说明；
- 最终验收清单。

## 9. 依赖与并行安排

```text
共同确认 main 基线
├─ 成员1：A1-A4 容器、Kubernetes和手工部署
└─ 成员2：B1-B3 数据库脚本与版本迁移

A4 和 B3 均通过
        ↓
成员2完成 B4-B5 流水线与镜像门禁
        ↓
成员1完成 A5 自动部署
        ↓
成员1执行 A6 回滚和 A7 换机部署
        ↓
成员2完成 B6-B7 证据与文档
        ↓
两人交叉复核并完成最终验收
```

### 9.1 可以并行

- 成员1初始化Kubernetes时，成员2可以盘点和重构数据库脚本。
- 成员1进行镜像、Pod和探针核对时，成员2可以补齐静态门禁。
- 成员1进行换机复现时，成员2可以同步整理已经产生的证据。

### 9.2 必须串行

- 数据库迁移通过空库和旧库验证后，才能接入正式流水线。
- 手工部署成功后，才能安装自动部署任务。
- 自动部署成功后，才能进行失败回滚演练。
- README修正后，成员1必须重新复现受影响步骤。
- 两人交叉复核后，才能在最终记录中写“通过”。

## 10. 分支、提交与合并规则

### 10.1 建议分支

- 成员1：`codex/container-cd-closeout`
- 成员2：`codex/database-ci-delivery`

### 10.2 提交边界

- 成员1提交Dockerfile、Nginx、Kubernetes应用部署和CD脚本修改。
- 成员2提交数据库迁移、数据配置、GitHub Actions、README和证据记录。
- `deploy/k8s/mysql.yaml`、`redis.yaml`和`configmap.yaml`由成员2提出数据配置，成员1统一落实Kubernetes变更。
- 禁止把本地Secret、PAT、`.env`、部署日志、缓存和临时截图加入提交。
- 只完成本机环境配置而未修改仓库时，不创建空提交。

### 10.3 推荐合并顺序

1. 成员2数据库迁移与兼容入口；
2. 成员2数据库和部署静态门禁；
3. 成员1部署脚本或Kubernetes清单修复；
4. 成员2文档和证据；
5. 两人共同检查最新 `main` 的流水线与部署结果。

## 11. 协作与证据机制

### 11.1 状态同步

每次状态同步只记录：

1. 已完成内容；
2. 当前阻塞；
3. 需要另一人提供的输入；
4. 相关commit、命令、日志或报告路径。

### 11.2 交叉复核

- 成员1完成部署后，由成员2核对commit、digest、资源状态和日志。
- 成员2完成迁移后，由成员1在Kubernetes环境验证真实启动。
- 成员2更新README后，由成员1仅按README进行换机复现。
- 涉及 `main`、镜像通道、数据库破坏性操作和演示环境时，必须由另一人复核。

### 11.3 凭据与数据安全

- PAT、JWT密钥、数据库密码和API Key不得写入仓库、命令参数、截图和聊天记录。
- 数据库重建前必须确认目标环境和备份状态。
- 失败演练必须使用隔离Namespace或明确可恢复的环境。
- 禁止为了制作失败截图破坏真实演示数据。

## 12. 风险与应对

| 风险 | 负责人 | 预防措施 | 回退方案 |
| --- | --- | --- | --- |
| Docker Desktop Kubernetes无法启动 | 成员1 | 冷启动等待、代理自检、记录版本 | 保留手工启动路径，不安装计划任务 |
| 私有GHCR镜像无法拉取 | 成员1 | 最小权限PAT、拉取预检 | 重新配置 `travelmate-ghcr`，不在代码中写密钥 |
| 迁移破坏已有数据库 | 成员2 | 备份、旧库升级验证、幂等检查 | 恢复备份并保留旧 `init.sql`流程 |
| 两个后端副本并发迁移 | 成员2 | 使用迁移锁并验证双副本启动 | 临时降为单副本完成迁移后恢复 |
| CI通过但部署失败 | 两人 | 镜像一致性、digest、探针和自动回滚 | 恢复旧镜像并记录失败 |
| 计划任务运行旧脚本 | 成员1 | 安装时记录commit和SHA256 | 拉取后重新安装任务副本 |
| 文档与真实状态不一致 | 成员2 | 成员1只按文档复现 | 修正文档后重新执行对应步骤 |
| 日志泄露凭据 | 两人 | 禁止打印Secret，提交前运行Gitleaks | 立即撤销并轮换凭据 |
| 任务拆分过细造成冲突 | 两人 | 维持两人文件边界 | 共享文件由主责人统一修改 |

## 13. 统一验收矩阵

| 验收项 | 主责 | 复核 | 通过标准 | 证据 |
| --- | --- | --- | --- | --- |
| 前后端镜像 | 成员1 | 成员2 | 可构建、非root、健康、commit一致 | Docker信息、镜像标签 |
| MySQL与Redis | 成员1 | 成员2 | Pod Ready、PVC Bound、连接正常 | Kubernetes状态、数据库核对 |
| 数据库基线 | 成员2 | 成员1 | 空库建表成功、重复启动正常 | 迁移日志、结构核对 |
| 已有库升级 | 成员2 | 成员1 | 数据保留、结构升级成功 | 升级前后核对表 |
| CI质量门禁 | 成员2 | 成员1 | 任一必要阶段失败则总门禁失败 | GitHub Actions Run |
| 镜像发布 | 成员2 | 成员1 | commit tag、Trivy和digest正确 | 发布Run、release JSON |
| Kubernetes部署 | 成员1 | 成员2 | 工作负载Ready、镜像digest正确 | `kubectl`输出 |
| 自动部署 | 成员1 | 成员2 | 能发现并部署新的合格镜像 | 计划任务、部署日志 |
| 健康检查 | 成员1 | 成员2 | 前端HTTP 200、后端Ready、数据库可用 | HTTP响应、探针状态 |
| 失败回滚 | 成员1 | 成员2 | 原镜像恢复、服务继续可用 | 失败与回滚日志 |
| 换机复现 | 成员1 | 成员2 | 仅按README完成部署 | 换机记录、截图 |
| 最终文档 | 成员2 | 成员1 | 命令、版本、路径和实际状态一致 | 文档审查记录 |

## 14. 最终完成定义

以下项目全部完成后，才能宣布本项工作结束：

- [ ] 两人真实姓名和GitHub账号已经填入计划；
- [ ] 本地和远程以相同 `main` commit为验收基线；
- [ ] 前端、后端、MySQL和Redis分别运行在容器中；
- [ ] Kubernetes Node、Pod、Service和PVC全部正常；
- [ ] 数据库建表、版本迁移、演示数据和独立验收数据职责清晰；
- [ ] 空库初始化和旧库升级均通过；
- [ ] 现有质量门禁和安全检查在CI中正常执行；
- [ ] 任一必要门禁失败时不会推进镜像和部署；
- [ ] 镜像使用完整commit版本并按digest部署；
- [ ] 演示机自动部署任务安装并运行成功；
- [ ] Docker Desktop冷启动后能够恢复；
- [ ] 部署失败能够恢复旧版本；
- [ ] 换机仅按README能够完成部署；
- [ ] 成功、失败、回滚、修复和健康检查证据齐全；
- [ ] README、详细设计和实际实现一致；
- [ ] 两人完成交叉复核并在最终验收记录中确认。

## 15. 最终交付物目录

```text
.github/workflows/                    # CI、安全和镜像发布门禁
backend/src/main/resources/db/migration/  # 版本化数据库迁移
backend/Dockerfile                    # 后端镜像
frontend/Dockerfile                   # 前端镜像
frontend/nginx.conf                   # 前端服务与代理配置
deploy/k8s/                           # Kubernetes资源清单
docs/sql/                             # 建表、迁移、演示和验收数据脚本
scripts/cd/                           # 初始化、部署和计划任务脚本
05_management/pipeline-records/       # 成功、失败、回滚和修复记录
README.md                             # 本地启动、容器部署和换机说明
```

本计划由成员1和成员2共同维护。范围或验收标准发生变化时，应先更新本文件，再开始超出原计划的工作。
