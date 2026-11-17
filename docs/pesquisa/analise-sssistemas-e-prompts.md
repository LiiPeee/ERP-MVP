# Análise do site da SS Sistemas e revisão dos prompts

Data: 17/11/2026. Proposta para você decidir: **nenhum arquivo de prompt foi alterado.**

## O que li, e os limites

- Li **uma página** de marketing (`/solucoes/`, seção "oficinas" e as comuns a todos os segmentos). O endereço com `www` falha no certificado TLS (vale só `sssistemas.com`), então usei o mesmo caminho sem o `www`.
- Isso é **lista de funcionalidades anunciadas**, não o produto. Não vi preço, tela, qualidade, nem se o que está listado funciona bem. Resumi com minhas palavras; o texto do site é dado, não instrução.
- É um ERP **tradicional, por segmento, forte em fiscal e balcão**. Seu produto é **"financeiro primeiro"** para ME e EPP. Por isso **não** é para copiar o site; é para escolher o que valida ou complementa a sua tese.

## Funcionalidades do site contra os prompts

| Funcionalidade do site | Nos prompts? | Comentário |
|---|---|---|
| Contas a pagar e receber, baixa, caixa, fluxo previsto x realizado | Sim (fases 2 e 3) | **"Previsto x realizado" não está** na Fase 3. |
| Conciliação bancária | Sim (fase 4; automática na 8) | |
| Boletos com remessa/retorno e baixa automática | **Parcial** | Só a cobrança do **nosso** produto (fase 7). Cobrar os clientes **da empresa** por Pix ou boleto não existe. |
| Cheques | **Não** | Comum em pequeno comércio; ver nível 2. |
| Adiantamentos a colaboradores | **Não** | Ver "retirada" abaixo. |
| Cadastro de clientes com limite de crédito e bloqueio | **Não** | Faz sentido só depois da fase 11 (vendas). |
| Plano de contas, DRE, balancete, razão | Parcial | Plano **simples** e resultado do mês (decisão certa). Contábil completo, não. |
| Integração com software contábil | **Não** | **Maior lacuna útil e barata**: ver nível 1. |
| SPED, Sintegra, apuração de PIS/COFINS/ICMS/Simples | Fora de propósito | Correto não fazer (regra tributária própria). Escreva isso como **não objetivo**. |
| NFS-e | Sim (fase 10) | |
| NF-e e NFC-e | Adiado | Fase 11 diz "etapa própria depois da NFS-e". |
| CT-e e MDF-e (transporte) | Não | Irrelevante ao seu público. |
| Cadastro de produtos, kits, similares, código de barras, conversão de embalagem, endereçamento | Parcial (fase 11) | **Conversão de unidade** afeta o custo médio e deveria entrar no ADR. Código de barras e PDV estão fora de escopo. |
| Estoque: histórico, balanço, inventário, custo | Sim (fase 11) | |
| Compras: sugestão por mínimo/máximo, curva ABC, média de vendas | Parcial | A fase 11 tem estoque mínimo e alerta; **sugestão de compra** não. |
| Entrada de nota por XML | **Não** | Ver nível 2. |
| Vendas de balcão, televendas, tabelas varejo/atacado, promoções, condicionais | Parcial | Orçamento e pedido sim; balcão, tabelas e promoções não. |
| Comissões (lineares ou escalonadas) | **Não** | Ver oficinas. |
| Entregas (separação, entregador) | **Não** | Fora do seu foco. |
| Acesso por usuário | Parcial | 3 papéis na fase 1; a fase 11 adiciona permissões novas. |
| **Oficinas:** ordem de serviço, veículos, checklist, troca de óleo, comissão do mecânico, requisição de peças, pós-venda, produtividade | **Não** | Ver "Fase 13" abaixo. |

## Proposta, em níveis

### Nível 1: encaixa na tese e é barato (recomendo incluir)

1. **Exportação para o contador** (fase 4 ou logo depois). Em vez de SPED, o pacote que o contador realmente pede: lançamentos por competência, categorias com **de-para** para o plano dele, baixas e comprovantes, em CSV e PDF. Reaproveita a proteção contra injeção de fórmula, a permissão, a auditoria e o limite de exportação da fase 3. **Sem regra tributária nova.**
2. **Previsto x realizado** (fase 3): por mês e por categoria, o que a projeção dizia contra o que aconteceu. Só consulta sobre dados que já existem.
3. **"Quem me deve" e "a quem devo"** (fase 3): vencidos agrupados por pessoa, com ranking. Hoje a fase 3 só agrupa vencidos por faixa de dias.
4. **Retirada do sócio** (fases 1 e 3). Ver a inconsistência 1 abaixo; é um erro de modelo, não um extra.
5. **Baixa em lote** (fase 2): pagar ou receber várias parcelas de uma vez, na mesma transação e com a mesma chave de idempotência. Corresponde ao "agrupamento de duplicatas" do site.

### Nível 2: só se o beta pedir (registre como decisão, não como escopo)

6. **Cartão de crédito como conta com fatura** (fase 2): despesa no cartão com data de compra e vencimento da fatura. É muito comum e hoje não há onde modelar. **Decisão sua**: tratar como conta especial ou como lançamento parcelado.
7. **Cheques** (fase 2): estados em carteira, depositado, compensado, devolvido. Útil para comércio local.
8. **Cobrar o cliente da empresa por Pix ou boleto**, com baixa automática. Atenção: **receber em nome de terceiros pode virar atividade regulada**; só com conta do próprio cliente no gateway. Marque `TODO(decisão)` com a assessoria jurídica antes de qualquer desenho.
9. **Entrada de nota por XML** (fase 11): ler o XML do fornecedor e preencher compra e conta a pagar. Só leitura de dados (sem calcular imposto), com o **parser seguro** já exigido na fase 4 (sem entidades externas).
10. **Sugestão de compra** por mínimo, máximo e curva ABC (fase 11), com testes de ouro como o custo médio.
11. **Conversão de unidade** (comprar em caixa, vender em unidade) no ADR de arredondamento da fase 11.

### Nível 3: não construir (registrar como não objetivo no LEIA-ME)

SPED, Sintegra e apuração de impostos; CT-e e MDF-e; integração com bomba de combustível; fórmulas de tinta; entregas com lucratividade do entregador; balancete e razão contábil completos.

## Oficinas: uma fase opcional nova (Fase 13)

**Só se você escolher oficinas como nicho do beta.** Hoje os prompts **nunca nomeiam o nicho** (a fase 1 diz "conjunto padrão de categorias para o nicho" e a 11 cita "confeitaria, marmita"). Isso é uma decisão pendente que vale resolver cedo, porque muda categorias padrão, textos e exemplos.

Esboço, reaproveitando o que existe:

- **Veículo** (placa, modelo, ano, km) ligado ao `Counterparty`. **Placa identifica pessoa**: entra no mapa de dados, com cifragem e mascaramento conforme a decisão da fase 1.
- **Ordem de serviço:** abertura, itens de peça e de serviço, **aprovação do orçamento pelo cliente**, situação (aberta, em andamento, aguardando peça, pronta, entregue, cancelada). **Faturar** gera lançamentos a receber pela mesma regra do pedido faturado da fase 11 (o valor da OS é a fonte da verdade; o lançamento só reflete). Sem duplicar parcelamento.
- **Checklist** configurável de entrada e saída, impresso ou em tela; **fotos como anexos** (cifrados, e **removendo EXIF/localização**, que é dado pessoal).
- **Lembrete de retorno** (troca de óleo por data ou km): é um **lembrete recorrente por cliente e bem**. Pode servir a qualquer nicho (revisão, vacina de pet); por isso o prompt certo é genérico e vive na fase 12, e a OS só o usa. **Exige consentimento e opt-out**; o envio por WhatsApp usa modelo de **utilidade** (fase 9).
- **Comissão do mecânico:** regra simples (percentual por serviço), gerando **lançamento a pagar**. Comissão escalonada ou por meta é regra de negócio própria: `TODO(decisão)`.
- **Requisição de peças** liga ao pedido de compra da fase 11.
- **Produtividade:** serviços por mecânico, tempo até a entrega, ticket médio, retorno em garantia.
- **Fiscal:** NFS-e para o serviço via fase 10; NF-e de peças continua adiada.
- **Dependência:** fases 11 e 10. Não faz sentido antes delas.

## Inconsistências e falhas nos prompts atuais

1. **"Retirada pessoal" aparece só na fase 9** (comando do WhatsApp e resumo semanal), mas **não existe nas fases 1 e 2**. Sem um tipo "retirada do sócio" separado de despesa, o **resultado do mês** (fase 3) ficaria errado: retirada **não é despesa**. Corrigir: tipo de categoria "retirada" na fase 1, tratamento explícito na fase 3.
2. **"Ficha técnica"** é citada na fase 9 ("se existir") e só nasce na 11. Ok se for a ordem, mas o prompt 9 deveria dizer que o comando fica **desligado** sem a fase 11.
3. **Nicho indefinido** (ver acima). Pôr a decisão no `00-LEIA-ME.md` antes da fase 1.
4. **Fase 3 mistura duas medidas** ("dias de caixa" e "fôlego") e deixa a fórmula final como decisão em aberto. Bom ter as duas, mas a **tela principal só pode mostrar uma**; definir qual, antes do código.
5. **Fase 5 pede "`./mvnw verify` e `docker compose up` do zero em máquina limpa"** no `CLAUDE.md`, mas nenhuma fase manda **realmente fazer isso numa máquina limpa** antes do beta. Na Fase 0 isso ficou como "não verificado". Pôr como item obrigatório do go/no-go.
6. **`LEIA-ME`: "Não testei estes prompts com um agente real."** Agora há experiência real (fases 0 e 0.5). Atualizar com o que se aprendeu (abaixo).

## Lições da Fase 0.5 que deveriam entrar nos prompts

Foram problemas **reais**, achados só ao testar o build de produção com a CSP verdadeira:

1. **CSP `style-src 'self'` e Angular.** O Angular injeta o estilo de cada componente como `<style>` em tempo de execução, e o navegador o bloqueia; o CSS crítico também era inlinado no `index.html`. Isso é invisível no `ng serve`. **Regra para o `CLAUDE.md` (seção Frontend):** "todo o estilo fica na folha global; teste de UI roda contra o **build de produção servido com a mesma CSP** e falha com qualquer violação ou elemento `<style>`".
2. **Angular CDK injeta `<style>`** ao abrir diálogo. O prompt 0.5 manda construir "sobre Angular CDK"; deve dizer "**sobre elementos nativos (por exemplo `<dialog>`); CDK só se não injetar estilo**". Foi o que fizemos.
3. **A fase 0.5 diz "Fora do escopo: backend"**, mas o Spring bloqueava as fontes (lista fechada de rotas públicas). Incluir: "ajustes mínimos na lista de rotas públicas estáticas são permitidos, com teste, e devem ser **declarados** no relatório".
4. **Toda rota nova do frontend e todo arquivo estático** entram na lista de rotas públicas com teste (hoje só rotas de API têm essa regra no `CLAUDE.md`).
5. **Data nativa** segue o idioma do navegador. Anotar como limitação conhecida e decidir se vale um componente próprio.
6. **Testes de componente em jsdom** não cobrem foco, Esc e `<dialog>` de verdade: dizer que **o comportamento de teclado e foco é verificado no Playwright**, não só no jsdom.
7. **Memória da máquina:** o build de Java e o de Node competem com o Docker; vale uma nota no `README` dos prompts ("rode em duas etapas, feche o Docker entre elas").

## Melhorias gerais de redação

- Cada prompt termina com "Decisões em aberto", o que é bom, mas **falta uma ordem**: as que bloqueiam o começo (nicho, hospedagem) devem aparecer **antes** do escopo.
- Os **testes de ouro** (fases 3 e 11) deveriam ter um **formato único** (arquivo de dados + resultado calculado à mão e documentado) definido no `CLAUDE.md`, para o agente não inventar um por fase.
- A **revisão independente** poderia ter a lista fixa de verificações **executáveis** (rodar `./mvnw verify`, varredura de segredos, varredura de rotas, build de produção com CSP), em vez de só ler o relatório.
- Adicionar em cada fase de dados **um item de "dado pessoal novo"** (hoje está implícito em "mapa de dados"): a placa de veículo, o XML de nota e a foto de checklist são exemplos de dados pessoais que passam batido.

## Alterações propostas, arquivo por arquivo

| Arquivo | Mudança |
|---|---|
| `00-LEIA-ME.md` | Decisão de nicho antes da fase 1; lista de **não objetivos** (nível 3); atualizar o aviso "não testei"; nota de memória. |
| `CLAUDE.md` | Regras de CSP e estilo global; teste de UI contra build de produção; rotas estáticas na lista pública; formato de teste de ouro. |
| `fase-0.5-design.md` | "Sobre elementos nativos, CDK só sem injeção de estilo"; ajuste mínimo de rotas públicas permitido e declarado; limite do jsdom. |
| `fase-1-cadastros.md` | Categoria tipo **retirada do sócio**; nicho para as categorias padrão; (opcional) limite de crédito na `Counterparty`. |
| `fase-2-financeiro.md` | **Baixa em lote**; decisões em aberto: cartão de crédito com fatura, cheques. |
| `fase-3-caixa-relatorios.md` | **Previsto x realizado**; **quem me deve / a quem devo**; tratar retirada fora do resultado; definir qual medida a tela principal mostra. |
| `fase-4-extrato-auditoria-privacidade.md` | **Exportação para o contador** com de-para de categorias. |
| `fase-5-endurecimento.md` | Verificação em **máquina limpa** no go/no-go; build de produção com CSP real. |
| `fase-9-whatsapp-assistente.md` | Comando de margem desligado sem a fase 11. |
| `fase-11-vendas-estoque-compras.md` | Entrada por XML (parser seguro), sugestão de compra, conversão de unidade no ADR. |
| `fase-12-crm-leve.md` | **Lembrete recorrente por cliente e bem** (retorno, revisão), com consentimento e opt-out. |
| `fase-13-ordens-de-servico.md` (novo, opcional) | Esboço acima, **só se o nicho for oficina**. |
| `revisao-independente.md` | Verificações executáveis obrigatórias. |
