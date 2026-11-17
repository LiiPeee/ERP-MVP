import { ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { buildTimeline } from '../cash/geometry';
import { formatBRL, formatDayLong, formatDayShort, IsoDate } from '../cash/format';
import { createCashModel } from './cash-model';
import { MESSAGES } from './messages';
import { validateQuickEntry } from './quick-entry';

const ROW = 26;
const SPAN = 150;

/** Direção B, "Mesa de controle": linha do tempo vertical como protagonista e o painel do dia ao lado. */
@Component({
  selector: 'app-direction-b',
  templateUrl: './direction-b.page.html',
  styleUrl: './direction-b.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown)': 'onKeydown($event)' },
})
export class DirectionBPage {
  protected readonly model = createCashModel();
  protected readonly text = MESSAGES;
  protected readonly formatBRL = formatBRL;
  protected readonly formatDayShort = formatDayShort;
  protected readonly span = SPAN;
  protected readonly formOpen = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly injector = inject(Injector);
  private readonly descriptionInput = viewChild<ElementRef<HTMLInputElement>>('description');

  protected readonly height = computed(() => this.model.series().length * ROW);
  protected readonly selectedDate = signal<IsoDate>(this.model.today);

  /** Mesma geometria da direção A, transposta: o tempo desce e o saldo avança para a direita. */
  protected readonly curve = computed(() => {
    const timeline = buildTimeline(this.model.series(), { width: this.height(), height: SPAN, paddingX: ROW / 2, paddingY: 12 });
    const toVertical = (p: { x: number; y: number }) => ({ x: SPAN - p.y, y: p.x });
    const points = timeline.points.map(toVertical);
    return {
      points,
      path: points.map((p, index) => `${index === 0 ? 'M' : 'L'}${p.x} ${p.y}`).join(' '),
      zeroX: SPAN - timeline.zeroY,
      negativeBands: timeline.negativeAreas.map((area) => ({ y: area.fromX, height: area.toX - area.fromX })),
    };
  });

  protected readonly markers = computed(() =>
    this.model.series().flatMap((day, index) => {
      const net = day.entries.reduce((sum, entry) => sum + entry.amountCents, 0);
      return day.entries.length === 0 ? [] : [{ date: day.date, ...this.curve().points[index], kind: net >= 0 ? 'receivable' : 'payable' }];
    }),
  );

  protected readonly firstNegative = computed(() => {
    const days = this.model.cashDays();
    return days === null ? null : this.model.series()[days];
  });

  protected readonly selectedDay = computed(() => this.model.series().find((day) => day.date === this.selectedDate()));
  /** Os atrasados vencem "hoje" na projeção, mas aparecem em bloco próprio no painel. */
  protected readonly selectedEntries = computed(() => {
    const overdueIds = new Set(this.model.overdue().map((entry) => entry.id));
    return (this.selectedDay()?.entries ?? []).filter((entry) => !overdueIds.has(entry.id));
  });
  protected readonly dayLabel = (date: IsoDate) => {
    const label = formatDayLong(date);
    return label.charAt(0).toUpperCase() + label.slice(1);
  };

  protected select(date: IsoDate): void {
    this.selectedDate.set(date);
  }

  protected move(step: number): void {
    const series = this.model.series();
    const index = series.findIndex((day) => day.date === this.selectedDate());
    const next = series[Math.min(series.length - 1, Math.max(0, index + step))];
    this.selectedDate.set(next.date);
    afterNextRender(() => document.getElementById(`row-${next.date}`)?.scrollIntoView?.({ block: 'nearest' }), { injector: this.injector });
  }

  protected openForm(): void {
    this.formError.set(null);
    this.formOpen.set(true);
    afterNextRender(() => this.descriptionInput()?.nativeElement.focus(), { injector: this.injector });
  }

  protected closeForm(): void {
    this.formOpen.set(false);
  }

  protected submit(event: Event, description: string, amount: string, date: string): void {
    event.preventDefault();
    const result = validateQuickEntry({ description, amount, date });
    if ('error' in result) {
      this.formError.set(result.error);
      return;
    }
    this.model.add(result.entry);
    this.selectedDate.set(result.entry.date);
    this.formOpen.set(false);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.formOpen()) {
      this.closeForm();
      return;
    }
    const typing = event.target instanceof HTMLElement && ['INPUT', 'TEXTAREA', 'SELECT'].includes(event.target.tagName);
    if (typing || event.ctrlKey || event.metaKey || event.altKey) {
      return;
    }
    const key = event.key.toLowerCase();
    if (key === 'n') {
      event.preventDefault();
      this.openForm();
    } else if (key === 'j') {
      this.move(1);
    } else if (key === 'k') {
      this.move(-1);
    }
  }
}
