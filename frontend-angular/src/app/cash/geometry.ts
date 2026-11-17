import { DayBalance } from './projection';

export interface TimelineBox {
  readonly width: number;
  readonly height: number;
  readonly paddingX: number;
  readonly paddingY: number;
}

export interface TimelinePoint {
  readonly x: number;
  readonly y: number;
}

export interface NegativeArea {
  readonly fromX: number;
  readonly toX: number;
}

export interface Timeline {
  readonly points: TimelinePoint[];
  readonly path: string;
  readonly zeroY: number;
  readonly negativeAreas: NegativeArea[];
  /** x em que o saldo passa a ser negativo; nulo se nunca fica negativo. */
  readonly zeroCrossingX: number | null;
}

/** Calcula só coordenadas; quem desenha (SVG da direção A ou B) decide o resto. */
export function buildTimeline(series: readonly DayBalance[], box: TimelineBox): Timeline {
  if (series.length === 0) {
    throw new Error('A série não pode ser vazia.');
  }
  const balances = series.map((day) => day.balanceCents);
  const low = Math.min(0, ...balances);
  const high = Math.max(0, ...balances);
  const range = high === low ? 1 : high - low;
  const usableHeight = box.height - 2 * box.paddingY;
  const toY = (cents: number) => box.paddingY + ((high - cents) / range) * usableHeight;
  const toX = (index: number) =>
    series.length === 1 ? box.width / 2 : box.paddingX + (index / (series.length - 1)) * (box.width - 2 * box.paddingX);

  const points = series.map((day, index) => ({ x: toX(index), y: toY(day.balanceCents) }));
  const path = points.map((p, index) => `${index === 0 ? 'M' : 'L'}${p.x} ${p.y}`).join(' ');

  const negativeAreas: NegativeArea[] = [];
  let openFrom: number | null = balances[0] < 0 ? points[0].x : null;
  let zeroCrossingX: number | null = openFrom;
  for (let i = 1; i < series.length; i++) {
    const [before, after] = [balances[i - 1], balances[i]];
    const crossingX = points[i - 1].x + (before / (before - after)) * (points[i].x - points[i - 1].x);
    if (before >= 0 && after < 0) {
      openFrom = crossingX;
      zeroCrossingX ??= crossingX;
    } else if (before < 0 && after >= 0 && openFrom !== null) {
      negativeAreas.push({ fromX: openFrom, toX: crossingX });
      openFrom = null;
    }
  }
  if (openFrom !== null) {
    negativeAreas.push({ fromX: openFrom, toX: points[points.length - 1].x });
  }
  return { points, path, zeroY: toY(0), negativeAreas, zeroCrossingX };
}
