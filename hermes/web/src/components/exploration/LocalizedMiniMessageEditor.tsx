import { SimpleGrid } from '@mantine/core';
import { MiniMessageField } from '../fields/ValueField';

export type LocalizedText = {
  en: string;
  de: string;
  legacy?: string;
};

export function LocalizedMiniMessageEditor({ label, value, disabled, compact = false, onChange }: {
  label: string;
  value: LocalizedText;
  disabled: boolean;
  compact?: boolean;
  onChange: (value: LocalizedText) => void;
}) {
  return (
    <SimpleGrid cols={{ base: 1, md: 2 }} spacing="xs">
      <MiniMessageField label={`${label}.en`} value={value.en} compact={compact} disabled={disabled} onChange={(next) => onChange({ ...value, en: String(next ?? '') })} />
      <MiniMessageField label={`${label}.de`} value={value.de} compact={compact} disabled={disabled} onChange={(next) => onChange({ ...value, de: String(next ?? '') })} />
    </SimpleGrid>
  );
}

export function localizedFromRecord(values: Record<string, string>, key: string, fallback = ''): LocalizedText {
  const legacy = values[key];
  const en = values[`${key}.en`] ?? legacy ?? fallback;
  const de = values[`${key}.de`] ?? '';
  return { en, de, legacy };
}

export function localizedFromYaml(section: Record<string, unknown>, key: string, fallback = ''): LocalizedText {
  const raw = section[key];
  if (isRecord(raw)) {
    return { en: String(raw.en ?? fallback), de: String(raw.de ?? ''), legacy: undefined };
  }
  const legacy = raw == null ? undefined : String(raw);
  return { en: legacy ?? fallback, de: '', legacy };
}

export function localizedToQLine(key: string, value: LocalizedText): Record<string, string> {
  const en = value.en.trim();
  const de = value.de.trim();
  if (en || de) {
    return { [`${key}.en`]: value.en, [`${key}.de`]: value.de };
  }
  return value.legacy ? { [key]: value.legacy } : {};
}

export function localizedToYaml(value: LocalizedText): string | { en: string; de: string } | null {
  const en = value.en.trim();
  const de = value.de.trim();
  if (de || (value.legacy !== undefined && en !== value.legacy.trim())) {
    return { en: value.en, de: value.de };
  }
  return en || value.legacy || null;
}

export function omitLocalized(source: Record<string, string>, keys: string[]) {
  return Object.fromEntries(Object.entries(source).filter(([key]) => !keys.some((base) => key === base || key === `${base}.en` || key === `${base}.de`)));
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}
