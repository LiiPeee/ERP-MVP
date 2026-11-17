import { Dialog, DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ButtonComponent } from './button';

export interface ConfirmRequest {
  readonly title: string;
  readonly message: string;
  readonly confirmLabel: string;
  readonly cancelLabel: string;
}

const TITLE_ID = 'erp-dialog-title';

@Component({
  selector: 'erp-confirm-dialog',
  imports: [ButtonComponent],
  template: `
    <h2 [id]="titleId">{{ data.title }}</h2>
    <p>{{ data.message }}</p>
    <div class="actions">
      <button erpButton type="button" (click)="ref.close(false)">{{ data.cancelLabel }}</button>
      <button erpButton variant="primary" type="button" cdkFocusInitial (click)="ref.close(true)">{{ data.confirmLabel }}</button>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: `
    :host { display: block; min-width: 20rem; max-width: 28rem; padding: var(--space-5); border: var(--line-strong) solid var(--color-ink); border-radius: var(--radius); background: var(--color-paper); color: var(--color-ink); }
    h2 { margin: 0 0 var(--space-3); font-size: var(--text-l); font-weight: var(--weight-heavy); }
    p { margin: 0 0 var(--space-5); color: var(--color-ink-muted); }
    .actions { display: flex; justify-content: flex-end; gap: var(--space-3); }
  `,
})
export class ConfirmDialogComponent {
  protected readonly data = inject<ConfirmRequest>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<boolean>>(DialogRef);
  protected readonly titleId = TITLE_ID;
}

/** Diálogo modal sobre o CDK: foco preso, Esc fecha, foco volta a quem abriu. Esc e clique fora contam como "não". */
@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private readonly dialog = inject(Dialog);

  async confirm(request: ConfirmRequest): Promise<boolean> {
    const ref = this.dialog.open<boolean, ConfirmRequest>(ConfirmDialogComponent, {
      data: request,
      ariaModal: true,
      ariaLabelledBy: TITLE_ID,
      autoFocus: 'first-tabbable',
      restoreFocus: true,
      backdropClass: 'erp-dialog-backdrop',
    });
    return (await firstValueFrom(ref.closed)) === true;
  }
}
