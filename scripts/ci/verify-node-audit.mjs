import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

export function verifyNodeAudit(report) {
  const results = report.Results;
  if (!Array.isArray(results)) throw new Error('Missing Trivy scan results');
  for (const target of ['package-lock.json', 'frontend/package-lock.json']) {
    const result = results.find(item => item.Type === 'npm' && item.Target === target);
    if (!result) throw new Error(`Missing npm scan for ${target}`);
    if ((result.Vulnerabilities || []).some(v => ['HIGH', 'CRITICAL'].includes(v.Severity))) {
      throw new Error(`High/critical vulnerabilities in ${target}`);
    }
  }
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  verifyNodeAudit(JSON.parse(readFileSync(process.argv[2], 'utf8')));
  console.log('Both root and frontend production lockfiles passed Trivy HIGH/CRITICAL scan.');
}
