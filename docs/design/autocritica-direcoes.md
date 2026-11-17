# Autocrítica das duas direções (ponto de verificação 2)

Capturas em `docs/capturas/fase-0.5/` (desktop 1280x800 e celular 390x844, página inteira). Rotas de laboratório: `/design/a` e `/design/b`, só com a fixture sintética. Feita por mim olhando as capturas; **não é uma avaliação de design de terceiros**.

## Verificações objetivas

| Item | A, Livro-caixa | B, Mesa de controle |
|---|---|---|
| axe (WCAG 2.0/2.1 A e AA), desktop e celular | 0 violações | 0 violações |
| Lançamento rápido só pelo teclado (N, digitar, Tab, Enter) | passa | passa |
| Erro de valor inválido anunciado (`role="alert"`) e Esc fecha | passa | passa |
| J e K mudam o dia | não se aplica | passa |
| Requisições fora da origem (fontes, scripts) | nenhuma | nenhuma |
| Testes unitários (funções puras e modelo) | 54 passando | os mesmos |

Limite do axe: ele não pega tudo. Contraste foi aprovado pela regra do axe, mas **não medi cada par de cores à mão**, e não testei leitor de tela de verdade.

## Lista de "sinais de interface genérica" do CLAUDE.md, item a item

| Sinal | A | B |
|---|---|---|
| Barra lateral escura + barra superior branca + 4 cartões + gráfico de linha + tabela | Não há | Não há (há uma curva, mas é o eixo da tela, não um gráfico decorativo) |
| Gradiente roxo ou azul | Não | Não |
| Glassmorphism | Não | Não |
| Sombra e cantos arredondados em tudo | Nenhuma sombra; raio de 2 px só em botões e campos | Nenhuma sombra; raio de 3 px só em botões e campos |
| Cartões dentro de cartões | Não há cartões | Não há cartões |
| Inter ou Roboto sem decisão | Newsreader + IBM Plex Sans, escolhidas | Schibsted Grotesk + IBM Plex Mono, escolhidas |
| Emoji como ícone | Não | Não |
| Ilustração de banco de imagens | Não | Não |
| Lorem ipsum | Não | Não |
| Texto vago | Não ("Você tem caixa para 24 dias") | Não ("24 dias de caixa") |
| Botão "Enviar" | "Lançar" | "Lançar" |
| Estado vazio só com ícone | Dia sem lançamento não existe na lista | Texto com orientação ("Use N para lançar o primeiro") |
| Dados de exemplo óbvios | "Fornecedor Fictício 1", "Cliente Fictício A": **falso de propósito**, mas óbvio | Igual |

## Pontos fracos que eu vejo

**A, Livro-caixa**
- Funciona bem como leitura, mas a lista é **longa e repetitiva**: 45 dias viram uma rolagem enorme, e a faixa de saldo some do alto da tela quando se rola.
- A faixa é fina; vencimentos grandes e pequenos se leem pelo comprimento das marcas, o que é sutil no celular.
- A serifada dá personalidade, mas o conjunto está **perto de "documento"**: é elegante e pouco interativo.
- Os rótulos "a pagar" e "a receber" repetidos em toda linha pesam; dá para tirar e deixar só o sinal e a cor.

**B, Mesa de controle**
- A curva ocupa só 150 px; há **muito espaço vazio** entre a curva e o saldo do dia na linha. A zona hachurada também só cobre a coluna da curva, não a linha inteira.
- A resposta ("24 dias de caixa") é forte, mas o "dia em que o caixa acaba" vive em outro ponto da tela (na linha, quase 800 px abaixo); em tela baixa é preciso rolar para ver.
- No celular, o painel do dia vem antes da linha do tempo e a curva fica comprimida; funciona, mas é a parte menos resolvida.
- A mono em todos os valores dá clareza, mas deixa o painel com cara de **ferramenta técnica**, o risco que eu já apontava.

## O que eu escolheria (opinião, a decisão é sua)

A **B** responde melhor à pergunta do produto ("em quantos dias o caixa acaba") e ensina o hábito de teclado. A **A** é mais calorosa e mais fácil no celular. Um caminho possível: a estrutura da B com o calor de papel e a serifada de título da A. Mas isso seria uma terceira direção; só faço se você pedir.
