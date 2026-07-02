import { useEffect, useMemo, useState } from 'react';
import { ActionIcon, Badge, Box, Code, Divider, Group, HoverCard, Paper, ScrollArea, Select, Stack, Tabs, Text, TextInput, Tooltip } from '@mantine/core';
import { IconPlus, IconSearch } from '@tabler/icons-react';
import type { AddTarget } from '../../model/qxlDocument';
import type { Catalog, ComponentEntry, EntryCategory, QxlDocument, Selection } from '../../api/types';
import { catalogFor, findEntry, rootSlotNames, rootSlotsForInspector, slotDefinitions } from '../../model/qxlDocument';
export function CatalogPanel({ doc, selection, catalog, onAdd, disabled, editableVisual }: { doc: QxlDocument; selection: Selection; catalog: Catalog; onAdd: (category: EntryCategory, entry: ComponentEntry, target?: AddTarget) => void; disabled: boolean; editableVisual: boolean }) {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState<EntryCategory>(() => defaultAddCategory(doc, selection, catalog));
  const targetsByCategory = useMemo(() => ({
    action: editableVisual ? compatibleAddTargets(doc, selection, 'action', catalog) : [],
    objective: editableVisual ? compatibleAddTargets(doc, selection, 'objective', catalog) : [],
    condition: editableVisual ? compatibleAddTargets(doc, selection, 'condition', catalog) : []
  }), [doc, selection, catalog, editableVisual]);
  useEffect(() => {
    const next = defaultAddCategory(doc, selection, catalog);
    if (!editableVisual || targetsByCategory[next].length > 0) setCategory(next);
  }, [doc, selection, catalog, editableVisual, targetsByCategory]);
  const sections: Array<[string, EntryCategory, ComponentEntry[]]> = [['Actions', 'action', catalog.actions], ['Objectives', 'objective', catalog.objectives], ['Conditions', 'condition', catalog.conditions]];
  const entries = catalogFor(catalog, category).filter((entry) => `${entry.id} ${entry.description || ''}`.toLowerCase().includes(query.toLowerCase()));
  const targets = targetsByCategory[category];
  const canAddCategory = !editableVisual || targets.length > 0;
  return (
    <ScrollArea h="calc(100vh - 98px)">
      <Stack p="sm">
        <TextInput leftSection={<IconSearch size={16} />} placeholder="Filter components" value={query} onChange={(event) => setQuery(event.currentTarget.value)} />
        <Tabs value={category} onChange={(value) => value && setCategory(value as EntryCategory)}>
          <Tabs.List grow>
            {sections.map(([title, value]) => <Tabs.Tab key={value} value={value} disabled={editableVisual && targetsByCategory[value].length === 0}>{title}</Tabs.Tab>)}
          </Tabs.List>
        </Tabs>
        {editableVisual && (
          <Text size="xs" c="dimmed">
            Adding to {selectionLabel(doc, selection)}{targets.length === 1 ? ` -> ${targets[0].label}` : targets.length > 1 ? '. Choose a target slot with the + dropdown.' : '. No compatible target for this category.'}
          </Text>
        )}
        <Stack gap={6}>
          {entries.map((entry) => (
            <Paper key={`${category}-${entry.id}`} p="sm" withBorder>
              <Group justify="space-between" align="start" wrap="nowrap">
                <CatalogDocHover entry={entry} category={category} />
                <CatalogAddControl entry={entry} category={category} targets={targets} disabled={disabled || !canAddCategory} editableVisual={editableVisual} onAdd={onAdd} />
              </Group>
            </Paper>
          ))}
          {entries.length === 0 && <Text c="dimmed" size="sm">No matching components.</Text>}
        </Stack>
      </Stack>
    </ScrollArea>
  );
}

function CatalogDocHover({ entry, category }: { entry: ComponentEntry; category: EntryCategory }) {
  const examples = componentExamples(entry);
  const required = entry.params.filter((param) => param.required && !param.slotCategory);
  const slots = entry.params.filter((param) => param.slotCategory);
  return (
    <HoverCard width={520} shadow="xl" openDelay={250} closeDelay={100} withinPortal position="right-start">
      <HoverCard.Target>
        <Box className="catalogDocTarget" style={{ minWidth: 0 }}>
          <Group gap="xs" wrap="nowrap">
            <Text fw={600}>{entry.id}</Text>
            <Badge size="xs" variant="light">{category}</Badge>
          </Group>
          <Text size="xs" c="dimmed" lineClamp={2}>{entry.description || 'No description'}</Text>
        </Box>
      </HoverCard.Target>
      <HoverCard.Dropdown>
        <Stack gap="sm">
          <Box>
            <Group gap="xs">
              <Text fw={700}>{entry.id}</Text>
              <Badge size="sm" variant="light">{entry.label || category}</Badge>
            </Group>
            <Text size="sm" c="dimmed" mt={4}>{entry.description || 'No description available.'}</Text>
          </Box>
          {(required.length > 0 || entry.params.length > 0) && (
            <>
              <Divider />
              <Box>
                <Text fw={700} size="sm" mb={4}>Parameters</Text>
                <Stack gap={5}>
                  {entry.params.filter((param) => !param.slotCategory).map((param) => (
                    <Box key={param.name} className="catalogDocParam">
                      <Group gap={6} wrap="wrap">
                        <Code>{param.name}</Code>
                        <Badge size="xs" color={param.required ? 'red' : 'gray'} variant="light">{param.required ? 'required' : 'optional'}</Badge>
                        <Badge size="xs" variant="outline">{param.editorKind || param.javaType || 'raw'}</Badge>
                        {param.defaultValue && <Badge size="xs" variant="outline">default {param.defaultValue}</Badge>}
                        {param.supportsVariables && <Badge size="xs" color="teal" variant="light">%variables%</Badge>}
                      </Group>
                      {param.description && <Text size="xs" c="dimmed" mt={2}>{param.description}</Text>}
                      {param.options?.length ? <Text size="xs" c="dimmed" mt={2}>Options: {param.options.slice(0, 12).join(', ')}{param.options.length > 12 ? ', ...' : ''}</Text> : null}
                    </Box>
                  ))}
                </Stack>
              </Box>
            </>
          )}
          {slots.length > 0 && (
            <>
              <Divider />
              <Box>
                <Text fw={700} size="sm" mb={4}>Nested slots</Text>
                <Group gap={6}>
                  {slots.map((slot) => <Badge key={slot.name} variant="light">{slot.name}: {slot.slotCategory}</Badge>)}
                </Group>
              </Box>
            </>
          )}
          {entry.providedVariables?.length ? (
            <>
              <Divider />
              <Box>
                <Text fw={700} size="sm" mb={4}>Provided variables</Text>
                <Group gap={6}>
                  {entry.providedVariables.map((variable) => <Badge key={variable.id} variant="light">%{variable.id}%</Badge>)}
                </Group>
              </Box>
            </>
          ) : null}
          {examples.length > 0 && (
            <>
              <Divider />
              <Box>
                <Text fw={700} size="sm" mb={4}>Examples</Text>
                <Stack gap={6}>
                  {examples.map((example, index) => (
                    <Code key={index} block className="catalogExampleCode">{example}</Code>
                  ))}
                </Stack>
              </Box>
            </>
          )}
        </Stack>
      </HoverCard.Dropdown>
    </HoverCard>
  );
}

function componentExamples(entry: ComponentEntry) {
  const examples: string[] = [];
  if (entry.shortExample) examples.push(entry.shortExample);
  if (entry.longExample?.length) examples.push(entry.longExample.join('\n'));
  if (examples.length === 0) {
    const params = entry.params
      .filter((param) => param.required && !param.slotCategory)
      .map((param) => `  ${param.name}: ${param.defaultValue || exampleValue(param)}`)
      .join('\n');
    examples.push(params ? `${entry.id}:\n  type: ${entry.id}\n${params}` : `${entry.id}:\n  type: ${entry.id}`);
  }
  return Array.from(new Set(examples.filter(Boolean)));
}

function exampleValue(param: { editorKind?: string; javaType?: string }) {
  if (param.editorKind === 'number' || param.editorKind === 'duration' || param.editorKind === 'ticksDuration') return '1';
  if (param.editorKind === 'boolean') return 'true';
  if (param.editorKind === 'location') return '{ world: Erethon, x: 0, y: 64, z: 0 }';
  if (param.editorKind === 'item') return 'erethon:item';
  if (param.editorKind === 'mob') return 'MobId';
  if (param.editorKind === 'translatable') return '"en=Text;de=Text"';
  return '...';
}

function CatalogAddControl({ entry, category, targets, disabled, editableVisual, onAdd }: { entry: ComponentEntry; category: EntryCategory; targets: AddTarget[]; disabled: boolean; editableVisual: boolean; onAdd: (category: EntryCategory, entry: ComponentEntry, target?: AddTarget) => void }) {
  const [choosingTarget, setChoosingTarget] = useState(false);
  if (!editableVisual || targets.length <= 1 || !choosingTarget) {
    return (
      <ActionIcon
        variant="subtle"
        disabled={disabled}
        onClick={() => {
          if (editableVisual && targets.length > 1) {
            setChoosingTarget(true);
            return;
          }
          onAdd(category, entry, targets[0]);
        }}
      >
        <IconPlus size={16} />
      </ActionIcon>
    );
  }
  return (
    <Select
      aria-label={`Add ${entry.id} to`}
      placeholder="Target"
      w={150}
      size="xs"
      disabled={disabled}
      data={targets.map((target) => ({ value: target.id, label: target.label }))}
      value={null}
      clearable
      autoFocus
      dropdownOpened={choosingTarget}
      onBlur={() => setChoosingTarget(false)}
      onChange={(value: string | null) => {
        const target = targets.find((item) => item.id === value);
        if (target) {
          onAdd(category, entry, target);
          setChoosingTarget(false);
        }
      }}
    />
  );
}

function compatibleAddTargets(doc: QxlDocument, selection: Selection, category: EntryCategory, catalog: Catalog): AddTarget[] {
  if (selection.kind === 'stage') {
    const stage = doc.stages.find((item) => item.id === selection.id);
    if (!stage) return [];
    if (category === 'action') return [
      { id: `stage:${stage.id}:onStart`, label: 'On start', category, kind: 'stage', ownerId: stage.id, slot: 'onStart' },
      { id: `stage:${stage.id}:onFinish`, label: 'On finish', category, kind: 'stage', ownerId: stage.id, slot: 'onFinish' }
    ];
    if (category === 'condition') return [{ id: `stage:${stage.id}:conditions`, label: 'Conditions', category, kind: 'stage', ownerId: stage.id, slot: 'conditions' }];
    if (category === 'objective') return [{ id: `stage:${stage.id}:objectives`, label: 'Objectives', category, kind: 'stage', ownerId: stage.id, slot: 'objectives' }];
  }
  if (selection.kind === 'entry') {
    const entry = findEntry(doc, selection.id)?.entry;
    if (!entry) return [];
    const catalogEntry = catalogFor(catalog, entry.category).find((item) => item.id === entry.type);
    return slotDefinitions(entry.category, entry.type, catalogEntry)
      .filter((slot) => slot.category === category)
      .map((slot) => ({ id: `entry:${entry.id}:${slot.name}`, label: slotLabel(slot.name), category, kind: 'entry', ownerId: entry.id, slot: slot.name }));
  }
  if (selection.kind === 'interaction') {
    if (category !== 'objective') return [];
    const interaction = doc.interactions.find((item) => item.id === selection.id);
    return interaction ? [{ id: `interaction:${interaction.id}:objectives`, label: 'Objectives', category, kind: 'interaction', ownerId: interaction.id, slot: 'objectives' }] : [];
  }
  const targets: AddTarget[] = [];
  for (const slot of rootSlotNames(doc.type)) {
    if (slot.category === category) targets.push({ id: `root:${slot.name}`, label: slotLabel(slot.name), category, kind: 'root', slot: slot.name });
  }
  for (const slot of rootSlotsForInspector(doc)) {
    if (slot.category === category && !targets.some((target) => target.slot === slot.name)) targets.push({ id: `root:${slot.name}`, label: slotLabel(slot.name), category, kind: 'root', slot: slot.name });
  }
  if (category === 'action') {
    for (const reward of doc.rewards) targets.push({ id: `reward:${reward.threshold}`, label: `Reward ${reward.threshold}`, category, kind: 'reward', threshold: reward.threshold });
  }
  if (doc.type === 'globalObjectives' && category === 'objective' && !targets.some((target) => target.slot === 'objectives')) targets.push({ id: 'root:objectives', label: 'Objectives', category, kind: 'root', slot: 'objectives' });
  if (doc.type === 'macros' && category === 'action' && targets.length === 0) targets.push({ id: 'root:new_macro', label: 'New macro', category, kind: 'root', slot: 'new_macro' });
  if (doc.type === 'periodicQuests' && category === 'action' && targets.length === 0) targets.push({ id: 'root:daily.completionRewards', label: 'Daily completion rewards', category, kind: 'root', slot: 'daily.completionRewards' });
  return targets;
}

function defaultAddCategory(doc: QxlDocument, selection: Selection, catalog: Catalog): EntryCategory {
  if (selection.kind === 'stage') return 'objective';
  if (selection.kind === 'interaction') return 'objective';
  if (selection.kind === 'entry') {
    const entry = findEntry(doc, selection.id)?.entry;
    if (entry) {
      for (const preferred of ['action', 'condition', 'objective'] as EntryCategory[]) {
        if (compatibleAddTargets(doc, selection, preferred, catalog).length > 0) return preferred;
      }
    }
  }
  if (compatibleAddTargets(doc, selection, 'action', catalog).length > 0) return 'action';
  if (compatibleAddTargets(doc, selection, 'objective', catalog).length > 0) return 'objective';
  return 'condition';
}

function selectionLabel(doc: QxlDocument, selection: Selection) {
  if (selection.kind === 'root') return 'root';
  if (selection.kind === 'stage') return `stage ${selection.id}`;
  if (selection.kind === 'interaction') return `interaction ${selection.id}`;
  const entry = findEntry(doc, selection.id)?.entry;
  return entry ? `${entry.key} (${entry.type})` : 'selected entry';
}

function slotLabel(slot: string) {
  return slot
    .replace(/\./g, ' / ')
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/^./, (letter) => letter.toUpperCase());
}

