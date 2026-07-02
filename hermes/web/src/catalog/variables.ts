import type { Catalog, ComponentEntry, EntryCategory, QxlDocument, QxlEntry, Selection, VariableOption } from '../api/types';

export function variablesForQxlSelection(doc: QxlDocument, selection: Selection, catalog: Catalog): VariableOption[] {
  if (selection.kind === 'entry') {
    const entry = findEntryPath(doc, selection.id);
    if (entry) {
      return mergeVariables([
        taggedVariables(catalog.variables?.builtins || [], 'Built-in'),
        contextVariables(entry.ancestors, catalog, 'Context'),
        contextVariables(entry.earlierSiblings, catalog, 'Context'),
        contextVariables([entry.entry], catalog, 'Context'),
        taggedVariables(allProvidedVariables(catalog), 'All known')
      ]);
    }
  }
  return mergeVariables([
    taggedVariables(catalog.variables?.builtins || [], 'Built-in'),
    taggedVariables(allProvidedVariables(catalog), 'All known')
  ]);
}

export function allVariableOptions(catalog: Catalog): VariableOption[] {
  return mergeVariables([
    taggedVariables(catalog.variables?.builtins || [], 'Built-in'),
    taggedVariables(allProvidedVariables(catalog), 'All known')
  ]);
}

export function variableToken(variable: VariableOption) {
  return variable.token || `%${variable.id}%`;
}

function contextVariables(entries: QxlEntry[], catalog: Catalog, source: string): VariableOption[] {
  return entries.flatMap((entry) => taggedVariables(providedVariablesForEntry(entry, catalog), `${source}: ${entry.type}`));
}

function providedVariablesForEntry(entry: QxlEntry, catalog: Catalog): VariableOption[] {
  const catalogEntry = entryCatalogEntry(entry, catalog);
  return catalogEntry?.providedVariables || [];
}

function entryCatalogEntry(entry: QxlEntry, catalog: Catalog): ComponentEntry | undefined {
  return catalogFor(catalog, entry.category).find((item) => item.id === entry.type);
}

function allProvidedVariables(catalog: Catalog): VariableOption[] {
  return [...catalog.actions, ...catalog.conditions, ...catalog.objectives]
    .flatMap((entry) => taggedVariables(entry.providedVariables || [], entry.id));
}

function taggedVariables(variables: VariableOption[], source: string): VariableOption[] {
  return variables.map((variable) => ({ ...variable, source: variable.source && variable.source !== 'Component' ? variable.source : source }));
}

function mergeVariables(groups: VariableOption[][]): VariableOption[] {
  const byKey = new Map<string, VariableOption>();
  for (const variable of groups.flat()) {
    if (!variable.id) continue;
    const token = variableToken(variable);
    if (!byKey.has(token)) byKey.set(token, { ...variable, token });
  }
  return Array.from(byKey.values());
}

function findEntryPath(doc: QxlDocument, id: string): { entry: QxlEntry; ancestors: QxlEntry[]; earlierSiblings: QxlEntry[] } | null {
  const search = (entries: QxlEntry[], ancestors: QxlEntry[]): { entry: QxlEntry; ancestors: QxlEntry[]; earlierSiblings: QxlEntry[] } | null => {
    for (let index = 0; index < entries.length; index++) {
      const entry = entries[index];
      if (entry.id === id) return { entry, ancestors, earlierSiblings: entries.slice(0, index) };
      for (const nested of Object.values(entry.slots)) {
        const found = search(nested, [...ancestors, entry]);
        if (found) return found;
      }
    }
    return null;
  };
  const groups: QxlEntry[][] = [];
  doc.stages.forEach((stage) => groups.push(stage.conditions, stage.onStart, stage.objectives, stage.onFinish));
  Object.values(doc.rootSlots).forEach((entries) => groups.push(entries));
  doc.rewards.forEach((reward) => groups.push(reward.actions));
  doc.interactions.forEach((interaction) => groups.push(interaction.objectives));
  for (const group of groups) {
    const found = search(group, []);
    if (found) return found;
  }
  return null;
}

function catalogFor(catalog: Catalog, category: EntryCategory) {
  if (category === 'action') return catalog.actions;
  if (category === 'condition') return catalog.conditions;
  return catalog.objectives;
}
