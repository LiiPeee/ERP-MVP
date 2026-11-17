import { ChangeDetectionStrategy, Component, Injector, afterNextRender, computed, inject, signal } from '@angular/core';
import { buildTimeline } from '../cash/geometry';
import { IsoDate } from '../cash/format';
import { ButtonComponent } from '../ui/button';
import { DateFieldComponent } from '../ui/date-field';
import { EmptyStateComponent } from '../ui/empty-state';
import { ErrorMessageComponent } from '../ui/error-message';
import { DataCurtaPipe, DataLongaPipe, MoedaPipe } from '../ui/format.pipes';
import { MoneyFieldComponent } from '../ui/money-field';
import { StatusTagComponent } from '../ui/status-tag';
import { TextFieldComponent } from '../ui/text-field';
import { createCashModel } from './cash-model';
import { MESSAGES } from './messages';
import { validateQuickEntry } from './quick-entry';

const ROW_PX = 26;
const SPAN = 100;

/** Protótipo da tela do caixa (Fase 0.5): dados da fixture, sem backend. A regra real é da Fase 3. */
@Component({
  selector: 'app-caixa',
  imports: [ButtonComponent, DateFieldComponent, EmptyStateComponent, ErrorMessageComponent, MoneyFieldComponent, StatusTagComponent, TextFieldComponent, MoedaPipe, DataCurtaPipe, DataLongaPipe],
  templateUrl: './caixa.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown)': 'onKeydown($event)' },
})
export class CaixaPage {
  protected readonly model = createCashModel();
  protected readonly text = MESSAGES;
  protected readonly span = SPAN;
  protected readonly selectedDate = signal<IsoDate>(this.model.today);

  protected readonly formOpen = signal(false);
  protected readonly formError = signal<string | null>(null);
  protected readonly description = signal('');
  protected readonly amountCents = signal<number | null>(null);
  protected readonly entryDate = signal<IsoDate>(this.model.today);

  private readonly injector = inject(Injector);

  protected readonly height = computed(() => this.model.series().length * ROW_PX);

  /** O tempo desce e o saldo avança para a direita (a geometria horizontal, transposta). */
  protected readonly curve = computed(() => {
    const timeline = buildTimeline(this.model.series(), { width: this.height(), height: SPAN, paddingX: ROW_PX / 2, paddingY: 6 });
    const points = timeline.points.map((p) => ({ x: SPAN - p.y, y: p.x }));
    return { points, path: points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x} ${p.y}`).join(' '), zeroX: SPAN - timeline.zeroY };
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

  protected readonly showOverdue = computed(() => this.selectedDate() === this.model.today && this.model.overdue().length > 0);

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
    this.entryDate.set(this.selectedDate());
    this.formOpen.set(true);
    afterNextRender(() => document.querySelector<HTMLInputElement>('app-caixa form input')?.focus(), { injector: this.injector });
  }

  protected closeForm(): void {
    this.formOpen.set(false);
  }

  protected submit(event: Event): void {
    event.preventDefault();
    const result = validateQuickEntry({ description: this.description(), amountCents: this.amountCents(), date: this.entryDate() });
    if ('error' in result) {
      this.formError.set(result.error);
      return;
    }
    this.model.add(result.entry);
    this.selectedDate.set(result.entry.date);
    this.description.set('');
    this.amountCents.set(null);
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
