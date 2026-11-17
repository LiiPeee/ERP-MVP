# Dependências

Lista mínima, uma linha por dependência: **o que resolve** e **por que não dá para fazer sem**. Todas são
projetos amplamente adotados e mantidos, de fontes oficiais (Maven Central, registro npm, imagens oficiais).
Versões conferidas em 16/11/2026. Versões gerenciadas pelo Spring Boot 4.1.2 não aparecem com número aqui.

## Backend (Maven)

| Dependência | Resolve | Por que não dá para fazer sem |
|---|---|---|
| spring-boot-starter-parent 4.1.2 | Versões compatíveis de tudo e configuração do build | Alinha centenas de versões; fazer à mão é a maior fonte de incompatibilidade. |
| spring-boot-starter-webmvc | Servidor HTTP e controllers REST | A API REST. |
| spring-boot-starter-security | Cadeia de filtros: CSRF, cabeçalhos, autorização | Escrever isso à mão é a forma mais comum de errar segurança. |
| spring-boot-starter-security-oauth2-resource-server | Validação do JWT no cabeçalho `Authorization` | Validação de assinatura e claims testada pela comunidade, em vez de código próprio. |
| spring-boot-starter-validation | Limites de tamanho na borda | Recusar entrada gigante antes de gastar CPU. |
| spring-boot-starter-actuator | `/actuator/health` (só `health` exposto) | Healthcheck do Docker. |
| spring-boot-starter-data-jpa (Hibernate, Spring Data) | Mapeamento entidade↔tabela e repositórios | Escolha do projeto (entidades JPA separadas do domínio). |
| spring-boot-starter-jdbc / JdbcClient | Definir as variáveis de sessão do RLS por transação | Único ponto que fala SQL direto (`tenancy`). |
| spring-boot-starter-flyway + flyway-database-postgresql | Migrações versionadas do banco | Esquema reproduzível do zero. |
| postgresql (driver JDBC) | Falar com o PostgreSQL | Obrigatório. |
| springdoc-openapi-starter-webmvc-api 3.2.0 | Gera o contrato OpenAPI dos controllers (sem interface gráfica) | Fonte do cliente do frontend. |
| spring-security-crypto | `Argon2PasswordEncoder` | Argon2id sem implementar criptografia. |
| bcprov-jdk18on 1.87 (BouncyCastle) | Primitivas do Argon2 | O `Argon2PasswordEncoder` do Spring Security exige (documentado). |
| spring-security-oauth2-jose | Emite (`NimbusJwtEncoder`) e valida (`NimbusJwtDecoder`) o token | Uma só família de bibliotecas para emitir e validar. |
| spring-boot + logback-classic | Log estruturado em JSON e o ponto único de mascaramento | `StructuredLoggingJsonMembersCustomizer`. |

### Testes e qualidade (Maven)

| Dependência | Resolve |
|---|---|
| JUnit Jupiter, AssertJ, spring-boot-starter-test (inclui Awaitility) | Testes e asserções. |
| Testcontainers (junit-jupiter, postgresql) 2.0.5 | PostgreSQL real descartável nos testes de integração, contrato e isolamento. |
| ArchUnit 1.5.1 | Regras de arquitetura (dependência entre módulos, convenções) como teste. |
| jacoco-maven-plugin 0.8.16 | Cobertura de ramos (mínimo 90% em domain e application). |
| maven-enforcer-plugin, maven-toolchains-plugin 3.3.0 | Garantem Java 21 e Maven compatível. |
| maven-failsafe-plugin, maven-jar-plugin (test-jar) | Testes de integração e reaproveitamento dos contratos das portas. |
| openapi-generator-maven-plugin 7.27.0 | Gera o cliente Angular do contrato (perfil `generate-client`). |
| dependency-check-maven 13.0.0 (OWASP) | Varredura de vulnerabilidades das dependências no CI. |
| Maven Wrapper 3.3.4 → Maven 3.10.1 | Mesma versão do Maven em toda máquina, com hash conferido. |

## Frontend (npm, versões exatas)

| Dependência | Resolve |
|---|---|
| @angular/{core,common,compiler,forms,platform-browser,router} 22.2.6 | O framework; formulários tipados e rotas com carregamento tardio. |
| rxjs 7.8.2, tslib 2.9.1 | Exigidas pelo Angular. |
| @angular/build, @angular/cli 22.2.7, @angular/compiler-cli 22.2.6 | Compilação e ferramenta de linha de comando. |
| typescript 6.0.3 | Versão exigida pelo Angular 22 (`>=6.0 <6.1`). |
| vitest 5.1.0, jsdom 30.1.3 | Testes unitários do frontend. |
| prettier 3.10.1 | Formatação (vem do esqueleto do Angular). |
| @testing-library/angular 19.6.0, @testing-library/dom 10.4.2, @testing-library/user-event 14.6.12 | Testes de componente pelo que o usuário vê e faz (papéis, rótulos, teclado), sem depender da estrutura interna. Padrão consolidado; só dev. |

Nenhuma biblioteca de interface visual, ícone, CDN ou analytics: o frontend não carrega nada de terceiros em tempo de
execução (e a CSP impede). As fontes são arquivos locais (ver [docs/design/fontes.md](design/fontes.md)); o diálogo usa o `<dialog>` nativo. O `.npmrc` bloqueia scripts de instalação de pacotes.

## Testes de ponta a ponta (npm)

| Dependência | Resolve |
|---|---|
| @playwright/test 1.65.1 | Teste do fluxo no navegador real. |
| @axe-core/playwright 4.14.0 | Verificação automática de acessibilidade (WCAG A e AA) nas telas, dentro do Playwright. Só dev. |

## Imagens e ferramentas

| Item | Uso |
|---|---|
| postgres:18.7-alpine | Banco (Compose e testes). |
| axllent/mailpit v1.33.1 | Servidor SMTP de desenvolvimento no Compose (sem uso na Fase 0). |
| eclipse-temurin 21.0.13_9 (jdk e jre) | Build e execução da aplicação. |
| node:24.22.3-bookworm-slim | Build do Angular na imagem. |
| gitleaks 8.30.1 | Varredura de segredos no CI (hash do binário conferido). |
| aquasecurity/trivy-action 0.36.0 (fixada por SHA) | Varredura da imagem de container no CI (altas e críticas com correção disponível). Mantida pela Aqua Security, amplamente usada; sem ela a imagem não seria varrida. |
| Dependabot (recurso do GitHub, `.github/dependabot.yml`) | Atualização automática de Maven, npm, Actions e Docker, por pull request. |
| GitHub Actions: checkout 7.0.1, setup-java 6.1.0, setup-node 7.1.0, upload-artifact 7.0.2 | CI, todas **fixadas por SHA**. |

## O que ficou de fora de propósito

Redis, mensageria, cache, SDK de qualquer nuvem, biblioteca de componentes visuais, biblioteca de limite de requisições
(o limitador próprio tem ~80 linhas e é testado), biblioteca de JSON para a auditoria (um serializador de 40 linhas,
testado contra o próprio PostgreSQL), Lombok.
