import { useEffect, useMemo, useState } from 'react';
import type { ReactNode } from 'react';
import { Badge, Box, Group, Paper, SimpleGrid, Stack, Text, ThemeIcon } from '@mantine/core';
import { IconActivity, IconBook, IconChevronRight, IconDatabase, IconServer, IconUsers } from '@tabler/icons-react';
import { api } from '../../api/client';
import type { ContentItem, ContentType } from '../../api/types';

type ServerStatus = {
  name?: string;
  version?: string;
  bukkitVersion?: string;
  onlinePlayers?: number;
  maxPlayers?: number;
  tps?: number[];
  mspt?: number;
  memoryUsed?: number;
  memoryMax?: number;
  uptimeMs?: number;
  worlds?: Array<{ name: string; players: number; loadedChunks: number; entities: number }>;
};

type MetricSample = { at: number; tps: number; mspt: number };

export function Dashboard({ types, onOpenType, onOpenServer }: { types: ContentType[]; onOpenType: (type: string) => void | Promise<void>; onOpenServer: () => void }) {
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [status, setStatus] = useState<ServerStatus | null>(null);
  const [samples, setSamples] = useState<MetricSample[]>([]);

  useEffect(() => {
    let cancelled = false;
    Promise.all(types.map(async (type) => {
      try {
        const items = await api<ContentItem[]>(`/api/content/${type.id}`);
        return [type.id, items.length] as const;
      } catch {
        return [type.id, 0] as const;
      }
    })).then((entries) => {
      if (!cancelled) setCounts(Object.fromEntries(entries));
    });
    return () => { cancelled = true; };
  }, [types]);

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const next = await api<ServerStatus>('/api/server/status');
        if (cancelled) return;
        setStatus(next);
        setSamples((current) => [...current, { at: Date.now(), tps: firstTps(next), mspt: Number(next.mspt || 0) }].slice(-48));
      } catch {
        if (!cancelled) setStatus(null);
      }
    };
    load();
    const handle = window.setInterval(load, 1000);
    return () => {
      cancelled = true;
      window.clearInterval(handle);
    };
  }, []);

  const totalContent = useMemo(() => Object.values(counts).reduce((sum, count) => sum + count, 0), [counts]);
  const tps = firstTps(status);
  const mspt = Number(status?.mspt || 0);
  const memoryPercent = memoryUsagePercent(status);

  return (
    <Box className="dashboardShell">
      <Stack gap="xl">
        <Group justify="space-between" align="flex-end">
          <Box>
            <Text size="xs" c="dimmed" tt="uppercase" fw={700}>Erethon Authoring</Text>
            <Text fw={800} size="32px">Content overview</Text>
          </Box>
          <Badge size="lg" variant="light" color={tps >= 19 ? 'green' : tps >= 17 ? 'yellow' : 'red'}>
            {tps ? `${tps.toFixed(2)} TPS` : 'TPS unavailable'}
          </Badge>
        </Group>

        <SimpleGrid cols={{ base: 1, sm: 2, lg: 4 }}>
          <MetricCard icon={<IconDatabase size={20} />} label="Editable files" value={String(totalContent)} detail={`${types.length} categories`} />
          <MetricCard icon={<IconActivity size={20} />} label="MSPT" value={mspt ? mspt.toFixed(2) : '-'} detail={mspt ? msptHealth(mspt) : 'Not exposed by server'} />
          <MetricCard icon={<IconUsers size={20} />} label="Players" value={`${status?.onlinePlayers ?? 0}/${status?.maxPlayers ?? 0}`} detail="Online now" />
          <MetricCard icon={<IconServer size={20} />} label="Memory" value={`${memoryPercent.toFixed(0)}%`} detail={formatMemory(status?.memoryUsed || 0, status?.memoryMax || 0)} />
        </SimpleGrid>

        <SimpleGrid cols={{ base: 1, lg: 2 }}>
          <Paper p="lg" withBorder className="dashboardPanel">
            <Group justify="space-between" mb="md">
              <Box>
                <Text fw={700}>Server performance</Text>
                <Text size="sm" c="dimmed">Updates every second</Text>
              </Box>
              <Text size="sm" c="dimmed">{formatUptime(status?.uptimeMs || 0)} uptime</Text>
            </Group>
            <Box className="performanceGraphBleed">
              <PerformanceGraph samples={samples} />
            </Box>
            <Group mt="md" gap="lg">
              <Text size="sm"><b>TPS</b> {tps ? tps.toFixed(2) : '-'}</Text>
              <Text size="sm"><b>MSPT</b> {mspt ? mspt.toFixed(2) : '-'}</Text>
              <Text size="sm"><b>Build</b> {status?.bukkitVersion || '-'}</Text>
            </Group>
          </Paper>

          <Paper p="lg" withBorder className="dashboardPanel">
            <Text fw={700} mb="md">Worlds</Text>
            <Stack gap="xs">
              {(status?.worlds || []).map((world) => (
                <Group key={world.name} justify="space-between" className="worldRow">
                  <Box>
                    <Text fw={600}>{world.name}</Text>
                    <Text size="xs" c="dimmed">{world.loadedChunks} chunks, {world.entities} entities</Text>
                  </Box>
                  <Badge variant="light">{world.players} players</Badge>
                </Group>
              ))}
              {(!status?.worlds || status.worlds.length === 0) && <Text c="dimmed">No world metrics available.</Text>}
            </Stack>
          </Paper>
        </SimpleGrid>

        <Box>
          <Text fw={700} mb="md">Editable categories</Text>
          <SimpleGrid cols={{ base: 1, sm: 2, lg: 3, xl: 4 }}>
            <Paper p="md" withBorder className="categoryCard clickable" onClick={onOpenServer}>
              <Group justify="space-between" align="start">
                <ThemeIcon variant="light" color="red" size="md"><IconServer size={16} /></ThemeIcon>
                <IconChevronRight size={16} />
              </Group>
              <Text fw={700} mt="sm">Server Control</Text>
              <Text size="sm" c="dimmed">Start, stop, logs, console</Text>
            </Paper>
            {types.map((type) => (
              <Paper key={type.id} p="md" withBorder className="categoryCard clickable" onClick={() => { void onOpenType(type.id); }}>
                <Group justify="space-between" align="start">
                  <ThemeIcon variant="light" color="teal" size="md"><IconBook size={16} /></ThemeIcon>
                  <IconChevronRight size={16} />
                </Group>
                <Text fw={700} mt="sm">{type.label}</Text>
                <Text size="sm" c="dimmed">{counts[type.id] ?? 0} files</Text>
              </Paper>
            ))}
          </SimpleGrid>
        </Box>
      </Stack>
    </Box>
  );
}

function MetricCard({ icon, label, value, detail }: { icon: ReactNode; label: string; value: string; detail: string }) {
  return (
    <Paper p="lg" withBorder className="dashboardPanel">
      <Group justify="space-between">
        <ThemeIcon variant="light" color="teal">{icon}</ThemeIcon>
        <Text size="xs" c="dimmed" tt="uppercase" fw={700}>{label}</Text>
      </Group>
      <Text fw={800} size="30px" mt="md">{value}</Text>
      <Text size="sm" c="dimmed">{detail}</Text>
    </Paper>
  );
}

function PerformanceGraph({ samples }: { samples: MetricSample[] }) {
  const width = 640;
  const height = 170;
  const tpsPoints = linePoints(samples.map((sample) => sample.tps), 0, 20, width, height);
  const msptPoints = linePoints(samples.map((sample) => Math.min(sample.mspt, 100)), 0, 100, width, height);
  return (
    <svg className="performanceGraph" viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" role="img" aria-label="Server TPS and MSPT history">
      <line x1="0" y1={height * 0.25} x2={width} y2={height * 0.25} />
      <line x1="0" y1={height * 0.5} x2={width} y2={height * 0.5} />
      <line x1="0" y1={height * 0.75} x2={width} y2={height * 0.75} />
      <polyline points={msptPoints} className="msptLine" />
      <polyline points={tpsPoints} className="tpsLine" />
    </svg>
  );
}

function linePoints(values: number[], min: number, max: number, width: number, height: number) {
  if (values.length === 0) return '';
  return values.map((value, index) => {
    const x = values.length === 1 ? width : (index / (values.length - 1)) * width;
    const normalized = Math.max(0, Math.min(1, (value - min) / (max - min)));
    const y = height - normalized * height;
    return `${x.toFixed(1)},${y.toFixed(1)}`;
  }).join(' ');
}

function firstTps(status: ServerStatus | null) {
  return Number(status?.tps?.[0] || 0);
}

function memoryUsagePercent(status: ServerStatus | null) {
  if (!status?.memoryMax || status.memoryMax <= 0) return 0;
  return Math.min(100, (Number(status.memoryUsed || 0) / Number(status.memoryMax)) * 100);
}

function formatMemory(used: number, max: number) {
  if (!max || max <= 0) return 'Memory max unavailable';
  return `${toMb(used)} / ${toMb(max)} MB`;
}

function toMb(value: number) {
  return Math.round(value / 1024 / 1024);
}

function msptHealth(mspt: number) {
  if (mspt <= 40) return 'Healthy';
  if (mspt <= 50) return 'Near tick budget';
  return 'Over tick budget';
}

function formatUptime(ms: number) {
  if (!ms) return '-';
  const hours = Math.floor(ms / 3600000);
  const minutes = Math.floor((ms % 3600000) / 60000);
  return hours ? `${hours}h ${minutes}m` : `${minutes}m`;
}
