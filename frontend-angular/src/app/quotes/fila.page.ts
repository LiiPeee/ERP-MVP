import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { QuoteQueue, QuoteSummary, QuotesService } from '../api';
import { problemMessage } from '../session/problem-message';
import { formatDay } from '../shared/dates';
import { formatMoney } from '../shared/money-text';
import { EmptyStateComponent } from '../ui/empty-state';
import { NoticeComponent } from '../ui/notice';
import { SkeletonComponent } from '../ui/skeleton';
import { StatusTagComponent } from '../ui/status-tag';

interface Group {
  readonly id: 'needsOwner' | 'awaitingClient' | 'closed';
  readonly title: string;
  readonly empty: { title: string; guidance: string };
}

const GROUPS: readonly Group[] = [
  {
    id: 'needsOwner',
    title: 'Precisam de você',
    empty: { title: 'Nada esperando por você.', guidance: 'Quando um orçamento ficar pronto para confirmar, ele aparece aqui.' },
  },
  {
    id: 'awaitingClient',
    title: 'Esperando o cliente',
    empty: { title: 'Nenhum orçamento esperando resposta.', guidance: 'Depois que você enviar o link, o orçamento espera aqui.' },
  },
  {
    id: 'closed',
    title: 'Fechados',
    empty: { title: 'Nada fechado ainda.', guidance: 'Aprovados, recusados, vencidos e cancelados ficam aqui.' },
  },
];

/** A tela inicial: o que precisa do dono primeiro. Uma linha por orçamento, sem cartão. */
@Component({
  selector: 'app-fila-page',
  imports: [RouterLink, StatusTagComponent, EmptyStateComponent, NoticeComponent, SkeletonComponent],
  template: `
    <header class="page-head">
      <h1>Orçamentos</h1>
      <a class="action-link" routerLink="/orcamento/novo">Novo orçamento</a>
    </header>
    @if (error()) {
      <erp-notice tone="alert">{{ error() }}</erp-notice>
    } @else if (queue(); as current) {
      @for (group of groups; track group.id) {
        <section [attr.aria-labelledby]="'grupo-' + group.id">
          <h2 [id]="'grupo-' + group.id">{{ group.title }} <span class="count">{{ current[group.id]?.length ?? 0 }}</span></h2>
          @if (current[group.id]?.length) {
            <ul class="queue">
              @for (quote of current[group.id]; track quote.id) {
                <li>
                  <a [routerLink]="['/orcamento', quote.id]">
                    <span class="vehicle">{{ quote.vehicleLabel }}</span>
                    <span class="who">{{ quote.contactName }}</span>
                    <span class="when">{{ when(quote) }}</span>
                    <span class="total num">{{ money(quote.total) }}</span>
                    <erp-status-tag [status]="quote.status ?? ''" />
                  </a>
                </li>
              }
            </ul>
          } @else {
            <erp-empty-state [title]="group.empty.title" [guidance]="group.empty.guidance" />
          }
        </section>
      }
    } @else {
      <erp-skeleton [lines]="4" label="Carregando os orçamentos" />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FilaPage implements OnInit {
  private readonly quotes = inject(QuotesService);

  protected readonly groups = GROUPS;
  protected readonly queue = signal<QuoteQueue | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly money = formatMoney;

  async ngOnInit(): Promise<void> {
    try {
      this.queue.set(await firstValueFrom(this.quotes.listQuotes()));
    } catch (failure) {
      this.error.set(problemMessage(failure));
    }
  }

  /** Quando importa para o dono: o prazo de quem espera, o dia do envio ou da criação nos demais. */
  protected when(quote: QuoteSummary): string {
    if (quote.status === 'SENT') {
      return `vale até ${formatDay(quote.expiresAt)}`;
    }
    return quote.sentAt ? `enviado em ${formatDay(quote.sentAt)}` : `criado em ${formatDay(quote.createdAt)}`;
  }
}
