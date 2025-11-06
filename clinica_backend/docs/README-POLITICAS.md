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

### 2. Solicitar Acceso

**Endpoint:** `POST /api/documentos/solicitar-acceso`

**Descripción:** Permite a un profesional solicitar acceso a documentos de un paciente.

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

### Escenario 3: Solicitar Acceso y Aprobar

```powershell
# 1. Profesional solicita acceso
$solicitudBody = @{
    codDocumPaciente = "12345678"
    razonSolicitud = "Necesito revisar el historial para una consulta"
} | ConvertTo-Json

$solicitud = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitar-acceso" `
    -Method POST -Body $solicitudBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud creada: ID $($solicitud.solicitudId)"

# 2. Ver solicitudes pendientes del paciente (desde servicio de políticas)
$pendientes = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/solicitudes/paciente/12345678/pendientes" `
    -Method GET

Write-Host "Solicitudes pendientes: $($pendientes.Count)"

# 3. Paciente aprueba la solicitud (desde servicio de políticas)
$aprobarBody = @{
    resueltoPor = "paciente_12345678"
    comentario = "Aprobado"
} | ConvertTo-Json

$solicitudAprobada = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/solicitudes/$($solicitud.solicitudId)/aprobar" `
    -Method POST -Body $aprobarBody -ContentType "application/json"

Write-Host "Solicitud aprobada: Estado $($solicitudAprobada.estado)"

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
         ▼
┌─────────────────────────────────┐
│ Componente Periférico           │
│ POST /documentos/solicitar-acceso│
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
│ POST /solicitudes/{id}/aprobar  │
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

1. **Autenticación Requerida**: Ambos endpoints requieren autenticación JWT
2. **Verificación Automática**: La verificación de permisos es automática, no requiere llamadas adicionales
3. **Registro Automático**: Todos los accesos se registran automáticamente (exitosos y denegados)
4. **Servicio de Políticas**: El servicio de políticas debe estar desplegado y accesible
5. **Políticas vs Solicitudes**: Las políticas permiten acceso directo, las solicitudes requieren aprobación

## Referencias

- [Documentación OpenAPI del Componente Periférico](openapi-periferico.yaml)
- [Documentación del Servicio de Políticas](../../hcen/docs/README-POLITICAS.md)
- [OpenAPI del Servicio de Políticas](../../hcen/docs/openapi-politicas.yaml)

