import test from 'node:test';
import assert from 'node:assert/strict';
import { verifyNodeAudit } from './verify-node-audit.mjs';

const clean = () => ({ Results: ['package-lock.json', 'frontend/package-lock.json'].map(Target => ({ Type: 'npm', Target, Vulnerabilities: [] })) });
test('requires successful coverage of both production lockfiles', () => {
  assert.doesNotThrow(() => verifyNodeAudit(clean()));
  assert.throws(() => verifyNodeAudit({ Results: [clean().Results[0]] }), /frontend/);
  assert.throws(() => verifyNodeAudit({ Results: [] }), /Missing/);
  assert.throws(() => verifyNodeAudit({}), /Missing/);
});
test('high and critical vulnerabilities fail even if a report exists', () => {
  for (const Severity of ['HIGH', 'CRITICAL']) {
    const report = clean();
    report.Results[1].Vulnerabilities.push({ Severity });
    assert.throws(() => verifyNodeAudit(report), /vulnerabilities/);
  }
});
