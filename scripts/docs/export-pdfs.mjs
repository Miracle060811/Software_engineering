// 将 02_docs/ 下关键交付文档导出为 PDF（与 Markdown 源文件同目录）
// 用法：node scripts/docs/export-pdfs.mjs
import { execFileSync } from 'node:child_process';
import { createRequire } from 'node:module';
import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { basename, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = join(scriptDir, '..', '..');
const require = createRequire(join(repoRoot, 'frontend', 'package.json'));
const { chromium } = require('playwright');

const docs = [
  '5组-软件需求规格说明.md',
  '软件概要设计说明书.md',
  '5组-软件详细设计说明.md',
  '测试计划.md',
  '测试报告.md',
  '测试执行报告-2026-09-02.md',
  '需求设计代码测试追溯表.md',
  '业务场景清单与用例说明.md',
];

const headerHtml = `<style>
body { font-family: "Microsoft YaHei", "SimSun", sans-serif; font-size: 11pt; line-height: 1.6; max-width: 100%; margin: 0; }
h1 { font-size: 20pt; border-bottom: 2px solid #333; padding-bottom: 6px; }
h2 { font-size: 15pt; margin-top: 1.4em; }
h3 { font-size: 13pt; }
table { border-collapse: collapse; width: 100%; font-size: 9pt; margin: 1em 0; }
th, td { border: 1px solid #888; padding: 4px 6px; text-align: left; }
th { background: #eee; }
code { font-family: Consolas, monospace; font-size: 9pt; background: #f4f4f4; padding: 0 2px; }
pre { background: #f4f4f4; padding: 8px; font-size: 8.5pt; white-space: pre-wrap; word-break: break-all; }
img { max-width: 100%; }
blockquote { border-left: 3px solid #999; padding-left: 10px; color: #444; }
</style>`;

const docDir = join(repoRoot, '02_docs');
const tmpDir = join(docDir, '.pdf-tmp');
mkdirSync(tmpDir, { recursive: true });
const headerPath = join(tmpDir, 'header.html');
writeFileSync(headerPath, headerHtml, 'utf8');

const browser = await chromium.launch();
const page = await browser.newPage();

for (const doc of docs) {
  const mdPath = join(docDir, doc);
  const htmlPath = join(tmpDir, basename(doc, '.md') + '.html');
  const html = execFileSync('pandoc', ['-f', 'gfm', '-t', 'html5', '-s', '--embed-resources', `--resource-path=${docDir}`, '-H', headerPath, mdPath], {
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
  });
  writeFileSync(htmlPath, html, 'utf8');
  const pdfPath = join(docDir, basename(doc, '.md') + '.pdf');
  await page.goto('file:///' + htmlPath.replace(/\\/g, '/'), { waitUntil: 'networkidle' });
  await page.pdf({
    path: pdfPath,
    format: 'A4',
    margin: { top: '18mm', bottom: '18mm', left: '14mm', right: '14mm' },
    printBackground: true,
  });
  console.log('OK', basename(doc, '.md') + '.pdf');
}

await browser.close();
rmSync(tmpDir, { recursive: true, force: true });
