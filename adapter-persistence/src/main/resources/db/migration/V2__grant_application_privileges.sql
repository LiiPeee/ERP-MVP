-- Privilégios mínimos do papel da aplicação (${app_role} vem do ambiente; o papel é criado no início do banco).
-- Nenhum DELETE em lugar nenhum. UPDATE só nas colunas que o negócio realmente altera.
-- A auditoria é só de inserção: a aplicação não lê, não altera e não apaga.

GRANT SELECT, INSERT ON company TO "${app_role}";

GRANT SELECT, INSERT ON "user" TO "${app_role}";
GRANT UPDATE (failed_login_count, locked_until) ON "user" TO "${app_role}";

GRANT SELECT, INSERT ON company_membership TO "${app_role}";

GRANT SELECT, INSERT ON refresh_token TO "${app_role}";
GRANT UPDATE (revoked_at) ON refresh_token TO "${app_role}";

GRANT INSERT ON audit_event TO "${app_role}";
