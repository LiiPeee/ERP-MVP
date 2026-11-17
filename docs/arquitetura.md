# Arquitetura

Monólito modular em Clean Architecture, com Maven multi-módulo. A regra da dependência é imposta pelo compilador
(cada módulo só declara as dependências permitidas) **e** por testes ArchUnit no módulo `bootstrap`.

| Módulo | Pode depender de | Conteúdo |
|---|---|---|
| `domain` | nada | Entidades, objetos de valor, regras, exceções de domínio. Java puro. |
| `application` | `domain` | Casos de uso (uma classe cada, `execute`), portas de entrada e de saída, comandos e resultados. Sem framework. |
| `adapter-web` | `application` (+ exceções do `domain`) | Controllers, DTOs, segurança HTTP, tradução de erros, limites, correlation id. |
| `adapter-persistence` | `application`, `domain` | Entidades JPA, adaptadores, mapeamento na borda, migrações, RLS. |
| `adapter-infra` | `application` | Argon2id, refresh token, JWT, ids, mascaramento de logs. As portas que ele implementa usam só texto e UUID. |
| `bootstrap` | todos | Configuração do Spring, casos de uso como beans, `main`. |

Organização **por funcionalidade** dentro de cada módulo (`identity`, `company`, `audit`).

## Regras verificadas por teste (ArchUnit)

Dependência entre módulos; `domain` e `application` só usam o JDK; casos de uso finais com um único método público
`execute`; interfaces públicas da aplicação só em `port.out`; exceções de domínio estendem `DomainException`; nenhum
`Instant.now()`, `UUID.randomUUID()` ou `System.currentTimeMillis()` em `domain` e `application` (tempo e ids são
injetados); sem ciclos entre funcionalidades; entidades JPA só na persistência e com o sufixo `JpaEntity`; escopos
de acesso "pré-tenant" só nos casos de uso de identidade; SQL manual só no pacote `tenancy`; uma única implementação
de `UnitOfWork`; o adaptador web só usa do domínio as exceções e nunca acessa portas de saída; controllers só no web.
Cada regra foi provada com uma violação temporária que a fez falhar.

## Isolamento entre empresas

- O **tenant vem sempre do token** validado, nunca de parâmetro, corpo ou cabeçalho do cliente.
- Cada transação roda sob um **escopo de acesso** (`AccessScope`) que o `PostgresUnitOfWork` grava em variáveis de
  sessão locais à transação (`set_config(..., true)`). É o único ponto que faz isso.
- **Row Level Security** (`ENABLE` + `FORCE`) em todas as tabelas, com políticas por escopo e sem referência circular.
  Sem escopo, nenhuma linha é visível; o próprio dono das tabelas vê zero linhas sem escopo.
- O papel de banco da aplicação **não é dono de nada, não é superusuário, não tem `BYPASSRLS`, nunca tem `DELETE`**, só
  atualiza colunas mutáveis, e a auditoria é só de inserção. As migrações usam outro papel.
- Cadastro e login acontecem **antes** de existir tenant: usam escopos próprios e mínimos (`Registration`,
  `CredentialLookup`, `RefreshTokenLookup`), cada um enxergando só o que aquele passo precisa.
- Uma falha que precisa deixar rastro (auditoria, contador de tentativas) é **devolvida como resultado** dentro da
  transação, não lançada, para o commit acontecer antes de o erro chegar ao cliente.

## Modelo de dados (migrações V1 a V4)

```
company 1───N company_membership N───1 "user"
company 1───N refresh_token      N───1 "user"
audit_event  (company_id e actor_user_id sem chave estrangeira, de propósito)
```

Chaves primárias em UUID gerado pela aplicação; datas em `timestamptz`. Dinheiro (futuro): `NUMERIC(18,2)` e um objeto
de valor `Money`.

## Fluxo

```
POST /api/auth/register -> cria empresa, dono e vínculo (uma transação) -> auditoria
POST /api/auth/login    -> token de acesso (corpo) + refresh token (cookie HttpOnly) -> auditoria
POST /api/auth/refresh  -> troca o refresh token (rotação); reuso de um já trocado revoga a sessão inteira
POST /api/auth/logout   -> revoga a sessão e apaga o cookie (idempotente)
GET  /api/me            -> usuário, empresa e papel; autorizado no caso de uso, não só no controller
```
