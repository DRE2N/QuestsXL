import { useRef, useState } from 'react';
import { Alert, Badge, Box, Button, Code, Group, Paper, Stack, Table, Text, TextInput } from '@mantine/core';
import { IconDownload, IconRefresh, IconTrash, IconUpload } from '@tabler/icons-react';
import { api, apiRaw } from '../../api/client';
import type { ContentItem, DaedalusModelItem } from '../../api/types';
import type { CheckMessage } from '../checks/CheckMessageCard';

export function DaedalusModelsWorkspace({ selectedPath, selectedItem, disabled, onRefreshItems, onOpenPath, onChecks }: {
  selectedPath: string;
  selectedItem?: ContentItem;
  disabled: boolean;
  onRefreshItems: () => Promise<ContentItem[]>;
  onOpenPath: (path: string) => void | Promise<void>;
  onChecks: (checks: CheckMessage[]) => void;
}) {
  const fileInput = useRef<HTMLInputElement | null>(null);
  const [uploadPath, setUploadPath] = useState('');
  const [overwrite, setOverwrite] = useState(false);
  const model = selectedItem as DaedalusModelItem | undefined;

  const refresh = async () => {
    await onRefreshItems();
  };
  const download = async () => {
    if (!selectedPath) return;
    const response = await apiRaw(`/api/files/daedalusModels/download?path=${encodeURIComponent(selectedPath)}`);
    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = selectedPath.split('/').pop() || 'model.bbmodel';
    link.click();
    URL.revokeObjectURL(url);
  };
  const upload = async (file: File | null) => {
    if (!file) return;
    const targetPath = normalizeUploadPath(uploadPath || file.name);
    try {
      const result = await api<{ success?: boolean; valid?: boolean; errors?: CheckMessage[]; path?: string }>(`/api/files/daedalusModels/upload?path=${encodeURIComponent(targetPath)}&overwrite=${overwrite}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/octet-stream' },
        body: await file.arrayBuffer()
      });
      const checks = result.errors?.length
        ? result.errors
        : [{ title: 'Upload complete', message: `${result.path || targetPath} was uploaded and Daedalus was reloaded.`, location: result.path || targetPath }];
      onChecks(checks);
      const loaded = await onRefreshItems();
      const nextPath = result.path || targetPath;
      if (loaded.some((item) => item.path === nextPath)) {
        await onOpenPath(nextPath);
      }
    } catch (error) {
      onChecks([{ title: 'Upload failed', message: error instanceof Error ? error.message : String(error), location: targetPath }]);
    } finally {
      if (fileInput.current) {
        fileInput.current.value = '';
      }
    }
  };
  const validate = async () => {
    if (!selectedPath) return;
    const result = await api<{ valid: boolean; errors?: CheckMessage[] }>(`/api/files/daedalusModels/validate?path=${encodeURIComponent(selectedPath)}`, { method: 'POST', body: '' });
    onChecks(result.errors?.length ? result.errors : [{ title: 'Validation passed', message: 'Daedalus model structure looks valid.', location: selectedPath }]);
  };
  const remove = async () => {
    if (!selectedPath || !confirm(`Delete ${selectedPath}? Hermes will create a backup first.`)) return;
    const result = await api<{ success: boolean; errors?: CheckMessage[] }>('/api/files/daedalusModels/delete', {
      method: 'POST',
      body: JSON.stringify({ path: selectedPath })
    });
    onChecks(result.errors?.length ? result.errors : [{ title: 'Delete complete', message: `${selectedPath} was deleted and Daedalus was reloaded.`, location: selectedPath }]);
    const loaded = await onRefreshItems();
    if (loaded[0]) {
      await onOpenPath(loaded[0].path);
    }
  };

  return (
    <Box p="lg">
      <Group justify="space-between" mb="md">
        <Box>
          <Text fw={700} size="lg">{selectedPath || 'Daedalus models'}</Text>
          <Text c="dimmed" size="sm">Upload, validate, download, rename, and delete Blockbench model files.</Text>
        </Box>
        <Group gap="xs">
          <Badge variant="outline">.bbmodel</Badge>
          {model?.valid === false && <Badge color="red">invalid</Badge>}
        </Group>
      </Group>

      <div className="sectionGrid">
        <Paper className="surfaceCard" withBorder>
          <Text fw={700}>Selected model</Text>
          <Text size="sm" c="dimmed">{selectedPath || 'No model selected'}</Text>
          {model?.validationMessage && <Alert mt="sm" color="red" variant="light">{model.validationMessage}</Alert>}
        </Paper>
        <Paper className="surfaceCard" withBorder>
          <Text fw={700}>Parsed metadata</Text>
          <Text size="sm" c="dimmed">{model?.modelName || model?.loadedName || model?.id || 'Unknown model'}</Text>
          <Text size="xs" c="dimmed">{model?.modelId || model?.loadedId || ''}</Text>
        </Paper>
        <Paper className="surfaceCard" withBorder>
          <Text fw={700}>File</Text>
          <Text size="sm" c="dimmed">{formatBytes(model?.size)} {model?.updatedAt ? `updated ${new Date(model.updatedAt).toLocaleString()}` : ''}</Text>
        </Paper>
      </div>

      <Group mt="md" mb="md">
        <Button variant="light" leftSection={<IconRefresh size={16} />} onClick={refresh}>Refresh</Button>
        <Button variant="light" leftSection={<IconDownload size={16} />} disabled={!selectedPath} onClick={download}>Download</Button>
        <Button variant="light" disabled={!selectedPath} onClick={validate}>Validate</Button>
        <Button color="red" variant="filled" leftSection={<IconTrash size={16} />} disabled={disabled || !selectedPath} onClick={remove}>Delete</Button>
      </Group>

      <Paper p="md" withBorder className="slotBox">
        <Stack gap="sm">
          <Text fw={700}>Upload model</Text>
          <TextInput label="Target path" description="May include folders under Daedalus/models. Must end in .bbmodel." placeholder="folder/model.bbmodel" value={uploadPath} disabled={disabled} onChange={(event) => setUploadPath(event.currentTarget.value)} />
          <Group>
            <Button leftSection={<IconUpload size={16} />} disabled={disabled} onClick={() => fileInput.current?.click()}>Choose .bbmodel</Button>
            <Button variant={overwrite ? 'filled' : 'light'} color={overwrite ? 'orange' : 'gray'} disabled={disabled} onClick={() => setOverwrite((current) => !current)}>{overwrite ? 'Overwrite on' : 'Overwrite off'}</Button>
            <input ref={fileInput} hidden type="file" accept=".bbmodel" onChange={(event) => void upload(event.currentTarget.files?.[0] || null)} />
          </Group>
        </Stack>
      </Paper>

      <Paper p="md" mt="md" withBorder className="slotBox">
        <Text fw={700} mb="sm">Model details</Text>
        <Table withTableBorder withColumnBorders>
          <Table.Tbody>
            <Detail label="Path" value={selectedPath} />
            <Detail label="Name" value={model?.modelName || model?.loadedName} />
            <Detail label="Identifier" value={model?.modelId || model?.loadedId} />
            <Detail label="Format" value={model?.formatVersion} />
            <Detail label="Textures" value={model?.texturesCount} />
            <Detail label="Elements" value={model?.elementsCount} />
            <Detail label="Outliner entries" value={model?.outlinerCount} />
            <Detail label="Animations" value={model?.animationsCount ?? 0} />
          </Table.Tbody>
        </Table>
        <Text size="xs" c="dimmed" mt="sm">Rename and move are available from the file list controls on the left.</Text>
      </Paper>
    </Box>
  );
}

function Detail({ label, value }: { label: string; value: unknown }) {
  return <Table.Tr><Table.Td w={180}><Text size="sm" c="dimmed">{label}</Text></Table.Td><Table.Td><Code>{value == null || value === '' ? '-' : String(value)}</Code></Table.Td></Table.Tr>;
}

function normalizeUploadPath(path: string) {
  const normalized = path.replace(/\\/g, '/').trim();
  return normalized.toLowerCase().endsWith('.bbmodel') ? normalized : `${normalized}.bbmodel`;
}

function formatBytes(bytes?: number) {
  if (!bytes) return '0 B';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
