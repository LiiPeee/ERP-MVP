import { buildTimeline } from './geometry';
import { DayBalance } from './projection';

function day(date: string, balanceCents: number): DayBalance {
  return { date, balanceCents, entries: [] };
}

const box = { width: 300, height: 100, paddingX: 10, paddingY: 10 };

describe('buildTimeline', () => {
  it('dado uma série, quando calcula, então espalha os dias de forma uniforme dentro das margens', () => {
    const timeline = buildTimeline([day('2026-03-01', 0), day('2026-03-02', 0), day('2026-03-03', 0)], box);

    expect(timeline.points.map((p) => p.x)).toEqual([10, 150, 290]);
  });

  it('dado saldo maior, quando calcula, então fica mais alto (y menor) e a linha do zero fica entre os extremos', () => {
    const timeline = buildTimeline([day('2026-03-01', -1000), day('2026-03-02', 0), day('2026-03-03', 1000)], box);

    expect(timeline.points[2].y).toBe(10);
    expect(timeline.points[0].y).toBe(90);
    expect(timeline.zeroY).toBe(50);
  });

  it('dado saldo sempre positivo, quando calcula, então o zero fica na base e não há zona negativa', () => {
    const timeline = buildTimeline([day('2026-03-01', 500), day('2026-03-02', 1000)], box);

    expect(timeline.zeroY).toBe(90);
    expect(timeline.negativeAreas).toEqual([]);
  });

  it('dado saldo que cruza o zero, quando calcula, então marca a zona negativa com o ponto exato do cruzamento', () => {
    const timeline = buildTimeline([day('2026-03-01', 1000), day('2026-03-02', -1000), day('2026-03-03', -1000)], box);

    expect(timeline.negativeAreas).toHaveLength(1);
    const [area] = timeline.negativeAreas;
    expect(area.fromX).toBeCloseTo(80, 5);
    expect(area.toX).toBe(290);
  });

  it('dado cruzamento, quando calcula, então informa o x em que o saldo vira negativo', () => {
    const timeline = buildTimeline([day('2026-03-01', 1000), day('2026-03-02', -1000)], box);

    expect(timeline.zeroCrossingX).toBeCloseTo(150, 5);
  });

  it('dado saldo sempre negativo, quando calcula, então o zero fica no topo e a zona cobre tudo', () => {
    const timeline = buildTimeline([day('2026-03-01', -500), day('2026-03-02', -1000)], box);

    expect(timeline.zeroY).toBe(10);
    expect(timeline.negativeAreas).toEqual([{ fromX: 10, toX: 290 }]);
  });

  it('dado saldo constante, quando calcula, então não divide por zero', () => {
    const timeline = buildTimeline([day('2026-03-01', 700), day('2026-03-02', 700)], box);

    expect(timeline.points.every((p) => Number.isFinite(p.y))).toBe(true);
  });

  it('dado um único dia, quando calcula, então centraliza o ponto', () => {
    const timeline = buildTimeline([day('2026-03-01', 100)], box);

    expect(timeline.points[0].x).toBe(150);
  });

  it('dado série vazia, quando calcula, então rejeita', () => {
    expect(() => buildTimeline([], box)).toThrow();
  });

  it('dado a série, quando calcula, então o caminho SVG começa com M e liga todos os pontos', () => {
    const timeline = buildTimeline([day('2026-03-01', 0), day('2026-03-02', 100), day('2026-03-03', 50)], box);

    expect(timeline.path.startsWith('M')).toBe(true);
    expect(timeline.path.split('L')).toHaveLength(3);
  });
});
