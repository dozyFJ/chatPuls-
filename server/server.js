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

export async function createServer({ file }) {
  let state;
  try { state = JSON.parse(await readFile(file, 'utf8')); }
  catch (error) {
    if (error.code !== 'ENOENT') throw error;
    state = { users: {}, sessions: {}, messages: [], nextSequence: 1 };
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
  async function issue(user) {
    const token = randomBytes(32).toString('hex');
    await change(next => {
      for (const [key, value] of Object.entries(next.sessions)) if (value.expires <= Date.now()) delete next.sessions[key];
      next.sessions[hash(token)] = { user, expires: Date.now() + sessionLifetime };
    });
    return token;
  }
  return http.createServer(async (req, res) => {
    try {
      const url = new URL(req.url, 'http://localhost');
      if (req.method === 'GET' && url.pathname === '/health') return reply(res, 200, { ok: true });
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
      if (req.method === 'GET' && url.pathname === '/v1/messages') {
        const afterText = url.searchParams.get('after') || '0';
        if (!/^\d{1,15}$/.test(afterText)) return reply(res, 400, { error: 'invalid cursor' });
        const after = Number(afterText);
        const messages = state.messages.filter(m => m.sequence > after && (m.from === user || m.to === user)).slice(0, 100);
        return reply(res, 200, { messages });
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
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const file = resolve(process.env.PULSE_DATA || 'data.json');
  const port = Number(process.env.PORT || 3000);
  const server = await createServer({ file });
  server.listen(port, '127.0.0.1', () => console.log(`Pulse server listening on 127.0.0.1:${port}`));
}
