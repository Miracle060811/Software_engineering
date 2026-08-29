import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const expectedIds = Array.from({ length: 19 }, (_, index) => `UC${String(index + 1).padStart(2, "0")}`);
const listPath = path.join(root, "document", "业务场景清单与用例说明.md");
const designPath = path.join(root, "document", "5组-软件详细设计说明.md");
const requirementsPath = path.join(root, "document", "5组-软件需求规格说明.md");
const overviewPath = path.join(root, "document", "软件概要设计说明书.md");
const traceabilityPath = path.join(root, "document", "需求设计代码测试追溯表.md");
const sysDiagramPath = path.join(root, "document", "需求规格说明");
const sysSourcePath = path.join(sysDiagramPath, "source");
const compDiagramPath = path.join(root, "document", "概要设计说明");
const compSourcePath = path.join(compDiagramPath, "source");
const diagramPath = path.join(root, "document", "详细设计说明");
const diagramSourcePath = path.join(diagramPath, "source");
const matrixPath = path.join(root, "docs", "ci", "use-case-test-matrix.json");
const policyPath = path.join(root, "docs", "ci", "test-quality-policy.json");
const reportPath = path.resolve(root, process.env.TRACEABILITY_REPORT || "04_tests/reports/ci/use-case-traceability.md");
const errors = [];

for (const requiredPath of [listPath, requirementsPath, overviewPath, designPath, traceabilityPath, sysDiagramPath, sysSourcePath, compDiagramPath, compSourcePath, diagramPath, diagramSourcePath, matrixPath, policyPath]) {
  if (!fs.existsSync(requiredPath)) errors.push(`缺少文件或目录：${path.relative(root, requiredPath)}`);
}

if (errors.length) {
  console.error(errors.join("\n"));
  process.exit(1);
}

const listText = fs.readFileSync(listPath, "utf8");
const requirementsText = fs.readFileSync(requirementsPath, "utf8");
const overviewText = fs.readFileSync(overviewPath, "utf8");
const designText = fs.readFileSync(designPath, "utf8");
const traceabilityText = fs.readFileSync(traceabilityPath, "utf8");
const matrix = JSON.parse(fs.readFileSync(matrixPath, "utf8"));
const policy = JSON.parse(fs.readFileSync(policyPath, "utf8"));
const diagrams = fs.readdirSync(diagramPath);
const sources = fs.readdirSync(diagramSourcePath);
const sysDiagrams = fs.readdirSync(sysDiagramPath);
const sysSources = fs.readdirSync(sysSourcePath);
const compDiagrams = fs.readdirSync(compDiagramPath);
const compSources = fs.readdirSync(compSourcePath);
const allowedStatuses = new Set(["covered", "partial", "planned"]);
const rows = [];
const sourceCache = new Map();

function expectedTestCaseIds(id) {
  const numericId = 100 + Number(id.slice(2));
  return [`UNIT-TC-${numericId}`, `INT-TC-${numericId}`, `E2E-TC-${numericId}`];
}

function readSource(relativePath) {
  if (!sourceCache.has(relativePath)) {
    sourceCache.set(relativePath, fs.readFileSync(path.join(root, relativePath), "utf8"));
  }
  return sourceCache.get(relativePath);
}

for (const id of expectedIds) {
  const entry = matrix[id];
  const hasList = listText.includes(`| ${id} |`);
  const hasDesign = designText.includes(id);
  const hasSysDiagram = sysDiagrams.some((name) => name.startsWith(`SYS-${id}_`) && name.endsWith(".png"));
  const hasSysSource = sysSources.some((name) => name.startsWith(`SYS-${id}_`) && name.endsWith(".mmd"));
  const hasCompDiagram = compDiagrams.some((name) => name.startsWith(`COMP-${id}_`) && name.endsWith(".png"));
  const hasCompSource = compSources.some((name) => name.startsWith(`COMP-${id}_`) && name.endsWith(".mmd"));
  const hasDiagram = diagrams.some((name) => name.includes(`${id}_`) && name.endsWith(".png"));
  const hasSource = sources.some((name) => name.includes(`${id}_`) && name.endsWith(".mmd"));

  if (!hasList) errors.push(`${id} 未出现在业务场景清单中`);
  if (!hasDesign) errors.push(`${id} 未出现在详细设计说明中`);
  if (!requirementsText.includes(`SYS-${id}`)) errors.push(`SYS-${id} 未在需求规格说明中引用`);
  if (!overviewText.includes(`COMP-${id}`)) errors.push(`COMP-${id} 未在概要设计说明中引用`);
  if (!hasSysDiagram) errors.push(`SYS-${id} 缺少系统级 PNG`);
  if (!hasSysSource) errors.push(`SYS-${id} 缺少系统级 Mermaid 源文件`);
  if (!hasCompDiagram) errors.push(`COMP-${id} 缺少组件级 PNG`);
  if (!hasCompSource) errors.push(`COMP-${id} 缺少组件级 Mermaid 源文件`);
  if (!hasDiagram) errors.push(`${id} 缺少对象级顺序图 PNG`);
  if (!hasSource) errors.push(`${id} 缺少对象级顺序图 Mermaid 源文件`);
  const requirementId = `REQ-${id.slice(2)}`;
  const traceabilityRow = traceabilityText.split(/\r?\n/).find((line) => line.startsWith(`| ${requirementId} |`));
  if (!traceabilityRow) {
    errors.push(`${id} 缺少统一追溯表主表行`);
  } else {
    const cells = traceabilityRow.split("|").slice(1, -1).map((cell) => cell.trim());
    const actualResult = cells.at(-1) || "";
    if (actualResult === "通过" || actualResult.length < 20) {
      errors.push(`${id} 的主表测试结果过于笼统，必须记录实际验证内容与证据位置`);
    }
    if (!actualResult.includes("§3.2") && !actualResult.includes("矩阵锚点")) {
      errors.push(`${id} 的主表测试结果缺少 §3.2 或矩阵锚点引用`);
    }
  }
  if (!entry) {
    errors.push(`${id} 缺少测试追溯矩阵条目`);
    continue;
  }
  if (!allowedStatuses.has(entry.status)) errors.push(`${id} 的测试状态无效：${entry.status}`);
  if (!Array.isArray(entry.tests)) errors.push(`${id} 的 tests 必须是数组`);
  if (entry.status !== "planned" && (!Array.isArray(entry.tests) || entry.tests.length === 0)) {
    errors.push(`${id} 标记为 ${entry.status}，但没有测试证据`);
  }

  const tests = entry.tests || [];
  const hasBackendTest = tests.some((t) => /^backend\/src\/test\/.*\.java$/.test(t));
  const hasE2ETest = tests.some((t) => /^frontend\/tests\/e2e-real\/.*\.(js|ts)$/.test(t));

  if (entry.status === "covered" && policy.coveredRequiresBothLayers !== false) {
    if (!hasBackendTest) {
      errors.push(`${id} 标记为 covered，但缺少后端 UNIT/INT 测试证据`);
    }
    if (!hasE2ETest) {
      errors.push(`${id} 标记为 covered，但缺少真实 E2E 测试证据`);
    }
  }

  const missingTests = tests.filter((testPath) => !fs.existsSync(path.join(root, testPath)));
  for (const testPath of missingTests) errors.push(`${id} 引用了不存在的测试：${testPath}`);

  const expectedCaseIds = expectedTestCaseIds(id);
  const expectedAnchors = {
    unit: `unitTc${expectedCaseIds[0].slice(-3)}*`,
    integration: `intTc${expectedCaseIds[1].slice(-3)}*`,
    e2e: `[${expectedCaseIds[2]}]`,
  };
  const testCaseAnchors = [];
  if (!Array.isArray(entry.testIds) || entry.testIds.length !== expectedCaseIds.length || expectedCaseIds.some((testCaseId) => !entry.testIds.includes(testCaseId))) {
    errors.push(`${id} 的 testIds 必须且只能包含：${expectedCaseIds.join("、")}`);
  }
  if (!entry.anchors || typeof entry.anchors !== "object" || Array.isArray(entry.anchors)) {
    errors.push(`${id} 缺少 UNIT/INT/E2E 编号到代码符号的 anchors 映射`);
  } else {
    for (const [layer, expectedAnchor] of Object.entries(expectedAnchors)) {
      if (entry.anchors[layer] !== expectedAnchor) {
        errors.push(`${id} 的 ${layer} 锚点应为 ${expectedAnchor}，实际为 ${entry.anchors[layer] || "缺失"}`);
        continue;
      }

      const testCaseId = expectedCaseIds[layer === "unit" ? 0 : layer === "integration" ? 1 : 2];
      const sourceAnchor = expectedAnchor.endsWith("*") ? expectedAnchor.slice(0, -1) : expectedAnchor;
      const candidatePaths = tests.filter((testPath) => layer === "e2e"
        ? /^frontend\/tests\/e2e-real\/.*\.(js|ts)$/.test(testPath)
        : /^backend\/src\/test\/.*\.java$/.test(testPath));
      const matches = [];
      for (const testPath of candidatePaths) {
        if (!fs.existsSync(path.join(root, testPath))) continue;
        const sourceLines = readSource(testPath).split(/\r?\n/);
        for (const [index, line] of sourceLines.entries()) {
          if (line.toLowerCase().includes(sourceAnchor.toLowerCase())) {
            matches.push(`${testCaseId}: ${testPath}:${index + 1} (${line.trim()})`);
          }
        }
      }
      if (matches.length === 0) {
        errors.push(`${id} 的 ${testCaseId} 在已登记测试文件中找不到锚点：${expectedAnchor}`);
      } else {
        testCaseAnchors.push(...matches);
      }
    }
  }

  rows.push({ id, status: entry.status, tests: entry.tests || [], testCaseAnchors, hasList, hasDesign, hasSysDiagram, hasSysSource, hasCompDiagram, hasCompSource, hasDiagram, hasSource });
}

for (const id of Object.keys(matrix)) {
  if (!expectedIds.includes(id)) errors.push(`测试追溯矩阵包含未知用例：${id}`);
}

const count = (status) => rows.filter((row) => row.status === status).length;
const evidenceScore = count("covered") * 2 + count("partial");

if (!Number.isInteger(policy.maximumPlanned) || policy.maximumPlanned < 0) {
  errors.push("test-quality-policy.json 的 maximumPlanned 必须是非负整数");
}
if (!Number.isInteger(policy.minimumEvidenceScore) || policy.minimumEvidenceScore < 0) {
  errors.push("test-quality-policy.json 的 minimumEvidenceScore 必须是非负整数");
}
if (count("planned") > policy.maximumPlanned) {
  errors.push(`尚待测试用例增加：${count("planned")} > ${policy.maximumPlanned}`);
}
if (evidenceScore < policy.minimumEvidenceScore) {
  errors.push(`自动化证据分下降：${evidenceScore} < ${policy.minimumEvidenceScore}`);
}

const report = [
  "# CI 用例追溯检查报告",
  "",
  `- Commit: ${process.env.GITHUB_SHA || "local"}`,
  `- 完整覆盖：${count("covered")}`,
  `- 部分覆盖：${count("partial")}`,
  `- 尚待测试：${count("planned")}`,
  `- 自动化证据分：${evidenceScore}（门槛：${policy.minimumEvidenceScore}）`,
  `- 尚待测试上限：${policy.maximumPlanned}`,
  `- 结构错误：${errors.length}`,
  "",
  "| 用例 | 测试状态 | 证据数 | SYS PNG/源 | COMP PNG/源 | OBJ PNG/源 | 清单/详细设计 |",
  "| --- | --- | ---: | --- | --- | --- | --- |",
  ...rows.map((row) => `| ${row.id} | ${row.status} | ${row.tests.length} | ${row.hasSysDiagram && row.hasSysSource ? "是" : "否"} | ${row.hasCompDiagram && row.hasCompSource ? "是" : "否"} | ${row.hasDiagram && row.hasSource ? "是" : "否"} | ${row.hasList && row.hasDesign ? "是" : "否"} |`),
  "",
  "## 测试编号锚点",
  "",
  "| 用例 | UNIT / INT / E2E 代码锚点 |",
  "| --- | --- |",
  ...rows.map((row) => `| ${row.id} | ${row.testCaseAnchors.join("<br>")} |`),
  "",
  "## 当前缺口",
  "",
  ...rows.filter((row) => row.status !== "covered").map((row) => `- ${row.id}: ${row.status === "partial" ? "只有部分自动化证据" : "尚无自动化测试证据"}`),
  "",
  "## 结构错误",
  "",
  ...(errors.length ? errors.map((error) => `- ${error}`) : ["- 无"]),
  "",
].join("\n");

fs.mkdirSync(path.dirname(reportPath), { recursive: true });
fs.writeFileSync(reportPath, report, "utf8");
console.log(report);

if (errors.length) process.exit(1);
