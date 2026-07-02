import { useEffect, useMemo, useState } from 'react';
import { ActionIcon, Badge, Box, Button, Group, Paper, ScrollArea, Select, Stack, Tabs, Text, TextInput, Tooltip } from '@mantine/core';
import { IconCopy, IconPlus, IconSearch, IconTrash } from '@tabler/icons-react';
import { parseDocument, stringify } from 'yaml';
import { RawYamlEditor } from '../fields/RawYamlEditor';
import { MiniMessageField } from '../fields/ValueField';

type TranslationRow = {
  id: string;
  key: string;
  english: string;
  german: string;
};

type TranslationDocument = {
  plugin: string;
  rows: TranslationRow[];
};

type FilterMode = 'all' | 'missingEnglish' | 'missingGerman' | 'incomplete' | 'changed';
const ROW_HEIGHT = 54;
const SELECTED_ROW_HEIGHT = 300;
const OVERSCAN_PX = ROW_HEIGHT * 8;

export function TranslationWorkspace({ selectedPath, yaml, baseYaml, disabled, onYamlChange }: {
  selectedPath: string;
  yaml: string;
  baseYaml: string;
  disabled: boolean;
  onYamlChange: (yaml: string) => void;
}) {
  const parsed = useMemo(() => parseTranslationYaml(yaml, selectedPath), [yaml, selectedPath]);
  const baseRows = useMemo(() => rowMap(parseTranslationYaml(baseYaml || yaml, selectedPath).rows), [baseYaml, yaml, selectedPath]);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<FilterMode>('all');
  const [selectedId, setSelectedId] = useState<string>('');
  const [scrollTop, setScrollTop] = useState(0);
  const [viewportHeight, setViewportHeight] = useState(0);

  useEffect(() => {
    if (!parsed.rows.length) {
      setSelectedId('');
      return;
    }
    if (!selectedId || !parsed.rows.some((row) => row.id === selectedId)) {
      setSelectedId(parsed.rows[0].id);
    }
  }, [parsed.rows, selectedId]);

  const validation = useMemo(() => validateRows(parsed.rows), [parsed.rows]);
  const filteredRows = useMemo(() => {
    const lower = query.trim().toLowerCase();
    return parsed.rows.filter((row) => {
      const matchesQuery = !lower || `${row.key} ${row.english} ${row.german}`.toLowerCase().includes(lower);
      if (!matchesQuery) return false;
      if (filter === 'missingEnglish') return !row.english.trim();
      if (filter === 'missingGerman') return !row.german.trim();
      if (filter === 'incomplete') return !row.english.trim() || !row.german.trim() || validation.duplicates.has(row.key.trim());
      if (filter === 'changed') return rowChanged(row, baseRows);
      return true;
    });
  }, [baseRows, filter, parsed.rows, query, validation.duplicates]);
  const rowLayout = useMemo(() => {
    let offset = 0;
    const rows = filteredRows.map((row) => {
      const height = row.id === selectedId ? SELECTED_ROW_HEIGHT : ROW_HEIGHT;
      const item = { row, top: offset, height };
      offset += height;
      return item;
    });
    return { rows, totalHeight: offset };
  }, [filteredRows, selectedId]);
  const visibleRows = useMemo(() => {
    const min = Math.max(0, scrollTop - OVERSCAN_PX);
    const max = scrollTop + viewportHeight + OVERSCAN_PX;
    return rowLayout.rows.filter((item) => item.top + item.height >= min && item.top <= max);
  }, [rowLayout.rows, scrollTop, viewportHeight]);

  const updateRows = (rows: TranslationRow[]) => {
    onYamlChange(serializeTranslationYaml({ plugin: parsed.plugin, rows }));
  };

  const updateRow = (id: string, patch: Partial<TranslationRow>) => {
    updateRows(parsed.rows.map((row) => row.id === id ? { ...row, ...patch } : row));
  };

  const addRow = () => {
    const next = uniqueKey(parsed.rows, preferredNewKey(query));
    const row = { id: newRowId(), key: next, english: '', german: '' };
    updateRows([...parsed.rows, row]);
    setSelectedId(row.id);
  };

  const duplicateRow = (row: TranslationRow) => {
    const next = { ...row, id: newRowId(), key: uniqueKey(parsed.rows, `${row.key}.copy`) };
    updateRows([...parsed.rows, next]);
    setSelectedId(next.id);
  };

  const deleteRow = (id: string) => {
    const rows = parsed.rows.filter((row) => row.id !== id);
    updateRows(rows);
    setSelectedId(rows[0]?.id || '');
  };

  return (
    <ScrollArea h="calc(100vh - 54px)">
      <Box p="lg">
        <Group justify="space-between" mb="md">
          <Box>
            <Text fw={700} size="lg">{parsed.plugin || selectedPath || 'Translations'}</Text>
            <Text c="dimmed" size="sm">{parsed.rows.length} translation keys</Text>
          </Box>
          <Group gap="xs">
            {validation.errors.length > 0 && <Badge color="red">{validation.errors.length} issues</Badge>}
            <Badge variant="outline">translations</Badge>
          </Group>
        </Group>
        <Tabs defaultValue="editor">
          <Tabs.List>
            <Tabs.Tab value="editor">Editor</Tabs.Tab>
            <Tabs.Tab value="yaml">Raw YAML</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="editor" pt="md">
            <Stack gap="md">
              <Paper p="md" withBorder>
                <Group align="end">
                  <TextInput
                    label="Search"
                    placeholder="translation key or text"
                    value={query}
                    leftSection={<IconSearch size={14} />}
                    onChange={(event) => setQuery(event.currentTarget.value)}
                    style={{ flex: 1 }}
                  />
                  <Select
                    label="Filter"
                    value={filter}
                    onChange={(value) => setFilter((value as FilterMode) || 'all')}
                    data={[
                      { value: 'all', label: 'All keys' },
                      { value: 'missingEnglish', label: 'Missing English' },
                      { value: 'missingGerman', label: 'Missing German' },
                      { value: 'incomplete', label: 'Incomplete' },
                      { value: 'changed', label: 'Changed' }
                    ]}
                    w={190}
                  />
                  <Button leftSection={<IconPlus size={15} />} disabled={disabled} onClick={addRow}>Add key</Button>
                </Group>
              </Paper>
              <Paper withBorder>
                <div className="translationHeader">
                  <Text size="xs" c="dimmed" fw={700}>Key</Text>
                  <Text size="xs" c="dimmed" fw={700}>English</Text>
                  <Text size="xs" c="dimmed" fw={700}>German</Text>
                  <Text size="xs" c="dimmed" fw={700}>Status</Text>
                  <Text size="xs" c="dimmed" fw={700}>Actions</Text>
                </div>
                <Box
                  className="translationVirtualViewport"
                  onScroll={(event) => setScrollTop(event.currentTarget.scrollTop)}
                  ref={(node) => {
                    if (node && node.clientHeight !== viewportHeight) setViewportHeight(node.clientHeight);
                  }}
                >
                  <div className="translationVirtualSpacer" style={{ height: rowLayout.totalHeight }}>
                    {visibleRows.map((item) => (
                      <TranslationTableRow
                        key={item.row.id}
                        row={item.row}
                        selected={item.row.id === selectedId}
                        duplicate={validation.duplicates.has(item.row.key.trim())}
                        changed={rowChanged(item.row, baseRows)}
                        disabled={disabled}
                        top={item.top}
                        height={item.height}
                        onSelect={() => setSelectedId(item.row.id)}
                        onChange={(patch) => updateRow(item.row.id, patch)}
                        onDuplicate={() => duplicateRow(item.row)}
                        onDelete={() => deleteRow(item.row.id)}
                      />
                    ))}
                  </div>
                  {filteredRows.length === 0 && (
                    <Text c="dimmed" ta="center" py="xl">No translation keys match this filter.</Text>
                  )}
                </Box>
              </Paper>
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

function TranslationTableRow({ row, selected, duplicate, changed, disabled, top, height, onSelect, onChange, onDuplicate, onDelete }: {
  row: TranslationRow;
  selected: boolean;
  duplicate: boolean;
  changed: boolean;
  disabled: boolean;
  top: number;
  height: number;
  onSelect: () => void;
  onChange: (patch: Partial<TranslationRow>) => void;
  onDuplicate: () => void;
  onDelete: () => void;
}) {
  const status = rowStatus(row, duplicate, changed);
  return (
    <div
      className={`translationVirtualRow ${selected ? 'selectedTranslationRow' : ''}`}
      style={{ transform: `translateY(${top}px)`, height }}
      onClick={onSelect}
    >
      {selected ? (
        <TextInput
          value={row.key}
          error={!row.key.trim() || duplicate}
          disabled={disabled}
          onClick={(event) => event.stopPropagation()}
          onChange={(event) => onChange({ key: event.currentTarget.value })}
        />
      ) : (
        <Text size="sm" fw={600} truncate>{row.key || <Text span c="dimmed">Empty key</Text>}</Text>
      )}
      <Text size="sm" lineClamp={2}>{row.english || <Text span c="dimmed">Missing</Text>}</Text>
      <Text size="sm" lineClamp={2}>{row.german || <Text span c="dimmed">Missing</Text>}</Text>
      <Badge size="sm" color={status.color} variant={status.variant}>{status.label}</Badge>
      <Group gap={4} wrap="nowrap" onClick={(event) => event.stopPropagation()}>
        <Tooltip label="Duplicate key">
          <ActionIcon variant="subtle" disabled={disabled} onClick={onDuplicate}><IconCopy size={15} /></ActionIcon>
        </Tooltip>
        <Tooltip label="Delete key">
          <ActionIcon variant="subtle" color="red" disabled={disabled} onClick={onDelete}><IconTrash size={15} /></ActionIcon>
        </Tooltip>
      </Group>
      {selected && (
        <div className="translationInlineEditor" onClick={(event) => event.stopPropagation()}>
          <MiniMessageField label="English" value={row.english} compact disabled={disabled} onChange={(value) => onChange({ english: String(value ?? '') })} />
          <MiniMessageField label="German" value={row.german} compact disabled={disabled} onChange={(value) => onChange({ german: String(value ?? '') })} />
        </div>
      )}
    </div>
  );
}

function parseTranslationYaml(yaml: string, fallbackPlugin: string): TranslationDocument {
  try {
    const doc = parseDocument(yaml || '{}');
    const data = doc.toJS() as Record<string, unknown> | null;
    const rows = Array.isArray(data?.rows) ? data.rows : [];
    return {
      plugin: String(data?.plugin || fallbackPlugin || ''),
      rows: rows.map((raw, index) => normalizeRow(raw, index))
    };
  } catch {
    return { plugin: fallbackPlugin || '', rows: [] };
  }
}

function normalizeRow(raw: unknown, index: number): TranslationRow {
  if (!raw || typeof raw !== 'object') {
    return { id: `row-${index}`, key: '', english: '', german: '' };
  }
  const row = raw as Record<string, unknown>;
  return {
    id: `row-${index}-${String(row.key || '')}`,
    key: String(row.key || ''),
    english: normalizeValue(row.english),
    german: normalizeValue(row.german)
  };
}

function serializeTranslationYaml(doc: TranslationDocument) {
  return stringify({
    plugin: doc.plugin,
    rows: doc.rows.map((row) => ({
      key: row.key.trim(),
      english: row.english,
      german: row.german
    }))
  }, { lineWidth: 0 });
}

function normalizeValue(value: unknown) {
  if (Array.isArray(value)) return value.map(String).join('\n');
  return value == null ? '' : String(value);
}

function validateRows(rows: TranslationRow[]) {
  const seen = new Set<string>();
  const duplicates = new Set<string>();
  const errors: string[] = [];
  for (const row of rows) {
    const key = row.key.trim();
    if (!key) errors.push('Empty key');
    if (key && seen.has(key)) duplicates.add(key);
    seen.add(key);
    if (!row.english.trim()) errors.push(`Missing English: ${key || '(empty)'}`);
    if (!row.german.trim()) errors.push(`Missing German: ${key || '(empty)'}`);
  }
  return { errors, duplicates };
}

function rowStatus(row: TranslationRow, duplicate: boolean, changed: boolean) {
  if (!row.key.trim()) return { label: 'Bad key', color: 'red', variant: 'light' as const };
  if (duplicate) return { label: 'Duplicate', color: 'red', variant: 'light' as const };
  if (!row.english.trim() || !row.german.trim()) return { label: 'Missing', color: 'yellow', variant: 'light' as const };
  if (changed) return { label: 'Changed', color: 'blue', variant: 'light' as const };
  return { label: 'Complete', color: 'green', variant: 'outline' as const };
}

function rowMap(rows: TranslationRow[]) {
  const result = new Map<string, TranslationRow>();
  for (const row of rows) result.set(row.key.trim(), row);
  return result;
}

function rowChanged(row: TranslationRow, baseRows: Map<string, TranslationRow>) {
  const base = baseRows.get(row.key.trim());
  return !base || base.english !== row.english || base.german !== row.german;
}

function uniqueKey(rows: TranslationRow[], preferred: string) {
  const existing = new Set(rows.map((row) => row.key.trim()).filter(Boolean));
  if (!existing.has(preferred)) return preferred;
  for (let index = 2; index < 1000; index++) {
    const next = `${preferred}.${index}`;
    if (!existing.has(next)) return next;
  }
  return `${preferred}.${Date.now()}`;
}

function preferredNewKey(query: string) {
  const value = query.trim();
  if (!value) return 'new.key';
  const candidate = value
    .replace(/\s+/g, '.')
    .replace(/[^A-Za-z0-9_.-]/g, '')
    .replace(/\.+/g, '.')
    .replace(/^\.|\.$/g, '');
  return candidate || 'new.key';
}

function newRowId() {
  return `row-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}
