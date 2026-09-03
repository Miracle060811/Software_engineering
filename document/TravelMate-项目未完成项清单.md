# TravelMate 项目未完成项与整改清单

> 基于《软件工程基础实践-2026夏》任务书要求，对当前仓库（commit `dda33316` 附近）进行逐项盘点后的差距分析。
> 文档生成时间：2026 年夏季学期实践第 10 天前后。

---

## 一、总体结论

| 维度 | 完成度 | 一句话判断 |
|------|--------|------------|
| 文档与设计（10 分） | 高 | 需求/概要/详细设计、三层模型图、追溯表、用例说明已覆盖 UC01–UC19。 |
| 测试（10 分） | 高 | 单元/集成/E2E 三类测试、断言、报告、CI 门禁均已落地。 |
| 容器化（8 分） | 高 | 前后端 Dockerfile、数据库脚本、Compose、README 启动说明完整。 |
| 原系统 CI/CD（10 分） | 高 | GitHub Actions 已覆盖取代码→编译→测试→镜像→K8s 部署→健康检查。 |
| 微服务划分（7 分） | 高 | 6 个业务服务、划分图、接口清单、表归属、跨服务调用说明完整。 |
| 数据归属（5 分） | 高 | 34 张表唯一归属，边界门禁通过，无跨服务直连表。 |
| 微服务流水线（5 分） | 高 | 六服务独立构建、测试、镜像、部署已实现。 |
| 用例回归（4 分） | 高 | 公开接口与 UC01–UC19 的 E2E 回归已接入 CI。 |
| 自动扩缩容（3 分） | **中** | HPA YAML 已配置，但负载实证/重复实验记录不完整。 |
| 故障处理（2 分） | 高 | identity-service 停机实验已通过，有原始 JSON 记录。 |
| 性能对比（4 分） | **低** | **单体 vs 微服务同机同数据同脚本的 ≥3 次对比数据缺失**。 |
| 项目管理（4 分） | 中 | 看板、CI 截图、证据总表具备；站会/流水线/PR 记录有断档。 |
| 交付和报告（6 分） | **中** | 最终 01–06 打包目录未组织，独立技术总结报告缺失，个人权重表缺失。 |
| 现场演示（5 分） | 高 | 最新 Run `33649461016` 全绿，可现场 push→流水线→K8s。 |
| 答辩问答（5 分） | 高 | 答辩 PPT、演讲稿、问答库已准备。 |

**当前最大风险点**：
1. **性能对比原始数据未生成**（4 分，直接影响第二阶段得分）。
2. **HPA 扩缩容实验记录不完整**（3 分）。
3. **个人权重表、全员确认记录、独立技术总结报告、最终打包目录缺失**（管理/交付分，且为最终提交硬性要求）。

---

## 二、按课程要求逐项核对

### 1. 前 5 天任务（40 分）

#### 1.1 文档和图补齐（文档 10 分）

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| 需求规格说明书 | ✅ | `document/5组-软件需求规格说明.md` | 含 REQ-01–REQ-19、UC01–UC19、用例图、概念类图 |
| 概要设计说明书 | ✅ | `document/软件概要设计说明书.md` | 含组件图、UC 组件级模型 |
| 详细设计说明书 | ✅ | `document/5组-软件详细设计说明.md` | 含类图、对象级顺序图、接口清单 |
| 追溯表 | ✅ | `document/需求设计代码测试追溯表.md` | REQ→UC→SYS/COMP/OBJ→代码→UNIT/INT/E2E |
| 每个 UC 系统级图 | ✅ | `document/需求规格说明/REQ-FIG-03`–`REQ-FIG-21` | 19 张，含 Mermaid 源文件 |
| 每个 UC 组件级图 | ✅ | `document/概要设计说明/HLD-FIG-02`–`HLD-FIG-20` | 19 张，含 Mermaid 源文件 |
| 每个 UC 对象级图 | ✅ | `document/详细设计说明/DLD-FIG-02`–`DLD-FIG-20` | 19 张，含 Mermaid 源文件 |
| 用例说明六要素 | ✅ | `document/业务场景清单与用例说明.md` | 参与者、触发条件、前置条件、主流程、备选/异常、可验证结果 |
| 用例清单 | ✅ | `document/业务场景清单与用例说明.md` 第 2 节 | UC01–UC19 |
| 需求-用例-模型-代码-测试对应 | ✅ | `docs/ci/use-case-test-matrix.json` | 机器可读追溯矩阵 |

**待补/注意**：
- `document/` 与 `中期检查/` 存在同名文件，最终交付前需确保版本一致。

#### 1.2 测试补齐并跑通（测试 10 分）

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| 单元测试 | ✅ | `backend/src/test/java/com/travelmate/`（46 类/201 方法）、`frontend/tests/unit/`、`microservices/services/*/src/test/` | 有断言 |
| 集成/API 测试 | ✅ | `backend/src/test/java/com/travelmate/integration/`、`microservices/services/*/src/test/...ApiTests.java` | 含 MySQL 集成、MockMvc 接口契约 |
| 端到端测试 | ✅ | `frontend/tests/e2e-real/travelmate-real.spec.js`、`frontend/tests/e2e-real/travelmate-real.spec.js` 微服务版 | 覆盖 UC01–UC19 |
| 测试报告要素完整 | ✅ | `document/测试报告.md`、`document/测试执行报告-2026-08-28.md`、`-09-01.md` | 总数/通过/失败/环境 |
| 失败让流水线停止 | ✅ | `.github/workflows/ci.yml` | 任一步失败即中止 |

**待补/注意**：
- 本地 `target/surefire-reports` 滞后于最新源码，交付前应重新全量运行并更新报告。
- 早期报告 `document/测试报告.md` 数字（15/16）与当前代码规模不一致，建议归档为历史基线而非最终报告。

#### 1.3 容器化与 CI/CD（容器化 8 分 + 原系统 CI/CD 10 分）

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| 前后端 Dockerfile | ✅ | `backend/Dockerfile`、`frontend/Dockerfile` | 另有 `.compose` 变体 |
| 数据库脚本 | ✅ | `docs/sql/init.sql`、`backend/src/main/resources/db/migration/V1__*`–`V7__*` | 含 Flyway 迁移 |
| README 启动说明 | ⚠️ 基本覆盖 | `README.md` | 环境/端口/启动/健康检查/初始数据已写，**未写明测试账号** |
| 流水线覆盖全阶段 | ✅ | `.github/workflows/ci.yml` | checkout→依赖→编译→单元→集成→镜像→K8s→健康检查 |
| 镜像带版本号 | ⚠️ CI 侧满足 | `ci.yml` 使用 `sha-<commit>` 与 digest | 静态 YAML 用 `:deploy` 占位，需说明实际部署替换为 digest |
| K8s 部署文件 | ✅ | `deploy/k8s/`、`deploy/k8s-overlays/` | 含前后端、六服务、HPA、MySQL、Redis |
| 成功/失败记录 | ⚠️ 远端有，本地未同步 | GitHub Actions Run `33649461016`（成功）、`33632106449`（失败） | `05_management/pipeline-records/` 仅到 08-28，缺 09 月初失败与修复记录 |

**待补/注意**：
- README 可补充种子账号位置：`docs/sql/init.sql:592-596`，默认密码 `123456`。
- 静态 K8s YAML 的 `:deploy` 标签需配说明文档，避免验收质疑。
- 补充 `05_management/pipeline-records/2026-09-02.md` 记录 HPA 兼容修复过程。

---

### 2. 后 5 天任务（30 分）

#### 2.1 后端微服务拆分（微服务划分 7 分 + 数据归属 5 分）

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| ≥3 业务微服务 | ✅ | `microservices/services/` 下 6 个服务 | identity/traffic/local/ai/community/ops |
| 服务独立构建/测试/部署 | ✅ | 每个服务独立 `pom.xml`/`Dockerfile`、CI matrix job | 父 POM module 已声明 |
| 业务表归属明确 | ✅ | `microservices/README.md`、`document/TravelMate中期验收基线.md` | 34 张表唯一归属 |
| 不跨服务联表查询 | ✅ | 边界检查脚本 `check-microservice-boundaries.mjs` | 已通过 |
| 跨服务调用说明 | ✅ | `microservices/README.md`、`document/5组-软件详细设计说明.md` 4.2.4 节 | 含超时/失败处理 |
| 服务划分图 | ✅ | `document/中期验收/TravelMate服务划分图.svg` | 含 `.mmd` 源文件 |
| 接口清单 | ✅ | `docs/ci/microservice-api-coverage.json` | 113 个端点 |

**待补/注意**：
- `scripts/Generate-MicroserviceSchemas.ps1` 与 `microservices/sql/README.md` 仍写“4 个服务”，与当前 6 服务不一致，需同步更新。
- `deploy/k8s/kustomization.yaml` 未显式列出六服务 Deployment，实际由 `Deploy-TravelMateMicroservices.ps1` 生成；若验收要求 `kubectl apply -k` 可直接运行，需补齐。

#### 2.2 微服务自动构建与部署（微服务流水线 5 分 + 用例回归 4 分）

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| 微服务自动构建/测试/镜像/部署 | ✅ | `.github/workflows/ci.yml` microservices / microservice-publish / deploy jobs | 使用 `sha-<commit>` + digest |
| 所有公开接口 API 测试 | ✅ | `docs/ci/microservice-api-coverage.json`、各 `*ApiTests.java` | 门禁通过 |
| 全部 UC E2E 回归 | ✅ | `document/测试执行报告-2026-09-01.md` | 本地 17/17 通过；CI 远端需确认最新 Run 全绿 |
| 服务日志/健康/就绪/版本号 | ✅ | `deploy/k8s/*-service.yaml` 探针、`README.md` 健康地址 | 部署后查看 |

**待补/注意**：
- `document/测试执行报告-2026-09-01.md` 指出：**单服务真实更新/回滚未在真实 K8s 执行**，仅做了 fake `kubectl` 回归测试。建议补充一次真实 K8s 单服务更新与回滚记录。

#### 2.3 云原生实验

| 实验 | 分值 | 状态 | 关键证据路径 | 备注 |
|------|------|------|--------------|------|
| 自动扩缩容 HPA | 3 | ⚠️ 部分 | `deploy/k8s/hpa.yaml`、`scripts/experiments/Invoke-MicroserviceHpaExperiment.ps1`、`04_tests/stress/results/HPA实验记录-20260902.md` | 静态配置和一次实验记录存在，但**同口径 ≥3 次重复实验的 CPU/内存采样、吞吐量/P95 完整记录待补齐** |
| 故障处理 | 2 | ✅ | `04_tests/fault/Invoke-IdentityOutageExperiment-k8s.ps1`、`04_tests/fault/results/identity-outage-k8s-20260902-222303.json` | identity 停机后返回 503，订单/库存不变，其余服务健康 |

**HPA 待补细节**：
- 压力脚本 `04_tests/stress/flight-search.js` / `hotel-search.js` / `hotel-order.js` 已具备，但当前 `results/` 下仅有 traffic-service 的 HPA 数据。
- 缺少系统化的 CPU/内存采样日志（如 `kubectl top pods` 或 `Get-Process java` 输出）。
- 旧记录 `hpa-20260902-173425` 为假阴性，报告里需明确排除。

#### 2.4 性能对比（4 分）—— **当前最大缺口**

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| 同机同数据同脚本 | ❌ | 无 | 脚本已具备，但未执行 |
| 2–3 个主要接口 | ❌ | 无 | 建议 `flight-search`、`hotel-search`、`hotel-order` |
| 各运行 ≥3 次 | ❌ | 无 | 单体 8080 3 次 + 微服务入口 3 次 |
| 记录并发/吞吐/平均/P95/错误率/CPU/内存 | ❌ | 无 | 需 k6 导出 + 资源采样 |
| 原始数据与解释 | ❌ | 无 | 需形成对比表与结论 |

**建议执行路径**：
1. 启动单体版本（`compose.yml` 或本地 `start.ps1`），固定 `BASE_URL=http://127.0.0.1:8080`。
2. 启动微服务版本（`microservices/compose.yml` 或 K8s 入口）。
3. 对三个脚本分别跑 3 轮（共 18 组数据），用 `--summary-export=results/<scene>-<arch>-run<N>.json`。
4. 同时采样 CPU/内存（Windows：`Get-Process java` / `kubectl top pods`）。
5. 整理 `document/单体与微服务性能对比报告.md`，附原始 JSON/CSV。

---

### 3. 项目管理、交付、演示与答辩（20 分）

#### 3.1 项目管理（4 分）

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| 每日站会简报 | ⚠️ 部分 | `05_management/standups/2026-08-29.md`、`-08-31.md`、`-09-01.md` | **缺少 08-25、08-26、08-27 独立站会文件**；`2026-08-28.md` 实际为“用例清单”，疑似归档错位 |
| 看板/统计截图 | ✅ | `05_management/kanban/2026-08-25.pdf` ~ `2026-09-02.pdf` | 共 8 份 |
| 任务管理证据 | ✅ | `05_management/任务级证据总表.md`、实施计划、整改计划 | 任务编号/负责人/验收条件/证据链接 |
| PR 合并与评审 | ⚠️ 部分 | `05_management/CI-CD验收截图/05-PR216已合并.png`、流水线索引 | 无独立 PR 记录目录 |

**待补/注意**：
- 补齐 08-25/26/27 站会简报；修正 `2026-08-28.md` 的目录位置或重命名。
- 补充 08-29、08-31、09-01、09-02 的流水线日记录。

#### 3.2 交付和报告（6 分）

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| 改造前后 Git 标签 | ✅ | `git tag`：`monolith-start`、`microservices-phase1` | 已存在 |
| README | ✅ | `README.md`、`document/部署文档.md` | 较完整 |
| Dockerfile/流水线/K8s/数据库脚本 | ✅ | 见上文 | 完整 |
| 测试代码与报告 | ✅ | `04_tests/`、`document/测试报告.md` 等 | 完整 |
| 压力脚本、原始结果、≥3 次重复 | ❌ | 仅 HPA 数据 | 性能对比未做 |
| 完整用例清单与设计/测试/追溯 | ✅ | `document/业务场景清单与用例说明.md`、追溯表 | 完整 |
| 服务划分图/接口清单/表归属/跨服务调用 | ✅ | 见上文 | 完整 |
| 项目管理平台/站会/看板 | ⚠️ 部分 | 看板截图齐全，站会/PR 有缺 | 见 3.1 |
| 答辩 PPT、技术总结报告、个人权重表 | ⚠️ 部分 | PPT 已准备，**技术总结报告和个人权重表缺失** | 见 3.3 |
| 最终 01–06 目录打包 | ❌ | 仅 `04_tests/`、`05_management/` 存在 | `01_source/`、`02_docs/`、`03_devops/`、`06_defense/` 未建立 |

#### 3.3 答辩与演示（5 + 5 分）

| 验收要求 | 状态 | 关键证据路径 | 备注 |
|---------|------|--------------|------|
| 答辩 PPT | ✅ | `defense_ppt/export/TravelMate云原生改造-最终答辩-第5组.pptx` | 17 页 |
| 技术总结报告 | ❌ | 无独立文件 | 功能由 PPT 与 `outputs/travelmate-defense-concepts.md` 替代，但未形成正式报告 |
| 个人权重表 | ❌ | 无 | 课程明确要求 |
| 全员确认记录 | ❌ | 无 | 课程明确要求 |
| AI 工具使用说明 | ⚠️ 分散 | PPT 尾页、`outputs/travelmate-defense-concepts.md` | 未写入独立技术总结报告 |
| 现场演示基线 | ✅ | GitHub Actions Run `33649461016` | 最新全绿，可现场 push→流水线→K8s |

**待补/注意**：
- 必须创建 `技术总结报告.md`（或 `.docx`），内容包含：项目概述、微服务拆分思路、CI/CD 过程、测试结果、HPA/故障/性能实验、AI 工具使用说明、个人分工与贡献。
- 必须创建 `个人权重表.xlsx/.md` 及 `全员确认记录.pdf/.png`，权重和 ≤ 组员人数，个人权重 0.9–1.1（特殊情况可低至 0.5）。
- 最终答辩 PPT 第 16 页“性能对比”目前仅有设计展示，需在补做实验后替换为真实数据。

---

## 三、整改优先级清单

| 优先级 | 事项 | 对应评分/要求 | 建议负责人 | 建议交付物 |
|--------|------|---------------|------------|------------|
| **P0** | 完成单体 vs 微服务性能对比实验并出报告 | 第二阶段 4 分 | 性能/测试负责人 | `04_tests/stress/results/*-run*.json`、`document/单体与微服务性能对比报告.md` |
| **P0** | 补齐 HPA 扩缩容完整实验记录 | 第二阶段 3 分 | 运维/云原生负责人 | `04_tests/stress/results/HPA实验记录-最终.md`、CPU/内存采样日志 |
| **P0** | 建立最终 01_source~06_defense 打包目录 | 交付 6 分 | 组长/配置管理员 | `01_source/`、`02_docs/`、`03_devops/`、`04_tests/`、`05_management/`、`06_defense/` |
| **P0** | 撰写独立技术总结报告 | 交付/答辩 | 文档负责人 | `document/技术总结报告.md` |
| **P0** | 制作个人权重表与全员确认记录 | 个人成绩 | 组长 | `05_management/个人权重表.xlsx`、`全员确认记录.pdf` |
| **P1** | 补充单服务真实 K8s 更新/回滚记录 | 现场演示 | 运维负责人 | `05_management/pipeline-records/2026-09-0X-单服务回滚验证.md` |
| **P1** | 补齐 08-25/26/27 站会简报，修正 08-28 归档 | 项目管理 4 分 | PM | `05_management/standups/2026-08-25.md` 等 |
| **P1** | 补充 09 月初流水线失败与修复记录 | 项目管理 | PM/运维 | `05_management/pipeline-records/2026-09-01.md`、`-09-02.md` |
| **P1** | 更新 `Generate-MicroserviceSchemas.ps1` 与 `microservices/sql/README.md` 到 6 服务 | 数据归属 | 数据负责人 | PR 更新 |
| **P2** | README 补充测试账号说明 | 可复现性 | 文档负责人 | `README.md` 补丁 |
| **P2** | 静态 K8s YAML 的 `:deploy` 标签增加说明 | 镜像版本号 | 运维负责人 | `deploy/k8s/README.md` |
| **P2** | 本地全量重跑测试并更新最终测试报告 | 测试报告 | 测试负责人 | `document/测试执行报告-最终.md` |

---

## 四、可直接用于最终验收的最新证据

- **最新成功部署**：GitHub Actions Run `33649461016`（commit `dda33316`）。
- **改造前基线标签**：`monolith-start`。
- **改造后标签**：`microservices-phase1`。
- **微服务 E2E 通过**：`document/测试执行报告-2026-09-01.md` 本地 17/17。
- **故障实验通过**：`04_tests/fault/results/identity-outage-k8s-20260902-222303.json`。
- **服务边界门禁**：`npm run check:microservice-boundaries` 通过。
- **API 覆盖门禁**：`npm run check:microservice-api` 通过，113 端点。

---

## 五、建议下一步动作

1. **今天内**：由组长牵头创建 `01_source~06_defense` 打包目录，把已有文件按课程要求分类复制/软链接进去。
2. **今天内**：启动性能对比实验（单体 + 微服务各 3 轮），同时采集 CPU/内存。
3. **今天内**：补齐 HPA 的 3 次重复实验与 CPU/内存采样。
4. **明天上午**：完成 `技术总结报告.md` 和 `个人权重表`，组织全员确认并保留截图/签名。
5. **明天中午前**：重新跑一遍本地全量测试，生成 `document/测试执行报告-最终.md`。
6. **最终提交前**：对照本清单逐项勾选，确保无 P0 遗留。
