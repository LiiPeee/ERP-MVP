-- Fase 1: perfil da oficina, clientes (contatos) e veículos.
--
-- Dado pessoal de terceiro (telefone, e-mail, placa) fica CIFRADO (bytea) e, quando a busca por valor exato é
-- necessária, com um ÍNDICE CEGO (HMAC em hexadecimal, 64 caracteres). Nunca em claro. O nome do cliente é a única
-- exceção declarada: a busca é por nome (sem diferenciar caixa e acento), então a coluna name_search guarda o nome
-- já normalizado. Está registrado em docs/privacidade/.
--
-- Toda chave estrangeira entre tabelas de oficina inclui company_id (chave composta): é impossível apontar para o
-- cliente ou o veículo de outra oficina, mesmo por erro da aplicação.

CREATE FUNCTION app_quote_id() RETURNS uuid LANGUAGE sql STABLE
    AS $$ SELECT NULLIF(current_setting('app.quote_id', true), '')::uuid $$;

-- company_profile ----------------------------------------------------------------------------------------
CREATE TABLE company_profile (
    company_id                uuid PRIMARY KEY REFERENCES company (id),
    display_name              varchar(120) NOT NULL,
    business_phone_ciphertext bytea,
    address                   varchar(200) NOT NULL DEFAULT '',
    opening_hours             varchar(200) NOT NULL DEFAULT '',
    default_validity_days     smallint     NOT NULL,
    signature_message         varchar(300) NOT NULL DEFAULT '',
    CONSTRAINT company_profile_validity_range CHECK (default_validity_days BETWEEN 1 AND 90)
);

-- contact ------------------------------------------------------------------------------------------------
CREATE TABLE contact (
    id                uuid PRIMARY KEY,
    company_id        uuid         NOT NULL REFERENCES company (id),
    name              varchar(120) NOT NULL,
    name_search       varchar(120) NOT NULL,
    phone_ciphertext  bytea        NOT NULL,
    phone_blind_index varchar(64)   NOT NULL,
    email_ciphertext  bytea,
    created_at        timestamptz  NOT NULL,
    CONSTRAINT contact_phone_unique_per_company UNIQUE (company_id, phone_blind_index),
    CONSTRAINT contact_id_company_unique UNIQUE (id, company_id),
    CONSTRAINT contact_name_search_lower CHECK (name_search = lower(name_search)),
    CONSTRAINT contact_phone_blind_index_length CHECK (length(phone_blind_index) = 64)
);
CREATE INDEX contact_company_created_idx ON contact (company_id, created_at DESC);

-- vehicle ------------------------------------------------------------------------------------------------
CREATE TABLE vehicle (
    id                uuid PRIMARY KEY,
    company_id        uuid        NOT NULL REFERENCES company (id),
    contact_id        uuid        NOT NULL,
    plate_ciphertext  bytea       NOT NULL,
    plate_blind_index varchar(64)  NOT NULL,
    brand             varchar(60) NOT NULL,
    model             varchar(60) NOT NULL,
    year              smallint    NOT NULL,
    odometer_km       integer,
    created_at        timestamptz NOT NULL,
    CONSTRAINT vehicle_plate_unique_per_company UNIQUE (company_id, plate_blind_index),
    CONSTRAINT vehicle_id_company_unique UNIQUE (id, company_id),
    CONSTRAINT vehicle_contact_same_company FOREIGN KEY (contact_id, company_id) REFERENCES contact (id, company_id),
    CONSTRAINT vehicle_plate_blind_index_length CHECK (length(plate_blind_index) = 64),
    CONSTRAINT vehicle_year_range CHECK (year BETWEEN 1950 AND 2100),
    CONSTRAINT vehicle_odometer_range CHECK (odometer_km IS NULL OR odometer_km BETWEEN 0 AND 9999999)
);
CREATE INDEX vehicle_contact_idx ON vehicle (company_id, contact_id, created_at);

-- Privilégios mínimos: nenhum DELETE. UPDATE só nas colunas que o negócio altera.
GRANT SELECT, INSERT ON company_profile TO "${app_role}";
GRANT UPDATE (display_name, business_phone_ciphertext, address, opening_hours, default_validity_days,
              signature_message) ON company_profile TO "${app_role}";

GRANT SELECT, INSERT ON contact TO "${app_role}";
GRANT UPDATE (name, name_search, phone_ciphertext, phone_blind_index, email_ciphertext) ON contact TO "${app_role}";

GRANT SELECT, INSERT ON vehicle TO "${app_role}";
GRANT UPDATE (plate_ciphertext, plate_blind_index, brand, model, year, odometer_km) ON vehicle TO "${app_role}";

-- Row Level Security -------------------------------------------------------------------------------------
-- Estas tabelas são só do escopo "tenant" (a oficina autenticada). O vínculo com o veículo no escopo de decisão
-- do link público é concedido em V6, depois que a tabela de orçamentos existe.
ALTER TABLE company_profile ENABLE ROW LEVEL SECURITY;
ALTER TABLE company_profile FORCE ROW LEVEL SECURITY;
CREATE POLICY company_profile_select ON company_profile FOR SELECT
    USING ((app_scope() = 'tenant' AND company_id = app_company_id())
        OR (app_scope() = 'quote_decision' AND company_id = app_company_id()));
CREATE POLICY company_profile_insert ON company_profile FOR INSERT
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());
CREATE POLICY company_profile_update ON company_profile FOR UPDATE
    USING (app_scope() = 'tenant' AND company_id = app_company_id())
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());

ALTER TABLE contact ENABLE ROW LEVEL SECURITY;
ALTER TABLE contact FORCE ROW LEVEL SECURITY;
CREATE POLICY contact_select ON contact FOR SELECT
    USING (app_scope() = 'tenant' AND company_id = app_company_id());
CREATE POLICY contact_insert ON contact FOR INSERT
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());
CREATE POLICY contact_update ON contact FOR UPDATE
    USING (app_scope() = 'tenant' AND company_id = app_company_id())
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());

ALTER TABLE vehicle ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicle FORCE ROW LEVEL SECURITY;
CREATE POLICY vehicle_insert ON vehicle FOR INSERT
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());
CREATE POLICY vehicle_update ON vehicle FOR UPDATE
    USING (app_scope() = 'tenant' AND company_id = app_company_id())
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());
-- vehicle_select é criada em V6 (depende da tabela quote).
