# erp-core

ERP SaaS multiempresa para micro e pequenas empresas brasileiras. Esta é a **Fase 0**: o alicerce (arquitetura,
isolamento entre empresas, segurança, testes) provado por um fluxo mínimo de ponta a ponta: **cadastrar a empresa
e o dono, entrar, renovar a sessão, ver quem sou eu e sair**, com auditoria de cada ação. Não há funcionalidade de
negócio (financeiro, cadastros, relatórios) nem identidade visual: as telas são descartáveis.

> Nenhum dado real deve ser usado em desenvolvimento ou teste. Todos os dados de exemplo são sintéticos.

## Princípios

1. **Simplicidade:** uma aplicação implantável (monólito modular); cada problema tem uma forma de ser resolvido.
2. **Testabilidade:** a regra de negócio roda sem framework, banco ou rede.
3. **Privacidade por padrão:** nenhum dado pessoal vaza para outra pessoa, outro sistema ou outro agente de IA.
4. **Sem nuvem proprietária:** nenhum SDK de nuvem; o que seria serviço de nuvem fica atrás de uma porta.

## Estrutura

```
backend-java/            monólito modular em Java (Maven multi-módulo)
  domain/                regras de negócio, Java puro (sem Spring, JPA ou Jackson)
  application/           casos de uso (um por classe, método execute) e portas
  adapter-web/           controllers REST, segurança HTTP, erros, limites de requisição
  adapter-persistence/   entidades JPA, adaptadores, migrações Flyway, Row Level Security
  adapter-infra/         Argon2id, refresh token, JWT, ids, mascaramento de logs
  bootstrap/             configuração do Spring, injeção de dependências, main
frontend-angular/        Angular 22 (rascunho) e cliente HTTP gerado do OpenAPI
e2e/                     testes de ponta a ponta (Playwright)
openapi/                 contrato da API (fonte do cliente do frontend)
infra/                   scripts do banco (papéis)
docs/                    documentação (dependências, glossário, segurança, privacidade, relatório)
compose.yaml, Dockerfile, .env.example, .github/   execução e CI de tudo
```

A regra da dependência (o código só depende de camadas mais internas) é imposta pelo compilador (módulos Maven)
**e** por testes de arquitetura (ArchUnit). Detalhes em [docs/arquitetura.md](docs/arquitetura.md).

## Versões (conferidas em 16/11/2026 nas fontes oficiais)

| Item | Versão | Por que |
|---|---|---|
| Java | **21 LTS** (Temurin 21.0.13) | Pedido do projeto; fixo por `maven.compiler.release`, toolchain e enforcer. Spring Boot 4.1 aceita de 17 a 27. |
| Spring Boot | **4.1.2** | Última versão estável da linha 4.1.x (suporte aberto até 31/07/2027). O 4.2 ainda era RC; subir é um passo separado. |
| Angular | **22.2.6** (CLI 22.2.7) | Major estável vigente (lançada em 03/06/2026). Exige Node `^22.22.3 \|\| ^24.15.0`, TypeScript 6.0. |
| Node | **24.22.3** | Satisfaz o Angular 22; usado no Docker e no CI. |
| PostgreSQL | **18.7** | Versão suportada até 11/2030. |
| Maven (wrapper) | **3.10.1** | Recomendada na página oficial; o Maven 4 ainda era RC. |
| Flyway | 12.4.0 | Gerenciada pelo Spring Boot 4.1.2. |
| Testcontainers | 2.0.5 | Gerenciada pelo Spring Boot 4.1.2 (compatível com o Docker 29). |
| springdoc-openapi | 3.2.0 | A matriz oficial liga Boot 4.1.x a springdoc 3.1.x–3.2.x. |
| Playwright | 1.65.1 | Última estável conferida. |

Todas as versões estão **fixas** (pom, `package.json` e `package-lock.json`). A lista completa, com o motivo de cada
dependência, está em [docs/dependencias.md](docs/dependencias.md).

## Como rodar

Requisitos: **JDK 21**, **Docker** (com Compose) e, para o frontend, **Node 24.22.3**.

```bash
cp .env.example .env        # na raiz do repositório; valores falsos de desenvolvimento; troque-os em qualquer ambiente real
docker compose up --build   # aplicação (com o Angular), PostgreSQL e Mailpit, tudo em 127.0.0.1
```

A aplicação (API e telas) fica em <http://localhost:8080>. O Mailpit (<http://localhost:8025>) está no Compose para
o futuro; **a Fase 0 não envia e-mail**. Nada no Compose usa serviço de nuvem.

A aplicação **não sobe** sem a chave de assinatura do token (`ERP_SECURITY_JWT_ACTIVE_KEY_ID` e
`ERP_SECURITY_JWT_KEYS_<ID>`, em Base64, com no mínimo 32 bytes). Gere uma por ambiente: `openssl rand -base64 48`.

## Testes

```bash
cd backend-java && ./mvnw verify          # backend: unitários, ArchUnit, JaCoCo (mínimo de 90% de ramos) e integração com PostgreSQL real
cd frontend-angular && npm test           # frontend (Vitest)
cd e2e && npx playwright test             # ponta a ponta contra a pilha do Compose (veja abaixo)
```

- Os testes de integração usam **Testcontainers** (PostgreSQL real); precisam do Docker rodando.
- **Máquinas com pouca RAM** (menos de uns 6 GB livres): o Docker Desktop ocioso segura cerca de 1,8 GB e o build pode
  cair por falta de memória nativa. Rode em duas etapas, encerrando o Docker no meio:
  `./mvnw verify -pl domain,application,adapter-infra,adapter-web` e depois
  `./mvnw verify -pl adapter-persistence,bootstrap -am` (dentro de `backend-java/`). Os heaps já são limitados em `.mvn/jvm.config` e no pom raiz.
- No **Windows**, o `mvnw.cmd` chamado do PowerShell 7 falha em alguns ambientes; use o `./mvnw` pelo Git Bash.
- **Ponta a ponta:** o teste cria várias contas e o limite padrão é 5 cadastros por hora por IP. Suba a pilha assim:
  `ERP_WEB_RATE_LIMIT_REGISTER_PER_IP=1000 ERP_WEB_RATE_LIMIT_LOGIN_PER_IP=1000 ERP_WEB_RATE_LIMIT_LOGIN_PER_EMAIL=1000 docker compose up --build`.
  Sem baixar navegador, use o Edge instalado: `PLAYWRIGHT_CHANNEL=msedge npx playwright test`.

## Contrato da API e cliente do frontend

`openapi/openapi.json` é o contrato. Se a API mudar, o teste `OpenApiContractIT` falha até o arquivo ser atualizado:

```bash
cd backend-java
./mvnw verify -pl bootstrap -am -Dit.test=OpenApiContractIT -Dopenapi.update=true   # atualiza o contrato
./mvnw -N -Pgenerate-client generate-sources                                        # regenera o cliente Angular (em frontend-angular/src/app/api)
```

O cliente gerado (`frontend-angular/src/app/api`) é versionado; o CI confere que ele está igual ao gerado.

## Segurança em resumo

Isolamento entre empresas por **Row Level Security** forçado no PostgreSQL; senhas com **Argon2id**; sessão com
token de acesso de 15 minutos e **refresh token com rotação** (só o hash no banco; reuso revoga a sessão); cookie
`HttpOnly`/`Secure`/`SameSite=Strict`; CSRF onde o cookie é a credencial; CSP sem terceiros; limites de requisição;
logs sem dado pessoal. O modelo de ameaças, os controles e os limites conhecidos estão em
[docs/seguranca.md](docs/seguranca.md).

## Privacidade e jurídico

Os documentos de [docs/privacidade/](docs/privacidade/) são **rascunhos para revisão jurídica**. Nenhuma regra
jurídica foi inventada: onde falta decisão há `TODO(decisão)`.

## Decisões em aberto

- Espelho do Maven Central: o POM do Flyway declara repositórios extras (Gradle, um bucket S3 da AWS, GitHub
  Packages e um feed da Redgate) que o Maven consulta ao resolver o Flyway.
- Como confiar em `X-Forwarded-For` atrás de proxy reverso (limites de requisição por IP).
- Nome definitivo do produto e domínio (cookie e CORS): provisório em desenvolvimento.

Relatório final da fase: [docs/relatorios/fase-0.md](docs/relatorios/fase-0.md). Roteiro de teste manual: [docs/roteiro-teste-manual.md](docs/roteiro-teste-manual.md). Decisões: [docs/adr/](docs/adr/).
