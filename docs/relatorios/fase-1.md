# Relatório da Fase 1: núcleo do orçamento (sem WhatsApp, sem IA)

Data: 17/11/2026. Estado: **implementada e testada nesta máquina; o CI nunca rodou (nada foi enviado ao GitHub) e a imagem Docker não foi reconstruída nesta fase.** Os commits da fase estão só no repositório local.

## O que foi feito

| Entrega | Onde |
|---|---|
| Perfil da oficina (nome de exibição, telefone, endereço, horário, validade padrão, mensagem final) | `domain/company`, `application/profile`, `/api/company-profile` |
| Cliente da oficina (`Contact`): nome em claro, telefone e e-mail cifrados (AES-256-GCM via Tink), telefone único por oficina (índice cego HMAC) | `domain/contact`, `application/contact`, `/api/contacts` |
| Veículo: placa cifrada e única por oficina, ano, quilometragem opcional | `domain/vehicle`, `application/vehicle`, `/api/contacts/{id}/vehicles` |
| `Quote` com máquina de estados `DRAFT → AWAITING_OWNER_CONFIRMATION → SENT → APPROVED / REJECTED / EXPIRED / CANCELLED`, itens (peça ou mão de obra), `Money` e `Quantity`, total sempre calculado, nunca guardado | `domain/quote`, `application/quote`, `/api/quotes` |
| Link público: token de 256 bits, só o hash no banco, abrir não consome, decisão de uso único e idempotente, mesma resposta para link inexistente, vencido ou cancelado | `adapter-infra/quote`, `adapter-persistence/quote`, `/api/public/quotes/{token}` |
| Auditoria de cada evento do orçamento (somente acréscimo) | `domain/audit`, `AuditRecorder` |
| Tarefa agendada que grava os vencidos | `bootstrap/ExpireDueQuotesJob` |
| Telas: fila, novo orçamento, detalhe, oficina, página pública de aprovação | `frontend-angular/src/app/quotes/` |
| Contrato OpenAPI com `operationId` legível e cliente Angular regenerado | `openapi/openapi.json`, `src/app/api/` |
| ADRs 0008 a 0010, mapa de dados, glossário, segurança | `docs/` |

O protótipo do caixa (`/caixa`), a galeria (`/componentes`) e a fixture da padaria foram **removidos**; o produto não tem "caixa" nesta fase.

## Decisões suas aplicadas

As dez decisões aprovadas no plano: confirmar já envia; reenviar não muda o estado; sem edição depois de enviado; quantidade com 3 casas e arredondamento `HALF_UP` por linha; Tink; validade contada do envio (7 dias, fim do dia em America/Sao_Paulo); página pública mostra marca, modelo, ano e só o final da placa; telefone e placa únicos por oficina; densidade confortável; URL pública por configuração (`erp.business.public-base-url`).

## Critérios de aceite

| Critério | Situação |
|---|---|
| Testes antes do código, com o ciclo no histórico | **Parcial.** Nos checkpoints 1 a 3 sim. No checkpoint 4 mostrei vermelho para o mascarador de logs; nos testes de web (controllers, rotas, limites, contrato) escrevi testes e código no mesmo lote e só rodei depois, então **o vermelho não foi mostrado**. |
| `./mvnw verify` verde, ArchUnit verde, cobertura de `domain` e `application` ≥ 90% | **Sim** (rodado no fim do checkpoint 4; depois disso só mudaram nomes e `operationId` de rotas, e rodei as ITs de contrato e de rotas). |
| `docker compose up` do zero | **Não verificado nesta fase.** Rodei o Postgres do compose e o jar localmente, servindo o Angular compilado. A imagem da aplicação não foi reconstruída. |
| Testes de isolamento entre duas oficinas em todas as rotas | **Sim**: a varredura de rotas (401 sem token, 401 com token forjado, 403 com empresa alheia) cobre todas as rotas novas, e `QuoteApiIT` confere que ids de outra oficina dão 404 em leitura, escrita e cancelamento. Nada se exclui (não há DELETE). |
| Nenhum dado pessoal em logs | **Sim**: o teste percorre o fluxo inteiro (nome, telefone, placa, e-mail, token do link) e falha se algo aparecer; o mascarador agora cobre telefone e placa. |
| OpenAPI e cliente regenerados | **Sim.** |
| Capturas desktop e celular com autocrítica | **Sim**, em `docs/capturas/fase-1/`; autocrítica abaixo. |
| Axe sem violações WCAG A e AA | **Sim** em fila, novo, detalhe, oficina e página pública (celular). 0 violações. Contraste não foi medido par a par fora do axe. |
| Roteiro de teste manual | Atualizado em `docs/roteiro-teste-manual.md` (passos 10 a 20). |

Números: frontend 120 testes (Vitest, Testing Library), e2e 11 testes (Playwright, Edge) cobrindo cadastro, fluxo do orçamento até a aprovação aberta num celular emulado (Pixel 7), recusa com motivo, link cancelado ou inventado, cabeçalhos e ausência de violações de CSP. Orçamento de bundle: o inicial passou a ter aviso em 200 kB e erro em 350 kB; hoje tem **318 kB brutos, 80 kB transferidos**, e a página do cliente soma mais cerca de 2 kB de JavaScript.

## Autocrítica visual (capturas)

Conferido contra a lista de sinais de interface genérica do `CLAUDE.md`: sem barra lateral escura, sem quatro cartões de indicador, sem gradiente, sem vidro, sem emoji, sem cartão dentro de cartão; fila em linhas de régua, status com sinal e texto, números em Plex Mono alinhados, botões com verbo ("Aprovar orçamento", "Confirmar e gerar link").

Defeitos que achei olhando as capturas e corrigi: a quantidade aparecia no formato da API (`1.000`); a barra de cima tinha régua dupla; os blocos vazios da fila ocupavam metade da tela.

O que ainda me incomoda: (1) o rótulo de status é pequeno (13 px) e o glifo `→` do "enviado" é discreto no detalhe; (2) a barra de cima só tem texto e não há marca (nome adiado); (3) a página do cliente é limpa mas fria: não tem o telefone da oficina nem o endereço, porque o perfil ainda não é mostrado ali (decisão de minimização, a rever com você); (4) a edição de itens no celular é longa (quatro campos por item).

## O que não foi verificado

- **O CI, o gitleaks e o OWASP Dependency-Check nunca rodaram no GitHub.** Rodei o gitleaks local nos checkpoints anteriores, não de novo ao final.
- **Entrega ao cliente:** não há envio. `ManualQuoteDelivery` sempre falha, então a resposta traz `delivered=false` e o dono copia o link e o manda sozinho. O WhatsApp oficial entra na fase seguinte.
- **A tarefa de vencimento** só foi testada com nada a vencer e na lógica (`ExpireDueQuotesUseCaseTest`, `PublicQuoteFlowIT`); não esperei 5 minutos reais.
- **Navegadores:** só o Edge (Chromium) e um celular emulado. Safari e Firefox não foram testados; um celular de verdade também não.
- **Concorrência:** a corrida de decisões simultâneas está testada no banco (15 rodadas), não por HTTP.
- **Teste de e2e instável?** Em uma de cinco execuções logo após subir a aplicação, um teste falhou e não guardei qual. Antes, uma causa real: o limite padrão de renovação de sessão (30 por minuto por IP) estourava com o e2e rápido; agora o compose e o README passam as variáveis. Nas cinco execuções seguintes, 4 passaram limpas.

## Riscos e limites conhecidos

1. **Quem tem o link aprova.** O token tem 256 bits e vale até o prazo, mas qualquer pessoa com o link decide. Isso é o desenho (zero login para o cliente) e está no modelo de ameaças.
2. **O link aparece uma vez** (só o hash fica). Perdeu, gere outro: o antigo continua valendo até o prazo (`TODO(decisão)`: invalidar o anterior ao reenviar?).
3. **Telefone da oficina no perfil:** a API nunca devolve o número completo; para manter ou trocar é preciso digitar de novo, e em branco apaga.
4. **Negação de acesso sem auditoria:** os casos de uso novos lançam `AccessDeniedException` sem gravar evento.
5. **Limitador em memória e IP do proxy** (`X-Forwarded-For` é `TODO(decisão)`).
6. **Busca de cliente só por nome**, com teto de 50. Não há busca por telefone (minimização); o teto é contra raspagem.
7. **Carga:** nada foi medido com volume.
8. O jar local não passa por `compose` nem por HTTPS; o cookie `Secure` funciona em `localhost`.

## Pendências de revisão jurídica

Tudo que tem efeito jurídico continua **rascunho para revisão**: base legal por finalidade (`TODO(decisão)` no mapa de dados), prazo de retenção do orçamento e dos dados do cliente, texto de consentimento e de aceite (a aprovação por link vale como aceite do orçamento? não afirmei), e o relatório de impacto. Nenhuma regra fiscal foi implementada.

## Para a revisão independente

Sugiro uma sessão nova só para revisar, sem este histórico, com foco em: política de RLS dos escopos `quote_link_lookup`, `quote_decision` e `quote_expiration` (`V6__quotes.sql`); `PublicQuoteAccess` e `QuoteLinkDecision`; o mascarador de logs; e a lista `PublicRoutes`.
