import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { CompanyProfileService } from '../api';
import { problemMessage } from '../session/problem-message';
import { ButtonComponent } from '../ui/button';
import { NoticeComponent } from '../ui/notice';
import { TextFieldComponent } from '../ui/text-field';

/** Os dados da oficina que o cliente vê na página do orçamento. */
@Component({
  selector: 'app-oficina-page',
  imports: [TextFieldComponent, ButtonComponent, NoticeComponent],
  template: `
    <h1>Sua oficina</h1>
    <form (submit)="$event.preventDefault(); save()">
      <erp-text-field label="Nome da oficina" [(value)]="displayName" />
      <erp-text-field label="Telefone da oficina" [hint]="phoneHint()" [(value)]="phone" />
      <erp-text-field label="Endereço" [(value)]="address" />
      <erp-text-field label="Horário de atendimento" [(value)]="hours" />
      <erp-text-field label="Validade do orçamento, em dias" hint="De 1 a 90. Conta a partir do envio." [(value)]="validity" />
      <erp-text-field label="Mensagem no fim do orçamento" [(value)]="signature" />
      <button erpButton variant="primary" type="submit" [disabled]="busy()">Salvar</button>
    </form>
    @if (saved()) {
      <erp-notice>Dados salvos.</erp-notice>
    }
    @if (error()) {
      <erp-notice tone="alert">{{ error() }}</erp-notice>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OficinaPage implements OnInit {
  private readonly profile = inject(CompanyProfileService);

  protected readonly displayName = signal('');
  protected readonly phone = signal('');
  protected readonly address = signal('');
  protected readonly hours = signal('');
  protected readonly validity = signal('7');
  protected readonly signature = signal('');
  protected readonly phoneHint = signal('Por exemplo (11) 3333-4444');
  protected readonly busy = signal(false);
  protected readonly saved = signal(false);
  protected readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      const current = await firstValueFrom(this.profile.getCompanyProfile());
      this.displayName.set(current.displayName ?? '');
      this.address.set(current.address ?? '');
      this.hours.set(current.openingHours ?? '');
      this.validity.set(String(current.defaultValidityDays ?? 7));
      this.signature.set(current.signatureMessage ?? '');
      if (current.businessPhoneMasked) {
        // O telefone completo não volta da API (privacidade): para trocar ou manter, digite de novo.
        this.phoneHint.set(`Hoje: ${current.businessPhoneMasked}. Digite de novo para manter ou trocar; em branco, apaga.`);
      }
    } catch (failure) {
      this.error.set(problemMessage(failure));
    }
  }

  protected async save(): Promise<void> {
    this.busy.set(true);
    this.saved.set(false);
    this.error.set(null);
    try {
      await firstValueFrom(
        this.profile.updateCompanyProfile({
          displayName: this.displayName(),
          businessPhone: this.phone().trim() || undefined,
          address: this.address(),
          openingHours: this.hours(),
          defaultValidityDays: Number(this.validity()),
          signatureMessage: this.signature(),
        }),
      );
      this.saved.set(true);
    } catch (failure) {
      this.error.set(problemMessage(failure));
    } finally {
      this.busy.set(false);
    }
  }
}
