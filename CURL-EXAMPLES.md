# 🔧 Ejemplos de curl para Probar Endpoints

## 📋 Endpoints Públicos (Sin autenticación)

### 1. Health Check
```bash
curl http://localhost:8081/hcen-web/api/config/health
```

**Respuesta esperada:**
```json
{"status":"ok","message":"Servicio operativo"}
```

---

### 2. Obtener Configuración de una Clínica
```bash
curl http://localhost:8081/hcen-web/api/config/1
```

**Respuesta esperada:**
```json
{
  "id": "1",
  "nombre": "Clínica Ejemplo",
  ...
}
```

---

### 3. Login (Obtener JWT)
```bash
curl -X POST http://localhost:8081/hcen-web/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'
```

**Respuesta esperada:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tenantId": "1",
  "role": "ADMINISTRADOR"
}
```

---

### 4. Configuración Pública del Portal
```bash
curl http://localhost:8081/hcen-web/api/portal-configuracion/public
```

**Respuesta esperada:**
```json
{
  "nombrePortal": "Portal Clínica",
  "colorPrimario": "#3b82f6",
  "colorSecundario": "#6b7280",
  "logoUrl": ""
}
```

---

## 🔐 Endpoints con Autenticación (Requieren JWT)

**Nota:** Primero obtén un token con el endpoint de login, luego úsalo en el header `Authorization: Bearer <token>`

### 5. Listar Profesionales
```bash
# Primero obtén el token con login
TOKEN="tu-token-aqui"

curl http://localhost:8081/hcen-web/api/profesionales \
  -H "Authorization: Bearer $TOKEN"
```

---

### 6. Listar Usuarios de Salud (Pacientes)
```bash
TOKEN="tu-token-aqui"
TENANT_ID="1"

curl http://localhost:8081/hcen-web/api/clinica/$TENANT_ID/usuarios-salud \
  -H "Authorization: Bearer $TOKEN"
```

---

### 7. Crear Usuario de Salud (Paciente)
```bash
TOKEN="tu-token-aqui"
TENANT_ID="1"

curl -X POST http://localhost:8081/hcen-web/api/clinica/$TENANT_ID/usuarios-salud \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ci": "12345678",
    "primerNombre": "Juan",
    "primerApellido": "Pérez",
    "fechaNacimiento": "1990-01-01",
    "sexo": "M"
  }'
```

---

### 8. Listar Documentos de un Paciente
```bash
TOKEN="tu-token-aqui"
CI="12345678"

curl http://localhost:8081/hcen-web/api/documentos-pdf/paciente/$CI \
  -H "Authorization: Bearer $TOKEN"
```

---

### 9. Obtener Estadísticas de una Clínica
```bash
TOKEN="tu-token-aqui"
TENANT_ID="1"

curl http://localhost:8081/hcen-web/api/stats/$TENANT_ID \
  -H "Authorization: Bearer $TOKEN"
```

---

## 🧪 Script Completo de Prueba

```bash
#!/bin/bash

BASE_URL="http://localhost:8081/hcen-web"
TENANT_ID="1"

echo "1. Health Check..."
curl -s "$BASE_URL/api/config/health" | jq '.'

echo -e "\n2. Login..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }')

echo "$LOGIN_RESPONSE" | jq '.'

# Extraer token (requiere jq instalado)
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')

if [ "$TOKEN" != "null" ] && [ -n "$TOKEN" ]; then
  echo -e "\n3. Listar Profesionales..."
  curl -s "$BASE_URL/api/profesionales" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
  
  echo -e "\n4. Listar Usuarios de Salud..."
  curl -s "$BASE_URL/api/clinica/$TENANT_ID/usuarios-salud" \
    -H "Authorization: Bearer $TOKEN" | jq '.'
  
  echo -e "\n5. Configuración del Portal..."
  curl -s "$BASE_URL/api/portal-configuracion/public" | jq '.'
else
  echo "Error: No se pudo obtener el token"
fi
```

---

## 🌐 Para Render (Producción)

Si el backend está desplegado en Render, cambia la URL base:

```bash
# Reemplaza con tu URL real de Render
BASE_URL="https://hcen-periferico-backend.onrender.com/hcen-web"

curl "$BASE_URL/api/config/health"
```

---

## 📝 Notas

- **Puerto local:** `8081` (según docker-compose.yml)
- **Context path:** `/hcen-web`
- **API base:** `/api`
- **Formato:** Todos los endpoints retornan JSON

### Requisitos para endpoints protegidos:
- Obtener token con `/api/auth/login`
- Incluir header: `Authorization: Bearer <token>`
- El token contiene el `tenantId` automáticamente

---

## 🐛 Troubleshooting

### Error 401 Unauthorized
- Verifica que el token esté incluido en el header
- El token puede haber expirado, obtén uno nuevo

### Error 403 Forbidden
- Verifica que el usuario tenga los permisos necesarios (rol ADMINISTRADOR)

### Error 404 Not Found
- Verifica la URL base (`/hcen-web/api/...`)
- Verifica que el backend esté corriendo

### Error CORS
- Los endpoints públicos deberían funcionar sin problemas
- Para requests desde el navegador, el frontend debe estar configurado

