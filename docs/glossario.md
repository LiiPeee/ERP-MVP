# Glossário

Liga os termos de negócio (pt-BR) aos nomes no código (inglês). Textos de interface e documentação ficam em
pt-BR; identificadores, em inglês.

| Termo de negócio | Nome no código | Observação |
|---|---|---|
| Empresa | `Company`, tabela `company` | O "tenant": a fronteira de isolamento dos dados. |
| Nome da empresa | `CompanyName` | Até 120 caracteres. |
| Usuário | `User`, tabela `"user"` | Global: uma pessoa, um registro (pode vir a ter várias empresas). `user` é palavra reservada do PostgreSQL, por isso sempre entre aspas. |
| Nome do usuário | `DisplayName` | |
| Vínculo (usuário com empresa) | `Membership`, tabela `company_membership` | Carrega o papel. |
| Papel | `Role` (`OWNER`) | Na Fase 0 só existe o dono. |
| Dono | `OWNER` | Quem cadastrou a empresa. |
| Senha | `PlainPassword` (texto puro, vive uma requisição), `PasswordHash` (Argon2id) | Mínimo 12, máximo 128 caracteres; sem regra de composição. |
| Bloqueio progressivo | `LoginLockoutPolicy`, `User.lockedUntil` | 5 falhas bloqueiam por 1 minuto, dobrando até 1 hora. |
| Token de acesso | `AccessTokenIssuer`, JWT HS256 | Vive 15 minutos; guardado só na memória do navegador. |
| Refresh token / sessão | `RefreshToken`, tabela `refresh_token` | 7 dias por token, teto de 30 dias por sessão. No banco só o hash. |
| Sessão (família de tokens) | `TokenFamilyId`, `SessionPolicy` | Reuso de um token já trocado revoga a sessão inteira. |
| Cadastrar empresa e dono | `RegisterCompanyAndOwnerUseCase` | `POST /api/auth/register` |
| Entrar | `LoginUseCase` | `POST /api/auth/login` |
| Renovar a sessão | `RefreshSessionUseCase` | `POST /api/auth/refresh` |
| Sair | `LogoutUseCase` | `POST /api/auth/logout` (idempotente) |
| Quem sou eu | `GetCurrentContextUseCase` | `GET /api/me` |
| Auditoria | `AuditEvent`, tabela `audit_event` | Só inserção; guarda códigos técnicos, nunca dado pessoal. |
| Tipos de evento | `AuditEventType`: `REGISTRATION`, `LOGIN`, `TOKEN_REFRESH`, `TOKEN_REUSE_DETECTED`, `LOGOUT`, `CONTEXT_VIEWED` | |
| Resultado | `AuditOutcome` (`SUCCESS`, `FAILURE`) | |
| Motivo da falha | `details.reason` (ex.: `BAD_PASSWORD`, `USER_NOT_FOUND`, `LOCKED`) | Só na auditoria; ao cliente a resposta é sempre a mesma. |
| Escopo de acesso | `AccessScope` | Diz ao banco o que a transação pode ver: `Tenant`, `Registration`, `CredentialLookup`, `RefreshTokenLookup`, `Anonymous`. |
| Unidade de trabalho / transação | `UnitOfWork`, `PostgresUnitOfWork` | Único ponto que define o escopo. |
| Isolamento entre empresas | Row Level Security (migração V3) | Reforça, no banco, o que a aplicação já faz. |
| Identificador de correlação | `correlation_id` (`X-Correlation-Id`) | Liga logs e auditoria de uma requisição. |
| Papel do banco "aplicação" | `ERP_DB_APP_USER` | Sem posse, sem superusuário, sujeito a RLS. |
| Papel do banco "migrações" | `ERP_DB_MIGRATOR_USER` | Dono do banco; roda o Flyway. |
| Dinheiro (futuro) | `Money` sobre `NUMERIC(18,2)` | Decidido; ainda não existe. |
