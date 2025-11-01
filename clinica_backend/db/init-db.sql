-- Asumimos que la BD 'hcen_db' ya existe (creada por las variables de entorno de Docker)

-- HASH BCrypt para la contraseña "password123" (Usado en PasswordUtils)
DO $$
DECLARE
    hash_password CONSTANT VARCHAR := '$2a$10$wK1WwD3T6S2C7Z8F9G0H.Q/1L2M3N4O5P6R7S8T9U0V1W2X3Y4Z'; -- Hash de "password123"
    tenant1_id CONSTANT BIGINT := 101;
    tenant2_id CONSTANT BIGINT := 102;
    id_usuario_t1 BIGINT;
    id_usuario_t2 BIGINT;
    id_admin_t1 BIGINT;
    id_admin_t2 BIGINT;
BEGIN

    -- -------------------------------------------------------------------------
    -- 1. CREACIÓN DE ESQUEMAS (TENANTS)
    -- -------------------------------------------------------------------------
    -- Los IDs '101' y '102' se usarán como TenantId en los JWTs.
    CREATE SCHEMA IF NOT EXISTS schema_clinica_101;
    CREATE SCHEMA IF NOT EXISTS schema_clinica_102;

    -- -------------------------------------------------------------------------
    -- 2. TABLA MAESTRA (En el schema 'public')
    -- -------------------------------------------------------------------------
    CREATE TABLE IF NOT EXISTS public.nodoperiferico (
        id BIGINT PRIMARY KEY, -- Usamos IDs predefinidos para coincidir con el TenantId
        nombre VARCHAR(255) NOT NULL,
        rut VARCHAR(255) UNIQUE NOT NULL
    );
    
    INSERT INTO public.nodoperiferico (id, nombre, rut) VALUES
        (101, 'Clinica Montevideo (T101)', '210000101010') ON CONFLICT (id) DO NOTHING;
    INSERT INTO public.nodoperiferico (id, nombre, rut) VALUES
        (102, 'Prestador Norte (T102)', '210000102020') ON CONFLICT (id) DO NOTHING;
    
    -- -------------------------------------------------------------------------
    -- 3. FUNCIÓN PARA CREAR ESTRUCTURAS DE TABLAS DE TENANT
    -- -------------------------------------------------------------------------

    -- Se replican las estructuras de usuario, admin y profesional por schema.
    CREATE OR REPLACE PROCEDURE create_tenant_structure(schema_name TEXT, master_schema TEXT) AS $$
    BEGIN
        EXECUTE '
            CREATE TABLE ' || quote_ident(schema_name) || '.usuario (
                id BIGSERIAL PRIMARY KEY,
                nombre VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL
            );

            CREATE TABLE ' || quote_ident(schema_name) || '.usuarioperiferico (
                id BIGINT PRIMARY KEY,
                nickname VARCHAR(255) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                DTYPE VARCHAR(31) NOT NULL, -- Tipo de herencia
                FOREIGN KEY (id) REFERENCES ' || quote_ident(schema_name) || '.usuario(id)
            );

            -- El nodo_periferico_id es el enlace lógico al schema público
            CREATE TABLE ' || quote_ident(schema_name) || '.profesionalsalud (
                id BIGINT PRIMARY KEY,
                especialidad VARCHAR(50),
                nodo_periferico_id BIGINT,
                FOREIGN KEY (id) REFERENCES ' || quote_ident(schema_name) || '.usuarioperiferico(id),
                FOREIGN KEY (nodo_periferico_id) REFERENCES ' || quote_ident(master_schema) || '.nodoperiferico(id)
            );
            
            CREATE TABLE ' || quote_ident(schema_name) || '.administradorclinica (
                id BIGINT PRIMARY KEY,
                nodo_periferico_id BIGINT,
                FOREIGN KEY (id) REFERENCES ' || quote_ident(schema_name) || '.usuarioperiferico(id),
                FOREIGN KEY (nodo_periferico_id) REFERENCES ' || quote_ident(master_schema) || '.nodoperiferico(id)
            );
        ';
    END;
    $$ LANGUAGE plpgsql;

    -- Ejecutar la creación de estructuras
    CALL create_tenant_structure('schema_clinica_101', 'public');
    CALL create_tenant_structure('schema_clinica_102', 'public');

    -- -------------------------------------------------------------------------
    -- 4. INSERCIÓN DE DATOS DE PRUEBA (TENANTS)
    -- -------------------------------------------------------------------------

    -- TENANT 101 (schema_clinica_101)
    
    -- Admin (Nickname: admin_c1 / Password: password123)
    INSERT INTO schema_clinica_101.usuario (nombre, email) VALUES ('Admin Claudia T101', 'admin.c1@c101.uy') RETURNING id INTO id_admin_t1;
    INSERT INTO schema_clinica_101.usuarioperiferico (id, nickname, password_hash, DTYPE) VALUES (id_admin_t1, 'admin_c1', hash_password, 'AdministradorClinica');
    INSERT INTO schema_clinica_101.administradorclinica (id, nodo_periferico_id) VALUES (id_admin_t1, tenant1_id);
    
    -- Profesional (Nickname: prof_c1 / Password: password123)
    INSERT INTO schema_clinica_101.usuario (nombre, email) VALUES ('Dr. Juan Perez T101', 'juan.perez@c101.uy') RETURNING id INTO id_usuario_t1;
    INSERT INTO schema_clinica_101.usuarioperiferico (id, nickname, password_hash, DTYPE) VALUES (id_usuario_t1, 'prof_c1', hash_password, 'ProfesionalSalud');
    INSERT INTO schema_clinica_101.profesionalsalud (id, especialidad, nodo_periferico_id) VALUES (id_usuario_t1, 'PEDIATRIA', tenant1_id);
    

    -- TENANT 102 (schema_clinica_102)
    
    -- Profesional (Nickname: prof_c2 / Password: password123)
    INSERT INTO schema_clinica_102.usuario (nombre, email) VALUES ('Dr. Ana Garcia T102', 'ana.garcia@c102.uy') RETURNING id INTO id_usuario_t2;
    INSERT INTO schema_clinica_102.usuarioperiferico (id, nickname, password_hash, DTYPE) VALUES (id_usuario_t2, 'prof_c2', hash_password, 'ProfesionalSalud');
    INSERT INTO schema_clinica_102.profesionalsalud (id, especialidad, nodo_periferico_id) VALUES (id_usuario_t2, 'MEDICINA_GENERAL', tenant2_id);
    
END $$;
