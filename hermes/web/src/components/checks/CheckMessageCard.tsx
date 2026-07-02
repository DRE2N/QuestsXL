import { useState } from 'react';
import { Box, Button, Code, Collapse, Group, Paper, Stack, Text } from '@mantine/core';

export type CheckMessage = { title?: string; message: string; location?: string; hint?: string; stacktrace?: string; stackPreview?: string };

export function CheckMessageCard({ message }: { message: CheckMessage }) {
  const [open, setOpen] = useState(false);
  const stacktrace = (message.stacktrace || message.stackPreview || '').trim();
  const passed = /passed|completed/i.test(message.title || '');
  return (
    <Paper p="sm" withBorder>
      <Stack gap={6}>
        <Group justify="space-between" align="start" gap="xs">
          <Box>
            <Text fw={700} c={passed ? 'green' : 'red'}>{message.title || 'Check failed'}</Text>
            {message.location && <Code>{message.location}</Code>}
          </Box>
          {stacktrace && (
            <Button size="compact-xs" variant="subtle" onClick={() => setOpen((current) => !current)}>
              {open ? 'Hide stacktrace' : 'Show stacktrace'}
            </Button>
          )}
        </Group>
        {message.message && <Text size="sm">{message.message}</Text>}
        {message.hint && <Text size="xs" c="dimmed">Hint: {message.hint}</Text>}
        {stacktrace && (
          <Collapse in={open}>
            <Code block>{stacktrace}</Code>
          </Collapse>
        )}
      </Stack>
    </Paper>
  );
}
