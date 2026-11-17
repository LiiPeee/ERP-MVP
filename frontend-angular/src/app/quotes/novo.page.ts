import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ContactView, ContactsService, QuotesService, VehicleView, VehiclesService } from '../api';
import { problemMessage } from '../session/problem-message';
import { ButtonComponent } from '../ui/button';
import { NoticeComponent } from '../ui/notice';
import { TextFieldComponent } from '../ui/text-field';
import { EMPTY_ROW, ItemRow, QuoteItemsEditorComponent, rowsToRequests } from './quote-items-editor';

/**
 * Orçamento novo em três passos numa tela só: cliente, veículo, itens. O mínimo de digitação: quem já é cliente
 * se acha pelo nome; o cadastro novo pede só nome e telefone (e placa, marca, modelo e ano do veículo).
 */
@Component({
  selector: 'app-novo-page',
  imports: [TextFieldComponent, ButtonComponent, NoticeComponent, QuoteItemsEditorComponent],
  template: `
    <h1>Novo orçamento</h1>

    <section aria-labelledby="passo-cliente">
      <h2 id="passo-cliente">1. Cliente</h2>
      @if (contact(); as chosen) {
        <p class="chosen">{{ chosen.name }} <span class="num">{{ chosen.phoneMasked }}</span></p>
        <button erpButton type="button" (click)="changeContact()">Trocar o cliente</button>
      } @else {
        <form class="inline-form" (submit)="$event.preventDefault(); search()">
          <erp-text-field label="Procurar pelo nome" [(value)]="searchText" />
          <button erpButton type="submit">Procurar</button>
        </form>
        @if (found().length) {
          <ul class="pick-list">
            @for (item of found(); track item.id) {
              <li>
                <button type="button" (click)="chooseContact(item)">
                  <span>{{ item.name }}</span> <span class="num">{{ item.phoneMasked }}</span>
                </button>
              </li>
            }
          </ul>
        } @else if (searched()) {
          <p class="hint">Ninguém com esse nome. Cadastre abaixo.</p>
        }
        <h3>Cliente novo</h3>
        <erp-text-field label="Nome" [(value)]="newName" />
        <erp-text-field label="Telefone com DDD" hint="Por exemplo (11) 91234-5678" [(value)]="newPhone" />
        <button erpButton type="button" (click)="createContact()" [disabled]="busy()">Cadastrar cliente</button>
      }
    </section>

    @if (contact()) {
      <section aria-labelledby="passo-veiculo">
        <h2 id="passo-veiculo">2. Veículo</h2>
        @if (vehicle(); as chosen) {
          <p class="chosen">{{ chosen.brand }} {{ chosen.model }} {{ chosen.year }} <span class="num">{{ chosen.plateMasked }}</span></p>
          <button erpButton type="button" (click)="vehicle.set(null)">Trocar o veículo</button>
        } @else {
          @if (vehicles().length) {
            <ul class="pick-list">
              @for (item of vehicles(); track item.id) {
                <li>
                  <button type="button" (click)="vehicle.set(item)">
                    <span>{{ item.brand }} {{ item.model }} {{ item.year }}</span> <span class="num">{{ item.plateMasked }}</span>
                  </button>
                </li>
              }
            </ul>
          }
          <h3>Veículo novo</h3>
          <erp-text-field label="Placa" hint="Antiga ou Mercosul, por exemplo ABC1D23" [(value)]="plate" />
          <erp-text-field label="Marca" [(value)]="brand" />
          <erp-text-field label="Modelo" [(value)]="model" />
          <erp-text-field label="Ano" [(value)]="year" />
          <button erpButton type="button" (click)="registerVehicle()" [disabled]="busy()">Cadastrar veículo</button>
        }
      </section>
    }

    @if (contact() && vehicle()) {
      <section aria-labelledby="passo-itens">
        <h2 id="passo-itens">3. Itens</h2>
        <erp-quote-items-editor [(rows)]="rows" />
        <erp-text-field label="Observação para o cliente (opcional)" [(value)]="note" />
        <p class="hint">O total é calculado quando você salva o rascunho.</p>
        <button erpButton variant="primary" type="button" (click)="save()" [disabled]="busy()">Salvar rascunho</button>
      </section>
    }

    @if (error()) {
      <erp-notice tone="alert">{{ error() }}</erp-notice>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NovoPage implements OnInit {
  private readonly contacts = inject(ContactsService);
  private readonly vehicles$ = inject(VehiclesService);
  private readonly quotes = inject(QuotesService);
  private readonly router = inject(Router);

  protected readonly searchText = signal('');
  protected readonly found = signal<ContactView[]>([]);
  protected readonly searched = signal(false);
  protected readonly contact = signal<ContactView | null>(null);
  protected readonly vehicles = signal<VehicleView[]>([]);
  protected readonly vehicle = signal<VehicleView | null>(null);
  protected readonly newName = signal('');
  protected readonly newPhone = signal('');
  protected readonly plate = signal('');
  protected readonly brand = signal('');
  protected readonly model = signal('');
  protected readonly year = signal('');
  protected readonly rows = signal<readonly ItemRow[]>([EMPTY_ROW]);
  protected readonly note = signal('');
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    await this.search();
    this.searched.set(false);
  }

  protected async search(): Promise<void> {
    await this.run(async () => {
      this.found.set(await firstValueFrom(this.contacts.listContacts(this.searchText().trim() || undefined)));
      this.searched.set(true);
    });
  }

  protected async createContact(): Promise<void> {
    await this.run(async () => {
      const created = await firstValueFrom(
        this.contacts.createContact({ name: this.newName(), phone: this.newPhone() }),
      );
      await this.chooseContact(created);
    });
  }

  protected async chooseContact(chosen: ContactView): Promise<void> {
    this.contact.set(chosen);
    this.vehicle.set(null);
    await this.run(async () => {
      this.vehicles.set(await firstValueFrom(this.vehicles$.listVehicles(chosen.id ?? '')));
    });
  }

  protected changeContact(): void {
    this.contact.set(null);
    this.vehicle.set(null);
    this.vehicles.set([]);
  }

  protected async registerVehicle(): Promise<void> {
    const year = Number(this.year());
    await this.run(async () => {
      const created = await firstValueFrom(
        this.vehicles$.registerVehicle(this.contact()?.id ?? '', {
          plate: this.plate().trim().toUpperCase(),
          brand: this.brand(),
          model: this.model(),
          year: Number.isInteger(year) ? year : undefined,
        }),
      );
      this.vehicles.update((list) => [...list, created]);
      this.vehicle.set(created);
    });
  }

  protected async save(): Promise<void> {
    const converted = rowsToRequests(this.rows());
    if (converted.error !== null) {
      this.error.set(converted.error);
      return;
    }
    await this.run(async () => {
      const draft = await firstValueFrom(
        this.quotes.createQuoteDraft({
          contactId: this.contact()?.id,
          vehicleId: this.vehicle()?.id,
          items: converted.items,
          note: this.note().trim() || undefined,
        }),
      );
      await this.router.navigate(['/orcamento', draft.id]);
    });
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
