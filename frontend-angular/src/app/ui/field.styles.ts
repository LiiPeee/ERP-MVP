let counter = 0;

/** Ids únicos para ligar rótulo, dica e erro ao campo (aria-describedby). */
export function nextFieldId(prefix: string): string {
  counter += 1;
  return `${prefix}-${counter}`;
}

/** Estilo único dos campos: um padrão só, para texto, valor, seletor e data. */
export const FIELD_STYLES = `
  :host { display: flex; flex-direction: column; gap: var(--space-1); }
  label { font-size: var(--text-s); font-weight: var(--weight-strong); }
  input, select {
    min-height: var(--control-height);
    padding: 0 var(--space-3);
    border: var(--line) solid var(--color-ink);
    border-radius: var(--radius);
    background: var(--color-paper);
    color: var(--color-ink);
    font: inherit;
  }
  input[aria-invalid="true"] { border-color: var(--color-alert); border-width: var(--line-strong); }
  .hint { margin: 0; color: var(--color-ink-muted); font-size: var(--text-s); }
`;
