import { ChangeDetectionStrategy, Component, ElementRef, Injector, afterNextRender, computed, inject, signal, viewChild } from '@angular/core';
import { buildTimeline } from '../cash/geometry';
import { formatBRL, formatDayLong, formatDayShort, addDays, IsoDate } from '../cash/format';
import { createCashModel } from './cash-model';
import { MESSAGES } from './messages';
import { validateQuickEntry } from './quick-entry';

const STRIP = { width: 820, height: 150, paddingX: 6, paddingY: 14 };
const MAX_TICK = 46;

/** Direção A, "Livro-caixa": uma página, a resposta em frase, uma faixa fina de saldo e os dias como extrato. */
@Component({
  selector: 'app-direction-a',
  templateUrl: './direction-a.page.html',
  styleUrl: './direction-a.page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown)': 'onKeydown($event)' },
})
export class DirectionAPage {
  protected readonly model = createCashModel();
  protected readonly text = MESSAGES;
  protected readonly strip = STRIP;
  protected readonly formatBRL = formatBRL;
  protected readonly formatDayShort = formatDayShort;
  protected readonly formOpen = signal(false);
  protected readonly formError = signal<string | null>(null);
  private readonly injector = inject(Injector);
  private readonly descriptionInput = viewChild<ElementRef<HTMLInputElement>>('description');

  protected readonly timeline = computed(() => buildTimeline(this.model.series(), STRIP));
  private readonly balanceByDate = computed(() => new Map(this.model.series().map((day) => [day.date, day.balanceCents])));

  protected readonly ticks = computed(() => {
    const { points } = this.timeline();
    const zeroY = this.timeline().zeroY;
    const entries = this.model.series().flatMap((day, index) => day.entries.map((entry) => ({ entry, x: points[index].x })));
    const largest = Math.max(1, ...entries.map(({ entry }) => Math.abs(entry.amountCents)));
    return entries.map(({ entry, x }) => {
      const length = Math.max(6, (Math.abs(entry.amountCents) / largest) * MAX_TICK);
      return { id: entry.id, x, kind: entry.kind, y1: zeroY, y2: entry.kind === 'receivable' ? zeroY - length : zeroY + length };
    });
  });

  protected readonly headline = computed(() => {
    const days = this.model.cashDays();
    if (days === null) {
      return { lead: 'Seu caixa fica positivo', strong: 'nos próximos 45 dias', detail: null };
    }
    const firstNegative = this.model.series()[days];
    const detail = `Em ${formatDayLong(firstNegative.date)} o saldo fica em ${formatBRL(firstNegative.balanceCents)}.`;
    if (days === 0) {
      return { lead: 'Seu caixa já está', strong: 'negativo hoje', detail };
    }
    return { lead: 'Você tem caixa para', strong: `${days} ${days === 1 ? 'dia' : 'dias'}`, detail };
  });

  protected readonly axisLabels = computed(() => {
    const { today, horizonEnd } = this.model;
    return [today, addDays(today, 15), addDays(today, 30), horizonEnd].map((date) => formatDayShort(date));
  });

  protected readonly dayLabel = (date: IsoDate) => {
    const label = formatDayLong(date);
    return label.charAt(0).toUpperCase() + label.slice(1);
  };
  protected readonly endOfDay = (date: IsoDate) => this.balanceByDate().get(date) ?? 0;
  protected readonly defaultDate = computed(() => addDays(this.model.today, 1));

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
    this.formOpen.set(false);
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape' && this.formOpen()) {
      this.closeForm();
      return;
    }
    const typing = event.target instanceof HTMLElement && ['INPUT', 'TEXTAREA', 'SELECT'].includes(event.target.tagName);
    if (event.key.toLowerCase() === 'n' && !typing && !event.ctrlKey && !event.metaKey && !event.altKey) {
      event.preventDefault();
      this.openForm();
    }
  }
}
