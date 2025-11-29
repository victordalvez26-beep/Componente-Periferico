-- ===================================================================
-- Script SQL SIMPLIFICADO para crear una clínica con administrador
-- Versión más simple sin variables de psql
-- ===================================================================
-- 
-- INSTRUCCIONES:
-- 1. Reemplaza los valores marcados con <<VALOR>>
-- 2. Ejecuta este script en la base de datos PostgreSQL
--
-- ===================================================================

BEGIN;

-- ===================================================================
-- CONFIGURACIÓN - CAMBIA ESTOS VALORES
-- ===================================================================

-- ID del tenant (debe ser único)
\set tenant_id 103

-- Datos de la clínica
\set nombre_clinica 'Clínica Ejemplo'
\set rut_clinica '210000103010'
\set color_primario '#007bff'
\set nombre_portal 'Portal Clínica Ejemplo'

-- Datos del administrador
\set admin_user_id 5103  -- Debe ser único (sugerencia: 5000 + tenant_id)
\set admin_nickname 'admin_clinica103'
\set admin_nombre 'Admin Clínica 103'
\set admin_email 'admin.clinica103@example.com'
\set admin_password_hash '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO'  -- Hash de "password123"

-- ===================================================================
-- CREAR SCHEMA Y TABLAS DEL TENANT
-- ===================================================================

CREATE SCHEMA IF NOT EXISTS schema_clinica_:tenant_id;

-- Secuencias
DO $$
DECLARE
    schema_name TEXT := 'schema_clinica_' || :'tenant_id';
BEGIN
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.usuario_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.usuarioperiferico_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.nodoperiferico_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.profesionalsalud_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.prestadorsalud_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.oas_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.administradorclinica_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.configuracionclinica_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %s.portal_configuracion_id_seq', schema_name);
END$$;

-- Tabla de configuración del portal
EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.portal_configuracion (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.portal_configuracion_id_seq''),
  color_primario VARCHAR(7) DEFAULT ''%s'',
  color_secundario VARCHAR(7) DEFAULT ''#6c757d'',
  logo_url VARCHAR(512),
  nombre_portal VARCHAR(100)
)', :'tenant_id', :'tenant_id', :'color_primario');

EXECUTE format('
INSERT INTO schema_clinica_%s.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
VALUES (1, ''%s'', ''#6c757d'', '''', ''%s'')
ON CONFLICT (id) DO NOTHING
', :'tenant_id', :'color_primario', :'nombre_portal');

-- Tablas restantes del tenant
EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.usuario (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.usuario_id_seq''),
  nombre VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
)', :'tenant_id', :'tenant_id');

EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.usuarioperiferico (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.usuarioperiferico_id_seq''),
  nickname VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  dtype VARCHAR(31) NOT NULL DEFAULT ''UsuarioPeriferico'',
  tenant_id VARCHAR(255),
  role VARCHAR(255)
)', :'tenant_id', :'tenant_id');

EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.nodoperiferico (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.nodoperiferico_id_seq''),
  nombre VARCHAR(255),
  rut VARCHAR(255),
  contacto VARCHAR(255),
  departamento VARCHAR(255),
  direccion VARCHAR(255),
  estado VARCHAR(50),
  localidad VARCHAR(255)
)', :'tenant_id', :'tenant_id');

EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.oas (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.oas_id_seq''),
  tipo VARCHAR(50)
)', :'tenant_id', :'tenant_id');

EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.prestadorsalud (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.prestadorsalud_id_seq'')
)', :'tenant_id', :'tenant_id');

EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.profesionalsalud (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.profesionalsalud_id_seq''),
  especialidad VARCHAR(50),
  nodo_periferico_id BIGINT,
  departamento VARCHAR(50),
  direccion VARCHAR(255)
)', :'tenant_id', :'tenant_id');

EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.configuracionclinica (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.configuracionclinica_id_seq''),
  nodo_periferico_id BIGINT UNIQUE,
  colorprincipal VARCHAR(7),
  habilitado BOOLEAN DEFAULT true,
  logourl VARCHAR(512)
)', :'tenant_id', :'tenant_id');

EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.administradorclinica (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.administradorclinica_id_seq'')
)', :'tenant_id', :'tenant_id');

-- ===================================================================
-- REGISTRAR EN PUBLIC
-- ===================================================================

INSERT INTO public.nodoperiferico (id, nombre, rut, schema_name)
VALUES (:tenant_id, :'nombre_clinica', :'rut_clinica', 'schema_clinica_' || :'tenant_id')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre, rut = EXCLUDED.rut, schema_name = EXCLUDED.schema_name;

INSERT INTO public.usuario (id, nombre, email)
VALUES (:admin_user_id, :'admin_nombre', :'admin_email')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre, email = EXCLUDED.email;

INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role)
VALUES (:admin_user_id, :'admin_nickname', :'admin_password_hash', 'AdministradorClinica', :'tenant_id'::text, 'ADMINISTRADOR')
ON CONFLICT (id) DO UPDATE
SET nickname = EXCLUDED.nickname, password_hash = EXCLUDED.password_hash, tenant_id = EXCLUDED.tenant_id, role = EXCLUDED.role;

INSERT INTO public.administradorclinica (id, nodo_periferico_id)
VALUES (:admin_user_id, :tenant_id)
ON CONFLICT (id) DO UPDATE
SET nodo_periferico_id = EXCLUDED.nodo_periferico_id;

COMMIT;

SELECT '✅ Clínica creada exitosamente' as resultado;

