# Direção de arte

Estado: **direção B (Mesa de controle) escolhida em 17/11/2026 e implementada** nos tokens (`frontend-angular/src/styles/_tokens.scss`), nos componentes base (`src/app/ui/`) e no protótipo da tela do caixa (`/caixa`). Este documento foi escrito antes da implementação; onde diz "proposta" ou "direção A", vale o que está em **Decisões tomadas**, no fim. Contraste: o axe não achou violação WCAG AA nas telas, mas os pares de cor não foram medidos um a um.

## Para quem e para quê

O dono de uma micro ou pequena empresa quer saber duas coisas: **tenho lucro?** e **quantos dias de caixa eu tenho?** A interface existe para responder isso em segundos, sem treinamento e sem parecer software de contabilidade.

## Personalidade em três adjetivos

1. **Direto.** Primeiro a resposta, depois o detalhe. Sem rodeio e sem enfeite.
2. **Confiável.** Números que alinham, contas que fecham, nada que "pisca". Quem mexe com dinheiro desconfia de interface exuberante.
3. **Artesanal.** Parece ter sido desenhado para este uso: régua, papel, anotação. Não parece painel de modelo.

## O que o produto não é

- Não é painel corporativo: barra lateral escura, barra superior branca, quatro cartões de indicador, gráfico de linha e tabela.
- Não é "fintech": gradiente, vidro fosco, número gigante em destaque, promessa de "controle total".
- Não é planilha crua: o rigor do livro-caixa sem a feiura da grade infinita.
- Não é alegre à força: sem mascote, sem emoji, sem ilustração de banco de imagens.

## Princípios

- **A cor tem significado, nunca decoração.** Cor só aparece para dizer *a receber*, *a pagar*, *atrasado*. O resto é papel e tinta.
- **Nunca só cor.** Todo significado de cor também vai em sinal, texto ou forma (acessibilidade e impressão em preto e branco).
- **Número é o herói.** Numerais tabulares, alinhados à direita, sempre com a mesma largura e a mesma casa decimal.
- **A régua organiza.** Linhas finas no lugar de cartões e caixas; espaço no lugar de bordas.
- **Densidade honesta.** Muita informação por tela, mas legível; o usuário escolhe a densidade (decisão em aberto).
- **Teclado primeiro** nas ações repetidas (lançar, baixar, filtrar).
- **Sem copiar.** Nenhum produto existente serve de modelo visual. As referências são de ofício (livro-caixa, notas de impressão, tipografia de balanço), não de marca.

## Paleta proposta (a validar em contraste)

Superfície e texto são neutros quentes; a cor entra só como significado.

| Papel | Direção | Uso | Justificativa |
|---|---|---|---|
| Papel | off-white quente (claro, sem ser branco puro) | fundo | Reduz o brilho do branco e lembra caderno. |
| Tinta | quase preto quente | texto | Contraste alto sem o preto puro. |
| Régua | bege-acinzentado | linhas e divisórias | Organiza sem pesar. |
| A receber | verde-musgo escuro | entradas | Verde para entrada é convenção forte e é fácil de aprender. |
| A pagar | azul-tinta | saídas | Evita usar vermelho para toda saída, o que cansa e perde sentido. |
| Atrasado e zona negativa | vermelho-tijolo | atraso, saldo negativo | O único vermelho do produto: quando aparece, é urgente. |
| Foco | contorno de alto contraste, com cor própria | teclado | Foco sempre visível, nunca removido. |

Daltonismo: verde e vermelho se distinguem também por sinal (+/−), rótulo e hachura da zona negativa.

## Tipografia: regras de uso

- Um par de fontes **escolhido de propósito**, licença livre (OFL ou similar), **hospedado localmente**, com numerais tabulares para valores. Nada de Inter ou Roboto por padrão. Licenças em `docs/design/fontes.md` (a criar na fase).
- Texto e rótulos numa família; números e valores em numerais tabulares (`font-variant-numeric: tabular-nums`).
- Poucos tamanhos (escala curta), pesos limitados, títulos pequenos e firmes em vez de grandes e chamativos.
- Valores em reais: `R$` discreto, número em destaque, negativo com sinal e cor, nunca só parênteses.
- As candidatas de cada direção estão abaixo. **Disponibilidade e licença serão conferidas na fonte oficial antes de baixar**; hoje são propostas, não verificadas.

## Tom de voz

Pt-BR natural, frases curtas, verbo no começo, sem jargão contábil quando houver palavra do dia a dia.

- "Você tem caixa para **23 dias**." (não "Saldo projetado: R$ …")
- "Falta R$ 1.200,00 em 12 de março." (diz o que e quando)
- Erro diz o que fazer: "A data precisa ser depois de hoje. Escolha outra."
- Botões dizem a ação: "Lançar conta a pagar", "Dar baixa", nunca "Enviar" ou "OK".
- Estado vazio orienta: "Nenhuma conta a receber nesta semana. Lance a primeira com N."
- Sem ponto de exclamação, sem "Ops!", sem promessa vaga.

## Duas direções para a linha do tempo do caixa

Mesmos dados sintéticos, mesma resposta ("quantos dias de caixa eu tenho?"), conceitos diferentes.

### Direção A, "Livro-caixa"

- **Conceito:** o caixa como uma folha de livro-razão impressa. A página é a lista; a linha do tempo é uma faixa fina sobre ela.
- **Estrutura:** coluna única e larga, estilo página. No alto, a resposta em frase ("Você tem caixa para 23 dias, até 12 de março"). Logo abaixo, a faixa do saldo projetado em SVG, baixa e larga, com os vencimentos como marcas verticais. Em seguida, os dias em ordem, cada um com cabeçalho de data e lançamentos em linhas de régua, com saldo acumulado à direita, como num extrato.
- **Tipografia:** serifada de leitura para títulos e frases; sans simples para rótulos; numerais tabulares na sans. Candidata a verificar: uma serifada OFL (família Newsreader ou Source Serif) com IBM Plex Sans.
- **Cor:** papel quente com régua bege; cor só nos valores e marcas.
- **Teclado:** `N` abre o lançamento rápido numa linha em branco no fim do dia; setas navegam entre linhas.
- **Risco:** pode parecer "retrô" demais; a serifada precisa ser contida para não virar nostalgia.

### Direção B, "Mesa de controle"

- **Conceito:** o caixa como um instrumento de leitura, uma régua de horizonte. A linha do tempo é o protagonista, a lista é seu detalhe.
- **Estrutura:** duas áreas lado a lado no desktop. À esquerda, uma linha do tempo **vertical** (dia por dia, rolagem contínua) com o saldo projetado como curva que avança para a direita e a zona negativa hachurada. À direita, o painel do dia selecionado com seus lançamentos e a edição na linha. A resposta ("23 dias") vira um marcador na própria linha, no ponto em que o saldo cruza o zero.
- **Tipografia:** sans geométrica ou grotesca com personalidade própria para títulos; mono ou sans tabular para valores. Candidata a verificar: uma grotesca OFL com uma mono OFL (por exemplo JetBrains Mono ou IBM Plex Mono).
- **Cor:** papel levemente mais frio e neutro, para diferir da A; mesmo significado de cor.
- **Teclado:** `J` e `K` descem e sobem os dias, `N` lança, `/` filtra.
- **Risco:** mais "painel técnico"; precisa de cuidado para não cair no visual de ferramenta de desenvolvedor, e o celular exige redesenho mais forte.

### Como elas diferem de verdade

| | A, Livro-caixa | B, Mesa de controle |
|---|---|---|
| Eixo da linha do tempo | horizontal, faixa fina | vertical, protagonista |
| Hierarquia | página única, de cima para baixo | mestre e detalhe lado a lado |
| Resposta "dias de caixa" | frase de abertura | marcador na própria linha |
| Tipografia | serifada + sans | grotesca + mono |
| Celular | quase igual ao desktop | vira lista com mini-faixa fixa |

## Nome e marca (3 propostas)

Só tipografia, sem ilustração. **Disponibilidade de nome, domínio e marca não foi pesquisada** (isso é com você e, para marca, com um profissional).

1. **Saldo**: curto e direto, mas genérico e provavelmente muito usado.
2. **Régua**: remete à linha do livro-caixa e ao horizonte; sobra espaço de marca. Combina com a direção A e a B.
3. **Prazo**: fala do que o dono teme (vencimento). Menos "financeiro", mais prático.

Construção simples: o nome em minúsculas, na fonte de título, com uma linha fina horizontal sob a palavra (a régua). Funciona em uma cor, em preto e branco e em tamanho pequeno.

## Decisões em aberto (suas)

1. **Modo escuro:** sim ou não? Se sim, com a mesma qualidade do claro, o que dobra o trabalho de paleta e de contraste.
2. **Densidade padrão das tabelas:** confortável, compacta ou escolhida pelo usuário.
3. **Nome:** uma das três, outra, ou adiar (o nome é provisório até lá).
4. **Dependências novas**, a aprovar antes de instalar (cada uma ganha linha em `docs/dependencias.md`): Angular CDK, Testing Library (Angular) e `@axe-core/playwright`. Mais as fontes escolhidas, baixadas do repositório oficial e hospedadas localmente.

## Decisões tomadas (17/11/2026)

- **Direção:** B, Mesa de controle. A direção A (Livro-caixa) foi removida do código; as fontes dela (Newsreader e IBM Plex Sans) também. Sua autocrítica fica em [autocritica-direcoes.md](autocritica-direcoes.md).
- **Fontes:** Schibsted Grotesk (texto) e IBM Plex Mono (valores). Detalhes e licenças em [fontes.md](fontes.md).
- **Paleta final** (tokens `--color-*`): papel `#edf0ec`, papel afundado `#e0e5df`, tinta `#11161a`, tinta suave `#4d5852`, régua `#c3cbc5`, a receber `#0f6a4b`, a pagar `#1f4fa3`, atrasado e saldo negativo `#b3331f`. O saldo negativo também leva hachura nas linhas, e todo status leva sinal e texto.
- **Modo escuro:** **não nesta fase.** Os tokens têm nomes semânticos para o escuro caber depois sem tocar nos componentes.
- **Densidade padrão:** compacta (linha de 26 px, token `--row-height`). Não há seletor de densidade.
- **Nome e marca:** adiados. A interface usa "Padaria Fictícia" só como dado da fixture; nenhum logotipo foi desenhado.
- **Componentes:** botão, campo de texto, campo de valor em reais, seletor, data, tabela densa, tag de status, diálogo (`<dialog>` nativo), aviso, mensagem de erro, estado vazio, esqueleto, navegação e formatadores pt-BR (galeria em `/componentes`).
- **Seletor e data usam os controles nativos** (`<select>` e `<input type="date">`): teclado, leitor de tela e celular já funcionam. O formato da data nativa segue o idioma do navegador, não o da página.
- **Estilo na folha global, não nos componentes.** A CSP só aceita `style-src 'self'`; o Angular injetaria o estilo encapsulado como `<style>` e o navegador o bloquearia. Todo o estilo dos componentes fica em `src/styles/_ui.scss`, aninhado sob o próprio elemento. Componente novo segue a mesma regra.
- **Sem Angular CDK.** O CDK também injeta `<style>` ao abrir o diálogo (3 violações de CSP medidas). O `<dialog>` nativo faz o que precisávamos (foco, Esc, retorno do foco) sem isso, então o CDK saiu das dependências.

## Ajuste para a oficina (Fase 1, 17/11/2026)

O produto virou o orçamento de oficina mecânica pequena. A direção B segue (papel frio, tinta, régua, Schibsted Grotesk e IBM Plex Mono, tema claro), com estas mudanças:

- **Celular primeiro.** O dono usa o telefone em pé, com a mão suja e uma só mão livre. Alvos de toque de pelo menos 48 px, uma coluna, ações principais ao alcance do polegar (no fim da tela), nada que dependa de passar o mouse. O desktop é a mesma coluna, mais larga (máximo de 44 rem), não um painel.
- **Densidade confortável**, não mais compacta: linha de 44 px (`--row-height`) e controle de 48 px. Isto **reverte** a decisão de densidade compacta de 17/11/2026, que era para o caixa.
- **A cor muda de significado.** Verde (`--color-in`): aprovado. Azul (`--color-out`): esperando o cliente. Tijolo (`--color-alert`): recusado ou vencido, o que pede uma ação do dono. Tinta suave: rascunho e cancelado. Todo status leva também sinal e texto: rascunho `•`, enviado `→`, aprovado `✓`, recusado `✕`, vencido `!`, cancelado `–`.
- **A fila é a tela inicial**, em três blocos com nomes de ação: "Precisam de você", "Esperando o cliente", "Fechados". Cada orçamento é uma linha de régua (veículo, cliente, valor), não um cartão.
- **A página do cliente** (link do WhatsApp) é a única que o cliente vê: sem menu, sem login, nome da oficina, veículo, itens em linhas de régua, o total grande e dois botões empilhados ("Aprovar orçamento", "Recusar"), mais a validade em uma frase. Carrega pouco JavaScript e abre rápido em rede ruim.
- **Zero digitação:** o cadastro do cliente e do veículo é o mínimo (nome e telefone; placa, marca, modelo e ano). O que o mecânico não deve digitar na oficina entra por áudio e conversa, em fases seguintes; esta fase só garante que o orçamento saia sem planilha.
- **Removido:** o protótipo do caixa (`/caixa`), a fixture da padaria e as telas de linha do tempo. O produto não tem mais "caixa" nesta fase.
- **Nome e marca:** continuam adiados; a barra de topo usa só o texto "Orçamentos".
