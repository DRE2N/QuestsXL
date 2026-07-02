import { parseDocument, stringify } from 'yaml';
import type { Catalog, ComponentEntry, ComponentParam, ContentType, EditorKind, EntryCategory, QxlDocument, QxlEntry, QxlInteraction, QxlStage, RewardSlot, Selection } from '../api/types';

export type AddTarget = {
  id: string;
  label: string;
  category: EntryCategory;
  kind: 'stage' | 'entry' | 'interaction' | 'root' | 'reward';
  ownerId?: string;
  slot?: string;
  threshold?: string;
};
function parseQxlDocument(source: string, type: string, catalog: Catalog): QxlDocument {
  const empty = starterDoc(type);
  if (!source.trim()) return empty;
  try {
    const parsed = parseDocument(source).toJS() ?? {};
    if (!isPlainObject(parsed)) return { ...empty, parseError: 'Root YAML value must be a map.' };
    if (type === 'interactions') return parseInteractions(parsed, catalog);
    if (type === 'macros') return parseMacroDocument(parsed, catalog);
    if (type === 'periodicQuests') return parsePeriodicQuestDocument(parsed, catalog);
    if (type === 'globalObjectives') return parseGlobalObjectiveDocument(parsed, catalog);
    return parseQuestLike(type, parsed, catalog);
  } catch (error) {
    return { ...empty, parseError: error instanceof Error ? error.message : 'Invalid YAML' };
  }
}

function parseQuestLike(type: string, root: Record<string, unknown>, catalog: Catalog): QxlDocument {
  const metadata: Record<string, unknown> = {};
  const extras: Record<string, unknown> = {};
  const rootSlots: Record<string, QxlEntry[]> = {};
  const stages: QxlStage[] = [];
  const rewards: RewardSlot[] = [];
  const reserved = new Set(['stages', 'conditions', 'onStart', 'onFinish', 'startConditions', 'onUpdate', 'rewards']);
  for (const [key, value] of Object.entries(root)) {
    if (['conditions', 'startConditions'].includes(key)) rootSlots[key] = parseEntryCollection(value, 'condition', `${key}`, catalog);
    else if (['onStart', 'onFinish', 'onUpdate'].includes(key)) rootSlots[key] = parseEntryCollection(value, 'action', `${key}`, catalog);
    else if (key === 'rewards' && isPlainObject(value)) Object.entries(value).forEach(([threshold, actions]) => rewards.push({ threshold, actions: parseEntryCollection(actions, 'action', `rewards.${threshold}`, catalog) }));
    else if (key === 'stages' && isPlainObject(value)) Object.entries(value).forEach(([id, raw]) => stages.push(parseStage(id, raw, catalog)));
    else if (!reserved.has(key)) metadata[key] = value;
    else extras[key] = value;
  }
  return { type, metadata, rootSlots, stages, interactions: [], rewards, extras };
}

function parseStage(id: string, raw: unknown, catalog: Catalog): QxlStage {
  const source = asRecord(raw);
  const extras: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(source)) {
    if (!['conditions', 'onStart', 'objectives', 'onFinish'].includes(key)) extras[key] = value;
  }
  return {
    id,
    conditions: parseEntryCollection(source.conditions, 'condition', `stages.${id}.conditions`, catalog),
    onStart: parseEntryCollection(source.onStart, 'action', `stages.${id}.onStart`, catalog),
    objectives: parseEntryCollection(source.objectives, 'objective', `stages.${id}.objectives`, catalog),
    onFinish: parseEntryCollection(source.onFinish, 'action', `stages.${id}.onFinish`, catalog),
    extras
  };
}

function parseInteractions(root: Record<string, unknown>, catalog: Catalog): QxlDocument {
  const interactions = Object.entries(root).map(([id, raw]) => {
    const source = asRecord(raw);
    const params: Record<string, unknown> = {};
    const extras: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(source)) {
      if (key === 'objectives') continue;
      if (['world', 'x', 'y', 'z', 'radius', 'repeatable'].includes(key)) params[key] = value;
      else extras[key] = value;
    }
    return { id, params, objectives: parseEntryCollection(source.objectives, 'objective', `interactions.${id}.objectives`, catalog), extras };
  });
  return { type: 'interactions', metadata: {}, rootSlots: {}, stages: [], interactions, rewards: [], extras: {} };
}

function parseMacroDocument(root: Record<string, unknown>, catalog: Catalog): QxlDocument {
  const rootSlots: Record<string, QxlEntry[]> = {};
  const extras: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(root)) {
    if (isPlainObject(value)) {
      rootSlots[key] = 'type' in value
        ? [parseMapEntry(key, value, 'action', key, catalog)]
        : parseEntryCollection(value, 'action', key, catalog);
    } else {
      extras[key] = value;
    }
  }
  return { type: 'macros', metadata: {}, rootSlots, stages: [], interactions: [], rewards: [], extras };
}

function parsePeriodicQuestDocument(root: Record<string, unknown>, catalog: Catalog): QxlDocument {
  const metadata: Record<string, unknown> = {};
  const rootSlots: Record<string, QxlEntry[]> = {};
  const extras: Record<string, unknown> = {};
  for (const [period, raw] of Object.entries(root)) {
    if (!isPlainObject(raw)) {
      extras[period] = raw;
      continue;
    }
    const section = asRecord(raw);
    for (const [key, value] of Object.entries(section)) {
      const path = `${period}.${key}`;
      if (key === 'completionRewards') rootSlots[path] = parseEntryCollection(value, 'action', path, catalog);
      else metadata[path] = value;
    }
  }
  return { type: 'periodicQuests', metadata, rootSlots, stages: [], interactions: [], rewards: [], extras };
}

function parseGlobalObjectiveDocument(root: Record<string, unknown>, catalog: Catalog): QxlDocument {
  const rootSlots: Record<string, QxlEntry[]> = {};
  const extras: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(root)) {
    if (isPlainObject(value) && 'objectives' in value) {
      const section = asRecord(value);
      rootSlots[`${key}.objectives`] = parseEntryCollection(section.objectives, 'objective', `${key}.objectives`, catalog);
      const rest = { ...section };
      delete rest.objectives;
      if (Object.keys(rest).length > 0) extras[key] = rest;
    } else if (key === 'objectives') {
      rootSlots.objectives = parseEntryCollection(value, 'objective', 'objectives', catalog);
    } else {
      extras[key] = value;
    }
  }
  return { type: 'globalObjectives', metadata: {}, rootSlots, stages: [], interactions: [], rewards: [], extras };
}

function parseEntryCollection(raw: unknown, category: EntryCategory, path: string, catalog: Catalog): QxlEntry[] {
  if (raw == null) return [];
  if (Array.isArray(raw)) {
    return raw.map((item, index) => parseListEntry(item, category, `${path}.${index}`, catalog)).filter(Boolean) as QxlEntry[];
  }
  if (isPlainObject(raw)) {
    return Object.entries(raw).map(([key, value]) => parseMapEntry(key, value, category, `${path}.${key}`, catalog));
  }
  return [];
}

function parseListEntry(item: unknown, category: EntryCategory, path: string, catalog: Catalog): QxlEntry | null {
  if (typeof item === 'string') {
    const [type, rest = ''] = item.split(/:(.*)/s);
    const cleanType = type.trim();
    return makeEntry(path, cleanType, cleanType, category, parseInlineParams(cleanType, category, rest.trim(), catalog), {}, {}, catalog);
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
    const type = key;
    const params = canonicalEntryParams(type, category, typeof value === 'string' ? parseInlineParams(type, category, value.trim(), catalog) : value == null ? {} : { [primaryParamName(type, category, catalog)]: value });
    return makeEntry(path, key, type, category, params, {}, {}, catalog);
  }
  const type = typeof value.type === 'string' ? value.type : key;
  const catalogEntry = catalogFor(catalog, category).find((entry) => entry.id === type);
  const slots: Record<string, QxlEntry[]> = {};
  const params: Record<string, unknown> = {};
  const extras: Record<string, unknown> = {};
  const slotNames = new Map(slotDefinitions(category, type, catalogEntry).map((slot) => [slot.name, slot.category]));
  for (const [field, fieldValue] of Object.entries(value)) {
    if (field === 'type') continue;
    const slotCategory = slotNames.get(field);
    if (slotCategory) slots[field] = parseEntryCollection(fieldValue, slotCategory, `${path}.${field}`, catalog);
    else if (isFlatLocationField(type, category, field) && catalogEntry?.params.some((param) => param.name === 'location')) {
      params.location = { ...asRecord(params.location), [field]: fieldValue };
    }
    else if (catalogEntry?.params.some((param) => param.name === field) || isCommonParam(field) || isDeprecatedEntryAlias(type, category, field)) params[field] = fieldValue;
    else extras[field] = fieldValue;
  }
  return makeEntry(path, key, type, category, canonicalEntryParams(type, category, params), slots, extras, catalog);
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

function splitKnownInlineParams(source: string, names: string[]): Record<string, unknown> {
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
    const rawValue = source.slice(valueStart, nextStart).replace(/;\s*$/, '').trim();
    result[name] = parseScalar(rawValue);
  }
  return result;
}

function splitGenericInlineParams(source: string): Record<string, unknown> {
  const result: Record<string, unknown> = {};
  for (const part of source.split(/;\s*/)) {
    const index = part.indexOf('=');
    if (index <= 0) return {};
    result[part.slice(0, index).trim()] = parseScalar(part.slice(index + 1).trim());
  }
  return result;
}

function inlineParamNames(type: string, category: EntryCategory, catalog: Catalog) {
  const catalogEntry = catalogFor(catalog, category).find((entry) => entry.id === type);
  return (catalogEntry?.params || [])
    .filter((param) => !param.slotCategory)
    .map((param) => param.name)
    .sort((left, right) => right.length - left.length);
}

function escapeRegex(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function makeEntry(path: string, key: string, type: string, category: EntryCategory, params: Record<string, unknown>, slots: Record<string, QxlEntry[]>, extras: Record<string, unknown>, catalog: Catalog): QxlEntry {
  const entry: QxlEntry = { id: `entry:${path}`, key, type, category, params, slots, extras };
  for (const slot of slotDefinitions(category, type, catalogFor(catalog, category).find((item) => item.id === type))) {
    if (!entry.slots[slot.name]) entry.slots[slot.name] = [];
  }
  return entry;
}

function canonicalEntryParams(type: string, category: EntryCategory, params: Record<string, unknown>): Record<string, unknown> {
  const next = { ...params };
  if (category === 'objective' && isMobReferenceComponent(type) && next.id != null && next.mob == null && next.mobs == null) {
    const value = next.id;
    delete next.id;
    if (Array.isArray(value) || isCommaList(value)) next.mobs = listValue(value);
    else next.mob = value;
  }
  if (category === 'action' && isMobReferenceComponent(type) && next.id != null && next.mob == null && next.mobs == null) {
    const value = next.id;
    delete next.id;
    if (Array.isArray(value) || isCommaList(value)) next.mobs = listValue(value);
    else next.mob = value;
  }
  return next;
}

function serializeEntryParams(type: string, category: EntryCategory, params: Record<string, unknown>) {
  return canonicalEntryParams(type, category, params);
}

function isFlatLocationComponent(type: string, category: EntryCategory) {
  return type.toLowerCase() === 'location' && (category === 'condition' || category === 'objective');
}

function isFlatLocationField(type: string, category: EntryCategory, field: string) {
  return isFlatLocationComponent(type, category) && ['x', 'y', 'z', 'w', 'world', 'd', 'randomxz', 'randomy'].includes(field.toLowerCase());
}

function isCommaList(value: unknown) {
  return typeof value === 'string' && value.includes(',');
}

function listValue(value: unknown) {
  if (Array.isArray(value)) return value.map(String).map((part) => part.trim()).filter(Boolean);
  return String(value ?? '').split(',').map((part) => part.trim()).filter(Boolean);
}

function isDeprecatedEntryAlias(type: string, category: EntryCategory, field: string) {
  return field === 'id' && (category === 'action' || category === 'objective') && isMobReferenceComponent(type);
}

function isMobReferenceComponent(type: string) {
  const lower = type.toLowerCase();
  return lower === 'spawn_mob' || lower === 'interact_mob' || lower === 'entity_interact';
}

function serializeQxlDocument(doc: QxlDocument, catalog: Catalog) {
  if (doc.type === 'interactions') {
    const root: Record<string, unknown> = {};
    doc.interactions.forEach((interaction) => {
      root[interaction.id] = { ...interaction.params, ...interaction.extras, objectives: serializeEntries(interaction.objectives, catalog) };
    });
    return stringify(root, { lineWidth: 0 });
  }
  if (doc.type === 'macros') {
    const root: Record<string, unknown> = { ...doc.extras };
    for (const [slot, entries] of Object.entries(doc.rootSlots)) root[slot] = serializeEntries(entries, catalog);
    return stringify(root, { lineWidth: 0 });
  }
  if (doc.type === 'periodicQuests') {
    const root: Record<string, any> = { ...doc.extras };
    for (const [key, value] of Object.entries(doc.metadata)) setPathValue(root, key, value);
    for (const [slot, entries] of Object.entries(doc.rootSlots)) setPathValue(root, slot, serializeEntries(entries, catalog));
    return stringify(root, { lineWidth: 0 });
  }
  if (doc.type === 'globalObjectives') {
    const root: Record<string, unknown> = { ...doc.extras };
    for (const [slot, entries] of Object.entries(doc.rootSlots)) setPathValue(root, slot, serializeEntries(entries, catalog));
    return stringify(root, { lineWidth: 0 });
  }
  const root: Record<string, unknown> = { ...doc.metadata, ...doc.extras };
  Object.entries(doc.rootSlots).forEach(([slot, entries]) => { if (entries.length > 0) root[slot] = serializeEntries(entries, catalog); });
  if (doc.rewards.length > 0) {
    root.rewards = Object.fromEntries(doc.rewards.map((reward) => [reward.threshold, serializeEntries(reward.actions, catalog)]));
  }
  root.stages = Object.fromEntries(doc.stages.map((stage) => [stage.id, serializeStage(stage, catalog)]));
  return stringify(root, { lineWidth: 0 });
}

function serializeStage(stage: QxlStage, catalog: Catalog) {
  const out: Record<string, unknown> = { ...stage.extras };
  if (stage.conditions.length > 0) out.conditions = serializeEntries(stage.conditions, catalog);
  if (stage.onStart.length > 0) out.onStart = serializeEntries(stage.onStart, catalog);
  out.objectives = serializeEntries(stage.objectives, catalog);
  if (stage.onFinish.length > 0) out.onFinish = serializeEntries(stage.onFinish, catalog);
  return out;
}

function serializeEntries(entries: QxlEntry[], catalog: Catalog) {
  return Object.fromEntries(entries.map((entry) => {
    const body: Record<string, unknown> = { type: entry.type, ...serializeEntryParams(entry.type, entry.category, entry.params), ...entry.extras };
    for (const [slot, nested] of Object.entries(entry.slots)) {
      if (nested.length > 0) body[slot] = serializeEntries(nested, catalog);
    }
    return [entry.key || entry.type, body];
  }));
}

function addStage(doc: QxlDocument): Selection {
  const id = nextKey(doc.stages.map((stage) => stage.id), '1');
  doc.stages.push({ id, conditions: [], onStart: [], objectives: [newEntry('impossible', 'objective', catalogStub())], onFinish: [], extras: {} });
  refreshIds(doc);
  return { kind: 'stage', id };
}

function deleteStage(doc: QxlDocument, id: string): Selection {
  doc.stages = doc.stages.filter((stage) => stage.id !== id);
  refreshIds(doc);
  return { kind: 'root' };
}

function renameStage(doc: QxlDocument, oldId: string, newId: string) {
  const stage = doc.stages.find((item) => item.id === oldId);
  if (stage && newId.trim()) stage.id = newId.trim();
  refreshIds(doc);
}

function addInteraction(doc: QxlDocument): Selection {
  const id = nextKey(doc.interactions.map((interaction) => interaction.id), 'interaction');
  doc.interactions.push({ id, params: { world: 'Erethon', x: 0, y: 64, z: 0, radius: 16, repeatable: true }, objectives: [], extras: {} });
  refreshIds(doc);
  return { kind: 'interaction', id };
}

function deleteInteraction(doc: QxlDocument, id: string): Selection {
  doc.interactions = doc.interactions.filter((interaction) => interaction.id !== id);
  refreshIds(doc);
  return { kind: 'root' };
}

function renameInteraction(doc: QxlDocument, oldId: string, newId: string) {
  const interaction = doc.interactions.find((item) => item.id === oldId);
  if (interaction && newId.trim()) interaction.id = newId.trim();
  refreshIds(doc);
}

function addEntryToStage(doc: QxlDocument, stageId: string, slot: keyof QxlStage, type: string, catalog: Catalog): Selection {
  const stage = doc.stages.find((item) => item.id === stageId);
  const entries = stage?.[slot];
  if (!Array.isArray(entries)) return { kind: 'stage', id: stageId };
  const category = slot === 'objectives' ? 'objective' : slot === 'conditions' ? 'condition' : 'action';
  const entry = newEntry(type, category, catalog, entries);
  entries.push(entry);
  refreshIds(doc);
  return { kind: 'entry', id: entry.id };
}

function addEntryToRootSlot(doc: QxlDocument, slot: string, category: EntryCategory, type: string, catalog: Catalog): Selection {
  if (!doc.rootSlots[slot]) doc.rootSlots[slot] = [];
  const entry = newEntry(type, category, catalog, doc.rootSlots[slot]);
  doc.rootSlots[slot].push(entry);
  refreshIds(doc);
  return { kind: 'entry', id: entry.id };
}

function addRewardSlot(doc: QxlDocument) {
  const threshold = nextKey(doc.rewards.map((reward) => reward.threshold), '0');
  doc.rewards.push({ threshold, actions: [] });
}

function renameRewardSlot(doc: QxlDocument, oldThreshold: string, nextThreshold: string) {
  const reward = doc.rewards.find((item) => item.threshold === oldThreshold);
  if (reward && nextThreshold.trim()) reward.threshold = nextThreshold.trim();
  refreshIds(doc);
}

function addEntryToRewardSlot(doc: QxlDocument, threshold: string, type: string, catalog: Catalog): Selection {
  const reward = doc.rewards.find((item) => item.threshold === threshold);
  if (!reward) return { kind: 'root' };
  const entry = newEntry(type, 'action', catalog, reward.actions);
  reward.actions.push(entry);
  refreshIds(doc);
  return { kind: 'entry', id: entry.id };
}

function addEntryToInteraction(doc: QxlDocument, interactionId: string, type: string, catalog: Catalog): Selection {
  const interaction = doc.interactions.find((item) => item.id === interactionId);
  if (!interaction) return { kind: 'interaction', id: interactionId };
  const entry = newEntry(type, 'objective', catalog, interaction.objectives);
  interaction.objectives.push(entry);
  refreshIds(doc);
  return { kind: 'entry', id: entry.id };
}

function addEntryForSelection(doc: QxlDocument, selection: Selection, category: EntryCategory, type: string, catalog: Catalog): Selection | null {
  if (selection.kind === 'stage') {
    const slot = stageSlotFor(category);
    if (!slot) return null;
    if (!doc.stages.some((stage) => stage.id === selection.id)) return null;
    return addEntryToStage(doc, selection.id, slot, type, catalog);
  }
  if (selection.kind === 'entry') {
    const selected = findEntry(doc, selection.id)?.entry;
    if (!selected) return null;
    const slot = firstCompatibleEntrySlot(selected, category, catalog);
    if (!slot) return null;
    return addEntryToEntrySlot(doc, selection.id, slot.name, slot.category, type, catalog);
  }
  if (selection.kind === 'interaction') {
    if (category !== 'objective') return null;
    if (!doc.interactions.some((interaction) => interaction.id === selection.id)) return null;
    return addEntryToInteraction(doc, selection.id, type, catalog);
  }
  const rootSlot = rootSlotNames(doc.type).find((slot) => slot.category === category);
  if (rootSlot) return addEntryToRootSlot(doc, rootSlot.name, rootSlot.category, type, catalog);
  const existingRootSlot = rootSlotsForInspector(doc).find((slot) => slot.category === category);
  if (existingRootSlot) return addEntryToRootSlot(doc, existingRootSlot.name, existingRootSlot.category, type, catalog);
  if (doc.type === 'globalObjectives' && category === 'objective') return addEntryToRootSlot(doc, 'objectives', 'objective', type, catalog);
  if (doc.type === 'macros' && category === 'action') return addEntryToRootSlot(doc, 'new_macro', 'action', type, catalog);
  if (doc.type === 'periodicQuests' && category === 'action') return addEntryToRootSlot(doc, 'daily.completionRewards', 'action', type, catalog);
  return null;
}

function addEntryToTarget(doc: QxlDocument, target: AddTarget, type: string, catalog: Catalog): Selection | null {
  if (target.kind === 'stage' && target.ownerId && target.slot) {
    return addEntryToStage(doc, target.ownerId, target.slot as keyof QxlStage, type, catalog);
  }
  if (target.kind === 'entry' && target.ownerId && target.slot) {
    return addEntryToEntrySlot(doc, target.ownerId, target.slot, target.category, type, catalog);
  }
  if (target.kind === 'interaction' && target.ownerId) {
    return addEntryToInteraction(doc, target.ownerId, type, catalog);
  }
  if (target.kind === 'root' && target.slot) {
    return addEntryToRootSlot(doc, target.slot, target.category, type, catalog);
  }
  if (target.kind === 'reward' && target.threshold) {
    return addEntryToRewardSlot(doc, target.threshold, type, catalog);
  }
  return null;
}

function stageSlotFor(category: EntryCategory): keyof QxlStage | null {
  if (category === 'action') return 'onStart';
  if (category === 'condition') return 'conditions';
  if (category === 'objective') return 'objectives';
  return null;
}

function firstCompatibleEntrySlot(entry: QxlEntry, category: EntryCategory, catalog: Catalog): { name: string; category: EntryCategory } | null {
  const catalogEntry = catalogFor(catalog, entry.category).find((item) => item.id === entry.type);
  const slots = slotDefinitions(entry.category, entry.type, catalogEntry).filter((slot) => slot.category === category);
  const priority = ['conditions', 'runAfter', 'onComplete', 'onFail', 'onSuccess'];
  for (const name of priority) {
    const slot = slots.find((item) => item.name === name);
    if (slot) return slot;
  }
  return slots[0] || null;
}

function addEntryToEntrySlot(doc: QxlDocument, entryId: string, slot: string, category: EntryCategory, type: string, catalog: Catalog): Selection {
  const entry = findEntry(doc, entryId)?.entry;
  if (!entry) return { kind: 'root' };
  if (!entry.slots[slot]) entry.slots[slot] = [];
  const next = newEntry(type, category, catalog, entry.slots[slot]);
  entry.slots[slot].push(next);
  refreshIds(doc);
  return { kind: 'entry', id: next.id };
}

function deleteEntry(doc: QxlDocument, entryId: string): Selection {
  const parent = findEntryParent(doc, entryId);
  if (parent) parent.entries.splice(parent.index, 1);
  refreshIds(doc);
  return { kind: 'root' };
}

function duplicateEntry(doc: QxlDocument, entryId: string) {
  const parent = findEntryParent(doc, entryId);
  if (!parent) return;
  const copy = cloneEntry(parent.entries[parent.index]);
  copy.key = nextKey(parent.entries.map((entry) => entry.key), copy.key);
  parent.entries.splice(parent.index + 1, 0, copy);
  refreshIds(doc);
}

function moveEntry(doc: QxlDocument, entryId: string, direction: number) {
  const parent = findEntryParent(doc, entryId);
  if (!parent) return;
  const target = parent.index + direction;
  if (target < 0 || target >= parent.entries.length) return;
  const [entry] = parent.entries.splice(parent.index, 1);
  parent.entries.splice(target, 0, entry);
  refreshIds(doc);
}

function changeEntryType(doc: QxlDocument, entryId: string, type: string, catalog: Catalog) {
  const entry = findEntry(doc, entryId)?.entry;
  if (!entry) return;
  entry.type = type;
  const catalogEntry = catalogFor(catalog, entry.category).find((item) => item.id === type);
  for (const param of catalogEntry?.params || []) {
    if (!param.slotCategory && param.required && entry.params[param.name] == null) entry.params[param.name] = defaultValue(param);
  }
  for (const slot of slotDefinitions(entry.category, entry.type, catalogEntry)) {
    if (!entry.slots[slot.name]) entry.slots[slot.name] = [];
  }
}

function refreshIds(doc: QxlDocument) {
  doc.stages.forEach((stage) => {
    stage.conditions.forEach((entry) => assignIds(entry, `stages.${stage.id}.conditions.${entry.key}`));
    stage.onStart.forEach((entry) => assignIds(entry, `stages.${stage.id}.onStart.${entry.key}`));
    stage.objectives.forEach((entry) => assignIds(entry, `stages.${stage.id}.objectives.${entry.key}`));
    stage.onFinish.forEach((entry) => assignIds(entry, `stages.${stage.id}.onFinish.${entry.key}`));
  });
  Object.entries(doc.rootSlots).forEach(([slot, entries]) => entries.forEach((entry) => assignIds(entry, `${slot}.${entry.key}`)));
  doc.rewards.forEach((reward) => reward.actions.forEach((entry) => assignIds(entry, `rewards.${reward.threshold}.${entry.key}`)));
  doc.interactions.forEach((interaction) => interaction.objectives.forEach((entry) => assignIds(entry, `interactions.${interaction.id}.objectives.${entry.key}`)));
}

function assignIds(entry: QxlEntry, path: string) {
  entry.id = `entry:${path}`;
  Object.entries(entry.slots).forEach(([slot, entries]) => entries.forEach((nested) => assignIds(nested, `${path}.${slot}.${nested.key}`)));
}

function findEntry(doc: QxlDocument, id: string): { entry: QxlEntry } | null {
  let found: QxlEntry | null = null;
  walkEntries(doc, (entry) => { if (entry.id === id) found = entry; });
  return found ? { entry: found } : null;
}

function findEntryParent(doc: QxlDocument, id: string): { entries: QxlEntry[]; index: number } | null {
  let result: { entries: QxlEntry[]; index: number } | null = null;
  const check = (entries: QxlEntry[]) => {
    const index = entries.findIndex((entry) => entry.id === id);
    if (index >= 0) result = { entries, index };
    entries.forEach((entry) => Object.values(entry.slots).forEach(check));
  };
  doc.stages.forEach((stage) => [stage.conditions, stage.onStart, stage.objectives, stage.onFinish].forEach(check));
  Object.values(doc.rootSlots).forEach(check);
  doc.rewards.forEach((reward) => check(reward.actions));
  doc.interactions.forEach((interaction) => check(interaction.objectives));
  return result;
}

function walkEntries(doc: QxlDocument, visitor: (entry: QxlEntry) => void) {
  const walk = (entries: QxlEntry[]) => entries.forEach((entry) => { visitor(entry); Object.values(entry.slots).forEach(walk); });
  doc.stages.forEach((stage) => [stage.conditions, stage.onStart, stage.objectives, stage.onFinish].forEach(walk));
  Object.values(doc.rootSlots).forEach(walk);
  doc.rewards.forEach((reward) => walk(reward.actions));
  doc.interactions.forEach((interaction) => walk(interaction.objectives));
}

function newEntry(type: string, category: EntryCategory, catalog: Catalog, siblings: QxlEntry[] = []): QxlEntry {
  const catalogEntry = catalogFor(catalog, category).find((entry) => entry.id === type);
  const params: Record<string, unknown> = {};
  for (const param of catalogEntry?.params || []) {
    if (!param.slotCategory && param.required) params[param.name] = defaultValue(param);
  }
  const key = nextKey(siblings.map((entry) => entry.key), type);
  return makeEntry(`new.${key}`, key, type, category, params, {}, {}, catalog);
}

function defaultValue(param: ComponentParam) {
  if (param.defaultValue && param.defaultValue !== ' ') return parseScalar(param.defaultValue.replaceAll('`', ''));
  if (param.editorKind === 'boolean') return false;
  if (param.editorKind === 'number') return 0;
  if (param.editorKind === 'duration' || param.editorKind === 'ticksDuration') return 0;
  if (param.editorKind === 'location') return { w: 'Erethon', x: 0, y: 64, z: 0 };
  if (param.editorKind === 'translatable') return 'en=; de=';
  if (['itemList', 'mobList', 'spellList', 'traitList', 'effectList', 'traitlineList'].includes(String(param.editorKind))) return [];
  if (param.editorKind === 'json') return '{}';
  if (param.editorKind === 'enum' && param.options && param.options.length > 0) return param.options[0];
  return '';
}

function starterDoc(type: string): QxlDocument {
  if (type === 'events') return { type, metadata: { displayName: 'New Event', startLocation: { world: 'Erethon', x: 0, y: 64, z: 0 }, range: 32 }, rootSlots: {}, stages: [{ id: '1', conditions: [], onStart: [], objectives: [], onFinish: [], extras: {} }], interactions: [], rewards: [], extras: {} };
  if (type === 'interactions') return { type, metadata: {}, rootSlots: {}, stages: [], interactions: [{ id: 'new_interaction', params: { world: 'Erethon', x: 0, y: 64, z: 0, radius: 16, repeatable: true }, objectives: [], extras: {} }], rewards: [], extras: {} };
  return { type, metadata: { displayName: 'New Quest', description: '' }, rootSlots: {}, stages: [{ id: '1', conditions: [], onStart: [], objectives: [], onFinish: [], extras: {} }], interactions: [], rewards: [], extras: {} };
}

function starterYaml(type: string, path: string, catalog: Catalog) {
  if (type === 'aetherMobs') {
    const id = idFromEditorPath(path);
    return stringify({ class: 'AetherbaseMob', displayType: 'pig', instancable: false, name: { en: id, de: id }, attributes: { health: 30, speed: 0.2 } });
  }
  if (type === 'hephaestusItems') {
    const id = idFromEditorPath(path);
    return stringify({ key: `erethon:${id}`, baseItem: 'minecraft:stone', name: { en: id, de: id }, category: { en: 'Items', de: 'Items' }, flavour: { en: '', de: '' }, patch: '{\n  "DataVersion": 4438\n}' });
  }
  if (type === 'hephaestusUpgrades') {
    const id = idFromEditorPath(path);
    return stringify({ id, type: 'attribute_modifying', name: { en_us: id, de_de: id }, minimumLevel: 0, attributes: {} });
  }
  if (type === 'hephaestusJobs') {
    const id = path.includes('#') ? path.split('#').pop() || 'new_job' : idFromEditorPath(path);
    return stringify({ id, description: '', maxLevel: 100, block: 'minecraft:bedrock', translations: { 'name.en': id, 'name.de': id, 'description.en': '', 'description.de': '' }, properties: {} });
  }
  if (type === 'hephaestusVanillaRecipes') {
    return stringify({ type: 'shaped', result: { item: 'minecraft:stone', amount: 1 }, pattern: ['   ', '   ', '   '], ingredients: {} });
  }
  if (type === 'hephaestusJobRecipes') {
    const id = path.includes('#') ? path.split('#').pop() || 'new_recipe' : idFromEditorPath(path);
    return stringify({ id, displayName: id, jobId: '', requiredLevel: 0, baseExperience: 0, craftingTime: 100, minRarity: 'COMMON', discoverable: true, ingredients: {}, resultType: 'fixed', result: { itemId: 'minecraft:stone', amount: 1, itemLevel: 0, rarity: 'COMMON' } });
  }
  if (type === 'hephaestusShops') {
    const id = idFromEditorPath(path);
    return stringify({ name: id, items: {} });
  }
  if (type === 'dialogues') {
    return stringify({ sender: { en: 'Speaker', de: 'Sprecher' }, canStartFromNPC: true, stages: { 0: { id: 'greeting', messages: ['en=Hello.;de=Hallo.'], autoNext: false, options: [] } } });
  }
  if (type === 'hecateClasses') {
    const id = idFromEditorPath(path);
    return stringify({ displayName: { en: id, de: id }, description: { en: '', de: '' }, defaultTraitline: '', color: '#ffffff', traitlines: [] });
  }
  if (type === 'hecateTraitlines') {
    return stringify({ class: '', displayName: { en: 'New Traitline', de: 'Neue Traitline' }, description: { en: [], de: [] }, energyColor: '#ffffff', energySymbol: '*', spells: [], defaultSpellSlots: [], innateTraits: [], armorTags: [], weaponTags: [], traitLine: {} });
  }
  if (type === 'spellbookSpells') {
    const id = idFromEditorPath(path);
    return stringify({ class: '', cooldown: 0, name: { en: id, de: id }, description: { en: [], de: [] }, coefficients: {} });
  }
  if (type === 'spellbookTraits') {
    const id = idFromEditorPath(path);
    return stringify({ class: '', name: { en: id, de: id }, description: { en: [], de: [] } });
  }
  if (type === 'spellbookEffects') {
    const id = idFromEditorPath(path);
    return stringify({ class: '', name: { en: id, de: id }, description: { en: [], de: [] } });
  }
  return serializeQxlDocument(starterDoc(type), catalog);
}

function slotDefinitions(category: EntryCategory, type: string, catalogEntry?: ComponentEntry): Array<{ name: string; category: EntryCategory }> {
  const slots: Array<{ name: string; category: EntryCategory }> = [];
  if (category === 'action') slots.push({ name: 'conditions', category: 'condition' }, { name: 'runAfter', category: 'action' });
  if (category === 'condition') slots.push({ name: 'onSuccess', category: 'action' }, { name: 'onFail', category: 'action' });
  if (category === 'objective') slots.push({ name: 'conditions', category: 'condition' }, { name: 'onConditionFail', category: 'action' }, { name: 'onProgress', category: 'action' }, { name: 'onFail', category: 'action' }, { name: 'onComplete', category: 'action' });
  for (const param of catalogEntry?.params || []) {
    if (param.slotCategory && !slots.some((slot) => slot.name === param.name)) slots.push({ name: param.name, category: param.slotCategory });
  }
  if (['delay', 'repeat', 'run_as'].includes(type) && !slots.some((slot) => slot.name === 'actions')) slots.push({ name: 'actions', category: 'action' });
  if (type === 'inverted' && !slots.some((slot) => slot.name === 'conditions')) slots.push({ name: 'conditions', category: 'condition' });
  if (type === 'timer' && !slots.some((slot) => slot.name === 'onExpire')) slots.push({ name: 'onExpire', category: 'action' });
  return slots;
}

function slotNamesFor(entry: QxlEntry, catalog: Catalog) {
  return slotDefinitions(entry.category, entry.type, catalogFor(catalog, entry.category).find((item) => item.id === entry.type));
}

function mergeParams(entry: QxlEntry, catalogEntry?: ComponentEntry) {
  const names = new Set([...(catalogEntry?.params || []).map((param) => param.name), ...Object.keys(entry.params)]);
  return Array.from(names).map((name) => catalogEntry?.params.find((param) => param.name === name) || { name, required: false, defaultValue: '', description: 'Custom parameter', javaType: 'Object', editorKind: 'raw' as EditorKind });
}

function rootFields(type: string, metadata: Record<string, unknown>) {
  const base = type === 'events' ? ['displayName', 'startLocation', 'range', 'cooldown'] : ['displayName', 'description'];
  return Array.from(new Set([...base, ...Object.keys(metadata)]));
}

function rootSlotNames(type: string): Array<{ name: string; category: EntryCategory }> {
  if (type === 'events') return [
    { name: 'startConditions', category: 'condition' },
    { name: 'onStart', category: 'action' },
    { name: 'onUpdate', category: 'action' },
    { name: 'onFinish', category: 'action' }
  ];
  if (type === 'quests') return [
    { name: 'conditions', category: 'condition' },
    { name: 'onStart', category: 'action' },
    { name: 'onFinish', category: 'action' }
  ];
  return [];
}

function rootSlotsForInspector(doc: QxlDocument): Array<{ name: string; category: EntryCategory }> {
  const slots = rootSlotNames(doc.type);
  for (const [name, entries] of Object.entries(doc.rootSlots)) {
    const category = entries[0]?.category || inferSlotCategory(name.split('.').pop() || name) || 'action';
    if (!slots.some((slot) => slot.name === name)) slots.push({ name, category });
  }
  return slots;
}

function catalogFor(catalog: Catalog, category: EntryCategory) {
  if (category === 'action') return catalog.actions;
  if (category === 'condition') return catalog.conditions;
  return catalog.objectives;
}

function normalizeCatalog(catalog: Catalog): Catalog {
  const normalizeParam = (entry: ComponentEntry, param: ComponentParam): ComponentParam => ({ ...param, slotCategory: param.slotCategory || inferSlotCategory(param.name), editorKind: param.editorKind || inferEditorKind(param, entry.id) });
  const actions = withSyntheticActions(catalog.actions).map((entry) => ({ ...entry, params: entry.params.map((param) => normalizeParam(entry, param)) }));
  const conditions = withSyntheticConditions(catalog.conditions).map((entry) => ({ ...entry, params: entry.params.map((param) => normalizeParam(entry, param)) }));
  return {
    actions,
    conditions,
    objectives: catalog.objectives.map((entry) => ({ ...entry, params: entry.params.map((param) => normalizeParam(entry, param)) })),
    variables: catalog.variables
  };
}

function withSyntheticActions(actions: ComponentEntry[]): ComponentEntry[] {
  if (actions.some((entry) => entry.id === 'spawn_mob')) return actions;
  return [...actions, {
    id: 'spawn_mob',
    label: 'spawn_mob',
    description: 'Spawn one or more Aether mobs at an optional QLocation.',
    shortExample: 'spawn_mob: mob=Rossdall_Wolf; amount=4',
    longExample: [],
    providedVariables: [],
    params: [
      syntheticParam('mob', 'Aether mob id. Deprecated YAML field id is accepted and saved as mob.', 'String', 'mob', true),
      syntheticParam('mobs', 'Aether mob ids. Used when deprecated id contains a list.', 'List', 'mobList'),
      syntheticParam('level', 'Optional Aether mob level.', 'int', 'number'),
      syntheticParam('location', 'Optional QLocation. World is optional and defaults to Erethon at runtime.', 'QLocation', 'location'),
      syntheticParam('amount', 'Number of mobs to spawn.', 'int', 'number'),
      syntheticParam('conditions', 'Conditions required before this action can run.', 'List', 'conditionList', false, 'condition'),
      syntheticParam('runAfter', 'Actions to run after this action finishes.', 'Set', 'actionList', false, 'action')
    ]
  }].sort((left, right) => left.id.localeCompare(right.id));
}

function withSyntheticConditions(conditions: ComponentEntry[]): ComponentEntry[] {
  return conditions.map((entry) => {
    let params = entry.params;
    if (['event_state', 'event_range', 'players_in_range'].includes(entry.id)) {
      params = ensureSyntheticParam(params, syntheticParam('id', 'Event id. Alias for event in older configs.', 'String', 'event'));
    }
    if (['active_quest', 'completed_quest', 'stage'].includes(entry.id)) {
      params = ensureSyntheticParam(params, syntheticParam('id', 'Quest id. Alias for quest in older configs.', 'String', 'quest'));
    }
    if (['inventory_contains', 'item_in_hand'].includes(entry.id)) {
      params = ensureSyntheticParam(params, syntheticParam('id', 'Hephaestus item id. Alias for item in older configs.', 'String', 'item'));
    }
    if (entry.id === 'item_in_hand') {
      params = ensureSyntheticParam(params, syntheticParam('item', 'Hephaestus item to check for.', 'String', 'item'));
      params = ensureSyntheticParam(params, syntheticParam('material', 'Vanilla material to check for.', 'String', 'material'));
      params = ensureSyntheticParam(params, syntheticParam('amount', 'Required amount.', 'int', 'number'));
    }
    return params === entry.params ? entry : { ...entry, params };
  });
}

function ensureSyntheticParam(params: ComponentParam[], param: ComponentParam) {
  return params.some((item) => item.name === param.name) ? params : [...params, param];
}

function syntheticParam(name: string, description: string, javaType: string, editorKind: EditorKind, required = false, slotCategory: EntryCategory | '' = ''): ComponentParam {
  return { name, description, javaType, editorKind, required, slotCategory, defaultValue: '', supportsVariables: ['number', 'string', 'duration', 'ticksDuration', 'translatable', 'minimessage'].includes(editorKind) };
}

function typeOptions(types: ContentType[]) {
  const hephaestusIds = ['hephaestusItems', 'hephaestusUpgrades', 'hephaestusJobs', 'hephaestusVanillaRecipes', 'hephaestusJobRecipes', 'hephaestusShops'];
  const groupedTypeIds = ['aetherMobs', 'daedalusModels', ...hephaestusIds, 'hecateClasses', 'hecateTraitlines', 'spellbookSpells', 'spellbookTraits', 'spellbookEffects', 'factionsBuildings', 'translations'];
  const qxl = types.filter((type) => !groupedTypeIds.includes(type.id)).map((type) => ({ value: type.id, label: type.label }));
  const aether = types.filter((type) => ['aetherMobs', 'daedalusModels'].includes(type.id)).map((type) => ({ value: type.id, label: type.id === 'daedalusModels' ? 'Daedalus Models' : type.label }));
  const hephaestus = types.filter((type) => hephaestusIds.includes(type.id)).map((type) => ({ value: type.id, label: type.label.replace(/^Hephaestus\s+/, '') }));
  const hecate = types.filter((type) => ['hecateClasses', 'hecateTraitlines'].includes(type.id)).map((type) => ({ value: type.id, label: type.label }));
  const spellbook = types.filter((type) => ['spellbookSpells', 'spellbookTraits', 'spellbookEffects'].includes(type.id)).map((type) => ({ value: type.id, label: type.label }));
  const factions = types.filter((type) => type.id === 'factionsBuildings').map((type) => ({ value: type.id, label: 'Buildings' }));
  const shared = types.filter((type) => type.id === 'translations').map((type) => ({ value: type.id, label: type.label }));
  return [
    ...(qxl.length ? [{ group: 'QXL', items: qxl }] : []),
    ...(aether.length ? [{ group: 'Aether', items: aether }] : []),
    ...(hephaestus.length ? [{ group: 'Hephaestus', items: hephaestus }] : []),
    ...(factions.length ? [{ group: 'Factions', items: factions }] : []),
    ...(shared.length ? [{ group: 'Shared', items: shared }] : []),
    ...(hecate.length ? [{ group: 'Hecate', items: hecate }] : []),
    ...(spellbook.length ? [{ group: 'Spellbook', items: spellbook }] : [])
  ];
}

function rootFieldKind(field: string): EditorKind | undefined {
  const lower = field.toLowerCase();
  if (lower === 'startlocation' || lower === 'location') return 'location';
  if (lower === 'world') return 'world';
  if (lower === 'display' || lower === 'displayname' || lower === 'displaytext' || lower === 'description') return 'translatable';
  if (lower === 'cooldown') return 'duration';
  if (['range', 'radius', 'x', 'y', 'z'].includes(lower)) return 'number';
  if (lower === 'repeatable' || lower.endsWith('.enabled')) return 'boolean';
  if (lower.endsWith('.count') || lower.endsWith('.lastreset')) return 'number';
  return undefined;
}

function idFromEditorPath(path: string) {
  const file = path.replace(/\\/g, '/').split('/').pop() || 'new_content';
  return file.replace(/\.(ya?ml)$/i, '') || 'new_content';
}

function inferSlotCategory(name: string): EntryCategory | '' {
  if (['actions', 'runAfter', 'onStart', 'onFinish', 'onUpdate', 'onComplete', 'onFail', 'onSuccess', 'onProgress', 'onConditionFail', 'onExpire'].includes(name)) return 'action';
  if (['conditions', 'startConditions'].includes(name)) return 'condition';
  if (name === 'objectives') return 'objective';
  return '';
}

function inferEditorKind(param: ComponentParam, componentId = ''): EditorKind {
  const slot = param.slotCategory || inferSlotCategory(param.name);
  if (slot) return `${slot}List` as EditorKind;
  const type = param.javaType.toLowerCase();
  const name = param.name.toLowerCase();
  if (name === 'display' || name === 'displaytext' || (componentId === 'message' && name === 'message')) return 'translatable';
  if (name === 'id' && ['event_state', 'event_range', 'players_in_range'].includes(componentId)) return 'event';
  if (name === 'id' && ['active_quest', 'completed_quest', 'stage'].includes(componentId)) return 'quest';
  if (name === 'id' && ['inventory_contains', 'item_in_hand'].includes(componentId)) return 'item';
  if (name === 'quest' || name === 'questid') return 'quest';
  if (name === 'event' || name === 'eventid') return 'event';
  if (name === 'id' && (param.description || '').toLowerCase().includes('quest or event')) return 'questOrEvent';
  if (name === 'id' && (param.description || '').toLowerCase().includes('event')) return 'event';
  if (name === 'id' && (param.description || '').toLowerCase().includes('quest')) return 'quest';
  if (name === 'dialogue' || name === 'dialogueid') return 'dialogue';
  if (name === 'shop' || name === 'shopid' || (componentId === 'open_shop' && name === 'id')) return 'shop';
  if (name === 'spell' || name === 'spellid') return 'spell';
  if (name === 'spells' || name === 'spellids') return 'spellList';
  if (name === 'trait' || name === 'traitid') return 'trait';
  if (name === 'traits' || name === 'traitids' || name === 'innatetraits') return 'traitList';
  if (name === 'effect' || name === 'effectid') return 'effect';
  if (name === 'effects' || name === 'effectids') return 'effectList';
  if (name === 'traitline' || name === 'defaulttraitline' || name === 'defaultdiscipline') return 'traitline';
  if (name === 'traitlines' || name === 'disciplines') return 'traitlineList';
  if (name === 'class') return 'hecateClass';
  if (name === 'goal') return 'number';
  if (name === 'scope' || name.startsWith('scope')) return 'enum';
  if (name.includes('location') || name === 'target') return 'location';
  if (name === 'mobs' || name === 'mobids') return 'mobList';
  if (name === 'mob' || name === 'mobid' || (name === 'id' && param.description.toLowerCase().includes('mob'))) return 'mob';
  if (name === 'item' || name === 'block' || name.endsWith('item')) return 'item';
  if (name === 'items' || name === 'blocks' || name.endsWith('items') || name.endsWith('blocks')) return 'itemList';
  if (name === 'world' || name === 'w') return 'world';
  if (name === 'material' || name.endsWith('material')) return 'material';
  if (name === 'cooldown' || name === 'duration') return 'duration';
  if (name === 'fadein' || name === 'fadeout' || name === 'stay') return 'ticksDuration';
  if (param.options && param.options.length > 0) return 'enum';
  if (type === 'boolean') return 'boolean';
  if (['int', 'integer', 'long', 'double', 'float'].includes(type)) return 'number';
  return 'string';
}

function primaryParamName(type: string, category: EntryCategory, catalog: Catalog) {
  return catalogFor(catalog, category).find((entry) => entry.id === type)?.params.find((param) => !param.slotCategory)?.name || type;
}

function collectUsedTypes(doc: QxlDocument) {
  const result = new Set<string>();
  walkEntries(doc, (entry) => result.add(entry.type));
  return result;
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

function parseLooseValue(value: string): unknown {
  try {
    return parseDocument(value).toJS();
  } catch {
    return value;
  }
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function asRecord(value: unknown): Record<string, unknown> {
  return isPlainObject(value) ? value : {};
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

function isCommonParam(key: string) {
  return [
    'display', 'displayText', 'hidden', 'cancel', 'conditions', 'runAfter', 'onSuccess', 'onFail', 'onConditionFail', 'onProgress', 'onComplete',
    'scopeSuccess', 'scopeProgress', 'scopeConditionFail', 'scopeFail', 'scopeComplete', 'optional', 'persistent', 'global', 'goal'
  ].includes(key);
}

function cloneDoc(doc: QxlDocument): QxlDocument {
  return JSON.parse(JSON.stringify(doc));
}

function cloneEntry(entry: QxlEntry): QxlEntry {
  return JSON.parse(JSON.stringify(entry));
}

function catalogStub(): Catalog {
  return { actions: [], conditions: [], objectives: [] };
}

function snippetFor(entry: ComponentEntry) {
  const params = entry.params.filter((param) => param.required && !param.slotCategory).map((param) => `  ${param.name}: ${param.defaultValue || ''}`).join('\n');
  return params ? `${entry.id}:\n  type: ${entry.id}\n${params}` : `${entry.id}:\n  type: ${entry.id}`;
}

export {
  parseQxlDocument,
  parseEntryCollection,
  serializeQxlDocument,
  serializeEntries,
  addStage,
  deleteStage,
  renameStage,
  addInteraction,
  deleteInteraction,
  renameInteraction,
  addEntryToStage,
  addEntryToRootSlot,
  addRewardSlot,
  renameRewardSlot,
  addEntryToRewardSlot,
  addEntryToInteraction,
  addEntryForSelection,
  addEntryToTarget,
  stageSlotFor,
  firstCompatibleEntrySlot,
  addEntryToEntrySlot,
  deleteEntry,
  duplicateEntry,
  moveEntry,
  changeEntryType,
  refreshIds,
  findEntry,
  findEntryParent,
  walkEntries,
  newEntry,
  defaultValue,
  starterDoc,
  starterYaml,
  slotDefinitions,
  slotNamesFor,
  mergeParams,
  rootFields,
  rootSlotNames,
  rootSlotsForInspector,
  catalogFor,
  normalizeCatalog,
  typeOptions,
  rootFieldKind,
  idFromEditorPath,
  inferSlotCategory,
  inferEditorKind,
  primaryParamName,
  collectUsedTypes,
  nextKey,
  parseScalar,
  parseLooseValue,
  isPlainObject,
  asRecord,
  setPathValue,
  isCommonParam,
  cloneDoc,
  cloneEntry,
  catalogStub,
  snippetFor
};
