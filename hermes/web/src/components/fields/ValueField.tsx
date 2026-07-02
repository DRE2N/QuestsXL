import { useEffect, useMemo, useState } from 'react';
import { Box, Button, Checkbox, ColorInput, ColorPicker, Group, MultiSelect, NumberInput, Paper, Popover, SegmentedControl, Select, Stack, Text, TextInput, Textarea } from '@mantine/core';
import { RichTextEditor } from '@mantine/tiptap';
import { IconBraces, IconPalette } from '@tabler/icons-react';
import Color from '@tiptap/extension-color';
import { TextStyle } from '@tiptap/extension-text-style';
import Underline from '@tiptap/extension-underline';
import { useEditor } from '@tiptap/react';
import StarterKit from '@tiptap/starter-kit';
import { parseDocument, stringify } from 'yaml';
import type { AssetCatalog, AssetOption, ComponentParam, EditorKind, VariableOption } from '../../api/types';
import { variableToken } from '../../catalog/variables';
import { htmlToMiniMessage, miniMessageToTiptapDoc } from '../../lib/minimessagePreview';

export function ValueField({ label, value, description, error, kind, param, disabled, onChange, assets = emptyAssets(), variables = [] }: { label: string; value: unknown; description?: string; error?: string; kind?: EditorKind; param?: ComponentParam; disabled: boolean; onChange: (value: unknown) => void; assets?: AssetCatalog; variables?: VariableOption[] }) {
  const lower = label.toLowerCase();
  const currentValue = value ?? defaultDisplayValue(param);
  const inferred = kind || (typeof currentValue === 'boolean' ? 'boolean' : typeof currentValue === 'number' || ['x', 'y', 'z', 'range', 'radius', 'cooldown', 'goal', 'amount', 'duration'].includes(lower) ? 'number' : 'string');
  if (inferred === 'location') return <LocationField label={label} value={currentValue} description={description} error={error} disabled={disabled} assets={assets} onChange={onChange} />;
  if (inferred === 'item') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.items} fallbackPlaceholder="Hephaestus item key" onChange={onChange} />;
  if (inferred === 'itemList') return <ListPickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.items} fallbackPlaceholder="Hephaestus item keys" onChange={onChange} />;
  if (inferred === 'mob') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.mobs} fallbackPlaceholder="Aether mob id" onChange={onChange} />;
  if (inferred === 'mobList') return <ListPickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.mobs} fallbackPlaceholder="Aether mob ids" onChange={onChange} />;
  if (inferred === 'quest') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.quests || []} fallbackPlaceholder="Quest id" onChange={onChange} />;
  if (inferred === 'event') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.events || []} fallbackPlaceholder="Event id" onChange={onChange} />;
  if (inferred === 'questOrEvent') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={[...(assets.quests || []), ...(assets.events || [])]} fallbackPlaceholder="Quest or event id" onChange={onChange} />;
  if (inferred === 'dialogue') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.dialogues || []} fallbackPlaceholder="Dialogue id" onChange={onChange} />;
  if (inferred === 'shop') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.hephaestusShops || []} fallbackPlaceholder="Shop id" onChange={onChange} />;
  if (inferred === 'spell') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.spellbookSpells || []} fallbackPlaceholder="Spell id" onChange={onChange} />;
  if (inferred === 'spellList') return <ListPickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.spellbookSpells || []} fallbackPlaceholder="Spell ids" onChange={onChange} />;
  if (inferred === 'trait') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.spellbookTraits || []} fallbackPlaceholder="Trait id" onChange={onChange} />;
  if (inferred === 'traitList') return <ListPickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.spellbookTraits || []} fallbackPlaceholder="Trait ids" onChange={onChange} />;
  if (inferred === 'effect') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.spellbookEffects || []} fallbackPlaceholder="Effect id" onChange={onChange} />;
  if (inferred === 'effectList') return <ListPickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.spellbookEffects || []} fallbackPlaceholder="Effect ids" onChange={onChange} />;
  if (inferred === 'hecateClass') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.hecateClasses || []} fallbackPlaceholder="Class id" onChange={onChange} />;
  if (inferred === 'traitline') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.hecateTraitlines || []} fallbackPlaceholder="Traitline id" onChange={onChange} />;
  if (inferred === 'traitlineList') return <ListPickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.hecateTraitlines || []} fallbackPlaceholder="Traitline ids" onChange={onChange} />;
  if (inferred === 'world') return <PickerField label={label} value={currentValue ?? 'Erethon'} description={description} error={error} disabled={disabled} options={assets.worlds} fallbackPlaceholder="Erethon" onChange={onChange} />;
  if (inferred === 'material') return <PickerField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={assets.materials} fallbackPlaceholder="minecraft:stone" onChange={onChange} />;
  if (inferred === 'enum') return <EnumField label={label} value={currentValue} description={description} error={error} disabled={disabled} options={param?.options || []} onChange={onChange} />;
  if (inferred === 'duration' || inferred === 'ticksDuration') return <DurationField label={label} value={currentValue} description={description} error={error} disabled={disabled} baseUnit={inferred === 'ticksDuration' ? 'ticks' : 'seconds'} variables={param?.supportsVariables ? variables : []} onChange={onChange} />;
  if (inferred === 'translatable') return <TranslatableField label={label} value={currentValue} description={description} error={error} disabled={disabled} variables={variables} onChange={onChange} />;
  if (inferred === 'minimessage') return <MiniMessageField label={label} value={currentValue} description={description} error={error} disabled={disabled} compact={isCompactMiniMessageLabel(label)} variables={variables} onChange={onChange} />;
  if (inferred === 'json') return <JsonTextField label={label} value={currentValue} description={description} error={error} disabled={disabled} onChange={onChange} />;
  if (inferred === 'boolean') return param?.supportsVariables ? <BooleanExpressionField label={label} description={description} error={error} value={currentValue} disabled={disabled} variables={variables} onChange={onChange} /> : <Checkbox label={label} description={description} error={error} checked={Boolean(currentValue)} disabled={disabled} onChange={(event) => onChange(event.currentTarget.checked)} />;
  if (inferred === 'number') return param?.supportsVariables ? <NumberExpressionField label={label} description={description} error={error} value={currentValue} disabled={disabled} variables={variables} onChange={onChange} /> : <NumberInput label={label} description={description} error={error} value={Number(currentValue ?? 0)} disabled={disabled} onChange={(next) => onChange(next === '' ? 0 : Number(next))} />;
  if (isPlainObject(currentValue) || Array.isArray(currentValue)) return <Textarea label={label} description={description} error={error} value={stringify(currentValue).trim()} disabled={disabled} autosize minRows={3} onChange={(event) => onChange(parseLooseValue(event.currentTarget.value))} />;
  if (param?.supportsVariables) return <StringExpressionField label={label} description={description} error={error} value={currentValue} disabled={disabled} variables={variables} onChange={onChange} />;
  return <TextInput label={label} description={description} error={error} value={currentValue == null ? '' : String(currentValue)} disabled={disabled} onChange={(event) => onChange(parseScalar(event.currentTarget.value))} />;
}

function PickerField({ label, value, description, error, disabled, options, fallbackPlaceholder, onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; options: AssetOption[]; fallbackPlaceholder: string; onChange: (value: unknown) => void }) {
  const current = value == null ? '' : String(value);
  const optionData = uniqueAssetOptions(options).map((option) => ({ value: option.id, label: optionLabel(option) }));
  const data = current && !optionData.some((option) => option.value === current) ? [...optionData, { value: current, label: current }] : optionData;
  if (data.length === 0) {
    return <TextInput label={label} description={description} error={error} placeholder={fallbackPlaceholder} value={current} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value)} />;
  }
  return <Select label={label} description={description} error={error} value={current} disabled={disabled} data={data} searchable clearable nothingFoundMessage="No matches" onChange={(next) => onChange(next || '')} />;
}

function ListPickerField({ label, value, description, error, disabled, options, fallbackPlaceholder, onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; options: AssetOption[]; fallbackPlaceholder: string; onChange: (value: unknown) => void }) {
  const current = listValue(value);
  const optionData = uniqueAssetOptions(options).map((option) => ({ value: option.id, label: optionLabel(option) }));
  const data = [
    ...optionData,
    ...current.filter((item) => !optionData.some((option) => option.value === item)).map((item) => ({ value: item, label: item }))
  ];
  if (optionData.length === 0) {
    return <TextInput label={label} description={description} error={error} placeholder={fallbackPlaceholder} value={current.join(', ')} disabled={disabled} onChange={(event) => onChange(listValue(event.currentTarget.value))} />;
  }
  return (
    <MultiSelect
      label={label}
      description={description}
      error={error}
      value={current}
      disabled={disabled}
      data={data}
      searchable
      clearable
      hidePickedOptions={false}
      maxDropdownHeight={320}
      nothingFoundMessage="No matches"
      onChange={onChange}
    />
  );
}

function EnumField({ label, value, description, error, disabled, options, onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; options: string[]; onChange: (value: unknown) => void }) {
  if (options.length === 0) return <TextInput label={label} description={description} error={error} value={value == null ? '' : String(value)} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value)} />;
  return <Select label={label} description={description} error={error} value={value == null ? '' : String(value)} disabled={disabled} data={options.map((option) => ({ value: option, label: option }))} searchable nothingFoundMessage="No matches" onChange={(next) => onChange(next || '')} />;
}

function DurationField({ label, value, description, error, disabled, baseUnit, variables = [], onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; baseUnit: 'seconds' | 'ticks'; variables?: VariableOption[]; onChange: (value: unknown) => void }) {
  const variableValue = typeof value === 'string' && value.includes('%');
  const [expressionMode, setExpressionMode] = useState(variableValue);
  const [unit, setUnit] = useState<'seconds' | 'minutes' | 'hours' | 'ticks'>(baseUnit === 'ticks' ? 'ticks' : 'seconds');
  if (expressionMode) {
    return (
      <Stack gap={4}>
        <TextInput label={label} description={description} error={error} value={value == null ? '' : String(value)} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value)} />
        <Group gap="xs">
          <VariableInsertSelect variables={variables} disabled={disabled} onInsert={(token) => onChange(`${value == null ? '' : String(value)}${token}`)} />
          <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={() => setExpressionMode(false)}>Use duration helper</Button>
        </Group>
        {variables.length > 0 && <Text size="xs" c="dimmed">Variables are resolved at runtime.</Text>}
      </Stack>
    );
  }
  const raw = Number(value ?? 0);
  const seconds = baseUnit === 'ticks' ? raw / 20 : raw;
  const display = unit === 'hours' ? seconds / 3600 : unit === 'minutes' ? seconds / 60 : unit === 'ticks' ? raw : seconds;
  const update = (next: number | string) => {
    const numeric = Number(next || 0);
    const asSeconds = unit === 'hours' ? numeric * 3600 : unit === 'minutes' ? numeric * 60 : unit === 'ticks' ? numeric / 20 : numeric;
    onChange(baseUnit === 'ticks' ? Math.round(asSeconds * 20) : asSeconds);
  };
  return (
    <Stack gap={4}>
      <NumberInput label={label} description={description} error={error} value={display} disabled={disabled} onChange={update} />
      <SegmentedControl size="xs" value={unit} disabled={disabled} onChange={(next) => setUnit(next as typeof unit)} data={baseUnit === 'ticks' ? ['ticks', 'seconds'] : ['seconds', 'minutes', 'hours']} />
      {variables.length > 0 && <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={() => setExpressionMode(true)}>Use variable/expression</Button>}
    </Stack>
  );
}

function LocationField({ label, value, description, error, disabled, assets, onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; assets: AssetCatalog; onChange: (value: unknown) => void }) {
  const current = parseLocationValue(value);
  const worldKey = current.w != null ? 'w' : current.world != null ? 'world' : 'w';
  const update = (patch: Record<string, unknown>) => onChange({ ...current, ...patch });
  const worldValue = current[worldKey] ?? 'Erethon';
  return (
    <Paper p="sm" withBorder className="slotBox">
      <Text fw={600} size="sm">{label}</Text>
      {description && <Text size="xs" c="dimmed">{description}</Text>}
      {error && <Text size="xs" c="red">{error}</Text>}
      <PickerField label={worldKey} value={worldValue} disabled={disabled} options={assets.worlds} fallbackPlaceholder="Erethon" onChange={(next) => update({ [worldKey]: next || 'Erethon' })} />
      <Group grow align="start">
        <CoordinateField label="x" parts={coordinateParts(current.x)} disabled={disabled} onChange={(next) => update({ x: serializeCoordinate(next) })} />
        <CoordinateField label="y" parts={coordinateParts(current.y)} disabled={disabled} onChange={(next) => update({ y: serializeCoordinate(next) })} />
        <CoordinateField label="z" parts={coordinateParts(current.z)} disabled={disabled} onChange={(next) => update({ z: serializeCoordinate(next) })} />
      </Group>
      <Group grow>
        <NumberInput label="randomxz" value={Number(current.randomxz ?? 0)} disabled={disabled} onChange={(next) => update({ randomxz: Number(next || 0) })} />
        <NumberInput label="randomy" value={Number(current.randomy ?? 0)} disabled={disabled} onChange={(next) => update({ randomy: Number(next || 0) })} />
      </Group>
    </Paper>
  );
}

function parseLocationValue(value: unknown): Record<string, unknown> {
  if (isPlainObject(value)) return value;
  if (typeof value !== 'string') return {};
  const text = value.trim();
  if (!text) return {};
  const keyed: Record<string, unknown> = {};
  for (const part of text.split(/[;,]/)) {
    const index = part.indexOf('=');
    if (index <= 0) continue;
    const key = part.slice(0, index).trim();
    const raw = part.slice(index + 1).trim();
    keyed[key] = parseScalar(raw);
  }
  if (Object.keys(keyed).length > 0) return keyed;
  const parts = text.split(/\s+/).filter(Boolean);
  if (parts.length >= 3) {
    return { x: parseScalar(parts[0]), y: parseScalar(parts[1]), z: parseScalar(parts[2]) };
  }
  return {};
}

function CoordinateField({ label, parts, disabled, onChange }: { label: string; parts: { relative: boolean; value: number }; disabled: boolean; onChange: (parts: { relative: boolean; value: number }) => void }) {
  return (
    <Stack gap={4}>
      <NumberInput label={label} value={parts.value} disabled={disabled} onChange={(next) => onChange({ ...parts, value: Number(next || 0) })} />
      <Checkbox label="relative" checked={parts.relative} disabled={disabled} onChange={(event) => onChange({ ...parts, relative: event.currentTarget.checked })} />
    </Stack>
  );
}

function TranslatableField({ label, value, description, error, disabled, variables = [], onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; variables?: VariableOption[]; onChange: (value: unknown) => void }) {
  const parsed = parseTranslatable(value);
  const update = (locale: 'en' | 'de', next: string) => onChange(serializeTranslatable({ ...parsed, [locale]: next }));
  const compact = isCompactMiniMessageLabel(label);
  return (
    <Paper p="sm" withBorder className="slotBox">
      <Text fw={600} size="sm">{label}</Text>
      {description && <Text size="xs" c="dimmed">{description}</Text>}
      {error && <Text size="xs" c="red">{error}</Text>}
      <MiniMessageField label="English" value={parsed.en} disabled={disabled} compact={compact} variables={variables} onChange={(next) => update('en', String(next ?? ''))} />
      <MiniMessageField label="German" value={parsed.de} disabled={disabled} compact={compact} variables={variables} onChange={(next) => update('de', String(next ?? ''))} />
      {parsed.raw && <TextInput label="Raw/key" value={parsed.raw} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value)} />}
    </Paper>
  );
}

export function MiniMessageField({ label, value, description, error, disabled, compact = false, placeholders = [], variables = [], onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; compact?: boolean; placeholders?: string[]; variables?: VariableOption[]; onChange: (value: unknown) => void }) {
  const [color, setColor] = useState('#55ff55');
  const [selectedPlaceholder, setSelectedPlaceholder] = useState<string | null>(null);
  const [selectedVariable, setSelectedVariable] = useState<string | null>(null);
  const text = value == null ? '' : String(value);
  const placeholderOptions = useMemo(() => {
    return Array.from(new Set(placeholders.flatMap(normalizePlaceholderTokens)))
      .sort((left, right) => left.localeCompare(right))
      .map((placeholder) => ({ value: placeholder, label: placeholder }));
  }, [placeholders]);
  const editor = useEditor({
    extensions: [
      StarterKit,
      Underline,
      TextStyle,
      Color.configure({ types: ['textStyle'] })
    ],
    content: miniMessageToTiptapDoc(text),
    editable: !disabled,
    immediatelyRender: false,
    onUpdate: ({ editor: current }) => onChange(htmlToMiniMessage(current.getHTML()))
  });

  useEffect(() => {
    if (!editor) return;
    editor.setEditable(!disabled);
  }, [disabled, editor]);

  useEffect(() => {
    if (!editor) return;
    if (htmlToMiniMessage(editor.getHTML()) !== text) {
      editor.commands.setContent(miniMessageToTiptapDoc(text), { emitUpdate: false });
    }
  }, [editor, text]);

  const applyColor = (next: string) => {
    setColor(next);
    editor?.chain().focus().setColor(next).run();
  };

  const insertPlaceholder = (next: string | null) => {
    if (!next) return;
    editor?.chain().focus().insertContent(next).run();
    setSelectedPlaceholder(null);
  };

  const insertVariable = (next: string | null) => {
    if (!next) return;
    editor?.chain().focus().insertContent(next).run();
    setSelectedVariable(null);
  };

  return (
    <Box>
      <Text fw={500} size="sm">{label}</Text>
      {description && <Text size="xs" c="dimmed">{description}</Text>}
      {error && <Text size="xs" c="red">{error}</Text>}
      <RichTextEditor editor={editor} className={compact ? 'miniMessageRte compactMiniMessageRte' : 'miniMessageRte'}>
        <RichTextEditor.Toolbar sticky={false}>
          <RichTextEditor.ControlsGroup>
            <RichTextEditor.Bold />
            <RichTextEditor.Italic />
            <RichTextEditor.Underline />
            <RichTextEditor.Strikethrough />
            <RichTextEditor.ClearFormatting />
          </RichTextEditor.ControlsGroup>
          <RichTextEditor.ControlsGroup>
            {MINI_MESSAGE_SWATCHES.map((swatch) => (
              <RichTextEditor.Control key={swatch.value} disabled={disabled} onClick={() => applyColor(swatch.value)} aria-label={swatch.label} title={swatch.label}>
                <span className="miniMessageColorSwatch" style={{ background: swatch.value }} />
              </RichTextEditor.Control>
            ))}
            <Popover position="bottom-start" shadow="md" withinPortal disabled={disabled}>
              <Popover.Target>
                <RichTextEditor.Control disabled={disabled} aria-label="Pick custom color" title="Pick custom color">
                  <IconPalette size={16} />
                </RichTextEditor.Control>
              </Popover.Target>
              <Popover.Dropdown>
                <Stack gap="xs">
                  <ColorPicker value={color} onChange={applyColor} format="hex" fullWidth swatches={MINI_MESSAGE_SWATCHES.map((swatch) => swatch.value)} />
                  <ColorInput size="xs" value={color} disabled={disabled} onChange={applyColor} w={220} />
                </Stack>
              </Popover.Dropdown>
            </Popover>
          </RichTextEditor.ControlsGroup>
          {placeholderOptions.length > 0 && (
            <RichTextEditor.ControlsGroup>
              <Select
                aria-label="Insert placeholder"
                placeholder="Insert placeholder"
                data={placeholderOptions}
                value={selectedPlaceholder}
                disabled={disabled}
                searchable
                clearable
                nothingFoundMessage="No placeholders"
                maxDropdownHeight={280}
                w={compact ? 180 : 260}
                size="xs"
                onChange={insertPlaceholder}
              />
            </RichTextEditor.ControlsGroup>
          )}
          {variables.length > 0 && (
            <RichTextEditor.ControlsGroup>
              <Popover position="bottom-start" shadow="md" withinPortal disabled={disabled}>
                <Popover.Target>
                  <RichTextEditor.Control disabled={disabled} aria-label="Insert variable" title="Insert variable">
                    <IconBraces size={16} />
                  </RichTextEditor.Control>
                </Popover.Target>
                <Popover.Dropdown>
                  <Select
                    aria-label="Insert variable"
                    placeholder="Insert variable"
                    data={variableSelectData(variables)}
                    value={selectedVariable}
                    disabled={disabled}
                    searchable
                    clearable
                    nothingFoundMessage="No variables"
                    maxDropdownHeight={280}
                    w={260}
                    size="xs"
                    onChange={insertVariable}
                  />
                </Popover.Dropdown>
              </Popover>
            </RichTextEditor.ControlsGroup>
          )}
        </RichTextEditor.Toolbar>
        <RichTextEditor.Content />
      </RichTextEditor>
      {variables.length > 0 && <Text size="xs" c="dimmed" mt={4}>Variables are resolved at runtime</Text>}
    </Box>
  );
}

function NumberExpressionField({ label, value, description, error, disabled, variables, onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; variables: VariableOption[]; onChange: (value: unknown) => void }) {
  const variableValue = typeof value === 'string' && (value.includes('%') || Number.isNaN(Number(value)));
  const [expressionMode, setExpressionMode] = useState(variableValue);
  if (expressionMode) {
    return (
      <Stack gap={4}>
        <TextInput label={label} description={description} error={error} value={value == null ? '' : String(value)} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value)} />
        <Group gap="xs">
          <VariableInsertSelect variables={variables} disabled={disabled} onInsert={(token) => onChange(`${value == null ? '' : String(value)}${token}`)} />
          <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={() => setExpressionMode(false)}>Use number</Button>
        </Group>
        {variables.length > 0 && <Text size="xs" c="dimmed">Variables are resolved at runtime</Text>}
      </Stack>
    );
  }
  return (
    <Stack gap={4}>
      <NumberInput label={label} description={description} error={error} value={Number(value ?? 0)} disabled={disabled} onChange={(next) => onChange(next === '' ? 0 : Number(next))} />
      {variables.length > 0 && <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={() => setExpressionMode(true)}>Use variable/expression</Button>}
    </Stack>
  );
}

function StringExpressionField({ label, value, description, error, disabled, variables, onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; variables: VariableOption[]; onChange: (value: unknown) => void }) {
  const current = value == null ? '' : String(value);
  return (
    <Stack gap={4}>
      <TextInput label={label} description={description} error={error} value={current} disabled={disabled} onChange={(event) => onChange(parseScalar(event.currentTarget.value))} />
      <VariableInsertSelect variables={variables} disabled={disabled} onInsert={(token) => onChange(`${current}${token}`)} />
    </Stack>
  );
}

function BooleanExpressionField({ label, value, description, error, disabled, variables, onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; variables: VariableOption[]; onChange: (value: unknown) => void }) {
  const expressionValue = typeof value === 'string' && value.includes('%');
  const [expressionMode, setExpressionMode] = useState(expressionValue);
  if (expressionMode) {
    return (
      <Stack gap={4}>
        <TextInput label={label} description={description} error={error} value={value == null ? '' : String(value)} disabled={disabled} onChange={(event) => onChange(event.currentTarget.value)} />
        <Group gap="xs">
          <VariableInsertSelect variables={variables} disabled={disabled} onInsert={(token) => onChange(`${value == null ? '' : String(value)}${token}`)} />
          <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={() => setExpressionMode(false)}>Use toggle</Button>
        </Group>
      </Stack>
    );
  }
  return (
    <Stack gap={4}>
      <Checkbox label={label} description={description} error={error} checked={Boolean(value)} disabled={disabled} onChange={(event) => onChange(event.currentTarget.checked)} />
      {variables.length > 0 && <Button size="compact-xs" variant="subtle" disabled={disabled} onClick={() => setExpressionMode(true)}>Use variable/expression</Button>}
    </Stack>
  );
}

function VariableInsertSelect({ variables, disabled, onInsert }: { variables: VariableOption[]; disabled: boolean; onInsert: (token: string) => void }) {
  const [selected, setSelected] = useState<string | null>(null);
  if (variables.length === 0) return null;
  return (
    <Select
      placeholder="Insert variable"
      data={variableSelectData(variables)}
      value={selected}
      disabled={disabled}
      searchable
      clearable
      nothingFoundMessage="No variables"
      size="xs"
      w={220}
      onChange={(next) => {
        if (next) onInsert(next);
        setSelected(null);
      }}
    />
  );
}

function variableSelectData(variables: VariableOption[]) {
  const byToken = new Map<string, { value: string; label: string }>();
  for (const variable of variables) {
    const token = variableToken(variable);
    if (byToken.has(token)) continue;
    const source = variable.source ? ` (${variable.source})` : '';
    byToken.set(token, { value: token, label: `${token}${source}` });
  }
  return Array.from(byToken.values());
}

function normalizePlaceholderTokens(value: string) {
  const tokens = value
    .split(/[\r\n]+/)
    .map((part) => part.trim())
    .filter(Boolean);
  return tokens.length > 0 ? tokens : [value.trim()].filter(Boolean);
}

const MINI_MESSAGE_SWATCHES = [
  { label: 'White', value: '#ffffff' },
  { label: 'Gray', value: '#aaaaaa' },
  { label: 'Dark gray', value: '#555555' },
  { label: 'Green', value: '#55ff55' },
  { label: 'Aqua', value: '#55ffff' },
  { label: 'Yellow', value: '#ffff55' },
  { label: 'Gold', value: '#ffaa00' },
  { label: 'Red', value: '#ff5555' },
  { label: 'Light purple', value: '#ff55ff' }
];

function isCompactMiniMessageLabel(label: string) {
  const lower = label.toLowerCase();
  return lower.includes('displayname') || lower === 'name' || lower.endsWith('.name') || lower.endsWith('name.en') || lower.endsWith('name.de') || lower.includes('sender');
}

function JsonTextField({ label, value, description, error, disabled, onChange }: { label: string; value: unknown; description?: string; error?: string; disabled: boolean; onChange: (value: unknown) => void }) {
  const [text, setText] = useState(value == null ? '' : String(value));
  useEffect(() => { setText(value == null ? '' : String(value)); }, [value]);
  let invalid = false;
  if (text.trim()) {
    try { JSON.parse(text); } catch { invalid = true; }
  }
  return <Textarea label={label} description={description} error={invalid ? 'Invalid JSON' : error} value={text} disabled={disabled} autosize minRows={8} onChange={(event) => setText(event.currentTarget.value)} onBlur={() => onChange(text)} />;
}

function emptyAssets(): AssetCatalog {
  return { quests: [], events: [], items: [], hephaestusShops: [], mobs: [], dialogues: [], hecateClasses: [], hecateTraitlines: [], spellbookSpells: [], spellbookTraits: [], spellbookEffects: [], entityTypes: [], worlds: [], materials: [], attributes: [] };
}

function defaultDisplayValue(param?: ComponentParam) {
  if (!param?.defaultValue || param.defaultValue === ' ') return undefined;
  return parseScalar(param.defaultValue.replaceAll('`', ''));
}

function optionLabel(option: AssetOption) {
  const secondary = option.baseItem || option.displayType || option.modelKey || option.category || '';
  const primary = option.label || option.displayName || option.id;
  const details = [option.id !== primary ? option.id : '', secondary].filter(Boolean).join(', ');
  return details ? `${primary} (${details})` : primary;
}

function uniqueAssetOptions(options: AssetOption[]) {
  const byId = new Map<string, AssetOption>();
  for (const option of options) {
    if (!option.id || byId.has(option.id)) {
      continue;
    }
    byId.set(option.id, option);
  }
  return Array.from(byId.values());
}

function listValue(value: unknown): string[] {
  if (Array.isArray(value)) return value.map(String).map((part) => part.trim()).filter(Boolean);
  return String(value ?? '').split(',').map((part) => part.trim()).filter(Boolean);
}

function coordinateParts(value: unknown) {
  const text = value == null ? '0' : String(value);
  const relative = text.startsWith('~');
  return { relative, value: Number((relative ? text.slice(1) : text) || 0) };
}

function serializeCoordinate(parts: { relative: boolean; value: number }) {
  return parts.relative ? `~${parts.value}` : parts.value;
}

function parseTranslatable(value: unknown) {
  if (isPlainObject(value)) {
    return { en: String(value.en ?? ''), de: String(value.de ?? ''), raw: '' };
  }
  const text = value == null ? '' : String(value);
  const result = { en: '', de: '', raw: '' };
  if (!text.includes('=') && text.includes('.')) return { ...result, raw: text };
  for (const part of text.split(';')) {
    const [locale, ...rest] = part.trim().split('=');
    if (locale === 'en' || locale === 'de') result[locale] = rest.join('=').trim();
    else if (part.trim()) result.raw = part.trim();
  }
  if (!result.en && !result.de && text && !result.raw) result.en = text;
  return result;
}

function serializeTranslatable(value: { en: string; de: string; raw?: string }) {
  const parts = [];
  if (value.en) parts.push(`en=${value.en}`);
  if (value.de) parts.push(`de=${value.de}`);
  return parts.length > 0 ? parts.join('; ') : value.raw || '';
}

function parseLooseValue(text: string): unknown {
  try {
    return parseDocument(text).toJS();
  } catch {
    return text;
  }
}

function parseScalar(text: string): unknown {
  if (text === 'true') return true;
  if (text === 'false') return false;
  if (text.trim() !== '' && !Number.isNaN(Number(text))) return Number(text);
  return text;
}

function asRecord(value: unknown): Record<string, unknown> {
  return isPlainObject(value) ? value : {};
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}
