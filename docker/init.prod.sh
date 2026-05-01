#!/bin/sh
# Production DB bootstrap. Runs ONCE on first volume init.
# Reads KLASIO_APP_PASSWORD from container env (set via .env file).
set -e

if [ -z "$KLASIO_APP_PASSWORD" ]; then
  echo "FATAL: KLASIO_APP_PASSWORD not set. Refusing to bootstrap with default password."
  exit 1
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'klasio_app') THEN
            CREATE ROLE klasio_app WITH LOGIN PASSWORD '$KLASIO_APP_PASSWORD';
        END IF;
    END
    \$\$;

    GRANT CONNECT ON DATABASE $POSTGRES_DB TO klasio_app;
    GRANT USAGE, CREATE ON SCHEMA public TO klasio_app;
    ALTER SCHEMA public OWNER TO klasio_app;

    ALTER DEFAULT PRIVILEGES FOR ROLE klasio_app IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO klasio_app;
    ALTER DEFAULT PRIVILEGES FOR ROLE klasio_app IN SCHEMA public
        GRANT USAGE, SELECT ON SEQUENCES TO klasio_app;
EOSQL
