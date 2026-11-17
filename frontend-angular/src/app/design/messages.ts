/** Texto de interface do caixa em um só lugar (pt-BR). Nenhum componente escreve frase própria. */
export const MESSAGES = {
  quickEntry: {
    open: 'Novo lançamento',
    description: 'Descrição',
    amount: 'Valor em reais',
    amountHint: 'Use o sinal de menos para uma saída, como −80,00.',
    date: 'Data',
    submit: 'Lançar',
    cancel: 'Cancelar',
    descriptionRequired: 'Diga o que é este lançamento, por exemplo "Farinha".',
    amountInvalid: 'Digite o valor em reais, como 1.200,50. Use o sinal de menos para uma saída.',
    amountZero: 'O valor precisa ser diferente de zero.',
    dateInvalid: 'Escolha uma data que exista no calendário.',
  },
  kind: { receivable: 'a receber', payable: 'a pagar' },
  overdue: 'Atrasados',
  overdueSince: 'venceu em',
  empty: 'Nenhum lançamento neste dia. Use N para lançar o primeiro.',
  balanceAtEndOfDay: 'Saldo no fim do dia',
  cashEnds: 'O caixa acaba aqui',
  today: 'hoje',
} as const;
