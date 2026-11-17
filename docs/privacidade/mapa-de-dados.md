# Mapa de dados (Fase 0)

> **RASCUNHO PARA REVISÃO JURÍDICA.** Só os campos que existem nesta fase. Fonte: migrações V1 a V4 e o código.
> A classificação como "dado pessoal" abaixo é técnica e **precisa ser confirmada** pelo jurídico.

## Dados guardados no banco (PostgreSQL)

| Tabela | Campo | Titular | Pessoal? | Finalidade | Observação |
|---|---|---|---|---|---|
| `"user"` | `email` | Usuário (dono) | Sim | Identificar e autenticar | Guardado em minúsculas; único. |
| `"user"` | `display_name` | Usuário | Sim | Exibir quem é o usuário | |
| `"user"` | `password_hash` | Usuário | Segredo | Autenticar | Só o hash Argon2id; a senha nunca é guardada. |
| `"user"` | `failed_login_count`, `locked_until` | Usuário | Sim (comportamento) | Bloqueio progressivo contra adivinhação de senha | |
| `"user"` | `id`, `created_at` | Usuário | Identificador | | UUID aleatório. |
| `company` | `name` | Empresa | `TODO(decisão)` | Exibir a empresa | Pode conter nome de pessoa física (ex.: empresário individual). |
| `company` | `id`, `created_at` | Empresa | Identificador | | |
| `company_membership` | `company_id`, `user_id`, `role`, `created_at` | Usuário | Sim (vínculo) | Saber a que empresa o usuário pertence e com que papel | |
| `refresh_token` | `token_hash`, `user_id`, `company_id`, `family_id`, datas | Usuário | Segredo/sessão | Manter a sessão | Só o hash do token; o token nunca é guardado. |
| `audit_event` | `occurred_at`, `event_type`, `outcome`, `correlation_id` | Usuário | Sim (ação) | Trilha de auditoria e investigação de incidentes | |
| `audit_event` | `company_id`, `actor_user_id` | Usuário | Sim (identificador) | Saber quem fez o quê | Sem chave estrangeira de propósito (V4). |
| `audit_event` | `details` | Usuário | Não deve ter | Código técnico do motivo da falha (ex.: `BAD_PASSWORD`) | Nunca e-mail, nome ou senha (testado). |

## Dados que **não** são coletados

CPF, CNPJ, telefone, endereço, data de nascimento, **endereço IP**, agente de navegador, localização, identificadores
de dispositivo e qualquer dado de pagamento. `TODO(decisão)`: guardar o IP na auditoria pode ser útil à segurança e
pode ser exigível ou vedado; **não é guardado** até o jurídico decidir.

## Dados em trânsito, no navegador e em memória

| Onde | O quê | Observação |
|---|---|---|
| Navegador (memória da aba) | Token de acesso (15 min) | Nunca em `localStorage`, `sessionStorage` nem cookie legível. |
| Navegador (cookie `HttpOnly`) | Refresh token | `Secure`, `SameSite=Strict`, `Path=/api/auth`, sem `Domain`; o JavaScript não o lê. |
| Navegador (cookie legível) | `XSRF-TOKEN` (valor aleatório) | Proteção contra CSRF; não identifica ninguém. |
| Memória da aplicação | Chaves do limitador de requisições | IP e **hash** do e-mail; somem ao reiniciar. |
| Logs | Método, caminho (sem query string), status, duração, `correlation_id`, `company_id` | Mascaramento central; pacotes que imprimem entidades ou URLs completas travados em nível alto. |

## Quem acessa

- A **aplicação** conecta com um papel de banco sem posse, sem superusuário e sujeito ao isolamento por empresa.
- O **papel de migrações** só roda o Flyway.
- Administradores de infraestrutura com acesso ao banco ou aos logs: `TODO(decisão)` (quem, com que controle e registro).

## Transferência a terceiros

Nenhuma. Não há integração externa, analytics, e-mail transacional (o servidor de e-mail do Compose é só de
desenvolvimento e a Fase 0 não envia e-mail), nem serviço de nuvem. `TODO(decisão)`: ao escolher hospedagem e backup,
registrar aqui onde os dados ficam fisicamente e se há transferência internacional.
