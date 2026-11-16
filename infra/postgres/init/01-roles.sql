-- Executado uma única vez pelo PostgreSQL ao criar o banco (docker-entrypoint-initdb.d),
-- conectado como superusuário. Os nomes e as senhas vêm do ambiente, nunca deste arquivo.
-- Roda no Docker Compose e nos testes (mesmo script, mesmos papéis).

\getenv migrator_user ERP_DB_MIGRATOR_USER
\getenv migrator_password ERP_DB_MIGRATOR_PASSWORD
\getenv app_user ERP_DB_APP_USER
\getenv app_password ERP_DB_APP_PASSWORD

-- Papel das migrações Flyway: dono do banco e do schema, não é superusuário.
CREATE ROLE :"migrator_user" LOGIN PASSWORD :'migrator_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;

-- Papel da aplicação: sem posse, sem superusuário, sempre sujeito a Row Level Security.
-- Nenhum privilégio em tabelas é concedido aqui; cada migração concede o que a tabela precisa.
CREATE ROLE :"app_user" LOGIN PASSWORD :'app_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS NOINHERIT;

ALTER DATABASE :"DBNAME" OWNER TO :"migrator_user";

REVOKE ALL ON DATABASE :"DBNAME" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"DBNAME" TO :"app_user";

ALTER SCHEMA public OWNER TO :"migrator_user";
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO :"app_user";
