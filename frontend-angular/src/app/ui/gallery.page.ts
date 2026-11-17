import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ButtonComponent } from './button';
import { ConfirmDialogService } from './confirm-dialog.service';
import { DataTableComponent, TableColumn } from './data-table';
import { DateFieldComponent } from './date-field';
import { EmptyStateComponent } from './empty-state';
import { DataCurtaPipe, MoedaPipe } from './format.pipes';
import { MoneyFieldComponent } from './money-field';
import { NavComponent } from './nav';
import { NoticeComponent } from './notice';
import { SelectFieldComponent } from './select-field';
import { SkeletonComponent } from './skeleton';
import { StatusTagComponent } from './status-tag';
import { TextFieldComponent } from './text-field';
import { FIXTURE_ENTRIES } from '../caixa/fixture';
import { CashEntry } from '../cash/projection';

/** Galeria dos componentes base: serve para olhar, medir contraste e testar teclado. Sai do produto na Fase 1. */
@Component({
  selector: 'app-gallery',
  imports: [ButtonComponent, DataTableComponent, DateFieldComponent, EmptyStateComponent, MoneyFieldComponent, NavComponent, NoticeComponent, SelectFieldComponent, SkeletonComponent, StatusTagComponent, TextFieldComponent],
  templateUrl: './gallery.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GalleryPage {
  private readonly dialog = inject(ConfirmDialogService);
  private readonly money = new MoedaPipe();
  private readonly shortDate = new DataCurtaPipe();

  protected readonly navItems = [
    { label: 'Caixa', link: '/caixa' },
    { label: 'Componentes', link: '/componentes' },
  ];
  protected readonly typeOptions = [
    { value: 'receivable', label: 'A receber' },
    { value: 'payable', label: 'A pagar' },
  ];
  protected readonly dialogAnswer = signal<string | null>(null);
  protected readonly rows: readonly CashEntry[] = FIXTURE_ENTRIES.filter((entry) => entry.date >= '2026-03-02').slice(0, 12);
  protected readonly rowKey = (row: CashEntry) => row.id;
  protected readonly columns: TableColumn<CashEntry>[] = [
    { key: 'date', label: 'Data', value: (r) => r.date, display: (r) => this.shortDate.transform(r.date) },
    { key: 'description', label: 'Descrição', value: (r) => r.description },
    { key: 'amount', label: 'Valor', align: 'end', value: (r) => r.amountCents, display: (r) => this.money.transform(r.amountCents, true) },
  ];

  protected async askToDelete(): Promise<void> {
    const confirmed = await this.dialog.confirm({
      title: 'Apagar lançamento?',
      message: 'Esta ação não pode ser desfeita.',
      confirmLabel: 'Apagar',
      cancelLabel: 'Manter',
    });
    this.dialogAnswer.set(confirmed ? 'Você apagou o lançamento.' : 'Você manteve o lançamento.');
  }
}
