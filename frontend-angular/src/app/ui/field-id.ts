let counter = 0;

/** Ids únicos para ligar rótulo, dica e erro ao campo (aria-describedby). */
export function nextFieldId(prefix: string): string {
  counter += 1;
  return `${prefix}-${counter}`;
}
