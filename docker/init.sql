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

GRANT CONNECT ON DATABASE klasio TO klasio_app;
GRANT USAGE, CREATE ON SCHEMA public TO klasio_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO klasio_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO klasio_app;
