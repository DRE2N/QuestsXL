import type { UIEvent } from 'react';
import { useMemo, useRef, useState } from 'react';
import { Alert, Box, Group, Text, Textarea } from '@mantine/core';
import { parseDocument } from 'yaml';

type YamlIssue = {
  message: string;
  line?: number;
  col?: number;
};

export function RawYamlEditor({ value, disabled, minRows = 32, onChange }: { value: string; disabled: boolean; minRows?: number; onChange: (value: string) => void }) {
  const highlightRef = useRef<HTMLPreElement>(null);
  const lineRef = useRef<HTMLDivElement>(null);
  const [focused, setFocused] = useState(false);
  const validation = useMemo(() => validateYaml(value), [value]);
  const highlighted = useMemo(() => highlightYaml(value, validation.issue?.line), [value, validation.issue?.line]);
  const lineCount = Math.max(1, value.split('\n').length);

  const syncScroll = (event: UIEvent<HTMLTextAreaElement>) => {
    const target = event.currentTarget;
    if (highlightRef.current) {
      highlightRef.current.scrollTop = target.scrollTop;
      highlightRef.current.scrollLeft = target.scrollLeft;
    }
    if (lineRef.current) lineRef.current.scrollTop = target.scrollTop;
  };

  return (
    <Box>
      <Group justify="space-between" mb="xs">
        <Text size="sm" c={validation.issue ? 'red' : 'teal'} fw={600}>
          {validation.issue ? 'YAML has syntax errors' : 'YAML syntax looks valid'}
        </Text>
        <Text size="xs" c="dimmed">{lineCount} lines</Text>
      </Group>
      {validation.issue && (
        <Alert color="red" variant="light" mb="sm" title={validation.issue.line ? `Line ${validation.issue.line}${validation.issue.col ? `, column ${validation.issue.col}` : ''}` : 'YAML parse error'}>
          {validation.issue.message}
        </Alert>
      )}
      <Box className={`rawYamlEditor ${focused ? 'focused' : ''}`}>
        <div ref={lineRef} className="rawYamlLineNumbers" aria-hidden="true">
          {Array.from({ length: lineCount }, (_, index) => <span key={index}>{index + 1}</span>)}
        </div>
        <pre ref={highlightRef} className="rawYamlHighlight" aria-hidden="true" dangerouslySetInnerHTML={{ __html: highlighted }} />
        <Textarea
          value={value}
          readOnly={disabled}
          autosize={false}
          minRows={minRows}
          className="rawYamlTextarea"
          onScroll={syncScroll}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          onChange={(event) => onChange(event.currentTarget.value)}
        />
      </Box>
    </Box>
  );
}

function validateYaml(value: string): { issue?: YamlIssue } {
  try {
    const doc = parseDocument(value || '{}', { prettyErrors: true });
    const issue = doc.errors[0];
    if (!issue) return {};
    const linePos = issue.linePos?.[0];
    return {
      issue: {
        message: issue.message,
        line: linePos?.line,
        col: linePos?.col
      }
    };
  } catch (error) {
    return { issue: { message: error instanceof Error ? error.message : 'Invalid YAML' } };
  }
}

function highlightYaml(value: string, errorLine?: number) {
  return value.split('\n').map((line, index) => {
    const lineNumber = index + 1;
    const highlighted = highlightLine(line);
    return `<span class="rawYamlLine ${errorLine === lineNumber ? 'errorLine' : ''}">${highlighted || ' '}</span>`;
  }).join('');
}

function highlightLine(line: string) {
  const escaped = escapeHtml(line);
  const commentIndex = findCommentStart(escaped);
  const body = commentIndex >= 0 ? escaped.slice(0, commentIndex) : escaped;
  const comment = commentIndex >= 0 ? escaped.slice(commentIndex) : '';
  const keyed = body.match(/^(\s*(?:-\s*)?)([A-Za-z0-9_.-]+)(\s*:)(.*)$/);
  const highlightedBody = keyed
    ? `${keyed[1]}<span class="yamlKey">${keyed[2]}</span>${keyed[3]}${highlightScalars(keyed[4])}`
    : highlightScalars(body);
  return `${highlightedBody}${comment ? `<span class="yamlComment">${comment}</span>` : ''}`;
}

function highlightScalars(value: string) {
  return value
    .replace(/(&quot;.*?&quot;|'.*?')/g, '<span class="yamlString">$1</span>')
    .replace(/\b(true|false|null)\b/g, '<span class="yamlBool">$1</span>')
    .replace(/(^|[\s:[,-])(-?\d+(?:\.\d+)?)(?=$|[\s,\]])/g, '$1<span class="yamlNumber">$2</span>');
}

function findCommentStart(value: string) {
  let single = false;
  let double = false;
  for (let index = 0; index < value.length; index++) {
    const char = value[index];
    if (char === "'" && !double) single = !single;
    if (char === '&' && value.slice(index, index + 6) === '&quot;' && !single) {
      double = !double;
      index += 5;
      continue;
    }
    if (char === '#' && !single && !double) return index;
  }
  return -1;
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}
