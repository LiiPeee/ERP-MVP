const ZONE = 'America/Sao_Paulo';
const day = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short', timeZone: ZONE });

/** "17 de nov.": dia e mês no fuso da oficina (o servidor guarda tudo em UTC). */
export function formatDay(instant: string | undefined): string {
  if (!instant) {
    return '';
  }
  return day.format(new Date(instant));
}

export function formatValidUntil(instant: string | undefined): string {
  return instant ? `Vale até ${formatDay(instant)}` : '';
}
