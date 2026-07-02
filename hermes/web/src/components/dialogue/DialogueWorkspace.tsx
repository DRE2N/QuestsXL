import { useMemo, useState } from 'react';
import { ActionIcon, Alert, Badge, Box, Button, Group, NumberInput, Paper, ScrollArea, Select, Stack, Tabs, Text, Textarea, TextInput, Tooltip } from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconPlus, IconTrash } from '@tabler/icons-react';
import { parseDocument, stringify } from 'yaml';
import type { AssetCatalog, Catalog, ComponentEntry, EntryCategory, QxlEntry, VariableOption } from '../../api/types';
import { allVariableOptions } from '../../catalog/variables';
import { ValueField } from '../fields/ValueField';
import { RawYamlEditor } from '../fields/RawYamlEditor';

type DialogueSelection =
  | { kind: 'root' }
  | { kind: 'stage'; stageKey: string }
  | { kind: 'message'; stageKey: string; index: number }
  | { kind: 'option'; stageKey: string; index: number }
  | { kind: 'stageEntry'; stageKey: string; slot: 'conditions' | 'actions'; entryId: string }
  | { kind: 'optionAction'; stageKey: string; optionIndex: number; entryId: string };

type DialogueDocument = {
  sender: unknown;
  npcId?: unknown;
  canStartFromNPC: boolean;
  conditions: QxlEntry[];
  stages: DialogueStage[];
  extras: Record<string, unknown>;
  parseError?: string;
};

type DialogueStage = {
  key: string;
  id: string;
  messages: DialogueMessage[];
  autoNext: boolean;
  conditions: QxlEntry[];
  actions: QxlEntry[];
  options: DialogueOption[];
  extras: Record<string, unknown>;
};

type DialogueMessage = {
  text: string;
  delay: number | '';
};

type DialogueOption = {
  text: string;
  hint: string;
  next: string;
  default: boolean;
  actions: QxlEntry[];
  extras: string[];
  raw?: string;
};

export function DialogueWorkspace({ selectedPath, yaml, catalog, assets, disabled, onYamlChange }: { selectedPath: string; yaml: string; catalog: Catalog; assets: AssetCatalog; disabled: boolean; onYamlChange: (yaml: string) => void }) {
  const doc = useMemo(() => parseDialogueDocument(yaml, catalog), [yaml, catalog]);
  const [selection, setSelection] = useState<DialogueSelection>({ kind: 'root' });
  const selected = normalizeSelection(selection, doc);
  const errors = validateDialogue(doc);
  const variables = useMemo(() => allVariableOptions(catalog), [catalog]);

  const commit = (next: DialogueDocument, nextSelection = selected) => {
    onYamlChange(serializeDialogueDocument(next, catalog));
    setSelection(nextSelection);
  };
  const mutate = (mutator: (draft: DialogueDocument) => DialogueSelection | void) => {
    const draft = cloneDialogue(doc);
    const nextSelection = mutator(draft) || selected;
    commit(draft, nextSelection);
  };

  return (
    <Box p="lg">
      <Group justify="space-between" mb="md">
        <Box>
          <Text fw={700} size="lg">{selectedPath || 'New dialogue'}</Text>
          <Text c="dimmed" size="sm">Dialogue graph editor</Text>
        </Box>
        <Group gap="xs">
          <Badge variant="outline">dialogues</Badge>
          <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled || Boolean(doc.parseError)} onClick={() => mutate(addStage)}>
            Stage
          </Button>
        </Group>
      </Group>
      <Tabs defaultValue="editor" className="centerTabs">
        <Tabs.List>
          <Tabs.Tab value="editor">Editor</Tabs.Tab>
          <Tabs.Tab value="yaml">Raw YAML</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="editor" pt="md">
          <ScrollArea h="calc(100vh - 150px)">
            {doc.parseError ? (
              <Paper p="md" withBorder><Text c="red" fw={700}>YAML could not be parsed.</Text><Text size="sm">{doc.parseError}</Text></Paper>
            ) : (
              <div className="dialogueWorkspace">
                <div className="dialogueGraph">
                  <DialogueRootCard doc={doc} selected={selected.kind === 'root'} onSelect={() => setSelection({ kind: 'root' })} />
                  {doc.stages.map((stage) => (
                    <StageCard
                      key={stage.key}
                      stage={stage}
                      doc={doc}
                      selection={selected}
                      onSelect={setSelection}
                      onMutate={mutate}
                      disabled={disabled}
                    />
                  ))}
                  {doc.stages.length === 0 && <Text c="dimmed">No stages configured.</Text>}
                </div>
                <Paper className="dialogueInspector" p="md" withBorder>
                  <DialogueValidation errors={errors} />
                  <DialogueInspector doc={doc} selection={selected} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onMutate={mutate} />
                </Paper>
              </div>
            )}
          </ScrollArea>
        </Tabs.Panel>
        <Tabs.Panel value="yaml" pt="md">
          <ScrollArea h="calc(100vh - 150px)">
            <RawYamlEditor value={yaml} onChange={onYamlChange} minRows={32} disabled={disabled} />
          </ScrollArea>
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

function DialogueRootCard({ doc, selected, onSelect }: { doc: DialogueDocument; selected: boolean; onSelect: () => void }) {
  return (
    <Paper className={`dialogueStageCard clickable ${selected ? 'selectedNode' : ''}`} withBorder onClick={onSelect}>
      <Group justify="space-between">
        <Box>
          <Text fw={700}>Dialogue root</Text>
          <Text size="xs" c="dimmed">{String(asRecord(doc.sender).en || asRecord(doc.sender).de || doc.sender || 'No sender')}</Text>
        </Box>
        <Badge variant="light">{doc.stages.length} stages</Badge>
      </Group>
    </Paper>
  );
}

function StageCard({ stage, doc, selection, onSelect, onMutate, disabled }: { stage: DialogueStage; doc: DialogueDocument; selection: DialogueSelection; onSelect: (selection: DialogueSelection) => void; onMutate: (mutator: (draft: DialogueDocument) => DialogueSelection | void) => void; disabled: boolean }) {
  return (
    <Paper className={`dialogueStageCard clickable ${selection.kind === 'stage' && selection.stageKey === stage.key ? 'selectedNode' : ''}`} withBorder onClick={() => onSelect({ kind: 'stage', stageKey: stage.key })}>
      <Group justify="space-between" mb="sm">
        <Box>
          <Text fw={700}>Stage {stage.key}</Text>
          <Text size="xs" c="dimmed">{stage.id}</Text>
        </Box>
        <Badge size="sm" variant="light">{stage.options.length} options</Badge>
      </Group>
      <Stack gap={6}>
        {stage.messages.map((message, index) => (
          <button key={`message-${index}`} type="button" className={`dialogueNode message ${selection.kind === 'message' && selection.stageKey === stage.key && selection.index === index ? 'selectedNode' : ''}`} onClick={(event) => { event.stopPropagation(); onSelect({ kind: 'message', stageKey: stage.key, index }); }}>
            <span>{dialogueTextPreview(message.text) || 'Empty message'}</span>
            {message.delay !== '' && <small>{message.delay} ticks</small>}
          </button>
        ))}
        {stage.options.map((option, index) => (
          <button key={`option-${index}`} type="button" className={`dialogueNode option ${selection.kind === 'option' && selection.stageKey === stage.key && selection.index === index ? 'selectedNode' : ''}`} onClick={(event) => { event.stopPropagation(); onSelect({ kind: 'option', stageKey: stage.key, index }); }}>
            <span>{option.text || 'Empty option'}</span>
            <small>{option.next ? `-> stage ${option.next}` : '-> End'}{option.default ? ' default' : ''}</small>
          </button>
        ))}
      </Stack>
      <Group gap="xs" mt="sm">
        <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={(event) => { event.stopPropagation(); onMutate((draft) => addMessage(draft, stage.key)); }}>Message</Button>
        <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={(event) => { event.stopPropagation(); onMutate((draft) => addOption(draft, stage.key)); }}>Option</Button>
      </Group>
      <div className="dialogueBranchList">
        {stage.options.map((option, index) => (
          <div key={`branch-${index}`} className="dialogueBranch">
            <span>{index + 1}</span>
            <b>{option.next ? stageLabel(doc, option.next) : 'End dialogue'}</b>
          </div>
        ))}
      </div>
    </Paper>
  );
}

function DialogueInspector({ doc, selection, catalog, assets, variables, disabled, onMutate }: { doc: DialogueDocument; selection: DialogueSelection; catalog: Catalog; assets: AssetCatalog; variables: VariableOption[]; disabled: boolean; onMutate: (mutator: (draft: DialogueDocument) => DialogueSelection | void) => void }) {
  if (selection.kind === 'root') {
    return (
      <Stack gap="sm">
        <Text fw={700}>Root</Text>
        <ValueField label="sender" description="Speaker name shown above the dialogue." value={translatableFrom(doc.sender)} kind="translatable" disabled={disabled} variables={variables} onChange={(value) => onMutate((draft) => { draft.sender = translatableMap(value); })} />
        <DialogueNpcIdField value={doc.npcId} assets={assets} disabled={disabled} onChange={(value) => onMutate((draft) => { draft.npcId = value; })} />
        <ValueField label="canStartFromNPC" description="If false, only actions/scripts can start this dialogue." value={doc.canStartFromNPC} kind="boolean" disabled={disabled} onChange={(value) => onMutate((draft) => { draft.canStartFromNPC = Boolean(value); })} />
        <ComponentSlotEditor title="Root conditions" category="condition" entries={doc.conditions} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onAdd={(type) => onMutate((draft) => addRootEntry(draft, type, catalog))} onChange={(entryId, updater) => onMutate((draft) => updateRootEntry(draft, entryId, updater))} onDelete={(entryId) => onMutate((draft) => deleteRootEntry(draft, entryId))} />
        <ValueField label="Raw extras" value={doc.extras} kind="raw" disabled={disabled} onChange={(value) => onMutate((draft) => { draft.extras = asRecord(value); })} />
      </Stack>
    );
  }
  const stage = doc.stages.find((item) => item.key === selection.stageKey);
  if (!stage) return <Text c="dimmed">Select a dialogue entry.</Text>;
  if (selection.kind === 'stage') {
    return <StageInspector stage={stage} doc={doc} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onMutate={onMutate} />;
  }
  if (selection.kind === 'message') {
    const message = stage.messages[selection.index];
    return <MessageInspector stage={stage} message={message} index={selection.index} variables={variables} disabled={disabled} onMutate={onMutate} />;
  }
  if (selection.kind === 'option') {
    const option = stage.options[selection.index];
    return <OptionInspector stage={stage} option={option} index={selection.index} doc={doc} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onMutate={onMutate} />;
  }
  const entry = selection.kind === 'stageEntry'
    ? stage[selection.slot].find((item) => item.id === selection.entryId)
    : stage.options[selection.optionIndex]?.actions.find((item) => item.id === selection.entryId);
  if (!entry) return <Text c="dimmed">Entry no longer exists.</Text>;
  return (
    <Stack gap="sm">
      <Text fw={700}>{entry.category}: {entry.type}</Text>
      <EntryEditor entry={entry} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onChange={(updater) => onMutate((draft) => updateSelectedEntry(draft, selection, updater))} />
      <Button color="red" variant="light" disabled={disabled} onClick={() => onMutate((draft) => deleteSelectedEntry(draft, selection))}>Delete entry</Button>
    </Stack>
  );
}

function StageInspector({ stage, doc, catalog, assets, variables, disabled, onMutate }: { stage: DialogueStage; doc: DialogueDocument; catalog: Catalog; assets: AssetCatalog; variables: VariableOption[]; disabled: boolean; onMutate: (mutator: (draft: DialogueDocument) => DialogueSelection | void) => void }) {
  return (
    <Stack gap="sm">
      <Group justify="space-between">
        <Text fw={700}>Stage {stage.key}</Text>
        <Button size="xs" color="red" variant="subtle" disabled={disabled} onClick={() => onMutate((draft) => deleteStage(draft, stage.key))}>Delete</Button>
      </Group>
      <NumberInput label="Index" description="Numeric stage key referenced by option next=..." value={Number(stage.key)} disabled={disabled} onChange={(value) => onMutate((draft) => renameStage(draft, stage.key, String(value ?? stage.key)))} />
      <TextInput label="id" description="Human-readable stage id used for debugging." value={stage.id} disabled={disabled} onChange={(event) => onMutate((draft) => { findStage(draft, stage.key).id = event.currentTarget.value; })} />
      <ValueField label="autoNext" description="Automatically continue after all messages finish." value={stage.autoNext} kind="boolean" disabled={disabled} onChange={(value) => onMutate((draft) => { findStage(draft, stage.key).autoNext = Boolean(value); })} />
      <ComponentSlotEditor title="Conditions" category="condition" entries={stage.conditions} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onAdd={(type) => onMutate((draft) => addStageEntry(draft, stage.key, 'conditions', type, catalog))} onChange={(entryId, updater) => onMutate((draft) => updateStageEntry(draft, stage.key, 'conditions', entryId, updater))} onDelete={(entryId) => onMutate((draft) => deleteStageEntry(draft, stage.key, 'conditions', entryId))} />
      <ComponentSlotEditor title="Actions" category="action" entries={stage.actions} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onAdd={(type) => onMutate((draft) => addStageEntry(draft, stage.key, 'actions', type, catalog))} onChange={(entryId, updater) => onMutate((draft) => updateStageEntry(draft, stage.key, 'actions', entryId, updater))} onDelete={(entryId) => onMutate((draft) => deleteStageEntry(draft, stage.key, 'actions', entryId))} />
      <ValueField label="Raw extras" value={stage.extras} kind="raw" disabled={disabled} onChange={(value) => onMutate((draft) => { findStage(draft, stage.key).extras = asRecord(value); })} />
      <Text size="xs" c="dimmed">{stage.options.length} branches from this stage. Targets: {stage.options.map((option) => option.next ? stageLabel(doc, option.next) : 'End').join(', ') || 'none'}</Text>
    </Stack>
  );
}

function MessageInspector({ stage, message, index, variables, disabled, onMutate }: { stage: DialogueStage; message: DialogueMessage; index: number; variables: VariableOption[]; disabled: boolean; onMutate: (mutator: (draft: DialogueDocument) => DialogueSelection | void) => void }) {
  return (
    <Stack gap="sm">
      <Group justify="space-between">
        <Text fw={700}>Message {index + 1}</Text>
        <MoveDeleteButtons disabled={disabled} canUp={index > 0} canDown={index < stage.messages.length - 1} onUp={() => onMutate((draft) => moveMessage(draft, stage.key, index, -1))} onDown={() => onMutate((draft) => moveMessage(draft, stage.key, index, 1))} onDelete={() => onMutate((draft) => deleteMessage(draft, stage.key, index))} />
      </Group>
      <ValueField label="Message" description="Localized dialogue line. English and German are saved as en=...; de=..." value={message.text} kind="translatable" disabled={disabled} variables={variables} onChange={(value) => onMutate((draft) => { findStage(draft, stage.key).messages[index].text = String(value ?? ''); })} />
      <NumberInput label="Delay" description="Optional delay in ticks. Empty means no suffix is written." value={message.delay} disabled={disabled} onChange={(value) => onMutate((draft) => { findStage(draft, stage.key).messages[index].delay = value === '' ? '' : Number(value ?? 0); })} />
    </Stack>
  );
}

function DialogueNpcIdField({ value, assets, disabled, onChange }: { value: unknown; assets: AssetCatalog; disabled: boolean; onChange: (value: unknown) => void }) {
  const options = uniqueAssetOptions(assets.mobs || []);
  return (
    <>
      <TextInput
        label="npcId"
        description="Optional Aether NPC id that starts this dialogue. You can type manually or pick a suggestion."
        value={value == null ? '' : String(value)}
        disabled={disabled}
        list="dialogue-npc-id-options"
        placeholder="Aether NPC id"
        onChange={(event) => onChange(event.currentTarget.value)}
      />
      <datalist id="dialogue-npc-id-options">
        {options.map((option) => (
          <option key={option.id} value={option.id} label={dialogueNpcOptionLabel(option)} />
        ))}
      </datalist>
    </>
  );
}

function OptionInspector({ stage, option, index, doc, catalog, assets, variables, disabled, onMutate }: { stage: DialogueStage; option: DialogueOption; index: number; doc: DialogueDocument; catalog: Catalog; assets: AssetCatalog; variables: VariableOption[]; disabled: boolean; onMutate: (mutator: (draft: DialogueDocument) => DialogueSelection | void) => void }) {
  const stageOptions = [{ value: '', label: 'End dialogue' }, ...doc.stages.map((item) => ({ value: item.key, label: stageLabel(doc, item.key) }))];
  return (
    <Stack gap="sm">
      <Group justify="space-between">
        <Text fw={700}>Option {index + 1}</Text>
        <MoveDeleteButtons disabled={disabled} canUp={index > 0} canDown={index < stage.options.length - 1} onUp={() => onMutate((draft) => moveOption(draft, stage.key, index, -1))} onDown={() => onMutate((draft) => moveOption(draft, stage.key, index, 1))} onDelete={() => onMutate((draft) => deleteOption(draft, stage.key, index))} />
      </Group>
      <ValueField label="text" description="Player-facing option text." value={option.text} kind="translatable" disabled={disabled} variables={variables} onChange={(value) => onMutate((draft) => { findStage(draft, stage.key).options[index].text = String(value ?? ''); })} />
      <ValueField label="hint" description="Optional hover/help text." value={option.hint} kind="translatable" disabled={disabled} variables={variables} onChange={(value) => onMutate((draft) => { findStage(draft, stage.key).options[index].hint = String(value ?? ''); })} />
      <Select label="next" description="Branch target. End dialogue writes no next= field." data={stageOptions} value={option.next} disabled={disabled} searchable onChange={(value) => onMutate((draft) => { findStage(draft, stage.key).options[index].next = value || ''; })} />
      <ValueField label="default" description="Default option for keyboard/auto selection." value={option.default} kind="boolean" disabled={disabled} onChange={(value) => onMutate((draft) => { findStage(draft, stage.key).options[index].default = Boolean(value); })} />
      <ComponentSlotEditor title="Option actions" category="action" entries={option.actions} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onAdd={(type) => onMutate((draft) => addOptionAction(draft, stage.key, index, type, catalog))} onChange={(entryId, updater) => onMutate((draft) => updateOptionAction(draft, stage.key, index, entryId, updater))} onDelete={(entryId) => onMutate((draft) => deleteOptionAction(draft, stage.key, index, entryId))} />
      {option.extras.length > 0 && <Textarea label="Unsupported option fragments" value={option.extras.join('\n')} disabled={disabled} autosize minRows={2} onChange={(event) => onMutate((draft) => { findStage(draft, stage.key).options[index].extras = event.currentTarget.value.split('\n').filter(Boolean); })} />}
    </Stack>
  );
}

function ComponentSlotEditor({ title, category, entries, catalog, assets, variables, disabled, onAdd, onChange, onDelete }: { title: string; category: EntryCategory; entries: QxlEntry[]; catalog: Catalog; assets: AssetCatalog; variables: VariableOption[]; disabled: boolean; onAdd: (type: string) => void; onChange: (entryId: string, updater: (entry: QxlEntry) => void) => void; onDelete: (entryId: string) => void }) {
  const options = catalogFor(catalog, category).map((entry) => ({ value: entry.id, label: entry.id }));
  return (
    <Paper p="sm" withBorder className="slotBox">
      <Group justify="space-between" mb="xs">
        <Text fw={600} size="sm">{title}</Text>
        <Select size="xs" placeholder={`Add ${category}`} data={options} searchable disabled={disabled} onChange={(value) => value && onAdd(value)} w={170} />
      </Group>
      <Stack gap="xs">
        {entries.map((entry) => (
          <Paper key={entry.id} p="xs" withBorder>
            <Group justify="space-between" align="start">
              <Box flex={1}>
                <EntryEditor entry={entry} catalog={catalog} assets={assets} variables={variables} disabled={disabled} onChange={(updater) => onChange(entry.id, updater)} />
              </Box>
              <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onDelete(entry.id)}><IconTrash size={16} /></ActionIcon>
            </Group>
          </Paper>
        ))}
        {entries.length === 0 && <Text size="sm" c="dimmed">No {title.toLowerCase()} configured.</Text>}
      </Stack>
    </Paper>
  );
}

function EntryEditor({ entry, catalog, assets, variables, disabled, onChange }: { entry: QxlEntry; catalog: Catalog; assets: AssetCatalog; variables: VariableOption[]; disabled: boolean; onChange: (updater: (entry: QxlEntry) => void) => void }) {
  const catalogEntry = catalogFor(catalog, entry.category).find((item) => item.id === entry.type);
  const params = mergeParams(entry, catalogEntry);
  return (
    <Stack gap="xs">
      <Group grow>
        <TextInput label="key" value={entry.key} disabled={disabled} onChange={(event) => onChange((target) => { target.key = event.currentTarget.value; })} />
        <Select label="type" data={catalogFor(catalog, entry.category).map((item) => ({ value: item.id, label: item.id }))} value={entry.type} disabled={disabled} searchable onChange={(value) => value && onChange((target) => { target.type = value; target.params = { ...defaultParams(value, entry.category, catalog), ...target.params }; })} />
      </Group>
      {params.map((param) => (
        <ValueField key={param.name} label={param.name} description={param.description} value={entry.params[param.name]} kind={param.editorKind} param={param} disabled={disabled} assets={assets} variables={variables} onChange={(value) => onChange((target) => { target.params[param.name] = value; })} />
      ))}
      <ValueField label="extras" value={entry.extras} kind="raw" disabled={disabled} onChange={(value) => onChange((target) => { target.extras = asRecord(value); })} />
    </Stack>
  );
}

function MoveDeleteButtons({ disabled, canUp, canDown, onUp, onDown, onDelete }: { disabled: boolean; canUp: boolean; canDown: boolean; onUp: () => void; onDown: () => void; onDelete: () => void }) {
  return (
    <Group gap={4}>
      <Tooltip label="Move up"><ActionIcon variant="subtle" disabled={disabled || !canUp} onClick={onUp}><IconArrowUp size={16} /></ActionIcon></Tooltip>
      <Tooltip label="Move down"><ActionIcon variant="subtle" disabled={disabled || !canDown} onClick={onDown}><IconArrowDown size={16} /></ActionIcon></Tooltip>
      <Tooltip label="Delete"><ActionIcon color="red" variant="subtle" disabled={disabled} onClick={onDelete}><IconTrash size={16} /></ActionIcon></Tooltip>
    </Group>
  );
}

function DialogueValidation({ errors }: { errors: Record<string, string> }) {
  const entries = Object.entries(errors);
  if (entries.length === 0) return <Alert color="green" variant="light" mb="sm">No dialogue issues detected.</Alert>;
  return (
    <Alert color="red" variant="light" mb="sm" title={`${entries.length} issue${entries.length === 1 ? '' : 's'}`}>
      <Stack gap={2}>{entries.map(([field, message]) => <Text key={field} size="sm"><b>{field}</b>: {message}</Text>)}</Stack>
    </Alert>
  );
}

function parseDialogueDocument(yaml: string, catalog: Catalog): DialogueDocument {
  try {
    const parsed = parseDocument(yaml || '{}').toJS();
    if (!isPlainObject(parsed)) return emptyDialogue('Root YAML value must be a map.');
    const source = asRecord(parsed);
    const stages = asRecord(source.stages);
    const extras = { ...source };
    delete extras.sender;
    delete extras.npcId;
    delete extras.canStartFromNPC;
    delete extras.conditions;
    delete extras.stages;
    return {
      sender: source.sender ?? '',
      npcId: source.npcId,
      canStartFromNPC: source.canStartFromNPC !== false,
      conditions: parseComponentCollection(source.conditions, 'condition', 'conditions', catalog),
      stages: Object.entries(stages).map(([key, raw]) => parseStage(key, raw, catalog)).sort((left, right) => Number(left.key) - Number(right.key)),
      extras
    };
  } catch (error) {
    return emptyDialogue(error instanceof Error ? error.message : 'Invalid YAML');
  }
}

function emptyDialogue(parseError?: string): DialogueDocument {
  return { sender: '', canStartFromNPC: true, conditions: [], stages: [], extras: {}, parseError };
}

function parseStage(key: string, raw: unknown, catalog: Catalog): DialogueStage {
  const source = asRecord(raw);
  const extras = { ...source };
  for (const field of ['id', 'messages', 'autoNext', 'conditions', 'actions', 'options']) delete extras[field];
  return {
    key,
    id: String(source.id ?? `stage_${key}`),
    messages: listValue(source.messages).map(parseMessageLine),
    autoNext: source.autoNext !== false,
    conditions: parseComponentCollection(source.conditions, 'condition', `stages.${key}.conditions`, catalog),
    actions: parseComponentCollection(source.actions, 'action', `stages.${key}.actions`, catalog),
    options: listValue(source.options).map((line, index) => parseDialogueOption(line, `stages.${key}.options.${index}`, catalog)),
    extras
  };
}

function parseMessageLine(line: string): DialogueMessage {
  const match = line.match(/^(.*)\|(\d+)$/s);
  return match ? { text: match[1], delay: Number(match[2]) } : { text: line, delay: '' };
}

function serializeMessageLine(message: DialogueMessage) {
  return message.delay === '' ? message.text : `${message.text}|${message.delay}`;
}

function parseDialogueOption(line: string, path: string, catalog: Catalog): DialogueOption {
  const option: DialogueOption = { text: '', hint: '', next: '', default: false, actions: [], extras: [] };
  for (const part of splitOptionParts(line)) {
    const index = part.indexOf('=');
    if (index <= 0) {
      option.extras.push(part);
      continue;
    }
    const key = part.slice(0, index).trim();
    const value = part.slice(index + 1).trim();
    if (key === 'text') option.text = value;
    else if (key === 'hint') option.hint = value;
    else if (key === 'next') option.next = value;
    else if (key === 'default') option.default = value === 'true';
    else if (key === 'actions') option.actions = parseInlineActions(value, `${path}.actions`, catalog);
    else option.extras.push(part);
  }
  return option;
}

function splitOptionParts(line: string) {
  return String(line ?? '').split('|').map((part) => part.trim()).filter(Boolean);
}

function serializeDialogueOption(option: DialogueOption, catalog: Catalog) {
  const parts = [`text=${option.text || 'en=;de='}`];
  if (option.hint) parts.push(`hint=${option.hint}`);
  if (option.next) parts.push(`next=${option.next}`);
  if (option.default) parts.push('default=true');
  const actions = serializeInlineActions(option.actions, catalog);
  if (actions) parts.push(`actions=${actions}`);
  parts.push(...option.extras);
  return parts.join('|');
}

function parseInlineActions(value: string, path: string, catalog: Catalog) {
  return value.split(';;').map((part, index) => parseInlineAction(part.trim(), `${path}.${index}`, catalog)).filter(Boolean) as QxlEntry[];
}

function parseInlineAction(value: string, path: string, catalog: Catalog): QxlEntry | null {
  if (!value) return null;
  const [type, rest = ''] = value.split(/:(.*)/s);
  const cleanType = type.trim();
  return makeEntry(path, cleanType, cleanType, 'action', parseInlineParams(cleanType, 'action', rest.trim(), catalog), {}, {});
}

function serializeInlineActions(entries: QxlEntry[], catalog: Catalog) {
  return entries.map((entry) => {
    const params = { ...entry.params, ...entry.extras };
    const body = serializeInlineParams(params);
    return body ? `${entry.type}: ${body}` : entry.type;
  }).join(';;');
}

function serializeInlineParams(params: Record<string, unknown>) {
  return Object.entries(params)
    .filter(([, value]) => value !== undefined && value !== '')
    .map(([key, value]) => `${key}=${inlineScalar(value)}`)
    .join('; ');
}

function inlineScalar(value: unknown): string {
  if (Array.isArray(value)) return value.map(String).join(',');
  if (isPlainObject(value)) return stringify(value, { lineWidth: 0 }).trim();
  return String(value ?? '');
}

function parseComponentCollection(raw: unknown, category: EntryCategory, path: string, catalog: Catalog): QxlEntry[] {
  if (raw == null) return [];
  if (Array.isArray(raw)) return raw.map((item, index) => parseListEntry(item, category, `${path}.${index}`, catalog)).filter(Boolean) as QxlEntry[];
  if (isPlainObject(raw)) return Object.entries(raw).map(([key, value]) => parseMapEntry(key, value, category, `${path}.${key}`, catalog));
  return [];
}

function parseListEntry(item: unknown, category: EntryCategory, path: string, catalog: Catalog): QxlEntry | null {
  if (typeof item === 'string') {
    const [type, rest = ''] = item.split(/:(.*)/s);
    const cleanType = type.trim();
    return makeEntry(path, cleanType, cleanType, category, parseInlineParams(cleanType, category, rest.trim(), catalog), {}, {});
  }
  if (isPlainObject(item)) {
    const [key, value] = Object.entries(item)[0] || [];
    if (!key) return null;
    return parseMapEntry(key, value, category, path, catalog);
  }
  return null;
}

function parseMapEntry(key: string, value: unknown, category: EntryCategory, path: string, catalog: Catalog): QxlEntry {
  if (!isPlainObject(value)) {
    const params = typeof value === 'string' ? parseInlineParams(key, category, value, catalog) : value == null ? {} : { value };
    return makeEntry(path, key, key, category, params, {}, {});
  }
  const type = typeof value.type === 'string' ? value.type : key;
  const catalogEntry = catalogFor(catalog, category).find((entry) => entry.id === type);
  const params: Record<string, unknown> = {};
  const extras: Record<string, unknown> = {};
  const slots: Record<string, QxlEntry[]> = {};
  const slotNames = new Map(slotDefinitions(category, catalogEntry).map((slot) => [slot.name, slot.category]));
  for (const [field, fieldValue] of Object.entries(value)) {
    if (field === 'type') continue;
    const slotCategory = slotNames.get(field);
    if (slotCategory) slots[field] = parseComponentCollection(fieldValue, slotCategory, `${path}.${field}`, catalog);
    else if (catalogEntry?.params.some((param) => param.name === field) || isCommonParam(field)) params[field] = fieldValue;
    else extras[field] = fieldValue;
  }
  return makeEntry(path, key, type, category, params, slots, extras);
}

function serializeComponentCollection(entries: QxlEntry[], catalog: Catalog) {
  return Object.fromEntries(entries.map((entry) => {
    const body: Record<string, unknown> = { type: entry.type, ...entry.params, ...entry.extras };
    for (const [slot, nested] of Object.entries(entry.slots)) {
      if (nested.length > 0) body[slot] = serializeComponentCollection(nested, catalog);
    }
    return [entry.key || entry.type, body];
  }));
}

function parseInlineParams(type: string, category: EntryCategory, source: string, catalog: Catalog): Record<string, unknown> {
  if (!source) return {};
  const knownNames = inlineParamNames(type, category, catalog);
  const known = splitKnownInlineParams(source, knownNames);
  if (Object.keys(known).length > 0) return known;
  const generic = splitGenericInlineParams(source);
  if (Object.keys(generic).length > 0) return generic;
  return { value: parseScalar(source) };
}

function splitKnownInlineParams(source: string, names: string[]) {
  if (names.length === 0) return {};
  const escaped = names.map(escapeRegex).join('|');
  const pattern = new RegExp(`(^|;\\s*)(${escaped})=`, 'g');
  const matches = Array.from(source.matchAll(pattern));
  if (matches.length === 0 || matches[0].index !== 0) return {};
  const result: Record<string, unknown> = {};
  for (let index = 0; index < matches.length; index++) {
    const match = matches[index];
    const name = match[2];
    const valueStart = (match.index || 0) + match[0].length;
    const nextStart = matches[index + 1]?.index ?? source.length;
    result[name] = parseScalar(source.slice(valueStart, nextStart).replace(/;\s*$/, '').trim());
  }
  return result;
}

function splitGenericInlineParams(source: string) {
  const result: Record<string, unknown> = {};
  for (const part of source.split(/;\s*/)) {
    const index = part.indexOf('=');
    if (index <= 0) return {};
    result[part.slice(0, index).trim()] = parseScalar(part.slice(index + 1).trim());
  }
  return result;
}

function inlineParamNames(type: string, category: EntryCategory, catalog: Catalog) {
  return (catalogFor(catalog, category).find((entry) => entry.id === type)?.params || [])
    .filter((param) => !param.slotCategory)
    .map((param) => param.name)
    .sort((left, right) => right.length - left.length);
}

function serializeDialogueDocument(doc: DialogueDocument, catalog: Catalog) {
  const root: Record<string, unknown> = { ...doc.extras };
  if (doc.sender !== undefined && doc.sender !== '') root.sender = doc.sender;
  if (doc.npcId !== undefined && doc.npcId !== '') root.npcId = doc.npcId;
  root.canStartFromNPC = doc.canStartFromNPC;
  if (doc.conditions.length > 0) root.conditions = serializeComponentCollection(doc.conditions, catalog);
  root.stages = Object.fromEntries(doc.stages.map((stage) => [stage.key, serializeStage(stage, catalog)]));
  return stringify(root, { lineWidth: 0 });
}

function serializeStage(stage: DialogueStage, catalog: Catalog) {
  const out: Record<string, unknown> = { ...stage.extras, id: stage.id, messages: stage.messages.map(serializeMessageLine), autoNext: stage.autoNext };
  if (stage.conditions.length > 0) out.conditions = serializeComponentCollection(stage.conditions, catalog);
  if (stage.actions.length > 0) out.actions = serializeComponentCollection(stage.actions, catalog);
  out.options = stage.options.map((option) => serializeDialogueOption(option, catalog));
  return out;
}

function validateDialogue(doc: DialogueDocument) {
  const errors: Record<string, string> = {};
  if (doc.parseError) return errors;
  if (doc.stages.length === 0) errors.stages = 'At least one stage is required.';
  if (!doc.stages.some((stage) => stage.key === '0')) errors['stages.0'] = 'Stage 0 is required as the dialogue entry point.';
  const keys = new Set(doc.stages.map((stage) => stage.key));
  doc.stages.forEach((stage) => {
    if (!stage.id.trim()) errors[`stages.${stage.key}.id`] = 'Stage id is required.';
    if (stage.messages.length === 0) errors[`stages.${stage.key}.messages`] = 'At least one message is required.';
    stage.options.forEach((option, index) => {
      if (option.next && !keys.has(option.next)) errors[`stages.${stage.key}.options.${index}.next`] = `Target stage ${option.next} does not exist.`;
    });
  });
  return errors;
}

function addStage(doc: DialogueDocument): DialogueSelection {
  const key = nextNumericKey(doc.stages.map((stage) => stage.key));
  doc.stages.push({ key, id: `stage_${key}`, messages: [{ text: 'en=New message;de=Neue Nachricht', delay: '' }], autoNext: false, conditions: [], actions: [], options: [], extras: {} });
  refreshDialogueIds(doc);
  return { kind: 'stage', stageKey: key };
}

function deleteStage(doc: DialogueDocument, key: string): DialogueSelection {
  doc.stages = doc.stages.filter((stage) => stage.key !== key);
  doc.stages.forEach((stage) => stage.options.forEach((option) => { if (option.next === key) option.next = ''; }));
  refreshDialogueIds(doc);
  return { kind: 'root' };
}

function renameStage(doc: DialogueDocument, oldKey: string, newKey: string): DialogueSelection {
  if (!newKey.trim() || doc.stages.some((stage) => stage.key === newKey && stage.key !== oldKey)) return { kind: 'stage', stageKey: oldKey };
  findStage(doc, oldKey).key = newKey;
  doc.stages.forEach((stage) => stage.options.forEach((option) => { if (option.next === oldKey) option.next = newKey; }));
  refreshDialogueIds(doc);
  return { kind: 'stage', stageKey: newKey };
}

function addMessage(doc: DialogueDocument, stageKey: string): DialogueSelection {
  const stage = findStage(doc, stageKey);
  stage.messages.push({ text: 'en=New message;de=Neue Nachricht', delay: '' });
  return { kind: 'message', stageKey, index: stage.messages.length - 1 };
}

function deleteMessage(doc: DialogueDocument, stageKey: string, index: number): DialogueSelection {
  findStage(doc, stageKey).messages.splice(index, 1);
  return { kind: 'stage', stageKey };
}

function moveMessage(doc: DialogueDocument, stageKey: string, index: number, direction: number): DialogueSelection {
  moveItem(findStage(doc, stageKey).messages, index, direction);
  return { kind: 'message', stageKey, index: index + direction };
}

function addOption(doc: DialogueDocument, stageKey: string): DialogueSelection {
  const stage = findStage(doc, stageKey);
  stage.options.push({ text: 'en=New option;de=Neue Option', hint: '', next: '', default: false, actions: [], extras: [] });
  refreshDialogueIds(doc);
  return { kind: 'option', stageKey, index: stage.options.length - 1 };
}

function deleteOption(doc: DialogueDocument, stageKey: string, index: number): DialogueSelection {
  findStage(doc, stageKey).options.splice(index, 1);
  return { kind: 'stage', stageKey };
}

function moveOption(doc: DialogueDocument, stageKey: string, index: number, direction: number): DialogueSelection {
  moveItem(findStage(doc, stageKey).options, index, direction);
  refreshDialogueIds(doc);
  return { kind: 'option', stageKey, index: index + direction };
}

function addRootEntry(doc: DialogueDocument, type: string, catalog: Catalog): DialogueSelection {
  doc.conditions.push(newEntry(type, 'condition', catalog, doc.conditions, 'conditions'));
  refreshDialogueIds(doc);
  return { kind: 'root' };
}

function updateRootEntry(doc: DialogueDocument, entryId: string, updater: (entry: QxlEntry) => void) {
  const entry = doc.conditions.find((item) => item.id === entryId);
  if (entry) updater(entry);
}

function deleteRootEntry(doc: DialogueDocument, entryId: string) {
  doc.conditions = doc.conditions.filter((entry) => entry.id !== entryId);
}

function addStageEntry(doc: DialogueDocument, stageKey: string, slot: 'conditions' | 'actions', type: string, catalog: Catalog): DialogueSelection {
  const stage = findStage(doc, stageKey);
  const category = slot === 'conditions' ? 'condition' : 'action';
  stage[slot].push(newEntry(type, category, catalog, stage[slot], `stages.${stageKey}.${slot}`));
  refreshDialogueIds(doc);
  return { kind: 'stageEntry', stageKey, slot, entryId: stage[slot][stage[slot].length - 1].id };
}

function updateStageEntry(doc: DialogueDocument, stageKey: string, slot: 'conditions' | 'actions', entryId: string, updater: (entry: QxlEntry) => void) {
  const entry = findStage(doc, stageKey)[slot].find((item) => item.id === entryId);
  if (entry) updater(entry);
}

function deleteStageEntry(doc: DialogueDocument, stageKey: string, slot: 'conditions' | 'actions', entryId: string) {
  const stage = findStage(doc, stageKey);
  stage[slot] = stage[slot].filter((entry) => entry.id !== entryId);
}

function addOptionAction(doc: DialogueDocument, stageKey: string, optionIndex: number, type: string, catalog: Catalog): DialogueSelection {
  const option = findStage(doc, stageKey).options[optionIndex];
  option.actions.push(newEntry(type, 'action', catalog, option.actions, `stages.${stageKey}.options.${optionIndex}.actions`));
  refreshDialogueIds(doc);
  return { kind: 'optionAction', stageKey, optionIndex, entryId: option.actions[option.actions.length - 1].id };
}

function updateOptionAction(doc: DialogueDocument, stageKey: string, optionIndex: number, entryId: string, updater: (entry: QxlEntry) => void) {
  const entry = findStage(doc, stageKey).options[optionIndex].actions.find((item) => item.id === entryId);
  if (entry) updater(entry);
}

function deleteOptionAction(doc: DialogueDocument, stageKey: string, optionIndex: number, entryId: string) {
  const option = findStage(doc, stageKey).options[optionIndex];
  option.actions = option.actions.filter((entry) => entry.id !== entryId);
}

function updateSelectedEntry(doc: DialogueDocument, selection: DialogueSelection, updater: (entry: QxlEntry) => void) {
  if (selection.kind === 'stageEntry') return updateStageEntry(doc, selection.stageKey, selection.slot, selection.entryId, updater);
  if (selection.kind === 'optionAction') return updateOptionAction(doc, selection.stageKey, selection.optionIndex, selection.entryId, updater);
}

function deleteSelectedEntry(doc: DialogueDocument, selection: DialogueSelection): DialogueSelection {
  if (selection.kind === 'stageEntry') deleteStageEntry(doc, selection.stageKey, selection.slot, selection.entryId);
  if (selection.kind === 'optionAction') deleteOptionAction(doc, selection.stageKey, selection.optionIndex, selection.entryId);
  return selection.kind === 'stageEntry' ? { kind: 'stage', stageKey: selection.stageKey } : selection.kind === 'optionAction' ? { kind: 'option', stageKey: selection.stageKey, index: selection.optionIndex } : { kind: 'root' };
}

function newEntry(type: string, category: EntryCategory, catalog: Catalog, siblings: QxlEntry[], path: string): QxlEntry {
  const key = nextKey(siblings.map((entry) => entry.key), type);
  return makeEntry(`${path}.${key}`, key, type, category, defaultParams(type, category, catalog), {}, {});
}

function defaultParams(type: string, category: EntryCategory, catalog: Catalog) {
  const params: Record<string, unknown> = {};
  for (const param of catalogFor(catalog, category).find((entry) => entry.id === type)?.params || []) {
    if (!param.slotCategory && param.required) params[param.name] = defaultValue(param);
  }
  return params;
}

function defaultValue(param: { defaultValue?: string; editorKind?: string; options?: string[] }) {
  if (param.defaultValue && param.defaultValue !== ' ') return parseScalar(param.defaultValue.replaceAll('`', ''));
  if (param.editorKind === 'boolean') return false;
  if (param.editorKind === 'number') return 0;
  if (param.editorKind === 'translatable') return 'en=; de=';
  if (param.editorKind === 'enum' && param.options?.length) return param.options[0];
  return '';
}

function makeEntry(path: string, key: string, type: string, category: EntryCategory, params: Record<string, unknown>, slots: Record<string, QxlEntry[]>, extras: Record<string, unknown>): QxlEntry {
  return { id: `entry:${path}`, key, type, category, params, slots, extras };
}

function refreshDialogueIds(doc: DialogueDocument) {
  doc.conditions.forEach((entry) => assignIds(entry, `conditions.${entry.key}`));
  doc.stages.forEach((stage) => {
    stage.conditions.forEach((entry) => assignIds(entry, `stages.${stage.key}.conditions.${entry.key}`));
    stage.actions.forEach((entry) => assignIds(entry, `stages.${stage.key}.actions.${entry.key}`));
    stage.options.forEach((option, optionIndex) => option.actions.forEach((entry) => assignIds(entry, `stages.${stage.key}.options.${optionIndex}.actions.${entry.key}`)));
  });
}

function assignIds(entry: QxlEntry, path: string) {
  entry.id = `entry:${path}`;
  Object.entries(entry.slots).forEach(([slot, entries]) => entries.forEach((nested) => assignIds(nested, `${path}.${slot}.${nested.key}`)));
}

function normalizeSelection(selection: DialogueSelection, doc: DialogueDocument): DialogueSelection {
  if (selection.kind === 'stage' && !doc.stages.some((stage) => stage.key === selection.stageKey)) return { kind: 'root' };
  if (selection.kind === 'message') {
    const stage = doc.stages.find((item) => item.key === selection.stageKey);
    if (!stage || !stage.messages[selection.index]) return { kind: 'stage', stageKey: selection.stageKey };
  }
  if (selection.kind === 'option') {
    const stage = doc.stages.find((item) => item.key === selection.stageKey);
    if (!stage || !stage.options[selection.index]) return { kind: 'stage', stageKey: selection.stageKey };
  }
  return selection;
}

function cloneDialogue(doc: DialogueDocument): DialogueDocument {
  return JSON.parse(JSON.stringify(doc));
}

function findStage(doc: DialogueDocument, key: string) {
  const stage = doc.stages.find((item) => item.key === key);
  if (!stage) throw new Error(`Stage ${key} does not exist`);
  return stage;
}

function moveItem<T>(items: T[], index: number, direction: number) {
  const target = index + direction;
  if (target < 0 || target >= items.length) return;
  const [item] = items.splice(index, 1);
  items.splice(target, 0, item);
}

function stageLabel(doc: DialogueDocument, key: string) {
  const stage = doc.stages.find((item) => item.key === key);
  return stage ? `Stage ${stage.key}: ${stage.id}` : `Missing stage ${key}`;
}

function dialogueTextPreview(value: unknown) {
  const text = translatableFrom(value);
  const localized: Record<string, string> = {};
  for (const part of text.split(';')) {
    const index = part.indexOf('=');
    if (index <= 0) continue;
    const locale = part.slice(0, index).trim();
    if (locale === 'en' || locale === 'de') localized[locale] = part.slice(index + 1).trim();
  }
  return localized.en || localized.de || text;
}

function dialogueNpcOptionLabel(option: { id: string; label?: string; displayName?: string; displayType?: string; category?: string }) {
  const primary = option.label || option.displayName || option.id;
  const details = [option.id !== primary ? option.id : '', option.displayType || option.category || ''].filter(Boolean).join(', ');
  return details ? `${primary} (${details})` : primary;
}

function uniqueAssetOptions(options: Array<{ id: string }>) {
  const byId = new Map<string, any>();
  for (const option of options || []) {
    if (option.id && !byId.has(option.id)) byId.set(option.id, option);
  }
  return Array.from(byId.values());
}

function mergeParams(entry: QxlEntry, catalogEntry?: ComponentEntry) {
  const names = new Set([...(catalogEntry?.params || []).filter((param) => !param.slotCategory).map((param) => param.name), ...Object.keys(entry.params)]);
  return Array.from(names).map((name) => catalogEntry?.params.find((param) => param.name === name) || { name, required: false, defaultValue: '', description: 'Custom parameter', javaType: 'Object', editorKind: 'raw' as const });
}

function slotDefinitions(category: EntryCategory, catalogEntry?: ComponentEntry): Array<{ name: string; category: EntryCategory }> {
  const slots: Array<{ name: string; category: EntryCategory }> = [];
  if (category === 'action') slots.push({ name: 'conditions', category: 'condition' }, { name: 'runAfter', category: 'action' });
  if (category === 'condition') slots.push({ name: 'onSuccess', category: 'action' }, { name: 'onFail', category: 'action' });
  for (const param of catalogEntry?.params || []) {
    if (param.slotCategory && !slots.some((slot) => slot.name === param.name)) slots.push({ name: param.name, category: param.slotCategory });
  }
  return slots;
}

function catalogFor(catalog: Catalog, category: EntryCategory) {
  if (category === 'action') return catalog.actions;
  if (category === 'condition') return catalog.conditions;
  return catalog.objectives;
}

function isCommonParam(field: string) {
  return ['conditions', 'runAfter', 'onSuccess', 'onFail', 'display', 'displayText'].includes(field);
}

function listValue(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(String);
  if (value == null) return [];
  return [String(value)];
}

function translatableFrom(value: unknown) {
  if (typeof value === 'string') return value;
  if (isPlainObject(value)) return ['en', 'de'].map((locale) => value[locale] ? `${locale}=${value[locale]}` : '').filter(Boolean).join('; ');
  return '';
}

function translatableMap(value: unknown) {
  const result: Record<string, string> = {};
  for (const part of String(value ?? '').split(';')) {
    const index = part.indexOf('=');
    if (index <= 0) continue;
    const locale = part.slice(0, index).trim();
    if (locale === 'en' || locale === 'de') result[locale] = part.slice(index + 1).trim();
  }
  return result;
}

function nextNumericKey(existing: string[]) {
  let index = 0;
  while (existing.includes(String(index))) index++;
  return String(index);
}

function nextKey(existing: string[], base: string) {
  const cleaned = base.replace(/[^A-Za-z0-9_-]/g, '_') || 'entry';
  if (!existing.includes(cleaned)) return cleaned;
  let i = 2;
  while (existing.includes(`${cleaned}_${i}`)) i++;
  return `${cleaned}_${i}`;
}

function parseScalar(value: string): unknown {
  const trimmed = value.trim();
  if (trimmed === 'true') return true;
  if (trimmed === 'false') return false;
  if (trimmed !== '' && !Number.isNaN(Number(trimmed))) return Number(trimmed);
  return value;
}

function escapeRegex(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function asRecord(value: unknown): Record<string, any> {
  return isPlainObject(value) ? value as Record<string, any> : {};
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}
