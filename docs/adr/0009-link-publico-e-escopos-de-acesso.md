# 0009. Link público do orçamento e escopos de acesso

Data: 17/11/2026. Estado: aceita.

## Contexto
O cliente da oficina aprova ou recusa o orçamento por um link, sem login. É dado de uma oficina servido sem tenant autenticado.

## Decisão
- Token de 256 bits; só o hash (SHA-256) vai ao banco. O link vale até o fim do prazo do orçamento.
- **GET não consome nada**; só a decisão (POST) o faz. Repetir a mesma decisão não muda nada; a oposta é recusada.
- Link inexistente, vencido ou de orçamento cancelado: **uma só resposta**.
- Acesso em duas transações, com escopos novos de RLS: `quote_link_lookup` (enxerga só o link com aquele hash) e depois
  `quote_decision` (preso a **um** orçamento; empresa e orçamento vêm do banco, nunca do cliente), que lê o que a página precisa
  (orçamento, itens, veículo, perfil, empresa) e altera só as colunas da decisão. A decisão trava a linha do orçamento.
- A tarefa de expiração usa `quote_expiration`: lê os enviados (e os vencidos, porque o PostgreSQL exige que a linha nova
  continue visível depois do UPDATE) e só muda o estado para vencido, e só de quem já passou do prazo.

## Consequências
- Primeira rota pública com dado: entra na lista de rotas públicas com teste; limite de requisições; `noindex`.
- O escopo de expiração cruza empresas por desenho; a política é estreita e testada de forma hostil.
- A aplicação continua sem `DELETE` em lugar nenhum.
