-- Fase 0: empresa, usuário, vínculo, refresh token e auditoria.
-- Chaves primárias em UUID gerado pela aplicação; datas em TIMESTAMPTZ.
-- Dados monetários ainda não existem; quando chegarem serão NUMERIC(18,2).

CREATE TABLE company (
    id         uuid PRIMARY KEY,
    name       varchar(120) NOT NULL,
    created_at timestamptz  NOT NULL
);

-- "user" é palavra reservada no PostgreSQL: sempre entre aspas.
CREATE TABLE "user" (
    id                 uuid PRIMARY KEY,
    email              varchar(254) NOT NULL,
    display_name       varchar(120) NOT NULL,
    password_hash      text         NOT NULL,
    failed_login_count integer      NOT NULL DEFAULT 0,
    locked_until       timestamptz,
    created_at         timestamptz  NOT NULL,
    CONSTRAINT user_email_unique UNIQUE (email),
    CONSTRAINT user_email_is_lower_case CHECK (email = lower(email)),
    CONSTRAINT user_failed_login_count_not_negative CHECK (failed_login_count >= 0)
);

CREATE TABLE company_membership (
    id         uuid PRIMARY KEY,
    company_id uuid        NOT NULL REFERENCES company (id),
    user_id    uuid        NOT NULL REFERENCES "user" (id),
    role       varchar(30) NOT NULL,
    created_at timestamptz NOT NULL,
    CONSTRAINT company_membership_unique UNIQUE (company_id, user_id),
    CONSTRAINT company_membership_role_known CHECK (role IN ('OWNER'))
);
CREATE INDEX company_membership_user_idx ON company_membership (user_id);

-- Só o hash do token é guardado; o token em si nunca toca o banco.
CREATE TABLE refresh_token (
    id                uuid PRIMARY KEY,
    user_id           uuid         NOT NULL REFERENCES "user" (id),
    company_id        uuid         NOT NULL REFERENCES company (id),
    family_id         uuid         NOT NULL,
    token_hash        varchar(128) NOT NULL,
    issued_at         timestamptz  NOT NULL,
    expires_at        timestamptz  NOT NULL,
    family_started_at timestamptz  NOT NULL,
    revoked_at        timestamptz,
    CONSTRAINT refresh_token_hash_unique UNIQUE (token_hash)
);
CREATE INDEX refresh_token_family_idx ON refresh_token (family_id);
CREATE INDEX refresh_token_user_idx ON refresh_token (user_id);

-- company_id e actor_user_id podem ser nulos: falhas antes de saber quem agiu (login de e-mail inexistente).
-- details guarda só códigos técnicos, nunca dado pessoal.
CREATE TABLE audit_event (
    id            uuid PRIMARY KEY,
    occurred_at   timestamptz  NOT NULL,
    company_id    uuid REFERENCES company (id),
    actor_user_id uuid REFERENCES "user" (id),
    event_type    varchar(40)  NOT NULL,
    outcome       varchar(10)  NOT NULL,
    correlation_id varchar(100) NOT NULL,
    details       jsonb        NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT audit_event_outcome_known CHECK (outcome IN ('SUCCESS', 'FAILURE'))
);
CREATE INDEX audit_event_company_time_idx ON audit_event (company_id, occurred_at);
