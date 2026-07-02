let miniMessageModule: Promise<unknown> | null = null;

export function warmMiniMessageRenderer() {
  miniMessageModule ??= import('minimessage-js').catch(() => null);
}

export function previewMiniMessageLine(value: string): Array<{ text: string; color?: string; bold?: boolean; italic?: boolean }> {
  const segments: Array<{ text: string; color?: string; bold?: boolean; italic?: boolean }> = [];
  const state: { color?: string; bold?: boolean; italic?: boolean } = {};
  const tagPattern = /<([^>]+)>/g;
  let cursor = 0;
  let match: RegExpExecArray | null;
  while ((match = tagPattern.exec(value)) !== null) {
    if (match.index > cursor) {
      segments.push({ text: value.slice(cursor, match.index), ...state });
    }
    applyMiniMessageTag(match[1].trim().toLowerCase(), state);
    cursor = match.index + match[0].length;
  }
  if (cursor < value.length) {
    segments.push({ text: value.slice(cursor), ...state });
  }
  return segments.length > 0 ? segments : [{ text: '' }];
}

export function miniMessageToHtml(value: string) {
  return value.split('\n').map((line) => `<p>${miniMessageLineToHtml(line)}</p>`).join('');
}

export function miniMessageToTiptapDoc(value: string) {
  return {
    type: 'doc',
    content: value.split('\n').map((line) => {
      const content = miniMessageLineSegments(line)
        .filter((segment) => segment.text.length > 0)
        .map((segment) => {
          const marks = segmentMarks(segment);
          return marks.length > 0 ? { type: 'text', text: segment.text, marks } : { type: 'text', text: segment.text };
        });
      return content.length > 0 ? { type: 'paragraph', content } : { type: 'paragraph' };
    })
  };
}

export function htmlToMiniMessage(html: string) {
  if (typeof window === 'undefined' || typeof DOMParser === 'undefined') {
    return stripHtml(html);
  }
  const doc = new DOMParser().parseFromString(`<div>${html}</div>`, 'text/html');
  return Array.from(doc.body.firstElementChild?.childNodes || [])
    .map((node) => nodeToMiniMessage(node, {}))
    .join('')
    .replace(/\n+$/g, '');
}

function miniMessageLineToHtml(value: string) {
  return miniMessageLineSegments(value).map((segment) => htmlSegment(segment.text, segment)).join('') || '<span></span>';
}

function miniMessageLineSegments(value: string): TextSegment[] {
  const segments: TextSegment[] = [];
  const state: TextState = {};
  const tagPattern = /<([^>]+)>/g;
  let cursor = 0;
  let match: RegExpExecArray | null;
  while ((match = tagPattern.exec(value)) !== null) {
    if (match.index > cursor) {
      segments.push({ text: value.slice(cursor, match.index), ...state });
    }
    const tag = match[1].trim();
    if (!applyMiniMessageTag(tag.toLowerCase(), state)) {
      segments.push({ text: match[0], ...state });
    }
    cursor = match.index + match[0].length;
  }
  if (cursor < value.length) {
    segments.push({ text: value.slice(cursor), ...state });
  }
  return segments.length > 0 ? segments : [{ text: '' }];
}

function htmlSegment(text: string, state: TextState) {
  const styles = [
    state.color ? `color: ${state.color}` : '',
    state.bold ? 'font-weight: 700' : '',
    state.italic ? 'font-style: italic' : '',
    state.underline ? 'text-decoration-line: underline' : '',
    state.strike ? 'text-decoration-line: line-through' : ''
  ].filter(Boolean).join('; ');
  return styles ? `<span style="${styles}">${escapeHtml(text)}</span>` : escapeHtml(text);
}

function applyMiniMessageTag(tag: string, state: { color?: string; bold?: boolean; italic?: boolean; underline?: boolean; strike?: boolean }) {
  const normalized = tag.startsWith('/') ? tag.slice(1) : tag;
  if (normalized === 'reset') {
    delete state.color;
    delete state.bold;
    delete state.italic;
    delete state.underline;
    delete state.strike;
    return true;
  }
  if (normalized === 'bold' || normalized === 'b') {
    state.bold = !tag.startsWith('/');
    return true;
  }
  if (normalized === 'italic' || normalized === 'i') {
    state.italic = !tag.startsWith('/') && tag !== '!i';
    return true;
  }
  if (tag === '!i') {
    state.italic = false;
    return true;
  }
  if (normalized === 'underline' || normalized === 'underlined' || normalized === 'u') {
    state.underline = !tag.startsWith('/');
    return true;
  }
  if (normalized === 'strikethrough' || normalized === 'strike' || normalized === 's') {
    state.strike = !tag.startsWith('/');
    return true;
  }
  const hexColor = miniMessageHexColor(normalized);
  if (hexColor) {
    state.color = hexColor;
    return true;
  }
  const namedColor = MINI_MESSAGE_COLORS[normalized];
  if (namedColor) {
    state.color = namedColor;
    return true;
  }
  return false;
}

function nodeToMiniMessage(node: Node, inherited: TextState): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return wrapMiniMessage(node.textContent || '', inherited);
  }
  if (!(node instanceof HTMLElement)) {
    return '';
  }
  if (node.tagName === 'BR') {
    return '\n';
  }
  const state = stateFromElement(node, inherited);
  const content = Array.from(node.childNodes).map((child) => nodeToMiniMessage(child, state)).join('');
  if (node.tagName === 'P' || node.tagName === 'DIV') {
    return `${content}\n`;
  }
  return content;
}

type TextState = {
  color?: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  strike?: boolean;
};

type TextSegment = TextState & {
  text: string;
};

function segmentMarks(segment: TextState) {
  const marks = [];
  if (segment.color) marks.push({ type: 'textStyle', attrs: { color: segment.color } });
  if (segment.bold) marks.push({ type: 'bold' });
  if (segment.italic) marks.push({ type: 'italic' });
  if (segment.underline) marks.push({ type: 'underline' });
  if (segment.strike) marks.push({ type: 'strike' });
  return marks;
}

function stateFromElement(element: HTMLElement, inherited: TextState): TextState {
  const style = element.style;
  const decoration = `${style.textDecoration || ''} ${style.textDecorationLine || ''}`.toLowerCase();
  return {
    color: cssColorToHex(style.color) || inherited.color,
    bold: inherited.bold || element.tagName === 'STRONG' || element.tagName === 'B' || style.fontWeight === '700' || style.fontWeight === 'bold',
    italic: inherited.italic || element.tagName === 'EM' || element.tagName === 'I' || style.fontStyle === 'italic',
    underline: inherited.underline || element.tagName === 'U' || decoration.includes('underline'),
    strike: inherited.strike || element.tagName === 'S' || element.tagName === 'DEL' || decoration.includes('line-through')
  };
}

function wrapMiniMessage(text: string, state: TextState) {
  if (!text) return '';
  const tags: string[] = [];
  const close: string[] = [];
  if (state.color) tags.push(`<${state.color}>`);
  if (state.bold) { tags.push('<bold>'); close.unshift('</bold>'); }
  if (state.italic) { tags.push('<italic>'); close.unshift('</italic>'); }
  if (state.underline) { tags.push('<underlined>'); close.unshift('</underlined>'); }
  if (state.strike) { tags.push('<strikethrough>'); close.unshift('</strikethrough>'); }
  return `${tags.join('')}${text}${close.join('')}`;
}

function cssColorToHex(value: string) {
  if (!value) return '';
  if (/^#[0-9a-f]{6}$/i.test(value)) return value.toLowerCase();
  const match = value.match(/^rgba?\((\d+)[,\s]+(\d+)[,\s]+(\d+)/i);
  if (!match) return '';
  return `#${[match[1], match[2], match[3]].map((part) => Number(part).toString(16).padStart(2, '0')).join('')}`;
}

function miniMessageHexColor(value: string) {
  const raw = value.startsWith('color:') || value.startsWith('colour:') ? value.slice(value.indexOf(':') + 1).trim() : value;
  const full = raw.match(/^#([0-9a-f]{6})$/i);
  if (full) return `#${full[1].toLowerCase()}`;
  const short = raw.match(/^#([0-9a-f]{3})$/i);
  if (!short) return '';
  return `#${short[1].split('').map((part) => part + part).join('').toLowerCase()}`;
}

function escapeHtml(value: string) {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
}

function stripHtml(value: string) {
  return value.replace(/<br\s*\/?>/gi, '\n').replace(/<[^>]+>/g, '');
}

const MINI_MESSAGE_COLORS: Record<string, string> = {
  black: '#000000',
  dark_blue: '#0000aa',
  dark_green: '#00aa00',
  dark_aqua: '#00aaaa',
  dark_red: '#aa0000',
  dark_purple: '#aa00aa',
  gold: '#ffaa00',
  gray: '#aaaaaa',
  dark_gray: '#555555',
  blue: '#5555ff',
  green: '#55ff55',
  aqua: '#55ffff',
  red: '#ff5555',
  light_purple: '#ff55ff',
  yellow: '#ffff55',
  white: '#ffffff'
};
