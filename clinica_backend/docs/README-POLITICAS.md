# Integración con Políticas de Acceso

Este documento describe cómo usar las funcionalidades de verificación de permisos y solicitud de acceso del componente periférico.

## 📋 Índice

- [Descripción General](#descripción-general)
- [Endpoints Relacionados](#endpoints-relacionados)
- [Ejemplos de Uso](#ejemplos-de-uso)
- [Flujo Completo](#flujo-completo)

## Descripción General

El componente periférico ahora incluye:

1. **Verificación Automática de Permisos**: Al acceder a un documento, se verifica si el profesional tiene permiso
2. **Registro de Accesos**: Todos los accesos se registran automáticamente para auditoría
3. **Solicitud de Acceso**: Los profesionales pueden solicitar acceso desde el componente periférico

**Requisitos:**
- El servicio de políticas debe estar desplegado y funcionando
- URL por defecto: `http://127.0.0.1:8080/hcen-politicas-service/api`

## Endpoints Relacionados

### 1. Descargar Contenido (con verificación de permisos)

**Endpoint:** `GET /api/documentos/{id}/contenido`

**Descripción:** Descarga el contenido de un documento. Verifica automáticamente si el profesional tiene permisos antes de devolver el contenido.

**Autenticación:** Requerida (Bearer Token)

**Respuestas:**
- `200 OK`: Contenido del documento (acceso permitido)
- `401 Unauthorized`: No autenticado o profesional no identificado
- `403 Forbidden`: Acceso denegado - No tiene permisos
- `404 Not Found`: Documento no encontrado

**Ejemplo:**
```powershell
$headers = @{
    "Authorization" = "Bearer $token"
}

$contenido = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/$mongoId/contenido" `
    -Method GET -Headers $headers
```

### 2. Solicitar Acceso a Documentos Específicos

**Endpoint:** `POST /api/documentos/solicitar-acceso`

**Descripción:** Permite a un profesional solicitar acceso a documentos específicos de un paciente.

**Autenticación:** Requerida (Bearer Token)

**Request Body:**
```json
{
  "codDocumPaciente": "12345678",  // Requerido
  "tipoDocumento": "text/plain",   // Opcional
  "documentoId": "doc-123",        // Opcional
  "razonSolicitud": "Necesito revisar el historial",  // Opcional
  "especialidad": "Cardiología"     // Opcional
}
```

**Respuestas:**
- `201 Created`: Solicitud creada exitosamente
- `400 Bad Request`: Campos requeridos faltantes
- `401 Unauthorized`: No autenticado
- `500 Internal Server Error`: Error al crear la solicitud

**Ejemplo:**
```powershell
$solicitudBody = @{
    codDocumPaciente = "12345678"
    tipoDocumento = "text/plain"
    razonSolicitud = "Necesito revisar el historial del paciente"
} | ConvertTo-Json

$solicitud = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitar-acceso" `
    -Method POST -Body $solicitudBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud ID: $($solicitud.solicitudId)"
Write-Host "Estado: $($solicitud.estado)"  # PENDIENTE
```

### 3. Solicitar Acceso a Historia Clínica Completa

**Endpoint:** `POST /api/documentos/solicitar-acceso-historia-clinica`

**Descripción:** Permite a un profesional solicitar acceso a **todos los documentos** de un paciente (su historia clínica completa).

**Autenticación:** Requerida (Bearer Token)

**Request Body:**
```json
{
  "codDocumPaciente": "12345678",  // Requerido
  "razonSolicitud": "Necesito revisar toda la historia clínica",  // Opcional
  "especialidad": "Medicina General"  // Opcional
}
```

**Respuestas:**
- `201 Created`: Solicitud creada exitosamente
- `400 Bad Request`: Campos requeridos faltantes
- `401 Unauthorized`: No autenticado
- `500 Internal Server Error`: Error al crear la solicitud

**Ejemplo:**
```powershell
$solicitudBody = @{
    codDocumPaciente = "12345678"
    razonSolicitud = "Necesito revisar toda la historia clínica del paciente para una evaluación completa"
    especialidad = "Medicina General"
} | ConvertTo-Json

$solicitud = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitar-acceso-historia-clinica" `
    -Method POST -Body $solicitudBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud ID: $($solicitud.solicitudId)"
Write-Host "Estado: $($solicitud.estado)"  # PENDIENTE
Write-Host "Tipo: $($solicitud.tipoSolicitud)"  # HISTORIA_CLINICA_COMPLETA
```

### 4. Aprobar Solicitud de Acceso

**Endpoint:** `POST /api/documentos/solicitudes/{id}/aprobar`

**Descripción:** Aprueba una solicitud de acceso a documentos o historia clínica de un paciente.

**Autenticación:** Requerida (Bearer Token)

**Parámetros:**
- `id` (path): ID de la solicitud a aprobar

**Request Body:**
```json
{
  "resueltoPor": "paciente_12345678",  // Opcional
  "comentario": "Aprobado por el paciente"  // Opcional
}
```

**Respuestas:**
- `200 OK`: Solicitud aprobada exitosamente
- `400 Bad Request`: Error de validación
- `401 Unauthorized`: No autenticado
- `404 Not Found`: Solicitud no encontrada
- `500 Internal Server Error`: Error al aprobar la solicitud

**Ejemplo:**
```powershell
$aprobarBody = @{
    resueltoPor = "paciente_12345678"
    comentario = "Aprobado por el paciente"
} | ConvertTo-Json

$solicitudAprobada = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitudes/$solicitudId/aprobar" `
    -Method POST -Body $aprobarBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud aprobada: Estado $($solicitudAprobada.estado)"  # APROBADA
```

### 5. Rechazar Solicitud de Acceso

**Endpoint:** `POST /api/documentos/solicitudes/{id}/rechazar`

**Descripción:** Rechaza una solicitud de acceso a documentos o historia clínica de un paciente.

**Autenticación:** Requerida (Bearer Token)

**Parámetros:**
- `id` (path): ID de la solicitud a rechazar

**Request Body:**
```json
{
  "resueltoPor": "paciente_12345678",  // Opcional
  "comentario": "Rechazado por el paciente"  // Opcional
}
```

**Respuestas:**
- `200 OK`: Solicitud rechazada exitosamente
- `400 Bad Request`: Error de validación
- `401 Unauthorized`: No autenticado
- `404 Not Found`: Solicitud no encontrada
- `500 Internal Server Error`: Error al rechazar la solicitud

**Ejemplo:**
```powershell
$rechazarBody = @{
    resueltoPor = "paciente_12345678"
    comentario = "Rechazado por el paciente"
} | ConvertTo-Json

$solicitudRechazada = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitudes/$solicitudId/rechazar" `
    -Method POST -Body $rechazarBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud rechazada: Estado $($solicitudRechazada.estado)"  # RECHAZADA
```

## Ejemplos de Uso

### Escenario 1: Acceso con Permiso

```powershell
# 1. Crear política de acceso (desde servicio de políticas)
$politicaBody = @{
    alcance = "TODOS_LOS_DOCUMENTOS"
    duracion = "INDEFINIDA"
    gestion = "AUTOMATICA"
    codDocumPaciente = "12345678"
    profesionalAutorizado = "prof_001"
} | ConvertTo-Json

$politica = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/politicas" `
    -Method POST -Body $politicaBody -ContentType "application/json"

# 2. Login como profesional
$loginBody = @{
    nickname = "prof_001"
    password = "password123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/auth/login" `
    -Method POST -Body $loginBody -ContentType "application/json"

$token = $loginResponse.token
$headers = @{ "Authorization" = "Bearer $token" }

# 3. Acceder al documento
$contenido = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/$mongoId/contenido" `
    -Method GET -Headers $headers

Write-Host "✓ Acceso permitido"
Write-Host "Contenido: $contenido"
```

### Escenario 2: Acceso Sin Permiso

```powershell
# Intentar acceder sin crear política primero
try {
    $contenido = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/$mongoId/contenido" `
        -Method GET -Headers $headers
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Host "✗ Acceso denegado: No tiene permisos"
        Write-Host "  El acceso fue registrado para auditoría"
    }
}
```

### Escenario 3: Solicitar Acceso a Documentos Específicos y Aprobar

```powershell
# 1. Profesional solicita acceso a documentos específicos
$solicitudBody = @{
    codDocumPaciente = "12345678"
    tipoDocumento = "text/plain"
    razonSolicitud = "Necesito revisar el historial para una consulta"
} | ConvertTo-Json

$solicitud = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitar-acceso" `
    -Method POST -Body $solicitudBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud creada: ID $($solicitud.solicitudId)"

# 2. Ver solicitudes pendientes del paciente (desde servicio de políticas)
$pendientes = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/solicitudes/paciente/12345678/pendientes" `
    -Method GET

Write-Host "Solicitudes pendientes: $($pendientes.Count)"

# 3. Paciente aprueba la solicitud (desde componente periférico)
$aprobarBody = @{
    resueltoPor = "paciente_12345678"
    comentario = "Aprobado"
} | ConvertTo-Json

$solicitudAprobada = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitudes/$($solicitud.solicitudId)/aprobar" `
    -Method POST -Body $aprobarBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud aprobada: Estado $($solicitudAprobada.estado)"
```

### Escenario 4: Solicitar Acceso a Historia Clínica Completa y Aprobar

```powershell
# 1. Profesional solicita acceso a toda la historia clínica
$solicitudBody = @{
    codDocumPaciente = "12345678"
    razonSolicitud = "Necesito revisar toda la historia clínica del paciente para una evaluación completa"
    especialidad = "Medicina General"
} | ConvertTo-Json

$solicitud = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitar-acceso-historia-clinica" `
    -Method POST -Body $solicitudBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud creada: ID $($solicitud.solicitudId)"
Write-Host "Tipo: $($solicitud.tipoSolicitud)"  # HISTORIA_CLINICA_COMPLETA

# 2. Verificar solicitud en servicio de políticas
$solicitudVerificada = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/solicitudes/$($solicitud.solicitudId)" `
    -Method GET

Write-Host "Estado: $($solicitudVerificada.estado)"  # PENDIENTE

# 3. Paciente aprueba la solicitud (desde componente periférico)
$aprobarBody = @{
    resueltoPor = "paciente_12345678"
    comentario = "Aprobado por el paciente"
} | ConvertTo-Json

$solicitudAprobada = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitudes/$($solicitud.solicitudId)/aprobar" `
    -Method POST -Body $aprobarBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud aprobada: Estado $($solicitudAprobada.estado)"  # APROBADA

# 4. Verificar estado final
$solicitudFinal = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/solicitudes/$($solicitud.solicitudId)" `
    -Method GET

Write-Host "Estado final: $($solicitudFinal.estado)"  # APROBADA
```

### Escenario 5: Rechazar Solicitud

```powershell
# 1. Crear solicitud
$solicitudBody = @{
    codDocumPaciente = "12345678"
    razonSolicitud = "Solicitud para probar rechazo"
} | ConvertTo-Json

$solicitud = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitar-acceso-historia-clinica" `
    -Method POST -Body $solicitudBody -ContentType "application/json" -Headers $headers

# 2. Rechazar la solicitud
$rechazarBody = @{
    resueltoPor = "paciente_12345678"
    comentario = "Rechazado por el paciente"
} | ConvertTo-Json

$solicitudRechazada = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitudes/$($solicitud.solicitudId)/rechazar" `
    -Method POST -Body $rechazarBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud rechazada: Estado $($solicitudRechazada.estado)"  # RECHAZADA
```

# 4. Crear política de acceso automática (opcional, desde servicio de políticas)
$politicaAuto = @{
    alcance = "TODOS_LOS_DOCUMENTOS"
    duracion = "TEMPORAL"
    gestion = "AUTOMATICA"
    codDocumPaciente = "12345678"
    profesionalAutorizado = "prof_001"
    fechaVencimiento = (Get-Date).AddDays(30).ToString("yyyy-MM-ddTHH:mm:ssZ")
} | ConvertTo-Json

$politica = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/politicas" `
    -Method POST -Body $politicaAuto -ContentType "application/json"

Write-Host "Política creada: ID $($politica.id)"

# 5. Ahora el profesional puede acceder
$contenido = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/$mongoId/contenido" `
    -Method GET -Headers $headers

Write-Host "✓ Acceso permitido después de aprobación"
```

## Flujo Completo

```
┌─────────────────┐
│  Profesional    │
└────────┬────────┘
         │
         │ 1. Solicita acceso
         │    POST /documentos/solicitar-acceso
         │    o
         │    POST /documentos/solicitar-acceso-historia-clinica
         ▼
┌─────────────────────────────────┐
│ Componente Periférico           │
│ Crea solicitud                  │
└────────┬────────────────────────┘
         │
         │ 2. Crea solicitud
         ▼
┌─────────────────────────────────┐
│ Servicio de Políticas           │
│ Estado: PENDIENTE               │
└────────┬────────────────────────┘
         │
         │ 3. Paciente revisa
         ▼
┌─────────────────────────────────┐
│ Paciente aprueba/rechaza        │
│ POST /documentos/solicitudes/   │
│      {id}/aprobar               │
│ o                               │
│ POST /documentos/solicitudes/   │
│      {id}/rechazar              │
└────────┬────────────────────────┘
         │
         │ 4. Crear política (opcional)
         ▼
┌─────────────────────────────────┐
│ Política de Acceso creada       │
│ Estado: ACTIVA                  │
└────────┬────────────────────────┘
         │
         │ 5. Profesional accede
         ▼
┌─────────────────────────────────┐
│ Componente Periférico           │
│ GET /documentos/{id}/contenido   │
│ ✓ Verifica permiso              │
│ ✓ Registra acceso               │
│ ✓ Devuelve contenido            │
└─────────────────────────────────┘
```

## Verificación de Registros

Para verificar que los accesos se están registrando:

```powershell
# Ver todos los registros de un paciente
$registros = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/registros/paciente/12345678" `
    -Method GET

Write-Host "Total de accesos: $($registros.Count)"
foreach ($registro in $registros) {
    Write-Host "- Fecha: $($registro.fecha)"
    Write-Host "  Referencia: $($registro.referencia)"
}

# Ver registros de un profesional
$registrosProf = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/registros/profesional/prof_001" `
    -Method GET

Write-Host "Accesos del profesional: $($registrosProf.Count)"
```

## Notas Importantes

1. **Autenticación Requerida**: Todos los endpoints requieren autenticación JWT
2. **Verificación Automática**: La verificación de permisos es automática, no requiere llamadas adicionales
3. **Registro Automático**: Todos los accesos se registran automáticamente (exitosos y denegados)
4. **Servicio de Políticas**: El servicio de políticas debe estar desplegado y accesible
5. **Políticas vs Solicitudes**: Las políticas permiten acceso directo, las solicitudes requieren aprobación
6. **Endpoints Separados**: 
   - `/documentos/solicitar-acceso`: Para solicitar acceso a documentos específicos
   - `/documentos/solicitar-acceso-historia-clinica`: Para solicitar acceso a toda la historia clínica
   - `/documentos/solicitudes/{id}/aprobar`: Para aprobar solicitudes (desde componente periférico)
   - `/documentos/solicitudes/{id}/rechazar`: Para rechazar solicitudes (desde componente periférico)
7. **Aprobación/Rechazo**: Los endpoints de aprobar y rechazar están disponibles tanto en el componente periférico como en el servicio de políticas. Ambos funcionan de la misma manera.

## Referencias

- [Documentación OpenAPI del Componente Periférico](openapi-periferico.yaml)
- [Documentación del Servicio de Políticas](../../hcen/docs/README-POLITICAS.md)
- [OpenAPI del Servicio de Políticas](../../hcen/docs/openapi-politicas.yaml)






