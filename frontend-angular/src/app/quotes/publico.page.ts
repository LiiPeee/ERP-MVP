import { ChangeDetectionStrategy, Component, OnInit, inject, input, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { PublicQuoteDecision, PublicQuoteLinkService, PublicQuoteView } from '../api';
import { problemCode, problemMessage } from '../session/problem-message';
import { formatValidUntil } from '../shared/dates';
import { formatMoney } from '../shared/money-text';
import { ButtonComponent } from '../ui/button';
import { NoticeComponent } from '../ui/notice';
import { SkeletonComponent } from '../ui/skeleton';
import { TextFieldComponent } from '../ui/text-field';

/**
 * O que o cliente vê ao abrir o link do WhatsApp. Sem menu, sem login e com pouco JavaScript. Abrir a página não
 * decide nada: só os botões decidem. Link que não serve (vencido, cancelado ou errado) tem uma só mensagem.
 */
@Component({
  selector: 'app-publico-page',
  imports: [ButtonComponent, NoticeComponent, SkeletonComponent, TextFieldComponent],
  template: `
    @if (unavailable()) {
      <h1>Este link não está mais disponível</h1>
      <p>Peça um novo link à oficina.</p>
    } @else if (quote(); as current) {
      <p class="shop">{{ current.shopName }}</p>
      <h1>Orçamento para {{ current.vehicleLabel }}</h1>
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

      @switch (current.status) {
        @case ('SENT') {
          <p class="valid">{{ validUntil(current) }}</p>
          @if (rejecting()) {
            <erp-text-field label="Quer contar o motivo? (opcional)" [(value)]="reason" />
            <div class="actions actions--bottom">
              <button erpButton variant="primary" type="button" (click)="reject()" [disabled]="busy()">Confirmar recusa</button>
              <button erpButton type="button" (click)="rejecting.set(false)">Voltar</button>
            </div>
          } @else {
            <div class="actions actions--bottom">
              <button erpButton variant="primary" type="button" (click)="approve()" [disabled]="busy()">Aprovar orçamento</button>
              <button erpButton type="button" (click)="rejecting.set(true)">Recusar</button>
            </div>
          }
        }
        @case ('APPROVED') {
          <erp-notice>Orçamento aprovado. A oficina já foi avisada.</erp-notice>
        }
        @case ('REJECTED') {
          <erp-notice>Orçamento recusado. A oficina já foi avisada.</erp-notice>
        }
        @default {
          <erp-notice tone="alert">Este orçamento não está mais aberto. Peça um novo à oficina.</erp-notice>
        }
      }
      @if (error()) {
        <erp-notice tone="alert">{{ error() }}</erp-notice>
      }
    } @else if (error()) {
      <erp-notice tone="alert">{{ error() }}</erp-notice>
    } @else {
      <erp-skeleton [lines]="4" label="Abrindo o orçamento" />
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicoPage implements OnInit {
  /** O token do link, vindo do endereço (withComponentInputBinding). */
  readonly token = input.required<string>();

  private readonly link = inject(PublicQuoteLinkService);

  protected readonly quote = signal<PublicQuoteView | null>(null);
  protected readonly unavailable = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly busy = signal(false);
  protected readonly rejecting = signal(false);
  protected readonly reason = signal('');
  protected readonly money = formatMoney;

  async ngOnInit(): Promise<void> {
    try {
      this.quote.set(await firstValueFrom(this.link.getPublicQuote(this.token())));
    } catch (failure) {
      this.fail(failure);
    }
  }

  protected validUntil(quote: PublicQuoteView): string {
    return formatValidUntil(quote.expiresAt);
  }

  protected approve(): Promise<void> {
    return this.decide(() => this.link.approveQuoteByLink(this.token()));
  }

  protected reject(): Promise<void> {
    return this.decide(() => this.link.rejectQuoteByLink(this.token(), { reason: this.reason().trim() || undefined }));
  }

  private async decide(call: () => Observable<PublicQuoteDecision>): Promise<void> {
    this.busy.set(true);
    this.error.set(null);
    try {
      const decision = await firstValueFrom(call());
      this.quote.update((current) => (current ? { ...current, status: decision.status } : current));
    } catch (failure) {
      this.fail(failure);
    } finally {
      this.busy.set(false);
    }
  }

  private fail(failure: unknown): void {
    if (problemCode(failure) === 'LINK_UNAVAILABLE') {
      this.unavailable.set(true);
    } else {
      this.error.set(problemMessage(failure));
    }
  }
}
