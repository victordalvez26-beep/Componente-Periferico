-- ============================================================================
-- Script de carga de datos de prueba para Componente Periférico (Clínica)
-- ============================================================================
-- Este script carga datos consistentes que se sincronizan con PDI INUS
-- y HCEN Backend para pruebas E2E del sistema.
--
-- IMPORTANTE: Los datos aquí deben coincidir con los de las otras aplicaciones
-- para asegurar la consistencia en las pruebas.
--
-- NOTA: Este script debe ejecutarse en el schema específico de cada clínica
-- Si usas multi-tenancy, ejecuta este script para cada tenant (schema).
-- ============================================================================

-- Limpiar datos existentes (solo para pruebas)
DELETE FROM profesionalsalud WHERE dtype = 'ProfesionalSalud';
DELETE FROM usuarioperiferico;
DELETE FROM usuario_salud;
DELETE FROM configuracionclinica;
DELETE FROM prestadorsalud;
DELETE FROM nodoperiferico WHERE dtype = 'PrestadorSalud';

-- ============================================================================
-- NODO PERIFÉRICO (Información de la Clínica)
-- ============================================================================
-- Este nodo representa la clínica actual
-- El RUT debe coincidir con el de HCEN Backend (tabla nodoperiferico)

INSERT INTO nodoperiferico (
    dtype, nombre, rut, departamento, localidad, direccion, contacto, estado
) VALUES
-- Hospital Central - debe coincidir con HCEN Backend
(
    'PrestadorSalud',
    'Hospital Central',
    '210123450015',
    'MONTEVIDEO',
    'Montevideo',
    'Av. Italia 2563',
    '2487 1234',
    'ACTIVO'
);

-- Obtener el ID del nodo creado (asumiendo ID = 1)
-- Este ID se usará como tenant_id en las demás tablas

-- ============================================================================
-- CONFIGURACIÓN DE LA CLÍNICA
-- ============================================================================
-- Configuración específica del portal web de la clínica

INSERT INTO configuracionclinica (
    nodo_periferico_id, nombre_portal, url_portal, color_primario, 
    color_secundario, logo_url, permitir_autoregistro, notificaciones_email
) VALUES
(
    1,  -- ID del nodo periférico
    'Portal Hospital Central',
    'https://portal.hospitalcentral.com.uy',
    '#1976D2',
    '#FFC107',
    'https://hospitalcentral.com.uy/logo.png',
    true,
    true
);

-- ============================================================================
-- PRESTADORES DE SALUD (Para asociación con usuarios)
-- ============================================================================
-- Estos prestadores deben coincidir con los de HCEN Backend

INSERT INTO prestadorsalud (
    nombre, rut, departamento, localidad, direccion, contacto, estado
) VALUES
-- Prestador 1: Hospital Central
(
    'Hospital Central',
    '210123450015',
    'MONTEVIDEO',
    'Montevideo',
    'Av. Italia 2563',
    'servicios@hospitalcentral.com.uy',
    'ACTIVO'
),

-- Prestador 2: Clínica del Este
(
    'Clínica del Este',
    '214587960018',
    'MALDONADO',
    'Punta del Este',
    'Av. Roosevelt y Parada 3',
    'servicios@clinicadeleste.com.uy',
    'ACTIVO'
),

-- Prestador 3: Laboratorio Montevideo
(
    'Laboratorio Montevideo',
    '212345670013',
    'MONTEVIDEO',
    'Montevideo',
    'Av. 18 de Julio 1550',
    'info@labmontevideo.com.uy',
    'ACTIVO'
);

-- ============================================================================
-- USUARIOS DE SALUD (Pacientes)
-- ============================================================================
-- Estos pacientes están registrados en esta clínica
-- Las CIs deben coincidir con las de PDI INUS y HCEN Backend
-- El tenant_id debe ser el ID del nodo periférico (1)

INSERT INTO usuario_salud (
    ci, nombre, apellido, fecha_nacimiento, direccion, telefono, email,
    departamento, localidad, hcen_user_id, tenant_id, 
    fecha_alta, fecha_actualizacion
) VALUES
-- Paciente 1: Victor Alvez
(
    '50830691',
    'Victor David',
    'Alvez González',
    '2000-12-26',
    'Av. 18 de Julio 1234',
    '099123456',
    'victor.alvez@example.com',
    'Montevideo',
    'Montevideo',
    1,  -- ID en HCEN Backend (tabla users)
    1,  -- tenant_id = ID del nodo periférico
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- Paciente 2: María López
(
    '25850303',
    'María Laura',
    'López García',
    '1990-07-22',
    'Av. Giannattasio Km 23',
    '099234567',
    'maria.lopez@example.com',
    'Canelones',
    'Ciudad de la Costa',
    2,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- Paciente 3: Carlos Rodríguez
(
    '58076354',
    'Carlos Alberto',
    'Rodríguez Martínez',
    '1978-11-08',
    'Bulevar Artigas 1567',
    '099345678',
    'carlos.rodriguez@example.com',
    'Montevideo',
    'Montevideo',
    3,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- Paciente 4: Roberto Silva (Extranjero)
(
    '26347848',
    'Roberto',
    'Silva Santos',
    '1982-04-18',
    NULL,
    '+55 11 98765-4321',
    'roberto.silva@example.com',
    NULL,
    NULL,
    5,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),

-- Paciente 5: Pedro González (Menor de edad)
(
    '39178531',
    'Pedro José',
    'González Díaz',
    '2010-09-12',
    'Av. Italia 2345',
    '099567890',
    'pedro.gonzalez@example.com',
    'Montevideo',
    'Montevideo',
    6,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- USUARIOS PERIFÉRICOS (Base para Profesionales y Administradores)
-- ============================================================================
-- La tabla usuarioperiferico es la clase base para profesionales de salud

-- Primero, insertar en la tabla base usuario
INSERT INTO usuario (id, nombre, email) VALUES
(100, 'Dr. Juan Pérez', 'juan.perez@hospitalcentral.com.uy'),
(101, 'Dra. Laura Martínez', 'laura.martinez@hospitalcentral.com.uy'),
(102, 'Dr. Roberto Sánchez', 'roberto.sanchez@hospitalcentral.com.uy'),
(103, 'Admin Central', 'admin@hospitalcentral.com.uy');

-- Ahora insertar en usuarioperiferico
INSERT INTO usuarioperiferico (
    id, nombre, email, nickname, password_hash, tenant_id, role, dtype
) VALUES
(100, 'Dr. Juan Pérez', 'juan.perez@hospitalcentral.com.uy', 'dr_perez', 
 '$2a$10$N9qo8uLOickgx2ZMRZoMye/3MZQqXFZUl3.G3OP6aDx', '1', 'PROFESIONAL', 'ProfesionalSalud'),

(101, 'Dra. Laura Martínez', 'laura.martinez@hospitalcentral.com.uy', 'dra_martinez',
 '$2a$10$N9qo8uLOickgx2ZMRZoMye/3MZQqXFZUl3.G3OP6aDx', '1', 'PROFESIONAL', 'ProfesionalSalud'),

(102, 'Dr. Roberto Sánchez', 'roberto.sanchez@hospitalcentral.com.uy', 'dr_sanchez',
 '$2a$10$N9qo8uLOickgx2ZMRZoMye/3MZQqXFZUl3.G3OP6aDx', '1', 'PROFESIONAL', 'ProfesionalSalud'),

(103, 'Admin Central', 'admin@hospitalcentral.com.uy', 'admin_central',
 '$2a$10$N9qo8uLOickgx2ZMRZoMye/3MZQqXFZUl3.G3OP6aDx', '1', 'ADMIN', 'AdministradorClinica');

-- ============================================================================
-- PROFESIONALES DE SALUD
-- ============================================================================
-- Profesionales que trabajan en esta clínica
-- Nota: profesionalsalud hereda de usuarioperiferico (SINGLE_TABLE inheritance)

INSERT INTO profesionalsalud (
    id, especialidad, departamento, direccion, telefono, ci, nodo_periferico_id, dtype
) VALUES
-- Profesional 1: Dr. Juan Pérez - Cardiólogo
(
    100,
    'CARDIOLOGIA',
    'MONTEVIDEO',
    'Av. Italia 2563',
    '2487 1235',
    '32145678',
    1,  -- ID del nodo periférico
    'ProfesionalSalud'
),

-- Profesional 2: Dra. Laura Martínez - Pediatra
(
    101,
    'PEDIATRIA',
    'MONTEVIDEO',
    'Av. Italia 2563',
    '2487 1236',
    '34567890',
    1,
    'ProfesionalSalud'
),

-- Profesional 3: Dr. Roberto Sánchez - Medicina General
(
    102,
    'MEDICINA_GENERAL',
    'MONTEVIDEO',
    'Av. Italia 2563',
    '2487 1237',
    '35678901',
    1,
    'ProfesionalSalud'
);

-- ============================================================================
-- VERIFICACIÓN DE DATOS CARGADOS
-- ============================================================================

-- Contar registros por tabla
SELECT 'nodoperiferico' AS tabla, COUNT(*) AS total FROM nodoperiferico
UNION ALL
SELECT 'usuario_salud', COUNT(*) FROM usuario_salud
UNION ALL
SELECT 'usuarioperiferico', COUNT(*) FROM usuarioperiferico
UNION ALL
SELECT 'profesionalsalud', COUNT(*) FROM profesionalsalud
UNION ALL
SELECT 'prestadorsalud', COUNT(*) FROM prestadorsalud
UNION ALL
SELECT 'configuracionclinica', COUNT(*) FROM configuracionclinica;

-- Mostrar resumen de usuarios de salud (pacientes)
SELECT 
    id,
    ci,
    nombre || ' ' || apellido AS nombre_completo,
    fecha_nacimiento,
    departamento,
    hcen_user_id,
    tenant_id
FROM usuario_salud
ORDER BY id;

-- Mostrar resumen de profesionales de salud
SELECT 
    p.id,
    u.nombre,
    u.nickname,
    p.especialidad,
    p.ci,
    p.departamento
FROM profesionalsalud p
JOIN usuarioperiferico u ON p.id = u.id
WHERE p.dtype = 'ProfesionalSalud'
ORDER BY p.id;

-- Mostrar resumen del nodo periférico
SELECT 
    id,
    nombre,
    rut,
    departamento,
    localidad,
    estado
FROM nodoperiferico
ORDER BY id;

-- Mostrar resumen de prestadores
SELECT 
    id,
    nombre,
    rut,
    departamento,
    estado
FROM prestadorsalud
ORDER BY id;

-- ============================================================================
-- DOCUMENTOS CLÍNICOS (MongoDB)
-- ============================================================================
-- Los documentos clínicos se almacenan en MongoDB
-- Para cargarlos, ejecuta el siguiente comando de MongoDB después de este script:
-- 
-- mongosh --username admin --password adminpassword --authenticationDatabase admin hcen_db --eval "
-- db.documentos_clinicos.insertMany([
--   {
--     _id: UUID(),
--     paciente_ci: '50830691',
--     paciente_nombre: 'Victor David Alvez González',
--     profesional_ci: '32145678',
--     profesional_nombre: 'Dr. Juan Pérez',
--     tipo_documento: 'CONSULTA',
--     fecha_consulta: new Date('2024-01-15'),
--     especialidad: 'CARDIOLOGIA',
--     diagnostico: 'Hipertensión arterial controlada',
--     indicaciones: 'Continuar con tratamiento antihipertensivo. Control en 3 meses.',
--     tenant_id: '1',
--     fecha_creacion: new Date(),
--     estado: 'ACTIVO'
--   },
--   {
--     _id: UUID(),
--     paciente_ci: '50830691',
--     paciente_nombre: 'Victor David Alvez González',
--     profesional_ci: '34567890',
--     profesional_nombre: 'Dra. Laura Martínez',
--     tipo_documento: 'LABORATORIO',
--     fecha_consulta: new Date('2024-02-10'),
--     especialidad: 'MEDICINA_GENERAL',
--     diagnostico: 'Hemograma completo - Valores normales',
--     indicaciones: 'Sin observaciones',
--     tenant_id: '1',
--     fecha_creacion: new Date(),
--     estado: 'ACTIVO'
--   },
--   {
--     _id: UUID(),
--     paciente_ci: '25850303',
--     paciente_nombre: 'María Laura López García',
--     profesional_ci: '35678901',
--     profesional_nombre: 'Dr. Roberto Sánchez',
--     tipo_documento: 'CONSULTA',
--     fecha_consulta: new Date('2024-03-05'),
--     especialidad: 'MEDICINA_GENERAL',
--     diagnostico: 'Control de rutina - Sin hallazgos patológicos',
--     indicaciones: 'Mantener hábitos saludables. Próximo control anual.',
--     tenant_id: '1',
--     fecha_creacion: new Date(),
--     estado: 'ACTIVO'
--   },
--   {
--     _id: UUID(),
--     paciente_ci: '58076354',
--     paciente_nombre: 'Carlos Alberto Rodríguez Martínez',
--     profesional_ci: '32145678',
--     profesional_nombre: 'Dr. Juan Pérez',
--     tipo_documento: 'RECETA',
--     fecha_consulta: new Date('2024-02-20'),
--     especialidad: 'CARDIOLOGIA',
--     diagnostico: 'Medicación antihipertensiva',
--     indicaciones: 'Losartan 50mg - 1 comprimido cada 12 horas por 90 días',
--     tenant_id: '1',
--     fecha_creacion: new Date(),
--     estado: 'ACTIVO'
--   },
--   {
--     _id: UUID(),
--     paciente_ci: '39178531',
--     paciente_nombre: 'Pedro José González Díaz',
--     profesional_ci: '34567890',
--     profesional_nombre: 'Dra. Laura Martínez',
--     tipo_documento: 'CONSULTA',
--     fecha_consulta: new Date('2024-01-25'),
--     especialidad: 'PEDIATRIA',
--     diagnostico: 'Control pediátrico - Desarrollo normal para la edad',
--     indicaciones: 'Continuar con vacunación según calendario. Control en 6 meses.',
--     tenant_id: '1',
--     fecha_creacion: new Date(),
--     estado: 'ACTIVO'
--   }
-- ]);
-- "
-- 
-- O guarda el script en load-test-data-mongodb.js y ejecútalo:
-- mongosh --username admin --password adminpassword --authenticationDatabase admin hcen_db load-test-data-mongodb.js
-- ============================================================================

-- ============================================================================
-- NOTAS IMPORTANTES
-- ============================================================================
-- 1. Las CIs de usuario_salud deben coincidir con PDI INUS (cod_docum) y 
--    HCEN Backend (codigo_documento)
-- 2. El RUT del nodo periférico debe coincidir con HCEN Backend
-- 3. Los hcen_user_id de usuario_salud deben coincidir con los IDs de la 
--    tabla users en HCEN Backend
-- 4. El tenant_id debe ser el ID del nodo periférico (normalmente 1)
-- 5. Los profesionales usan SINGLE_TABLE inheritance, por lo que los datos
--    están en una sola tabla con un campo discriminador (dtype)
-- 6. Las contraseñas están hasheadas con BCrypt (contraseña: profesional123)
-- 7. Los documentos clínicos se almacenan en MongoDB (ver sección anterior)
-- 
-- MULTI-TENANCY:
-- - Si usas schemas separados por clínica, ejecuta este script en cada schema
-- - Para Hospital Central: schema = tenant_1 o clinica_210123450015
-- - Para Clínica del Este: schema = tenant_2 o clinica_214587960018
-- - Ajusta los datos según corresponda a cada clínica
-- ============================================================================

-- ============================================================================
-- SCRIPT ALTERNATIVO PARA MULTI-TENANT
-- ============================================================================
-- Si necesitas cargar datos para múltiples clínicas, puedes usar algo así:
/*
-- Para Hospital Central (tenant_id = 1, RUT = 210123450015)
SET search_path TO clinica_210123450015;
-- ... ejecutar inserts arriba ...

-- Para Clínica del Este (tenant_id = 2, RUT = 214587960018)
SET search_path TO clinica_214587960018;
-- ... ejecutar inserts con datos de Clínica del Este ...
-- (ajustar pacientes y profesionales según corresponda)
*/
-- ============================================================================

