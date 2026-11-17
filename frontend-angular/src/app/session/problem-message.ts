import { HttpErrorResponse } from '@angular/common/http';

/** Códigos de erro estáveis da API, traduzidos para texto de tela. */
const MESSAGES: Record<string, string> = {
  EMAIL_ALREADY_REGISTERED: 'Este e-mail já está cadastrado.',
  INVALID_CREDENTIALS: 'E-mail ou senha incorretos.',
  TOO_MANY_REQUESTS: 'Muitas tentativas. Aguarde um pouco e tente de novo.',
  REQUIRED: 'Preencha este campo.',
  TOO_SHORT: 'Valor curto demais.',
  TOO_LONG: 'Valor longo demais.',
  INVALID_FORMAT: 'Formato inválido.',
  INVALID_REQUEST: 'Dados inválidos.',
  UNAUTHENTICATED: 'Sua sessão expirou. Entre novamente.',
  ACCESS_DENIED: 'Você não tem acesso a isto.',
  NOT_FOUND: 'Não encontramos isto. Volte para a fila e tente de novo.',
  PHONE_ALREADY_REGISTERED: 'Já existe um cliente com este telefone. Procure pelo nome na lista.',
  PLATE_ALREADY_REGISTERED: 'Já existe um veículo com esta placa. Escolha-o na lista.',
  QUOTE_INVALID_STATE: 'Este orçamento não aceita mais esta ação. Atualize a página.',
  QUOTE_EXPIRED: 'O prazo deste orçamento já passou.',
  QUOTE_ALREADY_DECIDED: 'Este orçamento já foi respondido de outra forma.',
  LINK_UNAVAILABLE: 'Este link não está mais disponível. Peça um novo à oficina.',
};

const FIELD_LABELS: Record<string, string> = {
  name: 'Nome',
  contactName: 'Nome',
  phone: 'Telefone',
  email: 'E-mail',
  licensePlate: 'Placa',
  plate: 'Placa',
  brand: 'Marca',
  model: 'Modelo',
  year: 'Ano',
  odometerKm: 'Quilometragem',
  description: 'Descrição',
  kind: 'Tipo',
  quantity: 'Quantidade',
  unitPrice: 'Preço',
  amount: 'Valor',
  note: 'Observação',
  reason: 'Motivo',
  validityDays: 'Validade',
  displayName: 'Nome da oficina',
  businessPhone: 'Telefone da oficina',
  address: 'Endereço',
  openingHours: 'Horário',
  signatureMessage: 'Mensagem final',
  items: 'Itens',
};

const FIELD_PROBLEMS: Record<string, string> = {
  REQUIRED: 'preencha este campo',
  TOO_SHORT: 'curto demais',
  TOO_LONG: 'longo demais',
  INVALID_FORMAT: 'formato inválido',
};

const FALLBACK = 'Algo deu errado. Tente novamente.';
const ITEM_FIELD = /^items\[(\d+)]\.(\w+)$/;

/** "items[1].quantity" vira "Item 2, quantidade"; um campo simples vira o rótulo da tela. */
function fieldLabel(field: string): string {
  const item = ITEM_FIELD.exec(field);
  if (item) {
    const label = (FIELD_LABELS[item[2]] ?? item[2]).toLowerCase();
    return `Item ${Number(item[1]) + 1}, ${label}`;
  }
  return FIELD_LABELS[field] ?? 'Um campo';
}

export function problemMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error as { code?: unknown; field?: unknown } | null;
    const code = body?.code;
    if (error.status === 422 && typeof body?.field === 'string' && typeof code === 'string') {
      return `${fieldLabel(body.field)}: ${FIELD_PROBLEMS[code] ?? 'valor inválido'}.`;
    }
    if (typeof code === 'string' && code in MESSAGES) {
      return MESSAGES[code];
    }
  }
  return FALLBACK;
}

/** Só o código estável da API, para a tela decidir o que fazer (por exemplo, link indisponível). */
export function problemCode(error: unknown): string | null {
  if (error instanceof HttpErrorResponse) {
    const code = (error.error as { code?: unknown } | null)?.code;
    return typeof code === 'string' ? code : null;
  }
  return null;
}
