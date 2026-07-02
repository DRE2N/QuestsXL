import { ActionIcon, Badge, Button, Checkbox, Group, NumberInput, Paper, Select, Stack, Table, Text, TextInput } from '@mantine/core';
import { IconArrowDown, IconArrowUp, IconPlus, IconTrash } from '@tabler/icons-react';
import type { AssetCatalog } from '../../api/types';
import { ValueField } from '../fields/ValueField';

type FieldChange = (field: string, value: unknown) => void;

export function HephaestusUpgradeEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: FieldChange }) {
  const rows = flattenAttributeRows(data.attributes || {});
  return (
    <Stack gap="sm">
      <Text fw={700}>Hephaestus upgrade</Text>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Identity</Text>
          <TextInput label="id" value={String(data.id || '')} disabled={disabled} onChange={(event) => onFieldChange('id', event.currentTarget.value)} />
          <ValueField label="type" value={data.type || 'attribute_modifying'} kind="enum" disabled={disabled} param={{ name: 'type', description: 'Upgrade loader type.', defaultValue: '', required: true, javaType: 'String', editorKind: 'enum', options: (assets.hephaestusUpgradeTypes || []).map((item) => item.id) }} onChange={(value) => onFieldChange('type', value)} />
          <ValueField label="name" value={translatableFromLocaleMap(data.name)} kind="translatable" disabled={disabled} onChange={(value) => onFieldChange('name', localeMapFromTranslatable(String(value || '')))} />
          <NumberInput label="minimumLevel" value={Number(data.minimumLevel || 0)} disabled={disabled} onChange={(value) => onFieldChange('minimumLevel', Number(value || 0))} />
          <ValueField label="validItems" value={data.validItems || []} kind="itemList" disabled={disabled} assets={assets} onChange={(value) => onFieldChange('validItems', value)} />
          <ValueField label="requiredUpgrades" value={data.requiredUpgrades || []} kind="itemList" disabled={disabled} assets={{ ...assets, items: assets.hephaestusUpgrades || [] }} onChange={(value) => onFieldChange('requiredUpgrades', value)} />
          <ValueField label="incompatibleUpgrades" value={data.incompatibleUpgrades || []} kind="itemList" disabled={disabled} assets={{ ...assets, items: assets.hephaestusUpgrades || [] }} onChange={(value) => onFieldChange('incompatibleUpgrades', value)} />
        </Stack>
      </Paper>
      {String(data.type || 'attribute_modifying') === 'attribute_modifying' && (
        <Paper p="md" withBorder className="slotBox">
          <Group justify="space-between" mb="sm">
            <Text fw={700}>Attribute rolls</Text>
            <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => onFieldChange('attributes', attributesFromRows([...rows, { attribute: 'minecraft:max_health', level: 0, min: 1, max: 1, weight: 100 }]))}>Roll</Button>
          </Group>
          <Table.ScrollContainer minWidth={760}>
            <Table>
              <Table.Thead><Table.Tr><Table.Th>Attribute</Table.Th><Table.Th>Level floor</Table.Th><Table.Th>Min</Table.Th><Table.Th>Max</Table.Th><Table.Th>Weight</Table.Th><Table.Th /></Table.Tr></Table.Thead>
              <Table.Tbody>
                {rows.map((row, index) => (
                  <Table.Tr key={`${row.attribute}-${row.level}-${index}`}>
                    <Table.Td><ValueField label="attribute" value={row.attribute} kind="enum" disabled={disabled} param={{ name: 'attribute', description: '', defaultValue: '', required: true, javaType: 'String', editorKind: 'enum', options: (assets.attributes || []).map((item) => item.id) }} onChange={(value) => updateAttributeRow(rows, index, { attribute: String(value || '') }, onFieldChange)} /></Table.Td>
                    <Table.Td><NumberInput value={row.level} disabled={disabled} onChange={(value) => updateAttributeRow(rows, index, { level: Number(value || 0) }, onFieldChange)} /></Table.Td>
                    <Table.Td><NumberInput value={row.min} disabled={disabled} onChange={(value) => updateAttributeRow(rows, index, { min: Number(value || 0) }, onFieldChange)} /></Table.Td>
                    <Table.Td><NumberInput value={row.max} disabled={disabled} onChange={(value) => updateAttributeRow(rows, index, { max: Number(value || 0) }, onFieldChange)} /></Table.Td>
                    <Table.Td><NumberInput value={row.weight} disabled={disabled} onChange={(value) => updateAttributeRow(rows, index, { weight: Number(value || 0) }, onFieldChange)} /></Table.Td>
                    <Table.Td><ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onFieldChange('attributes', attributesFromRows(rows.filter((_, rowIndex) => rowIndex !== index)))}><IconTrash size={16} /></ActionIcon></Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </Paper>
      )}
    </Stack>
  );
}

export function HephaestusJobEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: FieldChange }) {
  const translations = data.translations || {};
  return (
    <Stack gap="sm">
      <Text fw={700}>Hephaestus job</Text>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <TextInput label="id" value={String(data.id || '')} disabled={disabled} onChange={(event) => onFieldChange('id', event.currentTarget.value)} />
          <ValueField label="name" value={translatableFromFlatTranslations(translations, 'name')} kind="translatable" disabled={disabled} onChange={(value) => onFieldChange('translations', { ...translations, ...flatTranslationsFromTranslatable('name', String(value || '')) })} />
          <ValueField label="description" value={translatableFromFlatTranslations(translations, 'description') || data.description || ''} kind="translatable" disabled={disabled} onChange={(value) => onFieldChange('translations', { ...translations, ...flatTranslationsFromTranslatable('description', String(value || '')) })} />
          <NumberInput label="maxLevel" value={Number(data.maxLevel || 100)} disabled={disabled} onChange={(value) => onFieldChange('maxLevel', Number(value || 0))} />
          <ValueField label="block" value={data.block || 'minecraft:bedrock'} kind="material" disabled={disabled} assets={assets} onChange={(value) => onFieldChange('block', value)} />
          <ValueField label="properties" value={data.properties || {}} kind="raw" disabled={disabled} onChange={(value) => onFieldChange('properties', value)} />
        </Stack>
      </Paper>
    </Stack>
  );
}

export function HephaestusVanillaRecipeEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: FieldChange }) {
  const type = String(data.type || 'shaped').toLowerCase();
  return (
    <Stack gap="sm">
      <Text fw={700}>Vanilla crafting recipe</Text>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Select label="type" value={type} disabled={disabled} data={['shaped', 'shapeless']} onChange={(value) => onFieldChange('type', value || 'shaped')} />
          <RecipeResultEditor result={data.result || { item: 'minecraft:stone', amount: 1 }} disabled={disabled} assets={assets} onChange={(result) => onFieldChange('result', result)} vanilla />
        </Stack>
      </Paper>
      {type === 'shapeless'
        ? <ShapelessVanillaIngredients ingredients={listValue(data.ingredients)} disabled={disabled} assets={assets} onChange={(ingredients) => onFieldChange('ingredients', ingredients)} />
        : <ShapedVanillaGrid pattern={patternRows(data.pattern)} ingredients={asRecord(data.ingredients)} disabled={disabled} assets={assets} onChange={(pattern, ingredients) => onFieldChange('$patch', { pattern, ingredients })} />}
    </Stack>
  );
}

export function HephaestusJobRecipeEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: FieldChange }) {
  const ingredients = objectList(data.ingredients);
  const hasResultModifier = Object.keys(asRecord(data.resultModifier)).length > 0;
  const dynamic = hasResultModifier || String(data.resultType || 'fixed').toLowerCase() === 'dynamic';
  const setResultMode = (mode: string) => {
    if (mode === 'dynamic') {
      const modifier = hasResultModifier ? data.resultModifier : {
        baseItemId: data.result?.itemId || data.result?.item || '',
        baseAmount: data.result?.amount || 1,
        tierModifiers: {}
      };
      onFieldChange('$patch', { resultType: 'dynamic', resultModifier: modifier });
      return;
    }
    const result = Object.keys(asRecord(data.result)).length > 0 ? data.result : {
      itemId: data.resultModifier?.baseItemId || '',
      amount: data.resultModifier?.baseAmount || 1,
      itemLevel: 0,
      rarity: 'COMMON'
    };
    onFieldChange('$patch', { resultType: 'fixed', result, resultModifier: undefined });
  };
  return (
    <Stack gap="sm">
      <Text fw={700}>Job recipe</Text>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Group grow align="start">
            <TextInput label="id" value={String(data.id || '')} disabled={disabled} onChange={(event) => onFieldChange('id', event.currentTarget.value)} />
            <TextInput label="displayName" value={String(data.displayName || data.id || '')} disabled={disabled} onChange={(event) => onFieldChange('displayName', event.currentTarget.value)} />
            <ValueField label="jobId" value={data.jobId || ''} kind="enum" disabled={disabled} param={{ name: 'jobId', description: '', defaultValue: '', required: true, javaType: 'String', editorKind: 'enum', options: (assets.hephaestusJobs || []).map((item) => item.id) }} onChange={(value) => onFieldChange('jobId', value)} />
          </Group>
          <Group grow align="start">
            <NumberInput label="requiredLevel" value={Number(data.requiredLevel || 0)} disabled={disabled} onChange={(value) => onFieldChange('requiredLevel', Number(value || 0))} />
            <NumberInput label="baseExperience" value={Number(data.baseExperience || 0)} disabled={disabled} onChange={(value) => onFieldChange('baseExperience', Number(value || 0))} />
            <NumberInput label="craftingTime (ticks)" value={Number(data.craftingTime || 0)} disabled={disabled} onChange={(value) => onFieldChange('craftingTime', Number(value || 0))} />
          </Group>
          <Group grow align="center">
            <ValueField label="minRarity" value={data.minRarity || 'COMMON'} kind="enum" disabled={disabled} param={{ name: 'minRarity', description: '', defaultValue: '', required: false, javaType: 'String', editorKind: 'enum', options: rarityOptions(assets) }} onChange={(value) => onFieldChange('minRarity', value)} />
            <Checkbox label="discoverable" checked={data.discoverable !== false} disabled={disabled} onChange={(event) => onFieldChange('discoverable', event.currentTarget.checked)} />
          </Group>
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Group justify="space-between" mb="sm">
          <Text fw={700}>Ingredients</Text>
          <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => onFieldChange('ingredients', listToObject([...ingredients, { type: 'fixed', itemId: '', amount: 1, minLevel: 0, consumeOnCraft: true }]))}>Ingredient</Button>
        </Group>
        <Stack gap="sm">
          {ingredients.map((ingredient, index) => <JobIngredientEditor key={index} ingredient={ingredient} index={index} ingredients={ingredients} disabled={disabled} assets={assets} onChange={(next) => onFieldChange('ingredients', listToObject(next))} />)}
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Select label="result mode" value={dynamic ? 'dynamic' : 'fixed'} disabled={disabled} data={[{ value: 'fixed', label: 'Fixed result' }, { value: 'dynamic', label: 'Dynamic ResultModifier' }]} onChange={(value) => setResultMode(value || 'fixed')} />
          {dynamic
            ? <ResultModifierEditor value={data.resultModifier || { baseItemId: '', baseAmount: 1, tierModifiers: {} }} disabled={disabled} assets={assets} onChange={(value) => onFieldChange('resultModifier', value)} />
            : <RecipeResultEditor result={data.result || { itemId: '', amount: 1, itemLevel: 0, rarity: 'COMMON' }} disabled={disabled} assets={assets} onChange={(value) => onFieldChange('result', value)} />}
        </Stack>
      </Paper>
    </Stack>
  );
}

function ShapedVanillaGrid({ pattern, ingredients, disabled, assets, onChange }: { pattern: string[]; ingredients: Record<string, unknown>; disabled: boolean; assets: AssetCatalog; onChange: (pattern: string[], ingredients: Record<string, unknown>) => void }) {
  const cellItem = (row: number, col: number) => {
    const symbol = pattern[row]?.[col] || ' ';
    return symbol.trim() ? String(ingredients[symbol] || '') : '';
  };
  const setCell = (row: number, col: number, item: unknown) => {
    const rows = patternRows(pattern);
    const nextIngredients = { ...ingredients };
    if (!item) {
      rows[row] = replaceAt(rows[row], col, ' ');
      onChange(rows, pruneIngredients(rows, nextIngredients));
      return;
    }
    let symbol = Object.entries(nextIngredients).find(([, value]) => value === item)?.[0] || nextSymbol(nextIngredients);
    nextIngredients[symbol] = item;
    rows[row] = replaceAt(rows[row], col, symbol);
    onChange(rows, pruneIngredients(rows, nextIngredients));
  };
  return (
    <Paper p="md" withBorder className="slotBox">
      <Text fw={700} mb="sm">Crafting grid</Text>
      <div className="craftingGrid">
        {[0, 1, 2].map((row) => [0, 1, 2].map((col) => (
          <Paper key={`${row}-${col}`} p="xs" withBorder>
            <ValueField label={`${row + 1},${col + 1}`} value={cellItem(row, col)} kind="item" disabled={disabled} assets={assets} onChange={(value) => setCell(row, col, value)} />
          </Paper>
        )))}
      </div>
    </Paper>
  );
}

function ShapelessVanillaIngredients({ ingredients, disabled, assets, onChange }: { ingredients: string[]; disabled: boolean; assets: AssetCatalog; onChange: (ingredients: string[]) => void }) {
  return (
    <Paper p="md" withBorder className="slotBox">
      <Group justify="space-between" mb="sm"><Text fw={700}>Ingredients</Text><Button size="xs" variant="light" disabled={disabled} leftSection={<IconPlus size={14} />} onClick={() => onChange([...ingredients, ''])}>Ingredient</Button></Group>
      <Stack gap="xs">
        {ingredients.map((ingredient, index) => (
          <Group key={index} align="end">
            <ValueField label={`ingredient ${index + 1}`} value={ingredient} kind="item" disabled={disabled} assets={assets} onChange={(value) => onChange(ingredients.map((item, itemIndex) => itemIndex === index ? String(value || '') : item))} />
            <MoveDeleteControls disabled={disabled} index={index} length={ingredients.length} onMove={(direction) => onChange(moveItem(ingredients, index, direction))} onDelete={() => onChange(ingredients.filter((_, itemIndex) => itemIndex !== index))} />
          </Group>
        ))}
      </Stack>
    </Paper>
  );
}

function RecipeResultEditor({ result, disabled, assets, onChange, vanilla = false }: { result: Record<string, any>; disabled: boolean; assets: AssetCatalog; onChange: (result: Record<string, unknown>) => void; vanilla?: boolean }) {
  const itemKey = vanilla ? 'item' : 'itemId';
  return (
    <Paper p="sm" withBorder className="slotBox">
      <Text fw={700} mb="xs">Result</Text>
      <Group grow align="start">
        <ValueField label={itemKey} value={result[itemKey] || ''} kind="item" disabled={disabled} assets={assets} onChange={(value) => onChange({ ...result, [itemKey]: value })} />
        <NumberInput label="amount" value={Number(result.amount || 1)} disabled={disabled} onChange={(value) => onChange({ ...result, amount: Number(value || 1) })} />
      </Group>
      {vanilla ? <VanillaLevelEditor result={result} disabled={disabled} onChange={onChange} /> : (
        <Group grow align="start" mt="sm">
          <NumberInput label="itemLevel" value={Number(result.itemLevel || 0)} disabled={disabled} onChange={(value) => onChange({ ...result, itemLevel: Number(value || 0) })} />
          <ValueField label="rarity" value={result.rarity || 'COMMON'} kind="enum" disabled={disabled} param={{ name: 'rarity', description: '', defaultValue: '', required: false, javaType: 'String', editorKind: 'enum', options: rarityOptions(assets) }} onChange={(value) => onChange({ ...result, rarity: value })} />
          <SocketPatternEditor value={String(result.socketPattern || '')} disabled={disabled} onChange={(socketPattern) => onChange({ ...result, socketPattern: socketPattern || undefined })} />
        </Group>
      )}
    </Paper>
  );
}

function VanillaLevelEditor({ result, disabled, onChange }: { result: Record<string, any>; disabled: boolean; onChange: (result: Record<string, unknown>) => void }) {
  const level = result.level;
  const mode = level == null ? 'none' : typeof level === 'object' ? 'range' : String(level).includes('-') ? 'range' : 'fixed';
  const parsed = typeof level === 'object' ? level : parseLevelString(String(level || '0'));
  return (
    <Group grow align="start" mt="sm">
      <Select label="level" value={mode} disabled={disabled} data={[{ value: 'none', label: 'No level' }, { value: 'fixed', label: 'Fixed' }, { value: 'range', label: 'Range' }]} onChange={(value) => {
        if (value === 'none') onChange(omit(result, ['level']));
        if (value === 'fixed') onChange({ ...result, level: 1 });
        if (value === 'range') onChange({ ...result, level: { min: 1, max: 1 } });
      }} />
      {mode === 'fixed' && <NumberInput label="fixed level" value={Number(level || 1)} disabled={disabled} onChange={(value) => onChange({ ...result, level: Number(value || 0) })} />}
      {mode === 'range' && <>
        <NumberInput label="min" value={Number(parsed.min || 0)} disabled={disabled} onChange={(value) => onChange({ ...result, level: { ...parsed, min: Number(value || 0) } })} />
        <NumberInput label="max" value={Number(parsed.max || 0)} disabled={disabled} onChange={(value) => onChange({ ...result, level: { ...parsed, max: Number(value || 0) } })} />
      </>}
    </Group>
  );
}

function JobIngredientEditor({ ingredient, index, ingredients, disabled, assets, onChange }: { ingredient: Record<string, any>; index: number; ingredients: Record<string, any>[]; disabled: boolean; assets: AssetCatalog; onChange: (ingredients: Record<string, any>[]) => void }) {
  const type = String(ingredient.type || 'fixed');
  const update = (patch: Record<string, unknown>) => onChange(ingredients.map((item, itemIndex) => itemIndex === index ? { ...item, ...patch } : item));
  return (
    <Paper p="sm" withBorder>
      <Group justify="space-between" mb="xs">
        <Text fw={600}>Ingredient {index + 1}</Text>
        <MoveDeleteControls disabled={disabled} index={index} length={ingredients.length} onMove={(direction) => onChange(moveItem(ingredients, index, direction))} onDelete={() => onChange(ingredients.filter((_, itemIndex) => itemIndex !== index))} />
      </Group>
      <Group grow align="start">
        <Select label="type" value={type} disabled={disabled} data={['fixed', 'choice']} onChange={(value) => update(value === 'choice' ? { type: 'choice', choice: ingredient.choice || { choiceId: '', options: {} } } : { type: 'fixed', itemId: ingredient.itemId || '' })} />
        <NumberInput label="amount" value={Number(ingredient.amount || 1)} disabled={disabled} onChange={(value) => update({ amount: Number(value || 1) })} />
        <NumberInput label="minLevel" value={Number(ingredient.minLevel || 0)} disabled={disabled} onChange={(value) => update({ minLevel: Number(value || 0) })} />
        <Checkbox label="consume" checked={ingredient.consumeOnCraft !== false} disabled={disabled} onChange={(event) => update({ consumeOnCraft: event.currentTarget.checked })} />
      </Group>
      {type === 'choice'
        ? <ChoiceEditor value={ingredient.choice || { choiceId: '', options: {} }} disabled={disabled} assets={assets} onChange={(choice) => update({ choice })} />
        : <ValueField label="itemId" value={ingredient.itemId || ''} kind="item" disabled={disabled} assets={assets} onChange={(value) => update({ itemId: value })} />}
    </Paper>
  );
}

function ChoiceEditor({ value, disabled, assets, onChange }: { value: Record<string, any>; disabled: boolean; assets: AssetCatalog; onChange: (value: Record<string, unknown>) => void }) {
  const options = objectList(value.options);
  return (
    <Stack gap="xs" mt="sm">
      <TextInput label="choiceId" value={String(value.choiceId || '')} disabled={disabled} onChange={(event) => onChange({ ...value, choiceId: event.currentTarget.value })} />
      <Group justify="space-between"><Text fw={600}>Choice options</Text><Button size="xs" variant="light" disabled={disabled} leftSection={<IconPlus size={14} />} onClick={() => onChange({ ...value, options: listToObject([...options, { itemId: '', tier: 0, minLevel: 0 }]) })}>Option</Button></Group>
      {options.map((option, index) => (
        <Group key={index} align="end">
          <ValueField label="itemId" value={option.itemId || ''} kind="item" disabled={disabled} assets={assets} onChange={(itemId) => onChange({ ...value, options: listToObject(options.map((row, rowIndex) => rowIndex === index ? { ...row, itemId } : row)) })} />
          <NumberInput label="tier" value={Number(option.tier || 0)} disabled={disabled} onChange={(tier) => onChange({ ...value, options: listToObject(options.map((row, rowIndex) => rowIndex === index ? { ...row, tier: Number(tier || 0) } : row)) })} />
          <NumberInput label="minLevel" value={Number(option.minLevel || 0)} disabled={disabled} onChange={(minLevel) => onChange({ ...value, options: listToObject(options.map((row, rowIndex) => rowIndex === index ? { ...row, minLevel: Number(minLevel || 0) } : row)) })} />
          <ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => onChange({ ...value, options: listToObject(options.filter((_, rowIndex) => rowIndex !== index)) })}><IconTrash size={16} /></ActionIcon>
        </Group>
      ))}
    </Stack>
  );
}

function ResultModifierEditor({ value, disabled, assets, onChange }: { value: Record<string, any>; disabled: boolean; assets: AssetCatalog; onChange: (value: Record<string, unknown>) => void }) {
  const rows: Record<string, any>[] = Object.entries(asRecord(value.tierModifiers)).map(([tier, raw]) => ({ tier: Number(tier), ...asRecord(raw) }));
  const updateRows = (nextRows: Record<string, any>[]) => onChange({ ...value, tierModifiers: Object.fromEntries(nextRows.map((row) => [String(row.tier), omit(row, ['tier'])])) });
  return (
    <Stack gap="sm">
      <Group grow align="start">
        <ValueField label="baseItemId" value={value.baseItemId || ''} kind="item" disabled={disabled} assets={assets} onChange={(baseItemId) => onChange({ ...value, baseItemId })} />
        <NumberInput label="baseAmount" value={Number(value.baseAmount || 1)} disabled={disabled} onChange={(baseAmount) => onChange({ ...value, baseAmount: Number(baseAmount || 1) })} />
      </Group>
      <Group justify="space-between"><Text fw={700}>Tier modifiers</Text><Button size="xs" variant="light" disabled={disabled} leftSection={<IconPlus size={14} />} onClick={() => updateRows([...rows, { tier: nextTier(rows), itemLevel: 0, rarity: 'COMMON' }])}>Tier</Button></Group>
      <Table.ScrollContainer minWidth={760}>
        <Table>
          <Table.Thead><Table.Tr><Table.Th>Tier</Table.Th><Table.Th>Item override</Table.Th><Table.Th>Amount</Table.Th><Table.Th>Level</Table.Th><Table.Th>Rarity</Table.Th><Table.Th>Socket</Table.Th><Table.Th /></Table.Tr></Table.Thead>
          <Table.Tbody>
            {rows.map((row, index) => (
              <Table.Tr key={index}>
                <Table.Td><NumberInput value={Number(row.tier || 0)} disabled={disabled} onChange={(tier) => updateRows(rows.map((item, itemIndex) => itemIndex === index ? { ...item, tier: Number(tier || 0) } : item))} /></Table.Td>
                <Table.Td><ValueField label="itemId" value={row.itemId || ''} kind="item" disabled={disabled} assets={assets} onChange={(itemId) => updateRows(rows.map((item, itemIndex) => itemIndex === index ? { ...item, itemId } : item))} /></Table.Td>
                <Table.Td><NumberInput value={Number(row.amount || 0)} disabled={disabled} onChange={(amount) => updateRows(rows.map((item, itemIndex) => itemIndex === index ? { ...item, amount: Number(amount || 0) } : item))} /></Table.Td>
                <Table.Td><NumberInput value={Number(row.itemLevel || 0)} disabled={disabled} onChange={(itemLevel) => updateRows(rows.map((item, itemIndex) => itemIndex === index ? { ...item, itemLevel: Number(itemLevel || 0) } : item))} /></Table.Td>
                <Table.Td><ValueField label="rarity" value={row.rarity || 'COMMON'} kind="enum" disabled={disabled} param={{ name: 'rarity', description: '', defaultValue: '', required: false, javaType: 'String', editorKind: 'enum', options: rarityOptions(assets) }} onChange={(rarity) => updateRows(rows.map((item, itemIndex) => itemIndex === index ? { ...item, rarity } : item))} /></Table.Td>
                <Table.Td><SocketPatternEditor value={String(row.socketPattern || '')} disabled={disabled} onChange={(socketPattern) => updateRows(rows.map((item, itemIndex) => itemIndex === index ? { ...item, socketPattern: socketPattern || undefined } : item))} compact /></Table.Td>
                <Table.Td><ActionIcon color="red" variant="subtle" disabled={disabled} onClick={() => updateRows(rows.filter((_, rowIndex) => rowIndex !== index))}><IconTrash size={16} /></ActionIcon></Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
    </Stack>
  );
}

function SocketPatternEditor({ value, disabled, onChange, compact = false }: { value: string; disabled: boolean; onChange: (value: string) => void; compact?: boolean }) {
  const pattern = normalizeSocketPattern(value);
  const updateChar = (index: number, next: string | null) => {
    const chars = pattern.split('');
    if (next == null) chars.splice(index, 1);
    else chars[index] = next;
    onChange(chars.join(''));
  };
  const move = (index: number, direction: number) => {
    const chars = pattern.split('');
    const target = index + direction;
    if (target < 0 || target >= chars.length) return;
    const [char] = chars.splice(index, 1);
    chars.splice(target, 0, char);
    onChange(chars.join(''));
  };
  const add = (char: string) => onChange(pattern + char);
  return (
    <Stack gap={4}>
      {!compact && <Text size="sm" fw={500}>socketPattern</Text>}
      <Group gap={4} wrap="wrap">
        {pattern.split('').map((char, index) => (
          <Group key={`${char}-${index}`} gap={2} className="socketSlotGroup">
            <Badge
              variant="filled"
              radius="sm"
              styles={{ root: { minWidth: 34, justifyContent: 'center', ...socketStyle(char) } }}
            >
              {socketLabel(char)}
            </Badge>
            <ActionIcon size="xs" variant="subtle" disabled={disabled || index === 0} onClick={() => move(index, -1)}><IconArrowUp size={12} /></ActionIcon>
            <ActionIcon size="xs" variant="subtle" disabled={disabled || index === pattern.length - 1} onClick={() => move(index, 1)}><IconArrowDown size={12} /></ActionIcon>
            <ActionIcon size="xs" variant="subtle" color="red" disabled={disabled} onClick={() => updateChar(index, null)}><IconTrash size={12} /></ActionIcon>
          </Group>
        ))}
        {pattern.length === 0 && <Text size="xs" c="dimmed">No sockets</Text>}
      </Group>
      <Group gap={4}>
        {(['R', 'G', 'B', 'P'] as const).map((char) => (
          <Button key={char} size="compact-xs" variant="light" color={socketColor(char)} disabled={disabled} onClick={() => add(char)}>
            +{socketLabel(char)}
          </Button>
        ))}
      </Group>
      <TextInput
        label={compact ? undefined : 'Raw compact pattern'}
        value={pattern}
        disabled={disabled}
        placeholder="RRG, BBRR, RPG"
        onChange={(event) => onChange(normalizeSocketPattern(event.currentTarget.value))}
      />
      {!compact && <Text size="xs" c="dimmed">R red, G green, B blue, P prismatic/any. Unknown characters are kept in the raw pattern.</Text>}
    </Stack>
  );
}

function normalizeSocketPattern(value: string) {
  return String(value || '').toUpperCase().replace(/\s+/g, '');
}

function socketColor(char: string) {
  if (char === 'R') return 'red';
  if (char === 'G') return 'green';
  if (char === 'B') return 'blue';
  if (char === 'P') return 'grape';
  return 'gray';
}

function socketStyle(char: string) {
  if (char === 'R') return { background: '#c92a2a', color: 'white', boxShadow: 'inset 0 0 0 1px rgba(255,255,255,0.18)' };
  if (char === 'G') return { background: '#2b8a3e', color: 'white', boxShadow: 'inset 0 0 0 1px rgba(255,255,255,0.18)' };
  if (char === 'B') return { background: '#1971c2', color: 'white', boxShadow: 'inset 0 0 0 1px rgba(255,255,255,0.18)' };
  if (char === 'P') {
    return {
      background: 'linear-gradient(135deg, #c92a2a 0%, #f08c00 24%, #2b8a3e 48%, #1971c2 72%, #9c36b5 100%)',
      color: 'white',
      boxShadow: '0 0 10px rgba(156,54,181,0.35), inset 0 0 0 1px rgba(255,255,255,0.25)'
    };
  }
  return { background: '#495057', color: 'white' };
}

function socketLabel(char: string) {
  if (char === 'P') return 'Any';
  return char || '?';
}

export function HephaestusShopEditor({ data, disabled, assets, onFieldChange }: { data: Record<string, any>; disabled: boolean; assets: AssetCatalog; onFieldChange: FieldChange }) {
  const rows = shopRows(data.items);
  const updateRows = (nextRows: Record<string, any>[]) => onFieldChange('items', shopItemsFromRows(nextRows));
  return (
    <Stack gap="sm">
      <Text fw={700}>Hephaestus shop</Text>
      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <TextInput label="name" value={String(data.name || '')} disabled={disabled} onChange={(event) => onFieldChange('name', event.currentTarget.value)} />
        </Stack>
      </Paper>
      <Paper p="md" withBorder className="slotBox">
        <Group justify="space-between" mb="sm">
          <Text fw={700}>Shop items</Text>
          <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} disabled={disabled} onClick={() => updateRows([...rows, { itemId: '', type: 'BOTH', buyPrice: 0, sellPrice: 0, restockAmount: -1, restockTime: 60 }])}>Item</Button>
        </Group>
        <Table.ScrollContainer minWidth={980}>
          <Table>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>Item</Table.Th>
                <Table.Th>Type</Table.Th>
                <Table.Th>Buy</Table.Th>
                <Table.Th>Sell</Table.Th>
                <Table.Th>Restock amount</Table.Th>
                <Table.Th>Restock minutes</Table.Th>
                <Table.Th />
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {rows.map((row, index) => (
                <Table.Tr key={`${row.itemId}-${index}`}>
                  <Table.Td><ValueField label="item" value={row.itemId || ''} kind="item" disabled={disabled} assets={assets} onChange={(itemId) => updateRows(rows.map((item, rowIndex) => rowIndex === index ? { ...item, itemId } : item))} /></Table.Td>
                  <Table.Td><Select value={String(row.type || 'BUY').toUpperCase()} disabled={disabled} data={['BUY', 'SELL', 'BOTH']} onChange={(type) => updateRows(rows.map((item, rowIndex) => rowIndex === index ? { ...item, type: type || 'BUY' } : item))} /></Table.Td>
                  <Table.Td><NumberInput value={Number(row.buyPrice || 0)} disabled={disabled} onChange={(buyPrice) => updateRows(rows.map((item, rowIndex) => rowIndex === index ? { ...item, buyPrice: Number(buyPrice || 0) } : item))} /></Table.Td>
                  <Table.Td><NumberInput value={Number(row.sellPrice || 0)} disabled={disabled} onChange={(sellPrice) => updateRows(rows.map((item, rowIndex) => rowIndex === index ? { ...item, sellPrice: Number(sellPrice || 0) } : item))} /></Table.Td>
                  <Table.Td><NumberInput value={Number(row.restockAmount ?? -1)} disabled={disabled} onChange={(restockAmount) => updateRows(rows.map((item, rowIndex) => rowIndex === index ? { ...item, restockAmount: Number(restockAmount ?? -1) } : item))} /></Table.Td>
                  <Table.Td><NumberInput value={Number(row.restockTime || 60)} disabled={disabled} onChange={(restockTime) => updateRows(rows.map((item, rowIndex) => rowIndex === index ? { ...item, restockTime: Number(restockTime || 0) } : item))} /></Table.Td>
                  <Table.Td><MoveDeleteControls disabled={disabled} index={index} length={rows.length} onMove={(direction) => updateRows(moveItem(rows, index, direction))} onDelete={() => updateRows(rows.filter((_, rowIndex) => rowIndex !== index))} /></Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Paper>
    </Stack>
  );
}

function MoveDeleteControls({ disabled, index, length, onMove, onDelete }: { disabled: boolean; index: number; length: number; onMove: (direction: number) => void; onDelete: () => void }) {
  return <Group gap={4}><ActionIcon variant="subtle" disabled={disabled || index === 0} onClick={() => onMove(-1)}><IconArrowUp size={16} /></ActionIcon><ActionIcon variant="subtle" disabled={disabled || index === length - 1} onClick={() => onMove(1)}><IconArrowDown size={16} /></ActionIcon><ActionIcon color="red" variant="subtle" disabled={disabled} onClick={onDelete}><IconTrash size={16} /></ActionIcon></Group>;
}

type AttributeRow = { attribute: string; level: number; min: number; max: number; weight: number };

function flattenAttributeRows(attributes: Record<string, any>): AttributeRow[] {
  const rows: AttributeRow[] = [];
  for (const [attribute, levels] of Object.entries(asRecord(attributes))) {
    for (const [level, raw] of Object.entries(asRecord(levels))) {
      if (Array.isArray(raw)) {
        raw.forEach((entry) => rows.push({ attribute, level: Number(level), min: Number(asRecord(entry).min || 0), max: Number(asRecord(entry).max || 0), weight: Number(asRecord(entry).weight || 0) }));
      } else {
        for (const [value, weight] of Object.entries(asRecord(raw))) rows.push({ attribute, level: Number(level), min: Number(value), max: Number(value), weight: Number(weight || 0) });
      }
    }
  }
  return rows;
}

function attributesFromRows(rows: AttributeRow[]) {
  const result: Record<string, Record<string, Array<{ min: number; max: number; weight: number }>>> = {};
  rows.forEach((row) => {
    if (!result[row.attribute]) result[row.attribute] = {};
    const level = String(row.level || 0);
    if (!result[row.attribute][level]) result[row.attribute][level] = [];
    result[row.attribute][level].push({ min: row.min, max: row.max, weight: row.weight });
  });
  return result;
}

function updateAttributeRow(rows: AttributeRow[], index: number, patch: Partial<AttributeRow>, onFieldChange: FieldChange) {
  onFieldChange('attributes', attributesFromRows(rows.map((row, rowIndex) => rowIndex === index ? { ...row, ...patch } : row)));
}

function translatableFromLocaleMap(value: unknown) {
  const map = asRecord(value);
  return `en=${String(map.en || map.en_us || '')}; de=${String(map.de || map.de_de || '')}`;
}

function localeMapFromTranslatable(value: string) {
  const parsed = parseTranslatable(value);
  return { en_us: parsed.en, de_de: parsed.de };
}

function translatableFromFlatTranslations(value: unknown, prefix: string) {
  const map = asRecord(value);
  const nested = asRecord(map[prefix]);
  const en = String(map[`${prefix}.en`] || map[`${prefix}.en_us`] || nested.en || nested.en_us || '');
  const de = String(map[`${prefix}.de`] || map[`${prefix}.de_de`] || nested.de || nested.de_de || '');
  return en || de ? `en=${en}; de=${de}` : '';
}

function flatTranslationsFromTranslatable(prefix: string, value: string) {
  const parsed = parseTranslatable(value);
  return { [`${prefix}.en`]: parsed.en, [`${prefix}.de`]: parsed.de };
}

function parseTranslatable(value: string) {
  const result = { en: '', de: '' };
  value.split(';').forEach((part) => {
    const index = part.indexOf('=');
    if (index <= 0) return;
    const key = part.slice(0, index).trim();
    const text = part.slice(index + 1).trim();
    if (key === 'en' || key === 'en_us') result.en = text;
    if (key === 'de' || key === 'de_de') result.de = text;
  });
  return result;
}

function listValue(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(String);
  if (value == null || value === '') return [];
  return String(value).split(',').map((part) => part.trim()).filter(Boolean);
}

function objectList(value: unknown): Record<string, any>[] {
  if (Array.isArray(value)) return value.map(asRecord);
  return Object.entries(asRecord(value)).sort(([left], [right]) => Number(left) - Number(right)).map(([, raw]) => asRecord(raw));
}

function listToObject(rows: Record<string, any>[]) {
  return Object.fromEntries(rows.map((row, index) => [String(index), row]));
}

function asRecord(value: unknown): Record<string, any> {
  return typeof value === 'object' && value !== null && !Array.isArray(value) ? value as Record<string, any> : {};
}

function patternRows(value: unknown): string[] {
  const rows = Array.isArray(value) ? value.map(String) : [];
  return [0, 1, 2].map((index) => (rows[index] || '').padEnd(3, ' ').slice(0, 3));
}

function replaceAt(value: string, index: number, next: string) {
  return value.slice(0, index) + next + value.slice(index + 1);
}

function nextSymbol(ingredients: Record<string, unknown>) {
  for (const symbol of 'ABCDEFGHIJKLMNOPQRSTUVWXYZ') if (!ingredients[symbol]) return symbol;
  return 'Z';
}

function pruneIngredients(pattern: string[], ingredients: Record<string, unknown>) {
  const used = new Set(pattern.join('').split('').filter((symbol) => symbol.trim()));
  return Object.fromEntries(Object.entries(ingredients).filter(([symbol]) => used.has(symbol)));
}

function parseLevelString(value: string) {
  const [min, max = min] = value.split('-').map((part) => Number(part.trim() || 0));
  return { min, max };
}

function omit(source: Record<string, any>, keys: string[]) {
  return Object.fromEntries(Object.entries(source).filter(([key]) => !keys.includes(key)));
}

function moveItem<T>(items: T[], index: number, direction: number) {
  const next = [...items];
  const target = index + direction;
  if (target < 0 || target >= next.length) return next;
  const [item] = next.splice(index, 1);
  next.splice(target, 0, item);
  return next;
}

function nextTier(rows: Record<string, any>[]) {
  return rows.reduce((max, row) => Math.max(max, Number(row.tier || 0)), 0) + 1;
}

function shopRows(value: unknown): Record<string, any>[] {
  return Object.entries(asRecord(value)).map(([itemId, raw]) => ({ itemId, ...asRecord(raw) }));
}

function shopItemsFromRows(rows: Record<string, any>[]) {
  const result: Record<string, Record<string, unknown>> = {};
  rows.forEach((row) => {
    const itemId = String(row.itemId || '').trim();
    if (!itemId) return;
    result[itemId] = omit(row, ['itemId']);
  });
  return result;
}

function rarityOptions(assets: AssetCatalog) {
  return (assets.hephaestusRarities && assets.hephaestusRarities.length > 0 ? assets.hephaestusRarities.map((item) => item.id) : ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY']);
}
