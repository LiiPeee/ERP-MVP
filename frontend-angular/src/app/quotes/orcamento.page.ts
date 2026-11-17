import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { QuoteSentResult, QuoteView, QuotesService } from '../api';
import { problemMessage } from '../session/problem-message';
import { formatDay, formatValidUntil } from '../shared/dates';
import { formatMoney } from '../shared/money-text';
import { ButtonComponent } from '../ui/button';
import { ConfirmDialogService } from '../ui/confirm-dialog.service';
import { NoticeComponent } from '../ui/notice';
import { SkeletonComponent } from '../ui/skeleton';
import { StatusTagComponent } from '../ui/status-tag';
import { ItemRow, QuoteItemsEditorComponent, rowsToRequests } from './quote-items-editor';

/** Um orçamento do ponto de vista do dono: o que ele diz, em que pé está e o que dá para fazer agora. */
@Component({
  selector: 'app-orcamento-page',
  imports: [RouterLink, StatusTagComponent, ButtonComponent, NoticeComponent, SkeletonComponent, QuoteItemsEditorComponent],
  template: `
    <p><a routerLink="/fila">← Orçamentos</a></p>
    @if (quote(); as current) {
      <header class="quote-head">
        <h1>{{ current.vehicleLabel }}</h1>
        <erp-status-tag [status]="current.status ?? ''" />
      </header>
      <p class="who">{{ current.contactName }} <span class="num">{{ current.contactPhoneMasked }}</span></p>

      @if (editing()) {
        <erp-quote-items-editor [(rows)]="rows" />
        <div class="actions">
          <button erpButton variant="primary" type="button" (click)="saveItems()" [disabled]="busy()">Salvar itens</button>
          <button erpButton type="button" (click)="editing.set(false)">Descartar mudanças</button>
        </div>
      } @else {
        <ul class="lines">
          @for (item of current.items; track $index) {
            <li>
              <span class="what">{{ item.description }}</span>
              <span class="how num">{{ item.quantity }} × {{ money(item.unitPrice) }}</span>
              <span class="sum num">{{ money(item.lineTotal) }}</span>
            </li>
          }
        </ul>
        <p class="total">Total <strong class="num">{{ money(current.total) }}</strong></p>
        @if (current.note) {
          <p class="note">{{ current.note }}</p>
        }
      }

      @switch (current.status) {
        @case ('SENT') {
          <p>{{ validUntil(current) }} · enviado em {{ day(current.sentAt) }}</p>
        }
        @case ('APPROVED') {
          <erp-notice>O cliente aprovou em {{ day(current.decidedAt) }}.</erp-notice>
        }
        @case ('REJECTED') {
          <erp-notice tone="alert">
            O cliente recusou em {{ day(current.decidedAt) }}.
            @if (current.decisionReason) { Motivo: {{ current.decisionReason }} }
          </erp-notice>
        }
        @case ('EXPIRED') {
          <erp-notice tone="alert">O prazo passou sem resposta.</erp-notice>
        }
      }

      @if (link(); as sent) {
        <section class="link-box" aria-labelledby="titulo-link">
          <h2 id="titulo-link">Link para o cliente</h2>
          <p class="hint">Mande este link pelo WhatsApp. Ele só aparece agora: se perder, gere outro.</p>
          <input class="link-field num" type="text" readonly aria-label="Link do orçamento" [value]="sent.publicUrl" />
          <button erpButton variant="primary" type="button" (click)="copy(sent.publicUrl ?? '')">Copiar link</button>
          @if (copied()) {
            <p role="status">Link copiado.</p>
          }
        </section>
      }

      @if (error()) {
        <erp-notice tone="alert">{{ error() }}</erp-notice>
      }

      <div class="actions actions--bottom">
        @if (isOpenForEditing(current)) {
          <button erpButton variant="primary" type="button" (click)="confirm()" [disabled]="busy()">Confirmar e gerar link</button>
          @if (!editing()) {
            <button erpButton type="button" (click)="edit(current)">Editar itens</button>
          }
        }
        @if (current.status === 'SENT') {
          <button erpButton variant="primary" type="button" (click)="resend()" [disabled]="busy()">Gerar novo link</button>
        }
        @if (isCancellable(current)) {
          <button erpButton type="button" (click)="cancel()" [disabled]="busy()">Cancelar orçamento</button>
        }
      </div>
    } @else if (loadError()) {
      <erp-notice tone="alert">{{ loadError() }}</erp-notice>
    } @else {
      <erp-skeleton [lines]="4" label="Carregando o orçamento" />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrcamentoPage implements OnInit {
  /** O id vem da rota (withComponentInputBinding). */
  readonly id = input.required<string>();

  private readonly quotes = inject(QuotesService);
  private readonly dialog = inject(ConfirmDialogService);

  protected readonly quote = signal<QuoteView | null>(null);
  protected readonly loadError = signal<string | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly editing = signal(false);
  protected readonly rows = signal<readonly ItemRow[]>([]);
  protected readonly link = signal<QuoteSentResult | null>(null);
  protected readonly copied = signal(false);
  protected readonly money = formatMoney;
  protected readonly day = formatDay;

  async ngOnInit(): Promise<void> {
    try {
      this.quote.set(await firstValueFrom(this.quotes.getQuote(this.id())));
    } catch (failure) {
      this.loadError.set(problemMessage(failure));
    }
  }

  protected isOpenForEditing(quote: QuoteView): boolean {
    return quote.status === 'DRAFT' || quote.status === 'AWAITING_OWNER_CONFIRMATION';
  }

  protected isCancellable(quote: QuoteView): boolean {
    return this.isOpenForEditing(quote) || quote.status === 'SENT';
  }

  protected validUntil(quote: QuoteView): string {
    return formatValidUntil(quote.expiresAt);
  }

  protected edit(quote: QuoteView): void {
    this.rows.set(
      (quote.items ?? []).map((item) => ({
        description: item.description ?? '',
        kind: item.kind ?? 'LABOR',
        quantity: item.quantity ?? '1',
        unitPrice: item.unitPrice ?? '',
      })),
    );
    this.editing.set(true);
  }

  protected async saveItems(): Promise<void> {
    const converted = rowsToRequests(this.rows());
    if (converted.error !== null) {
      this.error.set(converted.error);
      return;
    }
    await this.run(async () => {
      this.quote.set(await firstValueFrom(this.quotes.reviseQuoteItems(this.id(), { items: converted.items })));
      this.editing.set(false);
    });
  }

  protected async confirm(): Promise<void> {
    const yes = await this.dialog.confirm({
      title: 'Confirmar e gerar o link?',
      message: 'O orçamento passa a valer e você recebe o link para mandar ao cliente. Depois disso os itens não mudam.',
      confirmLabel: 'Confirmar e gerar link',
      cancelLabel: 'Voltar',
    });
    if (yes) {
      await this.run(async () => {
        this.link.set(await firstValueFrom(this.quotes.confirmQuote(this.id())));
        this.quote.set(await firstValueFrom(this.quotes.getQuote(this.id())));
      });
    }
  }

  protected async resend(): Promise<void> {
    await this.run(async () => {
      this.link.set(await firstValueFrom(this.quotes.resendQuote(this.id())));
      this.quote.set(await firstValueFrom(this.quotes.getQuote(this.id())));
    });
  }

  protected async cancel(): Promise<void> {
    const yes = await this.dialog.confirm({
      title: 'Cancelar este orçamento?',
      message: 'O link deixa de funcionar e o cliente não consegue mais responder. Isto não pode ser desfeito.',
      confirmLabel: 'Cancelar orçamento',
      cancelLabel: 'Voltar',
    });
    if (yes) {
      await this.run(async () => {
        this.quote.set(await firstValueFrom(this.quotes.cancelQuote(this.id())));
        this.link.set(null);
      });
    }
  }

  protected async copy(text: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      this.copied.set(true);
    } catch {
      this.error.set('Não deu para copiar. Selecione o link e copie à mão.');
    }
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      await action();
    } catch (failure) {
      this.error.set(problemMessage(failure));
    } finally {
      this.busy.set(false);
    }
  }
}
