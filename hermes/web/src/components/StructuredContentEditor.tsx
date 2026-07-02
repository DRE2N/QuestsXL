import { useMemo, useState } from 'react';
import { Alert, Badge, Box, Button, Group, NumberInput, Paper, ScrollArea, Select, Stack, Table, Tabs, Text, TextInput, Textarea } from '@mantine/core';
import { parseDocument, stringify } from 'yaml';
import type { AssetCatalog, ComponentParam } from '../api/types';
import { RawYamlEditor } from './fields/RawYamlEditor';
import { MiniMessageField, ValueField } from './fields/ValueField';
import { HephaestusItemEditor } from './hephaestus/HephaestusItemEditor';
import { HephaestusJobEditor, HephaestusJobRecipeEditor, HephaestusShopEditor, HephaestusUpgradeEditor, HephaestusVanillaRecipeEditor } from './hephaestus/HephaestusContentEditors';
import { HecateTraitlineLevelsEditor } from './hecate/HecateTraitlineLevelsEditor';

export function StructuredContentWorkspace({ selectedType, selectedPath, yaml, assets, onChange, disabled }: { selectedType: string; selectedPath: string; yaml: string; assets: AssetCatalog; onChange: (yaml: string) => void; disabled: boolean }) {
  const parsed = useMemo(() => parseStructuredYaml(yaml), [yaml]);
  const title = contentTitle(selectedType);
  return (
    <ScrollArea h="calc(100vh - 54px)">
      <Box p="lg">
        <Group justify="space-between" mb="md">
          <Box>
            <Text fw={700} size="lg">{selectedPath || `New ${title}`}</Text>
            <Text c="dimmed" size="sm">Structured {title.toLowerCase()}</Text>
          </Box>
          <Badge variant="outline">{selectedType}</Badge>
        </Group>
        {parsed.error ? (
          <Paper p="md" withBorder><Text c="red" fw={700}>YAML could not be parsed.</Text><Text size="sm">{parsed.error}</Text></Paper>
        ) : (
          <div className="sectionGrid">
            <Paper className="surfaceCard" withBorder>
              <Text fw={700}>{summaryTitle(selectedType, parsed.data, selectedPath)}</Text>
              <Text size="sm" c="dimmed">{summarySubtitle(selectedType, parsed.data)}</Text>
            </Paper>
            <Paper className="surfaceCard" withBorder>
              <Text fw={700}>Available references</Text>
              <Text size="sm" c="dimmed">{assets.items.length} items, {assets.mobs.length} mobs, {assets.spellbookSpells?.length || 0} spells, {assets.spellbookTraits?.length || 0} traits</Text>
            </Paper>
            <Paper className="surfaceCard" withBorder>
              <Text fw={700}>Raw size</Text>
              <Text size="sm" c="dimmed">{yaml.length} characters</Text>
            </Paper>
          </div>
        )}
        <Tabs defaultValue="editor">
          <Tabs.List>
            <Tabs.Tab value="editor">Editor</Tabs.Tab>
            <Tabs.Tab value="yaml">Raw YAML</Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="editor" pt="md">
            <StructuredContentInspector selectedType={selectedType} yaml={yaml} disabled={disabled} assets={assets} onChange={onChange} />
          </Tabs.Panel>
          <Tabs.Panel value="yaml" pt="md">
            <RawYamlEditor value={yaml} disabled={disabled} minRows={28} onChange={onChange} />
          </Tabs.Panel>
        </Tabs>
      </Box>
    </ScrollArea>
  );
}

export function StructuredContentInspector({ selectedType, yaml, disabled, assets, onChange }: { selectedType: string; yaml: string; disabled: boolean; assets: AssetCatalog; onChange: (yaml: string) => void }) {
  const parsed = useMemo(() => parseStructuredYaml(yaml), [yaml]);
  if (parsed.error) return <Text c="red">{parsed.error}</Text>;
  const update = (field: string, value: unknown) => {
    const next = { ...parsed.data };
    if (field === '$patch' && value && typeof value === 'object' && !Array.isArray(value)) {
      for (const [key, nextValue] of Object.entries(value)) {
        if (nextValue === undefined) delete next[key];
        else next[key] = nextValue;
      }
    } else {
      setPathValue(next, field, value);
    }
    onChange(stringify(next));
  };
  if (selectedType === 'aetherMobs') return <AetherMobEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'hephaestusItems') return <HephaestusItemEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'hephaestusUpgrades') return <HephaestusUpgradeEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'hephaestusJobs') return <HephaestusJobEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'hephaestusVanillaRecipes') return <HephaestusVanillaRecipeEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'hephaestusJobRecipes') return <HephaestusJobRecipeEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'hephaestusShops') return <HephaestusShopEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'hecateClasses') return <HecateClassEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'hecateTraitlines') return <HecateTraitlineEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'spellbookSpells') return <SpellbookSpellEditor data={parsed.data} disabled={disabled} assets={assets} onFieldChange={update} />;
  if (selectedType === 'spellbookTraits') return <SpellbookDataEditor title="Spellbook trait" data={parsed.data} disabled={disabled} onFieldChange={update} />;
  if (selectedType === 'spellbookEffects') return <SpellbookDataEditor title="Spellbook effect" data={parsed.data} disabled={disabled} onFieldChange={update} />;
  return <Text c="dimmed">No structured editor for this content type.</Text>;
}

function HecateClassEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: (field: string, value: unknown) => void }) {
  const errors = validateHecateClass(data);
  return (
    <Stack gap="sm">
      <Text fw={700}>Hecate class</Text>
      <ValidationSummary errors={errors} />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Identity</Text>
          <ValueField label="displayName" description="Localized class name shown in selection and UI." error={errors.displayName} value={translatableFrom(data.displayName)} kind="translatable" disabled={disabled} onChange={(value) => onFieldChange('displayName', translatableMap(value))} />
          <LocalizedMiniMessageDescription field="description" description="Localized class description." value={data.description} disabled={disabled} onFieldChange={onFieldChange} />
          <ValueField label="color" description="Hex UI color, for example #18c9f5." value={data.color || '#ffffff'} disabled={disabled} onChange={(value) => onFieldChange('color', value)} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Traitlines</Text>
          <ValueField label="defaultTraitline" description="Starting traitline for new characters in this class." error={errors.defaultTraitline} value={data.defaultTraitline || data.defaultDiscipline} kind="traitline" disabled={disabled} assets={assets} onChange={(value) => onFieldChange(data.defaultDiscipline != null ? 'defaultDiscipline' : 'defaultTraitline', value)} />
          <ValueField label="traitlines" description="Traitlines available to this class." value={data.traitlines || data.disciplines || []} kind="traitlineList" disabled={disabled} assets={assets} onChange={(value) => onFieldChange(data.disciplines != null ? 'disciplines' : 'traitlines', value)} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Restrictions</Text>
          <ValueField label="armorTypes" description="Allowed armor groups. Preserved as a YAML list." value={data.armorTypes || []} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('armorTypes', value)} />
          <ValueField label="weaponTypes" description="Allowed weapon groups. Preserved as a YAML list." value={data.weaponTypes || []} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('weaponTypes', value)} />
          <ValueField label="attributeLevels" description="Optional per-level attribute map." value={data.attributeLevels || {}} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('attributeLevels', value)} />
        </Stack>
      </Paper>
    </Stack>
  );
}

function HecateTraitlineEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: (field: string, value: unknown) => void }) {
  const errors = validateHecateTraitline(data);
  return (
    <Stack gap="sm">
      <Text fw={700}>Hecate traitline</Text>
      <ValidationSummary errors={errors} />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Identity</Text>
          <ValueField label="class" description="Owning Hecate class." error={errors.class} value={data.class} kind="hecateClass" disabled={disabled} assets={assets} onChange={(value) => onFieldChange('class', value)} />
          <ValueField label="displayName" description="Localized traitline name." error={errors.displayName} value={translatableFrom(data.displayName)} kind="translatable" disabled={disabled} onChange={(value) => onFieldChange('displayName', translatableMap(value))} />
          <LocalizedMiniMessageDescription field="description" description="Localized traitline description lines." value={data.description} disabled={disabled} asLines onFieldChange={onFieldChange} />
          <LocalizedMiniMessageDescription field="attackModifierDescription" description="Localized basic-attack modifier description lines." value={data.attackModifierDescription} disabled={disabled} asLines onFieldChange={onFieldChange} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Spells and traits</Text>
          <ValueField label="spells" description="All spells learnable through this traitline." value={data.spells || []} kind="spellList" disabled={disabled} assets={assets} onChange={(value) => onFieldChange('spells', value)} />
          <ValueField label="defaultSpellSlots" description="Default spell bar order." value={data.defaultSpellSlots || []} kind="spellList" disabled={disabled} assets={assets} onChange={(value) => onFieldChange('defaultSpellSlots', value)} />
          <ValueField label="innateTraits" description="Traits granted automatically." value={data.innateTraits || []} kind="traitList" disabled={disabled} assets={assets} onChange={(value) => onFieldChange('innateTraits', value)} />
          <ValueField label="traitLine" description="Trait tree by row/level. Keep complex layouts in raw YAML." value={data.traitLine || {}} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('traitLine', value)} />
          <ValueField label="specialActionKeys" description="Special key bindings, each with a spell id." value={data.specialActionKeys || {}} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('specialActionKeys', value)} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Presentation and equipment</Text>
          <ValueField label="energyColor" description="Hex color used for this traitline resource." value={data.energyColor || '#ffffff'} disabled={disabled} onChange={(value) => onFieldChange('energyColor', value)} />
          <ValueField label="energySymbol" description="Short resource symbol." value={data.energySymbol || ''} disabled={disabled} onChange={(value) => onFieldChange('energySymbol', value)} />
          <ValueField label="armorTags" description="Required or supported armor tags." value={data.armorTags || []} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('armorTags', value)} />
          <ValueField label="weaponTags" description="Required or supported weapon tags." value={data.weaponTags || []} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('weaponTags', value)} />
          <HecateTraitlineLevelsEditor value={data.levels != null ? data.levels : data.characterLevels || {}} assets={assets} disabled={disabled} onChange={(value) => onFieldChange(data.levels != null ? 'levels' : 'characterLevels', value)} />
        </Stack>
      </Paper>
    </Stack>
  );
}

function LocalizedMiniMessageDescription({ field, description, value, disabled, asLines = false, onFieldChange }: { field: string; description: string; value: unknown; disabled: boolean; asLines?: boolean; onFieldChange: (field: string, value: unknown) => void }) {
  const record = asRecord(value);
  const update = (locale: 'en' | 'de', next: string) => {
    const original = record[locale];
    const shouldWriteLines = asLines || Array.isArray(original);
    onFieldChange(`${field}.${locale}`, shouldWriteLines ? next.split('\n').filter(Boolean) : next);
  };
  return (
    <Stack gap="xs">
      <Text size="sm" fw={600}>{field}</Text>
      <Text size="xs" c="dimmed">{description}</Text>
      <MiniMessageField label={`${field}.en`} value={localizedMiniMessageText(record.en)} disabled={disabled} onChange={(next) => update('en', String(next ?? ''))} />
      <MiniMessageField label={`${field}.de`} value={localizedMiniMessageText(record.de)} disabled={disabled} onChange={(next) => update('de', String(next ?? ''))} />
    </Stack>
  );
}

function SpellbookSpellEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: (field: string, value: unknown) => void }) {
  const errors = validateSpellbookData(data);
  return (
    <Stack gap="sm">
      <Text fw={700}>Spellbook spell</Text>
      <ValidationSummary errors={errors} />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Identity</Text>
          <ValueField label="class" description="Java spell class path relative to the Spellbook spell package." error={errors.class} value={data.class} disabled={disabled} onChange={(value) => onFieldChange('class', value)} />
          <ValueField label="name" description="Localized spell name." value={translatableFrom(data.name)} kind="translatable" disabled={disabled} onChange={(value) => onFieldChange('name', translatableMap(value))} />
          <SpellDescriptionEditor data={data} disabled={disabled} onFieldChange={onFieldChange} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Common values</Text>
          <ValueField label="cooldown" description="Cooldown in seconds." value={data.cooldown || 0} kind="duration" disabled={disabled} onChange={(value) => onFieldChange('cooldown', value)} />
          <ValueField label="duration" description="Duration in seconds, if this spell has one." value={data.duration || 0} kind="duration" disabled={disabled} onChange={(value) => onFieldChange('duration', value)} />
          <SpellCoefficientEditor value={data.coefficients || {}} assets={assets} disabled={disabled} onChange={(value) => onFieldChange('coefficients', value)} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Text fw={700}>Spell-specific fields</Text>
        <CustomFieldsTable data={customFields(data, ['class', 'name', 'description', 'cooldown', 'duration', 'coefficients', 'availablePlaceholders'])} disabled={disabled} onFieldChange={onFieldChange} />
      </Paper>
    </Stack>
  );
}

function SpellCoefficientEditor({ value, assets, disabled, onChange }: { value: unknown; assets: AssetCatalog; disabled: boolean; onChange: (value: Record<string, unknown>) => void }) {
  const model = useMemo(() => normalizeCoefficients(value), [value]);
  const [manualAttribute, setManualAttribute] = useState('');
  const [manualMetric, setManualMetric] = useState('');
  const attributeOptions = (assets.attributes || []).map((attribute) => ({ value: attribute.id, label: attribute.label || attribute.id }));
  const rowKey = (row: CoefficientRow) => `${row.metric}\u0000${row.attribute}`;
  const usedKeys = new Set(model.rows.map(rowKey));
  const availableOptions = attributeOptions.filter((option) => !usedKeys.has(`${manualMetric.trim()}\u0000${option.value}`));
  const commitRows = (rows: CoefficientRow[]) => onChange(serializeCoefficients(rows, model.extras));
  const update = (row: CoefficientRow, side: 'players' | 'entities', next: unknown) => {
    const numeric = next === '' ? undefined : Number(next || 0);
    commitRows(model.rows.map((item) => rowKey(item) === rowKey(row) ? { ...item, [side]: numeric } : item));
  };
  const remove = (row: CoefficientRow) => commitRows(model.rows.filter((item) => rowKey(item) !== rowKey(row)));
  const add = () => {
    if (!manualAttribute.trim()) return;
    const row = { metric: manualMetric.trim(), attribute: manualAttribute.trim().toLowerCase(), players: 0, entities: 0 };
    if (!usedKeys.has(rowKey(row))) commitRows([...model.rows, row]);
    setManualAttribute('');
    setManualMetric('');
  };
  return (
    <Box>
      <Text fw={700}>Coefficients</Text>
      <Text size="sm" c="dimmed">Attribute scaling values for players and entities. Attribute keys are stored lowercase.</Text>
      <Table mt="xs" withTableBorder withColumnBorders>
        <Table.Thead>
          <Table.Tr><Table.Th w={180}>Metric</Table.Th><Table.Th>Attribute</Table.Th><Table.Th w={150}>Players</Table.Th><Table.Th w={150}>Entities</Table.Th><Table.Th w={110}>Actions</Table.Th></Table.Tr>
        </Table.Thead>
        <Table.Tbody>
          {model.rows.map((row) => (
            <Table.Tr key={rowKey(row)}>
              <Table.Td><Text ff="monospace" size="sm">{row.metric || 'base'}</Text></Table.Td>
              <Table.Td><Text ff="monospace" size="sm">{row.attribute}</Text></Table.Td>
              <Table.Td><NumberInput value={row.players ?? ''} disabled={disabled} onChange={(next) => update(row, 'players', next)} /></Table.Td>
              <Table.Td><NumberInput value={row.entities ?? ''} disabled={disabled} onChange={(next) => update(row, 'entities', next)} /></Table.Td>
              <Table.Td><Button size="xs" variant="subtle" color="red" disabled={disabled} onClick={() => remove(row)}>Remove</Button></Table.Td>
            </Table.Tr>
          ))}
          {model.rows.length === 0 && (
            <Table.Tr><Table.Td colSpan={5}><Text size="sm" c="dimmed">No coefficients configured.</Text></Table.Td></Table.Tr>
          )}
        </Table.Tbody>
      </Table>
      <Group mt="sm" align="end">
        <TextInput label="Metric" placeholder="base / weaknessDuration" disabled={disabled} value={manualMetric} onChange={(event) => setManualMetric(event.currentTarget.value)} />
        {availableOptions.length > 0 ? (
          <Select label="Attribute" searchable clearable data={availableOptions} disabled={disabled} value={manualAttribute || null} onChange={(next) => setManualAttribute(next || '')} />
        ) : (
          <TextInput label="Attribute" placeholder="generic.attack_damage" disabled={disabled} value={manualAttribute} onChange={(event) => setManualAttribute(event.currentTarget.value)} />
        )}
        <Button variant="light" disabled={disabled || !manualAttribute.trim()} onClick={add}>Add coefficient</Button>
      </Group>
      {Object.keys(model.extras).length > 0 && (
        <Box mt="sm">
          <ValueField label="Unsupported coefficient data" value={model.extras} kind="raw" disabled={disabled} onChange={(next) => onChange(mergeCoefficientExtras(serializeCoefficients(model.rows, {}), next))} />
        </Box>
      )}
    </Box>
  );
}

type CoefficientRow = {
  metric: string;
  attribute: string;
  players?: number;
  entities?: number;
};

function normalizeCoefficients(value: unknown): { rows: CoefficientRow[]; extras: Record<string, unknown> } {
  const source = asRecord(value);
  const rows = new Map<string, CoefficientRow>();
  const extras: Record<string, unknown> = {};
  const sideExtras: Record<string, unknown> = {};
  for (const [key, raw] of Object.entries(source)) {
    if (key !== 'players' && key !== 'entities') {
      extras[key] = raw;
      continue;
    }
    collectCoefficientSide(key as 'players' | 'entities', raw, rows, sideExtras);
  }
  if (Object.keys(sideExtras).length > 0) {
    Object.assign(extras, sideExtras);
  }
  return { rows: Array.from(rows.values()).sort((left, right) => (left.metric || '').localeCompare(right.metric || '') || left.attribute.localeCompare(right.attribute)), extras };
}

function collectCoefficientSide(side: 'players' | 'entities', raw: unknown, rows: Map<string, CoefficientRow>, extras: Record<string, unknown>) {
  const sideMap = asRecord(raw);
  if (Object.keys(sideMap).length === 0 && raw != null && !isPlainObject(raw)) {
    extras[side] = raw;
    return;
  }
  for (const [key, value] of Object.entries(sideMap)) {
    if (isNumericLike(value)) {
      setCoefficientRow(rows, '', key.toLowerCase(), side, Number(value));
      continue;
    }
    if (isPlainObject(value)) {
      for (const [attribute, nestedValue] of Object.entries(value)) {
        if (isNumericLike(nestedValue)) setCoefficientRow(rows, key, attribute.toLowerCase(), side, Number(nestedValue));
        else setNestedExtra(extras, [side, key, attribute], nestedValue);
      }
      continue;
    }
    setNestedExtra(extras, [side, key], value);
  }
}

function setCoefficientRow(rows: Map<string, CoefficientRow>, metric: string, attribute: string, side: 'players' | 'entities', value: number) {
  const id = `${metric}\u0000${attribute}`;
  const row = rows.get(id) || { metric, attribute };
  row[side] = value;
  rows.set(id, row);
}

function serializeCoefficients(rows: CoefficientRow[], extras: Record<string, unknown>) {
  const result = JSON.parse(JSON.stringify(extras || {}));
  for (const row of rows) {
    if (row.players != null && !Number.isNaN(row.players)) setCoefficientValue(result, 'players', row.metric, row.attribute, row.players);
    if (row.entities != null && !Number.isNaN(row.entities)) setCoefficientValue(result, 'entities', row.metric, row.attribute, row.entities);
  }
  return result;
}

function setCoefficientValue(target: Record<string, any>, side: 'players' | 'entities', metric: string, attribute: string, value: number) {
  if (!isPlainObject(target[side])) target[side] = {};
  if (metric) {
    if (!isPlainObject(target[side][metric])) target[side][metric] = {};
    target[side][metric][attribute] = value;
  } else {
    target[side][attribute] = value;
  }
}

function mergeCoefficientExtras(base: Record<string, unknown>, extras: unknown) {
  return { ...asRecord(extras), ...base };
}

function setNestedExtra(target: Record<string, unknown>, path: string[], value: unknown) {
  let current: Record<string, any> = target;
  for (const part of path.slice(0, -1)) {
    if (!isPlainObject(current[part])) current[part] = {};
    current = current[part] as Record<string, any>;
  }
  current[path[path.length - 1]] = value;
}

function isNumericLike(value: unknown) {
  return typeof value === 'number' || (typeof value === 'string' && value.trim() !== '' && !Number.isNaN(Number(value)));
}

function CustomFieldsTable({ data, disabled, onFieldChange }: { data: Record<string, any>; disabled: boolean; onFieldChange: (field: string, value: unknown) => void }) {
  const entries = Object.entries(data);
  if (entries.length === 0) return <Text size="sm" c="dimmed" mt="xs">No spell-specific fields outside the structured sections.</Text>;
  return (
    <Table mt="xs" withTableBorder withColumnBorders>
      <Table.Thead>
        <Table.Tr><Table.Th w={240}>Field</Table.Th><Table.Th>Value</Table.Th></Table.Tr>
      </Table.Thead>
      <Table.Tbody>
        {entries.map(([key, value]) => (
          <Table.Tr key={key}>
            <Table.Td><Text ff="monospace" size="sm">{key}</Text></Table.Td>
            <Table.Td>
              <ValueField label={key} value={value} kind={typeof value === 'boolean' ? 'boolean' : typeof value === 'number' ? 'number' : isPlainObject(value) || Array.isArray(value) ? 'raw' : 'string'} disabled={disabled} onChange={(next) => onFieldChange(key, next)} />
            </Table.Td>
          </Table.Tr>
        ))}
      </Table.Tbody>
    </Table>
  );
}

type SpellPlaceholder = {
  index: string;
  name: string;
};

function SpellDescriptionEditor({ data, disabled, onFieldChange }: { data: Record<string, any>; disabled: boolean; onFieldChange: (field: string, value: unknown) => void }) {
  const placeholders = useMemo(() => spellPlaceholders(data.availablePlaceholders), [data.availablePlaceholders]);
  const description = asRecord(data.description);
  const english = spellDescriptionText(description.en, placeholders);
  const german = spellDescriptionText(description.de, placeholders);
  const updateDescription = (locale: 'en' | 'de', value: string) => {
    onFieldChange(`description.${locale}`, serializeSpellDescription(value, placeholders));
  };
  return (
    <Stack gap="sm">
      <SpellDescriptionLocaleEditor locale="en" label="description.en" value={english} disabled={disabled} placeholders={placeholders} onChange={(value) => updateDescription('en', value)} />
      <SpellDescriptionLocaleEditor locale="de" label="description.de" value={german} disabled={disabled} placeholders={placeholders} onChange={(value) => updateDescription('de', value)} />
    </Stack>
  );
}

function SpellDescriptionLocaleEditor({ locale, label, value, disabled, placeholders, onChange }: { locale: 'en' | 'de'; label: string; value: string; disabled: boolean; placeholders: SpellPlaceholder[]; onChange: (value: string) => void }) {
  return (
    <Box>
      <MiniMessageField
        label={label}
        description={locale === 'en' ? 'English MiniMessage. Named placeholders are saved as Spellbook <arg:n> placeholders.' : 'German MiniMessage. Use the same named placeholders for translated lines.'}
        value={value}
        disabled={disabled}
        placeholders={placeholders.map((placeholder) => `{${placeholder.name}}`)}
        onChange={(next) => onChange(String(next ?? ''))}
      />
    </Box>
  );
}

function SpellbookDataEditor({ title, data, disabled, onFieldChange }: { title: string; data: Record<string, any>; disabled: boolean; onFieldChange: (field: string, value: unknown) => void }) {
  const errors = validateSpellbookData(data);
  return (
    <Stack gap="sm">
      <Text fw={700}>{title}</Text>
      <ValidationSummary errors={errors} />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Identity</Text>
          <ValueField label="class" description="Java implementation class path." error={errors.class} value={data.class} disabled={disabled} onChange={(value) => onFieldChange('class', value)} />
          <ValueField label="name" description="Localized display name." value={translatableFrom(data.name)} kind="translatable" disabled={disabled} onChange={(value) => onFieldChange('name', translatableMap(value))} />
          <Textarea label="description.en" value={stringList(asRecord(data.description).en).join('\n')} disabled={disabled} autosize minRows={3} onChange={(event) => onFieldChange('description.en', event.currentTarget.value.split('\n').filter(Boolean))} />
          <Textarea label="description.de" value={stringList(asRecord(data.description).de).join('\n')} disabled={disabled} autosize minRows={3} onChange={(event) => onFieldChange('description.de', event.currentTarget.value.split('\n').filter(Boolean))} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Text fw={700}>Custom fields</Text>
        <ValueField label="custom parameters" value={customFields(data, ['class', 'name', 'description'])} kind="raw" disabled={disabled} onChange={(value) => applyCustomFields(data, value, ['class', 'name', 'description'], onFieldChange)} />
      </Paper>
    </Stack>
  );
}

function DialogueEditor({ data, disabled, assets, onChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onChange: (data: Record<string, any>) => void }) {
  const stages = asRecord(data.stages);
  const errors = validateDialogue(data);
  const update = (patch: Record<string, unknown>) => onChange({ ...data, ...patch });
  const updateStage = (stageId: string, patch: Record<string, unknown>) => onChange({ ...data, stages: { ...stages, [stageId]: { ...asRecord(stages[stageId]), ...patch } } });
  const addStage = () => {
    const nextId = nextNumericKey(Object.keys(stages));
    updateStage(nextId, { id: `stage_${nextId}`, messages: ['en=New message;de=Neue Nachricht'], autoNext: false, options: [] });
  };
  return (
    <Stack gap="sm">
      <Text fw={700}>Dialogue</Text>
      <ValidationSummary errors={errors} />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Root</Text>
          <ValueField label="sender" description="Speaker name. Use en=...; de=... or a sender translation section in raw YAML." value={translatableFrom(data.sender)} kind="translatable" disabled={disabled} onChange={(value) => update({ sender: value })} />
          <ValueField label="npcId" description="Optional Aether NPC id that starts this dialogue on click." value={data.npcId} kind="mob" disabled={disabled} assets={assets} onChange={(value) => update({ npcId: value })} />
          <ValueField label="canStartFromNPC" description="If false, this dialogue can only be started by actions/scripts." value={data.canStartFromNPC ?? true} kind="boolean" disabled={disabled} onChange={(value) => update({ canStartFromNPC: value })} />
        </Stack>
      </Paper>
      <Group justify="space-between">
        <Text fw={700}>Stages</Text>
        <Button size="xs" variant="light" disabled={disabled} onClick={addStage}>Add stage</Button>
      </Group>
      {Object.entries(stages).sort(([left], [right]) => Number(left) - Number(right)).map(([stageId, raw]) => {
        const stage = asRecord(raw);
        return (
          <Paper key={stageId} p="md" withBorder className="slotBox">
            <Stack gap="sm">
              <Group align="end">
                <NumberInput label="Index" description="Numeric stage key referenced by next=..." value={Number(stageId)} disabled />
                <TextInput label="id" description="Human-readable stage id used for debugging." value={String(stage.id || '')} disabled={disabled} onChange={(event) => updateStage(stageId, { id: event.currentTarget.value })} />
              </Group>
              <MiniMessageLinesField label="messages" description="One message per line. Optional delay: append |20 for 20 ticks." value={stage.messages} disabled={disabled} onChange={(value) => updateStage(stageId, { messages: value })} />
              <ValueField label="autoNext" description="Automatically continue to the next stage after messages finish." value={stage.autoNext ?? true} kind="boolean" disabled={disabled} onChange={(value) => updateStage(stageId, { autoNext: value })} />
              <Textarea label="options" description="One option per line. Format: text=en=...;de=...|hint=en=...;de=...|next=1|default=true|actions=message: message=..." value={stringList(stage.options).join('\n')} disabled={disabled} autosize minRows={3} onChange={(event) => updateStage(stageId, { options: event.currentTarget.value.split('\n').filter(Boolean) })} />
              <ValueField label="conditions" description="Optional QXL conditions required before this stage can start." value={stage.conditions || {}} kind="raw" disabled={disabled} onChange={(value) => updateStage(stageId, { conditions: value })} />
              <ValueField label="actions" description="Optional QXL actions run when this stage starts." value={stage.actions || {}} kind="raw" disabled={disabled} onChange={(value) => updateStage(stageId, { actions: value })} />
            </Stack>
          </Paper>
        );
      })}
    </Stack>
  );
}

function MiniMessageLinesField({ label, value, description, disabled, onChange }: { label: string; value: unknown; description?: string; disabled: boolean; onChange: (value: string[]) => void }) {
  return (
    <MiniMessageField
      label={label}
      description={description}
      value={stringList(value).join('\n')}
      disabled={disabled}
      onChange={(next) => onChange(String(next ?? '').split('\n').filter(Boolean))}
    />
  );
}

function AetherMobEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: (field: string, value: unknown) => void }) {
  const errors = validateAetherMob(data);
  return (
    <Stack gap="sm">
      <Text fw={700}>Aether mob</Text>
      <ValidationSummary errors={errors} />
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Identity</Text>
          <ValueField label="class" description="Aether mob implementation class, for example de.erethon.aether.creature.AetherBaseMob." error={errors.class} value={data.class || 'de.erethon.aether.creature.AetherBaseMob'} disabled={disabled} onChange={(value) => onFieldChange('class', value)} />
          <ValueField label="name.en" description="English player-facing name." error={errors['name.en']} value={data.name?.en} kind="minimessage" disabled={disabled} onChange={(value) => onFieldChange('name.en', value)} />
          <ValueField label="name.de" description="German player-facing name." error={errors['name.de']} value={data.name?.de} kind="minimessage" disabled={disabled} onChange={(value) => onFieldChange('name.de', value)} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Entity</Text>
          <ValueField label="displayType" description="Entity type shown to players. Example: player, pig, zombie. Aether maps player to mannequin internally." error={errors.displayType} value={data.displayType} disabled={disabled} assets={assets} onChange={(value) => onFieldChange('displayType', stripMinecraft(value))} />
          <ValueField label="mobCategory" description="Vanilla mob category override used for AI/spawn behavior." value={data.mobCategory || 'MONSTER'} kind="enum" param={mobCategoryParam} disabled={disabled} onChange={(value) => onFieldChange('mobCategory', value)} />
          <ValueField label="instancable" description="Creates one personal mob instance per player when used in instanced content." value={data.instancable} kind="boolean" disabled={disabled} onChange={(value) => onFieldChange('instancable', value)} />
          <ValueField label="skin" description="Optional Mineskin id or skin URL for player-like display types." value={data.skin} disabled={disabled} onChange={(value) => onFieldChange('skin', value)} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Equipment</Text>
          {['hand', 'offhand', 'helmet', 'chest', 'leggings', 'boots'].map((slot) => (
            <ValueField key={slot} label={`equipment.${slot}`} description="Optional Hephaestus item key or minecraft:air." value={data.equipment?.[slot]} kind="item" disabled={disabled} assets={assets} onChange={(value) => onFieldChange(`equipment.${slot}`, value || 'minecraft:air')} />
          ))}
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Advanced</Text>
          <ValueField label="attributes" description="YAML map of Bukkit/NMS attribute ids to numeric values. Common keys: health, speed, follow_range, attack_damage." error={errors.attributes} value={data.attributes || { health: 30, speed: 0.2 }} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('attributes', value)} />
          <ValueField label="skins" description="Optional YAML list of multiple Mineskin ids/URLs; Aether picks one randomly." value={data.skins || []} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('skins', value)} />
        </Stack>
      </Paper>
    </Stack>
  );
}

const mobCategoryParam: ComponentParam = { name: 'mobCategory', required: false, defaultValue: 'MONSTER', description: '', javaType: 'String', editorKind: 'enum', options: ['MONSTER', 'CREATURE', 'AMBIENT', 'AXOLOTLS', 'UNDERGROUND_WATER_CREATURE', 'WATER_CREATURE', 'WATER_AMBIENT', 'MISC'] };

function validateAetherMob(data: Record<string, any>) {
  const errors: Record<string, string> = {};
  if (!String(data.class || '').trim()) errors.class = 'Required. Use an Aether mob implementation class such as de.erethon.aether.creature.AetherBaseMob.';
  if (!String(asRecord(data.name).en || '').trim()) errors['name.en'] = 'Recommended. English names are used as the fallback.';
  if (!String(asRecord(data.name).de || '').trim()) errors['name.de'] = 'Recommended for German player-facing content.';
  if (!String(data.displayType || '').trim()) errors.displayType = 'Required. Use a vanilla entity id such as player, pig, or zombie.';
  if (data.attributes != null && !isPlainObject(data.attributes)) errors.attributes = 'Attributes must be a YAML map.';
  if (isPlainObject(data.attributes)) {
    for (const [key, value] of Object.entries(data.attributes)) {
      if (typeof value !== 'number') errors.attributes = `${key} must be numeric.`;
      if (typeof value === 'number' && value < 0) errors.attributes = `${key} should not be negative.`;
    }
  }
  return errors;
}

function ValidationSummary({ errors }: { errors: Record<string, string> }) {
  const values = Object.entries(errors);
  if (values.length === 0) return <Alert color="green" variant="light">No issues detected</Alert>;
  return (
    <Alert color="red" variant="light" title={`${values.length} issue${values.length === 1 ? '' : 's'}`}>
      <Stack gap={2}>{values.map(([field, message]) => <Text key={field} size="sm"><b>{field}</b>: {message}</Text>)}</Stack>
    </Alert>
  );
}

function validateDialogue(data: Record<string, any>) {
  const errors: Record<string, string> = {};
  const stages = asRecord(data.stages);
  if (Object.keys(stages).length === 0) errors.stages = 'At least one stage is required.';
  if (!stages['0']) errors.stages = 'Stage 0 is required as the dialogue entry point.';
  for (const [id, raw] of Object.entries(stages)) {
    const stage = asRecord(raw);
    if (!stage.id) errors[`stages.${id}.id`] = 'Stage id is required.';
    if (stringList(stage.messages).length === 0) errors[`stages.${id}.messages`] = 'At least one message is required.';
  }
  return errors;
}

function validateHecateClass(data: Record<string, any>) {
  const errors: Record<string, string> = {};
  if (!data.displayName) errors.displayName = 'Required. The class needs a player-facing name.';
  if (!String(data.defaultTraitline || data.defaultDiscipline || '').trim()) errors.defaultTraitline = 'Recommended. New characters need a starting traitline.';
  return errors;
}

function validateHecateTraitline(data: Record<string, any>) {
  const errors: Record<string, string> = {};
  if (!String(data.class || '').trim()) errors.class = 'Required. Traitlines must belong to a Hecate class.';
  if (!data.displayName) errors.displayName = 'Required. The traitline needs a player-facing name.';
  return errors;
}

function validateSpellbookData(data: Record<string, any>) {
  const errors: Record<string, string> = {};
  if (!String(data.class || '').trim()) errors.class = 'Required. Spellbook uses this Java class to instantiate the config.';
  return errors;
}

function contentTitle(selectedType: string) {
  if (selectedType === 'aetherMobs') return 'Aether mob';
  if (selectedType === 'hephaestusItems') return 'Hephaestus item';
  if (selectedType === 'hephaestusUpgrades') return 'Hephaestus upgrade';
  if (selectedType === 'hephaestusJobs') return 'Hephaestus job';
  if (selectedType === 'hephaestusVanillaRecipes') return 'Vanilla recipe';
  if (selectedType === 'hephaestusJobRecipes') return 'Job recipe';
  if (selectedType === 'hephaestusShops') return 'Hephaestus shop';
  if (selectedType === 'dialogues') return 'Dialogue';
  if (selectedType === 'hecateClasses') return 'Hecate class';
  if (selectedType === 'hecateTraitlines') return 'Hecate traitline';
  if (selectedType === 'spellbookSpells') return 'Spellbook spell';
  if (selectedType === 'spellbookTraits') return 'Spellbook trait';
  if (selectedType === 'spellbookEffects') return 'Spellbook effect';
  return 'content';
}

function summaryTitle(selectedType: string, data: Record<string, any>, selectedPath: string) {
  if (selectedType === 'aetherMobs') return String(data.name?.en || data.name?.de || data.class || selectedPath);
  if (selectedType === 'hephaestusItems') return String(data.key || selectedPath);
  if (selectedType === 'hephaestusShops') return String(data.name || selectedPath);
  if (selectedType.startsWith('hephaestus')) return String(data.displayName || data.id || selectedPath);
  if (selectedType === 'hecateClasses' || selectedType === 'hecateTraitlines') return String(asRecord(data.displayName).en || data.displayName || selectedPath);
  if (selectedType.startsWith('spellbook')) return String(asRecord(data.name).en || data.name || selectedPath);
  return String(data.sender?.en || data.sender || selectedPath);
}

function summarySubtitle(selectedType: string, data: Record<string, any>) {
  if (selectedType === 'aetherMobs') return `Display type ${String(data.displayType || 'pig')}`;
  if (selectedType === 'hephaestusItems') return `Base item ${String(data.baseItem || 'minecraft:stone')}`;
  if (selectedType === 'hephaestusUpgrades') return `Type ${String(data.type || 'attribute_modifying')}`;
  if (selectedType === 'hephaestusJobs') return `Max level ${String(data.maxLevel || 100)}`;
  if (selectedType === 'hephaestusVanillaRecipes') return `${String(data.type || 'shaped')} crafting recipe`;
  if (selectedType === 'hephaestusJobRecipes') return `${String(data.jobId || 'No job')} · level ${String(data.requiredLevel || 0)}`;
  if (selectedType === 'hephaestusShops') return `${Object.keys(asRecord(data.items)).length} shop items`;
  if (selectedType === 'hecateClasses') return `${stringList(data.traitlines || data.disciplines).length} traitlines`;
  if (selectedType === 'hecateTraitlines') return `${stringList(data.spells).length} spells, ${stringList(data.innateTraits).length} innate traits`;
  if (selectedType.startsWith('spellbook')) return String(data.class || 'No class configured');
  return `${Object.keys(asRecord(data.stages)).length} stages`;
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

function stringList(value: unknown) {
  return Array.isArray(value) ? value.map(String) : [];
}

function localizedMiniMessageText(value: unknown) {
  if (Array.isArray(value)) return value.map(String).join('\n');
  return value == null ? '' : String(value);
}

function spellPlaceholders(value: unknown): SpellPlaceholder[] {
  return placeholderEntries(value)
    .filter((placeholder) => placeholder.index.trim() && placeholder.name.trim())
    .sort((left, right) => Number(left.index) - Number(right.index));
}

function placeholderEntries(value: unknown): SpellPlaceholder[] {
  if (Array.isArray(value)) {
    return value.flatMap((item, index) => placeholderNamesFrom(item).map((name) => ({ index: String(index), name })));
  }
  if (isPlainObject(value)) {
    return Object.entries(value).flatMap(([index, name]) => placeholderNamesFrom(name).map((entryName) => ({ index: String(index), name: entryName })));
  }
  return placeholderNamesFrom(value).map((name, index) => ({ index: String(index), name }));
}

function placeholderNamesFrom(value: unknown): string[] {
  return String(value ?? '')
    .split(/[\r\n]+/)
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => part.replace(/^\{/, '').replace(/\}$/, ''));
}

function spellDescriptionText(value: unknown, placeholders: SpellPlaceholder[]) {
  const lines = Array.isArray(value) ? value.map(String) : typeof value === 'string' ? [value] : [];
  return lines.map((line) => placeholderArgsToNames(line, placeholders)).join('\n');
}

function serializeSpellDescription(value: string, placeholders: SpellPlaceholder[]) {
  return value.split('\n').map((line) => {
    const serialized = placeholderNamesToArgs(line, placeholders);
    return serialized.length === 0 ? ' ' : serialized;
  });
}

function placeholderArgsToNames(value: string, placeholders: SpellPlaceholder[]) {
  const byIndex = new Map(placeholders.map((placeholder) => [placeholder.index, placeholder.name]));
  return value.replace(/<arg:(\d+)>/g, (_, index) => {
    const name = byIndex.get(String(index));
    return name ? `{${name}}` : `<arg:${index}>`;
  });
}

function placeholderNamesToArgs(value: string, placeholders: SpellPlaceholder[]) {
  return placeholders.reduce((result, placeholder) => {
    return result.replace(new RegExp(`\\{${escapeRegExp(placeholder.name)}\\}`, 'g'), `<arg:${placeholder.index}>`);
  }, value);
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function customFields(data: Record<string, any>, known: string[]) {
  const result: Record<string, any> = {};
  for (const [key, value] of Object.entries(data)) {
    if (!known.includes(key)) result[key] = value;
  }
  return result;
}

function applyCustomFields(data: Record<string, any>, value: unknown, known: string[], onFieldChange: (field: string, value: unknown) => void) {
  if (!isPlainObject(value)) return;
  for (const [key, fieldValue] of Object.entries(value)) {
    if (known.includes(key)) continue;
    onFieldChange(key, fieldValue);
  }
}

function nextNumericKey(existing: string[]) {
  let index = 0;
  while (existing.includes(String(index))) index++;
  return String(index);
}

function parseStructuredYaml(yaml: string): { data: Record<string, any>; error?: string } {
  try {
    const parsed = parseDocument(yaml || '{}').toJS();
    return { data: isPlainObject(parsed) ? parsed as Record<string, any> : {} };
  } catch (error) {
    return { data: {}, error: error instanceof Error ? error.message : 'Invalid YAML' };
  }
}

function asRecord(value: unknown): Record<string, any> {
  return isPlainObject(value) ? value as Record<string, any> : {};
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

function stripMinecraft(value: unknown) {
  const text = String(value ?? '');
  return text.startsWith('minecraft:') ? text.slice('minecraft:'.length) : text;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}
