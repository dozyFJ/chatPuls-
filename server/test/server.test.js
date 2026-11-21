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

test('reports, moderation, bans and removal sync', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'pulse-admin-'));
  const adminToken = 'a'.repeat(64);
  const server = await createServer({ file: join(directory, 'data.json'), adminToken });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  const admin = async (route, data) => {
    const response = await fetch(base + route, { method: data ? 'POST' : 'GET', headers: { 'X-Pulse-Admin': adminToken, ...(data && { 'Content-Type': 'application/json' }) }, ...(data && { body: JSON.stringify(data) }) });
    return { status: response.status, body: await response.json() };
  };
  try {
    const register = async username => (await request(base, '/v1/register', { method: 'POST', data: { username, password: 'password-123' } })).body.token;
    const alice = await register('alice');
    const bob = await register('bob');
    const spam = { id: randomUUID(), to: 'alice', text: 'купи слона' };
    await request(base, '/v1/messages', { method: 'POST', token: bob, data: spam });

    assert.equal((await fetch(base + '/v1/admin/stats')).status, 401);
    assert.equal((await fetch(base + '/v1/admin/stats', { headers: { 'X-Pulse-Admin': 'b'.repeat(64) } })).status, 401);
    const stats = (await admin('/v1/admin/stats')).body;
    assert.equal(stats.users, 2);
    assert.equal(stats.messages, 1);
    assert.equal(stats.messagesToday, 1);

    assert.equal((await request(base, '/v1/reports', { method: 'POST', token: alice, data: { to: 'bob', id: randomUUID(), reason: 'спам' } })).status, 404);
    assert.equal((await request(base, '/v1/reports', { method: 'POST', token: alice, data: { to: 'bob', id: spam.id, reason: '' } })).status, 400);
    assert.equal((await request(base, '/v1/reports', { method: 'POST', token: alice, data: { to: 'bob', id: spam.id, reason: 'спам' } })).status, 200);
    const reports = (await admin('/v1/admin/reports')).body.reports;
    assert.equal(reports.length, 1);
    assert.equal(reports[0].text, 'купи слона');
    assert.equal(reports[0].reporter, 'alice');

    assert.equal((await admin('/v1/admin/reports', { id: reports[0].id, action: 'delete_ban' })).status, 200);
    assert.equal((await admin('/v1/admin/reports')).body.reports.length, 0);
    const synced = (await request(base, '/v1/messages', { token: alice })).body;
    assert.deepEqual(synced.messages, []);
    assert.deepEqual(synced.removed.map(r => r.id), [spam.id]);
    assert.deepEqual((await request(base, `/v1/messages?removed=${synced.removed[0].rseq}`, { token: alice })).body.removed, []);

    assert.equal((await request(base, '/v1/messages', { token: bob })).status, 401);
    assert.equal((await request(base, '/v1/login', { method: 'POST', data: { username: 'bob', password: 'password-123' } })).status, 403);
    const users = (await admin('/v1/admin/users?q=bo')).body.users;
    assert.deepEqual(users.map(u => [u.name, u.banned]), [['bob', true]]);
    assert.equal((await admin('/v1/admin/users', { name: 'bob', action: 'unban' })).status, 200);
    assert.equal((await request(base, '/v1/login', { method: 'POST', data: { username: 'bob', password: 'password-123' } })).status, 200);
    assert.equal((await admin('/v1/admin/stats')).body.banned, 0);
  } finally {
    await new Promise(resolve => server.close(resolve));
    await rm(directory, { recursive: true, force: true });
  }
});

test('first start creates an admin key once', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'pulse-key-'));
  const file = join(directory, 'data.json');
  try {
    const first = await createServer({ file });
    assert.match(first.createdAdminToken, /^[a-f0-9]{64}$/);
    const second = await createServer({ file });
    assert.equal(second.createdAdminToken, null);
    assert.ok(!(await readFile(file, 'utf8')).includes(first.createdAdminToken));
  } finally { await rm(directory, { recursive: true, force: true }); }
});
