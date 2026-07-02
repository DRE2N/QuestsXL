import React from 'react';
import { Badge, Box, Button, Divider, Group, Paper, ScrollArea, Stack, Tabs, Text } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import type { AssetCatalog, Catalog, ContentItem, QxlDocument, QxlEntry, Selection } from '../../api/types';
import { AetherMobWorkspace } from '../aether/AetherMobWorkspace';
import { DaedalusModelsWorkspace } from '../daedalus/DaedalusModelsWorkspace';
import { ExplorablesWorkspace } from '../exploration/ExplorablesWorkspace';
import { FactionsBuildingWorkspace } from '../factions/FactionsBuildingWorkspace';
import { StructuredContentWorkspace } from '../StructuredContentEditor';
import { DialogueWorkspace } from '../dialogue/DialogueWorkspace';
import { RawYamlEditor } from '../fields/RawYamlEditor';
import { TranslationWorkspace } from '../translations/TranslationWorkspace';

type SecondaryContentAccess = {
  load: (type: string, path: string) => Promise<{ yaml: string; baseYaml: string; path: string; type: string }>;
  update: (type: string, path: string, yaml: string) => void;
};

export function VisualWorkspace({ doc, selectedPath, selectedItem, selection, onSelect, canEdit, catalog, yaml, baseYaml, selectedType, onYamlChange, assets, contentAccess, onAddStage, onAddInteraction, onRefreshItems, onOpenPath, onChecks }: {
  doc: QxlDocument;
  selectedPath: string;
  selectedItem?: ContentItem;
  selection: Selection;
  onSelect: (selection: Selection) => void;
  canEdit: boolean;
  catalog: Catalog;
  yaml: string;
  baseYaml: string;
  selectedType: string;
  onYamlChange: (yaml: string) => void;
  assets: AssetCatalog;
  contentAccess?: SecondaryContentAccess;
  onAddStage: () => void;
  onAddInteraction: () => void;
  onRefreshItems: () => Promise<ContentItem[]>;
  onOpenPath: (path: string) => void | Promise<void>;
  onChecks: (checks: Array<{ title?: string; message: string; location?: string; hint?: string; stacktrace?: string; stackPreview?: string }>) => void;
}) {
  if (selectedType === 'dialogues') {
    return <DialogueWorkspace selectedPath={selectedPath} yaml={yaml} catalog={catalog} assets={assets} disabled={!canEdit} onYamlChange={onYamlChange} />;
  }
  if (selectedType === 'aetherMobs') {
    return <AetherMobWorkspace selectedPath={selectedPath} yaml={yaml} catalog={catalog} assets={assets} disabled={!canEdit} onYamlChange={onYamlChange} />;
  }
  if (selectedType === 'factionsBuildings') {
    return <FactionsBuildingWorkspace selectedPath={selectedPath} yaml={yaml} assets={assets} disabled={!canEdit} onYamlChange={onYamlChange} contentAccess={contentAccess} />;
  }
  if (selectedType === 'daedalusModels') {
    return <DaedalusModelsWorkspace selectedPath={selectedPath} selectedItem={selectedItem} disabled={!canEdit} onRefreshItems={onRefreshItems} onOpenPath={onOpenPath} onChecks={onChecks} />;
  }
  if (selectedType === 'translations') {
    return <TranslationWorkspace selectedPath={selectedPath} yaml={yaml} baseYaml={baseYaml} disabled={!canEdit} onYamlChange={onYamlChange} />;
  }
  if (selectedType === 'explorables') {
    return <ExplorablesWorkspace selectedPath={selectedPath} yaml={yaml} assets={assets} disabled={!canEdit} onYamlChange={onYamlChange} contentAccess={contentAccess} />;
  }
  if (['hephaestusItems', 'hephaestusUpgrades', 'hephaestusJobs', 'hephaestusVanillaRecipes', 'hephaestusJobRecipes', 'hephaestusShops', 'hecateClasses', 'hecateTraitlines', 'spellbookSpells', 'spellbookTraits', 'spellbookEffects'].includes(selectedType)) {
    return <StructuredContentWorkspace selectedType={selectedType} selectedPath={selectedPath} yaml={yaml} assets={assets} onChange={onYamlChange} disabled={!canEdit} />;
  }
  const questLike = ['quests', 'events', 'macros', 'periodicQuests', 'globalObjectives'].includes(doc.type);
  return (
    <Box p="lg">
      <Group justify="space-between" mb="md">
        <Box>
          <Text fw={700} size="lg">{selectedPath || 'No content selected'}</Text>
          <Text c="dimmed" size="sm">Editing QComponent</Text>
        </Box>
        <Group gap="xs">
          <Badge variant="outline">{doc.type}</Badge>
          {canEdit && (doc.type === 'quests' || doc.type === 'events') && <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} onClick={onAddStage}>Stage</Button>}
          {canEdit && doc.type === 'interactions' && <Button size="xs" variant="light" leftSection={<IconPlus size={14} />} onClick={onAddInteraction}>Interaction</Button>}
        </Group>
      </Group>
      <Tabs defaultValue="editor" className="centerTabs">
        <Tabs.List>
          <Tabs.Tab value="editor">Editor</Tabs.Tab>
          <Tabs.Tab value="yaml">Raw YAML</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="editor" pt="md">
          <ScrollArea h="calc(100vh - 150px)">
            {doc.parseError && <Paper p="md" withBorder><Text c="red" fw={700}>YAML could not be parsed.</Text><Text size="sm">{doc.parseError}</Text></Paper>}
            {!doc.parseError && !questLike && doc.type !== 'interactions' && <Text c="dimmed">This content type is currently edited through YAML with catalog-assisted insertion.</Text>}
            {!doc.parseError && questLike && (
              <>
                <RootSlotCards doc={doc} selection={selection} onSelect={onSelect} catalog={catalog} />
                {doc.stages.length > 0 && <div className="canvas">
                  {doc.stages.map((stage, index) => (
                    <React.Fragment key={stage.id}>
                      <Paper className={`stageCard clickable ${selection.kind === 'stage' && selection.id === stage.id ? 'selectedNode' : ''}`} withBorder onClick={() => onSelect({ kind: 'stage', id: stage.id })}>
                        <Group justify="space-between" mb="sm">
                          <Text fw={700}>Stage {stage.id}</Text>
                          <Badge size="sm" variant="light">{stage.objectives.length} objectives</Badge>
                        </Group>
                        <EntryList title="Conditions" entries={stage.conditions} selection={selection} onSelect={onSelect} catalog={catalog} />
                        <EntryList title="On start" entries={stage.onStart} selection={selection} onSelect={onSelect} catalog={catalog} />
                        <EntryList title="Objectives" entries={stage.objectives} selection={selection} onSelect={onSelect} catalog={catalog} />
                        <EntryList title="On finish" entries={stage.onFinish} selection={selection} onSelect={onSelect} catalog={catalog} />
                      </Paper>
                      {index < doc.stages.length - 1 && <div className="connector" />}
                    </React.Fragment>
                  ))}
                </div>}
              </>
            )}
            {!doc.parseError && doc.type === 'interactions' && (
              <div className="interactionGrid">
                {doc.interactions.map((interaction) => (
                  <Paper key={interaction.id} className={`interactionCard clickable ${selection.kind === 'interaction' && selection.id === interaction.id ? 'selectedNode' : ''}`} withBorder onClick={() => onSelect({ kind: 'interaction', id: interaction.id })}>
                    <Group justify="space-between" mb="sm">
                      <Text fw={700}>{interaction.id}</Text>
                      <Badge size="sm" variant="light">{interaction.objectives.length} objectives</Badge>
                    </Group>
                    <Text size="xs" c="dimmed">Radius {String(interaction.params.radius ?? 32)} at {String(interaction.params.world ?? 'Erethon')}</Text>
                    <Divider my="sm" />
                    <EntryList title="Objectives" entries={interaction.objectives} selection={selection} onSelect={onSelect} catalog={catalog} />
                  </Paper>
                ))}
                {doc.interactions.length === 0 && <Text c="dimmed">No interactions yet.</Text>}
              </div>
            )}
          </ScrollArea>
        </Tabs.Panel>
        <Tabs.Panel value="yaml" pt="md">
          <ScrollArea h="calc(100vh - 150px)">
            <RawYamlEditor value={yaml} onChange={onYamlChange} minRows={32} disabled={!canEdit} />
          </ScrollArea>
        </Tabs.Panel>
      </Tabs>
    </Box>
  );
}

function RootSlotCards({ doc, selection, onSelect, catalog }: { doc: QxlDocument; selection: Selection; onSelect: (selection: Selection) => void; catalog: Catalog }) {
  const rootSlots = Object.entries(doc.rootSlots).filter(([, entries]) => entries.length > 0);
  return (
    <div className="sectionGrid">
      <Paper className={`surfaceCard clickable ${selection.kind === 'root' ? 'selectedNode' : ''}`} withBorder onClick={() => onSelect({ kind: 'root' })}>
        <Text fw={700}>Metadata</Text>
        <Text size="sm" c="dimmed">{Object.keys(doc.metadata).length} editable fields</Text>
      </Paper>
      {doc.rewards.map((reward) => (
        <Paper key={`reward-${reward.threshold}`} className="surfaceCard" withBorder>
          <Group justify="space-between"><Text fw={700}>Reward {reward.threshold}</Text><Badge size="sm">{reward.actions.length}</Badge></Group>
          <EntryList title="Actions" entries={reward.actions} selection={selection} onSelect={onSelect} catalog={catalog} />
        </Paper>
      ))}
      {rootSlots.map(([name, entries]) => (
        <Paper key={name} className="surfaceCard" withBorder>
          <Group justify="space-between"><Text fw={700}>{name}</Text><Badge size="sm">{entries.length}</Badge></Group>
          <EntryList title="Entries" entries={entries} selection={selection} onSelect={onSelect} catalog={catalog} />
        </Paper>
      ))}
    </div>
  );
}

function EntryList({ title, entries, selection, onSelect, catalog }: { title: string; entries: QxlEntry[]; selection: Selection; onSelect: (selection: Selection) => void; catalog: Catalog }) {
  if (entries.length === 0) return null;
  return (
    <Box mb={8}>
      <Text size="xs" c="dimmed" mb={4}>{title}</Text>
      <Stack gap={5}>
        {entries.map((entry) => <EntryNode key={entry.id} entry={entry} selection={selection} onSelect={onSelect} catalog={catalog} />)}
      </Stack>
    </Box>
  );
}

function EntryNode({ entry, selection, onSelect, catalog }: { entry: QxlEntry; selection: Selection; onSelect: (selection: Selection) => void; catalog: Catalog }) {
  const populatedSlots = orderedSlots(entry).filter(([, values]) => values.length > 0);
  return (
    <Box className={populatedSlots.length > 0 ? 'entryNode hasNestedEntries' : 'entryNode'}>
      <button type="button" className={`entryButton ${entry.category} ${selection.kind === 'entry' && selection.id === entry.id ? 'selectedNode' : ''}`} onClick={(event) => { event.stopPropagation(); onSelect({ kind: 'entry', id: entry.id }); }}>
        <span>{entry.key}</span>
        <small>{entry.type}</small>
      </button>
      {populatedSlots.length > 0 && (
        <div className="nestedEntryLanes" onClick={(event) => event.stopPropagation()}>
          {populatedSlots.map(([slot, nested]) => (
            <div key={slot} className={`nestedEntryLane ${slot === 'actions' ? 'wrapperActions' : ''}`}>
              <EntryList title={slot} entries={nested} selection={selection} onSelect={onSelect} catalog={catalog} />
            </div>
          ))}
        </div>
      )}
    </Box>
  );
}

function orderedSlots(entry: QxlEntry) {
  const priority = ['actions', 'conditions', 'runAfter', 'onComplete', 'onFail', 'onSuccess', 'onProgress', 'onConditionFail', 'onExpire'];
  return Object.entries(entry.slots).sort(([left], [right]) => {
    const leftIndex = priority.indexOf(left);
    const rightIndex = priority.indexOf(right);
    if (leftIndex >= 0 || rightIndex >= 0) return (leftIndex >= 0 ? leftIndex : priority.length) - (rightIndex >= 0 ? rightIndex : priority.length);
    return left.localeCompare(right);
  });
}
