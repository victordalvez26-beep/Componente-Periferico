-- ===================================================================
-- Script SQL PASO A PASO para crear una clínica con administrador
-- Ejecutar cada bloque en orden dentro del contenedor PostgreSQL
-- ===================================================================
-- 
-- USO:
-- docker exec -it periferico-postgres-db psql -U postgres -d hcen_db
-- 
-- Luego ejecutar cada bloque de queries en orden
--
-- ===================================================================

-- ===================================================================
-- CONFIGURACIÓN (Cambiar estos valores)
-- ===================================================================

-- ID del tenant (debe ser único)
\set tenant_id 103
\set admin_user_id 5103  -- 5000 + tenant_id

-- Datos de la clínica
\set nombre_clinica 'Clínica Ejemplo 103'
\set rut_clinica '210000103010'
\set color_primario '#007bff'
\set nombre_portal 'Portal Clínica 103'

-- Datos del administrador
\set admin_nickname 'admin_clinica103'
\set admin_nombre 'Admin Clínica 103'
\set admin_email 'admin.clinica103@example.com'
\set admin_password_hash '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO'  -- password123

-- ===================================================================
-- PASO 1: Crear el schema del tenant
-- ===================================================================

CREATE SCHEMA IF NOT EXISTS schema_clinica_103;

-- Verificar que se creó
SELECT schema_name FROM information_schema.schemata WHERE schema_name = 'schema_clinica_103';

-- ===================================================================
-- PASO 2: Crear secuencias para IDs auto-incrementables
-- ===================================================================

CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.usuario_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.usuarioperiferico_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.nodoperiferico_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.profesionalsalud_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.prestadorsalud_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.oas_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.administradorclinica_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.configuracionclinica_id_seq;
CREATE SEQUENCE IF NOT EXISTS schema_clinica_103.portal_configuracion_id_seq;

-- Verificar secuencias creadas
SELECT sequence_name FROM information_schema.sequences 
WHERE sequence_schema = 'schema_clinica_103';

-- ===================================================================
-- PASO 3: Crear tablas del tenant
-- ===================================================================

-- Tabla de configuración del portal
CREATE TABLE IF NOT EXISTS schema_clinica_103.portal_configuracion (
    id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.portal_configuracion_id_seq'),
    color_primario VARCHAR(7) DEFAULT '#007bff',
    color_secundario VARCHAR(7) DEFAULT '#6c757d',
    logo_url VARCHAR(512),
    nombre_portal VARCHAR(100)
);

-- Insertar configuración inicial
INSERT INTO schema_clinica_103.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
VALUES (1, '#007bff', '#6c757d', '', 'Portal Clínica 103')
ON CONFLICT (id) DO NOTHING;

-- Tabla de usuarios
CREATE TABLE IF NOT EXISTS schema_clinica_103.usuario (
    id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.usuario_id_seq'),
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

-- Tabla de usuarios periféricos
CREATE TABLE IF NOT EXISTS schema_clinica_103.usuarioperiferico (
    id BIGINT PRIMARY KEY DEFAULT nextval('schema_clinica_103.usuarioperiferico_id_seq'),
    nickname VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    dtype VARCHAR(31) NOT NULL DEFAULT 'UsuarioPeriferico',
    tenant_id VARCHAR(255),
    role VARCHAR(255)
);

-- Tabla de nodo periférico
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

-- Otras tablas necesarias
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

-- Crear índice
CREATE INDEX IF NOT EXISTS idx_schema_clinica_103_profesionalsalud_nodo 
ON schema_clinica_103.profesionalsalud(nodo_periferico_id);

-- Verificar tablas creadas
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'schema_clinica_103' 
ORDER BY table_name;

-- ===================================================================
-- PASO 4: Registrar clínica en public.nodoperiferico
-- ===================================================================

-- Asegurar que la columna schema_name existe
ALTER TABLE public.nodoperiferico ADD COLUMN IF NOT EXISTS schema_name VARCHAR(255);

-- Insertar registro de la clínica
INSERT INTO public.nodoperiferico (id, nombre, rut, schema_name)
VALUES (103, 'Clínica Ejemplo 103', '210000103010', 'schema_clinica_103')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre,
    rut = EXCLUDED.rut,
    schema_name = EXCLUDED.schema_name;

-- Verificar que se insertó
SELECT * FROM public.nodoperiferico WHERE id = 103;

-- ===================================================================
-- PASO 5: Crear usuario administrador en public
-- ===================================================================

-- Asegurar que las tablas públicas existen y tienen las columnas necesarias
CREATE TABLE IF NOT EXISTS public.usuario (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.usuarioperiferico (
    id BIGINT PRIMARY KEY,
    nickname VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    dtype VARCHAR(31) NOT NULL,
    tenant_id VARCHAR(32),
    role VARCHAR(32)
);

ALTER TABLE public.usuarioperiferico ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(32);
ALTER TABLE public.usuarioperiferico ADD COLUMN IF NOT EXISTS role VARCHAR(32);

CREATE TABLE IF NOT EXISTS public.administradorclinica (
    id BIGINT PRIMARY KEY,
    nodo_periferico_id BIGINT
);

-- Insertar usuario en public.usuario
INSERT INTO public.usuario (id, nombre, email)
VALUES (5103, 'Admin Clínica 103', 'admin.clinica103@example.com')
ON CONFLICT (id) DO UPDATE
SET nombre = EXCLUDED.nombre,
    email = EXCLUDED.email;

-- Insertar usuario periférico en public.usuarioperiferico
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

-- Insertar en public.administradorclinica
INSERT INTO public.administradorclinica (id, nodo_periferico_id)
VALUES (5103, 103)
ON CONFLICT (id) DO UPDATE
SET nodo_periferico_id = EXCLUDED.nodo_periferico_id;

-- ===================================================================
-- VERIFICACIÓN FINAL
-- ===================================================================

-- Verificar clínica creada
SELECT 
    '✅ Clínica creada' as estado,
    id,
    nombre,
    rut,
    schema_name
FROM public.nodoperiferico
WHERE id = 103;

-- Verificar administrador creado
SELECT 
    '✅ Administrador creado' as estado,
    u.id,
    u.nombre,
    u.email,
    up.nickname,
    up.tenant_id,
    up.role,
    ac.nodo_periferico_id
FROM public.usuario u
JOIN public.usuarioperiferico up ON u.id = up.id
JOIN public.administradorclinica ac ON u.id = ac.id
WHERE u.id = 5103;

-- Verificar configuración del portal
SELECT 
    '✅ Configuración del portal' as estado,
    id,
    nombre_portal,
    color_primario,
    color_secundario
FROM schema_clinica_103.portal_configuracion
WHERE id = 1;

-- Resumen final
SELECT 
    '🎉 Proceso completado' as resultado,
    'Clínica ID: 103' as clinica,
    'Admin Username: admin_clinica103' as admin,
    'Password: password123' as password,
    'Schema: schema_clinica_103' as schema;

