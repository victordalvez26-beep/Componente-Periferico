# 📋 Plan de Despliegue en Render - Componente Periférico

## 🎯 Resumen Ejecutivo

Este documento describe el plan para desplegar el componente periférico (frontend React + backend Java/WildFly) en Render.

---

## 📦 Componentes a Desplegar

### 1. Frontend React (`frontend/`)
- **Tipo en Render:** Static Site o Web Service (Node.js)
- **Tecnología:** React 18.2.0 con `react-scripts`
- **Puerto por defecto:** 3001 (desarrollo)
- **Variables de entorno necesarias:**
  - `REACT_APP_BACKEND_URL` - URL del backend en Render

### 2. Backend Java/WildFly (`clinica_backend/`)
- **Tipo en Render:** Web Service (Docker)
- **Tecnología:** WildFly 37.0.0.Final con JDK 17
- **Puerto:** 8080 (localmente), dinámico en Render
- **Dependencias:**
  - PostgreSQL (Render PostgreSQL)
  - MongoDB (MongoDB Atlas o externo)

### 3. Bases de Datos
- **PostgreSQL:** Render PostgreSQL (managed)
- **MongoDB:** MongoDB Atlas o servicio externo (Render no ofrece MongoDB managed)

---

## 🔧 Cambios Necesarios

### Backend

#### 1. Dockerfile para Render
- ❌ **Actual:** Monta el `.ear` como volumen
- ✅ **Necesario:** Incluir el `.ear` compilado dentro de la imagen Docker
- ✅ **Necesario:** Configurar WildFly para usar `$PORT` de Render

#### 2. Variables de Entorno
Necesitas configurar en Render:
```
JWT_SECRET_BASE64=bXlzdXBlcnNlY3JldGtleWZvcmhjZW5qd3R0b2tlbnNzaG91bGRiZWxvbmdlcg==
MONGO_HOST=<host-mongodb-atlas>
MONGO_PORT=27017
MONGO_USER=<user-mongodb>
MONGO_PASSWORD=<password-mongodb>
MONGODB_URI=mongodb://user:password@host:27017/hcen_db?authSource=admin
MONGODB_DB=hcen_db
NODO_BASE_URL=https://tu-backend.onrender.com
DATASOURCE_URL=<postgres-connection-string-de-render>
DATASOURCE_USER=postgres
DATASOURCE_PASSWORD=<password-postgres-render>
```

#### 3. Configuración de Puerto
- WildFly debe escuchar en `0.0.0.0:$PORT` (Render asigna puertos dinámicamente)

#### 4. Compilación del EAR
- El `.ear` debe compilarse ANTES de construir la imagen Docker
- Necesitarás un Dockerfile multi-stage o un build script

### Frontend

#### 1. Build Configuration
- **Build Command:** `cd frontend && npm ci && npm run build`
- **Publish Directory:** `frontend/build`

#### 2. Variables de Entorno
```
REACT_APP_BACKEND_URL=https://tu-backend.onrender.com/hcen-web
```

---

## 📝 Pasos de Despliegue

### Paso 1: Preparar MongoDB
1. Crear cuenta en [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
2. Crear cluster gratuito
3. Crear usuario y obtener connection string
4. Configurar IP whitelist (0.0.0.0/0 para Render)

### Paso 2: Crear PostgreSQL en Render
1. Dashboard de Render → New → PostgreSQL
2. Nombre: `hcen-periferico-db`
3. Plan: Free tier (si aplica)
4. Copiar connection string interna

### Paso 3: Crear Backend Service en Render
1. Dashboard de Render → New → Web Service
2. Conectar repositorio Git
3. **Configuración:**
   - **Name:** `hcen-periferico-backend`
   - **Root Directory:** `componente-periferico/clinica_backend`
   - **Environment:** Docker
   - **Dockerfile Path:** `wildfly/Dockerfile.render` (a crear)
   - **Build Command:** `mvn clean package` (antes de Docker build)
   - **Start Command:** (manejado por Dockerfile)
   - **Port:** 8080 (pero Render lo sobrescribe con `$PORT`)

### Paso 4: Crear Frontend Service en Render
1. Dashboard de Render → New → Static Site o Web Service
2. **Opción A: Static Site (Recomendado)**
   - **Build Command:** `cd frontend && npm ci && npm run build`
   - **Publish Directory:** `frontend/build`
   - **Variables de entorno:** `REACT_APP_BACKEND_URL`

3. **Opción B: Web Service (Node.js)**
   - **Build Command:** `cd frontend && npm ci && npm run build`
   - **Start Command:** `cd frontend && npx serve -s build -l $PORT`
   - **Variables de entorno:** `REACT_APP_BACKEND_URL`

---

## 🚨 Consideraciones Importantes

### 1. **Tiempo de Inicio**
- Render apaga servicios inactivos después de 15 minutos
- El primer request puede tardar 30-60 segundos (cold start)
- Considera usar un ping periódico para mantener el servicio activo

### 2. **Límites del Free Tier**
- **Web Services:** Se duermen después de 15 min de inactividad
- **PostgreSQL:** 90 días de free tier, luego pago
- **Ancho de banda:** Limitado

### 3. **CORS**
- El backend debe permitir el origen del frontend en Render
- Verificar que los filtros CORS incluyan `*.onrender.com`

### 4. **Logs**
- Render ofrece logs en tiempo real
- Útil para debug del backend (WildFly logs)

### 5. **Variables de Entorno**
- **Backend:** Se configuran en el dashboard del Web Service
- **Frontend:** Se configuran en el dashboard del Static Site/Web Service
- **Sensibles:** Usar Render Secrets para passwords

---

## 📋 Checklist de Preparación

### Antes de Desplegar
- [ ] MongoDB Atlas configurado y connection string listo
- [ ] PostgreSQL creado en Render y connection string listo
- [ ] Dockerfile para Render creado (incluye `.ear` compilado)
- [ ] WildFly configurado para usar `$PORT` de Render
- [ ] Variables de entorno documentadas
- [ ] CORS configurado para permitir `*.onrender.com`
- [ ] Scripts de build preparados

### Durante el Despliegue
- [ ] Backend desplegado correctamente
- [ ] Frontend desplegado correctamente
- [ ] Variables de entorno configuradas
- [ ] Health checks funcionando

### Después del Despliegue
- [ ] Probar endpoints del backend
- [ ] Probar login desde el frontend
- [ ] Verificar conexión a PostgreSQL
- [ ] Verificar conexión a MongoDB
- [ ] Revisar logs para errores

---

## 🔗 URLs Esperadas

Después del despliegue, tendrás URLs como:
- **Frontend:** `https://hcen-periferico-frontend.onrender.com`
- **Backend:** `https://hcen-periferico-backend.onrender.com`
- **PostgreSQL:** (conexión interna, no pública)

---

## 📚 Recursos Adicionales

- [Render Documentation](https://render.com/docs)
- [MongoDB Atlas Free Tier](https://www.mongodb.com/cloud/atlas)
- [WildFly Documentation](https://docs.wildfly.org/)

---

## ⚠️ Notas Finales

1. **El backend actual usa Docker Compose localmente.** En Render, cada servicio es independiente.
2. **MongoDB debe ser externo.** Render no ofrece MongoDB managed.
3. **El `.ear` debe compilarse en el build.** No se puede montar como volumen.
4. **Render asigna puertos dinámicamente.** WildFly debe usar `$PORT`.

