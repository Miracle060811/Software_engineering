import test from 'node:test';
import assert from 'node:assert/strict';
import { auditWithRetry } from './audit-npm.mjs';

const unavailable = { status: 1, stdout: '{"error":{"code":"E503"}}', stderr: '503 Service Unavailable' };
const clean = { status: 0, stdout: '{"metadata":{"vulnerabilities":{"high":0,"critical":0}}}' };

async function scenario(results) {
  let calls = 0;
  const waits = [];
  const status = await auditWithRetry({
    run: () => results[Math.min(calls++, results.length - 1)],
    sleep: async ms => waits.push(ms), log: () => {},
  });
  return { status, calls, waits };
}

test('503 retries then requires an actual successful audit', async () => {
  assert.deepEqual(await scenario([unavailable, clean]), { status: 0, calls: 2, waits: [10000] });
});
test('persistent registry outage still fails the security gate', async () => {
  assert.deepEqual(await scenario([unavailable]), { status: 75, calls: 3, waits: [10000, 20000] });
});
test('high vulnerability is not retried or bypassed', async () => {
  const result = { status: 1, stdout: '{"metadata":{"vulnerabilities":{"high":1}},"vulnerabilities":{"pkg":{}}}', stderr: 'E503' };
  assert.deepEqual(await scenario([result]), { status: 1, calls: 1, waits: [] });
});
test('authentication and lockfile errors fail immediately', async () => {
  for (const code of ['E401', 'ENOLOCK', 'EUSAGE']) {
    assert.deepEqual(await scenario([{ status: 1, stdout: JSON.stringify({ error: { code } }) }]),
      { status: 1, calls: 1, waits: [] });
  }
});
test('network timeout retries but malformed output fails closed', async () => {
  assert.equal((await scenario([{ status: null, error: { code: 'ETIMEDOUT' } }, clean])).status, 0);
  assert.deepEqual(await scenario([{ status: 1, stdout: 'not JSON', stderr: 'E503' }]),
    { status: 1, calls: 1, waits: [] });
});
test('npm audit timeout without error code retries', async () => {
  const result = { status: 1, stdout: JSON.stringify({ message: 'network timeout at: https://registry.npmjs.org/-/npm/v1/security/advisories/bulk', error: { summary: '', detail: '' } }) };
  assert.deepEqual(await scenario([result, clean]), { status: 0, calls: 2, waits: [10000] });
});
