-- One-off upgrade for schema_clinica_101
SET search_path = schema_clinica_101, public;

-- Ensure schema exists
CREATE SCHEMA IF NOT EXISTS schema_clinica_101;

-- Portal configuration (safe/ idempotent)
CREATE TABLE IF NOT EXISTS schema_clinica_101.portal_configuracion (
  id BIGSERIAL PRIMARY KEY,
  color_primario VARCHAR(7) DEFAULT '#007bff',
  color_secundario VARCHAR(7) DEFAULT '#6c757d',
  logo_url VARCHAR(512),
  nombre_portal VARCHAR(100)
);

INSERT INTO schema_clinica_101.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
  VALUES (1, '#007bff', '#6c757d', '', 'Portal Clinica 101') ON CONFLICT (id) DO NOTHING;

-- Usuario / UsuarioPeriferico hierarchy
CREATE TABLE IF NOT EXISTS schema_clinica_101.usuario (
  id BIGINT PRIMARY KEY,
  nombre VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.usuarioperiferico (
  id BIGINT PRIMARY KEY,
  nickname VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  dtype VARCHAR(31) NOT NULL,
  tenant_id VARCHAR(255),
  role VARCHAR(255)
);

-- NodoPeriferico and subclasses (joined inheritance)
CREATE TABLE IF NOT EXISTS schema_clinica_101.nodoperiferico (
  id BIGINT PRIMARY KEY,
  nombre VARCHAR(255),
  rut VARCHAR(255),
  departamento VARCHAR(255),
  localidad VARCHAR(255),
  direccion VARCHAR(255),
  contacto VARCHAR(255),
  estado VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.oas (
  id BIGINT PRIMARY KEY,
  tipo VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.prestadorsalud (
  id BIGINT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.profesionalsalud (
  id BIGINT PRIMARY KEY,
  especialidad VARCHAR(50),
  nodo_periferico_id BIGINT,
  departamento VARCHAR(50),
  direccion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.configuracionclinica (
  id BIGINT PRIMARY KEY,
  nodo_periferico_id BIGINT UNIQUE,
  colorprincipal VARCHAR(7),
  habilitado BOOLEAN DEFAULT true,
  logourl VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.administradorclinica (
  id BIGINT PRIMARY KEY
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_schema_clinica_101_profesionalsalud_nodo ON schema_clinica_101.profesionalsalud(nodo_periferico_id);

-- Add missing columns to usuarioperiferico if not present
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='schema_clinica_101' AND table_name='usuarioperiferico' AND column_name='role') THEN
    ALTER TABLE schema_clinica_101.usuarioperiferico ADD COLUMN role VARCHAR(255);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='schema_clinica_101' AND table_name='usuarioperiferico' AND column_name='tenant_id') THEN
    ALTER TABLE schema_clinica_101.usuarioperiferico ADD COLUMN tenant_id VARCHAR(255);
  END IF;
END$$;

-- Ensure professionalsalud has nodo_periferico_id column
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='schema_clinica_101' AND table_name='profesionalsalud' AND column_name='nodo_periferico_id') THEN
    ALTER TABLE schema_clinica_101.profesionalsalud ADD COLUMN nodo_periferico_id BIGINT;
  END IF;
END$$;

-- Ensure usuarioperiferico.dtype exists
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='schema_clinica_101' AND table_name='usuarioperiferico' AND column_name='dtype') THEN
    ALTER TABLE schema_clinica_101.usuarioperiferico ADD COLUMN dtype VARCHAR(31) NOT NULL DEFAULT 'UsuarioPeriferico';
  END IF;
END$$;

-- Done
