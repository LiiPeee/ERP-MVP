-- Row Level Security: o banco recusa o acesso mesmo que a aplicação esqueça um WHERE.
--
-- Como funciona: a aplicação abre cada transação definindo, com set_config(..., true), o ESCOPO de acesso
-- (app.scope) e os valores dele (empresa, usuário, e-mail, hash do token). Valem só até o fim da transação.
-- Sem escopo, nenhuma linha é visível. FORCE faz a regra valer também para o dono das tabelas.
--
-- Escopos: anonymous, registration, credential_lookup, refresh_token_lookup, tenant.
-- Limite honesto: isto protege contra erro da aplicação, não contra quem já executa SQL arbitrário
-- (que poderia definir as variáveis). Contra isso valem o SQL parametrizado e o privilégio mínimo.
--
-- As políticas não podem ter referência circular entre tabelas (o PostgreSQL recusa). A ordem de dependência é:
-- audit_event -> company_membership -> refresh_token -> "user"; company não depende de ninguém.

CREATE FUNCTION app_scope() RETURNS text LANGUAGE sql STABLE
    AS $$ SELECT NULLIF(current_setting('app.scope', true), '') $$;
CREATE FUNCTION app_company_id() RETURNS uuid LANGUAGE sql STABLE
    AS $$ SELECT NULLIF(current_setting('app.company_id', true), '')::uuid $$;
CREATE FUNCTION app_user_id() RETURNS uuid LANGUAGE sql STABLE
    AS $$ SELECT NULLIF(current_setting('app.user_id', true), '')::uuid $$;
CREATE FUNCTION app_email() RETURNS text LANGUAGE sql STABLE
    AS $$ SELECT NULLIF(current_setting('app.email', true), '') $$;
CREATE FUNCTION app_token_hash() RETURNS text LANGUAGE sql STABLE
    AS $$ SELECT NULLIF(current_setting('app.token_hash', true), '') $$;
CREATE FUNCTION app_family_id() RETURNS uuid LANGUAGE sql STABLE
    AS $$ SELECT NULLIF(current_setting('app.family_id', true), '')::uuid $$;

-- company ------------------------------------------------------------------------------------------------
ALTER TABLE company ENABLE ROW LEVEL SECURITY;
ALTER TABLE company FORCE ROW LEVEL SECURITY;

CREATE POLICY company_select ON company FOR SELECT
    USING (app_scope() IN ('tenant', 'registration') AND id = app_company_id());
CREATE POLICY company_insert ON company FOR INSERT
    WITH CHECK (app_scope() = 'registration' AND id = app_company_id());

-- "user" -------------------------------------------------------------------------------------------------
ALTER TABLE "user" ENABLE ROW LEVEL SECURITY;
ALTER TABLE "user" FORCE ROW LEVEL SECURITY;

CREATE POLICY user_select ON "user" FOR SELECT
    USING ((app_scope() IN ('credential_lookup', 'registration') AND email = app_email())
        OR (app_scope() = 'tenant' AND id = app_user_id()));
CREATE POLICY user_insert ON "user" FOR INSERT
    WITH CHECK (app_scope() = 'registration' AND email = app_email());
CREATE POLICY user_update ON "user" FOR UPDATE
    USING (app_scope() = 'credential_lookup' AND email = app_email())
    WITH CHECK (app_scope() = 'credential_lookup' AND email = app_email());

-- refresh_token ------------------------------------------------------------------------------------------
-- Na renovação, a aplicação grava app.family_id depois de achar o token pelo hash: a sessão inteira (e só ela)
-- fica visível para revogar e para receber o token novo.
ALTER TABLE refresh_token ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_token FORCE ROW LEVEL SECURITY;

CREATE POLICY refresh_token_select ON refresh_token FOR SELECT
    USING (app_scope() = 'refresh_token_lookup'
           AND (token_hash = app_token_hash() OR family_id = app_family_id()));
CREATE POLICY refresh_token_insert ON refresh_token FOR INSERT
    WITH CHECK ((app_scope() = 'refresh_token_lookup' AND family_id = app_family_id())
             OR (app_scope() = 'credential_lookup' AND user_id IN (SELECT id FROM "user")));
CREATE POLICY refresh_token_update ON refresh_token FOR UPDATE
    USING (app_scope() = 'refresh_token_lookup'
           AND (token_hash = app_token_hash() OR family_id = app_family_id()))
    WITH CHECK (app_scope() = 'refresh_token_lookup'
           AND (token_hash = app_token_hash() OR family_id = app_family_id()));

-- company_membership -------------------------------------------------------------------------------------
ALTER TABLE company_membership ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_membership FORCE ROW LEVEL SECURITY;

CREATE POLICY company_membership_select ON company_membership FOR SELECT
    USING ((app_scope() = 'tenant' AND company_id = app_company_id())
        OR (app_scope() = 'credential_lookup' AND user_id IN (SELECT id FROM "user"))
        OR (app_scope() = 'refresh_token_lookup'
            AND EXISTS (SELECT 1 FROM refresh_token rt
                         WHERE rt.user_id = company_membership.user_id
                           AND rt.company_id = company_membership.company_id)));
CREATE POLICY company_membership_insert ON company_membership FOR INSERT
    WITH CHECK (app_scope() = 'registration' AND company_id = app_company_id());

-- audit_event --------------------------------------------------------------------------------------------
-- Só inserção. A empresa e o autor do evento precisam ser algo que o escopo atual já enxerga (ou nulos, para
-- falhas anteriores a saber quem agiu).
ALTER TABLE audit_event ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_event FORCE ROW LEVEL SECURITY;

CREATE POLICY audit_event_insert ON audit_event FOR INSERT
    WITH CHECK (app_scope() IS NOT NULL
        AND (company_id IS NULL
             OR company_id = app_company_id()
             OR company_id IN (SELECT company_id FROM company_membership)
             OR company_id IN (SELECT company_id FROM refresh_token))
        AND (actor_user_id IS NULL
             OR actor_user_id = app_user_id()
             OR actor_user_id IN (SELECT id FROM "user")
             OR actor_user_id IN (SELECT user_id FROM refresh_token)));
