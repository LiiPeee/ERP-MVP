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
| Tipos de evento | `AuditEventType`: `REGISTRATION`, `LOGIN`, `TOKEN_REFRESH`, `TOKEN_REUSE_DETECTED`, `LOGOUT`, `CONTEXT_VIEWED` e, da Fase 1, `COMPANY_PROFILE_UPDATED`, `CONTACT_CREATED`, `VEHICLE_REGISTERED`, `QUOTE_*` | |
| Resultado | `AuditOutcome` (`SUCCESS`, `FAILURE`) | |
| Motivo da falha | `details.reason` (ex.: `BAD_PASSWORD`, `USER_NOT_FOUND`, `LOCKED`) | Só na auditoria; ao cliente a resposta é sempre a mesma. |
| Escopo de acesso | `AccessScope` | Diz ao banco o que a transação pode ver: `Tenant`, `Registration`, `CredentialLookup`, `RefreshTokenLookup`, `Anonymous` e, da Fase 1, `QuoteLinkLookup`, `QuoteDecision`, `QuoteExpiration`. |
| Unidade de trabalho / transação | `UnitOfWork`, `PostgresUnitOfWork` | Único ponto que define o escopo. |
| Isolamento entre empresas | Row Level Security (migração V3) | Reforça, no banco, o que a aplicação já faz. |
| Identificador de correlação | `correlation_id` (`X-Correlation-Id`) | Liga logs e auditoria de uma requisição. |
| Papel do banco "aplicação" | `ERP_DB_APP_USER` | Sem posse, sem superusuário, sujeito a RLS. |
| Papel do banco "migrações" | `ERP_DB_MIGRATOR_USER` | Dono do banco; roda o Flyway. |
| Dinheiro | `Money` sobre `NUMERIC(18,2)` | Duas casas. Digitado é exato (mais de duas casas é recusado); calculado arredonda `HALF_UP`. |

## Fase 1: oficina, orçamento e link público

O produto passou a ser o SaaS "zero digitação" para oficinas mecânicas pequenas. Oficina é a empresa (`Company`); os termos
de financeiro da primeira versão do plano (`LedgerEntry`, `Counterparty` etc.) ficam em `reserva-erp-financeiro/` e **não**
existem no código.

| Termo de negócio | Nome no código | Observação |
|---|---|---|
| Oficina | `Company` | O tenant. |
| Perfil da oficina | `CompanyProfile`, tabela `company_profile` | Nome de exibição, telefone, endereço, horário, validade padrão e assinatura. |
| Cliente da oficina | `Contact`, tabela `contact` | Nome em claro; telefone e e-mail cifrados. Telefone único por oficina. |
| Telefone | `PhoneNumber` (em `domain.shared`) | Brasileiro, guardado em E.164 (`+55...`). Valida a forma, não a operadora. |
| Veículo | `Vehicle`, `LicensePlate` | Placa antiga ou Mercosul; cifrada. Única por oficina. Troca de dono fica fora do escopo. |
| Orçamento | `Quote`, `QuoteItem`, `QuoteItemKind` (`PART`, `LABOR`) | O total é calculado pelo domínio, nunca guardado. |
| Estados do orçamento | `QuoteStatus`: `DRAFT`, `AWAITING_OWNER_CONFIRMATION`, `SENT`, `APPROVED`, `REJECTED`, `EXPIRED`, `CANCELLED` | Rascunho, aguardando o dono, enviado, aprovado, recusado, vencido, cancelado. |
| Quantidade | `Quantity` | Maior que zero, até 9999,999, três casas. |
| Confirmar e enviar | `ConfirmQuoteUseCase` | Único ato que envia ao cliente. Gera o link e entrega depois do commit. |
| Reenviar | `SendQuoteUseCase` | Novo link, mesmo estado e mesma validade. |
| Link público | `QuoteLink`, tabela `quote_public_link` | Só o hash do token no banco. Abrir não gasta o link; só a decisão o consome. |
| Decisão pelo link | `ApproveQuoteByLinkUseCase`, `RejectQuoteByLinkUseCase` | Repetir a mesma decisão não muda nada; a oposta é recusada. |
| Validade | `Quote.expiresAt` | Conta do envio, até o fim do dia (fuso da oficina). Vence em `00:00` do dia seguinte ao último dia válido. |
| Expiração | `ExpireDueQuotesUseCase`, escopo `QuoteExpiration` | Tarefa agendada; o estado derivado (`statusAt`) já vale antes dela rodar. |
| Dono (autorização) | `OwnerGuard` | Confere, dentro do caso de uso, que a pessoa ainda é dona da empresa. |
| Protetor de campo | `FieldProtector`, `TinkFieldProtector` | Cifra com contexto (empresa e campo) e calcula o índice cego (HMAC). |
