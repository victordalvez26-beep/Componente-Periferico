-- ============================================================================
-- Script de carga de datos de prueba para Componente Periférico (CORREGIDO)
-- ============================================================================
-- Funciona con la estructura de tablas actual
-- Tablas: nodoperiferico, usuario, usuarioperiferico, administradorclinica
-- ============================================================================

-- Limpiar datos existentes (solo para pruebas)
DELETE FROM administradorclinica;
DELETE FROM usuarioperiferico;
DELETE FROM usuario;
DELETE FROM nodoperiferico;

-- ============================================================================
-- NODO PERIFÉRICO (Información de la Clínica)
-- ============================================================================

INSERT INTO nodoperiferico (id, nombre, rut, schema_name, created_at) VALUES
(1, 'Hospital Central', '210123450015', 'clinica_210123450015', CURRENT_TIMESTAMP);

-- Resetear la secuencia del ID
SELECT setval('nodoperiferico_id_seq', 1, true);

-- ============================================================================
-- USUARIOS BASE
-- ============================================================================

INSERT INTO usuario (id, nombre, email) VALUES
(100, 'Dr. Juan Pérez', 'juan.perez@hospitalcentral.com.uy'),
(101, 'Dra. Laura Martínez', 'laura.martinez@hospitalcentral.com.uy'),
(102, 'Dr. Roberto Sánchez', 'roberto.sanchez@hospitalcentral.com.uy'),
(103, 'Admin Central', 'admin@hospitalcentral.com.uy'),
(104, 'Admin Este', 'admin_este@hospitalcentral.com.uy');

-- Resetear la secuencia del ID
SELECT setval('usuario_id_seq', 104, true);

-- ============================================================================
-- USUARIOS PERIFÉRICOS
-- ============================================================================
-- dtype valores: 'ProfesionalSalud', 'AdministradorClinica', 'OtrosActoresSalud'
-- contraseña: 1234567@ (todos igual)


INSERT INTO usuarioperiferico (id, nickname, password_hash, tenant_id, role, dtype) VALUES
(100, 'dr_perez', '$2a$10$qgjKNZru0nlHNd9KFljnZOgKDRU7Baf/NdH4ROS025eQRY/sC3uLG', '1', 'PROFESIONAL', 'ProfesionalSalud'),
(101, 'dra_martinez', '$2a$10$qgjKNZru0nlHNd9KFljnZOgKDRU7Baf/NdH4ROS025eQRY/sC3uLG', '1', 'PROFESIONAL', 'ProfesionalSalud'),
(102, 'dr_sanchez', '$2a$10$qgjKNZru0nlHNd9KFljnZOgKDRU7Baf/NdH4ROS025eQRY/sC3uLG', '1', 'PROFESIONAL', 'ProfesionalSalud'),
(103, 'admin_central', '$2a$10$qgjKNZru0nlHNd9KFljnZOgKDRU7Baf/NdH4ROS025eQRY/sC3uLG', '1', 'ADMINISTRADOR', 'AdministradorClinica'),
(104, 'admin_este', '$2a$10$qgjKNZru0nlHNd9KFljnZOgKDRU7Baf/NdH4ROS025eQRY/sC3uLG', '2', 'ADMINISTRADOR', 'AdministradorClinica');

-- ============================================================================
-- ADMINISTRADORES DE CLÍNICA
-- ============================================================================

INSERT INTO administradorclinica (id, nodo_periferico_id) VALUES
(103, 1),  -- admin_central administra Hospital Central (nodo 1)
(104, 2);  -- admin_este también puede administrar Hospital Central

-- ============================================================================
-- VERIFICACIÓN
-- ============================================================================

SELECT 'nodoperiferico' AS tabla, COUNT(*) AS total FROM nodoperiferico
UNION ALL SELECT 'usuario', COUNT(*) FROM usuario
UNION ALL SELECT 'usuarioperiferico', COUNT(*) FROM usuarioperiferico
UNION ALL SELECT 'administradorclinica', COUNT(*) FROM administradorclinica;

\echo ''
\echo '--- Usuarios Periféricos ---'
SELECT 
    u.id,
    u.nombre,
    up.nickname,
    up.dtype AS tipo,
    up.role AS rol,
    up.tenant_id
FROM usuarioperiferico up
JOIN usuario u ON up.id = u.id
ORDER BY u.id;

\echo ''
\echo '--- Nodos Periféricos ---'
SELECT id, nombre, rut, schema_name FROM nodoperiferico;

\echo ''
\echo '✓ Componente Periférico: Datos de PostgreSQL cargados exitosamente'
\echo ''
\echo 'NOTA: Los documentos clínicos se almacenan en MongoDB.'
\echo 'Ejecutar: docker exec -i mongodb mongosh --username admin --password adminpassword --authenticationDatabase admin hcen_db < load-test-data-mongodb.js'

