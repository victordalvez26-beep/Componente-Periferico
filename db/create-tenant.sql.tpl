-- Template SQL for creating a new tenant schema and minimal data.
-- Replace {{TENANT_SCHEMA}} with the schema name (e.g. schema_clinica_103)
-- Replace {{TENANT_ID}} with the numeric tenant id (e.g. 103)
-- Replace {{COLOR_PRIMARIO}} and {{NOMBRE_PORTAL}} as needed.

CREATE SCHEMA IF NOT EXISTS {{TENANT_SCHEMA}};

-- Sequences and id defaults for tenant tables (idempotent)
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='usuario_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.usuario_id_seq;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='usuarioperiferico_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.usuarioperiferico_id_seq;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='nodoperiferico_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.nodoperiferico_id_seq;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='profesionalsalud_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.profesionalsalud_id_seq;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='prestadorsalud_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.prestadorsalud_id_seq;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='oas_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.oas_id_seq;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='administradorclinica_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.administradorclinica_id_seq;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='configuracionclinica_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.configuracionclinica_id_seq;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='{{TENANT_SCHEMA}}' AND c.relname='portal_configuracion_id_seq') THEN
    CREATE SEQUENCE {{TENANT_SCHEMA}}.portal_configuracion_id_seq;
  END IF;
END$$;

-- Core tenant tables (with id DEFAULTs set to use tenant sequences)
CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.portal_configuracion (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.portal_configuracion_id_seq'),
  color_primario VARCHAR(7) DEFAULT '{{COLOR_PRIMARIO}}',
  color_secundario VARCHAR(7) DEFAULT '#6c757d',
  logo_url VARCHAR(512),
  nombre_portal VARCHAR(100)
);

INSERT INTO {{TENANT_SCHEMA}}.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
  VALUES (1, '{{COLOR_PRIMARIO}}', '#6c757d', '', '{{NOMBRE_PORTAL}}') ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.usuario (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.usuario_id_seq'),
  nombre VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.usuarioperiferico (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.usuarioperiferico_id_seq'),
  nickname VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  dtype VARCHAR(31) NOT NULL DEFAULT 'UsuarioPeriferico',
  tenant_id VARCHAR(255),
  role VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.nodoperiferico (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.nodoperiferico_id_seq'),
  nombre VARCHAR(255),
  rut VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.oas (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.oas_id_seq'),
  tipo VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.prestadorsalud (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.prestadorsalud_id_seq')
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.profesionalsalud (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.profesionalsalud_id_seq'),
  especialidad VARCHAR(50),
  nodo_periferico_id BIGINT,
  departamento VARCHAR(50),
  direccion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.configuracionclinica (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.configuracionclinica_id_seq'),
  nodo_periferico_id BIGINT UNIQUE,
  colorprincipal VARCHAR(7),
  habilitado BOOLEAN DEFAULT true,
  logourl VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS {{TENANT_SCHEMA}}.administradorclinica (
  id BIGINT PRIMARY KEY DEFAULT nextval('{{TENANT_SCHEMA}}.administradorclinica_id_seq')
);

-- Ensure FK placeholders (application uses JPA to manage FKs; create indexes to help)
CREATE INDEX IF NOT EXISTS idx_{{TENANT_SCHEMA}}_profesionalsalud_nodo ON {{TENANT_SCHEMA}}.profesionalsalud(nodo_periferico_id);

