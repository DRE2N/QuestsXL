import { ActionIcon, Alert, Badge, Box, Button, Group, NumberInput, Paper, ScrollArea, Select, Stack, Tabs, Text, TextInput, Tooltip } from '@mantine/core';
import { IconCopy, IconPlus, IconTrash } from '@tabler/icons-react';
import { parseDocument, stringify } from 'yaml';
import type { AssetCatalog } from '../../api/types';
import { ValueField } from '../fields/ValueField';
import { RawYamlEditor } from '../fields/RawYamlEditor';
import { LocalizedMiniMessageEditor, type LocalizedText, localizedFromYaml, localizedToYaml } from './LocalizedMiniMessageEditor';

type RespawnRow = {
  id: string;
  displayName: LocalizedText;
  description: LocalizedText;
  world: string;
  x: number;
  y: number;
  z: number;
  yaw: number;
  pitch: number;
  cooldown: number;
  unlockMode: string;
  useMode: string;
  quest: string;
  set: string;
  extras: Record<string, unknown>;
};

export function RespawnPointsWorkspace({ selectedPath, yaml, assets, disabled, onYamlChange }: { selectedPath: string; yaml: string; assets: AssetCatalog; disabled: boolean; onYamlChange: (yaml: string) => void }) {
  const rows = parseRespawns(yaml);
  const errors = validateRespawns(rows);
  const update = (nextRows: RespawnRow[]) => onYamlChange(serializeRespawns(nextRows));
  return (
    <ScrollArea h="calc(100vh - 54px)">
      <Box p="lg">
        <Group justify="space-between" mb="md">
          <Box>
            <Text fw={700} size="lg">{selectedPath || 'respawnPoints.yml'}</Text>
            <Text c="dimmed" size="sm">{rows.length} respawn points / waypoints</Text>
          </Box>
          <Group gap="xs">
            {errors.length > 0 && <Badge color="red">{errors.length} issues</Badge>}
            <Badge variant="outline">respawns</Badge>
          </Group>
        </Group>
        {errors.length > 0 && <Alert color="red" mb="md">{errors.slice(0, 5).map((error) => <Text key={error} size="sm">{error}</Text>)}</Alert>}
        <Tabs defaultValue="editor">
          <Tabs.List>
            <Tabs.Tab value="editor">Editor</Tabs.Tab>
            <Tabs.Tab value="yaml">Raw YAML</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="editor" pt="md">
            <Stack gap="sm">
              <Group justify="space-between"><Text fw={700}>Waypoints</Text><Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => update([...rows, starterRow(rows, assets)])}>Waypoint</Button></Group>
              {rows.map((row, index) => <RespawnCard key={`${row.id}-${index}`} row={row} rows={rows} index={index} disabled={disabled} assets={assets} onChange={update} />)}
            </Stack>
          </Tabs.Panel>
          <Tabs.Panel value="yaml" pt="md">
            <RawYamlEditor value={yaml} disabled={disabled} minRows={32} onChange={onYamlChange} />
          </Tabs.Panel>
        </Tabs>
      </Box>
    </ScrollArea>
  );
}

function RespawnCard({ row, rows, index, disabled, assets, onChange }: { row: RespawnRow; rows: RespawnRow[]; index: number; disabled: boolean; assets: AssetCatalog; onChange: (rows: RespawnRow[]) => void }) {
  const update = (patch: Partial<RespawnRow>) => onChange(rows.map((item, rowIndex) => rowIndex === index ? { ...item, ...patch } : item));
  return (
    <Paper p="md" withBorder className="slotBox">
      <Stack gap="sm">
        <Group justify="space-between">
          <Text fw={700}>{row.id || 'New waypoint'}</Text>
          <Group gap={4}>
            <Tooltip label="Duplicate"><ActionIcon variant="subtle" disabled={disabled} onClick={() => onChange([...rows, { ...row, id: uniqueId(rows, `${row.id || 'waypoint'}_copy`) }])}><IconCopy size={16} /></ActionIcon></Tooltip>
            <Tooltip label="Delete"><ActionIcon variant="subtle" color="red" disabled={disabled} onClick={() => onChange(rows.filter((_, rowIndex) => rowIndex !== index))}><IconTrash size={16} /></ActionIcon></Tooltip>
          </Group>
        </Group>
        <Group grow align="start">
          <TextInput label="id" value={row.id} disabled={disabled} error={!row.id.trim()} onChange={(event) => update({ id: event.currentTarget.value })} />
          <Select label="unlockMode" value={row.unlockMode} disabled={disabled} data={['NEAR', 'ACTION', 'QUEST']} onChange={(unlockMode) => update({ unlockMode: unlockMode || 'NEAR' })} />
          <Select label="useMode" value={row.useMode} disabled={disabled} data={['NEAREST', 'LAST']} onChange={(useMode) => update({ useMode: useMode || 'NEAREST' })} />
          <NumberInput label="cooldown" value={row.cooldown} disabled={disabled} min={0} onChange={(cooldown) => update({ cooldown: Number(cooldown || 0) })} />
        </Group>
        <LocalizedMiniMessageEditor label="displayName" value={row.displayName} compact disabled={disabled} onChange={(displayName) => update({ displayName })} />
        <LocalizedMiniMessageEditor label="description" value={row.description} disabled={disabled} onChange={(description) => update({ description })} />
        <Group grow align="start">
          <ValueField label="world" value={row.world} kind="world" disabled={disabled} assets={assets} onChange={(world) => update({ world: String(world || defaultWorld(assets)) })} />
          <NumberInput label="x" value={row.x} disabled={disabled} onChange={(x) => update({ x: Number(x || 0) })} />
          <NumberInput label="y" value={row.y} disabled={disabled} onChange={(y) => update({ y: Number(y || 0) })} />
          <NumberInput label="z" value={row.z} disabled={disabled} onChange={(z) => update({ z: Number(z || 0) })} />
        </Group>
        <Group grow align="start">
          <NumberInput label="yaw" value={row.yaw} disabled={disabled} onChange={(yaw) => update({ yaw: Number(yaw || 0) })} />
          <NumberInput label="pitch" value={row.pitch} disabled={disabled} onChange={(pitch) => update({ pitch: Number(pitch || 0) })} />
          <ValueField label="quest" value={row.quest} kind="quest" disabled={disabled} assets={assets} onChange={(quest) => update({ quest: String(quest || '') })} />
          <TextInput label="set" value={row.set} disabled={disabled} onChange={(event) => update({ set: event.currentTarget.value })} />
        </Group>
      </Stack>
    </Paper>
  );
}

function parseRespawns(yaml: string): RespawnRow[] {
  try {
    const data = parseDocument(yaml || '{}').toJS() as Record<string, unknown> | null;
    return Object.entries(asRecord(data)).map(([id, raw]) => sectionToRow(id, asRecord(raw)));
  } catch {
    return [];
  }
}

function sectionToRow(id: string, section: Record<string, unknown>): RespawnRow {
  const location = asRecord(section.location);
  return {
    id,
    displayName: localizedFromYaml(section, 'displayName', id),
    description: localizedFromYaml(section, 'description', ''),
    world: String(location.world || 'Erethon'),
    x: Number(location.x || 0),
    y: Number(location.y || 64),
    z: Number(location.z || 0),
    yaw: Number(location.yaw || 0),
    pitch: Number(location.pitch || 0),
    cooldown: Number(section.cooldown || 0),
    unlockMode: String(section.unlockMode || 'NEAR'),
    useMode: String(section.useMode || 'NEAREST'),
    quest: String(section.quest || ''),
    set: String(section.set || ''),
    extras: omit(section, ['location', 'displayName', 'description', 'cooldown', 'unlockMode', 'useMode', 'quest', 'set'])
  };
}

function serializeRespawns(rows: RespawnRow[]) {
  return stringify(Object.fromEntries(rows.filter((row) => row.id.trim()).map((row) => [row.id.trim(), {
    ...row.extras,
    location: { '==': 'org.bukkit.Location', world: row.world, x: row.x, y: row.y, z: row.z, pitch: row.pitch, yaw: row.yaw },
    displayName: localizedToYaml(row.displayName),
    description: localizedToYaml(row.description),
    cooldown: row.cooldown,
    unlockMode: row.unlockMode,
    useMode: row.useMode,
    quest: row.quest || null,
    set: row.set || null
  }])), { lineWidth: 0 });
}

function validateRespawns(rows: RespawnRow[]) {
  const errors: string[] = [];
  const seen = new Set<string>();
  rows.forEach((row) => {
    if (!row.id.trim()) errors.push('Waypoint is missing an id');
    if (row.id && seen.has(row.id)) errors.push(`Duplicate waypoint id ${row.id}`);
    seen.add(row.id);
    if (!row.world.trim() || [row.x, row.y, row.z].some((value) => Number.isNaN(value))) errors.push(`${row.id || '(new)'} has an invalid location`);
    if (!['NEAR', 'ACTION', 'QUEST'].includes(row.unlockMode)) errors.push(`${row.id}: invalid unlockMode`);
    if (!['NEAREST', 'LAST'].includes(row.useMode)) errors.push(`${row.id}: invalid useMode`);
  });
  return errors;
}

function starterRow(rows: RespawnRow[], assets: AssetCatalog): RespawnRow {
  return { id: uniqueId(rows, 'waypoint'), displayName: { en: '<green>New waypoint', de: '' }, description: { en: '', de: '' }, world: defaultWorld(assets), x: 0, y: 64, z: 0, yaw: 0, pitch: 0, cooldown: 0, unlockMode: 'NEAR', useMode: 'NEAREST', quest: '', set: '', extras: {} };
}

function uniqueId(rows: Array<{ id: string }>, base: string) {
  const used = new Set(rows.map((row) => row.id));
  if (!used.has(base)) return base;
  for (let index = 2; index < 1000; index++) {
    const next = `${base}_${index}`;
    if (!used.has(next)) return next;
  }
  return `${base}_${Date.now()}`;
}

function defaultWorld(assets: AssetCatalog) {
  return assets.worlds?.[0]?.id || 'Erethon';
}

function asRecord(value: unknown): Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

function omit(source: Record<string, unknown>, keys: string[]) {
  return Object.fromEntries(Object.entries(source).filter(([key]) => !keys.includes(key)));
}
