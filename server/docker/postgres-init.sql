-- CipherChat PostgreSQL initialization
-- Run automatically by the postgres container on first start
-- (mounted at /docker-entrypoint-initdb.d/init.sql)
--
-- Creates two databases — one per service — so auth-service and
-- messaging-service are schema-isolated. A service can only touch its
-- own database. This prevents a bug in one service from accidentally
-- reading or corrupting another service's data, and makes it easy to
-- scale each service to its own database server in production without
-- a migration step.

-- Auth service database
CREATE DATABASE cipherchat_auth;
GRANT ALL PRIVILEGES ON DATABASE cipherchat_auth TO cipherchat;

-- Messaging service database
CREATE DATABASE cipherchat_messaging;
GRANT ALL PRIVILEGES ON DATABASE cipherchat_messaging TO cipherchat;

-- Tables are created by each service's DatabaseFactory on startup
-- (SchemaUtils.create) — not here. The init script only creates the
-- databases themselves so the containers can connect before the
-- services run their own migrations.
