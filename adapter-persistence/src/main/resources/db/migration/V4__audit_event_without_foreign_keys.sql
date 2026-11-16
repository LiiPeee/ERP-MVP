-- A auditoria é histórico, não relacionamento: precisa registrar também tentativas com identificadores que não
-- existem (um token com empresa inexistente, por exemplo) e não pode impedir que uma empresa ou um usuário seja
-- apagado depois (direito de eliminação da LGPD). Por isso company_id e actor_user_id ficam sem chave estrangeira.
ALTER TABLE audit_event DROP CONSTRAINT audit_event_company_id_fkey;
ALTER TABLE audit_event DROP CONSTRAINT audit_event_actor_user_id_fkey;
