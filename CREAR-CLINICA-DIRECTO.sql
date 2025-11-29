-- ===================================================================
-- Script SQL DIRECTO para crear una clínica (ID 103) con administrador
-- Sin variables, listo para ejecutar directamente
-- ===================================================================
-- 
-- Para crear otra clínica, reemplaza todos los "103" por el ID deseado
-- y ajusta los demás valores
--
-- ===================================================================

BEGIN;

-- Crear schema del tenant
CREATE SCHEMA IF NOT EXISTS schema_clinica_103;

-- Crear secuencias
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.usuario_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.usuarioperiferico_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.nodoperiferico_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.profesionalsalud_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.prestadorsalud_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.oas_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.administradorclinica_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.configuracionclinica_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.portal_configuracion_id_seq;

-- Tabla de configuración del portal
CREATE TABLE IF NOT EXISTS schema_clinica_103.portal_configuracion (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.portal_configuracion_id_seq'),
  color_primario VARCHAR(7) DEFAULT '#007bff',
  color_secundario VARCHAR(7) DEFAULT '#6c757d',
  logo_url VARCHAR(512),
  nombre_portal VARCHAR(100)
);

INSERT INTO schema_clinica_103.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
VALUES (1, '#007bff', '#6c757d', '', 'Portal Clínica 103')
ON CONFLICT (id) DO NOTHING;

-- Tablas del tenant
CREATE TABLE IF NOT EXISTS schema_clinica_103.usuario (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.usuario_id_seq'),
  nombre VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_clinica_103.usuarioperiferico (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.usuarioperiferico_id_seq'),
  nickname VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  dtype VARCHAR(31) NOT NULL DEFAULT 'UsuarioPeriferico',
  tenant_id VARCHAR(255),
  role VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS schema_clinica_103.nodoperiferico (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.nodoperiferico_id_seq'),
  nombre VARCHAR(255),
  rut VARCHAR(255),
  contacto VARCHAR(255),
  departamento VARCHAR(255),
  direccion VARCHAR(255),
  estado VARCHAR(50),
  localidad VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS schema_clinica_103.oas (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.oas_id_seq'),
  tipo VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS schema_clinica_103.prestadorsalud (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.prestadorsalud_id_seq')
);

CREATE TABLE IF NOT EXISTS schema_clinica_103.profesionalsalud (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.profesionalsalud_id_seq'),
  especialidad VARCHAR(50),
  nodo_periferico_id BIGINT,
  departamento VARCHAR(50),
  direccion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS schema_clinica_103.configuracionclinica (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.configuracionclinica_id_seq'),
  nodo_periferico_id BIGINT UNIQUE,
  colorprincipal VARCHAR(7),
  habilitado BOOLEAN DEFAULT true,
  logourl VARCHAR(512)
);

CREATE TABLE IF NOT EXISTS schema_clinica_103.administradorclinica (
  id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.administradorclinica_id_seq')
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_schema_clinica_103_profesionalsalud_nodo 
ON schema_clinica_103.profesionalsalud(nodo_periferico_id);

-- ===================================================================
-- REGISTRAR CLÍNICA EN PUBLIC.NODOPERIFERICO
-- ===================================================================

INSERT INTO public.nodoperiferico (id, nombre, rut, schema_name)
VALUES (103, 'Clínica Ejemplo 103', '210000103010', 'schema_clinica_103')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre,
    rut = EXCLUDED.rut,
    schema_name = EXCLUDED.schema_name;

-- ===================================================================
-- CREAR USUARIO ADMINISTRADOR EN PUBLIC
-- ===================================================================

-- Usuario en public.usuario
INSERT INTO public.usuario (id, nombre, email)
VALUES (5103, 'Admin Clínica 103', 'admin.clinica103@example.com')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre,
    email = EXCLUDED.email;

-- Usuario periférico en public.usuarioperiferico
-- Hash de contraseña: $2b$12$... corresponde a "password123"
INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role)
VALUES (
  5103,
  'admin_clinica103',
  '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO',
  'AdministradorClinica',
  '103',
  'ADMINISTRADOR'
)
ON CONFLICT (id) DO UPDATE
SET nickname = EXCLUDED.nickname,
    password_hash = EXCLUDED.password_hash,
    tenant_id = EXCLUDED.tenant_id,
    role = EXCLUDED.role;

-- Administrador en public.administradorclinica
INSERT INTO public.administradorclinica (id, nodo_periferico_id)
VALUES (5103, 103)
ON CONFLICT (id) DO UPDATE
SET nodo_periferico_id = EXCLUDED.nodo_periferico_id;

COMMIT;

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================

SELECT '✅ Clínica 103 creada exitosamente' as resultado;
SELECT 'Schema: schema_clinica_103' as schema_creado;
SELECT 'Admin Username: admin_clinica103' as admin_username;
SELECT 'Password por defecto: password123' as password_info;

-- Verificar registros
SELECT * FROM public.nodoperiferico WHERE id = 103;
SELECT id, nickname, tenant_id, role FROM public.usuarioperiferico WHERE id = 5103;

