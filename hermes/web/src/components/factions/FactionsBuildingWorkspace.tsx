import { useEffect, useMemo, useState } from 'react';
import { Alert, Badge, Box, Button, Collapse, Group, MultiSelect, NumberInput, Paper, ScrollArea, Select, Stack, Table, Tabs, Text, TextInput } from '@mantine/core';
import { IconPlus, IconTrash } from '@tabler/icons-react';
import { parseDocument, stringify } from 'yaml';
import type { AssetCatalog } from '../../api/types';
import { MiniMessageField, ValueField } from '../fields/ValueField';
import { RawYamlEditor } from '../fields/RawYamlEditor';

type BuildingData = Record<string, any>;
type ContentAccess = {
  load: (type: string, path: string) => Promise<{ yaml: string; baseYaml: string; path: string; type: string }>;
  update: (type: string, path: string, yaml: string) => void;
};
type BuildingTagRow = { id: string; minecraftTags: string[]; materials: string[]; references: string[] };

const BUILDING_TAGS_PATH = 'buildingTags.yml';
const ADD_CUSTOM_TAG = '__add_custom_tag__';

export function FactionsBuildingWorkspace({ selectedPath, yaml, assets, disabled, onYamlChange, contentAccess }: { selectedPath: string; yaml: string; assets: AssetCatalog; disabled: boolean; onYamlChange: (yaml: string) => void; contentAccess?: ContentAccess }) {
  const [tagsYaml, setTagsYaml] = useState('');
  const [tagsError, setTagsError] = useState('');
  const parsed = useMemo(() => parseBuildingYaml(yaml), [yaml]);
  const data = parsed.data;
  const customTags = useMemo(() => parseBuildingTags(tagsYaml, assets), [tagsYaml, assets]);
  const updateData = (next: BuildingData) => onYamlChange(stringify(next));
  const updateTags = (next: BuildingTagRow[]) => {
    const nextYaml = serializeBuildingTags(next);
    setTagsYaml(nextYaml);
    contentAccess?.update('factionsBuildingTags', BUILDING_TAGS_PATH, nextYaml);
  };
  const updateField = (field: string, value: unknown) => {
    const next = structuredClone(data);
    setPathValue(next, field, value);
    updateData(next);
  };
  useEffect(() => {
    let cancelled = false;
    if (!contentAccess) return;
    contentAccess.load('factionsBuildingTags', BUILDING_TAGS_PATH)
      .then((loaded) => {
        if (!cancelled) setTagsYaml(loaded.yaml || 'tags: {}\n');
      })
      .catch((error) => {
        if (!cancelled) setTagsError(error instanceof Error ? error.message : String(error));
      });
    return () => { cancelled = true; };
  }, []);
  const errors = validateBuilding(data, assets, selectedPath, customTags);
  const blockOptions = blockRequirementOptions(assets, customTags);
  return (
    <ScrollArea h="calc(100vh - 54px)">
      <Box p="lg">
        <Group justify="space-between" mb="md">
          <Box>
            <Text fw={700} size="lg">{selectedPath || 'New building'}</Text>
            <Text c="dimmed" size="sm">Factions building definition</Text>
          </Box>
          <Group gap="xs">
            <Badge variant="outline">Factions</Badge>
            <Badge color={errors.length === 0 ? 'green' : 'red'} variant="light">{errors.length === 0 ? 'valid hints' : `${errors.length} hints`}</Badge>
          </Group>
        </Group>
        {parsed.error && <Paper p="md" withBorder><Text c="red" fw={700}>YAML could not be parsed.</Text><Text size="sm">{parsed.error}</Text></Paper>}
        {tagsError && <Alert color="yellow" variant="light" mb="md" title="Building tags unavailable">{tagsError}</Alert>}
        {!parsed.error && (
          <Tabs defaultValue="editor" className="centerTabs">
            <Tabs.List>
              <Tabs.Tab value="editor">Editor</Tabs.Tab>
              <Tabs.Tab value="yaml">Raw YAML</Tabs.Tab>
              <Tabs.Tab value="buildingTagsYaml">Building Tags YAML</Tabs.Tab>
            </Tabs.List>
            <Tabs.Panel value="editor" pt="md">
              <Stack gap="md">
                {errors.length > 0 && <Alert color="red" variant="light" title="Validation hints">{errors.map((error) => <Text key={error} size="sm">{error}</Text>)}</Alert>}
                <Paper p="md" withBorder className="slotBox">
                  <Stack gap="sm">
                    <Text fw={700}>Building</Text>
                    <Group grow align="start">
                      <NumberInput label="size" description="Square building footprint size. Must be greater than 0." value={Number(data.size ?? 1)} disabled={disabled} min={1} onChange={(value) => updateField('size', Number(value || 1))} />
                      <ValueField label="allowOverlap" description="Allow this building footprint to overlap with existing build sites." value={Boolean(data.allowOverlap)} kind="boolean" disabled={disabled} onChange={(value) => updateField('allowOverlap', value)} />
                    </Group>
                    <PopulationRequirementEditor value={asRecord(data.requiredPopulation)} assets={assets} disabled={disabled} onChange={(value) => updateField('requiredPopulation', value)} />
                    <RegionTypesEditor value={data.requiredRegionTypes} assets={assets} disabled={disabled} onChange={(value) => updateField('requiredRegionTypes', value)} />
                  </Stack>
                </Paper>
                <Paper p="md" withBorder className="slotBox">
                  <Stack gap="sm">
                    <Text fw={700}>Costs and upgrades</Text>
                    <MapEditor title="unlockCost" value={asRecord(data.unlockCost)} disabled={disabled} keyPlaceholder="resource" valuePlaceholder="amount" options={assets.factionsResources || []} onChange={(value) => updateField('unlockCost', value)} />
                    <MapEditor title="requiredBuildings" value={asRecord(data.requiredBuildings)} disabled={disabled} keyPlaceholder="building_id" valuePlaceholder="count" options={assets.factionsBuildings || []} onChange={(value) => updateField('requiredBuildings', value)} />
                    <BlockRequirementEditor title="requiredBlocks" value={asRecord(data.requiredBlocks)} disabled={disabled} assets={assets} tags={customTags} options={blockOptions} onTagsChange={updateTags} onChange={(value) => updateField('requiredBlocks', value)} />
                    <UpgradeEditor value={asRecord(data.upgrade)} currentBuilding={buildingIdFromPath(selectedPath)} disabled={disabled} assets={assets} tags={customTags} blockOptions={blockOptions} onTagsChange={updateTags} onChange={(value) => updateField('upgrade', value)} />
                  </Stack>
                </Paper>
                <BuildingEffectsEditor value={asRecord(data.effects)} disabled={disabled} assets={assets} onChange={(value) => updateField('effects', value)} />
                <Paper p="md" withBorder className="slotBox">
                  <Text fw={700}>Advanced extras</Text>
                  <ValueField label="Unsupported fields" value={customFields(data, ['size', 'allowOverlap', 'requiredPopulation', 'requiredRegionTypes', 'unlockCost', 'upgrade', 'requiredBuildings', 'requiredBlocks', 'effects'])} kind="raw" disabled={disabled} onChange={(value) => applyCustomFields(data, value, ['size', 'allowOverlap', 'requiredPopulation', 'requiredRegionTypes', 'unlockCost', 'upgrade', 'requiredBuildings', 'requiredBlocks', 'effects'], updateField)} />
                </Paper>
              </Stack>
            </Tabs.Panel>
            <Tabs.Panel value="yaml" pt="md">
              <RawYamlEditor value={yaml} disabled={disabled} minRows={32} onChange={onYamlChange} />
            </Tabs.Panel>
            <Tabs.Panel value="buildingTagsYaml" pt="md">
              <RawYamlEditor value={tagsYaml} disabled={disabled} minRows={32} onChange={(next) => { setTagsYaml(next); contentAccess?.update('factionsBuildingTags', BUILDING_TAGS_PATH, next); }} />
            </Tabs.Panel>
          </Tabs>
        )}
      </Box>
    </ScrollArea>
  );
}

function BuildingEffectsEditor({ value, disabled, assets, onChange }: { value: Record<string, any>; disabled: boolean; assets: AssetCatalog; onChange: (value: Record<string, any>) => void }) {
  const effectOptions = (assets.factionsBuildingEffects || []).map((effect) => ({ value: effect.id, label: effect.label || effect.id }));
  const entries = Object.entries(value);
  const updateEffect = (key: string, nextKey: string, nextValue: Record<string, any>) => {
    const next = { ...value };
    delete next[key];
    next[nextKey] = nextValue;
    onChange(next);
  };
  const addEffect = () => {
    let index = entries.length + 1;
    let key = 'ResourceProduction';
    while (value[key]) key = `ResourceProduction_${index++}`;
    onChange({ ...value, [key]: { production: {} } });
  };
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm">
        <Box>
          <Text fw={700}>Building effects</Text>
          <Text size="sm" c="dimmed">Effect keys may include a suffix after _, while Factions uses the part before _ as the effect class.</Text>
        </Box>
        <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={addEffect}>Add effect</Button>
      </Group>
      <Stack gap="sm">
        {entries.map(([key, raw]) => {
          const effect = asRecord(raw);
          const type = key.replace(/_.*/, '');
          const suffix = key.includes('_') ? key.slice(key.indexOf('_') + 1) : '';
          const setTypeSuffix = (nextType: string, nextSuffix: string) => updateEffect(key, nextSuffix.trim() ? `${nextType}_${nextSuffix.trim()}` : nextType, effect);
          const updateParam = (field: string, fieldValue: unknown) => updateEffect(key, key, { ...effect, [field]: fieldValue });
          return (
            <Paper key={key} p="sm" withBorder>
              <Stack gap="sm">
                <Group justify="space-between" align="end">
                  <Group grow align="end" style={{ flex: 1 }}>
                    <Select label="Effect type" value={type} data={effectOptions} searchable disabled={disabled} onChange={(next) => next && setTypeSuffix(next, suffix)} />
                    <TextInput label="Instance suffix" value={suffix} placeholder="optional" disabled={disabled} onChange={(event) => setTypeSuffix(type, event.currentTarget.value)} />
                  </Group>
                  <Button size="xs" variant="subtle" color="red" leftSection={<IconTrash size={14} />} disabled={disabled} onClick={() => { const next = { ...value }; delete next[key]; onChange(next); }}>Remove</Button>
                </Group>
                <MiniMessageField label="displayName" value={effect.displayName ?? type} compact disabled={disabled} onChange={(next) => updateParam('displayName', next)} />
                <ValueField label="isTickable" value={Boolean(effect.isTickable)} kind="boolean" disabled={disabled} onChange={(next) => updateParam('isTickable', next)} />
                <EffectSpecificFields type={type} effect={effect} assets={assets} disabled={disabled} onChange={updateParam} />
              </Stack>
            </Paper>
          );
        })}
        {entries.length === 0 && <Text size="sm" c="dimmed">No effects configured.</Text>}
      </Stack>
    </Paper>
  );
}

function EffectSpecificFields({ type, effect, assets, disabled, onChange }: { type: string; effect: Record<string, any>; assets: AssetCatalog; disabled: boolean; onChange: (field: string, value: unknown) => void }) {
  const common = ['displayName', 'isTickable'];
  if (['ResourceProduction', 'ResourceConsumption', 'IncreaseResourceStorage'].includes(type)) {
    const field = type === 'ResourceProduction' ? 'production' : 'consumption';
    const actualField = type === 'IncreaseResourceStorage' ? 'storageIncrease' : field;
    return <MapEditor title={actualField} value={asRecord(effect[actualField])} disabled={disabled} keyPlaceholder="resource" valuePlaceholder="amount" options={assets.factionsResources || []} onChange={(value) => onChange(actualField, value)} />;
  }
  if (type === 'BlockDependentResourceProduction') {
    return (
      <Stack gap="sm">
        <NumberInput label="maximumCountedBlocks" value={Number(effect.maximumCountedBlocks ?? 200)} disabled={disabled} onChange={(value) => onChange('maximumCountedBlocks', Number(value || 0))} />
        <MapEditor title="production" value={asRecord(effect.production)} disabled={disabled} keyPlaceholder="resource" valuePlaceholder="amount" options={assets.factionsResources || []} onChange={(value) => onChange('production', value)} />
        <MapEditor title="blockModifiers" value={asRecord(effect.blockModifiers)} disabled={disabled} keyPlaceholder="wheat" valuePlaceholder="modifier" options={blockRequirementOptions(assets, [])} onChange={(value) => onChange('blockModifiers', value)} />
      </Stack>
    );
  }
  if (type === 'ChangeAttribute') {
    return (
      <Group grow align="start">
        <SelectWithCustom label="attribute" value={effect.attribute || ''} options={assets.factionsAttributes || []} disabled={disabled} placeholder="production_rate" onChange={(value) => onChange('attribute', value)} />
        <NumberInput label="value" value={Number(effect.value ?? 1)} disabled={disabled} onChange={(value) => onChange('value', Number(value || 0))} />
        <Select label="operation" value={String(effect.operation || 'ADD_NUMBER').toUpperCase()} data={modifierOperations} searchable disabled={disabled} onChange={(value) => onChange('operation', value || 'ADD_NUMBER')} />
      </Group>
    );
  }
  if (type === 'SpawnNPC') {
    return <ValueField label="id" description="Aether NPC or mob id to spawn." value={effect.id || ''} kind="mob" assets={assets} disabled={disabled} onChange={(value) => onChange('id', value)} />;
  }
  if (type === 'ItemProduction') {
    return (
      <Stack gap="sm">
        <NumberInput label="interval" value={Number(effect.interval ?? 60)} disabled={disabled} onChange={(value) => onChange('interval', Number(value || 0))} />
        <ValueField label="production" description="Map of output rows. Each row supports id and amount." value={effect.production || {}} kind="raw" disabled={disabled} onChange={(value) => onChange('production', value)} />
      </Stack>
    );
  }
  if (type === 'ItemConversion') {
    return (
      <Stack gap="sm">
        <NumberInput label="buildingTicksPerItem" value={Number(effect.buildingTicksPerItem ?? 20)} disabled={disabled} onChange={(value) => onChange('buildingTicksPerItem', Number(value || 0))} />
        <ValueField label="conversion" description="Map of input item id to output item id." value={effect.conversion || {}} kind="raw" disabled={disabled} onChange={(value) => onChange('conversion', value)} />
      </Stack>
    );
  }
  if (type === 'AddHousing') {
    return (
      <Group grow align="start">
        <SelectWithCustom label="level" value={String(effect.level || 'peasant').toLowerCase()} options={assets.factionsPopulationLevels || []} disabled={disabled} placeholder="peasant" onChange={(value) => onChange('level', value)} />
        <NumberInput label="amount" value={Number(effect.amount ?? 0)} disabled={disabled} onChange={(value) => onChange('amount', Number(value || 0))} />
        <NumberInput label="citizenSpawnChance" value={Number(effect.citizenSpawnChance ?? 0.1)} disabled={disabled} onChange={(value) => onChange('citizenSpawnChance', Number(value || 0))} />
      </Group>
    );
  }
  if (type === 'PopulationTax') {
    return (
      <Stack gap="sm">
        <MapEditor title="taxes" value={asRecord(effect.taxes)} disabled={disabled} keyPlaceholder="peasant" valuePlaceholder="tax" options={assets.factionsPopulationLevels || []} onChange={(value) => onChange('taxes', value)} />
        <NumberInput label="maximumTotal" value={Number(effect.maximumTotal ?? 10)} disabled={disabled} onChange={(value) => onChange('maximumTotal', Number(value || 0))} />
      </Stack>
    );
  }
  if (type === 'AddHappiness') {
    return (
      <Stack gap="sm">
        <TextInput label="name" value={String(effect.name || '')} disabled={disabled} onChange={(event) => onChange('name', event.currentTarget.value)} />
        <MapEditor title="levels" value={asRecord(effect.levels)} disabled={disabled} keyPlaceholder="peasant" valuePlaceholder="modifier" options={assets.factionsPopulationLevels || []} onChange={(value) => onChange('levels', value)} />
      </Stack>
    );
  }
  return <ValueField label="Effect parameters" value={customFields(effect, common)} kind="raw" disabled={disabled} onChange={(value) => applyCustomFields(effect, value, common, onChange)} />;
}

const modifierOperations = ['ADD_NUMBER', 'ADD_SCALAR', 'MULTIPLY_SCALAR_1'].map((operation) => ({ value: operation, label: operation }));

function PopulationRequirementEditor({ value, assets, disabled, onChange }: { value: Record<string, any>; assets: AssetCatalog; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  return <MapEditor title="requiredPopulation" value={value} disabled={disabled} keyPlaceholder="peasant" valuePlaceholder="amount" options={assets.factionsPopulationLevels || []} onChange={onChange} />;
}

function RegionTypesEditor({ value, assets, disabled, onChange }: { value: unknown; assets: AssetCatalog; disabled: boolean; onChange: (value: Record<string, boolean>) => void }) {
  const current = mapKeysOrList(value);
  const options = uniqueOptions((assets.factionsRegionTypes || []).map((option) => ({ value: option.id, label: option.label || option.id })));
  const data = [...options, ...current.filter((item) => !options.some((option) => option.value === item)).map((item) => ({ value: item, label: item }))];
  return (
    <MultiSelect
      label="requiredRegionTypes"
      description="Region types where this building may be placed. Saved as a YAML map for Factions."
      value={current}
      data={data}
      searchable
      clearable
      disabled={disabled}
      nothingFoundMessage="No region types"
      onChange={(next) => onChange(Object.fromEntries(next.map((key) => [key, true])))}
    />
  );
}

function UpgradeEditor({ value, currentBuilding, disabled, assets, tags, blockOptions, onTagsChange, onChange }: { value: Record<string, any>; currentBuilding: string; disabled: boolean; assets: AssetCatalog; tags: BuildingTagRow[]; blockOptions: Array<{ id: string; label?: string }>; onTagsChange: (tags: BuildingTagRow[]) => void; onChange: (value: Record<string, unknown>) => void }) {
  const update = (field: string, fieldValue: unknown) => onChange({ ...value, [field]: fieldValue });
  const extras = customFields(value, ['targetBuilding', 'requiredSatisfiedPaydays', 'requiredBlocks']);
  return (
    <Paper p="sm" withBorder>
      <Stack gap="sm">
        <Text fw={600}>upgrade</Text>
        <Group grow align="start">
          <SelectWithCustom label="targetBuilding" value={value.targetBuilding || ''} options={assets.factionsBuildings || []} disabled={disabled} placeholder="citizen_house" onChange={(next) => update('targetBuilding', next)} />
          <NumberInput label="requiredSatisfiedPaydays" value={Number(value.requiredSatisfiedPaydays ?? 3)} disabled={disabled} min={0} onChange={(next) => update('requiredSatisfiedPaydays', Number(next || 0))} />
        </Group>
        {value.targetBuilding === currentBuilding && <Text size="xs" c="red">Upgrade target should not be the same as this building.</Text>}
        <BlockRequirementEditor title="upgrade.requiredBlocks" value={asRecord(value.requiredBlocks)} disabled={disabled} assets={assets} tags={tags} options={blockOptions} onTagsChange={onTagsChange} onChange={(next) => update('requiredBlocks', next)} />
        {Object.keys(extras).length > 0 && <ValueField label="Unsupported upgrade fields" value={extras} kind="raw" disabled={disabled} onChange={(next) => applyCustomFields(value, next, ['targetBuilding', 'requiredSatisfiedPaydays', 'requiredBlocks'], update)} />}
      </Stack>
    </Paper>
  );
}

function BlockRequirementEditor({ title, value, disabled, assets, tags, options, onTagsChange, onChange }: { title: string; value: Record<string, any>; disabled: boolean; assets: AssetCatalog; tags: BuildingTagRow[]; options: Array<{ id: string; label?: string }>; onTagsChange: (tags: BuildingTagRow[]) => void; onChange: (value: Record<string, unknown>) => void }) {
  const [editing, setEditing] = useState<BuildingTagRow | null>(null);
  const [insertForKey, setInsertForKey] = useState<string | null>(null);
  const [tagEditorOpen, setTagEditorOpen] = useState(false);
  const rows = Object.entries(value);
  const optionData = uniqueOptions([
    { value: ADD_CUSTOM_TAG, label: 'Add custom tag' },
    ...options.map((option) => ({ value: option.id, label: option.label || option.id }))
  ]);
  const tagIds = new Set(tags.map((tag) => tag.id));
  const update = (oldKey: string, nextKey: string, nextValue: unknown) => {
    if (nextKey === ADD_CUSTOM_TAG) {
      setInsertForKey(oldKey);
      setEditing(starterTag(tags));
      setTagEditorOpen(true);
      return;
    }
    const next = { ...value };
    delete next[oldKey];
    if (nextKey.trim()) next[nextKey.trim()] = nextValue;
    onChange(next);
    const selectedTag = tags.find((tag) => tag.id === nextKey.toUpperCase());
    if (selectedTag) {
      setInsertForKey(nextKey);
      setEditing(structuredClone(selectedTag));
      setTagEditorOpen(false);
    } else {
      setEditing(null);
      setInsertForKey(null);
      setTagEditorOpen(false);
    }
  };
  const saveTag = (tag: BuildingTagRow) => {
    const normalized = normalizeTag(tag);
    const exists = tags.some((entry) => entry.id === normalized.id);
    onTagsChange(exists ? tags.map((entry) => entry.id === normalized.id ? normalized : entry) : [...tags, normalized]);
    if (insertForKey != null) {
      update(insertForKey, normalized.id, value[insertForKey] ?? 0);
    }
    setEditing(null);
    setInsertForKey(null);
    setTagEditorOpen(false);
  };
  const deleteTag = (id: string) => {
    onTagsChange(tags.filter((tag) => tag.id !== id));
    if (insertForKey != null && insertForKey.toUpperCase() === id) {
      const next = { ...value };
      delete next[insertForKey];
      onChange(next);
    }
    if (editing?.id === id) {
      setEditing(null);
      setInsertForKey(null);
      setTagEditorOpen(false);
    }
  };
  return (
    <Box>
      <Text fw={600} size="sm">{title}</Text>
      <Table mt={4} withTableBorder withColumnBorders>
        <Table.Tbody>
          {rows.map(([key, rowValue]) => {
            const data = optionData.some((option) => option.value === key) ? optionData : [...optionData, { value: key, label: key }];
            return (
              <Table.Tr key={key}>
                <Table.Td><Select value={key} data={data} searchable disabled={disabled} onChange={(next) => update(key, next || key, rowValue)} /></Table.Td>
                <Table.Td><NumberInput placeholder="amount" value={Number(rowValue ?? 0)} disabled={disabled} onChange={(next) => update(key, key, Number(next || 0))} /></Table.Td>
                <Table.Td w={70}><Button size="xs" variant="subtle" color="red" disabled={disabled} onClick={() => { const next = { ...value }; delete next[key]; onChange(next); }}>Del</Button></Table.Td>
              </Table.Tr>
            );
          })}
        </Table.Tbody>
      </Table>
      <Group mt="xs" gap="xs">
        <Button size="xs" variant="light" disabled={disabled} onClick={() => {
          let key = 'stone';
          let index = 1;
          while (value[key]) key = `stone_${index++}`;
          onChange({ ...value, [key]: 0 });
        }}>Add row</Button>
      </Group>
      {editing && <BuildingTagInlineEditor tag={editing} tags={tags} assets={assets} disabled={disabled} expanded={tagEditorOpen} onExpandedChange={setTagEditorOpen} canDelete={tagIds.has(editing.id)} onDelete={() => deleteTag(editing.id)} onCancel={() => {
        if (tagIds.has(editing.id)) {
          setTagEditorOpen(false);
        } else {
          setEditing(null);
          setInsertForKey(null);
          setTagEditorOpen(false);
        }
      }} onSave={saveTag} />}
    </Box>
  );
}

function BuildingTagInlineEditor({ tag, tags, assets, disabled, expanded, onExpandedChange, canDelete, onDelete, onCancel, onSave }: { tag: BuildingTagRow; tags: BuildingTagRow[]; assets: AssetCatalog; disabled: boolean; expanded: boolean; onExpandedChange: (expanded: boolean) => void; canDelete: boolean; onDelete: () => void; onCancel: () => void; onSave: (tag: BuildingTagRow) => void }) {
  const [draft, setDraft] = useState(tag);
  useEffect(() => setDraft(tag), [tag]);
  const minecraftTagOptions = uniqueOptions((assets.factionsMinecraftTags || []).map((option) => ({ value: option.id.toUpperCase(), label: option.label || option.id })));
  const materialOptions = uniqueOptions((assets.materials || []).map((option) => {
    const id = stripMinecraft(String(option.id)).toUpperCase();
    return { value: id, label: id };
  }));
  const referenceOptions = uniqueOptions(tags.filter((entry) => entry.id !== draft.id.toUpperCase()).map((entry) => ({ value: entry.id, label: entry.id })));
  const id = draft.id.toUpperCase().replace(/[^A-Z0-9_]/g, '_');
  return (
    <Paper p="sm" withBorder mt="sm">
      <Stack gap="sm">
        <Group justify="space-between" wrap="nowrap">
          <Box>
            <Group gap="xs"><Text fw={700}>{id || 'NEW_TAG'}</Text><Badge variant="light">tag</Badge></Group>
            <Text size="xs" c="dimmed">{draft.minecraftTags.length} minecraft tags, {draft.materials.length} materials, {draft.references.length} references</Text>
          </Box>
          <Group gap="xs" wrap="nowrap">
            {!expanded && <Button size="xs" variant="subtle" disabled={disabled} onClick={() => onExpandedChange(true)}>Edit</Button>}
            {canDelete && <Button size="xs" variant="subtle" color="red" disabled={disabled} onClick={onDelete}>Delete tag</Button>}
            <Button size="xs" variant="subtle" onClick={onCancel}>Close</Button>
          </Group>
        </Group>
        <Collapse in={expanded}>
          <Stack gap="sm">
            <Group grow align="start">
              <TextInput label="Tag id" value={draft.id} disabled={disabled} error={!id.trim()} onChange={(event) => setDraft({ ...draft, id: event.currentTarget.value.toUpperCase() })} />
              <Box>
                <Text size="xs" c="dimmed" mb={4}>Saved as</Text>
                <Badge variant="light">{id || 'NEW_TAG'}</Badge>
              </Box>
            </Group>
            <MultiSelect label="minecraftTags" value={draft.minecraftTags} data={withCurrentOptions(minecraftTagOptions, draft.minecraftTags)} searchable clearable disabled={disabled} onChange={(minecraftTags) => setDraft({ ...draft, minecraftTags: minecraftTags.map((value) => value.toUpperCase()) })} />
            <MultiSelect label="materials" value={draft.materials} data={withCurrentOptions(materialOptions, draft.materials)} searchable clearable disabled={disabled} onChange={(materials) => setDraft({ ...draft, materials: materials.map((value) => value.toUpperCase()) })} />
            <MultiSelect label="references" value={draft.references} data={withCurrentOptions(referenceOptions, draft.references)} searchable clearable disabled={disabled} onChange={(references) => setDraft({ ...draft, references: references.map((value) => value.toUpperCase()).filter((value) => value !== id) })} />
            <Group justify="flex-end">
              <Button size="xs" variant="subtle" onClick={() => onExpandedChange(false)}>Collapse</Button>
              <Button size="xs" disabled={disabled || !id.trim()} onClick={() => onSave({ ...draft, id })}>Save tag</Button>
            </Group>
          </Stack>
        </Collapse>
      </Stack>
    </Paper>
  );
}

function MapEditor({ title, value, disabled, keyPlaceholder, valuePlaceholder, options, onChange }: { title: string; value: Record<string, any>; disabled: boolean; keyPlaceholder: string; valuePlaceholder: string; options: Array<{ id: string; label?: string }>; onChange: (value: Record<string, unknown>) => void }) {
  const rows = Object.entries(value);
  const optionData = uniqueOptions(options.map((option) => ({ value: option.id, label: option.label || option.id })));
  const update = (oldKey: string, nextKey: string, nextValue: unknown) => {
    const next = { ...value };
    delete next[oldKey];
    if (nextKey.trim()) next[nextKey.trim()] = nextValue;
    onChange(next);
  };
  return (
    <Box>
      <Text fw={600} size="sm">{title}</Text>
      <Table mt={4} withTableBorder withColumnBorders>
        <Table.Tbody>
          {rows.map(([key, rowValue]) => (
            <Table.Tr key={key}>
              <Table.Td>{optionData.length > 0 ? <Select value={key} data={optionData.some((option) => option.value === key) ? optionData : [...optionData, { value: key, label: key }]} searchable disabled={disabled} onChange={(next) => update(key, next || key, rowValue)} /> : <TextInput value={key} disabled={disabled} onChange={(event) => update(key, event.currentTarget.value, rowValue)} />}</Table.Td>
              <Table.Td><NumberInput placeholder={valuePlaceholder} value={Number(rowValue ?? 0)} disabled={disabled} onChange={(next) => update(key, key, Number(next || 0))} /></Table.Td>
              <Table.Td w={70}><Button size="xs" variant="subtle" color="red" disabled={disabled} onClick={() => { const next = { ...value }; delete next[key]; onChange(next); }}>Del</Button></Table.Td>
            </Table.Tr>
          ))}
        </Table.Tbody>
      </Table>
      <Button mt="xs" size="xs" variant="light" disabled={disabled} onClick={() => {
        let key = keyPlaceholder;
        let index = 1;
        while (value[key]) key = `${keyPlaceholder}_${index++}`;
        onChange({ ...value, [key]: 0 });
      }}>Add row</Button>
    </Box>
  );
}

function uniqueOptions(options: Array<{ value: string; label: string }>) {
  const byValue = new Map<string, { value: string; label: string }>();
  for (const option of options) {
    if (!option.value) continue;
    byValue.set(option.value, option);
  }
  return Array.from(byValue.values());
}

function SelectWithCustom({ label, value, options, disabled, placeholder, onChange }: { label: string; value: unknown; options: Array<{ id: string; label?: string }>; disabled: boolean; placeholder: string; onChange: (value: string) => void }) {
  const current = value == null ? '' : String(value);
  const optionData = uniqueOptions(options.map((option) => ({ value: option.id, label: option.label || option.id })));
  const data = current && !optionData.some((option) => option.value === current) ? [...optionData, { value: current, label: current }] : optionData;
  if (data.length === 0) {
    return <TextInput label={label} value={current} placeholder={placeholder} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value)} />;
  }
  return <Select label={label} value={current || null} data={data} searchable clearable disabled={disabled} placeholder={placeholder} nothingFoundMessage="No matches" onChange={(next) => onChange(next || '')} />;
}

function blockRequirementOptions(assets: AssetCatalog, customTags: BuildingTagRow[]) {
  const materials = (assets.materials || []).map((option) => {
    const id = stripMinecraft(String(option.id));
    return { id, label: `${id} (material)` };
  });
  const assetTags = (assets.factionsBuildingTags || []).map((option) => ({ id: option.id.toUpperCase(), label: `${option.label || option.id} (tag)` }));
  const tags = customTags.map((tag) => ({ id: tag.id, label: `${tag.id} (tag)` }));
  return [...tags, ...assetTags, ...materials];
}

function mapKeysOrList(value: unknown) {
  if (Array.isArray(value)) return value.map((item) => String(item).toLowerCase());
  if (isPlainObject(value)) return Object.keys(value).map((item) => item.toLowerCase());
  return [];
}

function buildingIdFromPath(path: string) {
  const file = path.replace(/\\/g, '/').split('/').pop() || '';
  return file.replace(/\.ya?ml$/i, '');
}

function stripMinecraft(value: string) {
  return value.startsWith('minecraft:') ? value.slice('minecraft:'.length) : value;
}

function validateBuilding(data: BuildingData, assets: AssetCatalog, selectedPath: string, customTags: BuildingTagRow[]) {
  const errors: string[] = [];
  if (!Number(data.size)) errors.push('size is required and must be greater than 0.');
  if (data.effects != null && !isPlainObject(data.effects)) errors.push('effects must be a YAML map.');
  const population = asRecord(data.requiredPopulation);
  const populationIds = new Set((assets.factionsPopulationLevels || []).map((option) => option.id));
  for (const [key, value] of Object.entries(population)) {
    if (populationIds.size > 0 && !populationIds.has(key.toLowerCase())) errors.push(`requiredPopulation.${key} is not a known population level.`);
    if (!Number.isFinite(Number(value))) errors.push(`requiredPopulation.${key} must be numeric.`);
  }
  const regionIds = new Set((assets.factionsRegionTypes || []).map((option) => option.id));
  for (const key of mapKeysOrList(data.requiredRegionTypes)) {
    if (regionIds.size > 0 && !regionIds.has(key.toLowerCase())) errors.push(`requiredRegionTypes.${key} is not a known region type.`);
  }
  const buildingIds = new Set((assets.factionsBuildings || []).map((option) => option.id));
  for (const [key, value] of Object.entries(asRecord(data.requiredBuildings))) {
    if (buildingIds.size > 0 && !buildingIds.has(key)) errors.push(`requiredBuildings.${key} is not a known building id.`);
    if (!Number.isFinite(Number(value))) errors.push(`requiredBuildings.${key} must be numeric.`);
  }
  const blockIds = new Set(blockRequirementOptions(assets, customTags).map((option) => option.id));
  validateBlockMap('requiredBlocks', data.requiredBlocks, blockIds, errors);
  const upgrade = asRecord(data.upgrade);
  if (upgrade.targetBuilding && String(upgrade.targetBuilding) === buildingIdFromPath(selectedPath)) errors.push('upgrade.targetBuilding should not equal this building id.');
  if (upgrade.targetBuilding && buildingIds.size > 0 && !buildingIds.has(String(upgrade.targetBuilding))) errors.push(`upgrade.targetBuilding ${upgrade.targetBuilding} is not a known building id.`);
  validateBlockMap('upgrade.requiredBlocks', upgrade.requiredBlocks, blockIds, errors);
  const operations = new Set(modifierOperations.map((operation) => operation.value));
  for (const [key, raw] of Object.entries(asRecord(data.effects))) {
    const effect = asRecord(raw);
    const type = key.replace(/_.*/, '');
    if (type === 'ChangeAttribute') {
      if (!String(effect.attribute || '').trim()) errors.push(`effects.${key}.attribute is required.`);
      if (!Number.isFinite(Number(effect.value ?? 1))) errors.push(`effects.${key}.value must be numeric.`);
      if (effect.operation && !operations.has(String(effect.operation).toUpperCase())) errors.push(`effects.${key}.operation is not a valid AttributeModifier.Operation.`);
    }
    if (type === 'ResourceProduction' && Object.keys(asRecord(effect.production)).length === 0) errors.push(`effects.${key}.production is required.`);
    if (type === 'ResourceConsumption' && Object.keys(asRecord(effect.consumption)).length === 0) errors.push(`effects.${key}.consumption is required.`);
    if (type === 'BlockDependentResourceProduction') {
      validateBlockMap(`effects.${key}.blockModifiers`, effect.blockModifiers, blockIds, errors);
    }
  }
  return errors;
}

function validateBlockMap(path: string, value: unknown, blockIds: Set<string>, errors: string[]) {
  for (const [key, amount] of Object.entries(asRecord(value))) {
    if (blockIds.size > 0 && !blockIds.has(key) && !blockIds.has(key.toUpperCase()) && !blockIds.has(key.toLowerCase())) errors.push(`${path}.${key} is not a known material or Factions building tag.`);
    if (!Number.isFinite(Number(amount))) errors.push(`${path}.${key} must be numeric.`);
  }
}

function parseBuildingTags(yaml: string, assets: AssetCatalog): BuildingTagRow[] {
  const assetRows = (assets.factionsBuildingTags || []).map((tag) => normalizeTag({
    id: tag.id,
    minecraftTags: tag.minecraftTags || [],
    materials: tag.materials || [],
    references: tag.references || []
  }));
  try {
    const parsed = parseDocument(yaml || '{}').toJS();
    const root = asRecord(parsed);
    const tags = asRecord(root.tags);
    const rows = Object.entries(tags).map(([id, raw]) => {
      const section = asRecord(raw);
      return normalizeTag({
        id,
        minecraftTags: stringList(section.minecraftTags).map((value) => value.toUpperCase()),
        materials: stringList(section.materials).map((value) => stripMinecraft(value).toUpperCase()),
        references: stringList(section.references).map((value) => value.toUpperCase())
      });
    });
    if ('tags' in root) return rows;
  } catch {
    return [];
  }
  return assetRows;
}

function serializeBuildingTags(rows: BuildingTagRow[]) {
  const tags = Object.fromEntries(rows.filter((row) => row.id.trim()).map((row) => {
    const tag = normalizeTag(row);
    const value: Record<string, string[]> = {};
    if (tag.minecraftTags.length > 0) value.minecraftTags = tag.minecraftTags;
    if (tag.materials.length > 0) value.materials = tag.materials;
    if (tag.references.length > 0) value.references = tag.references;
    return [tag.id, value];
  }));
  return stringify({ tags }, { lineWidth: 0 });
}

function normalizeTag(tag: BuildingTagRow): BuildingTagRow {
  const id = tag.id.toUpperCase().replace(/[^A-Z0-9_]/g, '_');
  return {
    id,
    minecraftTags: uniqueStrings(tag.minecraftTags.map((value) => value.toUpperCase())),
    materials: uniqueStrings(tag.materials.map((value) => stripMinecraft(value).toUpperCase())),
    references: uniqueStrings(tag.references.map((value) => value.toUpperCase()).filter((value) => value !== id))
  };
}

function starterTag(tags: BuildingTagRow[]) {
  return { id: uniqueTagId(tags, 'NEW_TAG'), minecraftTags: [], materials: [], references: [] };
}

function uniqueTagId(tags: BuildingTagRow[], base: string) {
  const used = new Set(tags.map((tag) => tag.id));
  const normalized = base.toUpperCase().replace(/[^A-Z0-9_]/g, '_') || 'NEW_TAG';
  if (!used.has(normalized)) return normalized;
  for (let index = 2; index < 1000; index++) {
    const next = `${normalized}_${index}`;
    if (!used.has(next)) return next;
  }
  return `${normalized}_${Date.now()}`;
}

function stringList(value: unknown): string[] {
  return Array.isArray(value) ? value.map(String) : [];
}

function uniqueStrings(values: string[]) {
  return Array.from(new Set(values.map((value) => value.trim()).filter(Boolean)));
}

function withCurrentOptions(options: Array<{ value: string; label: string }>, values: string[]) {
  const current = values.filter((value) => !options.some((option) => option.value === value)).map((value) => ({ value, label: value }));
  return [...options, ...current];
}

function parseBuildingYaml(yaml: string): { data: BuildingData; error?: string } {
  try {
    const parsed = parseDocument(yaml || '{}').toJS();
    return { data: isPlainObject(parsed) ? parsed as BuildingData : {} };
  } catch (error) {
    return { data: {}, error: error instanceof Error ? error.message : 'Invalid YAML' };
  }
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

function customFields(data: Record<string, any>, known: string[]) {
  const result: Record<string, any> = {};
  for (const [key, value] of Object.entries(data)) {
    if (!known.includes(key)) result[key] = value;
  }
  return result;
}

function applyCustomFields(_data: Record<string, any>, value: unknown, known: string[], onFieldChange: (field: string, value: unknown) => void) {
  if (!isPlainObject(value)) return;
  for (const [key, fieldValue] of Object.entries(value)) {
    if (!known.includes(key)) onFieldChange(key, fieldValue);
  }
}

function asRecord(value: unknown): Record<string, any> {
  return isPlainObject(value) ? value as Record<string, any> : {};
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}
