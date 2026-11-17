import { afterNextRender, ApplicationRef, ChangeDetectionStrategy, Component, createComponent, DOCUMENT, ElementRef, EnvironmentInjector, inject, Injectable, input, output, viewChild } from '@angular/core';
import { ButtonComponent } from './button';

export interface ConfirmRequest {
  readonly title: string;
  readonly message: string;
  readonly confirmLabel: string;
  readonly cancelLabel: string;
}

const TITLE_ID = 'erp-dialog-title';

/**
 * Sobre o `<dialog>` nativo: foco preso, Esc que fecha e retorno do foco vêm do navegador, sem injetar estilo
 * (a CSP só aceita `style-src 'self'`). O foco inicial cai no primeiro botão, o de cancelar: o mais seguro.
 */
@Component({
  selector: 'erp-confirm-dialog',
  imports: [ButtonComponent],
  template: `
    <dialog #dialog [attr.aria-labelledby]="titleId" (close)="finish()" (click)="onBackdropClick($event)">
      <div class="panel">
        <h2 [id]="titleId">{{ request().title }}</h2>
        <p>{{ request().message }}</p>
        <div class="actions">
          <button erpButton type="button" (click)="close(false)">{{ request().cancelLabel }}</button>
          <button erpButton variant="primary" type="button" (click)="close(true)">{{ request().confirmLabel }}</button>
        </div>
      </div>
    </dialog>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialogComponent {
  readonly request = input.required<ConfirmRequest>();
  readonly closed = output<boolean>();
  protected readonly titleId = TITLE_ID;
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    afterNextRender(() => this.dialog().nativeElement.showModal());
  }

  protected close(confirmed: boolean): void {
    this.dialog().nativeElement.close(String(confirmed));
  }

  /** O clique no fundo (fora do painel) chega ao próprio <dialog>; conta como "não". */
  protected onBackdropClick(event: MouseEvent): void {
    if (event.target === this.dialog().nativeElement) {
      this.dialog().nativeElement.close('');
    }
  }

  /** Esc fecha sem valor ("" ), o que também conta como "não". */
  protected finish(): void {
    this.closed.emit(this.dialog().nativeElement.returnValue === 'true');
  }
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private readonly appRef = inject(ApplicationRef);
  private readonly injector = inject(EnvironmentInjector);
  private readonly document = inject(DOCUMENT);

  confirm(request: ConfirmRequest): Promise<boolean> {
    return new Promise((resolve) => {
      const ref = createComponent(ConfirmDialogComponent, { environmentInjector: this.injector });
      ref.setInput('request', request);
      ref.instance.closed.subscribe((confirmed) => {
        ref.destroy();
        resolve(confirmed);
      });
      this.appRef.attachView(ref.hostView);
      this.document.body.appendChild(ref.location.nativeElement);
    });
  }
}
