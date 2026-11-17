import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/** Os estados do orçamento, como a API os devolve. */
export type Status = 'DRAFT' | 'AWAITING_OWNER_CONFIRMATION' | 'SENT' | 'APPROVED' | 'REJECTED' | 'EXPIRED' | 'CANCELLED';

type Tone = 'pending' | 'waiting' | 'good' | 'bad' | 'quiet';

const LABELS: Record<Status, { text: string; glyph: string; tone: Tone }> = {
  DRAFT: { text: 'rascunho', glyph: '•', tone: 'pending' },
  AWAITING_OWNER_CONFIRMATION: { text: 'confirmar', glyph: '•', tone: 'pending' },
  SENT: { text: 'enviado', glyph: '→', tone: 'waiting' },
  APPROVED: { text: 'aprovado', glyph: '✓', tone: 'good' },
  REJECTED: { text: 'recusado', glyph: '✕', tone: 'bad' },
  EXPIRED: { text: 'vencido', glyph: '!', tone: 'bad' },
  CANCELLED: { text: 'cancelado', glyph: '–', tone: 'quiet' },
};

/** Cor sempre acompanhada de sinal e texto, para não depender só da cor. */
@Component({
  selector: 'erp-status-tag',
  template: '<span class="glyph" aria-hidden="true">{{ label().glyph }}</span>{{ label().text }}',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '[class]': "'status status--' + label().tone" },
})
export class StatusTagComponent {
  /** Um estado que a API ainda não conhecia aparece como cancelado (neutro) em vez de quebrar a tela. */
  readonly status = input.required<Status | string>();
  protected readonly label = computed(() => LABELS[this.status() as Status] ?? LABELS.CANCELLED);
}
