-- Asumimos que la BD 'hcen_db' ya existe (creada por las variables de entorno de Docker)

-- Con esto se puede ejecutar manualmente contra una BD ya creada.

-- Hash BCrypt (ejemplo) para la contraseña "password123"; sustituir por la que usa la app si es distinto.

-- Tenants predefinidos
CREATE SCHEMA IF NOT EXISTS schema_clinica_101;
CREATE SCHEMA IF NOT EXISTS schema_clinica_102;

-- Tabla maestra en public
CREATE TABLE IF NOT EXISTS public.nodoperiferico (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    rut VARCHAR(255) UNIQUE NOT NULL
);

INSERT INTO public.nodoperiferico (id, nombre, rut) VALUES
    (101, 'Clinica Montevideo (T101)', '210000101010') ON CONFLICT (id) DO NOTHING;
INSERT INTO public.nodoperiferico (id, nombre, rut) VALUES
    (102, 'Prestador Norte (T102)', '210000102020') ON CONFLICT (id) DO NOTHING;

-- Tablas globales en public para usuarios que actúan fuera del contexto multi-tenant
CREATE TABLE IF NOT EXISTS public.usuario (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.usuarioperiferico (
    id BIGINT PRIMARY KEY,
    nickname VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    dtype VARCHAR(31) NOT NULL,
    tenant_id VARCHAR(32) NULL
);

-- If the table already existed (older deploys), ensure the tenant_id column exists
ALTER TABLE public.usuarioperiferico ADD COLUMN IF NOT EXISTS tenant_id VARCHAR(32);
-- Ensure there's a role column for explicit role mapping (PROFESIONAL, ADMINISTRADOR, etc.)
ALTER TABLE public.usuarioperiferico ADD COLUMN IF NOT EXISTS role VARCHAR(32);

-- Insertar cuentas de login globales (usar mismo hash determinístico para pruebas)
INSERT INTO public.usuario (id, nombre, email) VALUES (5001, 'Admin Global C1', 'admin.c1@global') ON CONFLICT (id) DO NOTHING;
-- admin_c1 belongs to tenant 101
INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role) VALUES (5001, 'admin_c1', '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO', 'AdministradorClinica', '101', 'ADMINISTRADOR') ON CONFLICT (id) DO NOTHING;

INSERT INTO public.usuario (id, nombre, email) VALUES (5002, 'Admin Global C2', 'admin.c2@global') ON CONFLICT (id) DO NOTHING;
-- admin_c2 belongs to tenant 102
INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role) VALUES (5002, 'admin_c2', '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO', 'AdministradorClinica', '102', 'ADMINISTRADOR') ON CONFLICT (id) DO NOTHING;

-- Create joined-subclass tables in public so polymorphic loads from public return subclass instances
CREATE TABLE IF NOT EXISTS public.administradorclinica (
    id BIGINT PRIMARY KEY,
    nodo_periferico_id BIGINT
);

CREATE TABLE IF NOT EXISTS public.profesionalsalud (
    id BIGINT PRIMARY KEY,
    especialidad VARCHAR(50),
    nodo_periferico_id BIGINT
);

-- Ensure subclass rows for the global users exist (idempotent)
INSERT INTO public.administradorclinica (id, nodo_periferico_id) VALUES (5001, 101) ON CONFLICT (id) DO NOTHING;
INSERT INTO public.administradorclinica (id, nodo_periferico_id) VALUES (5002, 102) ON CONFLICT (id) DO NOTHING;

-- Creación de tablas por schema (simplificado, sin funciones dinámicas)
-- Estructura para schema_clinica_101
CREATE TABLE IF NOT EXISTS schema_clinica_101.usuario (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.usuarioperiferico (
    id BIGINT PRIMARY KEY,
    nickname VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    dtype VARCHAR(31) NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.profesionalsalud (
    id BIGINT PRIMARY KEY,
    especialidad VARCHAR(50),
    nodo_periferico_id BIGINT
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.administradorclinica (
    id BIGINT PRIMARY KEY,
    nodo_periferico_id BIGINT
);

-- Placeholder tables used by some joins/queries. These are minimal shapes
-- so the application can run without failing when it performs left joins
-- against these entities. Real schemas should be modelled as needed later.
CREATE TABLE IF NOT EXISTS schema_clinica_101.nodoperiferico (
    id BIGINT PRIMARY KEY,
    contacto VARCHAR(255),
    departamento VARCHAR(255),
    direccion VARCHAR(255),
    estado VARCHAR(50),
    localidad VARCHAR(255),
    nombre VARCHAR(255),
    rut VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.oas (
    id BIGINT PRIMARY KEY,
    tipo VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.prestadorsalud (
    id BIGINT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS schema_clinica_101.configuracionclinica (
    nodo_periferico_id BIGINT PRIMARY KEY,
    colorprincipal VARCHAR(50),
    habilitado BOOLEAN,
    logourl VARCHAR(255)
);

-- Portal configuration entity expected by the application (portal_configuracion)
CREATE TABLE IF NOT EXISTS schema_clinica_101.portal_configuracion (
    id BIGSERIAL PRIMARY KEY,
    color_primario VARCHAR(7) DEFAULT '#007bff',
    color_secundario VARCHAR(7) DEFAULT '#6c757d',
    logo_url VARCHAR(512),
    nombre_portal VARCHAR(100)
);
-- Ensure there's at least one row so the SELECT returns something
INSERT INTO schema_clinica_101.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
    VALUES (1, '#007bff', '#6c757d', '', 'Clinica Montevideo') ON CONFLICT (id) DO NOTHING;

-- Estructura para schema_clinica_102
CREATE TABLE IF NOT EXISTS schema_clinica_102.usuario (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_clinica_102.usuarioperiferico (
    id BIGINT PRIMARY KEY,
    nickname VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    dtype VARCHAR(31) NOT NULL
);

CREATE TABLE IF NOT EXISTS schema_clinica_102.profesionalsalud (
    id BIGINT PRIMARY KEY,
    especialidad VARCHAR(50),
    nodo_periferico_id BIGINT
);

CREATE TABLE IF NOT EXISTS schema_clinica_102.administradorclinica (
    id BIGINT PRIMARY KEY,
    nodo_periferico_id BIGINT
);

-- Placeholder tables in schema_clinica_102 (same minimal shape as 101)
CREATE TABLE IF NOT EXISTS schema_clinica_102.nodoperiferico (
    id BIGINT PRIMARY KEY,
    contacto VARCHAR(255),
    departamento VARCHAR(255),
    direccion VARCHAR(255),
    estado VARCHAR(50),
    localidad VARCHAR(255),
    nombre VARCHAR(255),
    rut VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS schema_clinica_102.oas (
    id BIGINT PRIMARY KEY,
    tipo VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS schema_clinica_102.prestadorsalud (
    id BIGINT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS schema_clinica_102.configuracionclinica (
    nodo_periferico_id BIGINT PRIMARY KEY,
    colorprincipal VARCHAR(50),
    habilitado BOOLEAN,
    logourl VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS schema_clinica_102.portal_configuracion (
    id BIGSERIAL PRIMARY KEY,
    color_primario VARCHAR(7) DEFAULT '#28a745',
    color_secundario VARCHAR(7) DEFAULT '#6c757d',
    logo_url VARCHAR(512),
    nombre_portal VARCHAR(100)
);
INSERT INTO schema_clinica_102.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
    VALUES (1, '#28a745', '#6c757d', '', 'Prestador Norte') ON CONFLICT (id) DO NOTHING;

-- Datos de prueba (IDs fijos para simplicidad)
-- Usamos un hash de ejemplo: reemplazar por el hash real si procede
-- Ejemplo hash (bcrypt) — asegúrate que coincida con el formato que PasswordUtils espera
INSERT INTO schema_clinica_101.usuario (id, nombre, email) VALUES (1001, 'Admin Claudia T101', 'admin.c1@c101.uy') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_101.usuarioperiferico (id, nickname, password_hash, dtype) VALUES (1001, 'admin_c1', '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO', 'AdministradorClinica') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_101.administradorclinica (id, nodo_periferico_id) VALUES (1001, 101) ON CONFLICT (id) DO NOTHING;

-- sample nodo_periferico and configuracionclinica in tenant schema
INSERT INTO schema_clinica_101.nodoperiferico (id, nombre, rut, contacto, departamento, direccion, estado, localidad) VALUES (101, 'Clinica Montevideo (T101)', '210000101010', 'contacto@c101.uy', 'Centro', 'Calle Falsa 123', 'ACTIVO', 'Montevideo') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_101.configuracionclinica (nodo_periferico_id, colorprincipal, habilitado, logourl) VALUES (101, '#007bff', true, '') ON CONFLICT (nodo_periferico_id) DO NOTHING;

INSERT INTO schema_clinica_101.usuario (id, nombre, email) VALUES (1002, 'Dr. Juan Perez T101', 'juan.perez@c101.uy') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_101.usuarioperiferico (id, nickname, password_hash, dtype) VALUES (1002, 'prof_c1', '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO', 'ProfesionalSalud') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_101.profesionalsalud (id, especialidad, nodo_periferico_id) VALUES (1002, 'PEDIATRIA', 101) ON CONFLICT (id) DO NOTHING;

-- Tenant 102
INSERT INTO schema_clinica_102.usuario (id, nombre, email) VALUES (2001, 'Dr. Ana Garcia T102', 'ana.garcia@c102.uy') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_102.usuarioperiferico (id, nickname, password_hash, dtype) VALUES (2001, 'prof_c2', '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO', 'ProfesionalSalud') ON CONFLICT (id) DO NOTHING;
-- Also add an administrator account in tenant 102 for testing convenience
INSERT INTO schema_clinica_102.usuario (id, nombre, email) VALUES (2002, 'Admin T102', 'admin.c2@c102.uy') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_102.usuarioperiferico (id, nickname, password_hash, dtype) VALUES (2002, 'admin_c2', '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO', 'AdministradorClinica') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_102.administradorclinica (id, nodo_periferico_id) VALUES (2002, 102) ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_102.profesionalsalud (id, especialidad, nodo_periferico_id) VALUES (2001, 'MEDICINA_GENERAL', 102) ON CONFLICT (id) DO NOTHING;

-- sample nodo_periferico and configuracionclinica for tenant 102
INSERT INTO schema_clinica_102.nodoperiferico (id, nombre, rut, contacto, departamento, direccion, estado, localidad) VALUES (102, 'Prestador Norte (T102)', '210000102020', 'contacto@c102.uy', 'Norte', 'Av. Siempre Viva 742', 'ACTIVO', 'Salto') ON CONFLICT (id) DO NOTHING;
INSERT INTO schema_clinica_102.configuracionclinica (nodo_periferico_id, colorprincipal, habilitado, logourl) VALUES (102, '#28a745', true, '') ON CONFLICT (nodo_periferico_id) DO NOTHING;

-- Fin del script
