import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ActionIcon,
  AppShell,
  Badge,
  Box,
  Button,
  Checkbox,
  Code,
  Collapse,
  Divider,
  Group,
  MantineProvider,
  Modal,
  Paper,
  PasswordInput,
  Select,
  Stack,
  Tabs,
  Text,
  TextInput,
  ThemeIcon,
  Tooltip
} from '@mantine/core';
import {
  IconAlertTriangle,
  IconCircleCheck,
  IconDeviceFloppy,
  IconEye,
  IconGitBranch,
  IconPlus,
  IconReload,
  IconSearch,
  IconSettings,
  IconUpload
} from '@tabler/icons-react';
import { api } from './api/client';
import type { AssetCatalog, Catalog, ComponentEntry, ComponentParam, ContentItem, ContentType, EditorKind, EntryCategory, QxlDocument, QxlEntry, QxlInteraction, QxlStage, RewardSlot, Selection, VariableOption, WebUser } from './api/types';
import type { AddTarget } from './model/qxlDocument';
import { normalizeCatalog, typeOptions } from './catalog/normalizeCatalog';
import { readRouteFromUrl, writeRouteToUrl } from './routing/urlState';
import {
  addEntryForSelection,
  addEntryToTarget,
  addInteraction,
  addStage,
  cloneDoc,
  parseQxlDocument,
  serializeQxlDocument,
  snippetFor,
  starterYaml
} from './model/qxlDocument';
import { LoginScreen } from './components/auth/LoginScreen';
import { CheckMessageCard, type CheckMessage } from './components/checks/CheckMessageCard';
import { Dashboard } from './components/dashboard/Dashboard';
import { ContentList } from './components/layout/ContentList';
import { CatalogPanel } from './components/qxl/CatalogPanel';
import { Inspector } from './components/qxl/Inspector';
import { ServerControlPage } from './components/server/ServerControlPage';
import { UsedFeaturesPanel } from './components/qxl/UsedFeaturesPanel';
import { VisualWorkspace } from './components/qxl/VisualWorkspace';

const EMPTY_ASSETS: AssetCatalog = { quests: [], events: [], items: [], mobs: [], dialogues: [], hecateClasses: [], hecateTraitlines: [], spellbookSpells: [], spellbookTraits: [], spellbookEffects: [], entityTypes: [], worlds: [], materials: [], attributes: [] };

type GitDirtyFile = { path: string; livePath?: string; deleted?: boolean; updatedAt?: string };
type GitStatus = {
  enabled: boolean;
  configured: boolean;
  repoUrl: string;
  branch: string;
  authorName: string;
  authorEmail: string;
  clonePath: string;
  dirtyFiles: GitDirtyFile[];
  lastPushAt?: string;
  lastCommit?: string;
  lastError?: string;
  errors?: CheckMessage[];
  warnings?: CheckMessage[];
};
type ContentStatus = 'clean' | 'staged' | 'invalid' | 'published';
type EditBuffer = {
  type: string;
  path: string;
  yaml: string;
  baseYaml: string;
  dirty: boolean;
  status: ContentStatus;
  messages: CheckMessage[];
  lastTouched: number;
};
type BatchPublishFileResult = { type: string; path: string; success: boolean; errors: CheckMessage[] };
type BatchPublishResult = {
  success: boolean;
  files: BatchPublishFileResult[];
  reloads: Array<{ plugin: string; success: boolean; errors: CheckMessage[] }>;
  errors: CheckMessage[];
};

const bufferKey = (type: string, path: string) => `${type}:${path}`;
const isBufferChanged = (buffer: EditBuffer) => buffer.dirty;

export default function App() {
  const initialRoute = useMemo(readRouteFromUrl, []);
  const [me, setMe] = useState<WebUser | null>(null);
  const [authError, setAuthError] = useState('');
  const [types, setTypes] = useState<ContentType[]>([]);
  const [items, setItems] = useState<ContentItem[]>([]);
  const [catalog, setCatalog] = useState<Catalog>({ actions: [], conditions: [], objectives: [] });
  const [assets, setAssets] = useState<AssetCatalog>(EMPTY_ASSETS);
  const [selectedType, setSelectedType] = useState(initialRoute.type || 'quests');
  const [selectedPath, setSelectedPath] = useState(initialRoute.path || '');
  const [yaml, setYaml] = useState('');
  const [buffers, setBuffers] = useState<Record<string, EditBuffer>>({});
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState<ContentStatus>('clean');
  const [messages, setMessages] = useState<CheckMessage[]>([]);
  const [newOpen, setNewOpen] = useState(false);
  const [users, setUsers] = useState<WebUser[]>([]);
  const [selection, setSelection] = useState<Selection>({ kind: 'root' });
  const [asideTab, setAsideTab] = useState('inspect');
  const [moveTarget, setMoveTarget] = useState<ContentItem | null>(null);
  const [movePath, setMovePath] = useState('');
  const [deleteTarget, setDeleteTarget] = useState<ContentItem | null>(null);
  const [dashboardOpen, setDashboardOpen] = useState(!initialRoute.view && !initialRoute.type && !initialRoute.path);
  const [serverOpen, setServerOpen] = useState(initialRoute.view === 'server');
  const [gitOpen, setGitOpen] = useState(false);
  const [usersOpen, setUsersOpen] = useState(false);
  const [publishOpen, setPublishOpen] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [gitStatus, setGitStatus] = useState<GitStatus | null>(null);
  const daedalusUploadInput = useRef<HTMLInputElement | null>(null);
  const canEdit = me?.role === 'EDITOR' || me?.role === 'ADMIN';
  const canManageFiles = canEdit && Boolean(types.find((type) => type.id === selectedType)?.directory);
  const changedBuffers = useMemo(() => Object.values(buffers).filter((buffer) => buffer.type !== 'daedalusModels' && isBufferChanged(buffer)), [buffers]);
  const dirtyPaths = useMemo(() => new Set(changedBuffers.filter((buffer) => buffer.type === selectedType).map((buffer) => buffer.path)), [changedBuffers, selectedType]);
  const changedCount = changedBuffers.length;

  const loadSession = async () => {
    try {
      const user = await api<WebUser>('/api/me');
      setMe(user);
      setAuthError('');
    } catch (error) {
      setMe(null);
      setAuthError(error instanceof Error ? error.message : 'Not authenticated');
      return;
    }
    const bootMessages: Array<{ title?: string; message: string; location?: string }> = [];
    try {
      setTypes(await api<ContentType[]>('/api/content/types'));
    } catch (error) {
      bootMessages.push({ title: 'Content types unavailable', message: error instanceof Error ? error.message : 'Could not load content types.' });
    }
    try {
      setCatalog(normalizeCatalog(await api<Catalog>('/api/catalog')));
    } catch (error) {
      bootMessages.push({ title: 'QXL catalog unavailable', message: error instanceof Error ? error.message : 'Could not load QXL component metadata.' });
    }
    try {
      setAssets(await api<AssetCatalog>('/api/assets'));
    } catch (error) {
      setAssets(EMPTY_ASSETS);
      bootMessages.push({ title: 'Asset catalog unavailable', message: error instanceof Error ? error.message : 'Could not load designer picker assets.' });
    }
    if (bootMessages.length) {
      setMessages(bootMessages);
    }
  };

  const loadGitStatus = async () => {
    try {
      const loaded = await api<GitStatus>('/api/git/status');
      setGitStatus(loaded);
      return loaded;
    } catch {
      return null;
    }
  };

  const refreshItems = async (type = selectedType) => {
    if (!me) return [];
    const loaded = type === 'daedalusModels' ? await api<ContentItem[]>('/api/files/daedalusModels') : await api<ContentItem[]>(`/api/content/${type}`);
    setItems(loaded);
    return loaded;
  };

  const updateCurrentYaml = (next: string, nextStatus: ContentStatus = 'staged') => {
    setYaml(next);
    setStatus(nextStatus);
    if (!selectedPath || selectedType === 'daedalusModels') return;
    const key = bufferKey(selectedType, selectedPath);
    setBuffers((current) => {
      const existing = current[key];
      return {
        ...current,
        [key]: {
          type: selectedType,
          path: selectedPath,
          yaml: next,
          baseYaml: existing?.baseYaml ?? '',
          dirty: true,
          status: nextStatus,
          messages: existing?.messages ?? [],
          lastTouched: Date.now()
        }
      };
    });
  };

  const loadSecondaryContent = async (type: string, path: string) => {
    const key = bufferKey(type, path);
    const existing = buffers[key];
    if (existing) {
      return { type, path, yaml: existing.yaml, baseYaml: existing.baseYaml };
    }
    const data = await api<{ yaml: string; draft?: { exists: boolean; yaml?: string } }>(`/api/content/${type}/read?path=${encodeURIComponent(path)}`);
    const nextYaml = data.draft?.exists && data.draft.yaml != null ? data.draft.yaml : data.yaml;
    const nextStatus: ContentStatus = data.draft?.exists ? 'staged' : 'clean';
    setBuffers((current) => current[key] ? current : ({
      ...current,
      [key]: {
        type,
        path,
        yaml: nextYaml,
        baseYaml: data.yaml,
        dirty: false,
        status: nextStatus,
        messages: [],
        lastTouched: Date.now()
      }
    }));
    return { type, path, yaml: nextYaml, baseYaml: data.yaml };
  };

  const updateSecondaryContent = (type: string, path: string, next: string, nextStatus: ContentStatus = 'staged') => {
    if (selectedType === type && selectedPath === path) {
      setYaml(next);
      setStatus(nextStatus);
    }
    const key = bufferKey(type, path);
    setBuffers((current) => {
      const existing = current[key];
      return {
        ...current,
        [key]: {
          type,
          path,
          yaml: next,
          baseYaml: existing?.baseYaml ?? '',
          dirty: true,
          status: nextStatus,
          messages: existing?.messages ?? [],
          lastTouched: Date.now()
        }
      };
    });
  };

  const loadItems = async (type = selectedType) => {
    if (!me) return;
    const loaded = await refreshItems(type);
    const route = readRouteFromUrl();
    const preferredPath = route.type === type ? route.path || selectedPath : selectedPath;
    const fallbackPath = loaded[0]?.path || '';
    const nextPath = preferredPath && loaded.some((item) => item.path === preferredPath) ? preferredPath : fallbackPath;
    if (nextPath) {
      try {
        await openItem(type, nextPath, true, !route.path && !selectedPath);
      } catch (error) {
        if (fallbackPath && fallbackPath !== nextPath) await openItem(type, fallbackPath, true, !route.path);
        else throw error;
      }
    } else {
      setSelectedPath('');
      setYaml('');
      setStatus('clean');
      writeRouteToUrl(type, '', true);
    }
  };

  const openItem = async (type: string, path: string, replaceUrl = false, skipUrl = false) => {
    if (type === 'daedalusModels') {
      setSelectedType(type);
      setSelectedPath(path);
      setYaml('');
      setStatus('clean');
      setMessages([]);
      setSelection({ kind: 'root' });
      if (!skipUrl) {
        writeRouteToUrl(type, path, replaceUrl);
      }
      return;
    }
    const key = bufferKey(type, path);
    const existing = buffers[key];
    if (existing) {
      setSelectedType(type);
      setSelectedPath(path);
      setYaml(existing.yaml);
      setStatus(existing.status);
      setMessages(existing.messages || []);
      setSelection({ kind: 'root' });
      if (!skipUrl) {
        writeRouteToUrl(type, path, replaceUrl);
      }
      return;
    }
    const data = await api<{ yaml: string; draft?: { exists: boolean; yaml?: string } }>(`/api/content/${type}/read?path=${encodeURIComponent(path)}`);
    const nextYaml = data.draft?.exists && data.draft.yaml != null ? data.draft.yaml : data.yaml;
    const nextStatus: ContentStatus = data.draft?.exists ? 'staged' : 'clean';
    setSelectedType(type);
    setSelectedPath(path);
    setYaml(nextYaml);
    setStatus(nextStatus);
    setMessages([]);
    setSelection({ kind: 'root' });
    setBuffers((current) => ({
      ...current,
      [key]: {
        type,
        path,
        yaml: nextYaml,
        baseYaml: data.yaml,
        dirty: false,
        status: nextStatus,
        messages: [],
        lastTouched: Date.now()
      }
    }));
    if (!skipUrl) {
      writeRouteToUrl(type, path, replaceUrl);
    }
  };

  useEffect(() => { loadSession(); }, []);
  useEffect(() => {
    if (!me) return;
    loadGitStatus().catch(console.error);
    if (!dashboardOpen && !serverOpen) loadItems(selectedType).catch(console.error);
  }, [me, selectedType, dashboardOpen, serverOpen]);
  useEffect(() => {
    if (!me) return;
    const onPopState = () => {
      const route = readRouteFromUrl();
      if (!route.type && !route.path) {
        setServerOpen(route.view === 'server');
        setDashboardOpen(route.view !== 'server');
        setSelectedPath('');
        setSelection({ kind: 'root' });
        return;
      }
      const nextType = route.type || 'quests';
      setServerOpen(false);
      setDashboardOpen(false);
      setSelectedType(nextType);
      setSelectedPath(route.path || '');
      setSelection({ kind: 'root' });
      if (nextType === selectedType) {
        loadItems(nextType).catch(console.error);
      }
    };
    window.addEventListener('popstate', onPopState);
    return () => window.removeEventListener('popstate', onPopState);
  }, [me, selectedType, selectedPath]);

  const doc = useMemo(() => parseQxlDocument(yaml, selectedType, catalog), [yaml, selectedType, catalog]);
  const filteredItems = items.filter((item) => `${item.id} ${item.path}`.toLowerCase().includes(search.toLowerCase()));
  const editableVisual = ['quests', 'events', 'interactions', 'macros', 'periodicQuests', 'globalObjectives'].includes(selectedType) && !doc.parseError;
  const structuredContent = ['aetherMobs', 'hephaestusItems', 'hephaestusUpgrades', 'hephaestusJobs', 'hephaestusVanillaRecipes', 'hephaestusJobRecipes', 'hephaestusShops', 'dialogues', 'hecateClasses', 'hecateTraitlines', 'spellbookSpells', 'spellbookTraits', 'spellbookEffects', 'factionsBuildings', 'daedalusModels', 'translations', 'explorables'].includes(selectedType);
  const selectedItem = items.find((item) => item.path === selectedPath);
  const isDaedalus = selectedType === 'daedalusModels';
  const currentBuffer = selectedPath ? buffers[bufferKey(selectedType, selectedPath)] : undefined;
  const baseYaml = currentBuffer?.baseYaml ?? yaml;

  const commitDoc = (next: QxlDocument, nextSelection = selection) => {
    updateCurrentYaml(serializeQxlDocument(next, catalog));
    setSelection(nextSelection);
  };

  const mutateDoc = (mutator: (draft: QxlDocument) => Selection | void) => {
    const draft = cloneDoc(doc);
    const nextSelection = mutator(draft) || selection;
    commitDoc(draft, nextSelection);
  };

  const addFromCatalog = (category: EntryCategory, entry: ComponentEntry, target?: AddTarget) => {
    if (editableVisual) {
      const draft = cloneDoc(doc);
      const nextSelection = target ? addEntryToTarget(draft, target, entry.id, catalog) : addEntryForSelection(draft, selection, category, entry.id, catalog);
      if (nextSelection) {
        commitDoc(draft, nextSelection);
        setAsideTab('inspect');
        return;
      }
    }
    updateCurrentYaml(`${yaml.trimEnd()}\n${snippetFor(entry)}\n`);
  };

  const saveDraft = async () => {
    if (isDaedalus) return;
    const targets = changedBuffers;
    if (targets.length === 0) {
      setMessages([{ title: 'No changes to save', message: 'Edit some files!', location: selectedPath }]);
      return;
    }
    const allMessages: CheckMessage[] = [];
    for (const target of targets) {
      const result = await api<{ valid: boolean; errors: CheckMessage[] }>(`/api/content/${target.type}/draft`, {
        method: 'POST',
        body: JSON.stringify({ path: target.path, yaml: target.yaml })
      });
      const nextStatus: ContentStatus = result.valid ? 'staged' : 'invalid';
      const targetMessages = (result.errors || []).map((message) => ({ ...message, location: message.location || target.path }));
      allMessages.push(...targetMessages);
      setBuffers((current) => {
        const key = bufferKey(target.type, target.path);
        const existing = current[key] || target;
        return { ...current, [key]: { ...existing, status: nextStatus, messages: targetMessages, lastTouched: Date.now() } };
      });
      if (target.type === selectedType && target.path === selectedPath) {
        setStatus(nextStatus);
        setMessages(targetMessages);
      }
    }
    if (targets.length > 1 || allMessages.length > 0) {
      setMessages(allMessages.length > 0 ? allMessages : [{ title: 'Drafts saved', message: `${targets.length} changed file${targets.length === 1 ? '' : 's'} saved as drafts.` }]);
    }
  };

  const validate = async () => {
    if (isDaedalus) {
      const result = await api<{ valid: boolean; errors: CheckMessage[] }>(`/api/files/daedalusModels/validate?path=${encodeURIComponent(selectedPath)}`, { method: 'POST', body: '' });
      const checks = result.errors?.length ? result.errors : [{ title: 'Validation passed', message: 'Daedalus model structure looks valid.', location: selectedPath }];
      setMessages(checks);
      setStatus(result.valid ? 'clean' : 'invalid');
      return;
    }
    const result = await api<{ valid: boolean; errors: CheckMessage[]; warnings?: CheckMessage[] }>(`/api/content/${selectedType}/validate`, {
      method: 'POST',
      body: JSON.stringify({ path: selectedPath, yaml })
    });
    const checks = [...(result.errors || []), ...(result.warnings || [])];
    setMessages(checks.length > 0 ? checks : [{ title: 'Validation passed', message: 'No YAML or content validation issues were reported.', location: selectedPath }]);
    const nextStatus: ContentStatus = result.valid ? 'staged' : 'invalid';
    setStatus(nextStatus);
    if (selectedPath) {
      const key = bufferKey(selectedType, selectedPath);
      setBuffers((current) => {
        const existing = current[key];
        if (!existing) return current;
        return { ...current, [key]: { ...existing, status: nextStatus, messages: checks, lastTouched: Date.now() } };
      });
    }
    setAsideTab('errors');
  };

  const openPublish = () => {
    if (isDaedalus) {
      void reloadDaedalusModels();
      return;
    }
    if (changedBuffers.length === 0) {
      setMessages([{ title: 'No changes to publish', message: 'Edit some files!', location: selectedPath }]);
      setAsideTab('errors');
      return;
    }
    setPublishOpen(true);
  };

  const reloadDaedalusModels = async () => {
      try {
        const result = await api<{ success: boolean; errors?: CheckMessage[] }>('/api/files/daedalusModels/reload', { method: 'POST', body: '' });
        const checks = result.errors?.length ? result.errors : [{ title: 'Reload complete', message: 'Daedalus models were reloaded.', location: 'Daedalus' }];
        setMessages(checks);
        setStatus(result.success && (result.errors || []).length === 0 ? 'published' : 'invalid');
        await refreshItems(selectedType);
        await loadGitStatus();
      } catch (error) {
        setMessages([{ title: 'Reload failed', message: error instanceof Error ? error.message : String(error), location: 'Daedalus' }]);
        setStatus('invalid');
      }
      return;
  };

  const publishChangedBuffers = async () => {
    const targets = changedBuffers;
    if (targets.length === 0) {
      setPublishOpen(false);
      setMessages([{ title: 'No changes to publish', message: 'Edit some files!', location: selectedPath }]);
      return;
    }
    setPublishing(true);
    const checks: CheckMessage[] = [];
    try {
      const result = await api<BatchPublishResult>('/api/content/batch/publish', {
        method: 'POST',
        body: JSON.stringify({ items: targets.map((target) => ({ type: target.type, path: target.path, yaml: target.yaml })) })
      });
      const byKey = new Map((result.files || []).map((file) => [bufferKey(file.type, file.path), file]));
      (result.files || []).forEach((file) => checks.push(...(file.errors || []).map((message) => ({ ...message, location: message.location || file.path }))));
      (result.reloads || []).forEach((reload) => checks.push(...(reload.errors || []).map((message) => ({ ...message, location: message.location || reload.plugin }))));
      setBuffers((current) => {
        const next = { ...current };
        for (const target of targets) {
          const key = bufferKey(target.type, target.path);
          const existing = next[key] || target;
          const file = byKey.get(key);
          if (!file) continue;
          const targetChecks = (file.errors || []).map((message) => ({ ...message, location: message.location || target.path }));
          next[key] = {
            ...existing,
            baseYaml: file.success ? existing.yaml : existing.baseYaml,
            dirty: file.success ? false : existing.dirty,
            status: file.success ? 'published' : 'invalid',
            messages: targetChecks,
            lastTouched: Date.now()
          };
        }
        return next;
      });
      const selectedResult = selectedPath ? byKey.get(bufferKey(selectedType, selectedPath)) : undefined;
      if (selectedResult) {
        setStatus(selectedResult.success ? 'published' : 'invalid');
        setMessages((selectedResult.errors || []).map((message) => ({ ...message, location: message.location || selectedPath })));
      }
      setMessages(checks.length > 0 ? checks : [{ title: 'Publish complete', message: `${targets.length} file${targets.length === 1 ? '' : 's'} published successfully.` }]);
      setPublishOpen(false);
      await loadGitStatus();
      if (checks.length > 0 || !result.success) {
        setAsideTab('errors');
      }
      await refreshItems(selectedType);
    } finally {
      setPublishing(false);
    }
  };

  const beginMove = (item: ContentItem) => {
    setMoveTarget(item);
    setMovePath(item.path);
  };

  const moveContent = async () => {
    if (!moveTarget || !movePath.trim()) return;
    const result = await api<{ success: boolean; path?: string; errors?: CheckMessage[] }>(isDaedalus ? '/api/files/daedalusModels/move' : `/api/content/${selectedType}/move`, {
      method: 'POST',
      body: JSON.stringify({ path: moveTarget.path, nextPath: movePath.trim() })
    });
    setMessages(result.errors || []);
    setStatus(result.success ? 'published' : 'invalid');
    if (!result.success) return;
    await loadGitStatus();
    const nextPath = result.path || movePath.trim();
    if (!isDaedalus) {
      setBuffers((current) => {
        const previousKey = bufferKey(selectedType, moveTarget.path);
        const nextKey = bufferKey(selectedType, nextPath);
        const existing = current[previousKey];
        if (!existing) return current;
        const next = { ...current };
        delete next[previousKey];
        next[nextKey] = { ...existing, path: nextPath, type: selectedType, lastTouched: Date.now() };
        return next;
      });
    }
    setMoveTarget(null);
    setMovePath('');
    await refreshItems(selectedType);
    await openItem(selectedType, nextPath);
  };

  const deleteContent = async () => {
    if (!deleteTarget) return;
    const removedPath = deleteTarget.path;
    const result = await api<{ success: boolean; errors?: CheckMessage[] }>(isDaedalus ? '/api/files/daedalusModels/delete' : `/api/content/${selectedType}/delete`, {
      method: 'POST',
      body: JSON.stringify({ path: removedPath })
    });
    setMessages(result.errors || []);
    setStatus(result.success ? 'published' : 'invalid');
    if (!result.success) return;
    await loadGitStatus();
    setDeleteTarget(null);
    if (!isDaedalus) {
      setBuffers((current) => {
        const next = { ...current };
        delete next[bufferKey(selectedType, removedPath)];
        return next;
      });
    }
    const loaded = await refreshItems(selectedType);
    if (selectedPath === removedPath) {
      const nextPath = loaded[0]?.path || '';
      if (nextPath) {
        await openItem(selectedType, nextPath, true);
      } else {
        setSelectedPath('');
        setYaml('');
        setStatus('clean');
        setSelection({ kind: 'root' });
        writeRouteToUrl(selectedType, '', true);
      }
    }
  };

  const revertBuffer = (type: string, path: string) => {
    const key = bufferKey(type, path);
    const target = buffers[key];
    if (!target) return;
    const reverted: EditBuffer = {
      ...target,
      yaml: target.baseYaml,
      status: 'clean',
      dirty: false,
      messages: [],
      lastTouched: Date.now()
    };
    setBuffers((current) => ({ ...current, [key]: reverted }));
    if (selectedType === type && selectedPath === path) {
      setYaml(reverted.yaml);
      setStatus('clean');
      setMessages([]);
      setSelection({ kind: 'root' });
    }
  };

  const revertContent = (item: ContentItem) => {
    revertBuffer(selectedType, item.path);
  };

  const uploadDaedalusModel = async (file: File | null) => {
    if (!file) return;
    const targetPath = file.name.toLowerCase().endsWith('.bbmodel') ? file.name : `${file.name}.bbmodel`;
    try {
      const result = await api<{ success?: boolean; valid?: boolean; errors?: CheckMessage[]; path?: string }>(`/api/files/daedalusModels/upload?path=${encodeURIComponent(targetPath)}&overwrite=false`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/octet-stream' },
        body: await file.arrayBuffer()
      });
      const checks = result.errors?.length
        ? result.errors
        : [{ title: 'Upload complete', message: `${result.path || targetPath} was uploaded and Daedalus was reloaded.`, location: result.path || targetPath }];
      setMessages(checks);
      setStatus(result.success === false || result.valid === false || result.errors?.length ? 'invalid' : 'published');
      await loadGitStatus();
      const loaded = await refreshItems('daedalusModels');
      const nextPath = result.path || targetPath;
      if (loaded.some((item) => item.path === nextPath)) {
        await openItem('daedalusModels', nextPath);
      }
    } catch (error) {
      setMessages([{ title: 'Upload failed', message: error instanceof Error ? error.message : String(error), location: targetPath }]);
      setStatus('invalid');
    } finally {
      if (daedalusUploadInput.current) {
        daedalusUploadInput.current.value = '';
      }
    }
  };

  const loadUsers = async () => {
    if (me?.role !== 'ADMIN') return;
    setUsers(await api<WebUser[]>('/api/users'));
  };

  const setUserRole = async (playerId: string, role: WebUser['role']) => {
    await api<WebUser>('/api/users/role', { method: 'POST', body: JSON.stringify({ playerId, role }) });
    await loadUsers();
  };

  const openContentType = async (type: string) => {
    setServerOpen(false);
    setDashboardOpen(false);
    setSelectedPath('');
    setSelectedType(type);
    setSelection({ kind: 'root' });
    const loaded = await refreshItems(type);
    const nextPath = loaded[0]?.path || '';
    if (nextPath) {
      await openItem(type, nextPath);
    } else {
      setYaml('');
      setStatus('clean');
      writeRouteToUrl(type, '', false);
    }
  };

  const openDashboard = () => {
    setServerOpen(false);
    setDashboardOpen(true);
    setSelectedPath('');
    setSelection({ kind: 'root' });
    writeRouteToUrl('', '', false);
  };

  const openServer = () => {
    setServerOpen(true);
    setDashboardOpen(false);
    setSelectedPath('');
    setSelection({ kind: 'root' });
    writeRouteToUrl('', '', false, 'server');
  };

  if (!me) return <LoginScreen onLogin={loadSession} error={authError} serverPanel={<ServerControlPage compact />} />;
  const showGitButton = me.role === 'ADMIN' || Boolean(gitStatus?.enabled);

  return (
    <MantineProvider defaultColorScheme="dark">
      <AppShell header={{ height: 54 }} navbar={dashboardOpen || serverOpen ? undefined : { width: 300, breakpoint: 'sm' }} aside={dashboardOpen || serverOpen || structuredContent ? undefined : { width: 460, breakpoint: 'md' }} padding={0}>
        <AppShell.Header className="topbar">
          <Group h="100%" px="md" justify="space-between">
            <Group gap="sm" className="brandButton" onClick={openDashboard}>
              <img src="https://erethon.de/favicon_32.png" alt="" className="brandIcon" />
              <Text fw={700}>Erethon</Text>
              <Badge color={statusColor(status)} variant="light">{status}</Badge>
              {changedCount > 0 && <Badge color="yellow" variant="light">{changedCount} edited</Badge>}
              {!dashboardOpen && doc.parseError && <Badge color="red" variant="light">YAML parse error</Badge>}
            </Group>
            <Group gap="xs">
              {!dashboardOpen && !serverOpen && <>
              {showGitButton && <Button size="xs" variant="light" onClick={() => { setGitOpen(true); void loadGitStatus(); }}>Git{gitStatus?.dirtyFiles?.length ? ` ${gitStatus.dirtyFiles.length}` : ''}</Button>}
              <Tooltip label="Validate"><ActionIcon variant="subtle" onClick={validate}><IconCircleCheck size={18} /></ActionIcon></Tooltip>
              <Tooltip label={changedCount > 1 ? `Save ${changedCount} drafts` : 'Save draft'}><ActionIcon variant="subtle" disabled={!canEdit} onClick={saveDraft}><IconDeviceFloppy size={18} /></ActionIcon></Tooltip>
              <Button size="xs" leftSection={<IconUpload size={16} />} disabled={!canEdit} onClick={openPublish}>{isDaedalus ? 'Reload' : changedCount > 1 ? `Publish all (${changedCount})` : 'Publish'}</Button>
              <Tooltip label={me.role === 'ADMIN' ? `Manage users as ${me.playerName || me.playerId}` : `${me.playerName || me.playerId} (${me.role})`}>
                {me.role === 'ADMIN' ? (
                  <ActionIcon variant="light" onClick={() => { setUsersOpen(true); void loadUsers(); }}><IconEye size={18} /></ActionIcon>
                ) : (
                  <ThemeIcon variant="light"><IconEye size={18} /></ThemeIcon>
                )}
              </Tooltip>
              </>}
            </Group>
          </Group>
        </AppShell.Header>

        {!dashboardOpen && !serverOpen && <AppShell.Navbar className="sidebar">
          <Stack gap="xs" p="sm">
            <Select value={selectedType} onChange={(value) => { void openContentType(value || 'quests'); }} data={typeOptions(types)} />
            <TextInput leftSection={<IconSearch size={16} />} placeholder="Search content" value={search} onChange={(event) => setSearch(event.currentTarget.value)} />
            <Button variant="light" leftSection={<IconPlus size={16} />} disabled={!canEdit} onClick={() => isDaedalus ? daedalusUploadInput.current?.click() : setNewOpen(true)}>{isDaedalus ? 'Upload model' : 'New content'}</Button>
            {isDaedalus && <input ref={daedalusUploadInput} hidden type="file" accept=".bbmodel" onChange={(event) => void uploadDaedalusModel(event.currentTarget.files?.[0] || null)} />}
          </Stack>
          <Divider />
          <ContentList
            items={filteredItems}
            selectedPath={selectedPath}
            canManageFiles={canManageFiles}
            dirtyPaths={dirtyPaths}
            onOpen={(item) => openItem(selectedType, item.path)}
            onRevert={revertContent}
            onMove={beginMove}
            onDelete={setDeleteTarget}
          />
        </AppShell.Navbar>}

        <AppShell.Main className="workspace">
          {serverOpen
            ? <ServerControlPage />
            : dashboardOpen
            ? <Dashboard types={types} onOpenType={openContentType} onOpenServer={openServer} />
            : <>
                {structuredContent && messages.length > 0 && (
                  <Box p="md" pb={0}>
                    <Paper p="sm" withBorder>
                      <Stack gap="xs">
                        {messages.map((message, index) => <CheckMessageCard key={index} message={message} />)}
                      </Stack>
                    </Paper>
                  </Box>
                )}
                <VisualWorkspace doc={doc} selectedPath={selectedPath} selectedItem={selectedItem} selection={selection} onSelect={setSelection} canEdit={canEdit} catalog={catalog} yaml={yaml} baseYaml={baseYaml} selectedType={selectedType} onYamlChange={(next) => updateCurrentYaml(next)} assets={assets} contentAccess={{ load: loadSecondaryContent, update: updateSecondaryContent }} onAddStage={() => mutateDoc((draft) => addStage(draft))} onAddInteraction={() => mutateDoc((draft) => addInteraction(draft))} onRefreshItems={() => refreshItems(selectedType)} onOpenPath={(path) => openItem(selectedType, path)} onChecks={(checks) => { setMessages(checks.length > 0 ? checks : [{ title: 'Operation completed', message: 'No validation messages were reported.', location: selectedPath }]); if (checks.length > 0) setAsideTab('errors'); }} />
              </>}
        </AppShell.Main>

        {!dashboardOpen && !serverOpen && !structuredContent && <AppShell.Aside className="inspector">
          <Tabs value={editableVisual ? asideTab : 'catalog'} onChange={(value) => setAsideTab(value || 'inspect')} h="100%">
            <Tabs.List>
              {editableVisual && <Tabs.Tab value="inspect" leftSection={<IconSettings size={15} />}>Inspect</Tabs.Tab>}
              <Tabs.Tab value="catalog" leftSection={<IconPlus size={15} />}>Add</Tabs.Tab>
              <Tabs.Tab value="used" leftSection={<IconGitBranch size={15} />}>Used</Tabs.Tab>
              <Tabs.Tab value="errors" leftSection={<IconAlertTriangle size={15} />}>Checks</Tabs.Tab>
            </Tabs.List>
            {editableVisual && (
              <Tabs.Panel value="inspect" className="tabPanel">
                <Inspector doc={doc} selection={selection} catalog={catalog} disabled={!canEdit} onMutate={mutateDoc} assets={assets} />
              </Tabs.Panel>
            )}
            <Tabs.Panel value="catalog" className="tabPanel">
              <CatalogPanel doc={doc} selection={selection} catalog={catalog} onAdd={addFromCatalog} disabled={!canEdit} editableVisual={editableVisual} />
            </Tabs.Panel>
            <Tabs.Panel value="used" className="tabPanel">
              <UsedFeaturesPanel doc={doc} catalog={catalog} disabled={!canEdit} />
            </Tabs.Panel>
            <Tabs.Panel value="errors" className="tabPanel">
              <Stack p="sm">
                {doc.parseError && <Paper p="sm" withBorder><Text fw={600} c="red">YAML parse error</Text><Text size="sm">{doc.parseError}</Text></Paper>}
                {messages.length === 0 && !doc.parseError && <Text c="dimmed">No validation messages.</Text>}
                {messages.map((message, index) => (
                  <CheckMessageCard key={index} message={message} />
                ))}
              </Stack>
            </Tabs.Panel>
          </Tabs>
        </AppShell.Aside>}
        <NewContentModal opened={newOpen} onClose={() => setNewOpen(false)} selectedType={selectedType} onCreate={(path) => {
          const contentPath = normalizeNewContentPath(selectedType, path);
          const nextYaml = starterYaml(selectedType, contentPath, catalog);
          setNewOpen(false);
          setSelectedPath(contentPath);
          setYaml(nextYaml);
          setSelection({ kind: 'root' });
          setStatus('staged');
          setBuffers((current) => ({
            ...current,
            [bufferKey(selectedType, contentPath)]: {
              type: selectedType,
              path: contentPath,
              yaml: nextYaml,
              baseYaml: '',
              dirty: true,
              status: 'staged',
              messages: [],
              lastTouched: Date.now()
            }
          }));
          writeRouteToUrl(selectedType, contentPath, false);
        }} />
        <PublishModal
          opened={publishOpen}
          onClose={() => setPublishOpen(false)}
          buffers={changedBuffers}
          types={types}
          publishing={publishing}
          onOpen={(buffer) => {
            setPublishOpen(false);
            void openItem(buffer.type, buffer.path);
          }}
          onRevert={(buffer) => revertBuffer(buffer.type, buffer.path)}
          onPublish={publishChangedBuffers}
        />
        <Modal opened={Boolean(moveTarget)} onClose={() => setMoveTarget(null)} title="Rename or move file">
          <Stack>
            <Text size="sm" c="dimmed">Use a new file name or folder path. Existing files are not overwritten.</Text>
            <TextInput label="Current path" value={moveTarget?.path || ''} readOnly />
            <TextInput label="New path" value={movePath} onChange={(event) => setMovePath(event.currentTarget.value)} autoFocus />
            <Group justify="flex-end">
              <Button variant="subtle" onClick={() => setMoveTarget(null)}>Cancel</Button>
              <Button disabled={!movePath.trim() || movePath.trim() === moveTarget?.path} onClick={moveContent}>Move</Button>
            </Group>
          </Stack>
        </Modal>
        <Modal opened={Boolean(deleteTarget)} onClose={() => setDeleteTarget(null)} title="Delete file">
          <Stack>
            <Text>Delete <Code>{deleteTarget?.path}</Code>?</Text>
            <Text size="sm" c="dimmed">The server creates a timestamped backup first and removes any matching draft.</Text>
            <Group justify="flex-end">
              <Button variant="subtle" onClick={() => setDeleteTarget(null)}>Cancel</Button>
              <Button color="red" onClick={deleteContent}>Delete</Button>
            </Group>
          </Stack>
        </Modal>
        <GitExportModal
          opened={gitOpen}
          onClose={() => setGitOpen(false)}
          me={me}
          status={gitStatus}
          onRefresh={loadGitStatus}
          onPush={async (commitMessage) => {
            const result = await api<GitStatus>('/api/git/push', { method: 'POST', body: JSON.stringify({ commitMessage }) });
            setGitStatus(result);
            setMessages(result.errors?.length ? result.errors : [{ title: 'Git push complete', message: result.lastCommit ? `Pushed ${result.lastCommit}` : 'No Git changes needed a commit.', location: result.branch }]);
            return result;
          }}
          onSaveConfig={async (values) => {
            const result = await api<GitStatus>('/api/git/config', { method: 'POST', body: JSON.stringify(values) });
            setGitStatus(result);
            return result;
          }}
          onClear={async () => {
            const result = await api<GitStatus>('/api/git/clear', { method: 'POST', body: '' });
            setGitStatus(result);
            return result;
          }}
        />
        {me.role === 'ADMIN' && (
          <UserManagementModal
            opened={usersOpen}
            onClose={() => setUsersOpen(false)}
            users={users}
            onRefresh={loadUsers}
            onSetRole={setUserRole}
          />
        )}
      </AppShell>
    </MantineProvider>
  );
}

function NewContentModal({ opened, onClose, selectedType, onCreate }: { opened: boolean; onClose: () => void; selectedType: string; onCreate: (path: string) => void }) {
  const [path, setPath] = useState('');
  return (
    <Modal opened={opened} onClose={onClose} title="New content">
      <Stack>
        <TextInput label="Path / ID" value={path} onChange={(event) => setPath(event.currentTarget.value)} placeholder={newContentPlaceholder(selectedType)} />
        <Button onClick={() => onCreate(path)} disabled={!path.trim()}>Create draft</Button>
      </Stack>
    </Modal>
  );
}

function PublishModal({ opened, onClose, buffers, types, publishing, onOpen, onRevert, onPublish }: {
  opened: boolean;
  onClose: () => void;
  buffers: EditBuffer[];
  types: ContentType[];
  publishing: boolean;
  onOpen: (buffer: EditBuffer) => void;
  onRevert: (buffer: EditBuffer) => void;
  onPublish: () => Promise<void>;
}) {
  const typeLabels = useMemo(() => new Map(types.map((type) => [type.id, type.label])), [types]);
  return (
    <Modal opened={opened} onClose={onClose} title="Publish changes" size="xl">
      <Stack>
        <Text size="sm" c="dimmed">Review the files changed in this browser session.</Text>
        <Stack gap="xs" mah={420} style={{ overflow: 'auto' }}>
          {buffers.map((buffer) => (
            <Paper key={bufferKey(buffer.type, buffer.path)} p="sm" withBorder>
              <Group justify="space-between" wrap="nowrap" align="center">
                <Box>
                  <Group gap="xs">
                    <Badge variant="light">{typeLabels.get(buffer.type) || buffer.type}</Badge>
                    <Badge color={statusColor(buffer.status)} variant="light">{buffer.status}</Badge>
                  </Group>
                  <Code>{buffer.path}</Code>
                </Box>
                <Group gap="xs" wrap="nowrap">
                  <Button size="xs" variant="subtle" onClick={() => onOpen(buffer)}>Open</Button>
                  <Button size="xs" variant="subtle" color="red" onClick={() => onRevert(buffer)}>Revert</Button>
                </Group>
              </Group>
            </Paper>
          ))}
          {buffers.length === 0 && <Text c="dimmed" ta="center" py="xl">No edited files are pending publish.</Text>}
        </Stack>
        <Group justify="flex-end">
          <Button variant="subtle" onClick={onClose}>Cancel</Button>
          <Button leftSection={<IconUpload size={16} />} disabled={buffers.length === 0} loading={publishing} onClick={() => void onPublish()}>Publish {buffers.length} file{buffers.length === 1 ? '' : 's'}</Button>
        </Group>
      </Stack>
    </Modal>
  );
}

function UserManagementModal({ opened, onClose, users, onRefresh, onSetRole }: {
  opened: boolean;
  onClose: () => void;
  users: WebUser[];
  onRefresh: () => Promise<void>;
  onSetRole: (playerId: string, role: WebUser['role']) => Promise<void>;
}) {
  useEffect(() => {
    if (opened) void onRefresh();
  }, [opened]);

  return (
    <Modal opened={opened} onClose={onClose} title="User roles" size="lg">
      <Stack>
        <Group justify="space-between">
          <Text size="sm" c="dimmed">Assign Hermes web roles for registered players.</Text>
          <Button size="xs" variant="light" leftSection={<IconReload size={16} />} onClick={() => void onRefresh()}>Refresh users</Button>
        </Group>
        <Stack gap="xs" mah={520} style={{ overflow: 'auto' }}>
          {users.map((user) => (
            <Paper key={user.playerId} p="sm" withBorder>
              <Group justify="space-between">
                <Box>
                  <Text fw={600}>{user.playerName || user.playerId}</Text>
                  <Text size="xs" c="dimmed">{user.playerId}</Text>
                </Box>
                <Select w={140} value={user.role} data={['NONE', 'VIEWER', 'EDITOR', 'ADMIN']} onChange={(role) => role && void onSetRole(user.playerId, role as WebUser['role'])} />
              </Group>
            </Paper>
          ))}
          {users.length === 0 && <Text c="dimmed" ta="center" py="xl">No registered web users found.</Text>}
        </Stack>
      </Stack>
    </Modal>
  );
}

function GitExportModal({ opened, onClose, me, status, onRefresh, onPush, onSaveConfig, onClear }: {
  opened: boolean;
  onClose: () => void;
  me: WebUser;
  status: GitStatus | null;
  onRefresh: () => Promise<GitStatus | null>;
  onPush: (commitMessage: string) => Promise<GitStatus>;
  onSaveConfig: (values: Record<string, string>) => Promise<GitStatus>;
  onClear: () => Promise<GitStatus>;
}) {
  const [saving, setSaving] = useState(false);
  const [pushing, setPushing] = useState(false);
  const [commitMessage, setCommitMessage] = useState('');
  const [form, setForm] = useState<Record<string, string>>({});
  const [configOpen, setConfigOpen] = useState(false);
  const canAdmin = me.role === 'ADMIN';
  const canPush = me.role === 'EDITOR' || me.role === 'ADMIN';

  useEffect(() => {
    if (!opened || !status) return;
    setForm({
      enabled: String(status.enabled),
      repoUrl: status.repoUrl || '',
      branch: status.branch || 'dev',
      token: '',
      authorName: status.authorName || 'Hermes',
      authorEmail: status.authorEmail || 'hermes@erethon.de',
      clonePath: status.clonePath || 'git-export'
    });
    if (!commitMessage) {
      setCommitMessage('Update content');
    }
    setConfigOpen(false);
  }, [opened, status]);

  const save = async () => {
    setSaving(true);
    try {
      const values = { ...form };
      if (!values.token) {
        delete values.token;
      }
      await onSaveConfig(values);
    } finally {
      setSaving(false);
    }
  };
  const push = async () => {
    setPushing(true);
    try {
      await onPush(commitMessage);
    } finally {
      setPushing(false);
    }
  };

  return (
    <Modal opened={opened} onClose={onClose} title="Git export" size="xl">
      <Stack>
        {!status && <Text c="dimmed">Git status is loading.</Text>}
        {status && (
          <>
            <Group gap="xs">
              <Badge color={status.enabled ? 'green' : 'gray'}>{status.enabled ? 'enabled' : 'disabled'}</Badge>
              <Badge color={status.configured ? 'green' : 'yellow'}>{status.configured ? 'configured' : 'not configured'}</Badge>
              <Badge variant="outline">{status.branch || 'no branch'}</Badge>
              <Badge variant="outline">{status.dirtyFiles?.length || 0} changed</Badge>
            </Group>
            <Paper p="sm" withBorder>
              <Stack gap={4}>
                <Text size="sm"><b>Repository:</b> {status.repoUrl || 'Not configured'}</Text>
                <Text size="sm"><b>Clone:</b> {status.clonePath || 'git-export'}</Text>
                <Text size="sm"><b>Last push:</b> {status.lastPushAt || 'Never'}</Text>
                <Text size="sm"><b>Last commit:</b> {status.lastCommit || '-'}</Text>
                {status.lastError && <Text size="sm" c="red"><b>Last error:</b> {status.lastError}</Text>}
              </Stack>
            </Paper>
            <Paper p="sm" withBorder>
              {canPush && (
                <TextInput
                  label="Commit message"
                  description="Hermes appends: - Hermes Content Update <timestamp>"
                  value={commitMessage}
                  onChange={(event) => setCommitMessage(event.currentTarget.value)}
                  mb="sm"
                />
              )}
              <Group justify="space-between" mb="xs">
                <Text fw={700}>Changed files</Text>
                <Group gap="xs">
                  <Button size="xs" variant="light" onClick={() => void onRefresh()}>Refresh status</Button>
                  {canAdmin && <Button size="xs" variant="subtle" color="red" disabled={!status.dirtyFiles?.length} onClick={() => void onClear()}>Clear queue</Button>}
                  <Button size="xs" disabled={!canPush || !status.configured || !status.dirtyFiles?.length} loading={pushing} onClick={push}>Push changes</Button>
                </Group>
              </Group>
              <Stack gap={4} mah={260} style={{ overflow: 'auto' }}>
                {status.dirtyFiles?.length ? status.dirtyFiles.map((file) => (
                  <Group key={file.path} justify="space-between" wrap="nowrap">
                    <Code>{file.path}</Code>
                    {file.deleted && <Badge color="red">delete</Badge>}
                  </Group>
                )) : <Text size="sm" c="dimmed">No changed files are queued.</Text>}
              </Stack>
            </Paper>
            {canAdmin && (
              <Paper p="sm" withBorder>
                <Group justify="space-between" mb={configOpen ? 'sm' : 0}>
                  <Box>
                    <Text fw={700}>Git configuration</Text>
                    <Text size="xs" c="dimmed">Admin-only repository and credential settings.</Text>
                  </Box>
                  <Button size="xs" variant="light" onClick={() => setConfigOpen((current) => !current)}>{configOpen ? 'Hide' : 'Show'}</Button>
                </Group>
                <Collapse in={configOpen}>
                  <Stack gap="xs">
                    <Checkbox label="Enable Git export" checked={form.enabled === 'true'} onChange={(event) => setForm({ ...form, enabled: String(event.currentTarget.checked) })} />
                    <TextInput label="Repository URL" value={form.repoUrl || ''} onChange={(event) => setForm({ ...form, repoUrl: event.currentTarget.value })} />
                    <TextInput label="Branch" value={form.branch || ''} onChange={(event) => setForm({ ...form, branch: event.currentTarget.value })} />
                    <PasswordInput label="Token" description="Leave blank to keep the current token unchanged only if the server config already contains one." value={form.token || ''} onChange={(event) => setForm({ ...form, token: event.currentTarget.value })} />
                    <Group grow>
                      <TextInput label="Author name" value={form.authorName || ''} onChange={(event) => setForm({ ...form, authorName: event.currentTarget.value })} />
                      <TextInput label="Author email" value={form.authorEmail || ''} onChange={(event) => setForm({ ...form, authorEmail: event.currentTarget.value })} />
                    </Group>
                    <TextInput label="Clone path" value={form.clonePath || ''} onChange={(event) => setForm({ ...form, clonePath: event.currentTarget.value })} />
                    <Button variant="light" loading={saving} onClick={save}>Save Git config</Button>
                  </Stack>
                </Collapse>
              </Paper>
            )}
            {[...(status.errors || []), ...(status.warnings || [])].map((message, index) => <CheckMessageCard key={index} message={message} />)}
          </>
        )}
      </Stack>
    </Modal>
  );
}

function normalizeNewContentPath(type: string, path: string) {
  const trimmed = path.trim();
  if (type === 'hephaestusJobs') return `jobs.yml#${trimmed.replace(/\.ya?ml$/i, '').replace(/^.*#/, '')}`;
  if (type === 'hephaestusVanillaRecipes') return `vanilla_recipes.yml#${trimmed.replace(/\.ya?ml$/i, '').replace(/^.*#/, '')}`;
  if (type === 'hephaestusJobRecipes') {
    if (trimmed.includes('#')) return trimmed;
    return `recipes.yml#${trimmed.replace(/\.ya?ml$/i, '')}`;
  }
  return trimmed.endsWith('.yml') || trimmed.endsWith('.yaml') ? trimmed : `${trimmed}.yml`;
}

function newContentPlaceholder(type: string) {
  if (type === 'hephaestusJobs') return 'blacksmith';
  if (type === 'hephaestusVanillaRecipes') return 'custom_sword';
  if (type === 'hephaestusJobRecipes') return 'weapons/smithing.yml#iron_blade';
  if (type === 'hephaestusShops') return 'general_shop';
  return `${type}/example`;
}

function statusColor(status: string) {
  if (status === 'published') return 'green';
  if (status === 'invalid') return 'red';
  if (status === 'staged') return 'orange';
  return 'gray';
}
