# 0002. Isolamento entre empresas com Row Level Security

Data: 16/11/2026. Estado: aceita.

## Contexto
Um erro de filtro numa consulta não pode expor dados de outra empresa.

## Decisão
- O tenant vem sempre do token autenticado, nunca de parâmetro, corpo ou cabeçalho.
- Toda tabela de negócio tem `company_id`, com `ENABLE` e `FORCE ROW LEVEL SECURITY`.
- Cada transação declara seu escopo (`AccessScope`: anônimo, cadastro, busca de credencial, busca de refresh token, tenant) pela porta `UnitOfWork`, que grava variáveis de sessão com `set_config(..., true)` (vale só na transação).
- A aplicação conecta com papel sem dono, sem superusuário, sem `BYPASSRLS` e sem `DELETE`; `UPDATE` só por coluna. As migrações usam outro papel.
- A auditoria é somente de acréscimo (insert-only).

## Consequências
- Mesmo uma consulta sem filtro não vaza dados entre empresas; testes com PostgreSQL real e duas empresas provam isso.
- Fluxos sem tenant (cadastro, login) usam escopos estreitos e nomeados, cada um com políticas próprias.
- Exige PostgreSQL real em testes (Testcontainers).
