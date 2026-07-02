import { useMemo, useState } from 'react';
import { ActionIcon, Badge, Box, Button, Group, Paper, Select, Stack, Text, TextInput } from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconCopy, IconTrash } from '@tabler/icons-react';
import type { AssetCatalog, Catalog, EntryCategory, QxlDocument, QxlEntry, Selection } from '../../api/types';
import { variablesForQxlSelection } from '../../catalog/variables';
import { ValueField } from '../fields/ValueField';
import {
  addEntryToEntrySlot,
  addEntryToRootSlot,
  asRecord,
  catalogFor,
  changeEntryType,
  deleteEntry,
  duplicateEntry,
  findEntry,
  mergeParams,
  moveEntry,
  parseLooseValue,
  refreshIds,
  slotNamesFor
} from '../../model/qxlDocument';
import { stringify } from 'yaml';
import { useEffect } from 'react';
import { Textarea } from '@mantine/core';

export function QxlSlotEditor({ title, slot, category, entries, catalog, assets, disabled, onChange }: {
  title: string;
  slot: string;
  category: EntryCategory;
  entries: QxlEntry[];
  catalog: Catalog;
  assets: AssetCatalog;
  disabled: boolean;
  onChange: (entries: QxlEntry[]) => void;
}) {
  const [selection, setSelection] = useState<Selection>({ kind: 'root' });
  const doc = useMemo(() => syntheticDoc(slot, entries), [slot, entries]);
  const docEntries = doc.rootSlots[slot] || [];
  const selectedEntry = selection.kind === 'entry' ? findEntry(doc, selection.id)?.entry : null;
  const variables = variablesForQxlSelection(doc, selection, catalog);
  const mutate = (mutator: (draft: QxlDocument) => Selection | void) => {
    const draft = cloneDoc(doc);
    const nextSelection = mutator(draft);
    refreshIds(draft);
    onChange(draft.rootSlots[slot] || []);
    if (nextSelection) setSelection(nextSelection);
  };
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm">
        <Box>
          <Text fw={700}>{title}</Text>
          <Text size="xs" c="dimmed">{slot}</Text>
        </Box>
        <Badge variant="light">{entries.length}</Badge>
      </Group>
      <Select
        disabled={disabled}
        placeholder={`Add ${category}`}
        data={catalogFor(catalog, category).map((entry) => ({ value: entry.id, label: entry.id }))}
        searchable
        clearable
        onChange={(value) => value && mutate((draft) => addEntryToRootSlot(draft, slot, category, value, catalog))}
      />
      <Stack gap="xs" mt="sm">
        {docEntries.map((entry) => (
          <button key={entry.id} type="button" className={`aetherNavButton ${selection.kind === 'entry' && selection.id === entry.id ? 'active' : ''}`} onClick={() => setSelection({ kind: 'entry', id: entry.id })}>
            {entry.key}: {entry.type}
          </button>
        ))}
        {docEntries.length === 0 && <Text size="sm" c="dimmed">No entries configured.</Text>}
      </Stack>
      {selectedEntry && (
        <Paper p="sm" withBorder mt="sm">
          <QxlEntryEditor entry={selectedEntry} catalog={catalog} assets={assets} disabled={disabled} variables={variables} mutate={mutate} />
        </Paper>
      )}
    </Paper>
  );
}

function QxlEntryEditor({ entry, catalog, assets, disabled, variables, mutate }: {
  entry: QxlEntry;
  catalog: Catalog;
  assets: AssetCatalog;
  disabled: boolean;
  variables: ReturnType<typeof variablesForQxlSelection>;
  mutate: (mutator: (draft: QxlDocument) => Selection | void) => void;
}) {
  const catalogEntry = catalogFor(catalog, entry.category).find((item) => item.id === entry.type);
  const params = mergeParams(entry, catalogEntry);
  return (
    <Stack gap="sm">
      <Group justify="space-between">
        <Box>
          <Text fw={700}>{entry.key}</Text>
          <Text size="xs" c="dimmed">{entry.category} / {entry.type}</Text>
        </Box>
        <Group gap={4}>
          <ActionIcon variant="subtle" disabled={disabled} onClick={() => mutate((draft) => moveEntry(draft, entry.id, -1))}><IconArrowUp size={16} /></ActionIcon>
          <ActionIcon variant="subtle" disabled={disabled} onClick={() => mutate((draft) => moveEntry(draft, entry.id, 1))}><IconArrowDown size={16} /></ActionIcon>
          <ActionIcon variant="subtle" disabled={disabled} onClick={() => mutate((draft) => duplicateEntry(draft, entry.id))}><IconCopy size={16} /></ActionIcon>
          <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => mutate((draft) => deleteEntry(draft, entry.id))}><IconTrash size={16} /></ActionIcon>
        </Group>
      </Group>
      <TextInput label="Entry key" value={entry.key} disabled={disabled} onChange={(event) => mutate((draft) => { const target = findEntry(draft, entry.id)?.entry; if (target) target.key = event.currentTarget.value; })} />
      <Select label="Type" value={entry.type} disabled={disabled} data={catalogFor(catalog, entry.category).map((item) => ({ value: item.id, label: item.id }))} searchable onChange={(value) => value && mutate((draft) => changeEntryType(draft, entry.id, value, catalog))} />
      {catalogEntry?.description && <Text size="sm" c="dimmed">{catalogEntry.description}</Text>}
      {params.filter((param) => !param.slotCategory).map((param) => (
        <ValueField
          key={param.name}
          label={param.name}
          description={param.description}
          value={entry.params[param.name]}
          kind={param.editorKind}
          param={param}
          disabled={disabled}
          assets={assets}
          variables={variables}
          onChange={(value) => mutate((draft) => { const target = findEntry(draft, entry.id)?.entry; if (target) target.params[param.name] = value; })}
        />
      ))}
      {slotNamesFor(entry, catalog).map((nestedSlot) => (
        <QxlSlotEditor
          key={nestedSlot.name}
          title={nestedSlot.name}
          slot={nestedSlot.name}
          category={nestedSlot.category}
          entries={entry.slots[nestedSlot.name] || []}
          catalog={catalog}
          assets={assets}
          disabled={disabled}
          onChange={(nextEntries) => mutate((draft) => { const target = findEntry(draft, entry.id)?.entry; if (target) target.slots[nestedSlot.name] = nextEntries; })}
        />
      ))}
      <ExtrasEditor value={entry.extras} disabled={disabled} onChange={(value) => mutate((draft) => { const target = findEntry(draft, entry.id)?.entry; if (target) target.extras = value; })} />
    </Stack>
  );
}

function ExtrasEditor({ value, disabled, onChange }: { value: Record<string, unknown>; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  const [text, setText] = useState(stringify(value || {}).trim());
  useEffect(() => { setText(stringify(value || {}).trim()); }, [value]);
  if (!value || Object.keys(value).length === 0) return null;
  return (
    <Textarea
      label="Raw extras"
      value={text}
      disabled={disabled}
      autosize
      minRows={3}
      onChange={(event) => setText(event.currentTarget.value)}
      onBlur={() => onChange(asRecord(parseLooseValue(text)))}
    />
  );
}

function syntheticDoc(slot: string, entries: QxlEntry[]): QxlDocument {
  const doc: QxlDocument = {
    type: 'aetherMobs',
    metadata: {},
    rootSlots: { [slot]: cloneEntries(entries) },
    stages: [],
    interactions: [],
    rewards: [],
    extras: {}
  };
  refreshIds(doc);
  return doc;
}

function cloneDoc(doc: QxlDocument): QxlDocument {
  return JSON.parse(JSON.stringify(doc));
}

function cloneEntries(entries: QxlEntry[]): QxlEntry[] {
  return JSON.parse(JSON.stringify(entries || []));
}
