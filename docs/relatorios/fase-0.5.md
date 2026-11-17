# Relatório da Fase 0.5: identidade visual e sistema de design

Data: 17/11/2026. Estado: **implementada e testada nesta máquina. O fluxo da Fase 0 e as fontes foram conferidos contra a pilha completa (ver abaixo); o CI ainda não rodou.**

## O que foi feito

| Entrega | Onde |
|---|---|
| Direção de arte | [docs/design/direcao.md](../design/direcao.md) |
| Duas direções avaliadas e autocrítica | [docs/design/autocritica-direcoes.md](../design/autocritica-direcoes.md) (você escolheu a B) |
| Fontes e licenças | [docs/design/fontes.md](../design/fontes.md) |
| Tokens de design (cor, tipografia, espaço, forma, movimento, densidade) | `frontend-angular/src/styles/_tokens.scss` |
| 13 componentes base e formatadores | `frontend-angular/src/app/ui/` (galeria em `/componentes`) |
| Protótipo da tela do caixa | `/caixa` (`src/app/caixa/`), só com dados sintéticos |
| Funções puras (moeda, datas, projeção, geometria, dias de caixa) | `src/app/cash/`, em TDD |
| Mensagens de interface centralizadas | `src/app/ui/messages.ts` e `src/app/caixa/messages.ts` |
| Capturas desktop e celular | `docs/capturas/fase-0.5/` (geradas por `e2e/capturas/capturar-design.mjs`) |

Decisões suas: direção B, só tema claro, densidade compacta, nome adiado.

## Critérios de aceite

| Critério | Situação |
|---|---|
| Nenhum sinal da lista de interface genérica | **Conferido item a item abaixo**, por mim, olhando as capturas. |
| Nenhum recurso de terceiros | **Sim.** O Playwright registra as requisições de `/caixa` e `/componentes` e falha se alguma sai da origem; nenhuma saiu. |
| Tokens como única fonte de valores visuais | **Sim para cores:** uma busca por `#hex` e `rgb(` fora dos tokens não achou nada. Larguras de layout (colunas, painel) ficam locais nos componentes. |
| Zero violações críticas no axe | **Sim:** 0 violações WCAG A e AA em `/caixa` e `/componentes`, desktop e celular, e com o diálogo aberto. |
| Fluxo do caixa completo por teclado | **Sim**, no Playwright: N, descrição, Tab, valor, Enter; J e K; Esc. |
| `direcao.md`, `fontes.md`, este relatório | Prontos. |

## Testes

- **Unitários do frontend: 91 passando** (eram 9 na Fase 0): funções puras, modelo do caixa, validação do lançamento, pipes e os 13 componentes com Testing Library (teclado incluído).
- **Playwright de design: 15 passando** no Edge instalado, **contra o build de produção servido com a mesma CSP do backend** (axe, rede, CSP, teclado, diálogo, tabela).
- Ciclo vermelho→verde está no histórico para as funções puras, o modelo, a validação e o campo de valor. Os componentes visuais nasceram junto da primeira versão dos testes.
- **Bug achado só pelo teste no navegador:** apertar Enter no campo de valor enviava o formulário com o valor ainda não lido (a leitura ocorria só ao sair do campo). Corrigido com teste unitário que falhava antes.

## Achados de CSP (os mais importantes desta etapa)

A CSP do backend é `style-src 'self'`. Testar só no servidor de desenvolvimento do Angular (que não tem CSP) escondia dois problemas, que um teste contra o build de produção com a CSP real expôs:

1. **CSS crítico inline.** O build de produção inlinava o CSS global como `<style>` (e um `<script>` de carga) no `index.html`. Isso nunca apareceu na Fase 0 porque a folha global estava vazia. Corrigido em `angular.json` (`optimization.styles.inlineCritical: false`); o teste de ponta a ponta da Fase 0 (`fluxo.spec.ts`) já exigia zero `<style>` e foi quem acusou.
2. **Estilo de componente.** O Angular injeta o estilo encapsulado de cada componente como `<style>` em tempo de execução, e o navegador o bloquearia: na medição, a linha do dia saía com 24 px em vez dos 26 px do token e o console acumulava violações. Movi **todo** o estilo dos componentes para a folha global (`src/styles/_ui.scss` e `src/styles/prototipo/`), sem afrouxar a CSP.
3. **Angular CDK.** Ao abrir o diálogo, o CDK injetava 3 elementos `<style>` (violação `style-src-elem`), sem opção de desligar. Troquei o diálogo pelo `<dialog>` nativo e **removi o CDK** das dependências. Contrapartida: o `<dialog>` nativo é modal (o resto da página fica inerte), mas o Tab pode sair para a interface do navegador depois do último botão; o travamento total de foco do CDK se perdeu. O teste afirma que o foco nunca chega à página por trás.

Resultado medido: 0 elementos `<style>`, 0 violações de CSP em `/caixa`, `/componentes` e com o diálogo aberto, e linha de 26 px.

## Autocrítica da tela final contra a lista do CLAUDE.md

| Sinal de interface genérica | Situação |
|---|---|
| Barra lateral escura + barra superior branca + 4 cartões + gráfico de linha + tabela | Não há. A curva é o eixo da tela, não um gráfico decorativo, e não há cartões de indicador. |
| Gradiente roxo ou azul | Não. A única hachura é do saldo negativo. |
| Glassmorphism | Não. |
| Sombra e cantos arredondados em tudo | Nenhuma sombra; raio de 3 px só em botões e campos. |
| Cartões dentro de cartões | Não. O único bloco com fundo é o formulário de lançamento. |
| Inter ou Roboto sem decisão | Schibsted Grotesk e IBM Plex Mono, escolhidas e registradas. |
| Emoji como ícone | Não. Os sinais ▲ ▼ ! são caracteres de texto, com `aria-hidden`. |
| Ilustração de banco de imagens | Não. |
| Lorem ipsum | Não. |
| Texto vago | Não; os textos dizem o dia e o valor. |
| Botão "Enviar" | "Lançar", "Apagar", "Manter". |
| Estado vazio só com ícone | Título, orientação e botão. |
| Dados de exemplo óbvios | **Sim, de propósito:** "Fornecedor Fictício 1", "Padaria Fictícia". São dados sintéticos assumidos como falsos; um dado de exemplo mais natural traria risco de parecer real. |

## Pontos fracos que eu vejo

- A curva mostra o saldo no fim do dia, não o intradiário; com vários lançamentos no dia ela só marca o último ponto.
- O painel do dia em tela estreita vem **antes** da linha do tempo; funciona, mas a hierarquia no celular não foi testada com pessoas.
- A **data nativa** (`<input type="date">`) segue o idioma do navegador: no Edge em inglês aparece `mm/dd/yyyy`. Em navegador em português mostra `dd/mm/aaaa`. Trocar por componente próprio é custo alto e fica para quando houver necessidade medida.
- Logo após apertar **N**, as primeiras letras digitadas podem cair antes de o foco chegar ao campo (a renderização leva um quadro). Não medi quão provável isso é para quem digita rápido.
- O selo "O caixa acaba aqui" depende de cor **e** texto, mas em telas muito estreitas ele ocupa a linha inteira do dia.
- Só o tema claro foi desenhado. O escuro não foi testado nem de contraste.

## O que **não** foi verificado

- Contraste de cada par de cores à mão (confiei na regra do axe), e leitor de tela de verdade.
- Safari, Firefox e Chromium: só o Edge foi usado localmente (o CI usa Chromium).
- Usabilidade com pessoas; as telas foram avaliadas só por mim.
- Desempenho da linha do tempo com muitos dias (a fixture tem 45).
- A rota `/caixa` e a galeria não exigem login: são protótipos sem backend. **Devem ser removidas ou protegidas antes de qualquer uso real.**

## Única mudança no backend (fora do escopo da fase, por necessidade)

Ao rodar contra o Spring, as fontes locais davam **401**: a lista de rotas públicas (`PublicRoutes.FRONTEND`) só liberava `.js`, `.css` e `.ico`. Sem isso o design quebraria na aplicação integrada. Liberei **somente** `GET /fonts/*.woff2` (um nível, só esse tipo). Os arquivos de licença da mesma pasta e subpastas continuam negados (401), o que o `RouteSecurityIT` agora confere (teste escrito antes, visto falhar, depois verde). Revise essa linha: ela amplia a superfície pública, ainda que só de fontes livres.

**Não** liberei `/caixa` nem `/componentes` no Spring: são protótipos sem login. Dentro do app funcionam (rota do Angular), mas abrir a URL direto na aplicação integrada dá 401. Por isso os testes de design rodam contra o servidor do Angular, não contra o Spring.

## Dependências novas

Testing Library (angular 19.6.0, dom 10.4.2, user-event 14.6.12), `@axe-core/playwright` 4.14.0 e as duas fontes OFL (arquivos, não pacote). Todas justificadas em [docs/dependencias.md](../dependencias.md).

## Pendências para fases seguintes

- Decidir **nome e marca** (adiado) e, se quiser, o modo escuro.
- Remover `/componentes` e a fixture do bundle de produção quando a tela real do caixa existir (Fase 3).
- Passar a tela do caixa pelo backend real; a regra de dias de caixa aqui é só da fixture.
- Revisão independente (`revisao-independente.md`) em sessão nova.
