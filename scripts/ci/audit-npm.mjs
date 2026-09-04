import { spawnSync } from 'node:child_process';
import { setTimeout as delay } from 'node:timers/promises';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

export function isTransientAuditFailure(result) {
  let report;
  try { report = JSON.parse(result.stdout || '{}'); } catch { return false; }
  const counts = report.metadata?.vulnerabilities;
  if (counts?.high > 0 || counts?.critical > 0 || report.vulnerabilities) return false;
  const code = report.error?.code || result.error?.code || '';
  const details = `${code}\n${report.message || ''}\n${result.stderr || ''}`;
  return /\b(?:E(?:429|500|502|503|504)|ETIMEDOUT|ECONNRESET|EAI_AGAIN|ECONNREFUSED)\b/.test(details)
    || /\b(?:429|500|502|503|504)\s+(?:Too Many Requests|Internal Server Error|Bad Gateway|Service Unavailable|Gateway Timeout)\b/i.test(details)
    || /network timeout at: https:\/\/registry\.npmjs\.org\//i.test(details);
}

export async function auditWithRetry({ cwd, run = runNpm, sleep = delay, log = console.log }) {
  for (let attempt = 1; attempt <= 3; attempt++) {
    log(`npm production audit: attempt ${attempt}/3`);
    const result = run(cwd);
    if (result.stdout) log(result.stdout);
    if (result.stderr) log(result.stderr);
    if (result.error) log(`npm audit process error: ${result.error.code || result.error.message}`);
    if (result.status === 0 && !result.error) return 0;
    if (!isTransientAuditFailure(result) || attempt === 3) return result.status || 1;
    log(`Temporary npm registry failure; retrying in ${attempt * 10} seconds.`);
    await sleep(attempt * 10_000);
  }
  return 1;
}

function runNpm(cwd) {
  return spawnSync(process.platform === 'win32' ? 'npm.cmd' : 'npm', [
    'audit', '--package-lock-only', '--omit=dev', '--audit-level=high', '--json',
    '--fetch-retries=0', '--fetch-timeout=30000',
  ], {
    cwd, encoding: 'utf8', timeout: 75_000, maxBuffer: 8 * 1024 * 1024,
    shell: process.platform === 'win32', windowsHide: true,
  });
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  process.exitCode = await auditWithRetry({ cwd: resolve(process.argv[2] || '.') });
}
