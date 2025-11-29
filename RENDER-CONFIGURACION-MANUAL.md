# ⚙️ Configuración Manual en Render (Si render.yaml falla)

Si tienes problemas con el Blueprint (`render.yaml`), puedes configurar todo manualmente. Sigue estos pasos:

## 🔧 Backend - Configuración Manual

### 1. Crear Web Service

1. **Render Dashboard** → "New" → "Web Service"
2. Conecta tu repositorio GitLab
3. Selecciona branch: `feature/deploy`

### 2. Configuración Básica

```
Name: hcen-periferico-backend
Environment: Docker
```

### 3. Configuración de Build

**Root Directory:** (deja vacío - Render usará el root del repo)

**Build Command:** (deja vacío - Docker lo maneja)

**Start Command:** (deja vacío - Docker lo maneja)

### 4. Configuración de Docker

**Dockerfile Path:** `componente-periferico/clinica_backend/wildfly/Dockerfile.render`

**Docker Context:** `componente-periferico/clinica_backend`

**⚠️ IMPORTANTE:** 
- El Dockerfile Path es relativo al **root del repositorio**
- El Docker Context es el directorio desde donde Docker ejecutará los comandos COPY

### 5. Variables de Entorno

Agrega estas variables en la sección "Environment":

```
JWT_SECRET_BASE64=bXlzdXBlcnNlY3JldGtleWZvcmhjZW5qd3R0b2tlbnNzaG91bGRiZWxvbmdlcg==

MONGODB_URI=<tu-connection-string-de-mongodb-atlas>
MONGODB_DB=hcen_db

NODO_BASE_URL=https://hcen-periferico-backend.onrender.com
```

### 6. Vincular PostgreSQL

1. En la sección "Link Resource"
2. Selecciona tu base de datos PostgreSQL
3. Render automáticamente agregará `DATABASE_URL`

### 7. Health Check

```
Health Check Path: /hcen-web/api/config/health
```

### 8. Crear y Desplegar

Click "Create Web Service" y espera a que compile (~10-15 minutos la primera vez)

---

## 🎨 Frontend - Configuración Manual

### 1. Crear Static Site

1. **Render Dashboard** → "New" → "Static Site"
2. Conecta tu repositorio GitLab
3. Selecciona branch: `feature/deploy`

### 2. Configuración

```
Name: hcen-periferico-frontend

Build Command: 
cd componente-periferico/frontend && npm ci && npm run build

Publish Directory: 
componente-periferico/frontend/build
```

### 3. Variables de Entorno

```
REACT_APP_BACKEND_URL=https://hcen-periferico-backend.onrender.com/hcen-web
```

(Usa la URL real de tu backend después del despliegue)

### 4. Crear y Desplegar

Click "Create Static Site"

---

## 🔍 Verificación

Después del despliegue:

1. **Backend Health Check:**
   ```bash
   curl https://tu-backend.onrender.com/hcen-web/api/config/health
   ```

2. **Frontend:**
   - Abre la URL del frontend en el navegador
   - Debe cargar sin errores

3. **Logs:**
   - Revisa los logs en Render Dashboard
   - Busca errores de conexión a MongoDB o PostgreSQL

---

## 🐛 Troubleshooting

### Error: "failed to read dockerfile"

**Solución:** Verifica que:
- `Dockerfile Path` sea: `componente-periferico/clinica_backend/wildfly/Dockerfile.render`
- El archivo existe en esa ubicación en el repositorio
- El branch `feature/deploy` tiene el archivo

### Error: "COPY failed: file not found"

**Solución:** El Docker Context debe ser: `componente-periferico/clinica_backend`

### Build muy lento

**Solución:** Normal la primera vez (~10-15 minutos). Builds siguientes serán más rápidos gracias al caché.

### Backend no inicia

**Revisa:**
- Variables de entorno configuradas correctamente
- MongoDB Atlas permite conexiones (Network Access)
- PostgreSQL está vinculado al servicio

---

## 📝 Resumen de Rutas

- **Repositorio:** `componente-periferico`
- **Dockerfile:** `componente-periferico/clinica_backend/wildfly/Dockerfile.render`
- **Docker Context:** `componente-periferico/clinica_backend`
- **Frontend Build:** `componente-periferico/frontend`
- **Frontend Output:** `componente-periferico/frontend/build`

