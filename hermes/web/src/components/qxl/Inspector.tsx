import { useEffect, useState } from 'react';
import { stringify } from 'yaml';
import { ActionIcon, Badge, Box, Button, Code, Group, Paper, ScrollArea, Select, Stack, Text, TextInput, Textarea } from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconCopy, IconTrash } from '@tabler/icons-react';
import type { AssetCatalog, Catalog, ComponentParam, EditorKind, EntryCategory, QxlDocument, QxlEntry, QxlInteraction, QxlStage, Selection, VariableOption } from '../../api/types';
import { variablesForQxlSelection } from '../../catalog/variables';
import { ValueField } from '../fields/ValueField';
import { addEntryToEntrySlot, addEntryToInteraction, addEntryToRewardSlot, addEntryToRootSlot, addEntryToStage, addRewardSlot, asRecord, catalogFor, changeEntryType, defaultValue, deleteEntry, deleteInteraction, deleteStage, duplicateEntry, findEntry, mergeParams, moveEntry, parseLooseValue, renameInteraction, renameRewardSlot, renameStage, rootFieldKind, rootFields, rootSlotsForInspector, slotNamesFor } from '../../model/qxlDocument';
export function Inspector({ doc, selection, catalog, disabled, onMutate, assets }: { doc: QxlDocument; selection: Selection; catalog: Catalog; disabled: boolean; onMutate: (mutator: (draft: QxlDocument) => Selection | void) => void; assets: AssetCatalog }) {
  const selectedEntry = selection.kind === 'entry' ? findEntry(doc, selection.id)?.entry : null;
  const selectedStage = selection.kind === 'stage' ? doc.stages.find((stage) => stage.id === selection.id) : null;
  const selectedInteraction = selection.kind === 'interaction' ? doc.interactions.find((interaction) => interaction.id === selection.id) : null;
  const variables = variablesForQxlSelection(doc, selection, catalog);
  return (
    <ScrollArea h="calc(100vh - 98px)">
      <Stack p="sm">
        {selection.kind === 'root' && <RootInspector doc={doc} catalog={catalog} disabled={disabled} onMutate={onMutate} assets={assets} variables={variables} />}
        {selectedStage && <StageInspector stage={selectedStage} disabled={disabled} onMutate={onMutate} catalog={catalog} />}
        {selectedInteraction && <InteractionInspector interaction={selectedInteraction} disabled={disabled} onMutate={onMutate} catalog={catalog} assets={assets} variables={variables} />}
        {selectedEntry && <EntryInspector entry={selectedEntry} catalog={catalog} disabled={disabled} onMutate={onMutate} assets={assets} variables={variables} />}
      </Stack>
    </ScrollArea>
  );
}

function RootInspector({ doc, catalog, disabled, onMutate, assets, variables }: { doc: QxlDocument; catalog: Catalog; disabled: boolean; onMutate: (mutator: (draft: QxlDocument) => Selection | void) => void; assets: AssetCatalog; variables: VariableOption[] }) {
  const fields = rootFields(doc.type, doc.metadata);
  const slots = rootSlotsForInspector(doc);
  return (
    <>
      <Text fw={700}>Root metadata</Text>
      {fields.map((field) => <ValueField key={field} label={field} value={doc.metadata[field]} kind={rootFieldKind(field)} disabled={disabled} assets={assets} variables={variables} onChange={(value) => onMutate((draft) => { draft.metadata[field] = value; })} />)}
      {slots.map((slot) => (
        <SlotEditor key={slot.name} title={slot.name} category={slot.category} entries={doc.rootSlots[slot.name] || []} catalog={catalog} disabled={disabled} onAdd={(type) => onMutate((draft) => addEntryToRootSlot(draft, slot.name, slot.category, type, catalog))} />
      ))}
      {doc.type === 'events' && (
        <Paper p="sm" withBorder className="slotBox">
          <Group justify="space-between" mb="xs">
            <Text fw={600}>Rewards</Text>
            <Button size="xs" variant="light" disabled={disabled} onClick={() => onMutate((draft) => addRewardSlot(draft))}>Add threshold</Button>
          </Group>
          <Stack gap="sm">
            {doc.rewards.map((reward) => (
              <Paper key={reward.threshold} p="xs" withBorder>
                <TextInput label="Participation threshold" value={reward.threshold} disabled={disabled} onChange={(event) => onMutate((draft) => renameRewardSlot(draft, reward.threshold, event.currentTarget.value))} />
                <SlotEditor title="Reward actions" category="action" entries={reward.actions} catalog={catalog} disabled={disabled} onAdd={(type) => onMutate((draft) => addEntryToRewardSlot(draft, reward.threshold, type, catalog))} />
              </Paper>
            ))}
          </Stack>
        </Paper>
      )}
      <ExtrasEditor value={doc.extras} disabled={disabled} onChange={(value) => onMutate((draft) => { draft.extras = value; })} />
    </>
  );
}

function StageInspector({ stage, disabled, onMutate, catalog }: { stage: QxlStage; disabled: boolean; onMutate: (mutator: (draft: QxlDocument) => Selection | void) => void; catalog: Catalog }) {
  return (
    <>
      <Group justify="space-between">
        <Text fw={700}>Stage {stage.id}</Text>
        <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onMutate((draft) => deleteStage(draft, stage.id))}><IconTrash size={16} /></ActionIcon>
      </Group>
      <TextInput label="Stage id" value={stage.id} disabled={disabled} onChange={(event) => onMutate((draft) => {
        const nextId = event.currentTarget.value;
        renameStage(draft, stage.id, nextId);
        return { kind: 'stage', id: nextId.trim() || stage.id };
      })} />
      <SlotEditor title="Conditions" category="condition" entries={stage.conditions} catalog={catalog} disabled={disabled} onAdd={(type) => onMutate((draft) => addEntryToStage(draft, stage.id, 'conditions', type, catalog))} />
      <SlotEditor title="On start" category="action" entries={stage.onStart} catalog={catalog} disabled={disabled} onAdd={(type) => onMutate((draft) => addEntryToStage(draft, stage.id, 'onStart', type, catalog))} />
      <SlotEditor title="Objectives" category="objective" entries={stage.objectives} catalog={catalog} disabled={disabled} onAdd={(type) => onMutate((draft) => addEntryToStage(draft, stage.id, 'objectives', type, catalog))} />
      <SlotEditor title="On finish" category="action" entries={stage.onFinish} catalog={catalog} disabled={disabled} onAdd={(type) => onMutate((draft) => addEntryToStage(draft, stage.id, 'onFinish', type, catalog))} />
      <ExtrasEditor value={stage.extras} disabled={disabled} onChange={(value) => onMutate((draft) => { const target = draft.stages.find((item) => item.id === stage.id); if (target) target.extras = value; })} />
    </>
  );
}

function InteractionInspector({ interaction, disabled, onMutate, catalog, assets, variables }: { interaction: QxlInteraction; disabled: boolean; onMutate: (mutator: (draft: QxlDocument) => Selection | void) => void; catalog: Catalog; assets: AssetCatalog; variables: VariableOption[] }) {
  return (
    <>
      <Group justify="space-between">
        <Text fw={700}>Interaction {interaction.id}</Text>
        <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onMutate((draft) => deleteInteraction(draft, interaction.id))}><IconTrash size={16} /></ActionIcon>
      </Group>
      <TextInput label="Interaction id" value={interaction.id} disabled={disabled} onChange={(event) => onMutate((draft) => {
        const nextId = event.currentTarget.value;
        renameInteraction(draft, interaction.id, nextId);
        return { kind: 'interaction', id: nextId.trim() || interaction.id };
      })} />
      {['world', 'x', 'y', 'z', 'radius', 'repeatable'].map((field) => <ValueField key={field} label={field} value={field === 'world' ? interaction.params[field] ?? 'Erethon' : interaction.params[field]} kind={rootFieldKind(field)} disabled={disabled} assets={assets} variables={variables} onChange={(value) => onMutate((draft) => { const target = draft.interactions.find((item) => item.id === interaction.id); if (target) target.params[field] = value; })} />)}
      <SlotEditor title="Objectives" category="objective" entries={interaction.objectives} catalog={catalog} disabled={disabled} onAdd={(type) => onMutate((draft) => addEntryToInteraction(draft, interaction.id, type, catalog))} />
      <ExtrasEditor value={interaction.extras} disabled={disabled} onChange={(value) => onMutate((draft) => { const target = draft.interactions.find((item) => item.id === interaction.id); if (target) target.extras = value; })} />
    </>
  );
}

function EntryInspector({ entry, catalog, disabled, onMutate, assets, variables }: { entry: QxlEntry; catalog: Catalog; disabled: boolean; onMutate: (mutator: (draft: QxlDocument) => Selection | void) => void; assets: AssetCatalog; variables: VariableOption[] }) {
  const catalogEntry = catalogFor(catalog, entry.category).find((item) => item.id === entry.type);
  const params = mergeParams(entry, catalogEntry);
  return (
    <>
      <Group justify="space-between">
        <Box>
          <Text fw={700}>{entry.key}</Text>
          <Text size="xs" c="dimmed">{entry.category} / {entry.type}</Text>
        </Box>
        <Group gap={4}>
          <ActionIcon variant="subtle" disabled={disabled} onClick={() => onMutate((draft) => moveEntry(draft, entry.id, -1))}><IconArrowUp size={16} /></ActionIcon>
          <ActionIcon variant="subtle" disabled={disabled} onClick={() => onMutate((draft) => moveEntry(draft, entry.id, 1))}><IconArrowDown size={16} /></ActionIcon>
          <ActionIcon variant="subtle" disabled={disabled} onClick={() => onMutate((draft) => duplicateEntry(draft, entry.id))}><IconCopy size={16} /></ActionIcon>
          <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onMutate((draft) => deleteEntry(draft, entry.id))}><IconTrash size={16} /></ActionIcon>
        </Group>
      </Group>
      <TextInput label="Entry key" value={entry.key} disabled={disabled} onChange={(event) => onMutate((draft) => { const target = findEntry(draft, entry.id)?.entry; if (target) target.key = event.currentTarget.value; })} />
      <Select label="Type" value={entry.type} disabled={disabled} data={catalogFor(catalog, entry.category).map((item) => ({ value: item.id, label: item.id }))} searchable onChange={(value) => value && onMutate((draft) => changeEntryType(draft, entry.id, value, catalog))} />
      {catalogEntry?.description && <Text size="sm" c="dimmed">{catalogEntry.description}</Text>}
      {params.map((param) => <ParamField key={param.name} entry={entry} param={param} disabled={disabled} onMutate={onMutate} assets={assets} variables={variables} />)}
      {slotNamesFor(entry, catalog).map((slot) => (
        <SlotEditor key={slot.name} title={slot.name} category={slot.category} entries={entry.slots[slot.name] || []} catalog={catalog} disabled={disabled} onAdd={(type) => onMutate((draft) => addEntryToEntrySlot(draft, entry.id, slot.name, slot.category, type, catalog))} />
      ))}
      <ExtrasEditor value={entry.extras} disabled={disabled} onChange={(value) => onMutate((draft) => { const target = findEntry(draft, entry.id)?.entry; if (target) target.extras = value; })} />
    </>
  );
}

function ParamField({ entry, param, disabled, onMutate, assets, variables }: { entry: QxlEntry; param: ComponentParam; disabled: boolean; onMutate: (mutator: (draft: QxlDocument) => Selection | void) => void; assets: AssetCatalog; variables: VariableOption[] }) {
  if (param.slotCategory) return null;
  const value = entry.params[param.name];
  return <ValueField label={param.name} description={param.description} value={value} kind={param.editorKind} param={param} disabled={disabled} assets={assets} variables={variables} onChange={(next) => onMutate((draft) => { const target = findEntry(draft, entry.id)?.entry; if (target) target.params[param.name] = next; })} />;
}

function SlotEditor({ title, category, entries, catalog, disabled, onAdd }: { title: string; category: EntryCategory; entries: QxlEntry[]; catalog: Catalog; disabled: boolean; onAdd: (type: string) => void }) {
  return (
    <Paper p="sm" withBorder className="slotBox">
      <Group justify="space-between" mb={6}>
        <Text fw={600} size="sm">{title}</Text>
        <Badge size="sm" variant="light">{entries.length}</Badge>
      </Group>
      <Select disabled={disabled} placeholder={`Add ${category}`} data={catalogFor(catalog, category).map((entry) => ({ value: entry.id, label: entry.id }))} searchable clearable onChange={(value) => value && onAdd(value)} />
      {entries.length > 0 && <Stack gap={4} mt="xs">{entries.map((entry) => <Badge key={entry.id} variant="light">{entry.key}: {entry.type}</Badge>)}</Stack>}
    </Paper>
  );
}

function ExtrasEditor({ value, disabled, onChange }: { value: Record<string, unknown>; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  const [text, setText] = useState(stringify(value || {}).trim());
  useEffect(() => { setText(stringify(value || {}).trim()); }, [value]);
  if (!value || Object.keys(value).length === 0) return null;
  return (
    <Textarea
      label="Raw extras"
      description="Unsupported or custom fields preserved by the visual editor."
      value={text}
      disabled={disabled}
      autosize
      minRows={4}
      onChange={(event) => setText(event.currentTarget.value)}
      onBlur={() => onChange(asRecord(parseLooseValue(text)))}
    />
  );
}

