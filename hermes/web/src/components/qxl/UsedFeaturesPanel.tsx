import { ActionIcon, Badge, Box, Group, Paper, ScrollArea, Stack, Text } from '@mantine/core';
import { IconPlus } from '@tabler/icons-react';
import type { Catalog, ComponentEntry, QxlDocument } from '../../api/types';
import { catalogFor, collectUsedTypes } from '../../model/qxlDocument';
export function UsedFeaturesPanel({ doc, catalog, disabled }: { doc: QxlDocument; catalog: Catalog; disabled: boolean }) {
  const known = new Map<string, ComponentEntry>();
  [...catalog.actions, ...catalog.objectives, ...catalog.conditions].forEach((entry) => known.set(entry.id, entry));
  const used = Array.from(collectUsedTypes(doc)).sort();
  return (
    <ScrollArea h="calc(100vh - 98px)">
      <Stack p="sm">
        <Text size="sm" c="dimmed">Component types detected in this file, including extension-provided or currently undocumented entries.</Text>
        {used.map((id) => {
          const entry = known.get(id);
          return (
            <Paper key={id} p="sm" withBorder>
              <Group justify="space-between" align="start">
                <Box>
                  <Group gap="xs"><Text fw={600}>{id}</Text><Badge size="xs" color={entry ? 'green' : 'yellow'} variant="light">{entry ? 'catalog' : 'custom'}</Badge></Group>
                  <Text size="xs" c="dimmed">{entry?.description || 'Detected from YAML. Available as a generic type block.'}</Text>
                </Box>
                <ActionIcon variant="subtle" disabled={disabled}><IconPlus size={16} /></ActionIcon>
              </Group>
            </Paper>
          );
        })}
        {used.length === 0 && <Text c="dimmed">No component type declarations found.</Text>}
      </Stack>
    </ScrollArea>
  );
}

