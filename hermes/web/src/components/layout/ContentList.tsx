import { useMemo, useState } from 'react';
import { ActionIcon, Badge, Box, Group, Text, Tooltip } from '@mantine/core';
import { IconRestore, IconSettings, IconTrash } from '@tabler/icons-react';
import type { ContentItem } from '../../api/types';

const ROW_HEIGHT = 58;
const OVERSCAN = 8;

export function ContentList({ items, selectedPath, canManageFiles, dirtyPaths = new Set(), onOpen, onRevert, onMove, onDelete }: {
  items: ContentItem[];
  selectedPath: string;
  canManageFiles: boolean;
  dirtyPaths?: Set<string>;
  onOpen: (item: ContentItem) => void;
  onRevert: (item: ContentItem) => void;
  onMove: (item: ContentItem) => void;
  onDelete: (item: ContentItem) => void;
}) {
  const [scrollTop, setScrollTop] = useState(0);
  const [height, setHeight] = useState(0);
  const visible = useMemo(() => {
    const start = Math.max(0, Math.floor(scrollTop / ROW_HEIGHT) - OVERSCAN);
    const count = Math.ceil(height / ROW_HEIGHT) + OVERSCAN * 2;
    const end = Math.min(items.length, start + count);
    return { start, end, rows: items.slice(start, end) };
  }, [height, items, scrollTop]);

  return (
    <Box
      className="contentListViewport"
      onScroll={(event) => setScrollTop(event.currentTarget.scrollTop)}
      ref={(node) => {
        if (node && node.clientHeight !== height) setHeight(node.clientHeight);
      }}
    >
      <div className="contentListSpacer" style={{ height: items.length * ROW_HEIGHT }}>
        {visible.rows.map((item, index) => {
          const active = item.path === selectedPath;
          const dirty = dirtyPaths.has(item.path);
          return (
            <button
              key={item.path}
              type="button"
              className={`contentListRow ${active ? 'active' : ''}`}
              style={{ transform: `translateY(${(visible.start + index) * ROW_HEIGHT}px)` }}
              onClick={() => onOpen(item)}
            >
              <span className="contentListText">
                <Group gap={6} wrap="nowrap">
                  <Text size="sm" fw={active ? 700 : 600} truncate>{item.id}</Text>
                  {dirty && <Badge size="xs" color="yellow" variant="light">edited</Badge>}
                </Group>
                <Text size="xs" c="dimmed" truncate>{item.path}</Text>
              </span>
              {(dirty || canManageFiles) && (
                <Group gap={2} wrap="nowrap" onClick={(event) => event.stopPropagation()}>
                  {dirty && (
                    <Tooltip label="Revert local edits">
                      <ActionIcon size="sm" variant="subtle" color="yellow" onClick={() => onRevert(item)}>
                        <IconRestore size={14} />
                      </ActionIcon>
                    </Tooltip>
                  )}
                  {canManageFiles && (
                    <>
                      <Tooltip label="Rename or move">
                        <ActionIcon size="sm" variant="subtle" onClick={() => onMove(item)}>
                          <IconSettings size={14} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="Delete file">
                        <ActionIcon size="sm" variant="subtle" color="red" onClick={() => onDelete(item)}>
                          <IconTrash size={14} />
                        </ActionIcon>
                      </Tooltip>
                    </>
                  )}
                </Group>
              )}
            </button>
          );
        })}
      </div>
      {items.length === 0 && (
        <Text size="sm" c="dimmed" p="sm">No content found.</Text>
      )}
    </Box>
  );
}
