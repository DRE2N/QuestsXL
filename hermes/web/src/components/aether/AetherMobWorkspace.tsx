import { useMemo, useState } from 'react';
import { ActionIcon, Alert, Badge, Box, Button, Group, NumberInput, Paper, ScrollArea, Select, Stack, Table, Tabs, Text, TextInput, Textarea } from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconCopy, IconPlus, IconTrash } from '@tabler/icons-react';
import { parseDocument, stringify } from 'yaml';
import type { AssetCatalog, AssetOption, Catalog, QxlEntry } from '../../api/types';
import { RawYamlEditor } from '../fields/RawYamlEditor';
import { MiniMessageField, ValueField } from '../fields/ValueField';
import { QxlSlotEditor } from '../qxl/QxlSlotEditor';
import { parseEntryCollection, serializeEntries } from '../../model/qxlDocument';

type MobData = Record<string, any>;
type GoalRow = { priority: number; type: string; args: string[]; raw: string };
type LootItemRow = { item: string; chance: number };
type AttributeRow = { key: string; id: string; value?: number; min?: number; max?: number; extras: Record<string, unknown> };
type LevelRow = { level: string; weight: number; xpMin: number; xpMax: number; xpMode: 'value' | 'range'; items: LootItemRow[]; attributes: AttributeRow[]; extras: Record<string, unknown> };
type SpellTrigger = 'onAttack' | 'onDamaged' | 'onTimer' | 'onDeath' | 'onTarget';
type BehaviorLine = { type: string; args: string[]; raw: string };
type GoalArgSpec = { name: string; kind: 'entity' | 'spell' | 'boolean' | 'enum' | 'number' | 'string'; default?: string | number | boolean; options?: string[]; description?: string };
type GoalSpec = { id: string; args: GoalArgSpec[] };
type BehaviorSpec = { id: string; hint: string; args?: GoalArgSpec[] };

const SPELL_TRIGGERS: SpellTrigger[] = ['onAttack', 'onDamaged', 'onTimer', 'onDeath', 'onTarget'];
const BEHAVIOR_ACTION_SLOTS = ['onEnter', 'onExit', 'onTick', 'onDamaged', 'onAttack', 'onTarget', 'onDeath'];
const EQUIPMENT_SLOTS = ['hand', 'offhand', 'helmet', 'chest', 'leggings', 'boots'];

export function AetherMobWorkspace({ selectedPath, yaml, catalog, assets, disabled, onYamlChange }: { selectedPath: string; yaml: string; catalog: Catalog; assets: AssetCatalog; disabled: boolean; onYamlChange: (yaml: string) => void }) {
  const parsed = useMemo(() => parseMobYaml(yaml), [yaml]);
  const [section, setSection] = useState('identity');
  const data = parsed.data;
  const errors = useMemo(() => validateMob(data, assets), [data, assets]);

  const commit = (next: MobData) => onYamlChange(stringify(next, { lineWidth: 0 }));
  const update = (path: string, value: unknown) => {
    const next = clone(data);
    setPathValue(next, path, value);
    commit(next);
  };
  const replace = (field: string, value: unknown) => {
    const next = clone(data);
    if (value === undefined) delete next[field];
    else next[field] = value;
    commit(next);
  };

  return (
    <Box p="lg">
      <Group justify="space-between" mb="md">
        <Box>
          <Text fw={700} size="lg">{selectedPath || 'New Aether mob'}</Text>
          <Text c="dimmed" size="sm">Aether creature editor</Text>
        </Box>
        <Group gap="xs">
          <Badge variant="outline">aetherMobs</Badge>
          <Badge color={Object.keys(errors).length ? 'red' : 'green'} variant="light">{Object.keys(errors).length ? `${Object.keys(errors).length} issues` : 'valid locally'}</Badge>
        </Group>
      </Group>
      <Tabs defaultValue="editor" className="centerTabs">
        <Tabs.List>
          <Tabs.Tab value="editor">Editor</Tabs.Tab>
          <Tabs.Tab value="yaml">Raw YAML</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="editor" pt="md">
          <ScrollArea h="calc(100vh - 150px)">
            {parsed.error ? (
              <Paper p="md" withBorder><Text c="red" fw={700}>YAML could not be parsed.</Text><Text size="sm">{parsed.error}</Text></Paper>
            ) : (
              <div className="aetherWorkspace">
                <Paper p="md" withBorder className="aetherNav">
                  <Text fw={700}>{mobTitle(data, selectedPath)}</Text>
                  <Text size="sm" c="dimmed">{String(data.displayType || 'pig')} / {simpleClassName(data.class)}</Text>
                  <Stack gap={4} mt="md">
                    {AETHER_SECTIONS.map((item) => (
                      <button key={item.value} type="button" className={`aetherNavButton ${section === item.value ? 'active' : ''}`} onClick={() => setSection(item.value)}>{item.label}</button>
                    ))}
                  </Stack>
                </Paper>
                <Box className="aetherEditorPane">
                  <ValidationSummary errors={errors} />
                  {section === 'identity' && <IdentitySection data={data} assets={assets} disabled={disabled} update={update} replace={replace} />}
                  {section === 'combat' && <CombatSection data={data} assets={assets} disabled={disabled} update={update} replace={replace} />}
                  {section === 'qxl' && <QxlSection data={data} catalog={catalog} assets={assets} disabled={disabled} update={update} />}
                  {section === 'ai' && <AiSection data={data} assets={assets} disabled={disabled} update={update} />}
                  {section === 'behavior' && <BehaviorSection data={data} assets={assets} disabled={disabled} update={update} />}
                  {section === 'loot' && <LootSection data={data} assets={assets} disabled={disabled} update={update} />}
                  {section === 'leveling' && <LevelingSection data={data} assets={assets} disabled={disabled} update={update} />}
                  {section === 'advanced' && <AdvancedSection data={data} disabled={disabled} onChange={commit} />}
                </Box>
              </div>
            )}
          </ScrollArea>
        </Tabs.Panel>
        <Tabs.Panel value="yaml" pt="md">
          <ScrollArea h="calc(100vh - 150px)">
            <RawYamlEditor value={yaml} disabled={disabled} minRows={32} onChange={onYamlChange} />
          </ScrollArea>
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

function IdentitySection({ data, assets, disabled, update, replace = () => undefined }: SectionProps) {
  return (
    <Stack gap="sm">
      <SectionTitle title="Identity" description="Core mob identity, model, localization, and server-side creature class." />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <ValueField label="class" description="Aether creature implementation class." value={data.class || 'de.erethon.aether.creature.AetherBaseMob'} disabled={disabled} onChange={(value) => update('class', value)} />
          <EntityTypeField label="displayType" value={data.displayType || 'pig'} description="Vanilla entity type displayed to players." assets={assets} disabled={disabled} onChange={(value) => update('displayType', stripMinecraft(value))} />
          <Group grow>
            <ValueField label="version" description="Content version marker." value={data.version ?? 1} kind="number" disabled={disabled} onChange={(value) => update('version', value)} />
            <ValueField label="team" description="Optional combat/faction team." value={data.team || ''} disabled={disabled} onChange={(value) => update('team', value)} />
          </Group>
          <MiniMessageField label="name.en" value={asRecord(data.name).en || ''} disabled={disabled} compact onChange={(value) => update('name.en', value)} />
          <MiniMessageField label="name.de" value={asRecord(data.name).de || ''} disabled={disabled} compact onChange={(value) => update('name.de', value)} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Presentation</Text>
          <ValueField label="model / modelId" description="ModelledMob model id. Existing modelId is preserved when present." value={data.modelId ?? data.model ?? ''} disabled={disabled} onChange={(value) => update(data.modelId != null ? 'modelId' : 'model', value)} />
          <ValueField label="skin" description="Mineskin id or skin URL." value={data.skin || ''} disabled={disabled} onChange={(value) => update('skin', value)} />
          <TagsField label="skins" value={data.skins || []} disabled={disabled} onChange={(value) => replace('skins', value)} />
        </Stack>
      </Paper>
    </Stack>
  );
}

function CombatSection({ data, assets, disabled, update }: SectionProps) {
  return (
    <Stack gap="sm">
      <SectionTitle title="Combat" description="Equipment, base attributes, spawn behavior flags, and home location." />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Equipment</Text>
          {EQUIPMENT_SLOTS.map((slot) => (
            <ValueField key={slot} label={slot} value={asRecord(data.equipment)[slot] || 'minecraft:air'} kind="item" assets={assets} disabled={disabled} onChange={(value) => update(`equipment.${slot}`, value || 'minecraft:air')} />
          ))}
        </Stack>
      </Paper>
      <AttributeTable title="Base attributes" value={data.attributes || {}} assets={assets} disabled={disabled} onChange={(value) => update('attributes', value)} />
      <SpellTriggersEditor spells={asRecord(data.spells)} assets={assets} disabled={disabled} onChange={(value) => update('spells', value)} />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Flags</Text>
          <Group grow>
            <ValueField label="instancable" value={data.instancable ?? false} kind="boolean" disabled={disabled} onChange={(value) => update('instancable', value)} />
            <EnumField label="mobCategory" value={data.mobCategory || 'MONSTER'} options={MOB_CATEGORIES} disabled={disabled} onChange={(value) => update('mobCategory', value)} />
          </Group>
          <EntityTypeField
            label="naturalSpawnType"
            value={data.naturalSpawnType || 'zombie'}
            description="Entity type used for natural spawn position checks. Defaults to zombie when unset."
            assets={assets}
            disabled={disabled}
            onChange={(value) => update('naturalSpawnType', stripMinecraft(value))}
          />
          {['collision', 'persistent', 'invulnerable', 'invulnerableToPlayers', 'glowing', 'gravity', 'nametagVisible'].map((field) => (
            <ValueField key={field} label={`config.${field}`} value={asRecord(data.config)[field] ?? defaultConfigFlag(field)} kind="boolean" disabled={disabled} onChange={(value) => update(`config.${field}`, value)} />
          ))}
          <Group grow>
            <ValueField label="config.noDamageTicks" value={asRecord(data.config).noDamageTicks ?? 20} kind="number" disabled={disabled} onChange={(value) => update('config.noDamageTicks', value)} />
            <ValueField label="config.maximumAir" value={asRecord(data.config).maximumAir ?? 20} kind="number" disabled={disabled} onChange={(value) => update('config.maximumAir', value)} />
          </Group>
          <ValueField label="homeLocation" value={data.homeLocation || { x: 0, y: 64, z: 0, range: 32 }} kind="raw" disabled={disabled} onChange={(value) => update('homeLocation', value)} />
        </Stack>
      </Paper>
    </Stack>
  );
}

function QxlSection({ data, catalog, assets, disabled, update }: { data: MobData; catalog: Catalog; assets: AssetCatalog; disabled: boolean; update: (path: string, value: unknown) => void }) {
  const qxl = asRecord(data.qxl);
  const setSlot = (slot: string, entries: QxlEntry[]) => {
    update(`qxl.${slot}`, serializeEntries(entries, catalog));
  };
  const setExtras = (value: unknown) => {
    const next = clone(qxl);
    applyCustomFields(next, value, QXL_SLOT_NAMES);
    update('qxl', next);
  };
  return (
    <Stack gap="sm">
      <SectionTitle title="QXL actions and conditions" description="Aether NPC integration points loaded by AetherHolder." />
      <Paper p="md" withBorder className="slotBox">
        <Text fw={700} mb="xs">Visibility and spawn</Text>
        <div className="sectionGrid">
          {QXL_CONDITION_SLOTS.map((slot) => (
            <QxlSlotEditor
              key={slot.name}
              title={slot.title}
              slot={slot.name}
              category="condition"
              entries={parseEntryCollection(qxl[slot.name], 'condition', `qxl.${slot.name}`, catalog)}
              catalog={catalog}
              assets={assets}
              disabled={disabled}
              onChange={(entries) => setSlot(slot.name, entries)}
            />
          ))}
        </div>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Text fw={700} mb="xs">Player interaction actions</Text>
        <div className="sectionGrid">
          {QXL_INTERACTION_ACTION_SLOTS.map((slot) => (
            <QxlSlotEditor
              key={slot.name}
              title={slot.title}
              slot={slot.name}
              category="action"
              entries={parseEntryCollection(qxl[slot.name], 'action', `qxl.${slot.name}`, catalog)}
              catalog={catalog}
              assets={assets}
              disabled={disabled}
              onChange={(entries) => setSlot(slot.name, entries)}
            />
          ))}
        </div>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Text fw={700} mb="xs">Lifecycle and combat actions</Text>
        <div className="sectionGrid">
          {QXL_LIFECYCLE_ACTION_SLOTS.map((slot) => (
            <QxlSlotEditor
              key={slot.name}
              title={slot.title}
              slot={slot.name}
              category="action"
              entries={parseEntryCollection(qxl[slot.name], 'action', `qxl.${slot.name}`, catalog)}
              catalog={catalog}
              assets={assets}
              disabled={disabled}
              onChange={(entries) => setSlot(slot.name, entries)}
            />
          ))}
        </div>
      </Paper>
      <ValueField label="Unsupported qxl fields" value={customFields(qxl, QXL_SLOT_NAMES)} kind="raw" disabled={disabled} onChange={setExtras} />
    </Stack>
  );
}

function AiSection({ data, assets, disabled, update }: SectionProps) {
  const ai = asRecord(data.ai);
  return (
    <Stack gap="sm">
      <SectionTitle title="AI goals and targets" description="Aether semicolon goal rows with typed fields for known GoalLoader entries." />
      <GoalTable title="Goals" value={listValue(ai.goals)} assets={assets} disabled={disabled} onChange={(value) => update('ai.goals', value)} />
      <GoalTable title="Targets" value={listValue(ai.targets)} assets={assets} disabled={disabled} onChange={(value) => update('ai.targets', value)} />
    </Stack>
  );
}

function BehaviorSection({ data, assets, disabled, update }: SectionProps) {
  const behavior = asRecord(asRecord(data.ai).behavior);
  const states = asRecord(behavior.states);
  const stateIds = Object.keys(states);
  const [selectedState, setSelectedState] = useState(stateIds[0] || '');
  const currentState = selectedState && states[selectedState] ? asRecord(states[selectedState]) : {};
  const setBehavior = (next: Record<string, unknown>) => update('ai.behavior', next);
  const mutateBehavior = (mutator: (draft: Record<string, any>) => void) => {
    const draft = clone(behavior);
    if (!isPlainObject(draft.states)) draft.states = {};
    mutator(draft);
    setBehavior(draft);
  };
  const addState = () => {
    const id = nextKey(stateIds, 'state');
    mutateBehavior((draft) => {
      draft.states[id] = { onEnter: [], onTick: [], transitions: {} };
      if (!draft.initialState) draft.initialState = id;
    });
    setSelectedState(id);
  };
  return (
    <Stack gap="sm">
      <SectionTitle title="Behavior graph" description="Visual state graph for ai.behavior" />
      <Paper p="md" withBorder className="slotBox">
        <Group grow>
          <ValueField label="tickInterval" value={behavior.tickInterval ?? 1} kind="number" disabled={disabled} onChange={(value) => mutateBehavior((draft) => { draft.tickInterval = value; })} />
          <Select label="initialState" data={stateIds} value={String(behavior.initialState || '')} disabled={disabled} searchable onChange={(value) => mutateBehavior((draft) => { draft.initialState = value || ''; })} />
        </Group>
      </Paper>
      <div className="behaviorWorkspace">
        <Paper p="md" withBorder className="behaviorGraph">
          <Group justify="space-between" mb="sm">
            <Text fw={700}>States</Text>
            <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={addState}>State</Button>
          </Group>
          <Stack gap="sm">
            {stateIds.map((stateId) => {
              const state = asRecord(states[stateId]);
              const transitionCount = Object.keys(asRecord(state.transitions)).length;
              return (
                <button key={stateId} type="button" className={`behaviorStateCard ${selectedState === stateId ? 'selectedNode' : ''}`} onClick={() => setSelectedState(stateId)}>
                  <span>{stateId}</span>
                  <small>{transitionCount} transitions</small>
                </button>
              );
            })}
            {stateIds.length === 0 && <Text c="dimmed" size="sm">No behavior states configured.</Text>}
          </Stack>
        </Paper>
        <Paper p="md" withBorder className="behaviorInspector">
          {selectedState ? (
            <Stack gap="sm">
              <Group justify="space-between">
                <Text fw={700}>State {selectedState}</Text>
                <Button size="xs" color="red" variant="subtle" disabled={disabled} onClick={() => {
                  mutateBehavior((draft) => {
                    delete draft.states[selectedState];
                    if (draft.initialState === selectedState) draft.initialState = Object.keys(draft.states)[0] || '';
                  });
                  setSelectedState('');
                }}>Delete</Button>
              </Group>
              <TextInput label="State id" value={selectedState} disabled={disabled} onChange={(event) => {
                const nextId = event.currentTarget.value.trim();
                if (!nextId || nextId === selectedState || stateIds.includes(nextId)) return;
                mutateBehavior((draft) => {
                  draft.states[nextId] = draft.states[selectedState];
                  delete draft.states[selectedState];
                  if (draft.initialState === selectedState) draft.initialState = nextId;
                  for (const state of Object.values(draft.states).map(asRecord)) {
                    for (const transition of Object.values(asRecord(state.transitions)).map(asRecord)) {
                      if (transition.to === selectedState) transition.to = nextId;
                    }
                  }
                });
                setSelectedState(nextId);
              }} />
              {BEHAVIOR_ACTION_SLOTS.map((slot) => (
                <BehaviorLineList key={slot} title={slot} kind="action" value={listValue(currentState[slot])} assets={assets} disabled={disabled} onChange={(value) => mutateBehavior((draft) => { draft.states[selectedState][slot] = value; })} />
              ))}
              <TransitionEditor stateId={selectedState} states={states} assets={assets} disabled={disabled} onChange={(next) => mutateBehavior((draft) => { draft.states[selectedState].transitions = next; })} />
              <ValueField label="Raw state extras" value={customFields(currentState, [...BEHAVIOR_ACTION_SLOTS, 'transitions'])} kind="raw" disabled={disabled} onChange={(value) => mutateBehavior((draft) => { applyCustomFields(draft.states[selectedState], value, [...BEHAVIOR_ACTION_SLOTS, 'transitions']); })} />
            </Stack>
          ) : (
            <Text c="dimmed">Select or add a behavior state.</Text>
          )}
        </Paper>
      </div>
      <GoalProfilesEditor behavior={behavior} assets={assets} disabled={disabled} onChange={(value) => mutateBehavior((draft) => { draft.goalProfiles = value; })} />
    </Stack>
  );
}

function LootSection({ data, assets, disabled, update }: SectionProps) {
  const loot = asRecord(data.loot);
  return (
    <Stack gap="sm">
      <SectionTitle title="Global loot" description="Fallback loot for non-levelled mobs or legacy configs." />
      <Paper p="md" withBorder className="slotBox">
        <ValueField label="loot.xp" value={loot.xp ?? 0} kind="number" disabled={disabled} onChange={(value) => update('loot.xp', value)} />
      </Paper>
      <LootItemsEditor value={listValue(loot.items).map(parseLootItem)} assets={assets} disabled={disabled} onChange={(rows) => update('loot.items', rows.map(serializeLootItem))} />
    </Stack>
  );
}

function LevelingSection({ data, assets, disabled, update }: SectionProps) {
  const levels = normalizeLevels(data.leveling);
  const commit = (rows: LevelRow[]) => update('leveling', serializeLevels(rows));
  const addLevel = () => {
    const level = nextNumericKey(levels.map((row) => row.level), '1');
    commit([...levels, { level, weight: 100, xpMin: 0, xpMax: 0, xpMode: 'range', items: [], attributes: [], extras: {} }]);
  };
  return (
    <Stack gap="sm">
      <Group justify="space-between">
        <SectionTitle title="Leveling" description="Weighted level variants with per-level XP, loot, and attribute ranges." />
        <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={addLevel}>Level</Button>
      </Group>
      {levels.map((level, index) => (
        <LevelCard
          key={`${level.level}-${index}`}
          level={level}
          levels={levels}
          index={index}
          assets={assets}
          disabled={disabled}
          onChange={(next) => commit(levels.map((row, rowIndex) => rowIndex === index ? next : row))}
          onDelete={() => commit(levels.filter((_, rowIndex) => rowIndex !== index))}
          onDuplicate={() => {
            const copy = clone(level);
            copy.level = nextNumericKey(levels.map((row) => row.level), String(Number(level.level || 0) + 1 || 1));
            commit([...levels.slice(0, index + 1), copy, ...levels.slice(index + 1)]);
          }}
        />
      ))}
      {levels.length === 0 && <Paper p="md" withBorder><Text c="dimmed">No level variants configured.</Text></Paper>}
    </Stack>
  );
}

function AdvancedSection({ data, disabled, onChange }: { data: MobData; disabled: boolean; onChange: (data: MobData) => void }) {
  return (
    <Stack gap="sm">
      <SectionTitle title="Advanced raw data" description="Unsupported or uncommon sections are preserved here." />
      <ValueField label="Full mob YAML data" value={data} kind="raw" disabled={disabled} onChange={(value) => isPlainObject(value) && onChange(value)} />
    </Stack>
  );
}

type SectionProps = {
  data: MobData;
  assets: AssetCatalog;
  disabled: boolean;
  update: (path: string, value: unknown) => void;
  replace?: (field: string, value: unknown) => void;
};

function GoalTable({ title, value, assets, disabled, onChange }: { title: string; value: string[]; assets: AssetCatalog; disabled: boolean; onChange: (value: string[]) => void }) {
  const rows = value.map(parseGoalLine);
  const commit = (next: GoalRow[]) => onChange(next.map(serializeGoalLine));
  const update = (index: number, row: GoalRow) => commit(rows.map((item, itemIndex) => itemIndex === index ? row : item));
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm">
        <Text fw={700}>{title}</Text>
        <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => commit([...rows, { priority: rows.length, type: 'float', args: [], raw: '' }])}>Goal</Button>
      </Group>
      <Table withTableBorder withColumnBorders>
        <Table.Thead><Table.Tr><Table.Th w={90}>Priority</Table.Th><Table.Th w={220}>Type</Table.Th><Table.Th>Arguments</Table.Th><Table.Th w={130}>Actions</Table.Th></Table.Tr></Table.Thead>
        <Table.Tbody>
          {rows.map((row, index) => (
            <Table.Tr key={index}>
              <Table.Td><NumberInput value={row.priority} disabled={disabled} onChange={(value) => update(index, { ...row, priority: Number(value || 0) })} /></Table.Td>
              <Table.Td><Select data={GOAL_TYPES.map((type) => ({ value: type.id, label: type.id }))} value={row.type} disabled={disabled} searchable onChange={(value) => update(index, { priority: row.priority, type: value || 'float', args: defaultGoalArgs(value || 'float'), raw: '' })} /></Table.Td>
              <Table.Td><GoalArgsEditor row={row} assets={assets} disabled={disabled} onChange={(args) => update(index, { ...row, args, raw: '' })} /></Table.Td>
              <Table.Td><RowActions disabled={disabled} index={index} length={rows.length} onUp={() => commit(moveArray(rows, index, -1))} onDown={() => commit(moveArray(rows, index, 1))} onDelete={() => commit(rows.filter((_, itemIndex) => itemIndex !== index))} /></Table.Td>
            </Table.Tr>
          ))}
          {rows.length === 0 && <Table.Tr><Table.Td colSpan={4}><Text size="sm" c="dimmed">No {title.toLowerCase()} configured.</Text></Table.Td></Table.Tr>}
        </Table.Tbody>
      </Table>
    </Paper>
  );
}

function GoalArgsEditor({ row, assets, disabled, onChange }: { row: GoalRow; assets: AssetCatalog; disabled: boolean; onChange: (args: string[]) => void }) {
  const spec = GOAL_TYPES.find((goal) => goal.id === row.type);
  if (!spec) return <TextInput value={row.raw || row.args.join(';')} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value.split(';'))} />;
  return (
    <div className="aetherArgsGrid">
      {spec.args.map((arg, index) => {
        const value = row.args[index] ?? '';
        const update = (next: unknown) => {
          const args = [...row.args];
          args[index] = String(next ?? '');
          onChange(args);
        };
        if (arg.kind === 'entity') return <EntityTypeField key={arg.name} label={arg.name} value={value} assets={assets} disabled={disabled} onChange={(next) => update(stripMinecraft(next))} />;
        if (arg.kind === 'spell') return <PickerField key={arg.name} label={arg.name} value={value} options={assets.spellbookSpells || []} disabled={disabled} fallback="Spell id" onChange={update} />;
        if (arg.kind === 'boolean') return <EnumField key={arg.name} label={arg.name} value={value || 'false'} options={['true', 'false']} disabled={disabled} onChange={update} />;
        if (arg.kind === 'enum') return <EnumField key={arg.name} label={arg.name} value={value} options={[...(arg.options || [])]} disabled={disabled} onChange={update} />;
        if (arg.kind === 'number') return <NumberInput key={arg.name} label={arg.name} value={Number(value || 0)} disabled={disabled} onChange={(next) => update(next)} />;
        return <TextInput key={arg.name} label={arg.name} value={value} disabled={disabled} onChange={(event) => update(event.currentTarget.value)} />;
      })}
    </div>
  );
}

function SpellTriggersEditor({ spells, assets, disabled, onChange }: { spells: Record<string, any>; assets: AssetCatalog; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  const updateTrigger = (trigger: SpellTrigger, rows: Array<{ spell: string; chance: number; extras: Record<string, unknown> }>) => {
    const next = clone(spells);
    next[trigger] = Object.fromEntries(rows.filter((row) => row.spell).map((row) => [row.spell, { ...row.extras, chance: row.chance }]));
    onChange(next);
  };
  return (
    <Stack gap="sm">
      {SPELL_TRIGGERS.map((trigger) => {
        const rows = Object.entries(asRecord(spells[trigger])).map(([spell, raw]) => ({ spell, chance: Number(asRecord(raw).chance ?? 0), extras: omit(asRecord(raw), ['chance']) }));
        return (
          <Paper key={trigger} p="md" withBorder className="slotBox">
            <Group justify="space-between" mb="sm">
              <Text fw={700}>{trigger}</Text>
              <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => updateTrigger(trigger, [...rows, { spell: '', chance: 100, extras: {} }])}>Spell</Button>
            </Group>
            <Stack gap="xs">
              {rows.map((row, index) => (
                <Group key={index} align="end" grow>
                  <PickerField label="spell" value={row.spell} options={assets.spellbookSpells || []} disabled={disabled} fallback="Spell id" onChange={(value) => updateTrigger(trigger, rows.map((item, itemIndex) => itemIndex === index ? { ...item, spell: String(value || '') } : item))} />
                  <NumberInput label="chance %" value={row.chance} disabled={disabled} min={0} max={100} onChange={(value) => updateTrigger(trigger, rows.map((item, itemIndex) => itemIndex === index ? { ...item, chance: Number(value || 0) } : item))} />
                  <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => updateTrigger(trigger, rows.filter((_, itemIndex) => itemIndex !== index))}><IconTrash size={16} /></ActionIcon>
                </Group>
              ))}
              {rows.length === 0 && <Text size="sm" c="dimmed">No spells for {trigger}.</Text>}
            </Stack>
          </Paper>
        );
      })}
    </Stack>
  );
}

function AttributeTable({ title, value, assets, disabled, onChange }: { title: string; value: unknown; assets: AssetCatalog; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  const rows = normalizeAttributes(value);
  const commit = (next: AttributeRow[]) => onChange(serializeAttributes(next));
  const update = (index: number, row: AttributeRow) => commit(rows.map((item, itemIndex) => itemIndex === index ? row : item));
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm">
        <Text fw={700}>{title}</Text>
        <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => commit([...rows, { key: String(rows.length), id: 'max_health', value: 1, extras: {} }])}>Attribute</Button>
      </Group>
      <Table withTableBorder withColumnBorders>
        <Table.Thead><Table.Tr><Table.Th>Attribute</Table.Th><Table.Th w={120}>Mode</Table.Th><Table.Th w={130}>Value</Table.Th><Table.Th w={130}>Min</Table.Th><Table.Th w={130}>Max</Table.Th><Table.Th w={130}>Actions</Table.Th></Table.Tr></Table.Thead>
        <Table.Tbody>
          {rows.map((row, index) => {
            const mode = row.min != null || row.max != null ? 'range' : 'value';
            return (
              <Table.Tr key={`${row.key}-${index}`}>
                <Table.Td><PickerField label="" value={row.id} options={assets.attributes || []} disabled={disabled} fallback="attribute" onChange={(value) => update(index, { ...row, id: stripMinecraft(value) })} /></Table.Td>
                <Table.Td><Select data={['value', 'range']} value={mode} disabled={disabled} onChange={(next) => update(index, next === 'range' ? { ...row, value: undefined, min: row.value ?? row.min ?? 0, max: row.value ?? row.max ?? 0 } : { ...row, value: row.min ?? row.value ?? 0, min: undefined, max: undefined })} /></Table.Td>
                <Table.Td><NumberInput value={row.value ?? ''} disabled={disabled || mode !== 'value'} onChange={(value) => update(index, { ...row, value: Number(value || 0) })} /></Table.Td>
                <Table.Td><NumberInput value={row.min ?? ''} disabled={disabled || mode !== 'range'} onChange={(value) => update(index, { ...row, min: Number(value || 0) })} /></Table.Td>
                <Table.Td><NumberInput value={row.max ?? ''} disabled={disabled || mode !== 'range'} onChange={(value) => update(index, { ...row, max: Number(value || 0) })} /></Table.Td>
                <Table.Td><RowActions disabled={disabled} index={index} length={rows.length} onUp={() => commit(moveArray(rows, index, -1))} onDown={() => commit(moveArray(rows, index, 1))} onDelete={() => commit(rows.filter((_, itemIndex) => itemIndex !== index))} /></Table.Td>
              </Table.Tr>
            );
          })}
          {rows.length === 0 && <Table.Tr><Table.Td colSpan={6}><Text size="sm" c="dimmed">No attributes configured.</Text></Table.Td></Table.Tr>}
        </Table.Tbody>
      </Table>
    </Paper>
  );
}

function LootItemsEditor({ value, assets, disabled, onChange }: { value: LootItemRow[]; assets: AssetCatalog; disabled: boolean; onChange: (rows: LootItemRow[]) => void }) {
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm">
        <Text fw={700}>Loot items</Text>
        <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => onChange([...value, { item: '', chance: 100 }])}>Item</Button>
      </Group>
      <Stack gap="xs">
        {value.map((row, index) => (
          <Group key={index} align="end" grow>
            <PickerField label="item" value={row.item} options={assets.items} disabled={disabled} fallback="item id" onChange={(item) => onChange(value.map((entry, entryIndex) => entryIndex === index ? { ...entry, item: String(item || '') } : entry))} />
            <NumberInput label="chance %" value={row.chance} disabled={disabled} min={0} onChange={(chance) => onChange(value.map((entry, entryIndex) => entryIndex === index ? { ...entry, chance: Number(chance || 0) } : entry))} />
            <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onChange(value.filter((_, entryIndex) => entryIndex !== index))}><IconTrash size={16} /></ActionIcon>
          </Group>
        ))}
        {value.length === 0 && <Text size="sm" c="dimmed">No loot items configured.</Text>}
      </Stack>
    </Paper>
  );
}

function LevelCard({ level, levels, index, assets, disabled, onChange, onDelete, onDuplicate }: { level: LevelRow; levels: LevelRow[]; index: number; assets: AssetCatalog; disabled: boolean; onChange: (level: LevelRow) => void; onDelete: () => void; onDuplicate: () => void }) {
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
      <Group grow align="end">
        <TextInput
          label="Level"
          value={levelDraft}
          disabled={disabled}
          inputMode="numeric"
          error={levelError}
          onChange={(event) => setLevelDraft(event.currentTarget.value.replace(/[^\d]/g, ''))}
          onBlur={commitLevel}
          onKeyDown={(event) => {
            if (event.key === 'Enter') event.currentTarget.blur();
            if (event.key === 'Escape') setLevelDraft(level.level);
          }}
        />
        <NumberInput label="Weight" value={level.weight} disabled={disabled} min={0} onChange={(value) => onChange({ ...level, weight: Number(value || 0) })} />
        <Select label="XP mode" data={['range', 'value']} value={level.xpMode} disabled={disabled} onChange={(value) => onChange({ ...level, xpMode: value === 'value' ? 'value' : 'range', xpMax: value === 'value' ? level.xpMin : level.xpMax })} />
        <NumberInput label={level.xpMode === 'value' ? 'XP value' : 'XP min'} value={level.xpMin} disabled={disabled} min={0} onChange={(value) => onChange({ ...level, xpMin: Number(value || 0), xpMax: level.xpMode === 'value' ? Number(value || 0) : level.xpMax })} />
        {level.xpMode === 'range' && <NumberInput label="XP max" value={level.xpMax} disabled={disabled} min={0} onChange={(value) => onChange({ ...level, xpMax: Number(value || 0) })} />}
      </Group>
      <Box mt="md">
        <LootItemsEditor value={level.items} assets={assets} disabled={disabled} onChange={(items) => onChange({ ...level, items })} />
      </Box>
      <Box mt="md">
        <AttributeTable title="Level attributes" value={serializeAttributes(level.attributes)} assets={assets} disabled={disabled} onChange={(value) => onChange({ ...level, attributes: normalizeAttributes(value) })} />
      </Box>
      {Object.keys(level.extras).length > 0 && <Box mt="md"><ValueField label="Level extras" value={level.extras} kind="raw" disabled={disabled} onChange={(value) => onChange({ ...level, extras: asRecord(value) })} /></Box>}
    </Paper>
  );
}

function normalizeLevelKey(value: string) {
  const trimmed = String(value ?? '').trim();
  if (!/^\d+$/.test(trimmed)) return '';
  return String(Number(trimmed));
}

function BehaviorLineList({ title, kind, value, assets, disabled, onChange }: { title: string; kind: 'action' | 'condition'; value: string[]; assets: AssetCatalog; disabled: boolean; onChange: (value: string[]) => void }) {
  const specs = kind === 'action' ? BEHAVIOR_ACTIONS : BEHAVIOR_CONDITIONS;
  const rows = value.map(parseBehaviorLine);
  const commit = (next: BehaviorLine[]) => onChange(next.map((row) => serializeBehaviorLine(row)));
  const update = (index: number, row: BehaviorLine) => commit(rows.map((item, itemIndex) => itemIndex === index ? row : item));
  return (
    <Paper p="sm" withBorder className="slotBox">
      <Group justify="space-between" mb="xs">
        <Text fw={600} size="sm">{title}</Text>
        <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={() => {
          const type = specs[0]?.id || '';
          commit([...rows, { type, args: defaultBehaviorArgs(specs, type), raw: '' }]);
        }}>Add</Button>
      </Group>
      <Stack gap="xs">
        {rows.map((row, index) => {
          const spec = specs.find((item) => item.id === row.type);
          return (
            <Paper key={index} p="xs" withBorder>
              <Group align="end" wrap="nowrap">
                <Select label="type" data={specs.map((item) => ({ value: item.id, label: item.id }))} value={row.type} disabled={disabled} searchable onChange={(value) => update(index, { type: value || '', args: defaultBehaviorArgs(specs, value || ''), raw: '' })} />
                <Box style={{ flex: 1 }}>
                  <BehaviorArgsEditor row={row} spec={spec} assets={assets} disabled={disabled} onChange={(args) => update(index, { ...row, args, raw: '' })} />
                </Box>
                <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => commit(rows.filter((_, itemIndex) => itemIndex !== index))}><IconTrash size={16} /></ActionIcon>
              </Group>
            </Paper>
          );
        })}
        {rows.length === 0 && <Text size="sm" c="dimmed">No entries.</Text>}
      </Stack>
    </Paper>
  );
}

function BehaviorArgsEditor({ row, spec, assets, disabled, onChange }: { row: BehaviorLine; spec?: BehaviorSpec; assets: AssetCatalog; disabled: boolean; onChange: (args: string[]) => void }) {
  if (!spec?.args) {
    return <TextInput label="args" value={row.args.join(';')} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value.split(';').filter(Boolean))} description={spec?.hint || 'Semicolon-separated arguments'} />;
  }
  if (spec.args.length === 0) {
    return <TextInput label="args" value="" disabled description="No arguments" readOnly />;
  }
  return (
    <div className="aetherArgsGrid">
      {spec.args.map((arg, index) => {
        const value = row.args[index] ?? String(arg.default ?? '');
        const update = (next: unknown) => {
          const args = spec.args!.map((item, itemIndex) => row.args[itemIndex] ?? String(item.default ?? ''));
          args[index] = String(next ?? '');
          onChange(args);
        };
        if (arg.kind === 'spell') return <PickerField key={arg.name} label={arg.name} value={value} options={assets.spellbookSpells || []} disabled={disabled} fallback="Spell id" description={arg.description} onChange={update} />;
        if (arg.kind === 'entity') return <EntityTypeField key={arg.name} label={arg.name} value={value} assets={assets} disabled={disabled} description={arg.description} onChange={(next) => update(stripMinecraft(next))} />;
        if (arg.kind === 'boolean') return <EnumField key={arg.name} label={arg.name} value={value || 'false'} options={['true', 'false']} disabled={disabled} onChange={update} />;
        if (arg.kind === 'enum') return <EnumField key={arg.name} label={arg.name} value={value} options={[...(arg.options || [])]} disabled={disabled} onChange={update} />;
        if (arg.kind === 'number') return <NumberInput key={arg.name} label={arg.name} description={arg.description} value={Number(value || 0)} disabled={disabled} onChange={(next) => update(next)} />;
        return <TextInput key={arg.name} label={arg.name} description={arg.description} value={value} disabled={disabled} onChange={(event) => update(event.currentTarget.value)} />;
      })}
    </div>
  );
}

function TransitionEditor({ stateId, states, assets, disabled, onChange }: { stateId: string; states: Record<string, any>; assets: AssetCatalog; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  const transitions = asRecord(asRecord(states[stateId]).transitions);
  const stateOptions = Object.keys(states);
  const updateTransition = (id: string, next: Record<string, unknown>) => onChange({ ...transitions, [id]: next });
  const removeTransition = (id: string) => {
    const next = { ...transitions };
    delete next[id];
    onChange(next);
  };
  const addTransition = () => {
    const id = nextKey(Object.keys(transitions), 'transition');
    updateTransition(id, { to: stateOptions.find((item) => item !== stateId) || stateId, priority: 10, conditions: [] });
  };
  return (
    <Paper p="sm" withBorder className="slotBox">
      <Group justify="space-between" mb="xs">
        <Text fw={600}>Transitions</Text>
        <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={addTransition}>Add</Button>
      </Group>
      <Stack gap="sm">
        {Object.entries(transitions).map(([id, raw]) => {
          const transition = asRecord(raw);
          return (
            <Paper key={id} p="xs" withBorder>
              <Group justify="space-between">
                <Text fw={600} size="sm">{id}</Text>
                <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => removeTransition(id)}><IconTrash size={16} /></ActionIcon>
              </Group>
              <Group grow align="end">
                <Select label="to" data={stateOptions} value={String(transition.to || '')} disabled={disabled} searchable onChange={(value) => updateTransition(id, { ...transition, to: value || '' })} />
                <NumberInput label="priority" value={Number(transition.priority ?? 10)} disabled={disabled} onChange={(value) => updateTransition(id, { ...transition, priority: Number(value || 0) })} />
              </Group>
              <Box mt="sm">
                <BehaviorLineList title="conditions" kind="condition" value={listValue(transition.conditions)} assets={assets} disabled={disabled} onChange={(value) => updateTransition(id, { ...transition, conditions: value })} />
              </Box>
            </Paper>
          );
        })}
        {Object.keys(transitions).length === 0 && <Text size="sm" c="dimmed">No transitions.</Text>}
      </Stack>
    </Paper>
  );
}

function GoalProfilesEditor({ behavior, assets, disabled, onChange }: { behavior: Record<string, any>; assets: AssetCatalog; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  const profiles = asRecord(behavior.goalProfiles);
  const updateProfile = (id: string, next: Record<string, unknown>) => onChange({ ...profiles, [id]: next });
  const add = () => updateProfile(nextKey(Object.keys(profiles), 'profile'), { goals: [], targets: [] });
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm">
        <Text fw={700}>Behavior goal profiles</Text>
        <Button size="xs" variant="light" disabled={disabled} onClick={add}>Profile</Button>
      </Group>
      <Stack gap="sm">
        {Object.entries(profiles).map(([id, raw]) => {
          const profile = asRecord(raw);
          return (
            <Paper key={id} p="sm" withBorder>
              <Text fw={600}>{id}</Text>
              <GoalTable title="Profile goals" value={listValue(profile.goals)} assets={assets} disabled={disabled} onChange={(goals) => updateProfile(id, { ...profile, goals })} />
              <Box mt="sm"><GoalTable title="Profile targets" value={listValue(profile.targets)} assets={assets} disabled={disabled} onChange={(targets) => updateProfile(id, { ...profile, targets })} /></Box>
            </Paper>
          );
        })}
        {Object.keys(profiles).length === 0 && <Text c="dimmed" size="sm">No goal profiles.</Text>}
      </Stack>
    </Paper>
  );
}

function SectionTitle({ title, description }: { title: string; description: string }) {
  return <Box><Text fw={700} size="lg">{title}</Text><Text size="sm" c="dimmed">{description}</Text></Box>;
}

function ValidationSummary({ errors }: { errors: Record<string, string> }) {
  const entries = Object.entries(errors);
  if (entries.length === 0) return <Alert color="green" variant="light" mb="sm">No mob config issues detected. </Alert>;
  return (
    <Alert color="red" variant="light" mb="sm" title={`${entries.length} issue${entries.length === 1 ? '' : 's'}`}>
      <Stack gap={2}>{entries.map(([key, message]) => <Text key={key} size="sm"><b>{key}</b>: {message}</Text>)}</Stack>
    </Alert>
  );
}

function EntityTypeField({ label, value, description, assets, disabled, onChange }: { label: string; value: unknown; description?: string; assets: AssetCatalog; disabled: boolean; onChange: (value: unknown) => void }) {
  return <PickerField label={label} value={String(value ?? '')} options={assets.entityTypes || []} disabled={disabled} fallback="entity type" onChange={onChange} description={description} />;
}

function PickerField({ label, value, options, disabled, fallback, description, onChange }: { label: string; value: unknown; options: AssetOption[]; disabled: boolean; fallback: string; description?: string; onChange: (value: unknown) => void }) {
  const current = value == null ? '' : String(value);
  const data = uniqueOptions(options).map((option) => ({ value: option.id, label: optionLabel(option) }));
  const withCurrent = current && !data.some((option) => option.value === current) ? [...data, { value: current, label: current }] : data;
  if (withCurrent.length === 0) return <TextInput label={label} description={description} placeholder={fallback} value={current} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value)} />;
  return <Select label={label} description={description} value={current} data={withCurrent} disabled={disabled} searchable clearable nothingFoundMessage="No matches" onChange={(value) => onChange(value || '')} />;
}

function EnumField({ label, value, options, disabled, onChange }: { label: string; value: unknown; options: string[]; disabled: boolean; onChange: (value: unknown) => void }) {
  return <Select label={label} value={String(value ?? '')} data={options} disabled={disabled} searchable onChange={(value) => onChange(value || '')} />;
}

function TagsField({ label, value, disabled, onChange }: { label: string; value: unknown; disabled: boolean; onChange: (value: string[]) => void }) {
  const text = Array.isArray(value) ? value.map(String).join('\n') : '';
  return <Textarea label={label} description="One entry per line." value={text} disabled={disabled} autosize minRows={2} onChange={(event) => onChange(event.currentTarget.value.split('\n').map((line) => line.trim()).filter(Boolean))} />;
}

function RowActions({ disabled, index, length, onUp, onDown, onDelete }: { disabled: boolean; index: number; length: number; onUp: () => void; onDown: () => void; onDelete: () => void }) {
  return (
    <Group gap={4}>
      <ActionIcon variant="subtle" disabled={disabled || index === 0} onClick={onUp}><IconArrowUp size={16} /></ActionIcon>
      <ActionIcon variant="subtle" disabled={disabled || index === length - 1} onClick={onDown}><IconArrowDown size={16} /></ActionIcon>
      <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={onDelete}><IconTrash size={16} /></ActionIcon>
    </Group>
  );
}

function parseMobYaml(yaml: string): { data: MobData; error?: string } {
  try {
    const parsed = parseDocument(yaml || '{}').toJS();
    return { data: isPlainObject(parsed) ? parsed : {} };
  } catch (error) {
    return { data: {}, error: error instanceof Error ? error.message : 'Invalid YAML' };
  }
}

function validateMob(data: MobData, assets: AssetCatalog) {
  const errors: Record<string, string> = {};
  if (!String(data.class || '').trim()) errors.class = 'Required. Aether uses this class to instantiate the creature.';
  if (!String(data.displayType || '').trim()) errors.displayType = 'Required. Use a vanilla entity type such as pig, zombie, or player.';
  if (!String(asRecord(data.name).en || '').trim()) errors['name.en'] = 'Recommended. English is the fallback display name.';
  if (!String(asRecord(data.name).de || '').trim()) errors['name.de'] = 'Recommended for German player-facing content.';
  for (const level of normalizeLevels(data.leveling)) {
    if (!/^\d+$/.test(level.level)) errors[`leveling.${level.level}`] = 'Level keys must be numeric.';
    if (level.xpMode === 'range' && level.xpMin > level.xpMax) errors[`leveling.${level.level}.xp`] = 'XP min cannot be greater than XP max.';
    level.attributes.forEach((attribute, index) => {
      if (!attribute.id) errors[`leveling.${level.level}.attributes.${index}`] = 'Attribute id is required.';
      if (attribute.value == null && (attribute.min == null || attribute.max == null)) errors[`leveling.${level.level}.attributes.${index}`] = 'Use either value or min/max.';
    });
  }
  const availableItems = new Set((assets.items || []).map((item) => item.id));
  if (availableItems.size > 0) {
    listValue(asRecord(data.loot).items).map(parseLootItem).forEach((item, index) => {
      if (item.item && !availableItems.has(item.item)) errors[`loot.items.${index}`] = `${item.item} is not in the Hephaestus item catalog.`;
    });
  }
  const behavior = asRecord(asRecord(data.ai).behavior);
  const states = asRecord(behavior.states);
  for (const [stateId, state] of Object.entries(states).map(([key, raw]) => [key, asRecord(raw)] as const)) {
    for (const [transitionId, transition] of Object.entries(asRecord(state.transitions)).map(([key, raw]) => [key, asRecord(raw)] as const)) {
      if (transition.to && !states[String(transition.to)]) errors[`ai.behavior.${stateId}.${transitionId}.to`] = `Target state ${transition.to} does not exist.`;
    }
  }
  return errors;
}

function normalizeAttributes(value: unknown): AttributeRow[] {
  return Object.entries(asRecord(value)).map(([key, raw]) => {
    const row = asRecord(raw);
    return {
      key,
      id: String(row.id || ''),
      value: row.value == null ? undefined : Number(row.value),
      min: row.min == null ? undefined : Number(row.min),
      max: row.max == null ? undefined : Number(row.max),
      extras: omit(row, ['id', 'value', 'min', 'max'])
    };
  });
}

function serializeAttributes(rows: AttributeRow[]) {
  return Object.fromEntries(rows.map((row, index) => {
    const body: Record<string, unknown> = { ...row.extras, id: stripMinecraft(row.id) };
    if (row.min != null || row.max != null) {
      body.min = row.min ?? 0;
      body.max = row.max ?? row.min ?? 0;
    } else {
      body.value = row.value ?? 0;
    }
    return [String(index), body];
  }));
}

function normalizeLevels(value: unknown): LevelRow[] {
  return Object.entries(asRecord(value)).map(([level, raw]) => {
    const row = asRecord(raw);
    const loot = asRecord(row.loot);
    const xp = loot.xp;
    const xpRecord = asRecord(xp);
    const xpIsValue = typeof xp === 'number' || xpRecord.value != null;
    const xpValue = typeof xp === 'number' ? xp : Number(xpRecord.value ?? 0);
    return {
      level,
      weight: Number(row.weight ?? 100),
      xpMin: xpIsValue ? xpValue : Number(xpRecord.min ?? 0),
      xpMax: xpIsValue ? xpValue : Number(xpRecord.max ?? 0),
      xpMode: xpIsValue ? 'value' as const : 'range' as const,
      items: listValue(loot.items).map(parseLootItem),
      attributes: normalizeAttributes(row.attributes),
      extras: omit(row, ['weight', 'loot', 'attributes'])
    };
  }).sort((left, right) => Number(left.level) - Number(right.level));
}

function serializeLevels(rows: LevelRow[]) {
  return Object.fromEntries(rows.map((row) => {
    const body: Record<string, unknown> = { ...row.extras, weight: row.weight };
    const loot: Record<string, unknown> = {};
    loot.xp = row.xpMode === 'value' ? { value: row.xpMin } : { min: row.xpMin, max: row.xpMax };
    if (row.items.length > 0) loot.items = row.items.map(serializeLootItem);
    body.loot = loot;
    if (row.attributes.length > 0) body.attributes = serializeAttributes(row.attributes);
    return [row.level, body];
  }));
}

function parseGoalLine(line: string): GoalRow {
  const parts = String(line ?? '').split(';');
  return { priority: Number(parts[0] || 0), type: parts[1] || '', args: parts.slice(2), raw: line };
}

function serializeGoalLine(row: GoalRow) {
  return [row.priority, row.type, ...row.args].filter((part) => part !== '').join(';');
}

function defaultGoalArgs(type: string) {
  return GOAL_TYPES.find((goal) => goal.id === type)?.args.map((arg) => String(arg.default ?? '')) || [];
}

function parseBehaviorLine(line: string): BehaviorLine {
  const parts = String(line ?? '').split(';');
  return { type: parts[0] || '', args: parts.slice(1), raw: line };
}

function serializeBehaviorLine(row: BehaviorLine) {
  return [row.type, ...row.args].filter((part) => part !== '').join(';');
}

function defaultBehaviorArgs(specs: BehaviorSpec[], type: string) {
  return specs.find((spec) => spec.id === type)?.args?.map((arg) => String(arg.default ?? '')) || [];
}

function parseLootItem(value: string): LootItemRow {
  const [item = '', chance = '100'] = String(value ?? '').split(';');
  return { item, chance: Number(chance || 100) };
}

function serializeLootItem(row: LootItemRow) {
  return `${row.item};${row.chance}`;
}

function parseScalarValue(value: string): unknown {
  if (value === 'true') return true;
  if (value === 'false') return false;
  if (value.trim() !== '' && !Number.isNaN(Number(value))) return Number(value);
  return value;
}

function setPathValue(target: Record<string, any>, path: string, value: unknown) {
  const parts = path.split('.');
  let current = target;
  for (let index = 0; index < parts.length - 1; index++) {
    const part = parts[index];
    if (!isPlainObject(current[part])) current[part] = {};
    current = current[part];
  }
  current[parts[parts.length - 1]] = value;
}

function listValue(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(String);
  if (value == null || isPlainObject(value)) return [];
  return [String(value)];
}

function customFields(data: Record<string, any>, known: string[]) {
  return Object.fromEntries(Object.entries(data).filter(([key]) => !known.includes(key)));
}

function applyCustomFields(target: Record<string, any>, value: unknown, known: string[]) {
  for (const key of Object.keys(target)) {
    if (!known.includes(key)) delete target[key];
  }
  if (isPlainObject(value)) {
    for (const [key, fieldValue] of Object.entries(value)) target[key] = fieldValue;
  }
}

function omit(data: Record<string, any>, keys: string[]) {
  return Object.fromEntries(Object.entries(data).filter(([key]) => !keys.includes(key)));
}

function stripMinecraft(value: unknown) {
  const text = String(value ?? '');
  return text.startsWith('minecraft:') ? text.slice('minecraft:'.length) : text;
}

function defaultConfigFlag(field: string) {
  return !['persistent', 'invulnerable', 'invulnerableToPlayers', 'glowing', 'nametagVisible'].includes(field);
}

function optionLabel(option: AssetOption) {
  const secondary = option.baseItem || option.displayType || option.modelKey || option.category || '';
  const primary = option.label || option.displayName || option.id;
  const details = [option.id !== primary ? option.id : '', secondary].filter(Boolean).join(', ');
  return details ? `${primary} (${details})` : primary;
}

function uniqueOptions(options: AssetOption[]) {
  const byId = new Map<string, AssetOption>();
  for (const option of options || []) {
    if (option.id && !byId.has(option.id)) byId.set(option.id, option);
  }
  return Array.from(byId.values());
}

function nextKey(existing: string[], base: string) {
  const cleaned = base.replace(/[^A-Za-z0-9_-]/g, '_') || 'entry';
  if (!existing.includes(cleaned)) return cleaned;
  let index = 2;
  while (existing.includes(`${cleaned}_${index}`)) index++;
  return `${cleaned}_${index}`;
}

function nextNumericKey(existing: string[], base: string) {
  let index = Number(base || 1);
  while (existing.includes(String(index))) index++;
  return String(index);
}

function moveArray<T>(items: T[], index: number, direction: number) {
  const next = [...items];
  const target = index + direction;
  if (target < 0 || target >= next.length) return next;
  const [item] = next.splice(index, 1);
  next.splice(target, 0, item);
  return next;
}

function mobTitle(data: MobData, selectedPath: string) {
  return String(asRecord(data.name).en || asRecord(data.name).de || selectedPath || 'Aether mob');
}

function simpleClassName(value: unknown) {
  const text = String(value || '');
  if (!text) return 'No class';
  const dot = text.lastIndexOf('.');
  return dot >= 0 ? text.slice(dot + 1) : text;
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value ?? {}));
}

function asRecord(value: unknown): Record<string, any> {
  return isPlainObject(value) ? value as Record<string, any> : {};
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

const AETHER_SECTIONS = [
  { value: 'identity', label: 'Identity' },
  { value: 'combat', label: 'Combat' },
  { value: 'qxl', label: 'QXL' },
  { value: 'ai', label: 'AI' },
  { value: 'behavior', label: 'Behavior' },
  { value: 'loot', label: 'Loot' },
  { value: 'leveling', label: 'Leveling' },
  { value: 'advanced', label: 'Advanced' }
];

const QXL_CONDITION_SLOTS = [
  { name: 'visibilityConditions', title: 'Visibility conditions' },
  { name: 'spawnConditions', title: 'Spawn conditions' }
] as const;

const QXL_INTERACTION_ACTION_SLOTS = [
  { name: 'rightClickActions', title: 'Right click actions' },
  { name: 'leftClickActions', title: 'Left click actions' }
] as const;

const QXL_LIFECYCLE_ACTION_SLOTS = [
  { name: 'spawnActions', title: 'Spawn actions' },
  { name: 'deathActions', title: 'Death actions' },
  { name: 'damageActions', title: 'Damage actions' },
  { name: 'attackActions', title: 'Attack actions' }
] as const;

const QXL_SLOT_NAMES = [
  ...QXL_CONDITION_SLOTS.map((slot) => slot.name),
  ...QXL_INTERACTION_ACTION_SLOTS.map((slot) => slot.name),
  ...QXL_LIFECYCLE_ACTION_SLOTS.map((slot) => slot.name)
];

const MOB_CATEGORIES = ['MONSTER', 'CREATURE', 'AMBIENT', 'AXOLOTLS', 'UNDERGROUND_WATER_CREATURE', 'WATER_CREATURE', 'WATER_AMBIENT', 'MISC'];

const GOAL_TYPES: GoalSpec[] = [
  { id: 'float', args: [] },
  { id: 'melee_attack', args: [{ name: 'speed', kind: 'number', default: 1 }, { name: 'followTargetIfNotSeen', kind: 'boolean', default: false }] },
  { id: 'look_at_players', args: [{ name: 'lookDistance', kind: 'number', default: 20 }, { name: 'probability', kind: 'number', default: 20 }] },
  { id: 'random_stroll', args: [{ name: 'speed', kind: 'number', default: 1 }, { name: 'interval', kind: 'number', default: 20 }, { name: 'checkNoActionTime', kind: 'boolean', default: false }] },
  { id: 'random_look_around', args: [] },
  { id: 'random_swim', args: [{ name: 'speed', kind: 'number', default: 1 }, { name: 'interval', kind: 'number', default: 20 }] },
  { id: 'nearest_attackable', args: [{ name: 'target', kind: 'entity', default: 'player' }, { name: 'checkVisibility', kind: 'boolean', default: false }] },
  { id: 'hurt_target', args: [] },
  { id: 'panic', args: [{ name: 'speed', kind: 'number', default: 1 }] },
  { id: 'leap_at', args: [{ name: 'velocity', kind: 'number', default: 0.4 }] },
  { id: 'ranged_bow_attack', args: [{ name: 'speed', kind: 'number', default: 1 }, { name: 'attackIntervalMin', kind: 'number', default: 20 }, { name: 'range', kind: 'number', default: 15 }] },
  { id: 'ranged_crossbow_attack', args: [{ name: 'attackIntervalMin', kind: 'number', default: 20 }, { name: 'range', kind: 'number', default: 15 }] },
  { id: 'restrict_sun', args: [] },
  { id: 'flee_sun', args: [{ name: 'speed', kind: 'number', default: 1 }] },
  { id: 'avoid_water', args: [{ name: 'speed', kind: 'number', default: 1 }] },
  { id: 'door_interact', args: [] },
  { id: 'avoid_target', args: [{ name: 'entity', kind: 'entity', default: 'player' }, { name: 'maxDistance', kind: 'number', default: 8 }, { name: 'walkSpeed', kind: 'number', default: 1 }, { name: 'sprintSpeed', kind: 'number', default: 1.2 }] },
  { id: 'cast_spellbook_spell', args: [{ name: 'spell', kind: 'spell', default: '' }, { name: 'mode', kind: 'enum', options: ['SELF', 'TARGET', 'LOCATION'], default: 'TARGET' }, { name: 'cooldown', kind: 'number', default: 100 }, { name: 'range', kind: 'number', default: 16 }] },
  { id: 'enemy_team_target', args: [{ name: 'checkVisibility', kind: 'boolean', default: false }] }
] as const;

const BEHAVIOR_ACTIONS: BehaviorSpec[] = [
  { id: 'set_target_nearest_player', hint: 'range', args: [{ name: 'range', kind: 'number', default: 16 }] },
  { id: 'set_target_last_attacker', hint: '', args: [] },
  { id: 'clear_target', hint: '', args: [] },
  { id: 'look_at_target', hint: 'maxYaw;maxPitch', args: [{ name: 'maxYaw', kind: 'number', default: 30 }, { name: 'maxPitch', kind: 'number', default: 30 }] },
  { id: 'move_to_target', hint: 'speed;stopDistance', args: [{ name: 'speed', kind: 'number', default: 1 }, { name: 'stopDistance', kind: 'number', default: 2 }] },
  { id: 'move_away_from_attacker', hint: 'speed;distance', args: [{ name: 'speed', kind: 'number', default: 1 }, { name: 'distance', kind: 'number', default: 8 }] },
  {
    id: 'cast_spell',
    hint: 'spell;mode;cooldown;range',
    args: [
      { name: 'spell', kind: 'spell', default: '' },
      { name: 'mode', kind: 'enum', options: ['SELF', 'TARGET', 'LOCATION'], default: 'TARGET' },
      { name: 'cooldown', kind: 'number', default: 100 },
      { name: 'range', kind: 'number', default: 16 }
    ]
  },
  { id: 'set_cooldown', hint: 'key;ticks', args: [{ name: 'key', kind: 'string', default: 'default' }, { name: 'ticks', kind: 'number', default: 100 }] },
  { id: 'clear_goals', hint: '', args: [] },
  { id: 'reset_default_goals', hint: '', args: [] },
  { id: 'apply_goal_profile', hint: 'profileId', args: [{ name: 'profileId', kind: 'string', default: 'profile' }] }
];

const BEHAVIOR_CONDITIONS: BehaviorSpec[] = [
  { id: 'always', hint: '', args: [] },
  { id: 'random_chance', hint: 'chancePercent', args: [{ name: 'chance %', kind: 'number', default: 50 }] },
  { id: 'target_distance_lt', hint: 'distance', args: [{ name: 'distance', kind: 'number', default: 8 }] },
  { id: 'target_distance_gt', hint: 'distance', args: [{ name: 'distance', kind: 'number', default: 8 }] },
  { id: 'attacker_distance_lt', hint: 'distance', args: [{ name: 'distance', kind: 'number', default: 8 }] },
  { id: 'attacker_distance_gt', hint: 'distance', args: [{ name: 'distance', kind: 'number', default: 8 }] },
  { id: 'health_below_percent', hint: 'percent', args: [{ name: 'health %', kind: 'number', default: 50 }] },
  { id: 'health_above_percent', hint: 'percent', args: [{ name: 'health %', kind: 'number', default: 50 }] },
  { id: 'state_time_at_least', hint: 'ticks', args: [{ name: 'ticks', kind: 'number', default: 20 }] },
  { id: 'has_target', hint: '', args: [] },
  { id: 'cooldown_ready', hint: 'key', args: [{ name: 'key', kind: 'string', default: 'default' }] },
  { id: 'qxl_visibility', hint: '', args: [] },
  { id: 'qxl_spawn', hint: '', args: [] }
];
