import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const expectedIds = Array.from({ length: 19 }, (_, index) => `UC${String(index + 1).padStart(2, "0")}`);
const listPath = path.join(root, "document", "业务场景清单与用例说明.md");
const designPath = path.join(root, "document", "5组-软件详细设计说明.md");
const diagramPath = path.join(root, "document", "详细设计说明");
const diagramSourcePath = path.join(diagramPath, "source");
const matrixPath = path.join(root, "docs", "ci", "use-case-test-matrix.json");
const reportPath = path.resolve(root, process.env.TRACEABILITY_REPORT || "04_tests/reports/ci/use-case-traceability.md");
const errors = [];

for (const requiredPath of [listPath, designPath, diagramPath, diagramSourcePath, matrixPath]) {
  if (!fs.existsSync(requiredPath)) errors.push(`缺少文件或目录：${path.relative(root, requiredPath)}`);
}

if (errors.length) {
  console.error(errors.join("\n"));
  process.exit(1);
}

const listText = fs.readFileSync(listPath, "utf8");
const designText = fs.readFileSync(designPath, "utf8");
const matrix = JSON.parse(fs.readFileSync(matrixPath, "utf8"));
const diagrams = fs.readdirSync(diagramPath);
const sources = fs.readdirSync(diagramSourcePath);
const allowedStatuses = new Set(["covered", "partial", "planned"]);
const rows = [];

for (const id of expectedIds) {
  const entry = matrix[id];
  const hasList = listText.includes(`| ${id} |`);
  const hasDesign = designText.includes(id);
  const hasDiagram = diagrams.some((name) => name.includes(`${id}_`) && name.endsWith(".png"));
  const hasSource = sources.some((name) => name.includes(`${id}_`) && name.endsWith(".mmd"));

  if (!hasList) errors.push(`${id} 未出现在业务场景清单中`);
  if (!hasDesign) errors.push(`${id} 未出现在详细设计说明中`);
  if (!hasDiagram) errors.push(`${id} 缺少对象级顺序图 PNG`);
  if (!hasSource) errors.push(`${id} 缺少对象级顺序图 Mermaid 源文件`);
  if (!entry) {
    errors.push(`${id} 缺少测试追溯矩阵条目`);
    continue;
  }
  if (!allowedStatuses.has(entry.status)) errors.push(`${id} 的测试状态无效：${entry.status}`);
  if (!Array.isArray(entry.tests)) errors.push(`${id} 的 tests 必须是数组`);
  if (entry.status !== "planned" && (!Array.isArray(entry.tests) || entry.tests.length === 0)) {
    errors.push(`${id} 标记为 ${entry.status}，但没有测试证据`);
  }

  const missingTests = (entry.tests || []).filter((testPath) => !fs.existsSync(path.join(root, testPath)));
  for (const testPath of missingTests) errors.push(`${id} 引用了不存在的测试：${testPath}`);

  rows.push({ id, status: entry.status, tests: entry.tests || [], hasList, hasDesign, hasDiagram, hasSource });
}

for (const id of Object.keys(matrix)) {
  if (!expectedIds.includes(id)) errors.push(`测试追溯矩阵包含未知用例：${id}`);
}

const count = (status) => rows.filter((row) => row.status === status).length;
const report = [
  "# CI 用例追溯检查报告",
  "",
  `- Commit: ${process.env.GITHUB_SHA || "local"}`,
  `- 完整覆盖：${count("covered")}`,
  `- 部分覆盖：${count("partial")}`,
  `- 尚待测试：${count("planned")}`,
  `- 结构错误：${errors.length}`,
  "",
  "| 用例 | 测试状态 | 测试证据数 | 清单 | 详细设计 | PNG | Mermaid 源文件 |",
  "| --- | --- | ---: | --- | --- | --- | --- |",
  ...rows.map((row) => `| ${row.id} | ${row.status} | ${row.tests.length} | ${row.hasList ? "是" : "否"} | ${row.hasDesign ? "是" : "否"} | ${row.hasDiagram ? "是" : "否"} | ${row.hasSource ? "是" : "否"} |`),
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
