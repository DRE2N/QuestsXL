import { useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import { Box, Button, Code, Group, MantineProvider, Paper, PasswordInput, Stack, Tabs, Text, TextInput, ThemeIcon } from '@mantine/core';
import { IconSettings } from '@tabler/icons-react';
import { api } from '../../api/client';

type AuthResult = { success: boolean; message: string; token?: string };

export function LoginScreen({ onLogin, error, serverPanel }: { onLogin: () => void; error: string; serverPanel?: ReactNode }) {
  const [playerName, setPlayerName] = useState('');
  const [password, setPassword] = useState('');
  const [setupToken, setSetupToken] = useState('');
  const [setupMessage, setSetupMessage] = useState('');
  const [localError, setLocalError] = useState(error);
  const [busy, setBusy] = useState(false);
  useEffect(() => { setLocalError(error); }, [error]);
  const login = async () => {
    setBusy(true);
    try {
      await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ playerName, password }) });
      await onLogin();
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setBusy(false);
    }
  };
  const startSetup = async () => {
    setBusy(true);
    try {
      const result = await api<AuthResult>('/api/auth/setup/start', { method: 'POST', body: JSON.stringify({ playerName }) });
      setSetupToken(result.token || '');
      setSetupMessage(result.message);
      setLocalError('');
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Setup failed');
    } finally {
      setBusy(false);
    }
  };
  const finishSetup = async () => {
    setBusy(true);
    try {
      await api('/api/auth/setup/finish', { method: 'POST', body: JSON.stringify({ token: setupToken, password }) });
      await onLogin();
    } catch (err) {
      setLocalError(err instanceof Error ? err.message : 'Setup failed');
    } finally {
      setBusy(false);
    }
  };
  return (
    <MantineProvider defaultColorScheme="dark">
      <Box className="loginShell">
        <Paper p="xl" w={serverPanel ? 920 : 380} maw="calc(100vw - 32px)" withBorder>
          <Stack>
            <Group><ThemeIcon color="teal"><IconSettings size={18} /></ThemeIcon><Text fw={700}>Erethon</Text></Group>
            <Tabs defaultValue="login">
              <Tabs.List grow>
                <Tabs.Tab value="login">Log in</Tabs.Tab>
                <Tabs.Tab value="setup">Set password</Tabs.Tab>
                {serverPanel && <Tabs.Tab value="server">Server</Tabs.Tab>}
              </Tabs.List>
              <Tabs.Panel value="login" pt="md">
                <Stack>
                  <TextInput label="Minecraft name" value={playerName} onChange={(event) => setPlayerName(event.currentTarget.value)} />
                  <PasswordInput label="Web password" value={password} onChange={(event) => setPassword(event.currentTarget.value)} />
                  <Button loading={busy} onClick={login}>Log in</Button>
                </Stack>
              </Tabs.Panel>
              <Tabs.Panel value="setup" pt="md">
                <Stack>
                  <TextInput label="Minecraft name" value={playerName} onChange={(event) => setPlayerName(event.currentTarget.value)} />
                  <Button variant="light" loading={busy} onClick={startSetup}>Start setup</Button>
                  {setupMessage && <Text size="sm" c="dimmed">{setupMessage}</Text>}
                  {setupToken && <><Code block>{setupToken}</Code><PasswordInput label="New web password" value={password} onChange={(event) => setPassword(event.currentTarget.value)} /><Button loading={busy} onClick={finishSetup}>Finish setup</Button></>}
                </Stack>
              </Tabs.Panel>
              {serverPanel && <Tabs.Panel value="server" pt="md">{serverPanel}</Tabs.Panel>}
            </Tabs>
            {localError && <Text c="red" size="sm">{localError}</Text>}
          </Stack>
        </Paper>
      </Box>
    </MantineProvider>
  );
}
