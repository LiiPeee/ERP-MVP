# Plano de resposta a incidentes de segurança

> **RASCUNHO PARA REVISÃO JURÍDICA.** A obrigação, o prazo e o conteúdo da comunicação à autoridade (ANPD) e aos
> titulares **mudam com a regulamentação e não foram pesquisados aqui**. `TODO(decisão)`: **conferir o prazo
> vigente de comunicação com a assessoria jurídica** antes de usar este plano. Nenhum prazo está afirmado neste
> documento de propósito.

## Fases

1. **Detectar e registrar.** Quem notar (alerta, usuário, pesquisador) abre o registro do incidente com data e hora,
   quem relatou e o que foi observado.
2. **Conter.** Reduzir o dano sem destruir evidência (ver "Ações técnicas de contenção").
3. **Avaliar.** Que dados, de quais empresas e de quantas pessoas; houve acesso, cópia ou alteração; o risco ou dano
   relevante para os titulares. `TODO(decisão)`: critério formal de "risco ou dano relevante".
4. **Comunicar.** Decidir com o jurídico se e quando comunicar a ANPD, os titulares e as empresas clientes (que podem
   ter as suas próprias obrigações). `TODO(decisão)`: prazo vigente, texto-modelo e responsável pela comunicação.
5. **Erradicar e recuperar.** Corrigir a causa, restaurar o serviço, trocar segredos.
6. **Aprender.** Relatório pós-incidente e ajuste de controles, sem culpar pessoas.

## Responsáveis `TODO(decisão)`

| Função | Quem | Contato |
|---|---|---|
| Coordenação do incidente | | |
| Técnico (investigação e contenção) | | |
| Encarregado (DPO) | | |
| Jurídico | | |
| Comunicação com clientes | | |

## Onde está a evidência técnica

- **Auditoria** (`audit_event`): quem entrou, renovou, saiu, consultou o contexto, falhas de login por motivo,
  **reuso de refresh token** (`TOKEN_REUSE_DETECTED`, indício de roubo de sessão) e tentativas de acesso negado.
- **Logs estruturados:** uma linha por requisição, com `correlation_id` e `company_id`; sem dado pessoal.
- O `correlation_id` de uma resposta de erro liga a requisição à auditoria e aos logs.
- **Não há** IP nem agente de navegador registrados (ver mapa de dados). `TODO(decisão)` se isso será necessário.

## Ações técnicas de contenção (já suportadas)

| Situação | Ação |
|---|---|
| Chave de assinatura do token suspeita de vazamento | Configurar uma chave nova (`ERP_SECURITY_JWT_KEYS_<novo id>`), apontar `ERP_SECURITY_JWT_ACTIVE_KEY_ID` para ela e **remover a antiga**: todos os tokens de acesso assinados por ela deixam de valer (vivem no máximo 15 minutos). Os refresh tokens são independentes da chave. |
| Sessões de um usuário comprometidas | Revogar os refresh tokens do usuário (`revoked_at`), com o papel de migrações e registro da operação. Os tokens de acesso já emitidos valem até 15 minutos. |
| Senha de um usuário comprometida | O bloqueio progressivo limita tentativas; a troca de senha **ainda não existe** (fora da Fase 0). `TODO(decisão)`. |
| Credencial do banco comprometida | Trocar `ERP_DB_APP_PASSWORD` e `ERP_DB_MIGRATOR_PASSWORD` e reiniciar. |
| Ataque de força bruta ou abuso | Os limites por IP e por e-mail respondem 429; ajustar `ERP_WEB_RATE_LIMIT_*`. |

## Limites conhecidos

- O token de acesso **não pode ser revogado individualmente** antes de expirar (é sem estado, 15 minutos).
- O limitador de requisições vale por instância e zera ao reiniciar.
- Não há alerta automático: a detecção hoje depende de consultar a auditoria e os logs. `TODO(decisão)`.
