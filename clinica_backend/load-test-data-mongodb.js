// ============================================================================
// Script de carga de documentos clínicos de prueba para MongoDB
// ============================================================================
// Este script carga documentos clínicos de prueba en MongoDB para el
// Componente Periférico
//
// USO:
//   mongosh --username admin --password adminpassword --authenticationDatabase admin hcen_db load-test-data-mongodb.js
//
// O con Docker:
//   docker exec -i mongodb mongosh --username admin --password adminpassword --authenticationDatabase admin hcen_db < load-test-data-mongodb.js
// ============================================================================

// Seleccionar la base de datos
db = db.getSiblingDB('hcen_db');

print('============================================================================');
print('  Cargando documentos clínicos de prueba en MongoDB');
print('============================================================================');

// Limpiar documentos existentes (solo para pruebas)
db.documentos_clinicos.deleteMany({tenant_id: '1'});
print('✓ Documentos anteriores eliminados');

// ============================================================================
// DOCUMENTOS CLÍNICOS
// ============================================================================

const documentos = [
  // Documento 1: Consulta cardiológica de Victor Alvez
  {
    _id: UUID(),
    paciente_ci: '50830691',
    paciente_nombre: 'Victor David Alvez González',
    profesional_ci: '32145678',
    profesional_nombre: 'Dr. Juan Pérez',
    profesional_especialidad: 'CARDIOLOGIA',
    tipo_documento: 'CONSULTA',
    fecha_consulta: new Date('2024-01-15T10:30:00'),
    motivo_consulta: 'Control cardiológico de rutina',
    diagnostico: 'Hipertensión arterial controlada',
    observaciones: 'Paciente con buen control de presión arterial. Sin síntomas. Continúa tratamiento.',
    indicaciones: 'Continuar con tratamiento antihipertensivo actual. Control en 3 meses. Mantener dieta hiposódica.',
    medicamentos: [
      'Losartan 50mg - 1 comprimido cada 12 horas',
      'Atenolol 25mg - 1 comprimido al día en ayunas'
    ],
    examenes_solicitados: [],
    tenant_id: '1',
    fecha_creacion: new Date('2024-01-15T10:45:00'),
    fecha_modificacion: new Date('2024-01-15T10:45:00'),
    estado: 'ACTIVO',
    sincronizado_hcen: true,
    metadata_id: null
  },

  // Documento 2: Laboratorio de Victor Alvez
  {
    _id: UUID(),
    paciente_ci: '50830691',
    paciente_nombre: 'Victor David Alvez González',
    profesional_ci: '34567890',
    profesional_nombre: 'Dra. Laura Martínez',
    profesional_especialidad: 'MEDICINA_GENERAL',
    tipo_documento: 'LABORATORIO',
    fecha_consulta: new Date('2024-02-10T09:00:00'),
    motivo_consulta: 'Hemograma completo solicitado por cardiólogo',
    diagnostico: 'Hemograma completo - Valores dentro de rangos normales',
    observaciones: 'Glóbulos rojos: 4.8 M/μL, Glóbulos blancos: 7200/μL, Hemoglobina: 14.5 g/dL, Hematocrito: 42%, Plaquetas: 250000/μL',
    indicaciones: 'Sin observaciones. Valores normales.',
    medicamentos: [],
    examenes_solicitados: [],
    tenant_id: '1',
    fecha_creacion: new Date('2024-02-10T14:30:00'),
    fecha_modificacion: new Date('2024-02-10T14:30:00'),
    estado: 'ACTIVO',
    sincronizado_hcen: true,
    metadata_id: null
  },

  // Documento 3: Consulta de María López
  {
    _id: UUID(),
    paciente_ci: '25850303',
    paciente_nombre: 'María Laura López García',
    profesional_ci: '35678901',
    profesional_nombre: 'Dr. Roberto Sánchez',
    profesional_especialidad: 'MEDICINA_GENERAL',
    tipo_documento: 'CONSULTA',
    fecha_consulta: new Date('2024-03-05T11:15:00'),
    motivo_consulta: 'Control médico anual',
    diagnostico: 'Control de rutina - Sin hallazgos patológicos',
    observaciones: 'Paciente asintomática. Examen físico normal. Presión arterial: 120/80 mmHg. Peso: 62kg. Talla: 165cm. IMC: 22.8 (normal)',
    indicaciones: 'Mantener hábitos saludables. Alimentación balanceada y actividad física regular. Próximo control en 12 meses.',
    medicamentos: [],
    examenes_solicitados: [
      'Mamografía (próximo año)',
      'Papanicolaou (próximo año)'
    ],
    tenant_id: '1',
    fecha_creacion: new Date('2024-03-05T11:45:00'),
    fecha_modificacion: new Date('2024-03-05T11:45:00'),
    estado: 'ACTIVO',
    sincronizado_hcen: true,
    metadata_id: null
  },

  // Documento 4: Receta para Carlos Rodríguez
  {
    _id: UUID(),
    paciente_ci: '58076354',
    paciente_nombre: 'Carlos Alberto Rodríguez Martínez',
    profesional_ci: '32145678',
    profesional_nombre: 'Dr. Juan Pérez',
    profesional_especialidad: 'CARDIOLOGIA',
    tipo_documento: 'RECETA',
    fecha_consulta: new Date('2024-02-20T15:00:00'),
    motivo_consulta: 'Renovación de medicación antihipertensiva',
    diagnostico: 'Hipertensión arterial esencial',
    observaciones: 'Paciente con buen control. Presión arterial: 125/82 mmHg',
    indicaciones: 'Tomar medicación según prescripción. Evitar alimentos con alto contenido de sodio.',
    medicamentos: [
      'Losartan 50mg - 1 comprimido cada 12 horas (90 días)',
      'Hidroclorotiazida 12.5mg - 1 comprimido al día en ayunas (90 días)',
      'Aspirina 100mg - 1 comprimido al día después del desayuno (90 días)'
    ],
    examenes_solicitados: [],
    tenant_id: '1',
    fecha_creacion: new Date('2024-02-20T15:15:00'),
    fecha_modificacion: new Date('2024-02-20T15:15:00'),
    estado: 'ACTIVO',
    sincronizado_hcen: true,
    metadata_id: null,
    vigencia_hasta: new Date('2024-05-20')
  },

  // Documento 5: Consulta pediátrica de Pedro González (menor)
  {
    _id: UUID(),
    paciente_ci: '39178531',
    paciente_nombre: 'Pedro José González Díaz',
    profesional_ci: '34567890',
    profesional_nombre: 'Dra. Laura Martínez',
    profesional_especialidad: 'PEDIATRIA',
    tipo_documento: 'CONSULTA',
    fecha_consulta: new Date('2024-01-25T16:00:00'),
    motivo_consulta: 'Control pediátrico de rutina',
    diagnostico: 'Control pediátrico - Desarrollo y crecimiento normal para la edad',
    observaciones: 'Adolescente de 13 años. Peso: 45kg. Talla: 155cm. Desarrollo puberal adecuado para la edad. Sin patologías.',
    indicaciones: 'Continuar con alimentación saludable. Actividad física regular. Completar esquema de vacunación. Control en 6 meses.',
    medicamentos: [],
    examenes_solicitados: [],
    tenant_id: '1',
    fecha_creacion: new Date('2024-01-25T16:30:00'),
    fecha_modificacion: new Date('2024-01-25T16:30:00'),
    estado: 'ACTIVO',
    sincronizado_hcen: false,  // No sincronizado (paciente menor)
    metadata_id: null,
    requiere_autorizacion_tutor: true
  },

  // Documento 6: Radiografía de Carlos Rodríguez
  {
    _id: UUID(),
    paciente_ci: '58076354',
    paciente_nombre: 'Carlos Alberto Rodríguez Martínez',
    profesional_ci: '32145678',
    profesional_nombre: 'Dr. Juan Pérez',
    profesional_especialidad: 'CARDIOLOGIA',
    tipo_documento: 'RADIOGRAFIA',
    fecha_consulta: new Date('2024-03-10T10:00:00'),
    motivo_consulta: 'Radiografía de tórax - control cardiológico',
    diagnostico: 'Radiografía de tórax - Sin alteraciones significativas',
    observaciones: 'Silueta cardíaca de tamaño normal. Campos pulmonares libres. Sin infiltrados ni consolidaciones. Sin derrame pleural.',
    indicaciones: 'Sin hallazgos patológicos. Continuar seguimiento cardiológico.',
    medicamentos: [],
    examenes_solicitados: [],
    tenant_id: '1',
    fecha_creacion: new Date('2024-03-10T11:00:00'),
    fecha_modificacion: new Date('2024-03-10T11:00:00'),
    estado: 'ACTIVO',
    sincronizado_hcen: true,
    metadata_id: null,
    archivo_adjunto: 'rxd_58076354_20240310.dcm'
  },

  // Documento 7: Consulta de Roberto Silva (extranjero)
  {
    _id: UUID(),
    paciente_ci: '26347848',
    paciente_nombre: 'Roberto Silva Santos',
    profesional_ci: '35678901',
    profesional_nombre: 'Dr. Roberto Sánchez',
    profesional_especialidad: 'MEDICINA_GENERAL',
    tipo_documento: 'CONSULTA',
    fecha_consulta: new Date('2024-02-15T14:00:00'),
    motivo_consulta: 'Consulta por malestar general',
    diagnostico: 'Síndrome gripal',
    observaciones: 'Paciente extranjero (Brasil). Presenta fiebre (38.2°C), dolor de garganta y congestión nasal desde hace 2 días.',
    indicaciones: 'Reposo domiciliario. Hidratación abundante. Paracetamol 500mg cada 6 horas por 5 días si presenta fiebre. Consultar si los síntomas persisten más de 7 días.',
    medicamentos: [
      'Paracetamol 500mg - 1 comprimido cada 6 horas si fiebre (5 días)',
      'Ibuprofeno 400mg - 1 comprimido cada 8 horas si dolor (5 días)'
    ],
    examenes_solicitados: [],
    tenant_id: '1',
    fecha_creacion: new Date('2024-02-15T14:30:00'),
    fecha_modificacion: new Date('2024-02-15T14:30:00'),
    estado: 'ACTIVO',
    sincronizado_hcen: true,
    metadata_id: null
  }
];

// Insertar documentos
const resultado = db.documentos_clinicos.insertMany(documentos);
print(`✓ ${documentos.length} documentos clínicos insertados`);

// Crear índices para mejorar el rendimiento
db.documentos_clinicos.createIndex({ paciente_ci: 1, tenant_id: 1 });
db.documentos_clinicos.createIndex({ profesional_ci: 1, tenant_id: 1 });
db.documentos_clinicos.createIndex({ fecha_consulta: -1 });
db.documentos_clinicos.createIndex({ tipo_documento: 1 });
db.documentos_clinicos.createIndex({ estado: 1 });
print('✓ Índices creados');

// Verificar documentos cargados
const count = db.documentos_clinicos.countDocuments({tenant_id: '1'});
print('\n============================================================================');
print(`✓ Total de documentos cargados: ${count}`);
print('============================================================================\n');

// Mostrar resumen por paciente
print('Resumen de documentos por paciente:\n');
const resumen = db.documentos_clinicos.aggregate([
  { $match: { tenant_id: '1' } },
  { $group: {
      _id: { ci: '$paciente_ci', nombre: '$paciente_nombre' },
      total_documentos: { $sum: 1 },
      tipos: { $addToSet: '$tipo_documento' }
    }
  },
  { $sort: { '_id.ci': 1 } }
]);

resumen.forEach(doc => {
  print(`  CI ${doc._id.ci} - ${doc._id.nombre}`);
  print(`    Documentos: ${doc.total_documentos} (${doc.tipos.join(', ')})\n`);
});

print('============================================================================');
print('Datos de MongoDB cargados exitosamente');
print('Los documentos son consistentes con los datos de PostgreSQL');
print('============================================================================\n');

