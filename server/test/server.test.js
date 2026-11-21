import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, rm, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { createServer } from '../server.js';
import { randomUUID } from 'node:crypto';

async function fixture() {
  const directory = await mkdtemp(join(tmpdir(), 'pulse-test-'));
  const file = join(directory, 'data.json');
  let server;
  async function start() {
    server = await createServer({ file });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    return `http://127.0.0.1:${server.address().port}`;
  }
  async function stop() { await new Promise((resolve, reject) => server.close(error => error ? reject(error) : resolve())); }
  return { file, start, stop, async cleanup() { await rm(directory, { recursive: true, force: true }); } };
}
async function request(base, route, { method = 'GET', token, data } = {}) {
  const response = await fetch(base + route, { method, headers: { ...(token && { Authorization: `Bearer ${token}` }), ...(data !== undefined && { 'Content-Type': 'application/json' }) }, ...(data !== undefined && { body: JSON.stringify(data) }) });
  return { status: response.status, body: await response.json() };
}

test('accounts, private messages, idempotent retry and restart', async () => {
  const f = await fixture();
  let base = await f.start();
  try {
    const register = username => request(base, '/v1/register', { method: 'POST', data: { username, password: 'password-123' } });
    const alice = (await register('Alice')).body.token;
    const bob = (await register('bob')).body.token;
    const carol = (await register('carol')).body.token;
    assert.ok(alice && bob && carol);
    assert.equal((await register('ALICE')).status, 409);
    assert.equal((await request(base, '/v1/login', { method: 'POST', data: { username: 'alice', password: 'wrong-password' } })).status, 401);
    assert.equal((await request(base, '/v1/messages')).status, 401);
    const message = { id: randomUUID(), to: 'bob', text: 'Привет!' };
    const send = () => request(base, '/v1/messages', { method: 'POST', token: alice, data: message });
    const first = await send();
    assert.equal(first.status, 200);
    assert.deepEqual((await send()).body, first.body);
    assert.equal((await request(base, '/v1/messages', { method: 'POST', token: alice, data: { ...message, text: 'другое' } })).status, 409);
    assert.equal((await request(base, '/v1/messages', { method: 'POST', token: alice, data: { id: randomUUID(), to: 'nobody', text: 'test' } })).status, 404);
    assert.deepEqual((await request(base, '/v1/messages', { token: bob })).body.messages.map(m => m.text), ['Привет!']);
    assert.deepEqual((await request(base, '/v1/messages', { token: carol })).body.messages, []);
    assert.deepEqual((await request(base, '/v1/messages?after=1', { token: bob })).body.messages, []);
    assert.equal((await request(base, '/v1/messages?after=-1', { token: bob })).status, 400);
    const persisted = await readFile(f.file, 'utf8');
    assert.ok(!persisted.includes('password-123'));
    assert.ok(!persisted.includes(alice));
    await f.stop();
    base = await f.start();
    assert.equal((await request(base, '/v1/messages', { token: bob })).body.messages.length, 1);
    assert.equal((await request(base, '/v1/login', { method: 'POST', data: { username: 'alice', password: 'password-123' } })).status, 200);
  } finally { await f.stop(); await f.cleanup(); }
});

test('simultaneous registration and retries do not duplicate records', async () => {
  const f = await fixture();
  const base = await f.start();
  try {
    const data = { username: 'same', password: 'password-123' };
    const results = await Promise.all([1, 2].map(() => request(base, '/v1/register', { method: 'POST', data })));
    assert.deepEqual(results.map(r => r.status).sort(), [200, 409]);
    const token = results.find(r => r.status === 200).body.token;
    await request(base, '/v1/register', { method: 'POST', data: { username: 'other', password: 'password-123' } });
    const message = { id: randomUUID(), to: 'other', text: 'hello' };
    const sends = await Promise.all([1, 2].map(() => request(base, '/v1/messages', { method: 'POST', token, data: message })));
    assert.equal(sends[0].body.sequence, sends[1].body.sequence);
    assert.equal((await request(base, '/v1/messages', { token })).body.messages.length, 1);
    assert.equal((await request(base, '/v1/register', { method: 'POST', data: null })).status, 400);
  } finally { await f.stop(); await f.cleanup(); }
});
