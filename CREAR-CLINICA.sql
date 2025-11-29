-- ===================================================================
-- Script SQL para crear una clínica completa con administrador
-- Componente Periférico - HCEN
-- ===================================================================
-- 
-- INSTRUCCIONES:
-- 1. Reemplaza los valores entre {{ }} con tus datos
-- 2. Ejecuta este script en la base de datos PostgreSQL
-- 3. Verifica que todas las tablas se crearon correctamente
--
-- ===================================================================

-- ===================================================================
-- VARIABLES A PERSONALIZAR (Reemplaza estos valores)
-- ===================================================================

-- ID del tenant (debe ser único, ej: 103, 104, etc.)
\set tenant_id 103

-- Nombre de la clínica
\set nombre_clinica 'Clínica Ejemplo'

-- RUT de la clínica (formato: sin guiones, ej: '210000103010')
\set rut_clinica '210000103010'

-- Nombre de usuario del administrador
\set admin_nickname 'admin_clinica103'

-- Contraseña del administrador (se hashea con BCrypt)
-- NOTA: El hash por defecto corresponde a "password123"
-- Para generar un nuevo hash de BCrypt, puedes usar: https://bcrypt-generator.com/
\set admin_password_hash '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO'

-- Email del administrador
\set admin_email 'admin.clinica103@example.com'

-- Nombre completo del administrador
\set admin_nombre 'Admin Clínica 103'

-- Color primario del portal (formato HEX, ej: '#007bff')
\set color_primario '#007bff'

-- Nombre del portal
\set nombre_portal 'Portal Clínica Ejemplo'

-- ID único para el usuario administrador (debe ser único en public.usuario)
-- Sugerencia: usar 5000 + tenant_id
\set admin_user_id 5103

-- ===================================================================
-- CREACIÓN DEL SCHEMA DEL TENANT
-- ===================================================================

BEGIN;

-- Crear schema del tenant
CREATE SCHEMA IF NOT EXISTS schema_clinica_:tenant_id;

-- Crear secuencias para IDs auto-incrementables
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='usuario_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.usuario_id_seq', :'tenant_id');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='usuarioperiferico_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.usuarioperiferico_id_seq', :'tenant_id');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='nodoperiferico_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.nodoperiferico_id_seq', :'tenant_id');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='profesionalsalud_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.profesionalsalud_id_seq', :'tenant_id');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='prestadorsalud_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.prestadorsalud_id_seq', :'tenant_id');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='oas_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.oas_id_seq', :'tenant_id');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='administradorclinica_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.administradorclinica_id_seq', :'tenant_id');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='configuracionclinica_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.configuracionclinica_id_seq', :'tenant_id');
  END IF;
  
  IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind='S' AND n.nspname='schema_clinica_' || :'tenant_id' AND c.relname='portal_configuracion_id_seq') THEN
    EXECUTE format('CREATE SEQUENCE schema_clinica_%s.portal_configuracion_id_seq', :'tenant_id');
  END IF;
END$$;

-- ===================================================================
-- CREAR TABLAS DEL TENANT
-- ===================================================================

-- Tabla de configuración del portal
EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.portal_configuracion (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.portal_configuracion_id_seq''),
  color_primario VARCHAR(7) DEFAULT ''%s'',
  color_secundario VARCHAR(7) DEFAULT ''#6c757d'',
  logo_url VARCHAR(512),
  nombre_portal VARCHAR(100)
)', :'tenant_id', :'tenant_id', :'color_primario');

-- Insertar configuración inicial del portal
EXECUTE format('
INSERT INTO schema_clinica_%s.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
VALUES (1, ''%s'', ''#6c757d'', '''', ''%s'')
ON CONFLICT (id) DO NOTHING
', :'tenant_id', :'color_primario', :'nombre_portal');

-- Tabla de usuarios
EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.usuario (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.usuario_id_seq''),
  nombre VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
)', :'tenant_id', :'tenant_id');

-- Tabla de usuarios periféricos
EXECUTE format('
CREATE TABLE IF NOT EXISTS schema_clinica_%s.usuarioperiferico (
  id BIGINT PRIMARY KEY DEFAULT nextval(''schema_clinica_%s.usuarioperiferico_id_seq''),
  nickname VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  dtype VARCHAR(31) NOT NULL DEFAULT ''UsuarioPeriferico'',
  tenant_id VARCHAR(255),
  role VARCHAR(255)
)', :'tenant_id', :'tenant_id');

-- Tabla de nodo periférico
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

-- Otras tablas necesarias
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

-- Crear índices
EXECUTE format('
CREATE INDEX IF NOT EXISTS idx_schema_clinica_%s_profesionalsalud_nodo 
ON schema_clinica_%s.profesionalsalud(nodo_periferico_id)
', :'tenant_id', :'tenant_id');

-- ===================================================================
-- REGISTRAR CLÍNICA EN PUBLIC
-- ===================================================================

-- Registrar nodo periférico en public
INSERT INTO public.nodoperiferico (id, nombre, rut, schema_name)
VALUES (:tenant_id, :'nombre_clinica', :'rut_clinica', 'schema_clinica_' || :'tenant_id')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre,
    rut = EXCLUDED.rut,
    schema_name = EXCLUDED.schema_name;

-- ===================================================================
-- CREAR USUARIO ADMINISTRADOR EN PUBLIC
-- ===================================================================

-- Crear usuario en public.usuario
INSERT INTO public.usuario (id, nombre, email)
VALUES (:admin_user_id, :'admin_nombre', :'admin_email')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre,
    email = EXCLUDED.email;

-- Crear usuario periférico en public.usuarioperiferico
INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role)
VALUES (
  :admin_user_id,
  :'admin_nickname',
  :'admin_password_hash',
  'AdministradorClinica',
  :'tenant_id'::text,
  'ADMINISTRADOR'
)
ON CONFLICT (id) DO UPDATE
SET nickname = EXCLUDED.nickname,
    password_hash = EXCLUDED.password_hash,
    tenant_id = EXCLUDED.tenant_id,
    role = EXCLUDED.role;

-- Crear registro en public.administradorclinica
INSERT INTO public.administradorclinica (id, nodo_periferico_id)
VALUES (:admin_user_id, :tenant_id)
ON CONFLICT (id) DO UPDATE
SET nodo_periferico_id = EXCLUDED.nodo_periferico_id;

COMMIT;

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================

-- Verificar que todo se creó correctamente
SELECT '✅ Clínica creada exitosamente' as estado;
SELECT 'Schema: schema_clinica_' || :'tenant_id' as schema_creado;
SELECT 'Nodo ID: ' || :'tenant_id' as nodo_id;
SELECT 'Admin Username: ' || :'admin_nickname' as admin_username;

-- Verificar registro en public
SELECT * FROM public.nodoperiferico WHERE id = :tenant_id;
SELECT * FROM public.usuarioperiferico WHERE id = :admin_user_id;

