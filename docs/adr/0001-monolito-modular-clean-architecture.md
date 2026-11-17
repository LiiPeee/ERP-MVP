# 0001. Monólito modular com Clean Architecture

Data: 16/11/2026. Estado: aceita.

## Contexto
Precisamos de uma única aplicação implantável, com regra de negócio testável sem framework, banco ou rede.

## Decisão
Monólito modular em Maven multi-módulo: `domain`, `application`, `adapter-web`, `adapter-persistence`, `adapter-infra`, `bootstrap`.
A regra da dependência (só se depende de camadas mais internas) é imposta pelo compilador (dependências entre módulos) e por ArchUnit.
Casos de uso são registrados como beans só no `bootstrap`. Entidades JPA não são entidades de domínio; o mapeamento fica na borda.

## Consequências
- `domain` e `application` rodam em testes unitários com fakes em memória; cobertura mínima de 90% de ramos (JaCoCo).
- Mais classes (mapeadores) do que um CRUD direto: custo aceito em troca de isolamento e testabilidade.
- Sem microsserviços, mensageria, cache ou CQRS formal, até uma medição justificar.
