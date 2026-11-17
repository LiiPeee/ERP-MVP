# 0004. Logs sem dado pessoal

Data: 16/11/2026. Estado: aceita.

## Decisão
- Logs estruturados em JSON, com `correlation_id` e `company_id`.
- Mascaramento centralizado (`PersonalDataMasker`) de e-mail, CPF, CNPJ, JWT e tokens longos.
- Loggers do Hibernate, do Spring Web e do Spring Security fixados em WARN/INFO, pois em DEBUG vazavam conteúdo de entidades e URLs.
- Teste de integração (`LogsDoNotLeakPersonalDataIT`) falha se um log contiver esses padrões.
- Erros HTTP em `ProblemDetail`, sem detalhe interno. Auditoria separada e somente de acréscimo.

## Consequências
Mudar o nível de log de bibliotecas exige passar pelo mesmo teste. Um dado pessoal novo exige nova regra no mascarador.
