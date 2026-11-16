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
};

const FALLBACK = 'Algo deu errado. Tente novamente.';

export function problemMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const code = (error.error as { code?: unknown } | null)?.code;
    if (typeof code === 'string' && code in MESSAGES) {
      return MESSAGES[code];
    }
  }
  return FALLBACK;
}
