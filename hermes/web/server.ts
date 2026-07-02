import { existsSync } from 'node:fs';
import { readFile } from 'node:fs/promises';
import { join, normalize } from 'node:path';
import postgres from 'postgres';

const port = Number(process.env.PORT || 3000);
const distDir = process.env.HERMES_WEB_DIST || join(import.meta.dir, 'dist');
type ControlServer = {
  id: string;
  label: string;
  namespace: string;
  statefulSet: string;
  podLabelSelector: string;
  container: string;
  pluginUrl?: string;
};

const controlServers = await loadControlServers();
const controlServerById = new Map(controlServers.map((target) => [target.id, target]));
const pluginUrl = trimTrailingSlash(process.env.HERMES_PLUGIN_URL || controlServers.find((target) => target.pluginUrl)?.pluginUrl || '');
const hecateSessionSecret = process.env.HERMES_HECATE_SESSION_SECRET || process.env.HECATE_WEB_SESSION_SECRET || process.env.HERMES_WEB_SESSION_SECRET || 'dev-only-change-me';
const webSessionSeconds = Math.max(3600, Number(process.env.HERMES_WEB_SESSION_SECONDS || process.env.HERMES_CONTROL_SESSION_SECONDS || 60 * 60 * 24 * 30));
const databaseUrl = process.env.HERMES_POSTGRES_URL || process.env.DATABASE_URL || '';
const db = databaseUrl ? postgres(databaseUrl, { max: 4 }) : postgres({ max: 4 });
const serviceHost = process.env.HERMES_KUBE_HOST || 'kubernetes.default.svc';
const servicePort = process.env.KUBERNETES_SERVICE_PORT || '443';
const kubeApi = process.env.HERMES_KUBE_API || `https://${serviceHost}:${servicePort}`;
const serviceAccountTokenPath = process.env.HERMES_KUBE_TOKEN_PATH || '/var/run/secrets/kubernetes.io/serviceaccount/token';
const serviceAccountToken = await readOptional(serviceAccountTokenPath);
const serviceAccountCaPath = process.env.HERMES_KUBE_CA_PATH || '/var/run/secrets/kubernetes.io/serviceaccount/ca.crt';
const serviceAccountCa = await readOptional(serviceAccountCaPath);
const kubeTls = process.env.HERMES_KUBE_INSECURE_SKIP_TLS_VERIFY === 'true'
  ? { rejectUnauthorized: false }
  : serviceAccountCa
    ? { ca: serviceAccountCa }
    : undefined;
const kubeInsecureSkipTlsVerify = process.env.HERMES_KUBE_INSECURE_SKIP_TLS_VERIFY === 'true';
if (kubeInsecureSkipTlsVerify) {
  process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0';
}
const kubectlPath = process.env.HERMES_KUBECTL_PATH || 'kubectl';

const server = Bun.serve<{ pod?: string; target?: ControlServer; kubectl?: any; kubectlStdin?: any }>({
  port,
  async fetch(request, server) {
    try {
      const url = new URL(request.url);
      if (url.pathname === '/api/me' || url.pathname === '/api/auth/login' || url.pathname === '/api/auth/logout') {
        return await handleWebAuth(request, url);
      }
      const consoleRoute = matchControlServerRoute(url.pathname, '/console');
      if (consoleRoute) {
        const auth = await authorizeControl(request);
        if (!auth.authorized) {
          return json({ error: auth.error || 'Controller authorization required' }, 401);
        }
        const target = getControlServer(consoleRoute.serverId);
        if (!target) return json({ error: `Unknown server "${consoleRoute.serverId}"` }, 404);
        const pod = await currentPodName(target);
        if (!pod) return json({ error: 'Minecraft pod is not running' }, 409);
        if (server.upgrade(request, { data: { pod, target } })) return undefined;
        return json({ error: 'WebSocket upgrade failed' }, 400);
      }
      if (url.pathname.startsWith('/api/control')) {
        return await handleControl(request, url);
      }
      if (url.pathname.startsWith('/api/')) {
        return await proxyToPlugin(request, url);
      }
      return await serveStatic(url.pathname);
    } catch (error) {
      console.error('[Hermes web] Request failed', error);
      return json({ error: error instanceof Error ? error.message : 'Internal server error' }, 500);
    }
  },
  websocket: {
    open(ws) {
      const pod = ws.data.pod;
      const target = ws.data.target;
      if (!pod || !target) {
        ws.close(1008, 'Missing console target');
        return;
      }
      startKubectlAttach(ws, target, pod);
    },
    message(ws, message) {
      const stdin = ws.data.kubectlStdin;
      if (!stdin) return;
      try {
        handleConsoleInput(ws, stdin, typeof message === 'string' ? message : new TextDecoder().decode(message));
      } catch (error) {
        console.warn('[Hermes web] Console input failed', error);
      }
    },
    close(ws) {
      try {
        ws.data.kubectlStdin?.end();
      } catch {
        // Ignore close races.
      }
      ws.data.kubectl?.kill();
    }
  }
});

console.log(`[Hermes web] Listening on http://0.0.0.0:${port}`);
console.log(`[Hermes web] Loaded ${controlServers.length} control server(s): ${controlServers.map((target) => target.id).join(', ') || 'none'}`);
console.log(`[Hermes web] Proxying plugin API to ${pluginUrl || 'not configured'}`);
console.log(`[Hermes web] Kubernetes API ${kubeApi}`);

async function handleControl(request: Request, url: URL) {
  const action = url.pathname.substring('/api/control'.length) || '/status';
  if (action === '/auth') {
    if (request.method !== 'POST') return json({ error: 'Method not allowed' }, 405);
    const body = await request.json().catch(() => ({}));
    const login = await loginWithHecateDatabase(String(body.playerName || ''), String(body.password || ''), 'ADMIN');
    if (!login.success) {
      return json({ error: login.message }, 401);
    }
    const expiresAt = Math.floor(Date.now() / 1000) + webSessionSeconds;
    const session = await createHecateSessionToken(login.playerId!, login.sessionVersion!, expiresAt);
    return json({ success: true, expiresAt, playerId: login.playerId, playerName: login.playerName, role: login.role }, 200, {
      'Set-Cookie': `hecate_session=${session}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${webSessionSeconds}`
    });
  }

  const auth = await authorizeControl(request);
  if (!auth.authorized) {
    return json({ error: auth.error || 'Controller authorization required' }, 401);
  }

  if (action === '/servers' && request.method === 'GET') {
    return json({ servers: controlServers.map(publicControlServer) });
  }

  const route = matchControlServerAction(action);
  if (!route) return json({ error: 'Not found' }, 404);
  const target = getControlServer(route.serverId);
  if (!target) return json({ error: `Unknown server "${route.serverId}"` }, 404);
  const targetAction = route.action || '/status';

  if (targetAction === '/status' && request.method === 'GET') {
    return json(await controlStatus(request, target));
  }
  if (targetAction === '/start' && request.method === 'POST') {
    await scaleStatefulSet(target, 1);
    return json(await controlStatus(request, target));
  }
  if (targetAction === '/stop' && request.method === 'POST') {
    await scaleStatefulSet(target, 0);
    return json(await controlStatus(request, target));
  }
  if (targetAction === '/restart' && request.method === 'POST') {
    const status = await readKubernetesStatus(target);
    if ((status.replicas || 0) < 1) {
      await scaleStatefulSet(target, 1);
    } else if (status.pod?.name) {
      await kubeFetch(`/api/v1/namespaces/${target.namespace}/pods/${encodeURIComponent(status.pod.name)}`, { method: 'DELETE' });
    } else {
      await scaleStatefulSet(target, 1);
    }
    return json(await controlStatus(request, target));
  }
  if (targetAction === '/logs' && request.method === 'GET') {
    const tailLines = clamp(Number(url.searchParams.get('tailLines') || 500), 1, 5000);
    const pod = await currentPodName(target);
    if (!pod) return json({ error: 'Minecraft pod is not running', lines: [] }, 409);
    const text = await kubeText(logPath(target, pod, tailLines, false));
    return json({ pod, lines: text.split(/\r?\n/).filter(Boolean) });
  }
  if (targetAction === '/logs/stream' && request.method === 'GET') {
    const tailLines = clamp(Number(url.searchParams.get('tailLines') || 500), 1, 5000);
    const pod = await currentPodName(target);
    if (!pod) return eventStreamFromLines([{ error: 'Minecraft pod is not running' }]);
    return streamPodLogs(target, pod, tailLines);
  }
  return json({ error: 'Not found' }, 404);
}

async function handleWebAuth(request: Request, url: URL) {
  if (url.pathname === '/api/auth/login') {
    if (request.method !== 'POST') return json({ error: 'Method not allowed' }, 405);
    const body = await request.json().catch(() => ({}));
    const login = await loginWithHecateDatabase(String(body.playerName || ''), String(body.password || ''));
    if (!login.success) return json({ success: false, message: login.message, error: login.message }, 401);
    await db`UPDATE Players SET web_last_login_at = NOW() WHERE player_id = ${login.playerId}`;
    const expiresAt = Math.floor(Date.now() / 1000) + webSessionSeconds;
    const session = await createHecateSessionToken(login.playerId!, login.sessionVersion!, expiresAt);
    return json({ success: true, message: 'Logged in.', playerId: login.playerId, role: login.role }, 200, {
      'Set-Cookie': `hecate_session=${session}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${webSessionSeconds}`
    });
  }
  if (url.pathname === '/api/auth/logout') {
    const user = await authenticateHecateRequest(request);
    if (user) {
      await db`UPDATE Players SET web_session_version = COALESCE(web_session_version, 0) + 1 WHERE player_id = ${user.playerId}`;
    }
    return json({ success: true }, 200, {
      'Set-Cookie': 'hecate_session=; Path=/; HttpOnly; SameSite=Lax; Max-Age=0'
    });
  }
  if (url.pathname === '/api/me') {
    const user = await authenticateHecateRequest(request);
    if (user) return json(user);
    return proxyToPlugin(request, url);
  }
  return json({ error: 'Not found' }, 404);
}

async function proxyToPlugin(request: Request, url: URL) {
  if (!pluginUrl) {
    return json({ error: 'Hermes plugin API is not configured for this controller.' }, 503);
  }
  const target = `${pluginUrl}${url.pathname}${url.search}`;
  const headers = new Headers(request.headers);
  headers.set('X-Forwarded-Host', url.host);
  headers.set('X-Forwarded-Proto', url.protocol.replace(':', ''));
  await addProxyAuthHeaders(headers, request);
  headers.delete('host');
  const response = await fetch(target, {
    method: request.method,
    headers,
    body: request.method === 'GET' || request.method === 'HEAD' ? undefined : request.body,
    redirect: 'manual'
  });
  return new Response(response.body, {
    status: response.status,
    statusText: response.statusText,
    headers: response.headers
  });
}

async function addProxyAuthHeaders(headers: Headers, request: Request) {
  headers.delete('X-Hermes-Proxy-Player-Id');
  headers.delete('X-Hermes-Proxy-Expires');
  headers.delete('X-Hermes-Proxy-Signature');
  const user = await authenticateHecateRequest(request);
  if (!user) return;
  const expiresAt = Math.floor(Date.now() / 1000) + 60;
  headers.set('X-Hermes-Proxy-Player-Id', user.playerId);
  headers.set('X-Hermes-Proxy-Expires', String(expiresAt));
  headers.set('X-Hermes-Proxy-Signature', await hmacBase64Url(hecateSessionSecret, `${user.playerId}:${expiresAt}`));
}

async function authorizeControl(request: Request) {
  const offlineUser = await authenticateHecateRequest(request);
  if (offlineUser) {
    return { authorized: offlineUser.role === 'ADMIN', source: 'database', error: offlineUser.role === 'ADMIN' ? '' : 'Hermes ADMIN role required' };
  }
  return { authorized: false, source: 'database', error: 'Hermes ADMIN role required' };
}

async function loginWithHecateDatabase(playerName: string, password: string, requiredRole: 'VIEWER' | 'EDITOR' | 'ADMIN' = 'VIEWER') {
  if (!playerName.trim() || !password) return { success: false, message: 'Minecraft name and password are required.' };
  let rows;
  try {
    rows = await db`
      SELECT player_id, last_known_name, web_password_hash, COALESCE(web_session_version, 0) AS web_session_version, COALESCE(web_role, '') AS web_role
      FROM Players
      WHERE lower(last_known_name) = lower(${playerName})
      ORDER BY last_online DESC NULLS LAST
      LIMIT 1
    `;
  } catch (error) {
    return { success: false, message: `Postgres auth unavailable: ${error instanceof Error ? error.message : String(error)}` };
  }
  const row = rows[0];
  if (!row || !await verifyHecatePassword(password, String(row.web_password_hash || ''))) {
    return { success: false, message: 'Invalid Minecraft name or password.' };
  }
  const role = normalizeWebRole(row.web_role);
  if (!hasRole(role, requiredRole)) {
    return { success: false, message: `Hermes ${requiredRole} role required.` };
  }
  return { success: true, message: 'Logged in.', playerId: String(row.player_id), playerName: String(row.last_known_name || playerName), role, sessionVersion: Number(row.web_session_version || 0) };
}

async function authenticateHecateRequest(request: Request) {
  const claims = await parseHecateSessionToken(cookie(request, 'hecate_session'));
  if (!claims || claims.expiresAt < Math.floor(Date.now() / 1000)) return null;
  let rows;
  try {
    rows = await db`
      SELECT player_id, last_known_name, COALESCE(web_session_version, 0) AS web_session_version, COALESCE(web_role, '') AS web_role
      FROM Players
      WHERE player_id = ${claims.playerId}
      LIMIT 1
    `;
  } catch {
    return null;
  }
  const row = rows[0];
  if (!row || Number(row.web_session_version || 0) !== claims.sessionVersion) return null;
  return {
    playerId: String(row.player_id),
    playerName: String(row.last_known_name || ''),
    role: normalizeWebRole(row.web_role)
  };
}

function handleConsoleInput(ws: any, stdin: { write?: (text: string) => unknown; flush?: () => unknown }, input: string) {
  let buffer = String(ws.data.consoleBuffer || '');
  for (const char of input) {
    if (char === '\r' || char === '\n') {
      const command = sanitizeConsoleCommand(buffer);
      buffer = '';
      if (command) {
        stdin.write?.(`${command}\n`);
        stdin.flush?.();
      }
      continue;
    }
    if (char === '\u007f' || char === '\b') {
      buffer = buffer.slice(0, -1);
      continue;
    }
    if (isAllowedConsoleChar(char) && buffer.length < 256) {
      buffer += char;
    }
  }
  ws.data.consoleBuffer = buffer;
}

function sanitizeConsoleCommand(command: string) {
  return Array.from(command)
    .filter(isAllowedConsoleChar)
    .join('')
    .replace(/\s+/g, ' ')
    .trim()
    .slice(0, 256);
}

function isAllowedConsoleChar(char: string) {
  return /^[A-Za-zÄÖÜäöüß0-9 _./:;,@#%+*=\-()[\]{}"'!?<>|&$~]$/.test(char);
}

function hasRole(actual: string, required: 'VIEWER' | 'EDITOR' | 'ADMIN') {
  const order = ['NONE', 'VIEWER', 'EDITOR', 'ADMIN'];
  return order.indexOf(actual || 'NONE') >= order.indexOf(required);
}

function normalizeWebRole(value: unknown) {
  const role = String(value || '').trim().toUpperCase();
  return ['NONE', 'VIEWER', 'EDITOR', 'ADMIN'].includes(role) ? role : 'EDITOR';
}

async function createHecateSessionToken(playerId: string, sessionVersion: number, expiresAt: number) {
  const payload = `${playerId}:${sessionVersion}:${expiresAt}`;
  const signature = await hmacBase64Url(hecateSessionSecret, payload);
  return Buffer.from(`${payload}:${signature}`, 'utf8').toString('base64url');
}

async function parseHecateSessionToken(sessionToken: string) {
  if (!sessionToken) return null;
  try {
    const decoded = Buffer.from(sessionToken, 'base64url').toString('utf8');
    const parts = decoded.split(':');
    if (parts.length !== 4) return null;
    const payload = `${parts[0]}:${parts[1]}:${parts[2]}`;
    if (!constantTimeStringEquals(await hmacBase64Url(hecateSessionSecret, payload), parts[3])) return null;
    return { playerId: parts[0], sessionVersion: Number(parts[1]), expiresAt: Number(parts[2]) };
  } catch {
    return null;
  }
}

async function controlStatus(request: Request, target: ControlServer) {
  const kubernetes = await readKubernetesStatus(target);
  const plugin = await pluginStatus(request, target);
  return { ...kubernetes, plugin };
}

async function readKubernetesStatus(target: ControlServer) {
  const [statefulSetData, scaleData, podsData] = await Promise.all([
    kubeJson(`/apis/apps/v1/namespaces/${target.namespace}/statefulsets/${target.statefulSet}`),
    kubeJson(`/apis/apps/v1/namespaces/${target.namespace}/statefulsets/${target.statefulSet}/scale`),
    kubeJson(`/api/v1/namespaces/${target.namespace}/pods?labelSelector=${encodeURIComponent(target.podLabelSelector)}`)
  ]);
  const pod = selectCurrentPod(target, podsData.items || []);
  return {
    serverId: target.id,
    label: target.label,
    namespace: target.namespace,
    statefulSet: target.statefulSet,
    labelSelector: target.podLabelSelector,
    container: target.container,
    desiredReplicas: Number(scaleData.spec?.replicas ?? statefulSetData.spec?.replicas ?? 0),
    replicas: Number(statefulSetData.status?.replicas ?? 0),
    readyReplicas: Number(statefulSetData.status?.readyReplicas ?? 0),
    updatedReplicas: Number(statefulSetData.status?.updatedReplicas ?? 0),
    image: statefulSetData.spec?.template?.spec?.containers?.find((container: any) => container.name === target.container)?.image || '',
    pod
  };
}

async function pluginStatus(request: Request, target: ControlServer) {
  const targetPluginUrl = trimTrailingSlash(target.pluginUrl || pluginUrl);
  if (!targetPluginUrl) return { online: false, configured: false };
  try {
    const headers = new Headers();
    await addProxyAuthHeaders(headers, request);
    const response = await fetch(`${targetPluginUrl}/api/server/status`, {
      headers
    });
    if (!response.ok) return { online: false, status: response.status };
    return { online: true, ...(await response.json()) };
  } catch {
    return { online: false };
  }
}

async function scaleStatefulSet(target: ControlServer, replicas: number) {
  await kubeJson(`/apis/apps/v1/namespaces/${target.namespace}/statefulsets/${target.statefulSet}/scale`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/merge-patch+json' },
    body: JSON.stringify({ spec: { replicas } })
  });
}

async function currentPodName(target: ControlServer) {
  const pods = await kubeJson(`/api/v1/namespaces/${target.namespace}/pods?labelSelector=${encodeURIComponent(target.podLabelSelector)}`);
  return selectCurrentPod(target, pods.items || [])?.name || '';
}

function selectCurrentPod(target: ControlServer, pods: any[]) {
  const sorted = [...pods].sort((left, right) => String(right.metadata?.creationTimestamp || '').localeCompare(String(left.metadata?.creationTimestamp || '')));
  const pod = sorted[0];
  if (!pod) return null;
  const container = (pod.status?.containerStatuses || []).find((status: any) => status.name === target.container) || pod.status?.containerStatuses?.[0];
  return {
    name: pod.metadata?.name || '',
    phase: pod.status?.phase || '',
    ready: Boolean(container?.ready),
    restartCount: Number(container?.restartCount || 0),
    nodeName: pod.spec?.nodeName || '',
    podIP: pod.status?.podIP || '',
    startTime: pod.status?.startTime || '',
    reason: pod.status?.reason || container?.state?.waiting?.reason || container?.state?.terminated?.reason || ''
  };
}

function logPath(target: ControlServer, pod: string, tailLines: number, follow: boolean) {
  const params = new URLSearchParams({
    container: target.container,
    tailLines: String(tailLines),
    timestamps: 'true'
  });
  if (follow) params.set('follow', 'true');
  return `/api/v1/namespaces/${target.namespace}/pods/${encodeURIComponent(pod)}/log?${params}`;
}

async function streamPodLogs(target: ControlServer, pod: string, tailLines: number) {
  const response = await kubeFetch(logPath(target, pod, tailLines, true));
  if (!response.body) return eventStreamFromLines([{ error: 'Kubernetes log stream is unavailable' }]);
  const stream = new ReadableStream({
    async start(controller) {
      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split(/\r?\n/);
          buffer = lines.pop() || '';
          for (const line of lines) {
            if (line) controller.enqueue(sse({ pod, line }));
          }
        }
        if (buffer) controller.enqueue(sse({ pod, line: buffer }));
      } catch (error) {
        controller.enqueue(sse({ error: error instanceof Error ? error.message : String(error) }));
      } finally {
        controller.close();
      }
    }
  });
  return new Response(stream, { headers: sseHeaders() });
}

function startKubectlAttach(ws: any, target: ControlServer, pod: string) {
  const args = [
    '--server', kubeApi,
    '--token', serviceAccountToken.trim(),
    ...kubectlTlsArgs(),
    'attach',
    '-n', target.namespace,
    pod,
    '-c', target.container,
    '--stdin=true',
    '--tty=false'
  ];
  console.log(`[Hermes web] Starting kubectl attach for ${target.id}/${pod}: namespace=${target.namespace} container=${target.container} stdin=true tty=false kubectl=${kubectlPath}`);
  const proc = Bun.spawn([kubectlPath, ...args], {
    stdin: 'pipe',
    stdout: 'pipe',
    stderr: 'pipe',
    env: { ...process.env, TERM: 'xterm-256color' }
  });
  ws.data.kubectl = proc;
  ws.data.kubectlStdin = proc.stdin;
  ws.send(JSON.stringify({ type: 'status', message: `Attached to ${pod}` }));
  pumpKubectlStream(ws, proc.stdout, 'output');
  pumpKubectlStream(ws, proc.stderr, 'error');
  proc.exited.then((code) => {
    console.warn(`[Hermes web] kubectl attach exited for ${target.id}/${pod}: ${code}`);
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'status', message: `Console detached (${code})` }));
      ws.close();
    }
  }).catch((error) => {
    console.error(`[Hermes web] kubectl attach failed for ${target.id}/${pod}`, error);
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'error', message: `kubectl attach failed: ${error instanceof Error ? error.message : String(error)}` }));
      ws.close();
    }
  });
}

async function pumpKubectlStream(ws: any, stream: ReadableStream<Uint8Array>, type: 'output' | 'error') {
  const reader = stream.getReader();
  const decoder = new TextDecoder();
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const text = decoder.decode(value, { stream: true });
      if (text && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(type === 'output' ? { type, data: text } : { type, message: text }));
      }
    }
  } catch {
    // The stream normally errors when the browser closes the console.
  }
}

function kubectlTlsArgs() {
  if (kubeInsecureSkipTlsVerify) return ['--insecure-skip-tls-verify=true'];
  if (serviceAccountCa) return ['--certificate-authority', serviceAccountCaPath];
  return [];
}

function eventStreamFromLines(events: unknown[]) {
  return new Response(events.map((event) => new TextDecoder().decode(sse(event))).join(''), { headers: sseHeaders() });
}

async function loadControlServers(): Promise<ControlServer[]> {
  const raw = process.env.HERMES_SERVERS_FILE
    ? await readOptional(process.env.HERMES_SERVERS_FILE)
    : process.env.HERMES_SERVERS || '';
  if (!raw.trim()) {
    console.warn('[Hermes web] HERMES_SERVERS or HERMES_SERVERS_FILE is required for server control.');
    return [];
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    console.error('[Hermes web] Failed to parse HERMES_SERVERS JSON', error);
    return [];
  }
  if (!Array.isArray(parsed)) {
    console.error('[Hermes web] HERMES_SERVERS must be a JSON array.');
    return [];
  }
  const seen = new Set<string>();
  const result: ControlServer[] = [];
  for (const item of parsed) {
    if (!item || typeof item !== 'object') continue;
    const row = item as Record<string, unknown>;
    const target: ControlServer = {
      id: safeId(String(row.id || '')),
      label: String(row.label || row.id || '').trim(),
      namespace: String(row.namespace || '').trim(),
      statefulSet: String(row.statefulSet || '').trim(),
      podLabelSelector: String(row.podLabelSelector || '').trim(),
      container: String(row.container || '').trim(),
      pluginUrl: row.pluginUrl ? trimTrailingSlash(String(row.pluginUrl)) : undefined
    };
    const missing = ['id', 'label', 'namespace', 'statefulSet', 'podLabelSelector', 'container'].filter((key) => !String((target as any)[key] || '').trim());
    if (missing.length) {
      console.error(`[Hermes web] Skipping control server with missing field(s): ${missing.join(', ')}`);
      continue;
    }
    if (seen.has(target.id)) {
      console.error(`[Hermes web] Skipping duplicate control server id "${target.id}".`);
      continue;
    }
    seen.add(target.id);
    result.push(target);
  }
  return result;
}

function publicControlServer(target: ControlServer) {
  return {
    id: target.id,
    label: target.label,
    namespace: target.namespace,
    statefulSet: target.statefulSet,
    podLabelSelector: target.podLabelSelector,
    container: target.container,
    pluginConfigured: Boolean(target.pluginUrl || pluginUrl)
  };
}

function getControlServer(serverId: string) {
  return controlServerById.get(safeId(serverId));
}

function matchControlServerAction(action: string) {
  const match = action.match(/^\/servers\/([^/]+)(\/.*)?$/);
  if (!match) return null;
  return { serverId: decodeURIComponent(match[1]), action: match[2] || '/status' };
}

function matchControlServerRoute(pathname: string, suffix: string) {
  const match = pathname.match(/^\/api\/control\/servers\/([^/]+)(\/.*)$/);
  if (!match || match[2] !== suffix) return null;
  return { serverId: decodeURIComponent(match[1]) };
}

async function kubeJson(path: string, init: RequestInit = {}) {
  const response = await kubeFetch(path, init);
  return await response.json();
}

async function kubeText(path: string, init: RequestInit = {}) {
  const response = await kubeFetch(path, init);
  return await response.text();
}

async function kubeFetch(path: string, init: RequestInit = {}) {
  if (!serviceAccountToken) throw new Error('Kubernetes service account token is not available');
  const headers = new Headers(init.headers || {});
  headers.set('Authorization', `Bearer ${serviceAccountToken.trim()}`);
  headers.set('Accept', headers.get('Accept') || 'application/json');
  const response = await fetch(`${kubeApi}${path}`, { ...init, headers, tls: kubeTls } as any);
  if (!response.ok) {
    const text = await response.text().catch(() => '');
    throw new Error(`Kubernetes ${response.status}: ${text || response.statusText}`);
  }
  return response;
}

async function serveStatic(pathname: string) {
  const relative = pathname === '/' ? 'index.html' : pathname.replace(/^\/+/, '');
  const target = normalize(join(distDir, relative));
  const root = normalize(distDir);
  const file = target.startsWith(root) && existsSync(target) ? target : join(distDir, 'index.html');
  if (!existsSync(file)) return new Response('Hermes web bundle has not been built.', { status: 503 });
  return new Response(Bun.file(file), { headers: { 'Content-Type': contentType(file) } });
}

function json(body: unknown, status = 200, headers: Record<string, string> = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json; charset=utf-8', ...headers }
  });
}

function sse(body: unknown) {
  return new TextEncoder().encode(`data: ${JSON.stringify(body)}\n\n`);
}

function sseHeaders() {
  return {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache, no-transform',
    Connection: 'keep-alive'
  };
}

async function hmacBase64Url(secret: string, value: string) {
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  const signature = await crypto.subtle.sign('HMAC', key, new TextEncoder().encode(value));
  return Buffer.from(signature).toString('base64url');
}

function cookie(request: Request, name: string) {
  const raw = request.headers.get('cookie') || '';
  for (const part of raw.split(';')) {
    const [key, ...value] = part.trim().split('=');
    if (key === name) return value.join('=');
  }
  return '';
}

function contentType(path: string) {
  if (path.endsWith('.html')) return 'text/html; charset=utf-8';
  if (path.endsWith('.js')) return 'text/javascript; charset=utf-8';
  if (path.endsWith('.css')) return 'text/css; charset=utf-8';
  if (path.endsWith('.svg')) return 'image/svg+xml';
  if (path.endsWith('.png')) return 'image/png';
  if (path.endsWith('.ico')) return 'image/x-icon';
  return 'application/octet-stream';
}

async function readOptional(path: string) {
  try {
    return (await readFile(path, 'utf8')).trim();
  } catch {
    return '';
  }
}

function trimTrailingSlash(value: string) {
  return value.replace(/\/+$/, '');
}

function safeId(value: string) {
  return value.trim().toLowerCase().replace(/[^a-z0-9_.-]/g, '-');
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, Number.isFinite(value) ? value : min));
}

async function verifyHecatePassword(password: string, storedHash: string) {
  if (!storedHash.trim()) return false;
  const parts = storedHash.split('$');
  if (parts.length !== 4 || parts[0] !== 'pbkdf2_sha256') return false;
  const iterations = Number(parts[1]);
  if (!Number.isFinite(iterations) || iterations <= 0) return false;
  const salt = Buffer.from(parts[2], 'base64');
  const expected = Buffer.from(parts[3], 'base64');
  const key = await crypto.subtle.importKey('raw', new TextEncoder().encode(password), 'PBKDF2', false, ['deriveBits']);
  const bits = await crypto.subtle.deriveBits({ name: 'PBKDF2', hash: 'SHA-256', salt, iterations }, key, expected.length * 8);
  return constantTimeEquals(new Uint8Array(bits), new Uint8Array(expected));
}

function constantTimeEquals(left: Uint8Array, right: Uint8Array) {
  if (left.length !== right.length) return false;
  let diff = 0;
  for (let index = 0; index < left.length; index++) {
    diff |= left[index] ^ right[index];
  }
  return diff === 0;
}

function constantTimeStringEquals(left: string, right: string) {
  return constantTimeEquals(new TextEncoder().encode(left), new TextEncoder().encode(right));
}

function withKubernetesChannel(channel: number, text: string) {
  const payload = new TextEncoder().encode(text);
  const framed = new Uint8Array(payload.length + 1);
  framed[0] = channel;
  framed.set(payload, 1);
  return framed;
}

function toUint8Array(data: unknown) {
  if (data instanceof ArrayBuffer) return new Uint8Array(data);
  if (data instanceof Uint8Array) return data;
  if (typeof data === 'string') return new TextEncoder().encode(data);
  return new Uint8Array();
}
