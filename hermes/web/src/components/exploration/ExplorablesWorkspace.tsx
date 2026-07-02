import { Fragment, useEffect, useMemo, useState } from 'react';
import { ActionIcon, Alert, Badge, Box, Button, Group, NumberInput, Paper, ScrollArea, Select, Stack, Tabs, Table, Text, TextInput, Tooltip } from '@mantine/core';
import { IconChevronDown, IconChevronRight, IconCopy, IconPlus, IconTrash } from '@tabler/icons-react';
import { parseDocument, stringify } from 'yaml';
import type { AssetCatalog } from '../../api/types';
import { ValueField } from '../fields/ValueField';
import { RawYamlEditor } from '../fields/RawYamlEditor';
import { LocalizedMiniMessageEditor, type LocalizedText, localizedFromRecord, localizedFromYaml, localizedToQLine, localizedToYaml, omitLocalized } from './LocalizedMiniMessageEditor';

type ContentAccess = {
  load: (type: string, path: string) => Promise<{ yaml: string; baseYaml: string; path: string; type: string }>;
  update: (type: string, path: string, yaml: string) => void;
};
type QLine = { id: string; values: Record<string, string> };
type LootItemRow = { item: string; chance: number };
type BaseRow = { id: string; parentSet: string; displayName: LocalizedText; world: string; x: number; y: number; z: number; extras: Record<string, string> };
type PoiRow = BaseRow & { type: 'poi'; flavourText: LocalizedText; radius: number };
type LootRow = BaseRow & { type: 'lootChest'; description: LocalizedText; loot: string; lootItems: LootItemRow[] };
type WaypointRow = Omit<BaseRow, 'extras'> & { type: 'waypoint'; description: LocalizedText; yaw: number; pitch: number; cooldown: number; unlockMode: string; useMode: string; quest: string; extras: Record<string, unknown> };
type ExplorableRow = PoiRow | LootRow | WaypointRow;
type SetRow = { id: string; parentSet: string; displayName: LocalizedText; description: LocalizedText; extras: Record<string, string> };
type VirtualItem = { kind: 'group'; setId: string; rows: ExplorableRow[] } | { kind: 'row'; row: ExplorableRow; index: number };

const SETS_PATH = 'explorationSets.yml';
const RESPAWNS_PATH = 'respawnPoints.yml';
const ROW_HEIGHT = 42;
const GROUP_HEIGHT = 38;
const EDITOR_HEIGHT = 380;

export function ExplorablesWorkspace({ selectedPath, yaml, assets, disabled, onYamlChange, contentAccess }: {
  selectedPath: string;
  yaml: string;
  assets: AssetCatalog;
  disabled: boolean;
  onYamlChange: (yaml: string) => void;
  contentAccess?: ContentAccess;
}) {
  const [setsYaml, setSetsYaml] = useState('sets: []\n');
  const [respawnsYaml, setRespawnsYaml] = useState('{}\n');
  const [setsLoading, setSetsLoading] = useState(Boolean(contentAccess));
  const [setsError, setSetsError] = useState('');
  const [respawnsError, setRespawnsError] = useState('');
  const [activeTab, setActiveTab] = useState<string | null>('explorables');
  const parsed = useMemo(() => parseExplorables(yaml), [yaml]);
  const respawns = useMemo(() => parseRespawns(respawnsYaml), [respawnsYaml]);
  const sets = useMemo(() => parseSets(setsYaml), [setsYaml]);
  const setIds = useMemo(() => new Set(sets.map((set) => set.id).filter(Boolean)), [sets]);
  const rows = useMemo(() => [...parsed.rows, ...respawns], [parsed.rows, respawns]);
  const errors = useMemo(() => validateExplorables(rows, sets, assets), [rows, sets, assets]);

  useEffect(() => {
    let cancelled = false;
    if (!contentAccess) return;
    setSetsLoading(true);
    contentAccess.load('explorationSets', SETS_PATH)
      .then((loaded) => {
        if (!cancelled) setSetsYaml(loaded.yaml || 'sets: []\n');
      })
      .catch((error) => {
        if (!cancelled) setSetsError(error instanceof Error ? error.message : String(error));
      })
      .finally(() => {
        if (!cancelled) setSetsLoading(false);
      });
    contentAccess.load('respawns', RESPAWNS_PATH)
      .then((loaded) => {
        if (!cancelled) setRespawnsYaml(loaded.yaml || '{}\n');
      })
      .catch((error) => {
        if (!cancelled) setRespawnsError(error instanceof Error ? error.message : String(error));
      });
    return () => { cancelled = true; };
  }, []);

  const updateRows = (nextRows: ExplorableRow[]) => {
    onYamlChange(serializeExplorables(nextRows));
    const nextRespawnsYaml = serializeRespawns(nextRows.filter((row): row is WaypointRow => row.type === 'waypoint'));
    setRespawnsYaml(nextRespawnsYaml);
    contentAccess?.update('respawns', RESPAWNS_PATH, nextRespawnsYaml);
  };
  const updateSets = (next: SetRow[]) => {
    const nextYaml = serializeSets(next);
    setSetsYaml(nextYaml);
    contentAccess?.update('explorationSets', SETS_PATH, nextYaml);
  };

  return (
    <ScrollArea h="calc(100vh - 54px)">
      <Box p="lg">
        <Group justify="space-between" mb="md">
          <Box>
            <Text fw={700} size="lg">{selectedPath || 'explorables.yml'}</Text>
            <Text c="dimmed" size="sm">{rows.filter((row) => row.type === 'poi').length} points, {rows.filter((row) => row.type === 'lootChest').length} loot chests, {rows.filter((row) => row.type === 'waypoint').length} waypoints, {sets.length} sets</Text>
          </Box>
          <Group gap="xs">
            {errors.length > 0 && <Badge color="red">{errors.length} issues</Badge>}
            {setsLoading && <Badge color="yellow">sets loading</Badge>}
            <Badge variant="outline">explorables</Badge>
          </Group>
        </Group>
        {(errors.length > 0 || setsError || respawnsError) && <Alert color={setsError || respawnsError ? 'yellow' : 'red'} mb="md">{setsError || respawnsError || errors.slice(0, 6).map((error) => <Text key={error} size="sm">{error}</Text>)}</Alert>}
        <Tabs value={activeTab} onChange={setActiveTab}>
          <Tabs.List>
            <Tabs.Tab value="explorables">Explorables</Tabs.Tab>
            <Tabs.Tab value="sets">Sets</Tabs.Tab>
            <Tabs.Tab value="yaml">Raw YAML</Tabs.Tab>
            <Tabs.Tab value="respawnsYaml">Respawns YAML</Tabs.Tab>
            <Tabs.Tab value="setsYaml">Sets YAML</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="explorables" pt="md">
            <ExplorableTable rows={rows} sets={sets} setIds={setIds} disabled={disabled} assets={assets} onChange={updateRows} />
          </Tabs.Panel>
          <Tabs.Panel value="sets" pt="md">
            <SetsTable rows={sets} explorables={rows} disabled={disabled} onChange={updateSets} />
          </Tabs.Panel>
          <Tabs.Panel value="yaml" pt="md">
            <RawYamlEditor value={yaml} disabled={disabled} minRows={32} onChange={onYamlChange} />
          </Tabs.Panel>
          <Tabs.Panel value="setsYaml" pt="md">
            <RawYamlEditor value={setsYaml} disabled={disabled} minRows={32} onChange={(next) => { setSetsYaml(next); contentAccess?.update('explorationSets', SETS_PATH, next); }} />
          </Tabs.Panel>
          <Tabs.Panel value="respawnsYaml" pt="md">
            <RawYamlEditor value={respawnsYaml} disabled={disabled} minRows={32} onChange={(next) => { setRespawnsYaml(next); contentAccess?.update('respawns', RESPAWNS_PATH, next); }} />
          </Tabs.Panel>
        </Tabs>
      </Box>
    </ScrollArea>
  );
}

function ExplorableTable({ rows, sets, setIds, disabled, assets, onChange }: { rows: ExplorableRow[]; sets: SetRow[]; setIds: Set<string>; disabled: boolean; assets: AssetCatalog; onChange: (rows: ExplorableRow[]) => void }) {
  const [expanded, setExpanded] = useState('');
  const [filter, setFilter] = useState('all');
  const [query, setQuery] = useState('');
  const [scrollTop, setScrollTop] = useState(0);
  const [viewportHeight, setViewportHeight] = useState(() => Math.max(620, window.innerHeight - 230));
  useEffect(() => {
    const updateHeight = () => setViewportHeight(Math.max(620, window.innerHeight - 230));
    window.addEventListener('resize', updateHeight);
    return () => window.removeEventListener('resize', updateHeight);
  }, []);
  const sortedRows = useMemo(() => rows
    .filter((row) => filter === 'all' || (filter === 'poi' && row.type === 'poi') || (filter === 'lootChest' && row.type === 'lootChest') || (filter === 'waypoint' && row.type === 'waypoint') || (filter === 'missingSet' && !setIds.has(row.parentSet)))
    .filter((row) => `${row.id} ${row.parentSet} ${row.displayName.en} ${row.displayName.de}`.toLowerCase().includes(query.toLowerCase()))
    .sort((a, b) => a.parentSet.localeCompare(b.parentSet) || a.type.localeCompare(b.type) || a.id.localeCompare(b.id)), [rows, filter, query, setIds]);
  const items = groupedItems(sortedRows, sets);
  const rowKey = (row: ExplorableRow) => `${row.type}:${row.id}`;
  const heights = items.map((item) => item.kind === 'group' ? GROUP_HEIGHT : rowKey(item.row) === expanded ? ROW_HEIGHT + EDITOR_HEIGHT : ROW_HEIGHT);
  const totalHeight = heights.reduce((sum, height) => sum + height, 0);
  let y = 0;
  const visible = items.map((item, index) => {
    const top = y;
    y += heights[index];
    return { item, index, top, height: heights[index] };
  }).filter((entry) => entry.top + entry.height >= scrollTop - 80 && entry.top <= scrollTop + viewportHeight + 80);
  const addRow = (type: ExplorableRow['type'], parentSet = '') => onChange([...rows, type === 'poi' ? starterPoi(rows, assets, parentSet) : type === 'lootChest' ? starterLoot(rows, assets, parentSet) : starterWaypoint(rows, assets, parentSet)]);
  const updateRow = (target: ExplorableRow, patch: Partial<ExplorableRow>) => onChange(rows.map((row) => row === target ? { ...row, ...patch } as ExplorableRow : row));
  const duplicateRow = (target: ExplorableRow) => onChange([...rows, { ...target, id: uniqueId(rows, `${target.id || target.type}_copy`) }]);
  const deleteRow = (target: ExplorableRow) => onChange(rows.filter((row) => row !== target));

  return (
    <Stack gap="sm">
      <Group justify="space-between">
        <Group gap="xs">
          <Select w={170} value={filter} data={[{ value: 'all', label: 'All' }, { value: 'poi', label: 'POIs' }, { value: 'lootChest', label: 'Loot chests' }, { value: 'waypoint', label: 'Waypoints' }, { value: 'missingSet', label: 'Invalid set' }]} onChange={(value) => setFilter(value || 'all')} />
          <TextInput w={260} placeholder="Filter entries" value={query} onChange={(event) => setQuery(event.currentTarget.value)} />
        </Group>
        <Group gap="xs">
          <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => addRow('poi')}>POI</Button>
          <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => addRow('lootChest')}>Loot chest</Button>
          <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => addRow('waypoint')}>Waypoint</Button>
        </Group>
      </Group>
      <Paper withBorder style={{ height: viewportHeight, overflow: 'auto', position: 'relative' }} onScroll={(event) => setScrollTop(event.currentTarget.scrollTop)}>
        <Box style={{ height: totalHeight, position: 'relative' }}>
          {visible.map(({ item, index, top, height }) => item.kind === 'group'
            ? <GroupHeader key={`group-${item.setId}-${index}`} top={top} height={height} setId={item.setId} rows={item.rows} disabled={disabled} onAddPoi={() => addRow('poi', item.setId)} onAddLoot={() => addRow('lootChest', item.setId)} onAddWaypoint={() => addRow('waypoint', item.setId)} />
            : <ExplorableVirtualRow key={`${rowKey(item.row)}-${item.index}`} top={top} height={height} row={item.row} expanded={expanded === rowKey(item.row)} sets={sets} disabled={disabled} assets={assets} setIds={setIds} onToggle={() => setExpanded(expanded === rowKey(item.row) ? '' : rowKey(item.row))} onUpdate={(patch) => updateRow(item.row, patch)} onDuplicate={() => duplicateRow(item.row)} onDelete={() => deleteRow(item.row)} />)}
        </Box>
      </Paper>
    </Stack>
  );
}

function GroupHeader({ top, height, setId, rows, disabled, onAddPoi, onAddLoot, onAddWaypoint }: { top: number; height: number; setId: string; rows: ExplorableRow[]; disabled: boolean; onAddPoi: () => void; onAddLoot: () => void; onAddWaypoint: () => void }) {
  return (
    <Group justify="space-between" px="sm" style={{ position: 'absolute', top, left: 0, right: 0, height, background: 'var(--mantine-color-default)', borderBottom: '1px solid var(--mantine-color-default-border)' }}>
      <Group gap="xs"><Text fw={700}>{setId || 'No set'}</Text><Badge size="sm" variant="light">{rows.length}</Badge><Badge size="sm" variant="outline">{rows.filter((row) => row.type === 'poi').length} POI</Badge><Badge size="sm" variant="outline">{rows.filter((row) => row.type === 'lootChest').length} loot</Badge><Badge size="sm" variant="outline">{rows.filter((row) => row.type === 'waypoint').length} waypoints</Badge></Group>
      <Group gap={4}>
        <Button size="compact-xs" variant="subtle" leftSection={<IconPlus size={12} />} disabled={disabled} onClick={onAddPoi}>POI</Button>
        <Button size="compact-xs" variant="subtle" leftSection={<IconPlus size={12} />} disabled={disabled} onClick={onAddLoot}>Loot</Button>
        <Button size="compact-xs" variant="subtle" leftSection={<IconPlus size={12} />} disabled={disabled} onClick={onAddWaypoint}>Waypoint</Button>
      </Group>
    </Group>
  );
}

function ExplorableVirtualRow({ top, height, row, expanded, sets, setIds, disabled, assets, onToggle, onUpdate, onDuplicate, onDelete }: {
  top: number; height: number; row: ExplorableRow; expanded: boolean; sets: SetRow[]; setIds: Set<string>; disabled: boolean; assets: AssetCatalog; onToggle: () => void; onUpdate: (patch: Partial<ExplorableRow>) => void; onDuplicate: () => void; onDelete: () => void;
}) {
  return (
    <Box style={{ position: 'absolute', top, left: 0, right: 0, minHeight: height, borderBottom: '1px solid var(--mantine-color-default-border)', background: 'var(--mantine-color-body)' }}>
      <Group h={ROW_HEIGHT} px="sm" wrap="nowrap" onClick={onToggle} style={{ cursor: 'pointer' }}>
        {expanded ? <IconChevronDown size={16} /> : <IconChevronRight size={16} />}
        <Badge w={94} variant="light" color={row.type === 'poi' ? 'blue' : row.type === 'lootChest' ? 'yellow' : 'teal'}>{row.type === 'poi' ? 'POI' : row.type === 'lootChest' ? 'Loot' : 'Waypoint'}</Badge>
        <Text w={220} fw={600} truncate>{row.id || '(new)'}</Text>
        <Text w={220} c={!setIds.has(row.parentSet) ? 'red' : undefined} truncate>{row.parentSet || 'No set'}</Text>
        <Text flex={1} c="dimmed" truncate>{row.displayName.en || row.displayName.de}</Text>
        <Text w={160} c="dimmed" size="xs" ta="right">{row.world} {formatCoord(row.x)}, {formatCoord(row.y)}, {formatCoord(row.z)}</Text>
        <Tooltip label="Duplicate"><ActionIcon variant="subtle" disabled={disabled} onClick={(event) => { event.stopPropagation(); onDuplicate(); }}><IconCopy size={16} /></ActionIcon></Tooltip>
        <Tooltip label="Delete"><ActionIcon variant="subtle" color="red" disabled={disabled} onClick={(event) => { event.stopPropagation(); onDelete(); }}><IconTrash size={16} /></ActionIcon></Tooltip>
      </Group>
      {expanded && <Box px="md" pb="md"><ExplorableEditor row={row} sets={sets} disabled={disabled} assets={assets} onUpdate={onUpdate} /></Box>}
    </Box>
  );
}

function ExplorableEditor({ row, sets, disabled, assets, onUpdate }: { row: ExplorableRow; sets: SetRow[]; disabled: boolean; assets: AssetCatalog; onUpdate: (patch: Partial<ExplorableRow>) => void }) {
  return (
    <Stack gap="sm">
      <Group grow align="start">
        <TextInput label="id" value={row.id} disabled={disabled} error={!row.id.trim()} onChange={(event) => onUpdate({ id: event.currentTarget.value })} />
        <SetSelector label="parentSet" value={row.parentSet} sets={sets} disabled={disabled} error={!row.parentSet.trim()} onChange={(parentSet) => onUpdate({ parentSet })} />
        {row.type === 'poi' && <NumberInput label="radius" value={row.radius} disabled={disabled} min={0} onChange={(radius) => onUpdate({ radius: Number(radius || 0) })} />}
        {row.type === 'waypoint' && <NumberInput label="cooldown" value={row.cooldown} disabled={disabled} min={0} onChange={(cooldown) => onUpdate({ cooldown: Number(cooldown || 0) } as Partial<ExplorableRow>)} />}
      </Group>
      <LocalizedMiniMessageEditor label="displayName" value={row.displayName} compact disabled={disabled} onChange={(displayName) => onUpdate({ displayName })} />
      {row.type === 'poi'
        ? <LocalizedMiniMessageEditor label="flavourText" value={row.flavourText} disabled={disabled} onChange={(flavourText) => onUpdate({ flavourText } as Partial<ExplorableRow>)} />
        : <LocalizedMiniMessageEditor label="description" value={row.description} disabled={disabled} onChange={(description) => onUpdate({ description } as Partial<ExplorableRow>)} />}
      <LocationFields row={row} disabled={disabled} assets={assets} onChange={onUpdate} />
      {row.type === 'lootChest' && row.loot && <Alert color="yellow" variant="light">This chest has command-captured base64 loot. Hermes item rows are added in addition to that loot.</Alert>}
      {row.type === 'lootChest' && <LootItemsTable rows={row.lootItems} disabled={disabled} assets={assets} onChange={(lootItems) => onUpdate({ lootItems } as Partial<ExplorableRow>)} />}
      {row.type === 'waypoint' && <WaypointFields row={row} disabled={disabled} assets={assets} onChange={onUpdate} />}
    </Stack>
  );
}

function WaypointFields({ row, disabled, assets, onChange }: { row: WaypointRow; disabled: boolean; assets: AssetCatalog; onChange: (patch: Partial<ExplorableRow>) => void }) {
  return (
    <Group grow align="start">
      <Select label="unlockMode" value={row.unlockMode} disabled={disabled} data={['NEAR', 'ACTION', 'QUEST']} onChange={(unlockMode) => onChange({ unlockMode: unlockMode || 'NEAR' } as Partial<ExplorableRow>)} />
      <Select label="useMode" value={row.useMode} disabled={disabled} data={['NEAREST', 'LAST']} onChange={(useMode) => onChange({ useMode: useMode || 'NEAREST' } as Partial<ExplorableRow>)} />
      <ValueField label="quest" value={row.quest} kind="quest" disabled={disabled} assets={assets} onChange={(quest) => onChange({ quest: String(quest || '') } as Partial<ExplorableRow>)} />
      <NumberInput label="yaw" value={row.yaw} disabled={disabled} onChange={(yaw) => onChange({ yaw: Number(yaw || 0) } as Partial<ExplorableRow>)} />
      <NumberInput label="pitch" value={row.pitch} disabled={disabled} onChange={(pitch) => onChange({ pitch: Number(pitch || 0) } as Partial<ExplorableRow>)} />
    </Group>
  );
}

function SetsTable({ rows, explorables, disabled, onChange }: { rows: SetRow[]; explorables: ExplorableRow[]; disabled: boolean; onChange: (rows: SetRow[]) => void }) {
  const [expanded, setExpanded] = useState('');
  const ordered = useMemo(() => orderSets(rows), [rows]);
  const updateRow = (target: SetRow, patch: Partial<SetRow>) => onChange(rows.map((row) => row === target ? { ...row, ...patch } : row));
  const add = (parentSet = '') => onChange([...rows, { id: uniqueId(rows, 'exploration_set'), parentSet, displayName: { en: 'New exploration set', de: '' }, description: { en: '', de: '' }, extras: {} }]);
  return (
    <Stack gap="sm">
      <Group justify="space-between"><Text fw={700}>Exploration Sets</Text><Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => add()}>Set</Button></Group>
      <Paper withBorder style={{ background: 'var(--mantine-color-body)', overflow: 'hidden' }}>
        <Table.ScrollContainer minWidth={900}>
          <Table withTableBorder>
            <Table.Thead><Table.Tr style={{ background: 'var(--mantine-color-default)' }}><Table.Th>Set</Table.Th><Table.Th>Parent</Table.Th><Table.Th>Entries</Table.Th><Table.Th>Display</Table.Th><Table.Th w={120} /></Table.Tr></Table.Thead>
            <Table.Tbody>
              {ordered.map(({ row, depth }) => (
                <Fragment key={row.id || `set-${depth}`}>
                <Table.Tr key={`${row.id}-summary`} style={{ background: 'var(--mantine-color-body)' }}>
                  <Table.Td><Group gap="xs" pl={depth * 18}><ActionIcon size="sm" variant="subtle" onClick={() => setExpanded(expanded === row.id ? '' : row.id)}>{expanded === row.id ? <IconChevronDown size={14} /> : <IconChevronRight size={14} />}</ActionIcon><Text fw={600}>{row.id || '(new)'}</Text></Group></Table.Td>
                  <Table.Td>{row.parentSet || '-'}</Table.Td>
                  <Table.Td>{explorables.filter((entry) => entry.parentSet === row.id).length}</Table.Td>
                  <Table.Td>{row.displayName.en || row.displayName.de}</Table.Td>
                  <Table.Td>
                    <Group gap={4} wrap="nowrap">
                      <Tooltip label="Add child"><ActionIcon variant="subtle" disabled={disabled || !row.id} onClick={() => add(row.id)}><IconPlus size={16} /></ActionIcon></Tooltip>
                      <Tooltip label="Duplicate"><ActionIcon variant="subtle" disabled={disabled} onClick={() => onChange([...rows, { ...row, id: uniqueId(rows, `${row.id || 'set'}_copy`) }])}><IconCopy size={16} /></ActionIcon></Tooltip>
                      <Tooltip label="Delete"><ActionIcon variant="subtle" color="red" disabled={disabled} onClick={() => onChange(rows.filter((item) => item !== row))}><IconTrash size={16} /></ActionIcon></Tooltip>
                    </Group>
                  </Table.Td>
                </Table.Tr>
                {expanded === row.id && <Table.Tr key={`${row.id}-editor`} style={{ background: 'var(--mantine-color-body)' }}><Table.Td colSpan={5}><SetEditor row={row} rows={rows} disabled={disabled} onUpdate={(patch) => updateRow(row, patch)} /></Table.Td></Table.Tr>}
              </Fragment>
            ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Paper>
    </Stack>
  );
}

function SetEditor({ row, rows, disabled, onUpdate }: { row: SetRow; rows: SetRow[]; disabled: boolean; onUpdate: (patch: Partial<SetRow>) => void }) {
  return (
    <Stack gap="sm" p="sm">
      <Group grow align="start">
        <TextInput label="id" value={row.id} disabled={disabled} error={!row.id.trim()} onChange={(event) => onUpdate({ id: event.currentTarget.value })} />
        <Select label="parentSet" value={row.parentSet || null} clearable searchable disabled={disabled} data={parentOptions(rows, row.id)} onChange={(parentSet) => onUpdate({ parentSet: parentSet || '' })} />
      </Group>
      <LocalizedMiniMessageEditor label="displayName" value={row.displayName} compact disabled={disabled} onChange={(displayName) => onUpdate({ displayName })} />
      <LocalizedMiniMessageEditor label="description" value={row.description} disabled={disabled} onChange={(description) => onUpdate({ description })} />
    </Stack>
  );
}

function LocationFields({ row, disabled, assets, onChange }: { row: { world: string; x: number; y: number; z: number }; disabled: boolean; assets: AssetCatalog; onChange: (patch: Partial<ExplorableRow>) => void }) {
  return <Group grow align="start"><ValueField label="world" value={row.world} kind="world" disabled={disabled} assets={assets} onChange={(world) => onChange({ world: String(world || defaultWorld(assets)) })} /><NumberInput label="x" value={row.x} disabled={disabled} onChange={(x) => onChange({ x: Number(x || 0) })} /><NumberInput label="y" value={row.y} disabled={disabled} onChange={(y) => onChange({ y: Number(y || 0) })} /><NumberInput label="z" value={row.z} disabled={disabled} onChange={(z) => onChange({ z: Number(z || 0) })} /></Group>;
}

function LootItemsTable({ rows, disabled, assets, onChange }: { rows: LootItemRow[]; disabled: boolean; assets: AssetCatalog; onChange: (rows: LootItemRow[]) => void }) {
  return (
    <Box>
      <Group justify="space-between" mb="xs"><Text fw={700}>Hermes loot</Text><Button size="xs" variant="light" disabled={disabled} leftSection={<IconPlus size={14} />} onClick={() => onChange([...rows, { item: '', chance: 100 }])}>Item</Button></Group>
      <Table.ScrollContainer minWidth={620}><Table withTableBorder withColumnBorders><Table.Thead><Table.Tr><Table.Th>Hephaestus item</Table.Th><Table.Th w={140}>chance %</Table.Th><Table.Th w={70} /></Table.Tr></Table.Thead><Table.Tbody>
        {rows.map((row, index) => <Table.Tr key={index}><Table.Td><ValueField label="item" value={row.item} kind="item" disabled={disabled} assets={assets} onChange={(item) => onChange(rows.map((entry, rowIndex) => rowIndex === index ? { ...entry, item: String(item || '') } : entry))} /></Table.Td><Table.Td><NumberInput value={row.chance} disabled={disabled} min={0} max={100} onChange={(chance) => onChange(rows.map((entry, rowIndex) => rowIndex === index ? { ...entry, chance: clampChance(Number(chance || 0)) } : entry))} /></Table.Td><Table.Td><ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onChange(rows.filter((_, rowIndex) => rowIndex !== index))}><IconTrash size={16} /></ActionIcon></Table.Td></Table.Tr>)}
      </Table.Tbody></Table></Table.ScrollContainer>
    </Box>
  );
}

function SetSelector({ label, value, sets, disabled, error, onChange }: { label: string; value: string; sets: SetRow[]; disabled: boolean; error?: boolean; onChange: (value: string) => void }) {
  return <Select label={label} value={value || null} clearable searchable disabled={disabled} error={error} data={sets.map((set) => ({ value: set.id, label: set.id }))} onChange={(next) => onChange(next || '')} />;
}

function parseExplorables(yaml: string) {
  try {
    const data = parseDocument(yaml || '{}').toJS() as Record<string, unknown> | null;
    return { rows: [...stringList(data?.pointsOfInterest).map(qlineToPoi), ...stringList(data?.lootChests).map(qlineToLootChest)] };
  } catch {
    return { rows: [] as ExplorableRow[] };
  }
}

function qlineToPoi(text: string): PoiRow {
  const parsed = parseQLine(text);
  return { type: 'poi', id: parsed.id, parentSet: parsed.values.parentSet || '', displayName: localizedFromRecord(parsed.values, 'displayName'), flavourText: localizedFromRecord(parsed.values, 'flavourText'), world: parsed.values['location.world'] || 'Erethon', x: Number(parsed.values['location.x'] || 0), y: Number(parsed.values['location.y'] || 0), z: Number(parsed.values['location.z'] || 0), radius: Number(parsed.values.radius || 3), extras: omitLocalized(parsed.values, ['parentSet', 'displayName', 'flavourText', 'location.world', 'location.x', 'location.y', 'location.z', 'radius']) };
}

function qlineToLootChest(text: string): LootRow {
  const parsed = parseQLine(text);
  return { type: 'lootChest', id: parsed.id, parentSet: parsed.values.parentSet || '', displayName: localizedFromRecord(parsed.values, 'displayName'), description: localizedFromRecord(parsed.values, 'description'), world: parsed.values['location.world'] || 'Erethon', x: Number(parsed.values['location.x'] || 0), y: Number(parsed.values['location.y'] || 0), z: Number(parsed.values['location.z'] || 0), loot: parsed.values.loot || '', lootItems: parseLootItems(parsed.values.lootItems || ''), extras: omitLocalized(parsed.values, ['parentSet', 'displayName', 'description', 'location.world', 'location.x', 'location.y', 'location.z', 'loot', 'lootItems']) };
}

function serializeExplorables(rows: ExplorableRow[]) {
  return stringify({
    pointsOfInterest: rows.filter((row): row is PoiRow => row.type === 'poi').map((row) => serializeQLine(row.id, { ...row.extras, parentSet: row.parentSet, ...localizedToQLine('displayName', row.displayName), ...localizedToQLine('flavourText', row.flavourText), 'location.x': row.x, 'location.y': row.y, 'location.z': row.z, 'location.world': row.world, radius: row.radius })),
    lootChests: rows.filter((row): row is LootRow => row.type === 'lootChest').map((row) => serializeQLine(row.id, { ...row.extras, parentSet: row.parentSet, ...localizedToQLine('displayName', row.displayName), ...localizedToQLine('description', row.description), 'location.x': row.x, 'location.y': row.y, 'location.z': row.z, 'location.world': row.world, loot: row.loot, lootItems: serializeLootItems(row.lootItems) }))
  }, { lineWidth: 0 });
}

function parseRespawns(yaml: string): WaypointRow[] {
  try {
    const data = parseDocument(yaml || '{}').toJS() as Record<string, unknown> | null;
    return Object.entries(asRecord(data)).map(([id, raw]) => sectionToWaypoint(id, asRecord(raw)));
  } catch {
    return [];
  }
}

function sectionToWaypoint(id: string, section: Record<string, unknown>): WaypointRow {
  const location = asRecord(section.location);
  return {
    type: 'waypoint',
    id,
    parentSet: String(section.set || ''),
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
    extras: omitUnknown(section, ['location', 'displayName', 'description', 'cooldown', 'unlockMode', 'useMode', 'quest', 'set'])
  };
}

function serializeRespawns(rows: WaypointRow[]) {
  return stringify(Object.fromEntries(rows.filter((row) => row.id.trim()).map((row) => [row.id.trim(), {
    ...row.extras,
    location: { '==': 'org.bukkit.Location', world: row.world, x: row.x, y: row.y, z: row.z, pitch: row.pitch, yaw: row.yaw },
    displayName: localizedToYaml(row.displayName),
    description: localizedToYaml(row.description),
    cooldown: row.cooldown,
    unlockMode: row.unlockMode,
    useMode: row.useMode,
    quest: row.quest || null,
    set: row.parentSet || null
  }])), { lineWidth: 0 });
}

function parseSets(yaml: string): SetRow[] {
  try {
    const data = parseDocument(yaml || '{}').toJS() as Record<string, unknown> | null;
    return stringList(data?.sets).map((text) => {
      const parsed = parseQLine(text);
      return { id: parsed.id, parentSet: parsed.values.parentSet || '', displayName: localizedFromRecord(parsed.values, 'displayName', parsed.id), description: localizedFromRecord(parsed.values, 'description'), extras: omitLocalized(parsed.values, ['parentSet', 'displayName', 'description']) };
    });
  } catch {
    return [];
  }
}

function serializeSets(rows: SetRow[]) {
  return stringify({ sets: rows.filter((row) => row.id.trim()).map((row) => serializeQLine(row.id, { ...row.extras, ...localizedToQLine('displayName', row.displayName), ...localizedToQLine('description', row.description), parentSet: row.parentSet })) }, { lineWidth: 0 });
}

function groupedItems(rows: ExplorableRow[], sets: SetRow[]): VirtualItem[] {
  const labels = new Set([...sets.map((set) => set.id), ...rows.map((row) => row.parentSet || '')]);
  return Array.from(labels).sort().flatMap((setId) => {
    const groupRows = rows.filter((row) => (row.parentSet || '') === setId);
    return groupRows.length ? [{ kind: 'group' as const, setId, rows: groupRows }, ...groupRows.map((row, index) => ({ kind: 'row' as const, row, index }))] : [];
  });
}

function orderSets(rows: SetRow[]) {
  const byParent = new Map<string, SetRow[]>();
  rows.forEach((row) => byParent.set(row.parentSet || '', [...(byParent.get(row.parentSet || '') || []), row]));
  const result: Array<{ row: SetRow; depth: number }> = [];
  const visit = (parent: string, depth: number, seen = new Set<string>()) => {
    for (const row of (byParent.get(parent) || []).sort((a, b) => a.id.localeCompare(b.id))) {
      if (seen.has(row.id)) continue;
      result.push({ row, depth });
      visit(row.id, depth + 1, new Set([...seen, row.id]));
    }
  };
  visit('', 0);
  rows.filter((row) => !result.some((entry) => entry.row === row)).forEach((row) => result.push({ row, depth: 0 }));
  return result;
}

function parentOptions(rows: SetRow[], id: string) {
  const excluded = new Set([id, ...descendants(rows, id)]);
  return rows.filter((row) => row.id && !excluded.has(row.id)).map((row) => ({ value: row.id, label: row.id }));
}

function descendants(rows: SetRow[], id: string): string[] {
  const children = rows.filter((row) => row.parentSet === id).map((row) => row.id);
  return children.flatMap((child) => [child, ...descendants(rows, child)]);
}

function parseQLine(text: string): QLine {
  const trimmed = text.trim();
  const firstSpace = trimmed.indexOf(' ');
  const firstEquals = trimmed.indexOf('=');
  const id = firstSpace >= 0 && (firstEquals < 0 || firstSpace < firstEquals) ? trimmed.slice(0, firstSpace) : trimmed;
  const body = firstSpace >= 0 && (firstEquals < 0 || firstSpace < firstEquals) ? trimmed.slice(firstSpace + 1) : '';
  const values: Record<string, string> = {};
  body.split(';').forEach((part) => {
    const index = part.indexOf('=');
    if (index > 0) values[part.slice(0, index).trim()] = part.slice(index + 1).trim();
  });
  return { id, values };
}

function serializeQLine(id: string, values: Record<string, unknown>) {
  const entries = Object.entries(values).filter(([, value]) => value !== undefined && value !== null && String(value) !== '').map(([key, value]) => `${key}=${String(value)}`);
  return `${id.trim()}${entries.length ? ` ${entries.join(';')}` : ''}`;
}

function parseLootItems(value: string): LootItemRow[] {
  return value.split(',').map((entry) => entry.trim()).filter(Boolean).map((entry) => {
    const separator = entry.lastIndexOf(':');
    const chance = separator > 0 ? Number(entry.slice(separator + 1)) : 100;
    return { item: separator > 0 && !Number.isNaN(chance) ? entry.slice(0, separator) : entry, chance: clampChance(Number.isNaN(chance) ? 100 : chance) };
  });
}

function serializeLootItems(rows: LootItemRow[]) {
  return rows.filter((row) => row.item.trim()).map((row) => `${row.item.trim()}:${clampChance(row.chance)}`).join(',');
}

function validateExplorables(rows: ExplorableRow[], sets: SetRow[], assets: AssetCatalog) {
  const errors: string[] = [];
  const ids = new Set<string>();
  const setIds = new Set(sets.map((set) => set.id).filter(Boolean));
  rows.forEach((row) => {
    if (!row.id.trim()) errors.push(`${row.type}: missing id`);
    if (row.id && ids.has(`${row.type}:${row.id}`)) errors.push(`${row.type}: duplicate id ${row.id}`);
    ids.add(`${row.type}:${row.id}`);
    if (!row.parentSet.trim()) errors.push(`${row.id || '(new)'}: missing parentSet`);
    if (row.parentSet && !setIds.has(row.parentSet)) errors.push(`${row.id || '(new)'}: unknown parentSet ${row.parentSet}`);
    if (!row.world.trim() || [row.x, row.y, row.z].some((value) => Number.isNaN(value))) errors.push(`${row.id || '(new)'}: invalid location`);
  });
  const itemIds = new Set((assets.items || []).map((item) => item.id));
  rows.filter((row): row is LootRow => row.type === 'lootChest').forEach((chest) => chest.lootItems.forEach((item) => {
    if (!item.item.trim()) errors.push(`Loot chest ${chest.id}: missing item key`);
    if ((assets.items || []).length > 0 && item.item && !itemIds.has(item.item)) errors.push(`Loot chest ${chest.id}: ${item.item} is not in the Hephaestus item catalog`);
  }));
  return errors;
}

function starterPoi(rows: ExplorableRow[], assets: AssetCatalog, parentSet = ''): PoiRow {
  return { type: 'poi', id: uniqueId(rows, 'poi'), parentSet, displayName: { en: '<green>New point of interest', de: '' }, flavourText: { en: '', de: '' }, world: defaultWorld(assets), x: 0, y: 64, z: 0, radius: 3, extras: {} };
}

function starterLoot(rows: ExplorableRow[], assets: AssetCatalog, parentSet = ''): LootRow {
  return { type: 'lootChest', id: uniqueId(rows, 'loot_chest'), parentSet, displayName: { en: '<gold>Loot Chest', de: '' }, description: { en: '', de: '' }, world: defaultWorld(assets), x: 0, y: 64, z: 0, loot: '', lootItems: [], extras: {} };
}

function starterWaypoint(rows: ExplorableRow[], assets: AssetCatalog, parentSet = ''): WaypointRow {
  return { type: 'waypoint', id: uniqueId(rows, 'waypoint'), parentSet, displayName: { en: '<green>New waypoint', de: '' }, description: { en: '', de: '' }, world: defaultWorld(assets), x: 0, y: 64, z: 0, yaw: 0, pitch: 0, cooldown: 0, unlockMode: 'NEAR', useMode: 'NEAREST', quest: '', extras: {} };
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String) : [];
}

function asRecord(value: unknown): Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : {};
}

function omitUnknown(source: Record<string, unknown>, keys: string[]) {
  return Object.fromEntries(Object.entries(source).filter(([key]) => !keys.includes(key)));
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

function formatCoord(value: number) {
  return Number.isFinite(value) ? String(Math.round(value)) : '0';
}

function clampChance(value: number) {
  return Math.max(0, Math.min(100, Math.round(value)));
}
