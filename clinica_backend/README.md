# Componente Periférico - Backend Clínica

Backend del componente periférico que permite a las clínicas gestionar documentos clínicos y enviar sus metadatos al HCEN central.

## Descripción

El componente periférico es el sistema que:
- Permite a profesionales de salud crear documentos clínicos
- Almacena el contenido de documentos en MongoDB
- Envía metadatos de documentos al HCEN central
- Soporta multi-tenancy (múltiples clínicas en la misma instancia)
- Proporciona autenticación mediante JWT

## Arquitectura

```
Profesional → Componente Periférico → HCEN Central → RNDC
                      ↓
                  MongoDB (contenido)
```

## Compilación y Despliegue

Ver la guía completa en [`GUIA-COMPILACION-DESPLIEGUE.md`](../../GUIA-COMPILACION-DESPLIEGUE.md) en la raíz del proyecto.

### Resumen rápido:

#### 1. Clonar el repositorio
```bash
# Compilar
cd componente-periferico/clinica_backend
mvn clean package -DskipTests

# Desplegar
cp ear/target/hcen_clinica.ear $WILDFLY_HOME/standalone/deployments/
```

## Configuración de Entorno

### 1. Variables de Entorno

Crear archivo `.env` en la raíz del proyecto:

```properties
MONGODB_URI=mongodb://mongouser:your_secure_password_here@localhost:27017/?authSource=admin
MONGODB_DB=hcen_db
HCEN_JWT_SECRET_BASE64=<base64-encoded-secret>
```

### 2. Iniciar MongoDB

```bash
docker-compose up -d
```

### 3. Iniciar WildFly

**Windows (PowerShell):**
```powershell
.\start-wildfly.ps1
```

**Linux/Mac:**
```bash
chmod +x start-wildfly.sh
./start-wildfly.sh
```

## Prueba del Flujo Completo

### 1. Crear Tenant y Usuario Admin

```powershell
$initBody = @{
    tenantId = "101"
    nombre = "Clínica Test"
    rut = "11111111"
    colorPrimario = "#007bff"
    nombrePortal = "Clínica Test"
    adminEmail = "admin@test.com"
} | ConvertTo-Json

$initResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/config/init" `
    -Method POST -Body $initBody -ContentType "application/json"

Write-Host "Tenant creado: $($initResponse.tenantId)"
Write-Host "Admin creado: $($initResponse.adminNickname)"
Write-Host "Token: $($initResponse.activationToken)"
```

### 2. Activar Usuario Admin

```powershell
$activateBody = @{
    tenantId = "101"
    token = $initResponse.activationToken
    password = "password123"
} | ConvertTo-Json

$activateResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/config/activate-simple" `
    -Method POST -Body $activateBody -ContentType "application/json"
```

### 3. Login como Admin

```powershell
$loginBody = @{
    nickname = $activateResponse.nickname
    password = "password123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/auth/login" `
    -Method POST -Body $loginBody -ContentType "application/json"

$token = $loginResponse.token
Write-Host "Token obtenido: $($token.Substring(0, 20))..."
```

### 4. Crear Documento Completo (Flujo Principal)

Este endpoint es el corazón del sistema. Crea un documento completo:
- ✅ Guarda el contenido en MongoDB
- ✅ Genera un `documentoId` único si no se proporciona
- ✅ Envía metadatos al HCEN central
- ✅ El HCEN central crea el paciente automáticamente si no existe
- ✅ Los metadatos se encolan en JMS para RNDC

```powershell
$docBody = @{
    contenido = "Diagnóstico: Paciente presenta síntomas de gripe. Tratamiento: reposo y paracetamol."
    documentoIdPaciente = "12345678"
    tenantId = "101"
    especialidad = "Medicina General"
    formato = "text/plain"
    autor = "Dr. Juan Pérez"
    titulo = "Consulta Médica"
    languageCode = "es"
    breakingTheGlass = $false
    descripcion = "Consulta de rutina"
    datosPatronimicos = @{
        nombre = "Juan"
        apellido = "García"
        fechaNacimiento = "1990-01-15"
    }
} | ConvertTo-Json -Depth 10

$headers = @{
    "Authorization" = "Bearer $token"
    "X-Tenant-Id" = "101"
}

$docResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/completo" `
    -Method POST -Body $docBody -ContentType "application/json" -Headers $headers

Write-Host "Documento creado:"
Write-Host "  - Documento ID: $($docResponse.documentoId)"
Write-Host "  - MongoDB ID: $($docResponse.mongoId)"
Write-Host "  - URL Acceso: $($docResponse.urlAcceso)"
```

### 5. Verificar Documento en MongoDB

```powershell
$mongoId = $docResponse.mongoId
$doc = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/$mongoId" -Method GET

Write-Host "Contenido del documento:"
Write-Host $doc.contenido
```

### 6. Consultar Metadatos del Paciente

```powershell
$pacienteId = "12345678"
$metadatos = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/paciente/$pacienteId/metadatos" `
    -Method GET -Headers $headers

Write-Host "Metadatos encontrados: $($metadatos.Count)"
$metadatos | ForEach-Object {
    Write-Host "  - $($_.documentoId): $($_.titulo)"
}
```

### 7. Descargar Contenido del Documento

```powershell
$mongoId = $docResponse.mongoId
$contenido = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/$mongoId/contenido" -Method GET

Write-Host "Contenido descargado:"
Write-Host $contenido
```

## Endpoints Principales

### Autenticación
- `POST /api/auth/login` - Login y obtención de token JWT
- `POST /api/config/init` - Crear tenant y usuario admin
- `POST /api/config/activate-simple` - Activar usuario admin

### Documentos
- `POST /api/documentos/completo` - Crear documento completo (contenido + metadatos)
- `POST /api/documentos` - Guardar solo contenido (requiere metadatos previos)
- `GET /api/documentos/{id}` - Obtener documento por MongoDB ID
- `GET /api/documentos/{id}/contenido` - Descargar contenido del documento (con verificación de permisos)
- `GET /api/documentos/paciente/{documentoIdPaciente}/metadatos` - Metadatos de documentos del paciente
- `POST /api/documentos/solicitar-acceso` - Solicitar acceso a documentos de un paciente

### Profesionales
- `GET /api/profesionales` - Listar profesionales (requiere rol ADMINISTRADOR)
- `POST /api/profesionales` - Crear profesional (requiere rol ADMINISTRADOR)

### Portal
- `GET /api/portal/configuracion` - Obtener configuración del portal
- `PUT /api/portal/configuracion` - Actualizar configuración del portal

## Verificación del Flujo Completo

Para verificar que todo el flujo funciona correctamente:

1. ✅ **Crear tenant y usuario admin** → Usuario creado y activado
2. ✅ **Login** → Token JWT obtenido
3. ✅ **Crear documento completo** → Documento guardado en MongoDB y metadatos enviados a HCEN
4. ✅ **Esperar 20 segundos** → Procesamiento JMS
5. ✅ **Consultar metadatos en RNDC** → Metadatos encontrados
6. ✅ **Consultar metadatos desde HCEN central** → Paciente puede ver sus documentos
7. ✅ **Descargar documento desde HCEN central** → Contenido descargado correctamente

## Pruebas de Políticas de Acceso

El componente periférico ahora integra verificación de permisos y solicitudes de acceso.

### 1. Crear Política de Acceso

Primero, crear una política que permita al profesional acceder a documentos del paciente:

```powershell
# Desde el servicio de políticas
$politicaBody = @{
    alcance = "TODOS_LOS_DOCUMENTOS"
    duracion = "INDEFINIDA"
    gestion = "AUTOMATICA"
    codDocumPaciente = "12345678"
    profesionalAutorizado = "prof_001"  # Nickname del profesional
    tipoDocumento = $null
    fechaVencimiento = $null
    referencia = "Política de acceso para pruebas"
} | ConvertTo-Json

$politica = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/politicas" `
    -Method POST -Body $politicaBody -ContentType "application/json"

Write-Host "Política creada: ID $($politica.id)"
```

### 2. Acceder a Documento con Verificación de Permisos

Al intentar acceder a un documento, el sistema verifica automáticamente los permisos:

```powershell
# Login como profesional
$loginBody = @{
    nickname = "prof_001"
    password = "password123"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/auth/login" `
    -Method POST -Body $loginBody -ContentType "application/json"

$token = $loginResponse.token

# Intentar acceder al documento
$headers = @{
    "Authorization" = "Bearer $token"
}

# Si tiene permiso: retorna 200 con el contenido
# Si no tiene permiso: retorna 403 Forbidden
try {
    $contenido = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/$mongoId/contenido" `
        -Method GET -Headers $headers
    Write-Host "✓ Acceso permitido - Contenido obtenido"
} catch {
    if ($_.Exception.Response.StatusCode -eq 403) {
        Write-Host "✗ Acceso denegado - No tiene permisos"
    }
}
```

**Nota:** Todos los intentos de acceso (exitosos y denegados) se registran automáticamente en el servicio de políticas.

### 3. Solicitar Acceso desde el Componente Periférico

Un profesional puede solicitar acceso a documentos de un paciente:

```powershell
$solicitudBody = @{
    codDocumPaciente = "12345678"
    tipoDocumento = "text/plain"  # Opcional
    documentoId = "doc-123"       # Opcional - para un documento específico
    razonSolicitud = "Necesito revisar el historial del paciente para una consulta"
    especialidad = "Cardiología"  # Opcional
} | ConvertTo-Json

$solicitud = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-web/api/documentos/solicitar-acceso" `
    -Method POST -Body $solicitudBody -ContentType "application/json" -Headers $headers

Write-Host "Solicitud creada: ID $($solicitud.solicitudId)"
Write-Host "Estado: $($solicitud.estado)"  # PENDIENTE
```

La solicitud queda en estado `PENDIENTE` y debe ser aprobada por el paciente a través del servicio de políticas:

```powershell
# El paciente aprueba la solicitud (desde el servicio de políticas)
$aprobarBody = @{
    resueltoPor = "paciente_12345678"
    comentario = "Aprobado por el paciente"
} | ConvertTo-Json

$solicitudAprobada = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/solicitudes/$($solicitud.solicitudId)/aprobar" `
    -Method POST -Body $aprobarBody -ContentType "application/json"

Write-Host "Solicitud aprobada: Estado $($solicitudAprobada.estado)"
```

### 4. Verificar Registros de Acceso

Para ver todos los accesos registrados:

```powershell
# Ver registros de un paciente
$registros = Invoke-RestMethod -Uri "http://127.0.0.1:8080/hcen-politicas-service/api/registros/paciente/12345678" `
    -Method GET

Write-Host "Registros encontrados: $($registros.Count)"
foreach ($registro in $registros) {
    Write-Host "  - Fecha: $($registro.fecha), Referencia: $($registro.referencia)"
}
```

## Características de Seguridad

### Verificación Automática de Permisos

- ✅ **Al acceder a un documento**: El sistema verifica automáticamente si el profesional tiene permiso
- ✅ **Registro de accesos**: Todos los accesos se registran (exitosos y denegados)
- ✅ **Auditoría completa**: Se puede consultar quién accedió a qué documentos y cuándo

### Flujo de Solicitud de Acceso

1. **Profesional solicita acceso** → Estado: `PENDIENTE`
2. **Paciente revisa solicitud** → Puede aprobar o rechazar
3. **Si se aprueba** → Se puede crear una política de acceso automática
4. **Profesional puede acceder** → Si tiene política activa

### Integración con Servicio de Políticas

El componente periférico se comunica con el servicio de políticas en:
- **URL por defecto**: `http://127.0.0.1:8080/hcen-politicas-service/api`
- **Configurable**: Variable de entorno `POLITICAS_SERVICE_URL`

## Creación Automática de Pacientes

**Importante**: Cuando se crea un documento con un `documentoIdPaciente` que no existe:

- ✅ El HCEN central **crea automáticamente** el paciente
- ✅ Se usan los `datosPatronimicos` proporcionados (nombre, apellido, fechaNacimiento)
- ✅ El paciente queda registrado en el sistema para futuras consultas

**Nota sobre profesionales**: El sistema **NO crea automáticamente** profesionales de salud. El campo `autor` es solo un campo de texto. Los profesionales deben crearse manualmente mediante el endpoint `/api/profesionales`.

## Multi-Tenancy

El sistema soporta múltiples clínicas (tenants) en la misma instancia:

- Cada tenant tiene su propio esquema de base de datos
- Los documentos se filtran por `tenantId`
- El `tenantId` se obtiene del JWT o del header `X-Tenant-Id`

## Documentación Adicional

- [Documentación OpenAPI](docs/openapi-periferico.yaml) - Especificación completa de la API
- [Guía de Testing](README-TESTING.md) - Pruebas adicionales
- [Guía de Compilación y Despliegue](../../GUIA-COMPILACION-DESPLIEGUE.md) - Instrucciones detalladas

## Estructura del Proyecto

```
clinica_backend/
├── ejb/                    # EJB module (business logic)
│   └── src/main/java/
│       └── uy/edu/tse/hcen/
│           ├── config/     # MongoDB configuration
│           ├── multitenancy/ # Multi-tenancy support
│           ├── repository/ # Data access layer
│           ├── service/    # Business services
│           └── utils/      # Utilities (JWT, etc.)
├── web/                    # Web module (REST API)
│   └── src/main/java/
│       └── uy/edu/tse/hcen/
│           ├── rest/       # REST endpoints
│           └── rest/filter/ # JWT authentication filter
├── ear/                    # Enterprise archive packaging
└── docker-compose.yml      # MongoDB container config
```
