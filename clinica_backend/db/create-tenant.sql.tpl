-- Template SQL for creating a new tenant schema and minimal data.
-- Replace {{TENANT_SCHEMA}} with the schema name (e.g. schema_clinica_103)
-- Replace {{TENANT_ID}} with the numeric tenant id (e.g. 103)
-- Replace {{COLOR_PRIMARIO}} and {{NOMBRE_PORTAL}} as needed.

CREATE SCHEMA IF NOT EXISTS {{TENANT_SCHEMA}};

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.portal_configuracion (
  id BIGSERIAL PRIMARY KEY,
  color_primario VARCHAR(7) DEFAULT '{{COLOR_PRIMARIO}}',
  color_secundario VARCHAR(7) DEFAULT '#6c757d',
  logo_url VARCHAR(512),
  nombre_portal VARCHAR(100)
);

INSERT INTO {{TENANT_SCHEMA}}.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
  VALUES (1, '{{COLOR_PRIMARIO}}', '#6c757d', '', '{{NOMBRE_PORTAL}}') ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.usuario (
  id BIGINT PRIMARY KEY,
  nombre VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.usuarioperiferico (
  id BIGINT PRIMARY KEY,
  nickname VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  dtype VARCHAR(31) NOT NULL
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.nodoperiferico (
  id BIGINT PRIMARY KEY,
  nombre VARCHAR(255),
  rut VARCHAR(255)
);

-- Optionally more tenant-local tables may be created by the application later.
