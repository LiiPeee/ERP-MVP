# 0007. Cadeia de suprimentos e CI

Data: 16/11/2026. Estado: aceita.

## Decisão
- Versões fixas (pom, `package-lock.json`); actions do GitHub fixadas por SHA de commit.
- CI com: build e testes (JaCoCo), varredura de segredos (gitleaks, binário com hash conferido, histórico inteiro), varredura de dependências (OWASP Dependency-Check), frontend, conferência do cliente gerado, ponta a ponta (Playwright) e varredura da imagem de container (Trivy).
- Dependabot para Maven, npm, GitHub Actions e Docker.
- `.gitleaks.toml` mantém todas as regras padrão. A única exceção exige duas condições juntas: arquivo de teste ou `.env.example`, e marcador explícito de valor falso (`sintetic`, `dev-only`, `fake-`) ou exemplo fixo.
- `.env` nunca vai ao repositório; só `.env.example`, com valores falsos.

## Consequências
Credencial real em código de produção continua sendo detectada. Quem criar um novo valor sintético precisa usar um dos marcadores. O OWASP Dependency-Check precisa do segredo `NVD_API_KEY` no GitHub para rodar sem estrangulamento.
