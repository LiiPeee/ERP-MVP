-- Fase 1: orçamento, itens e link público.
--
-- Itens só de acréscimo: revisar os itens grava um conjunto novo (revision + 1) e a coluna quote.items_revision passa
-- a apontar para ele. O conjunto antigo continua no banco (histórico, e a aplicação não tem DELETE em lugar nenhum).
-- O total NÃO é guardado: é sempre calculado pelo domínio a partir dos itens.
--
-- Escopos de acesso (ver ScopeVariables):
--   tenant           a oficina autenticada: enxerga e altera o que é dela.
--   quote_link_lookup quem apresenta um link: enxerga SÓ a linha do link com aquele hash (nada de orçamento).
--   quote_decision   quem tem o link agindo sobre UM orçamento: lê o orçamento, os itens, o veículo, o perfil e a
--                    empresa dele (para montar a página) e altera só as colunas da decisão.
--   quote_expiration a tarefa que vence orçamentos enviados: lê os enviados (e os vencidos, porque o PostgreSQL exige
--                    que a linha nova continue visível depois do UPDATE) e só pode mudar o estado para vencido,
--                    e só de quem já passou do prazo.

-- quote --------------------------------------------------------------------------------------------------
CREATE TABLE quote (
    id              uuid PRIMARY KEY,
    company_id      uuid         NOT NULL REFERENCES company (id),
    contact_id      uuid         NOT NULL,
    vehicle_id      uuid         NOT NULL,
    status          varchar(30)  NOT NULL,
    validity_days   smallint     NOT NULL,
    note            varchar(500) NOT NULL DEFAULT '',
    items_revision  integer      NOT NULL,
    created_at      timestamptz  NOT NULL,
    sent_at         timestamptz,
    expires_at      timestamptz,
    decided_at      timestamptz,
    decision_reason varchar(300),
    CONSTRAINT quote_id_company_unique UNIQUE (id, company_id),
    CONSTRAINT quote_contact_same_company FOREIGN KEY (contact_id, company_id) REFERENCES contact (id, company_id),
    CONSTRAINT quote_vehicle_same_company FOREIGN KEY (vehicle_id, company_id) REFERENCES vehicle (id, company_id),
    CONSTRAINT quote_status_known CHECK (status IN ('DRAFT', 'AWAITING_OWNER_CONFIRMATION', 'SENT', 'APPROVED',
                                                    'REJECTED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT quote_validity_range CHECK (validity_days BETWEEN 1 AND 90),
    CONSTRAINT quote_items_revision_positive CHECK (items_revision >= 1),
    CONSTRAINT quote_sent_has_deadline CHECK (status NOT IN ('SENT', 'APPROVED', 'REJECTED', 'EXPIRED')
                                              OR (sent_at IS NOT NULL AND expires_at IS NOT NULL))
);
CREATE INDEX quote_company_status_idx ON quote (company_id, status, created_at DESC);
CREATE INDEX quote_due_idx ON quote (expires_at) WHERE status = 'SENT';

-- quote_item ---------------------------------------------------------------------------------------------
CREATE TABLE quote_item (
    quote_id    uuid          NOT NULL,
    revision    integer       NOT NULL,
    position    smallint      NOT NULL,
    company_id  uuid          NOT NULL,
    description varchar(200)  NOT NULL,
    kind        varchar(10)   NOT NULL,
    quantity    numeric(7, 3) NOT NULL,
    unit_price  numeric(18, 2) NOT NULL,
    PRIMARY KEY (quote_id, revision, position),
    CONSTRAINT quote_item_same_company FOREIGN KEY (quote_id, company_id) REFERENCES quote (id, company_id),
    CONSTRAINT quote_item_kind_known CHECK (kind IN ('PART', 'LABOR')),
    CONSTRAINT quote_item_quantity_range CHECK (quantity > 0 AND quantity <= 9999.999),
    CONSTRAINT quote_item_price_not_negative CHECK (unit_price >= 0)
);

-- quote_public_link --------------------------------------------------------------------------------------
-- Só o hash do token é guardado; o token em si nunca toca o banco.
CREATE TABLE quote_public_link (
    token_hash varchar(128) PRIMARY KEY,
    quote_id   uuid        NOT NULL,
    company_id uuid        NOT NULL,
    issued_at  timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    decided_at timestamptz,
    CONSTRAINT quote_link_same_company FOREIGN KEY (quote_id, company_id) REFERENCES quote (id, company_id)
);
CREATE INDEX quote_public_link_quote_idx ON quote_public_link (quote_id);

GRANT SELECT, INSERT ON quote TO "${app_role}";
GRANT UPDATE (status, items_revision, sent_at, expires_at, decided_at, decision_reason) ON quote TO "${app_role}";
GRANT SELECT, INSERT ON quote_item TO "${app_role}";
GRANT SELECT, INSERT ON quote_public_link TO "${app_role}";
GRANT UPDATE (decided_at) ON quote_public_link TO "${app_role}";

-- Row Level Security -------------------------------------------------------------------------------------
ALTER TABLE quote ENABLE ROW LEVEL SECURITY;
ALTER TABLE quote FORCE ROW LEVEL SECURITY;

CREATE POLICY quote_select ON quote FOR SELECT
    USING ((app_scope() = 'tenant' AND company_id = app_company_id())
        OR (app_scope() = 'quote_decision' AND company_id = app_company_id() AND id = app_quote_id())
        OR (app_scope() = 'quote_expiration' AND status IN ('SENT', 'EXPIRED')));
CREATE POLICY quote_insert ON quote FOR INSERT
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());
CREATE POLICY quote_update ON quote FOR UPDATE
    USING ((app_scope() = 'tenant' AND company_id = app_company_id())
        OR (app_scope() = 'quote_decision' AND company_id = app_company_id() AND id = app_quote_id())
        OR (app_scope() = 'quote_expiration' AND status = 'SENT' AND expires_at <= now()))
    WITH CHECK ((app_scope() = 'tenant' AND company_id = app_company_id())
        OR (app_scope() = 'quote_decision' AND company_id = app_company_id() AND id = app_quote_id())
        OR (app_scope() = 'quote_expiration' AND status = 'EXPIRED'));

ALTER TABLE quote_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE quote_item FORCE ROW LEVEL SECURITY;

CREATE POLICY quote_item_select ON quote_item FOR SELECT
    USING ((app_scope() = 'tenant' AND company_id = app_company_id())
        OR (app_scope() = 'quote_decision' AND company_id = app_company_id() AND quote_id = app_quote_id())
        OR (app_scope() = 'quote_expiration' AND quote_id IN (SELECT id FROM quote)));
CREATE POLICY quote_item_insert ON quote_item FOR INSERT
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());

ALTER TABLE quote_public_link ENABLE ROW LEVEL SECURITY;
ALTER TABLE quote_public_link FORCE ROW LEVEL SECURITY;

CREATE POLICY quote_public_link_select ON quote_public_link FOR SELECT
    USING ((app_scope() = 'tenant' AND company_id = app_company_id())
        OR (app_scope() = 'quote_link_lookup' AND token_hash = app_token_hash())
        OR (app_scope() = 'quote_decision' AND company_id = app_company_id() AND quote_id = app_quote_id()));
CREATE POLICY quote_public_link_insert ON quote_public_link FOR INSERT
    WITH CHECK (app_scope() = 'tenant' AND company_id = app_company_id());
CREATE POLICY quote_public_link_update ON quote_public_link FOR UPDATE
    USING (app_scope() = 'quote_decision' AND company_id = app_company_id() AND quote_id = app_quote_id())
    WITH CHECK (app_scope() = 'quote_decision' AND company_id = app_company_id() AND quote_id = app_quote_id());

-- O veículo: da oficina (tenant) ou, na decisão, só o do orçamento daquele link.
CREATE POLICY vehicle_select ON vehicle FOR SELECT
    USING ((app_scope() = 'tenant' AND company_id = app_company_id())
        OR (app_scope() = 'quote_decision' AND company_id = app_company_id()
            AND id IN (SELECT vehicle_id FROM quote WHERE id = app_quote_id())));

-- A página pública mostra o nome da oficina: o escopo de decisão lê a própria empresa.
ALTER POLICY company_select ON company
    USING (app_scope() IN ('tenant', 'registration', 'quote_decision') AND id = app_company_id());

-- A tarefa de expiração registra um único tipo de evento, sem autor, em nome da empresa do orçamento.
CREATE POLICY audit_event_insert_expiration ON audit_event FOR INSERT
    WITH CHECK (app_scope() = 'quote_expiration' AND event_type = 'QUOTE_EXPIRED' AND actor_user_id IS NULL
                AND company_id IS NOT NULL);
