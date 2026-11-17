# Papéis: controlador e operador

> **RASCUNHO PARA REVISÃO JURÍDICA.** Quem é controlador e quem é operador é uma qualificação jurídica. O texto
> abaixo é só uma **hipótese de trabalho** para o jurídico confirmar ou corrigir.

## Hipótese

| Dados | Quem decide a finalidade e os meios | Papel provável do provedor do sistema |
|---|---|---|
| Dados que cada **empresa cliente** cadastra sobre os seus próprios clientes, fornecedores e funcionários (fases futuras) | A empresa cliente | **Operador** |
| Dados da **conta de acesso** do dono e dos usuários (e-mail, nome, vínculo, auditoria de acesso) | O provedor, para operar e proteger o serviço | **Controlador** (ou controlador conjunto) `TODO(decisão)` |
| Dados de segurança (bloqueio por tentativas, auditoria, logs) | O provedor | **Controlador** `TODO(decisão)` |

Na Fase 0 só existem dados da conta de acesso e de segurança; ainda não há dados de terceiros da empresa cliente.

## Pendências de decisão

- `TODO(decisão)`: confirmar a qualificação acima, inclusive a de "controlador conjunto".
- `TODO(decisão)`: base legal de cada tratamento (execução de contrato, legítimo interesse, obrigação legal...).
- `TODO(decisão)`: nome e contato do encarregado (DPO) e do canal do titular.
- `TODO(decisão)`: contrato de tratamento de dados com as empresas clientes (instruções, subcontratação,
  eliminação ao fim do contrato).
- `TODO(decisão)`: lista de suboperadores (hospedagem, backup, e-mail) quando existirem.

## O que o sistema já oferece para apoiar o cumprimento

- Isolamento técnico entre empresas, com testes.
- Trilha de auditoria de acesso (quem entrou, renovou a sessão, saiu, consultou o contexto).
- Nenhum dado pessoal em logs.
- Direitos do titular (acesso, correção, eliminação, portabilidade): **ainda não há funcionalidade** para isso.
  `TODO(decisão)`: definir o processo e a ordem de implementação.
