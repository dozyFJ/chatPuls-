import http from 'node:http';
import { randomBytes, createHash, scrypt as scryptCallback, timingSafeEqual } from 'node:crypto';
import { readFile, rename, writeFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { promisify } from 'node:util';
import { pathToFileURL } from 'node:url';

const scrypt = promisify(scryptCallback);
const usernamePattern = /^[a-z0-9_]{3,32}$/;
const idPattern = /^[a-f0-9-]{36}$/;
const sessionLifetime = 30 * 24 * 60 * 60 * 1000;
const hash = value => createHash('sha256').update(value).digest('hex');
const normalized = value => typeof value === 'string' ? value.trim().toLowerCase() : '';
const validUser = value => usernamePattern.test(value) && !['__proto__', 'constructor', 'prototype'].includes(value);

const adminPattern = /^[a-f0-9]{64}$/;
const day = 24 * 60 * 60 * 1000;
const reportReasons = 300;

/**
 * adminToken — ключ для приложения chatPuls Admin. Если не передан, берётся сохранённый,
 * а при первом запуске создаётся новый и возвращается в server.createdAdminToken.
 */
export async function createServer({ file, adminToken }) {
  let state;
  try { state = JSON.parse(await readFile(file, 'utf8')); }
  catch (error) {
    if (error.code !== 'ENOENT') throw error;
    state = { users: {}, sessions: {}, messages: [], nextSequence: 1 };
  }
  state.banned ??= {};
  state.reports ??= [];
  state.nextReport ??= 1;
  state.removed ??= [];
  state.nextRemoval ??= 1;
  let createdAdminToken = null;
  if (adminToken !== undefined) {
    if (!adminPattern.test(adminToken)) throw new Error('admin token must be 64 hex characters');
    state.adminHash = hash(adminToken);
  } else if (!state.adminHash) {
    createdAdminToken = randomBytes(32).toString('hex');
    state.adminHash = hash(createdAdminToken);
  }
  let writes = Promise.resolve();
  async function change(fn) {
    const job = writes.then(async () => {
      // Commit a copy only after it has been written, so a failed write cannot be acknowledged later.
      const next = structuredClone(state);
      const result = await fn(next);
      await mkdir(dirname(file), { recursive: true });
      await writeFile(file + '.tmp', JSON.stringify(next), { mode: 0o600 });
      await rename(file + '.tmp', file);
      state = next;
      return result;
    });
    writes = job.catch(() => {});
    return job;
  }
  const attempts = new Map();
  function allowed(ip) {
    const now = Date.now();
    const entry = attempts.get(ip);
    if (!entry || now - entry.start >= 60_000) {
      if (attempts.size >= 10_000) {
        for (const [key, value] of attempts) if (now - value.start >= 60_000) attempts.delete(key);
        if (attempts.size >= 10_000) return false;
      }
      attempts.set(ip, { start: now, count: 1 });
      return true;
    }
    return ++entry.count <= 30;
  }
  function reply(res, code, body) {
    res.writeHead(code, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' });
    res.end(JSON.stringify(body));
  }
  async function body(req) {
    let size = 0;
    const chunks = [];
    for await (const chunk of req) {
      size += chunk.length;
      if (size > 16_384) throw new Error('body too large');
      chunks.push(chunk);
    }
    return JSON.parse(Buffer.concat(chunks).toString('utf8'));
  }
  function session(req) {
    const match = /^Bearer ([a-f0-9]{64})$/.exec(req.headers.authorization || '');
    if (!match) return null;
    const info = state.sessions[hash(match[1])];
    return info && info.expires > Date.now() ? info.user : null;
  }
  function isAdmin(req) {
    const value = req.headers['x-pulse-admin'] || '';
    if (!adminPattern.test(value)) return false;
    const expected = Buffer.from(state.adminHash, 'hex');
    const actual = Buffer.from(hash(value), 'hex');
    return expected.length === actual.length && timingSafeEqual(expected, actual);
  }
  async function json(req, res) {
    let input;
    try { input = await body(req); } catch { reply(res, 400, { error: 'invalid request' }); return null; }
    if (!input || typeof input !== 'object' || Array.isArray(input)) { reply(res, 400, { error: 'invalid request' }); return null; }
    return input;
  }
  function removeMessage(next, from, id) {
    const index = next.messages.findIndex(m => m.from === from && m.id === id);
    if (index < 0) return false;
    const [message] = next.messages.splice(index, 1);
    next.removed.push({ rseq: next.nextRemoval++, from, id, to: message.to });
    return true;
  }
  function ban(next, name) {
    next.banned[name] = Date.now();
    for (const [key, value] of Object.entries(next.sessions)) if (value.user === name) delete next.sessions[key];
  }
  async function admin(req, res, url) {
    if (!allowed('admin:' + (req.socket.remoteAddress || 'unknown')) && !isAdmin(req)) return reply(res, 429, { error: 'too many attempts' });
    if (!isAdmin(req)) return reply(res, 401, { error: 'admin key required' });
    const now = Date.now();
    if (req.method === 'GET' && url.pathname === '/v1/admin/stats') {
      const recent = state.messages.filter(m => now - m.time < day);
      return reply(res, 200, {
        users: Object.keys(state.users).length,
        banned: Object.keys(state.banned).length,
        sessions: Object.values(state.sessions).filter(s => s.expires > now).length,
        messages: state.messages.length,
        messagesToday: recent.length,
        activeToday: new Set(recent.map(m => m.from)).size,
        openReports: state.reports.filter(r => r.status === 'open').length,
      });
    }
    if (req.method === 'GET' && url.pathname === '/v1/admin/users') {
      const query = normalized(url.searchParams.get('q') || '');
      const activity = new Map();
      for (const m of state.messages) {
        const entry = activity.get(m.from) || { messages: 0, lastActive: 0 };
        entry.messages++;
        entry.lastActive = Math.max(entry.lastActive, m.time);
        activity.set(m.from, entry);
      }
      const users = Object.keys(state.users).filter(name => name.includes(query)).map(name => ({
        name, banned: Object.hasOwn(state.banned, name), ...(activity.get(name) || { messages: 0, lastActive: 0 }),
      })).sort((a, b) => b.lastActive - a.lastActive || a.name.localeCompare(b.name)).slice(0, 200);
      return reply(res, 200, { users });
    }
    if (req.method === 'POST' && url.pathname === '/v1/admin/users') {
      const input = await json(req, res);
      if (!input) return;
      const name = normalized(input.name);
      if (!Object.hasOwn(state.users, name) || !['ban', 'unban', 'logout'].includes(input.action)) return reply(res, 400, { error: 'invalid request' });
      await change(next => {
        if (input.action === 'ban') ban(next, name);
        else if (input.action === 'unban') delete next.banned[name];
        else for (const [key, value] of Object.entries(next.sessions)) if (value.user === name) delete next.sessions[key];
      });
      return reply(res, 200, { ok: true });
    }
    if (req.method === 'GET' && url.pathname === '/v1/admin/reports') {
      const all = url.searchParams.get('status') === 'all';
      const reports = state.reports.filter(r => all || r.status === 'open').slice(-200).reverse();
      return reply(res, 200, { reports });
    }
    if (req.method === 'POST' && url.pathname === '/v1/admin/reports') {
      const input = await json(req, res);
      if (!input) return;
      if (!['dismiss', 'delete', 'ban', 'delete_ban'].includes(input.action)) return reply(res, 400, { error: 'invalid request' });
      const found = await change(next => {
        const report = next.reports.find(r => r.id === input.id);
        if (!report) return false;
        if (input.action === 'delete' || input.action === 'delete_ban') {
          if (report.messageId) removeMessage(next, report.target, report.messageId);
        }
        if (input.action === 'ban' || input.action === 'delete_ban') ban(next, report.target);
        report.status = input.action === 'dismiss' ? 'dismissed' : 'resolved';
        report.resolved = Date.now();
        return true;
      });
      return found ? reply(res, 200, { ok: true }) : reply(res, 404, { error: 'report not found' });
    }
    return reply(res, 404, { error: 'not found' });
  }
  async function issue(user) {
    const token = randomBytes(32).toString('hex');
    await change(next => {
      for (const [key, value] of Object.entries(next.sessions)) if (value.expires <= Date.now()) delete next.sessions[key];
      next.sessions[hash(token)] = { user, expires: Date.now() + sessionLifetime };
    });
    return token;
  }
  if (createdAdminToken) await change(() => {});
  const server = http.createServer(async (req, res) => {
    try {
      const url = new URL(req.url, 'http://localhost');
      if (req.method === 'GET' && url.pathname === '/health') return reply(res, 200, { ok: true });
      if (url.pathname.startsWith('/v1/admin/')) return await admin(req, res, url);
      if (req.method === 'POST' && ['/v1/register', '/v1/login'].includes(url.pathname)) {
        if (!allowed(req.socket.remoteAddress || 'unknown')) return reply(res, 429, { error: 'too many attempts' });
        let input;
        try { input = await body(req); } catch { return reply(res, 400, { error: 'invalid request' }); }
        if (!input || typeof input !== 'object' || Array.isArray(input)) return reply(res, 400, { error: 'invalid request' });
        const user = normalized(input.username);
        const password = input.password;
        if (!validUser(user) || typeof password !== 'string' || password.length < 10 || password.length > 128)
          return reply(res, 400, { error: 'invalid credentials' });
        if (url.pathname === '/v1/register') {
          const salt = randomBytes(16).toString('hex');
          const derived = (await scrypt(password, salt, 64)).toString('hex');
          const created = await change(next => {
            if (Object.hasOwn(next.users, user)) return false;
            next.users[user] = { salt, password: derived };
            return true;
          });
          if (!created) return reply(res, 409, { error: 'username unavailable' });
        } else {
          if (Object.hasOwn(state.banned, user)) return reply(res, 403, { error: 'account banned' });
          const record = Object.hasOwn(state.users, user) ? state.users[user] : null;
          const derived = await scrypt(password, record?.salt || '00000000000000000000000000000000', 64);
          const expected = record ? Buffer.from(record.password, 'hex') : Buffer.alloc(64);
          if (!record || expected.length !== derived.length || !timingSafeEqual(expected, derived))
            return reply(res, 401, { error: 'invalid credentials' });
        }
        return reply(res, 200, { username: user, token: await issue(user) });
      }
      const user = session(req);
      if (!user) return reply(res, 401, { error: 'authentication required' });
      if (Object.hasOwn(state.banned, user)) return reply(res, 403, { error: 'account banned' });
      if (req.method === 'GET' && url.pathname === '/v1/messages') {
        const afterText = url.searchParams.get('after') || '0';
        const removedText = url.searchParams.get('removed') || '0';
        if (!/^\d{1,15}$/.test(afterText) || !/^\d{1,15}$/.test(removedText)) return reply(res, 400, { error: 'invalid cursor' });
        const after = Number(afterText);
        const removedAfter = Number(removedText);
        const messages = state.messages.filter(m => m.sequence > after && (m.from === user || m.to === user)).slice(0, 100);
        const removed = state.removed.filter(r => r.rseq > removedAfter && (r.from === user || r.to === user)).slice(0, 200)
          .map(({ rseq, from, id }) => ({ rseq, from, id }));
        return reply(res, 200, { messages, removed });
      }
      if (req.method === 'POST' && url.pathname === '/v1/reports') {
        const input = await json(req, res);
        if (!input) return;
        const target = normalized(input.to);
        const reason = typeof input.reason === 'string' ? input.reason.trim() : '';
        const messageId = input.id === undefined || input.id === null ? null : input.id;
        if (!Object.hasOwn(state.users, target) || target === user || !reason || reason.length > reportReasons
            || (messageId !== null && (typeof messageId !== 'string' || !idPattern.test(messageId))))
          return reply(res, 400, { error: 'invalid report' });
        const result = await change(next => {
          if (next.reports.filter(r => r.reporter === user && r.status === 'open').length >= 50) return 429;
          let text = '';
          if (messageId !== null) {
            const message = next.messages.find(m => m.from === target && m.to === user && m.id === messageId);
            if (!message) return 404;
            text = message.text;
          }
          next.reports.push({ id: next.nextReport++, reporter: user, target, messageId, text, reason, time: Date.now(), status: 'open' });
          return 200;
        });
        return result === 200 ? reply(res, 200, { ok: true }) : reply(res, result, { error: 'report rejected' });
      }
      if (req.method === 'POST' && url.pathname === '/v1/messages') {
        let input;
        try { input = await body(req); } catch { return reply(res, 400, { error: 'invalid request' }); }
        if (!input || typeof input !== 'object' || Array.isArray(input)) return reply(res, 400, { error: 'invalid request' });
        const to = normalized(input.to);
        if (!validUser(to) || to === user || typeof input.id !== 'string' || !idPattern.test(input.id)
            || typeof input.text !== 'string' || !input.text.trim() || input.text.length > 2000)
          return reply(res, 400, { error: 'invalid message' });
        const result = await change(next => {
          if (!Object.hasOwn(next.users, to)) return { code: 404, body: { error: 'recipient not found' } };
          if (Object.hasOwn(next.banned, to)) return { code: 404, body: { error: 'recipient not found' } };
          const existing = next.messages.find(m => m.from === user && m.id === input.id);
          if (existing) return existing.to === to && existing.text === input.text
            ? { code: 200, body: existing } : { code: 409, body: { error: 'message id already used' } };
          const message = { sequence: next.nextSequence++, id: input.id, from: user, to, text: input.text, time: Date.now() };
          next.messages.push(message);
          return { code: 200, body: message };
        });
        return reply(res, result.code, result.body);
      }
      return reply(res, 404, { error: 'not found' });
    } catch {
      if (!res.headersSent) reply(res, 500, { error: 'server error' });
      else res.end();
    }
  });
  server.createdAdminToken = createdAdminToken;
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const file = resolve(process.env.PULSE_DATA || 'data.json');
  const port = Number(process.env.PORT || 3000);
  const server = await createServer({ file, adminToken: process.env.PULSE_ADMIN_TOKEN || undefined });
  if (server.createdAdminToken) {
    console.log(`Ключ администратора (сохраните, больше не покажется): ${server.createdAdminToken}`);
  }
  server.listen(port, '127.0.0.1', () => console.log(`Pulse server listening on 127.0.0.1:${port}`));
}
