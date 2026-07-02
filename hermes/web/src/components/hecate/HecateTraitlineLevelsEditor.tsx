import { useState } from 'react';
import { ActionIcon, Box, Button, Group, NumberInput, Paper, Select, Stack, Table, Text, TextInput } from '@mantine/core';
import { IconCopy, IconPlus, IconTrash } from '@tabler/icons-react';
import type { AssetCatalog, AssetOption } from '../../api/types';
import { ValueField } from '../fields/ValueField';

type LevelRow = { level: string; message: Record<string, unknown>; attributes: AttributeRow[]; extras: Record<string, unknown> };
type AttributeRow = { id: string; value?: number; min?: number; max?: number; extras: Record<string, unknown> };

export function HecateTraitlineLevelsEditor({ value, assets, disabled, onChange }: { value: unknown; assets: AssetCatalog; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  const levels = normalizeLevels(value);
  const [activeLevel, setActiveLevel] = useState('');
  const activeIndex = Math.max(0, levels.findIndex((level) => level.level === activeLevel));
  const selectedLevel = levels[activeIndex];
  const commit = (rows: LevelRow[]) => onChange(serializeLevels(rows));
  const update = (index: number, row: LevelRow) => commit(levels.map((item, itemIndex) => itemIndex === index ? row : item));
  const addLevel = () => {
    const nextLevel = nextNumericKey(levels.map((level) => level.level), '1');
    setActiveLevel(nextLevel);
    commit([...levels, { level: nextLevel, message: {}, attributes: [], extras: {} }]);
  };
  return (
    <Stack gap="md">
      <Group justify="space-between">
        <Box>
          <Text fw={700}>Character levels</Text>
          <Text size="sm" c="dimmed">Per-level Hecate attribute changes. Unsupported level fields stay in extras.</Text>
        </Box>
        <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={addLevel}>Level</Button>
      </Group>
      {levels.length > 0 && (
        <div style={{ display: 'grid', gridTemplateColumns: '180px minmax(0, 1fr)', gap: 16, alignItems: 'start' }}>
          <Paper p="sm" withBorder className="slotBox">
            <Text fw={700} size="sm" mb="xs">Levels</Text>
            <Stack gap={6}>
              {levels.map((level, index) => (
                <button
                  key={level.level}
                  type="button"
                  className={`aetherNavButton ${index === activeIndex ? 'active' : ''}`}
                  onClick={() => setActiveLevel(level.level)}
                >
                  <Group justify="space-between" wrap="nowrap" gap={6}>
                    <Text size="sm" fw={700}>Lv {level.level}</Text>
                    <Text size="xs" c="dimmed" truncate>{levelOverviewTotal(levels, index)}</Text>
                  </Group>
                </button>
              ))}
            </Stack>
          </Paper>
          {selectedLevel && (
            <LevelCard
              key={`${selectedLevel.level}-${activeIndex}`}
              level={selectedLevel}
              levels={levels}
              index={activeIndex}
              assets={assets}
              disabled={disabled}
              totals={activeIndex > 0 ? cumulativeTotals(levels, activeIndex) : {}}
              onChange={(next) => {
                setActiveLevel(next.level);
                update(activeIndex, next);
              }}
              onDelete={() => {
                const next = levels.filter((_, itemIndex) => itemIndex !== activeIndex);
                setActiveLevel(next[Math.max(0, activeIndex - 1)]?.level || '');
                commit(next);
              }}
              onDuplicate={() => {
                const nextLevel = nextNumericKey(levels.map((row) => row.level), String(Number(selectedLevel.level || 0) + 1 || 1));
                setActiveLevel(nextLevel);
                commit([...levels, { ...clone(selectedLevel), level: nextLevel }]);
              }}
            />
          )}
        </div>
      )}
      {levels.length === 0 && <Text size="sm" c="dimmed">No character levels configured.</Text>}
    </Stack>
  );
}

function LevelCard({ level, levels, index, assets, disabled, totals, onChange, onDelete, onDuplicate }: { level: LevelRow; levels: LevelRow[]; index: number; assets: AssetCatalog; disabled: boolean; totals: Record<string, string>; onChange: (level: LevelRow) => void; onDelete: () => void; onDuplicate: () => void }) {
  const [levelDraft, setLevelDraft] = useState(level.level);
  const normalizedDraft = normalizeLevelKey(levelDraft);
  const levelError = !normalizedDraft
    ? 'Level must be a number'
    : levels.some((row, rowIndex) => rowIndex !== index && normalizeLevelKey(row.level) === normalizedDraft)
      ? 'Duplicate level'
      : undefined;
  const commitLevel = () => {
    if (levelError || normalizedDraft === level.level) {
      setLevelDraft(level.level);
      return;
    }
    onChange({ ...level, level: normalizedDraft });
  };
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm">
        <Text fw={700}>Level {level.level}</Text>
        <Group gap={4}>
          <ActionIcon variant="subtle" disabled={disabled} onClick={onDuplicate}><IconCopy size={16} /></ActionIcon>
          <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={onDelete}><IconTrash size={16} /></ActionIcon>
        </Group>
      </Group>
      <TextInput
        label="Level"
        value={levelDraft}
        disabled={disabled}
        inputMode="numeric"
        error={levelError}
        maw={220}
        onChange={(event) => setLevelDraft(event.currentTarget.value.replace(/[^\d]/g, ''))}
        onBlur={commitLevel}
        onKeyDown={(event) => {
          if (event.key === 'Enter') event.currentTarget.blur();
          if (event.key === 'Escape') setLevelDraft(level.level);
        }}
      />
      <Box mt="md">
        <ValueField label="message" description="Localized unlock or level-up message. Hecate registers this as characterlevel.<traitline>.<level>." value={translatableFrom(level.message)} kind="translatable" disabled={disabled} onChange={(value) => onChange({ ...level, message: translatableMap(String(value || '')) })} />
      </Box>
      <Box mt="md">
        <AttributeTable value={level.attributes} assets={assets} disabled={disabled} totals={totals} onChange={(attributes) => onChange({ ...level, attributes })} />
      </Box>
      {Object.keys(level.extras).length > 0 && (
        <Box mt="md">
          <ValueField label="Level extras" value={level.extras} kind="raw" disabled={disabled} onChange={(value) => onChange({ ...level, extras: asRecord(value) })} />
        </Box>
      )}
    </Paper>
  );
}

function AttributeTable({ value, assets, disabled, totals, onChange }: { value: AttributeRow[]; assets: AssetCatalog; disabled: boolean; totals: Record<string, string>; onChange: (rows: AttributeRow[]) => void }) {
  const update = (index: number, row: AttributeRow) => onChange(value.map((item, itemIndex) => itemIndex === index ? row : item));
  const showTotals = Object.keys(totals).length > 0;
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm">
        <Text fw={700}>Attributes</Text>
        <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => onChange([...value, { id: 'max_health', value: 1, extras: {} }])}>Attribute</Button>
      </Group>
      <Table withTableBorder withColumnBorders>
        <Table.Thead><Table.Tr><Table.Th>Attribute</Table.Th><Table.Th w={120}>Mode</Table.Th><Table.Th w={130}>Value</Table.Th><Table.Th w={130}>Min</Table.Th><Table.Th w={130}>Max</Table.Th>{showTotals && <Table.Th w={150}>Total</Table.Th>}<Table.Th w={80}>Actions</Table.Th></Table.Tr></Table.Thead>
        <Table.Tbody>
          {value.map((row, index) => {
            const mode = row.min != null || row.max != null ? 'range' : 'value';
            return (
              <Table.Tr key={`${row.id}-${index}`}>
                <Table.Td><PickerField value={row.id} options={assets.attributes || []} disabled={disabled} onChange={(id) => update(index, { ...row, id: stripMinecraft(id) })} /></Table.Td>
                <Table.Td><Select data={['value', 'range']} value={mode} disabled={disabled} onChange={(next) => update(index, next === 'range' ? { ...row, value: undefined, min: row.value ?? row.min ?? 0, max: row.value ?? row.max ?? 0 } : { ...row, value: row.min ?? row.value ?? 0, min: undefined, max: undefined })} /></Table.Td>
                <Table.Td><NumberInput value={row.value ?? ''} disabled={disabled || mode !== 'value'} onChange={(next) => update(index, { ...row, value: Number(next || 0) })} /></Table.Td>
                <Table.Td><NumberInput value={row.min ?? ''} disabled={disabled || mode !== 'range'} onChange={(next) => update(index, { ...row, min: Number(next || 0) })} /></Table.Td>
                <Table.Td><NumberInput value={row.max ?? ''} disabled={disabled || mode !== 'range'} onChange={(next) => update(index, { ...row, max: Number(next || 0) })} /></Table.Td>
                {showTotals && <Table.Td><Text size="sm" fw={600}>{totals[stripMinecraft(row.id)] || '-'}</Text></Table.Td>}
                <Table.Td><ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onChange(value.filter((_, itemIndex) => itemIndex !== index))}><IconTrash size={16} /></ActionIcon></Table.Td>
              </Table.Tr>
            );
          })}
          {value.length === 0 && <Table.Tr><Table.Td colSpan={showTotals ? 7 : 6}><Text size="sm" c="dimmed">No attributes configured.</Text></Table.Td></Table.Tr>}
        </Table.Tbody>
      </Table>
    </Paper>
  );
}

function levelOverviewTotal(levels: LevelRow[], index: number) {
  const first = levels[index]?.attributes[0];
  if (!first?.id) return '-';
  const totals = cumulativeTotals(levels, index);
  return totals[stripMinecraft(first.id)] || attributeValueText(first);
}

function cumulativeTotals(levels: LevelRow[], activeIndex: number) {
  const totals = new Map<string, { min: number; max: number; range: boolean }>();
  for (let index = 0; index <= activeIndex; index++) {
    for (const attribute of levels[index]?.attributes || []) {
      const id = stripMinecraft(attribute.id);
      if (!id) continue;
      const existing = totals.get(id) || { min: 0, max: 0, range: false };
      if (attribute.min != null || attribute.max != null) {
        existing.min += Number(attribute.min ?? 0);
        existing.max += Number(attribute.max ?? attribute.min ?? 0);
        existing.range = true;
      } else {
        const value = Number(attribute.value ?? 0);
        existing.min += value;
        existing.max += value;
      }
      totals.set(id, existing);
    }
  }
  return Object.fromEntries(Array.from(totals.entries()).map(([id, total]) => [id, total.range || total.min !== total.max ? `${formatNumber(total.min)} - ${formatNumber(total.max)}` : formatNumber(total.min)]));
}

function attributeValueText(attribute: AttributeRow) {
  if (attribute.min != null || attribute.max != null) return `${formatNumber(attribute.min ?? 0)}-${formatNumber(attribute.max ?? attribute.min ?? 0)}`;
  return formatNumber(attribute.value ?? 0);
}

function formatNumber(value: number) {
  return Number.isInteger(value) ? String(value) : String(Number(value.toFixed(3)));
}

function PickerField({ value, options, disabled, onChange }: { value: unknown; options: AssetOption[]; disabled: boolean; onChange: (value: unknown) => void }) {
  const current = String(value ?? '');
  const data = uniqueOptions(options).map((option) => ({ value: option.id, label: option.label || option.displayName || option.id }));
  const withCurrent = current && !data.some((option) => option.value === current) ? [...data, { value: current, label: current }] : data;
  if (withCurrent.length === 0) return <TextInput value={current} disabled={disabled} placeholder="attribute" onChange={(event) => onChange(event.currentTarget.value)} />;
  return <Select value={current} data={withCurrent} disabled={disabled} searchable clearable nothingFoundMessage="No matches" onChange={(next) => onChange(next || '')} />;
}

function normalizeLevels(value: unknown): LevelRow[] {
  return Object.entries(asRecord(value)).map(([level, raw]) => {
    const row = asRecord(raw);
    return {
      level,
      message: asRecord(row.message),
      attributes: normalizeAttributes(row.attributes),
      extras: omit(row, ['attributes', 'message'])
    };
  }).sort((left, right) => Number(left.level) - Number(right.level));
}

function serializeLevels(rows: LevelRow[]) {
  return Object.fromEntries(rows.map((row) => {
    const body: Record<string, unknown> = { ...row.extras };
    if (Object.keys(row.message).length > 0) body.message = row.message;
    if (row.attributes.length > 0) body.attributes = serializeAttributes(row.attributes);
    return [row.level, body];
  }));
}

function normalizeAttributes(value: unknown): AttributeRow[] {
  return Object.entries(asRecord(value)).map(([key, raw]) => {
    if (typeof raw === 'number') return { id: key, value: raw, extras: {} };
    const row = asRecord(raw);
    return {
      id: String(row.id || key),
      value: row.value == null ? undefined : Number(row.value),
      min: row.min == null ? undefined : Number(row.min),
      max: row.max == null ? undefined : Number(row.max),
      extras: omit(row, ['id', 'value', 'min', 'max'])
    };
  });
}

function serializeAttributes(rows: AttributeRow[]) {
  return Object.fromEntries(rows.map((row) => {
    if (row.min == null && row.max == null && Object.keys(row.extras).length === 0) return [stripMinecraft(row.id), row.value ?? 0];
    const body: Record<string, unknown> = { ...row.extras };
    if (row.min != null || row.max != null) {
      body.min = row.min ?? 0;
      body.max = row.max ?? row.min ?? 0;
    } else {
      body.value = row.value ?? 0;
    }
    return [stripMinecraft(row.id), body];
  }));
}

function normalizeLevelKey(value: string) {
  const trimmed = String(value ?? '').trim();
  if (!/^\d+$/.test(trimmed)) return '';
  return String(Number(trimmed));
}

function nextNumericKey(existing: string[], base: string) {
  let index = Number(base || 1);
  while (existing.includes(String(index))) index++;
  return String(index);
}

function asRecord(value: unknown): Record<string, any> {
  return value && typeof value === 'object' && !Array.isArray(value) ? value as Record<string, any> : {};
}

function omit(data: Record<string, any>, keys: string[]) {
  return Object.fromEntries(Object.entries(data).filter(([key]) => !keys.includes(key)));
}

function stripMinecraft(value: unknown) {
  const text = String(value ?? '');
  return text.startsWith('minecraft:') ? text.slice('minecraft:'.length) : text;
}

function translatableFrom(value: unknown) {
  const record = asRecord(value);
  if (record.en != null || record.de != null) return `en=${String(record.en ?? '')}; de=${String(record.de ?? '')}`;
  return String(value ?? '');
}

function translatableMap(value: string) {
  const result: Record<string, string> = {};
  for (const part of value.split(';')) {
    const index = part.indexOf('=');
    if (index > 0) result[part.slice(0, index).trim()] = part.slice(index + 1).trim();
  }
  if (!result.en && !result.de && value.trim()) result.en = value.trim();
  return result;
}

function uniqueOptions(options: AssetOption[]) {
  const byId = new Map<string, AssetOption>();
  for (const option of options || []) {
    if (option.id && !byId.has(option.id)) byId.set(option.id, option);
  }
  return Array.from(byId.values());
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value ?? {}));
}
