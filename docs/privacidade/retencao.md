# Política inicial de retenção

> **RASCUNHO PARA REVISÃO JURÍDICA.** Os prazos abaixo são **propostas técnicas**, não obrigações legais. Prazos
> legais (fiscais, trabalhistas, de guarda de registros de acesso) **não foram pesquisados nem aplicados**.
> `TODO(decisão)` em todos os prazos.

## Estado atual (fato)

**O sistema ainda não apaga nada automaticamente.** Não há rotina de expiração, anonimização ou eliminação. Os
dados abaixo permanecem no banco até alguém removê-los manualmente.

| Dado | Hoje | Proposta técnica (a decidir) |
|---|---|---|
| Conta do usuário e vínculo | Fica enquanto existir | Enquanto o contrato estiver ativo; eliminação ou anonimização depois, `TODO(decisão)` do prazo. |
| Empresa | Fica enquanto existir | Idem. |
| Refresh token expirado ou revogado | **Fica para sempre** (a validade só impede o uso) | Expurgar poucos dias depois de expirar. **A implementar.** |
| Evento de auditoria | Fica para sempre | Prazo único a definir com o jurídico; é a base de investigação de incidentes. |
| Logs da aplicação | Depende da infraestrutura de logs (não definida) | Prazo curto e rotação; sem dado pessoal de qualquer forma. `TODO(decisão)` |
| Backups | Não definidos | `TODO(decisão)`: frequência, retenção e criptografia. |

## O que já ajuda

- A auditoria **não tem chave estrangeira** para empresa e usuário (migração V4): apagar uma empresa ou um usuário
  não é bloqueado pelo histórico, e o histórico guarda só identificadores e códigos técnicos.
- A senha e o refresh token nunca são guardados em claro, então um vazamento do banco não entrega credenciais.

## Pendências para decidir e implementar

1. Prazos de cada linha acima (com base legal e suporte do jurídico).
2. Rotina de expurgo de refresh tokens vencidos.
3. Fluxo de **eliminação** da conta e da empresa (e o que acontece com a auditoria: manter, anonimizar ou apagar).
4. Retenção de logs e de backups.
5. Como atender o pedido do titular (acesso, correção, eliminação, portabilidade).
