# Mapa de dados (Fases 0 e 1)

> **RASCUNHO PARA REVISÃO JURÍDICA.** Só os campos que existem hoje. Fonte: migrações V1 a V6 e o código.
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

### Fase 1: oficina, clientes da oficina, veículos e orçamentos

Papéis (rascunho, ver `papeis.md`): para os dados dos **clientes da oficina** (contato, veículo, orçamento), a oficina é
**controladora** e nós somos **operadores**. Os campos abaixo marcados "cifrado" ficam em `bytea`, com AES-256-GCM presa à
empresa e ao campo (Tink); onde há busca exata, há também um **índice cego** (HMAC-SHA256, com chave separada), que não
revela o valor. Em claro no banco, só o nome do cliente (a busca é por nome) e o texto do orçamento.

| Tabela | Campo | Titular | Pessoal? | Finalidade | Observação |
|---|---|---|---|---|---|
| `company_profile` | `display_name`, `address`, `opening_hours`, `signature_message` | Oficina (pode ser pessoa física) | `TODO(decisão)` | Como a oficina se apresenta ao cliente | Em claro. |
| `company_profile` | `business_phone_ciphertext` | Oficina | `TODO(decisão)` | Contato da oficina | **Cifrado.** A API devolve só mascarado. |
| `contact` | `name` e `name_search` | Cliente da oficina | **Sim** | Identificar o cliente no orçamento e na busca | **Em claro**, por decisão: a busca é por nome. `name_search` é o mesmo nome sem acento e sem maiúsculas. |
| `contact` | `phone_ciphertext`, `phone_blind_index` | Cliente da oficina | **Sim** | Contato e unicidade por oficina | **Cifrado** + índice cego. API: mascarado `(11) *****-4321`. |
| `contact` | `email_ciphertext` | Cliente da oficina | **Sim** | Contato opcional | **Cifrado.** API: mascarado `a***@dominio`. |
| `vehicle` | `plate_ciphertext`, `plate_blind_index` | Cliente da oficina (a placa identifica uma pessoa) | **Sim** | Identificar o veículo e evitar duplicidade | **Cifrado** + índice cego. API do dono: `****234`. Página pública: só os 3 últimos. |
| `vehicle` | `brand`, `model`, `year`, `odometer_km` | Cliente da oficina | Indireto | Descrever o veículo no orçamento | Em claro. |
| `quote` | `note`, `decision_reason` | Cliente da oficina | **Pode conter** | Observação do orçamento; motivo da recusa digitado pelo cliente | Texto livre, **não confiável**: pode conter dado pessoal. Escapado na saída, limitado em tamanho, nunca em log. |
| `quote`, `quote_item` | estado, datas, descrição, quantidade, preço | Cliente da oficina | Indireto | Orçamento | Itens só de acréscimo (histórico de revisões). O total não é guardado. |
| `quote_public_link` | `token_hash`, datas | Cliente da oficina | Segredo/sessão | Link de aprovação | Só o **hash** do token; o token nunca toca o banco. |
| `audit_event` | `details` (`quoteId`, `contactId`, `vehicleId`) | Cliente da oficina | Identificador | Trilha | Só identificadores, nunca nome, telefone, e-mail, placa ou texto de orçamento (testado). |

**Base legal e retenção:** `TODO(decisão)` com a assessoria. Hipótese de trabalho, a validar: tratamento necessário à execução
de procedimentos preliminares de contrato a pedido do titular (orçar um serviço), com a oficina como controladora. Prazo de
guarda de orçamentos, de contatos sem movimento e de links vencidos: **a definir**, junto com a retenção da Fase 7.

## Dados que **não** são coletados

CPF, CNPJ, data de nascimento, **endereço IP**, agente de navegador, localização, identificadores
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
