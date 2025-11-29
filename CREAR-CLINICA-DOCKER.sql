-- ===================================================================
-- Script SQL para crear una clínica completa con administrador
-- Ejecutar dentro del contenedor PostgreSQL de Docker
-- ===================================================================
-- 
-- USO:
-- docker exec -i periferico-postgres-db psql -U postgres -d hcen_db < CREAR-CLINICA-DOCKER.sql
-- 
-- O desde dentro del contenedor:
-- psql -U postgres -d hcen_db -f /ruta/al/archivo.sql
--
-- ===================================================================

BEGIN;

-- ===================================================================
-- CONFIGURACIÓN - CAMBIAR ESTOS VALORES
-- ===================================================================

-- ID del tenant (debe ser único, ej: 103, 104, 105, etc.)
DO $$
DECLARE
    tenant_id_val BIGINT := 103;
    admin_user_id_val BIGINT := 5103;  -- Debe ser único (sugerencia: 5000 + tenant_id)
    nombre_clinica_val TEXT := 'Clínica Ejemplo 103';
    rut_clinica_val TEXT := '210000103010';
    admin_nickname_val TEXT := 'admin_clinica103';
    admin_nombre_val TEXT := 'Admin Clínica 103';
    admin_email_val TEXT := 'admin.clinica103@example.com';
    admin_password_hash_val TEXT := '$2b$12$i4KLHFvjqcWCJ5kiIapVHuLPiXWftj/ZXIlDStUCRwzkS3bi0mfOO';  -- Hash de "password123"
    color_primario_val TEXT := '#007bff';
    nombre_portal_val TEXT := 'Portal Clínica 103';
    schema_name TEXT;
BEGIN
    schema_name := 'schema_clinica_' || tenant_id_val;

    -- ===================================================================
    -- PASO 1: Crear schema del tenant
    -- ===================================================================
    RAISE NOTICE 'Creando schema: %', schema_name;
    EXECUTE format('CREATE SCHEMA IF NOT EXISTS %I', schema_name);

    -- ===================================================================
    -- PASO 2: Crear secuencias para IDs auto-incrementables
    -- ===================================================================
    RAISE NOTICE 'Creando secuencias...';
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.usuario_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.usuarioperiferico_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.nodoperiferico_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.profesionalsalud_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.prestadorsalud_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.oas_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.administradorclinica_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.configuracionclinica_id_seq', schema_name);
    EXECUTE format('CREATE SEQUENCE IF NOT EXISTS %I.portal_configuracion_id_seq', schema_name);

    -- ===================================================================
    -- PASO 3: Crear tablas del tenant
    -- ===================================================================
    RAISE NOTICE 'Creando tablas del tenant...';

    -- Tabla de configuración del portal
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.portal_configuracion (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.portal_configuracion_id_seq''),
            color_primario VARCHAR(7) DEFAULT %L,
            color_secundario VARCHAR(7) DEFAULT ''#6c757d'',
            logo_url VARCHAR(512),
            nombre_portal VARCHAR(100)
        )', schema_name, schema_name, color_primario_val);

    EXECUTE format('
        INSERT INTO %I.portal_configuracion (id, color_primario, color_secundario, logo_url, nombre_portal)
        VALUES (1, %L, ''#6c757d'', '''', %L)
        ON CONFLICT (id) DO NOTHING
    ', schema_name, color_primario_val, nombre_portal_val);

    -- Tabla de usuarios
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.usuario (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.usuario_id_seq''),
            nombre VARCHAR(255) NOT NULL,
            email VARCHAR(255) NOT NULL
        )', schema_name, schema_name);

    -- Tabla de usuarios periféricos
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.usuarioperiferico (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.usuarioperiferico_id_seq''),
            nickname VARCHAR(255) UNIQUE NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            dtype VARCHAR(31) NOT NULL DEFAULT ''UsuarioPeriferico'',
            tenant_id VARCHAR(255),
            role VARCHAR(255)
        )', schema_name, schema_name);

    -- Tabla de nodo periférico
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.nodoperiferico (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.nodoperiferico_id_seq''),
            nombre VARCHAR(255),
            rut VARCHAR(255),
            contacto VARCHAR(255),
            departamento VARCHAR(255),
            direccion VARCHAR(255),
            estado VARCHAR(50),
            localidad VARCHAR(255)
        )', schema_name, schema_name);

    -- Otras tablas necesarias
    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.oas (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.oas_id_seq''),
            tipo VARCHAR(50)
        )', schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.prestadorsalud (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.prestadorsalud_id_seq'')
        )', schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.profesionalsalud (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.profesionalsalud_id_seq''),
            especialidad VARCHAR(50),
            nodo_periferico_id BIGINT,
            departamento VARCHAR(50),
            direccion VARCHAR(255)
        )', schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.configuracionclinica (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.configuracionclinica_id_seq''),
            nodo_periferico_id BIGINT UNIQUE,
            colorprincipal VARCHAR(7),
            habilitado BOOLEAN DEFAULT true,
            logourl VARCHAR(512)
        )', schema_name, schema_name);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I.administradorclinica (
            id BIGINT PRIMARY KEY DEFAULT nextval(''%I.administradorclinica_id_seq'')
        )', schema_name, schema_name);

    -- Crear índices
    EXECUTE format('
        CREATE INDEX IF NOT EXISTS idx_%I_profesionalsalud_nodo 
        ON %I.profesionalsalud(nodo_periferico_id)
    ', schema_name, schema_name);

    -- ===================================================================
    -- PASO 4: Registrar clínica en public.nodoperiferico
    -- ===================================================================
    RAISE NOTICE 'Registrando clínica en public.nodoperiferico...';
    
    -- Asegurar que la columna schema_name existe
    DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' 
            AND table_name = 'nodoperiferico' 
            AND column_name = 'schema_name'
        ) THEN
            ALTER TABLE public.nodoperiferico ADD COLUMN schema_name VARCHAR(255);
        END IF;
    END$$;

    INSERT INTO public.nodoperiferico (id, nombre, rut, schema_name)
    VALUES (tenant_id_val, nombre_clinica_val, rut_clinica_val, schema_name)
    ON CONFLICT (id) DO UPDATE
    SET nombre = EXCLUDED.nombre,
        rut = EXCLUDED.rut,
        schema_name = EXCLUDED.schema_name;

    -- ===================================================================
    -- PASO 5: Crear usuario administrador en public
    -- ===================================================================
    RAISE NOTICE 'Creando usuario administrador...';

    -- Asegurar que las tablas públicas existen
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

    -- Asegurar que las columnas existen
    DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' 
            AND table_name = 'usuarioperiferico' 
            AND column_name = 'tenant_id'
        ) THEN
            ALTER TABLE public.usuarioperiferico ADD COLUMN tenant_id VARCHAR(32);
        END IF;
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'public' 
            AND table_name = 'usuarioperiferico' 
            AND column_name = 'role'
        ) THEN
            ALTER TABLE public.usuarioperiferico ADD COLUMN role VARCHAR(32);
        END IF;
    END$$;

    CREATE TABLE IF NOT EXISTS public.administradorclinica (
        id BIGINT PRIMARY KEY,
        nodo_periferico_id BIGINT
    );

    -- Crear usuario en public.usuario
    INSERT INTO public.usuario (id, nombre, email)
    VALUES (admin_user_id_val, admin_nombre_val, admin_email_val)
    ON CONFLICT (id) DO UPDATE
    SET nombre = EXCLUDED.nombre,
        email = EXCLUDED.email;

    -- Crear usuario periférico en public.usuarioperiferico
    INSERT INTO public.usuarioperiferico (id, nickname, password_hash, dtype, tenant_id, role)
    VALUES (
        admin_user_id_val,
        admin_nickname_val,
        admin_password_hash_val,
        'AdministradorClinica',
        tenant_id_val::TEXT,
        'ADMINISTRADOR'
    )
    ON CONFLICT (id) DO UPDATE
    SET nickname = EXCLUDED.nickname,
        password_hash = EXCLUDED.password_hash,
        tenant_id = EXCLUDED.tenant_id,
        role = EXCLUDED.role;

    -- Crear registro en public.administradorclinica
    INSERT INTO public.administradorclinica (id, nodo_periferico_id)
    VALUES (admin_user_id_val, tenant_id_val)
    ON CONFLICT (id) DO UPDATE
    SET nodo_periferico_id = EXCLUDED.nodo_periferico_id;

    RAISE NOTICE '✅ Clínica % creada exitosamente', tenant_id_val;
    RAISE NOTICE '   Schema: %', schema_name;
    RAISE NOTICE '   Admin Username: %', admin_nickname_val;
    RAISE NOTICE '   Password por defecto: password123';
END$$;

COMMIT;

-- ===================================================================
-- VERIFICACIÓN
-- ===================================================================

-- Mostrar la clínica creada
SELECT 
    '✅ Clínica creada exitosamente' as estado,
    id,
    nombre,
    rut,
    schema_name
FROM public.nodoperiferico
ORDER BY id DESC
LIMIT 1;

-- Mostrar el administrador creado
SELECT 
    '✅ Administrador creado' as estado,
    u.id,
    u.nombre,
    u.email,
    up.nickname,
    up.tenant_id,
    up.role
FROM public.usuario u
JOIN public.usuarioperiferico up ON u.id = up.id
ORDER BY u.id DESC
LIMIT 1;

