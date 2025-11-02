-- Idempotent fix: create sequences and defaults for id columns in tenant schema_clinica_101
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind = 'S' AND n.nspname = 'schema_clinica_101' AND c.relname = 'usuario_id_seq') THEN
        CREATE SEQUENCE schema_clinica_101.usuario_id_seq;
        ALTER TABLE schema_clinica_101.usuario ALTER COLUMN id SET DEFAULT nextval('schema_clinica_101.usuario_id_seq');
        PERFORM setval('schema_clinica_101.usuario_id_seq', COALESCE((SELECT max(id) FROM schema_clinica_101.usuario),0)+1, false);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind = 'S' AND n.nspname = 'schema_clinica_101' AND c.relname = 'usuarioperiferico_id_seq') THEN
        CREATE SEQUENCE schema_clinica_101.usuarioperiferico_id_seq;
        ALTER TABLE schema_clinica_101.usuarioperiferico ALTER COLUMN id SET DEFAULT nextval('schema_clinica_101.usuarioperiferico_id_seq');
        PERFORM setval('schema_clinica_101.usuarioperiferico_id_seq', COALESCE((SELECT max(id) FROM schema_clinica_101.usuarioperiferico),0)+1, false);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind = 'S' AND n.nspname = 'schema_clinica_101' AND c.relname = 'profesionalsalud_id_seq') THEN
        CREATE SEQUENCE schema_clinica_101.profesionalsalud_id_seq;
        ALTER TABLE schema_clinica_101.profesionalsalud ALTER COLUMN id SET DEFAULT nextval('schema_clinica_101.profesionalsalud_id_seq');
        PERFORM setval('schema_clinica_101.profesionalsalud_id_seq', COALESCE((SELECT max(id) FROM schema_clinica_101.profesionalsalud),0)+1, false);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind = 'S' AND n.nspname = 'schema_clinica_101' AND c.relname = 'nodoperiferico_id_seq') THEN
        CREATE SEQUENCE schema_clinica_101.nodoperiferico_id_seq;
        ALTER TABLE schema_clinica_101.nodoperiferico ALTER COLUMN id SET DEFAULT nextval('schema_clinica_101.nodoperiferico_id_seq');
        PERFORM setval('schema_clinica_101.nodoperiferico_id_seq', COALESCE((SELECT max(id) FROM schema_clinica_101.nodoperiferico),0)+1, false);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='schema_clinica_101' AND table_name='prestadorsalud') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind = 'S' AND n.nspname = 'schema_clinica_101' AND c.relname = 'prestadorsalud_id_seq') THEN
            CREATE SEQUENCE schema_clinica_101.prestadorsalud_id_seq;
            ALTER TABLE schema_clinica_101.prestadorsalud ALTER COLUMN id SET DEFAULT nextval('schema_clinica_101.prestadorsalud_id_seq');
            PERFORM setval('schema_clinica_101.prestadorsalud_id_seq', COALESCE((SELECT max(id) FROM schema_clinica_101.prestadorsalud),0)+1, false);
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='schema_clinica_101' AND table_name='oas') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind = 'S' AND n.nspname = 'schema_clinica_101' AND c.relname = 'oas_id_seq') THEN
            CREATE SEQUENCE schema_clinica_101.oas_id_seq;
            ALTER TABLE schema_clinica_101.oas ALTER COLUMN id SET DEFAULT nextval('schema_clinica_101.oas_id_seq');
            PERFORM setval('schema_clinica_101.oas_id_seq', COALESCE((SELECT max(id) FROM schema_clinica_101.oas),0)+1, false);
        END IF;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='schema_clinica_101' AND table_name='administradorclinica') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON c.relnamespace = n.oid WHERE c.relkind = 'S' AND n.nspname = 'schema_clinica_101' AND c.relname = 'administradorclinica_id_seq') THEN
            CREATE SEQUENCE schema_clinica_101.administradorclinica_id_seq;
            ALTER TABLE schema_clinica_101.administradorclinica ALTER COLUMN id SET DEFAULT nextval('schema_clinica_101.administradorclinica_id_seq');
            PERFORM setval('schema_clinica_101.administradorclinica_id_seq', COALESCE((SELECT max(id) FROM schema_clinica_101.administradorclinica),0)+1, false);
        END IF;
    END IF;
END
$$;

-- Ensure sequences ownerships (best-effort, ignore failures)
DO $$
BEGIN
    BEGIN
        ALTER SEQUENCE schema_clinica_101.usuario_id_seq OWNED BY schema_clinica_101.usuario.id;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
    BEGIN
        ALTER SEQUENCE schema_clinica_101.usuarioperiferico_id_seq OWNED BY schema_clinica_101.usuarioperiferico.id;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
    BEGIN
        ALTER SEQUENCE schema_clinica_101.profesionalsalud_id_seq OWNED BY schema_clinica_101.profesionalsalud.id;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
    BEGIN
        ALTER SEQUENCE schema_clinica_101.nodoperiferico_id_seq OWNED BY schema_clinica_101.nodoperiferico.id;
    EXCEPTION WHEN OTHERS THEN
        NULL;
    END;
END
$$;
