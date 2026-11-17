# Relatório final da Fase 0

Data: 16/11/2026. Estado: **código completo e testado nesta máquina; o CI foi executado e falhou na primeira vez (permissão do `mvnw` e gitleaks); as correções estão commitadas localmente e o CI só será reverificado depois do envio ao GitHub. Nada verificado em máquina limpa.**

## O que foi feito

Um alicerce de ERP SaaS multiempresa em monólito modular (Clean Architecture, 6 módulos Maven + Angular), provado por
um fluxo de ponta a ponta: **cadastrar empresa e dono, entrar, renovar a sessão com rotação, sair e ver quem sou eu**,
com auditoria de cada ação.

| Passo | Entrega | Verificação |
|---|---|---|
| 1 | Plano, versões nas fontes oficiais, DER, ameaças | Aprovado |
| 2 | Esqueleto: módulos, ArchUnit, Docker Compose, CI, Flyway | `docker compose up` saudável |
| 3 | Domínio e 5 casos de uso com fakes em memória, auditoria, contratos das portas | Domínio 93,3% e aplicação 94,1% de cobertura de ramos |
| 4 | PostgreSQL: migrações, adaptadores JPA, Row Level Security, isolamento entre empresas | Vazamento provado sem RLS (20 de 24 falharam) e bloqueado com RLS |
| 5 | Argon2id, JWT, controllers, Spring Security, CSRF, CSP, CORS, limites, logs sem dado pessoal, OpenAPI | Teste de todas as rotas; teste de logs |
| 6 | Angular 22, cliente gerado, telas descartáveis, Playwright | 5 testes no navegador real |
| 7 | README, dependências, glossário, privacidade (rascunhos), segurança, arquitetura, este relatório | |

**Testes (nesta máquina, tudo verde):** backend **343** (domain 72, application 72, adapter-web 6, adapter-infra 50,
adapter-persistence 73 incluindo 69 de integração com PostgreSQL real, bootstrap 70 incluindo 16 de arquitetura e
54 de integração HTTP), frontend **9**, ponta a ponta **5**. Total **357**. Cada ciclo de implementação foi
commitado separadamente (histórico mostra vermelho e verde).

## Definição de pronto: situação honesta

| Critério | Situação |
|---|---|
| Testes escritos antes do código, todos passando | **Majoritariamente.** O ciclo vermelho→verde está no histórico para domínio, aplicação, RLS e várias partes da borda. Exceções: os testes de contrato nasceram depois dos fakes (verdes de saída; o vermelho veio no banco real) e alguns testes de borda foram escritos junto do código. |
| ArchUnit verde; cobertura de domain e application ≥ 90% | **Sim** (93,3% e 94,1% de ramos; o JaCoCo falha o build abaixo de 90%). |
| `./mvnw verify` e `docker compose up` do zero em máquina limpa | **Só nesta máquina, e com ressalva:** o `verify` em uma única execução cai por falta de memória aqui; rodei em duas etapas. Nunca testei em máquina limpa. |
| Varreduras de segredos e dependências sem pendência crítica | **Parcial.** gitleaks rodou localmente sobre todo o histórico: 30 achados, todos valores sintéticos de teste/desenvolvimento, agora cobertos por uma allowlist estreita (`.gitleaks.toml`, ADR 0007) e com 0 achados; segredo falso fora das condições continua sendo detectado. `npm audit`: 0 vulnerabilidades. **Não verificado:** OWASP Dependency-Check e Trivy (novo job de imagem), que só rodam no CI; o OWASP precisa do segredo `NVD_API_KEY`. |
| Isolamento entre empresas em todas as rotas; nenhum dado pessoal em logs | **Sim**, com testes. |
| Playwright do fluxo | **Sim** (no Edge; Chromium só no CI). |
| Relatório final | Este documento. |

## O que os testes acharam (bugs reais, todos corrigidos)

1. Token válido com empresa inexistente devolvia **500** (a auditoria violava uma chave estrangeira): migração V4.
2. Token sem a afirmação de papel lançava `NullPointerException` no decodificador.
3. Com o log em DEBUG vazavam o **conteúdo de entidades** (Hibernate) e **URLs com query string** (Spring): pacotes travados.
4. O mascarador transformava um **UUID em CPF**: padrões corrigidos.
5. O `Set-Cookie` do `ResponseEntity` substituía o cookie CSRF.
6. O Spring Security **apagava o cookie CSRF** a cada requisição autenticada.
7. O cookie CSRF tinha caminho restrito e o JavaScript não o lia.
8. O contrato OpenAPI declarava `*/*` e o cliente gerado lia o login como texto.
9. A guarda de rota do Angular perdia o contexto de injeção (NG0203).
10. Sem bloqueio de linha, dois refresh simultâneos passavam os dois (gasto duplo): provado por mutação.

## O que **não** foi verificado

- **CI após as correções**, **OWASP Dependency-Check**, **Trivy** e o job de ponta a ponta no Chromium (dependem de enviar ao GitHub).
- **Máquina limpa**, outro sistema operacional, outro navegador além do Edge.
- **HTTPS real** (HSTS, cookie `Secure` em domínio real).
- **Várias instâncias**, carga, desempenho; os parâmetros do Argon2id não foram calibrados no hardware alvo.
- Recuperação de desastre, backup e restauração.
- A revisão jurídica de tudo em `docs/privacidade/`.

## Riscos

| Risco | Impacto | Mitigação ou próximo passo |
|---|---|---|
| Limites de requisição em memória | Não valem entre instâncias; zeram ao reiniciar | Armazenamento compartilhado quando houver mais de uma instância |
| IP atrás de proxy reverso | Limite por IP vê o IP do proxy | `TODO(decisão)` sobre `X-Forwarded-For` |
| Token de acesso sem revogação individual | Até 15 minutos de uso após um roubo | Vida curta; revogar a sessão (refresh) e trocar a chave se necessário |
| Reuso de refresh derruba a sessão | Duplo clique legítimo desloga | Decidir se aceita janela de tolerância |
| RLS não protege contra SQL arbitrário | Injeção de SQL poderia definir o escopo | Só SQL parametrizado; revisão contínua |
| Repositórios extras do POM do Flyway | Consultas a hosts (um é bucket da AWS) | Espelho do Maven Central (aguarda decisão) |
| Máquina de desenvolvimento com pouca RAM | Build e testes caem sem cuidado | Duas etapas; heaps limitados; documentado no README |
| Sem rotina de expurgo | Refresh tokens vencidos e auditoria acumulam | Implementar com a política de retenção |
| Enumeração de e-mail no cadastro (409) | Revela que uma conta existe | Limite por IP; fechar com verificação de e-mail |

## O que depende de revisão jurídica

Tudo em `docs/privacidade/`, em especial: qualificação controlador/operador; bases legais; **prazo vigente de
comunicação de incidentes** (não afirmado em lugar nenhum); prazos de retenção (inclusive registros de acesso, fiscais
e trabalhistas); guardar ou não o **IP** na auditoria; se o **nome da empresa** é dado pessoal; processo para os
direitos do titular; encarregado e canais; contrato de tratamento com as empresas clientes; suboperadores e
transferência internacional na hospedagem.

## Decisões que tomei durante o trabalho (para você confirmar ou reverter)

- **Filtro de tenant:** sem `@TenantId` do Hibernate (cadastro e login acontecem antes de existir tenant). O ponto
  único é o `PostgresUnitOfWork` (escopo), e o Row Level Security é quem filtra.
- **Configuração dos módulos:** `PersistenceConfiguration` e `WebConfiguration` ficam no próprio módulo e o `bootstrap`
  as importa (foge um pouco de "configuração no bootstrap"); os casos de uso são registrados no `bootstrap`.
- **Auditoria sem chave estrangeira** (V4) e **só de inserção** para a aplicação.
- **Cadastro com entrada inválida não gera auditoria** (para um robô não encher a tabela).
- **Adaptador de e-mail (SMTP) não foi implementado:** nenhuma funcionalidade da Fase 0 envia e-mail; o Mailpit está
  no Compose para o futuro.
- **Serializador de JSON próprio e pequeno para a auditoria** (para não adicionar uma biblioteca de JSON só para isso).
- **Frontend servido pelo próprio Spring** (mesma origem), com o cliente HTTP gerado e versionado.
- **Node 24.22.3 instalado via `nvm install` sem `nvm use`**, para não afetar outros projetos da máquina; o Docker
  Desktop foi instalado e a JDK 21 também (ambos pelo `winget`, a seu pedido).
- **`.wslconfig`:** acrescentei a linha `[wsl2]` (com a sua autorização) para o Docker subir.

## Decisões em aberto

1. Espelho do Maven Central.
2. `X-Forwarded-For` atrás de proxy reverso.
3. Nome definitivo do produto e domínio (cookie e CORS).
4. Gestão de chaves além do ambiente (arquivo montado, cofre de segredos local) para produção.
5. Janela de tolerância no reuso do refresh token.
6. Guardar o IP na auditoria.
7. Identidade visual (fase seguinte): ajustar a CSP se precisar de estilo inline (nonce ou hash).

## Próximos passos sugeridos

1. Enviar as correções ao GitHub (sob seu controle), **conferir o CI verde** e cadastrar o segredo `NVD_API_KEY`.
2. Decidir o espelho do Maven e o proxy reverso.
3. Revisão jurídica dos rascunhos; definir retenção e implementar o expurgo.
4. Verificação de e-mail e troca/recuperação de senha (fora da Fase 0).
5. Calibrar o Argon2id no hardware de produção.

## Fechamento da Fase 0

- ADRs em `docs/adr/` (0001 a 0007).
- Roteiro de teste manual: [docs/roteiro-teste-manual.md](../roteiro-teste-manual.md).
- Capturas desktop e celular com autocrítica visual: [docs/capturas/fase-0/](../capturas/fase-0/README.md).
- CI: job de varredura de imagem (Trivy) e `dependabot.yml`.
- Revisão independente (`revisao-independente.md`) ainda por fazer, em **sessão nova**.
