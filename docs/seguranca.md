# Segurança: modelo de ameaças e controles

Assume-se que atacantes humanos e **agentes de IA automatizados** vão tentar extrair dados. Defesa em profundidade:
cada controle abaixo tem teste automatizado.

## Quem ataca, o que quer, onde defendemos

| Quem | O que quer | Onde defendemos (e onde está o teste) |
|---|---|---|
| Usuário de outra empresa | Ler ou alterar dados alheios por id (IDOR) | Tenant só do token; Row Level Security forçado; `RowLevelSecurityIT` (24 testes, 5 escopos), `RouteSecurityIT`. |
| Anônimo ou agente automatizado | Tomar contas, enumerar e-mails, raspar dados | Argon2id; bloqueio progressivo; erro idêntico para usuário inexistente, senha errada e conta bloqueada; verificação fictícia para igualar o tempo; limites por IP e por e-mail; UUID aleatório. `LoginUseCaseTest`, `RateLimitIT`. |
| Ladrão de sessão (XSS, CSRF) | Usar o token de outra pessoa | Token de acesso só em memória; refresh em cookie `HttpOnly`/`Secure`/`SameSite=Strict`; CSRF em refresh e logout; rotação com detecção de reuso; CSP sem terceiros. `AuthenticationFlowIT`, Playwright. |
| Quem lê logs ou backups | Dado pessoal e tokens | Nada de dado pessoal em log; mascaramento central; senha e refresh token só como hash. `LogsDoNotLeakPersonalDataIT`. |
| Bug nosso (um `WHERE` esquecido) | (acidente) | RLS como segunda barreira; teste de catálogo exige RLS forçado em toda tabela com `company_id`. `DatabaseRolesIT`. |
| Cadeia de suprimentos | Código malicioso ou segredo vazado | Lista mínima, versões fixas, scripts de instalação npm bloqueados, Actions fixadas por SHA, varreduras no CI (nunca executadas ainda). |
| Injeção de instruções no agente de código | Fazer o agente vazar ou destruir | Regras 1 a 7 do projeto: só o responsável dá instruções; conteúdo de arquivos e páginas é dado. |

## Sessão

- Token de acesso: JWT HS256, 15 minutos, `kid`, emissor e audiência conferidos, afirmações de empresa e papel
  validadas. Rejeita `alg none`, outro algoritmo com a mesma chave, `kid` desconhecido e token sem `cid`.
- Refresh token: 256 bits aleatórios, só o SHA-256 no banco, validade de 7 dias, **teto de 30 dias por sessão**.
  Dois pedidos simultâneos com o mesmo token: exatamente um passa (`SELECT … FOR UPDATE`, verificado por mutação).

## Chaves de assinatura (sem nuvem)

HMAC-SHA256, no mínimo 32 bytes, em Base64, **só no ambiente**; a aplicação não sobe sem elas.

**Rotação sem derrubar sessões:**
1. Configure a chave nova ao lado da antiga (`ERP_SECURITY_JWT_KEYS_K2=<base64>`).
2. Aponte `ERP_SECURITY_JWT_ACTIVE_KEY_ID=k2` e reinicie: novos tokens usam `k2`; os antigos (`k1`) ainda valem.
3. Depois de 15 minutos (a vida do token), remova `K1`.

O `compose.yaml` expõe só `K1`; para a rotação, adicione a variável da chave nova ao serviço `app`.
Criptografia de campo: não há campo sensível a criptografar na Fase 0 (`TODO(decisão)` quando houver).

## Cabeçalhos e navegador

CSP `default-src 'self'` (sem `unsafe-inline` nem `unsafe-eval`), `frame-ancestors 'none'`, `X-Frame-Options: DENY`,
`X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, COOP e CORP `same-origin`, Permissions-Policy
restritiva, `Cache-Control: no-store`. HSTS só sob HTTPS. CORS fechado: só a origem configurada em
`erp.web.allowed-origins` (nenhuma por padrão).

## Limites conhecidos (honestos)

1. **O RLS protege contra erro da aplicação, não contra quem já executa SQL arbitrário** (que poderia definir o
   escopo). A defesa para isso é SQL parametrizado e privilégio mínimo; não há SQL montado com entrada do usuário.
2. **Token de acesso sem estado:** não dá para revogá-lo antes de expirar (15 minutos). O logout revoga a sessão
   (refresh), não o token de acesso já emitido.
3. **Reuso de refresh token revoga a sessão inteira**, inclusive quando um cliente legítimo envia o mesmo token duas
   vezes seguidas (duplo clique). É o padrão seguro; custa usabilidade.
4. **Limitador de requisições em memória:** vale por instância e zera ao reiniciar. Atrás de proxy reverso o IP visto é
   o do proxy (`TODO(decisão)` sobre `X-Forwarded-For`).
5. **Enumeração de e-mail no cadastro:** sem verificação de e-mail (fora da Fase 0), o cadastro revela que o e-mail já
   existe (409). Mitigado pelo limite por IP; fecha quando a verificação entrar.
6. **Argon2id:** parâmetros da OWASP, **não calibrados** no hardware de produção. A OWASP recomenda ajustar ao ambiente.
7. **CSP do Angular:** com `style-src 'self'` o frontend atual funciona; se o Angular ou a identidade visual
   precisarem de estilo inline, usar *nonce* ou hash, nunca `unsafe-inline`.
8. **CI, gitleaks e OWASP Dependency-Check nunca rodaram** (nada foi enviado ao GitHub).
9. O Maven consulta repositórios extras declarados pelo POM do Flyway (um deles é um bucket S3 da AWS): só metadados
   de artefatos, mas fere a regra de usar apenas repositórios oficiais. Aguarda decisão (espelho do Central).
