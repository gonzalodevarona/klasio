-- Klasio database initialization
-- Runs once when the PostgreSQL container is first created.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'klasio_app') THEN
        CREATE ROLE klasio_app WITH LOGIN PASSWORD 'klasio_pass';
    END IF;
END
$$;

-- klasio_app is the sole DDL owner: Flyway connects as klasio_app and creates all tables.
-- Granting CREATEDB is unnecessary; CREATE on schema public is enough.
GRANT CONNECT ON DATABASE klasio TO klasio_app;
GRANT USAGE, CREATE ON SCHEMA public TO klasio_app;

-- Transfer schema ownership so that any object created by postgres (e.g. via psql seeds
-- or manual DDL) is still accessible to Flyway without ownership errors.
ALTER SCHEMA public OWNER TO klasio_app;

-- Default privileges for objects klasio_app creates (redundant but explicit for clarity).
ALTER DEFAULT PRIVILEGES FOR ROLE klasio_app IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO klasio_app;
ALTER DEFAULT PRIVILEGES FOR ROLE klasio_app IN SCHEMA public
    GRANT USAGE, SELECT ON SEQUENCES TO klasio_app;
