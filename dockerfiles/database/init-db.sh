#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE "bank-dev";
    GRANT ALL PRIVILEGES ON DATABASE "bank-dev" TO "$POSTGRES_USER";

    CREATE DATABASE "bank-test";
    GRANT ALL PRIVILEGES ON DATABASE "bank-test" TO "$POSTGRES_USER";
EOSQL
