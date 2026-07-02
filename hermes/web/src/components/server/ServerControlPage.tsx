import { useEffect, useRef, useState } from 'react';
import { ActionIcon, Badge, Box, Button, Code, Collapse, Group, NumberInput, Paper, PasswordInput, ScrollArea, Select, SimpleGrid, Stack, Text, TextInput } from '@mantine/core';
import { IconCopy, IconPlayerPlay, IconPlayerStop, IconRefresh, IconReload } from '@tabler/icons-react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import '@xterm/xterm/css/xterm.css';
import { api } from '../../api/client';

type ControlStatus = {
  serverId: string;
  label: string;
  namespace: string;
  statefulSet: string;
  desiredReplicas: number;
  replicas: number;
  readyReplicas: number;
  updatedReplicas: number;
  image?: string;
  pod?: {
    name: string;
    phase: string;
    ready: boolean;
    restartCount: number;
    nodeName: string;
    podIP: string;
    startTime: string;
    reason?: string;
  } | null;
  plugin?: {
    online: boolean;
    onlinePlayers?: number;
    maxPlayers?: number;
    tps?: number[];
    mspt?: number;
    uptimeMs?: number;
    status?: number;
  };
};

type ControlServer = {
  id: string;
  label: string;
  namespace: string;
  statefulSet: string;
  podLabelSelector: string;
  container: string;
  pluginConfigured: boolean;
};

type LogEvent = { pod?: string; line?: string; error?: string };
type ConsoleEvent = { type: 'output' | 'error' | 'status'; data?: string; message?: string };

export function ServerControlPage({ compact = false }: { compact?: boolean }) {
  const [servers, setServers] = useState<ControlServer[]>([]);
  const [selectedServerId, setSelectedServerId] = useState(() => new URLSearchParams(window.location.search).get('server') || localStorage.getItem('hermes.serverId') || '');
  const selectedServerIdRef = useRef(selectedServerId);
  const [status, setStatus] = useState<ControlStatus | null>(null);
  const [error, setError] = useState('');
  const [playerName, setPlayerName] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState('');
  const [tailLines, setTailLines] = useState(500);
  const [logs, setLogs] = useState<string[]>([]);
  const [following, setFollowing] = useState(true);
  const [logsOpen, setLogsOpen] = useState(false);
  const terminalHost = useRef<HTMLDivElement | null>(null);
  const terminal = useRef<Terminal | null>(null);
  const terminalFit = useRef<FitAddon | null>(null);
  const consoleSocket = useRef<WebSocket | null>(null);
  const consoleServer = useRef('');
  const consoleInput = useRef<{ dispose: () => void } | null>(null);
  const reconnectTimer = useRef<number | null>(null);
  const allowReconnect = useRef(true);

  const selectedServer = servers.find((server) => server.id === selectedServerId) || null;
  const controlPath = (suffix: string) => `/api/control/servers/${encodeURIComponent(selectedServerId)}${suffix}`;

  const updateSelectedServer = (serverId: string, replace = false) => {
    selectedServerIdRef.current = serverId;
    setSelectedServerId(serverId);
    localStorage.setItem('hermes.serverId', serverId);
    if (!compact) {
      const url = new URL(window.location.href);
      url.searchParams.set('view', 'server');
      url.searchParams.set('server', serverId);
      window.history[replace ? 'replaceState' : 'pushState']({}, '', url);
    }
  };

  const loadServers = async () => {
    const result = await api<{ servers: ControlServer[] }>('/api/control/servers');
    const nextServers = result.servers || [];
    setServers(nextServers);
    const requested = new URLSearchParams(window.location.search).get('server') || selectedServerIdRef.current;
    const nextSelected = nextServers.some((server) => server.id === requested) ? requested : nextServers[0]?.id || '';
    if (nextSelected && nextSelected !== selectedServerIdRef.current) {
      updateSelectedServer(nextSelected, true);
    }
    return nextSelected;
  };

  const loadStatus = async () => {
    try {
      const serverId = selectedServerIdRef.current || await loadServers();
      if (!serverId) throw new Error('No controllable servers are configured.');
      const next = await api<ControlStatus>(`/api/control/servers/${encodeURIComponent(serverId)}/status`);
      setStatus(next);
      setError('');
      return next;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Server control is unavailable.');
      return null;
    }
  };

  const controllerLogin = async () => {
    setBusy('auth');
    try {
      await api('/api/control/auth', { method: 'POST', body: JSON.stringify({ playerName, password }) });
      setPassword('');
      await loadServers();
      await loadStatus();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Controller login failed.');
    } finally {
      setBusy('');
    }
  };

  const control = async (action: 'start' | 'stop' | 'restart') => {
    if (!selectedServerId) return;
    if ((action === 'stop' || action === 'restart') && !confirm(`${action === 'stop' ? 'Stop' : 'Restart'} the Minecraft server?`)) return;
    setBusy(action);
    try {
      setStatus(await api<ControlStatus>(controlPath(`/${action}`), { method: 'POST', body: '' }));
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : `${action} failed.`);
    } finally {
      setBusy('');
    }
  };

  const loadLogs = async () => {
    if (!selectedServerId) return;
    try {
      const result = await api<{ pod?: string; lines: string[] }>(`${controlPath('/logs')}?tailLines=${tailLines}`);
      setLogs(result.lines || []);
      setError('');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Logs are unavailable.');
    }
  };

  useEffect(() => {
    loadServers().then(() => loadStatus()).catch((err) => setError(err instanceof Error ? err.message : 'Server control is unavailable.'));
    const timer = window.setInterval(loadStatus, 3000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!following || !selectedServerId) return;
    const source = new EventSource(`${controlPath('/logs/stream')}?tailLines=${tailLines}`);
    source.onmessage = (event) => {
      const payload = JSON.parse(event.data) as LogEvent;
      if (payload.error) {
        setError(payload.error);
        source.close();
        return;
      }
      if (payload.line) setLogs((current) => [...current, payload.line!].slice(-2000));
    };
    source.onerror = () => source.close();
    return () => source.close();
  }, [following, tailLines, selectedServerId]);

  useEffect(() => {
    setLogs([]);
    closeConsole(false);
    if (selectedServerId) void loadStatus();
  }, [selectedServerId]);

  useEffect(() => {
    const onPopState = () => {
      const serverId = new URLSearchParams(window.location.search).get('server') || '';
      if (serverId && serverId !== selectedServerIdRef.current) {
        selectedServerIdRef.current = serverId;
        setSelectedServerId(serverId);
      }
    };
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, [selectedServerId]);

  useEffect(() => {
    if (!terminalHost.current || terminal.current) return;
    const next = new Terminal({
      cursorBlink: true,
      convertEol: true,
      fontFamily: 'JetBrains Mono, Consolas, monospace',
      fontSize: 13,
      theme: { background: '#090b10', foreground: '#dbe4ff' }
    });
    const fit = new FitAddon();
    next.loadAddon(fit);
    next.open(terminalHost.current);
    terminal.current = next;
    terminalFit.current = fit;
    fitTerminal();
    requestAnimationFrame(fitTerminal);
    const resizeObserver = new ResizeObserver(fitTerminal);
    resizeObserver.observe(terminalHost.current);
    return () => {
      resizeObserver.disconnect();
      closeConsole(false);
      next.dispose();
      terminal.current = null;
      terminalFit.current = null;
    };
  }, []);

  const closeConsole = (reconnect: boolean) => {
    allowReconnect.current = reconnect;
    if (reconnectTimer.current) {
      window.clearTimeout(reconnectTimer.current);
      reconnectTimer.current = null;
    }
    consoleInput.current?.dispose();
    consoleInput.current = null;
    consoleSocket.current?.close();
    consoleSocket.current = null;
  };

  const connectConsole = () => {
    const serverId = selectedServerIdRef.current;
    if (!serverId || !status?.pod?.ready) return;
    if (consoleSocket.current && consoleServer.current === serverId && [WebSocket.CONNECTING, WebSocket.OPEN].includes(consoleSocket.current.readyState)) return;
    fitTerminal();
    requestAnimationFrame(fitTerminal);
    closeConsole(false);
    allowReconnect.current = true;
    consoleServer.current = serverId;
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const socket = new WebSocket(`${protocol}//${window.location.host}/api/control/servers/${encodeURIComponent(serverId)}/console`);
    consoleSocket.current = socket;
    socket.onopen = () => terminal.current?.writeln('\r\n[Hermes] Console attached.');
    socket.onmessage = (event) => {
      const payload = JSON.parse(event.data) as ConsoleEvent;
      if (payload.type === 'output') terminal.current?.write(payload.data || '');
      else {
        terminal.current?.writeln(`\r\n[Hermes] ${payload.message || payload.type}`);
      }
    };
    socket.onclose = () => {
      terminal.current?.writeln('\r\n[Hermes] Console detached.');
      if (consoleSocket.current === socket) {
        consoleInput.current?.dispose();
        consoleInput.current = null;
        consoleSocket.current = null;
      }
      if (allowReconnect.current && selectedServerIdRef.current === serverId && consoleSocket.current == null) {
        reconnectTimer.current = window.setTimeout(() => {
          reconnectTimer.current = null;
          if (selectedServerIdRef.current === serverId) connectConsole();
        }, 2500);
      }
    };
    socket.onerror = () => terminal.current?.writeln('\r\n[Hermes] Console connection failed.');
    consoleInput.current?.dispose();
    consoleInput.current = terminal.current?.onData((data) => {
      if (socket.readyState === WebSocket.OPEN) {
        const safeInput = sanitizeConsoleInput(data);
        if (!safeInput) return;
        echoConsoleInput(safeInput);
        socket.send(safeInput);
      }
    }) || null;
  };

  const echoConsoleInput = (data: string) => {
    const term = terminal.current;
    if (!term) return;
    for (const char of data) {
      if (char === '\r' || char === '\n') {
        term.write('\r\n');
      } else if (char === '\u007f' || char === '\b') {
        term.write('\b \b');
      } else if (char >= ' ') {
        term.write(char);
      }
    }
  };

  const sanitizeConsoleInput = (data: string) => Array.from(data).filter((char) => {
    if (char === '\r' || char === '\n' || char === '\u007f' || char === '\b') return true;
    return /^[A-Za-zÄÖÜäöüß0-9 _./:;,@#%+*=\-()[\]{}"'!?<>|&$~]$/.test(char);
  }).join('');

  const fitTerminal = () => {
    terminalFit.current?.fit();
    const term = terminal.current;
    if (term && term.rows > 3) {
      term.resize(term.cols, term.rows - 1);
    }
  };

  const online = Boolean(status?.pod?.ready);
  const pluginOnline = Boolean(status?.plugin?.online);
  const tps = Array.isArray(status?.plugin?.tps) ? Number(status?.plugin?.tps?.[0] || 0) : 0;

  useEffect(() => {
    if (online && selectedServerId) {
      connectConsole();
    } else {
      closeConsole(false);
    }
  }, [online, selectedServerId]);

  return (
    <Box className={compact ? 'serverPanel compact' : 'serverPanel'}>
      <Stack gap="lg">
        <Group justify="space-between" align="end">
          <Box>
            <Text size="xs" c="dimmed" tt="uppercase" fw={700}>Hermes Control</Text>
            <Text fw={800} size={compact ? '24px' : '32px'}>{selectedServer?.label || 'Minecraft server'}</Text>
          </Box>
          <Group gap="xs">
            <Select
              w={compact ? 180 : 240}
              placeholder="Select server"
              value={selectedServerId || null}
              onChange={(value) => value && updateSelectedServer(value)}
              data={servers.map((server) => ({
                value: server.id,
                label: `${server.label} (${server.namespace}/${server.statefulSet})`
              }))}
              searchable
            />
            <Badge color={online ? 'green' : status?.desiredReplicas ? 'yellow' : 'gray'}>{online ? 'ready' : status?.desiredReplicas ? 'starting' : 'stopped'}</Badge>
            <Badge color={pluginOnline ? 'green' : 'red'} variant="light">{pluginOnline ? 'plugin online' : 'plugin offline'}</Badge>
          </Group>
        </Group>

        {error && (
          <Paper p="md" withBorder>
            <Stack gap="xs">
              <Text c="red" fw={700}>{error}</Text>
              <Group align="end">
                <TextInput label="Minecraft name" value={playerName} onChange={(event) => setPlayerName(event.currentTarget.value)} style={{ flex: 1 }} />
                <PasswordInput label="Web password" value={password} onChange={(event) => setPassword(event.currentTarget.value)} style={{ flex: 1 }} />
                <Button loading={busy === 'auth'} onClick={controllerLogin}>Unlock control</Button>
              </Group>
            </Stack>
          </Paper>
        )}

        <SimpleGrid cols={{ base: 1, md: 4 }}>
          <Metric label="StatefulSet" value={status?.statefulSet || '-'} detail={status?.namespace || '-'} />
          <Metric label="Replicas" value={`${status?.readyReplicas ?? 0}/${status?.desiredReplicas ?? 0}`} detail={`${status?.replicas ?? 0} current`} />
          <Metric label="Pod" value={status?.pod?.phase || '-'} detail={status?.pod?.name || 'No pod'} />
          <Metric label="TPS / MSPT" value={tps ? tps.toFixed(2) : '-'} detail={status?.plugin?.mspt ? `${Number(status.plugin.mspt).toFixed(2)} mspt` : 'Plugin metrics offline'} />
        </SimpleGrid>

        <Paper p="md" withBorder>
          <Group justify="space-between" mb="md">
            <Box>
              <Text fw={700}>Lifecycle</Text>
              <Text size="sm" c="dimmed">{status?.image || 'No image reported'}</Text>
            </Box>
            <Group>
              <Button variant="light" leftSection={<IconRefresh size={16} />} onClick={loadStatus}>Refresh</Button>
              <Button color="green" leftSection={<IconPlayerPlay size={16} />} loading={busy === 'start'} disabled={!selectedServerId} onClick={() => void control('start')}>Start</Button>
              <Button color="yellow" variant="light" leftSection={<IconReload size={16} />} loading={busy === 'restart'} disabled={!status?.desiredReplicas && !status?.pod} onClick={() => void control('restart')}>Restart</Button>
              <Button color="red" leftSection={<IconPlayerStop size={16} />} loading={busy === 'stop'} disabled={!status?.desiredReplicas && !status?.pod} onClick={() => void control('stop')}>Stop</Button>
            </Group>
          </Group>
          <SimpleGrid cols={{ base: 1, md: 3 }}>
            <Text size="sm"><b>Node:</b> {status?.pod?.nodeName || '-'}</Text>
            <Text size="sm"><b>Pod IP:</b> {status?.pod?.podIP || '-'}</Text>
            <Text size="sm"><b>Restarts:</b> {status?.pod?.restartCount ?? '-'}</Text>
          </SimpleGrid>
        </Paper>

        <Paper p="md" withBorder>
          <Group justify="space-between" mb="sm">
            <Text fw={700}>Console</Text>
            <Badge color={online ? 'green' : 'gray'} variant="light">{online ? 'connected' : 'offline'}</Badge>
          </Group>
          <Box ref={terminalHost} className="terminalHost" />
        </Paper>

        <Paper p="md" withBorder>
          <Group justify="space-between" mb="sm">
            <Box>
              <Text fw={700}>Logs</Text>
              <Text size="sm" c="dimmed">Kubernetes pod logs</Text>
            </Box>
            <Group>
              <NumberInput w={120} value={tailLines} min={1} max={5000} onChange={(value) => setTailLines(Number(value || 500))} />
              <Button size="xs" variant={following ? 'filled' : 'light'} onClick={() => setFollowing((value) => !value)}>{following ? 'Following' : 'Paused'}</Button>
              <ActionIcon variant="light" onClick={() => navigator.clipboard?.writeText(logs.join('\n'))}><IconCopy size={16} /></ActionIcon>
              <Button size="xs" variant="light" onClick={loadLogs}>Reload</Button>
              <Button size="xs" variant="light" onClick={() => setLogsOpen((value) => !value)}>{logsOpen ? 'Collapse' : 'Expand'}</Button>
            </Group>
          </Group>
          <Collapse in={logsOpen}>
            <ScrollArea h={compact ? 240 : 360} className="logPanel">
              <Code block>{logs.join('\n') || 'No log lines loaded.'}</Code>
            </ScrollArea>
          </Collapse>
        </Paper>
      </Stack>
    </Box>
  );
}

function Metric({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <Paper p="md" withBorder>
      <Text size="xs" c="dimmed" tt="uppercase" fw={700}>{label}</Text>
      <Text fw={800} size="xl">{value}</Text>
      <Text size="sm" c="dimmed" truncate>{detail}</Text>
    </Paper>
  );
}
