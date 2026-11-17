# Privacidade (LGPD): rascunhos

> **RASCUNHO PARA REVISÃO JURÍDICA.** Estes documentos descrevem **o que o sistema faz hoje** (fatos técnicos
> verificáveis no código) e deixam em aberto, com `TODO(decisão)`, tudo o que depende de interpretação jurídica,
> de prazo legal ou de decisão do negócio. Nenhuma regra, base legal ou prazo foi inventado aqui. Nada neste
> diretório vale como parecer jurídico.

| Documento | Conteúdo |
|---|---|
| [mapa-de-dados.md](mapa-de-dados.md) | Quais dados pessoais existem na Fase 0, onde ficam e quem os vê. |
| [papeis.md](papeis.md) | Quem é controlador e quem é operador (a definir com o jurídico). |
| [retencao.md](retencao.md) | Política inicial de retenção e o que o sistema ainda **não** faz. |
| [resposta-a-incidentes.md](resposta-a-incidentes.md) | Plano de resposta a incidentes de segurança com dados pessoais. |

## Princípios técnicos já implementados

- **Minimização:** a Fase 0 não coleta CPF, CNPJ, telefone, endereço, IP, agente de navegador nem localização.
- **Isolamento:** uma empresa não lê, altera nem apaga dados de outra (Row Level Security no banco, com testes).
- **Segredos:** senha só como hash Argon2id; refresh token só como hash; nada disso em log.
- **Logs:** sem dado pessoal; mascaramento central de e-mail, CPF, CNPJ e tokens; teste automatizado.
- **Sem terceiros:** nenhum SDK de nuvem, analytics, fonte, CDN ou script externo; nada sai da infraestrutura da aplicação.
- **Dados de desenvolvimento e teste:** sempre sintéticos.
