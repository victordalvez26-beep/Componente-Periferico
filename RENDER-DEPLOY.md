# 🚀 Guía de Despliegue en Render - Componente Periférico

Esta guía te llevará paso a paso para desplegar el componente periférico completo (backend + frontend) en Render.

---

## 📋 Prerequisitos

1. **Cuenta en Render:** [https://render.com](https://render.com) (cuenta gratuita funciona)
2. **MongoDB Atlas:** [https://www.mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas) (cuenta gratuita)
3. **Repositorio Git:** Tu código debe estar en GitHub, GitLab o Bitbucket

---

## 🗄️ Paso 1: Configurar MongoDB Atlas

1. **Crear cuenta en MongoDB Atlas:**
   - Ve a [MongoDB Atlas](https://www.mongodb.com/cloud/atlas)
   - Crea una cuenta gratuita

2. **Crear un Cluster:**
   - Click en "Build a Database"
   - Selecciona el plan **FREE (M0)**
   - Elige una región (preferiblemente la misma que usarás en Render)
   - Click "Create"

3. **Configurar seguridad:**
   - **Database Access:** Crea un usuario y contraseña (guarda estos datos)
   - **Network Access:** Agrega `0.0.0.0/0` para permitir conexiones desde cualquier IP (o solo las IPs de Render)

4. **Obtener Connection String:**
   - Click en "Connect" → "Connect your application"
   - Copia la connection string, será algo como:
     ```
     mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
     ```
   - Reemplaza `<password>` con tu contraseña real
   - Añade el nombre de la base de datos:
     ```
     mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/hcen_db?retryWrites=true&w=majority
     ```

✅ **Guarda esta connection string**, la necesitarás en el Paso 3.

---

## 🐘 Paso 2: Crear PostgreSQL en Render

1. **En Render Dashboard:**
   - Click "New" → "PostgreSQL"
   - **Name:** `hcen-periferico-db`
   - **Database:** `hcen_db`
   - **User:** `hcen_user`
   - **Region:** Elige la región más cercana (ej: Oregon)
   - **Plan:** Free (o Starter si prefieres)

2. **Espera a que se cree la base de datos** (~2 minutos)

3. **Obtener Connection String:**
   - Una vez creada, Render mostrará `DATABASE_URL`
   - Esta variable estará disponible automáticamente en tu servicio web

✅ **No necesitas copiar nada manualmente**, Render lo hace automáticamente.

---

## 🔧 Paso 3: Desplegar Backend en Render

### Opción A: Usar render.yaml (Recomendado)

1. **Conectar repositorio:**
   - En Render Dashboard → "New" → "Blueprint"
   - Conecta tu repositorio Git
   - Selecciona la branch `feature/deploy`
   - Render detectará automáticamente el archivo `render.yaml`

2. **Configurar MongoDB:**
   - En el servicio `hcen-periferico-backend`, ve a "Environment"
   - Agrega las siguientes variables:
     ```
     MONGODB_URI=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/hcen_db?retryWrites=true&w=majority
     MONGODB_DB=hcen_db
     ```
     (Reemplaza con tu connection string real de MongoDB Atlas)

3. **Configurar NODO_BASE_URL:**
   - Después del primer despliegue, vuelve a "Environment"
   - Actualiza `NODO_BASE_URL` con la URL del backend:
     ```
     NODO_BASE_URL=https://hcen-periferico-backend.onrender.com
     ```

4. **Verificar PostgreSQL:**
   - Render agregará automáticamente `DATABASE_URL` al servicio web
   - No necesitas configurarla manualmente

### Opción B: Manual (Sin render.yaml)

1. **Crear Web Service:**
   - Render Dashboard → "New" → "Web Service"
   - Conecta tu repositorio
   - Selecciona branch `feature/deploy`

2. **Configuración del servicio:**
   - **Name:** `hcen-periferico-backend`
   - **Environment:** `Docker`
   - **Dockerfile Path:** `componente-periferico/clinica_backend/wildfly/Dockerfile.render`
   - **Root Directory:** `componente-periferico/clinica_backend`
   - **Build Command:** `mvn clean package -DskipTests`
   - **Start Command:** (manejado por Dockerfile)
   - **Plan:** Free o Starter

3. **Variables de entorno:**
   Agrega todas estas variables en "Environment":
   ```
   JWT_SECRET_BASE64=bXlzdXBlcnNlY3JldGtleWZvcmhjZW5qd3R0b2tlbnNzaG91bGRiZWxvbmdlcg==
   
   MONGODB_URI=mongodb+srv://username:password@cluster0.xxxxx.mongodb.net/hcen_db?retryWrites=true&w=majority
   MONGODB_DB=hcen_db
   
   NODO_BASE_URL=https://hcen-periferico-backend.onrender.com
   ```
   
   **PostgreSQL:**
   - En la sección "Environment", Render automáticamente agregará `DATABASE_URL` si vinculaste el servicio PostgreSQL
   - Si no, ve a "Link Resource" y vincula la base de datos PostgreSQL

4. **Deploy:**
   - Click "Create Web Service"
   - Espera a que compile (~5-10 minutos la primera vez)
   - El servicio estará disponible en: `https://hcen-periferico-backend.onrender.com`

✅ **Verifica los logs** para asegurarte de que el despliegue fue exitoso.

---

## 🎨 Paso 4: Desplegar Frontend en Render

1. **Crear Static Site:**
   - Render Dashboard → "New" → "Static Site"
   - Conecta tu repositorio
   - Selecciona branch `feature/deploy`

2. **Configuración:**
   - **Name:** `hcen-periferico-frontend`
   - **Build Command:** `cd componente-periferico/frontend && npm ci && npm run build`
   - **Publish Directory:** `componente-periferico/frontend/build`
   - **Root Directory:** `componente-periferico/frontend` (opcional)

3. **Variables de entorno:**
   - Agrega en "Environment":
     ```
     REACT_APP_BACKEND_URL=https://hcen-periferico-backend.onrender.com/hcen-web
     ```
     (Reemplaza con la URL real de tu backend)

4. **Deploy:**
   - Click "Create Static Site"
   - Espera a que compile (~2-3 minutos)
   - El sitio estará disponible en: `https://hcen-periferico-frontend.onrender.com`

✅ **Frontend desplegado!**

---

## ✅ Paso 5: Verificar el Despliegue

### Verificar Backend:

1. **Health Check:**
   ```bash
   curl https://hcen-periferico-backend.onrender.com/hcen-web/api/config/health
   ```
   Debería responder con un JSON.

2. **Verificar logs:**
   - En Render Dashboard → `hcen-periferico-backend` → "Logs"
   - Debe mostrar "WildFly iniciado correctamente"

### Verificar Frontend:

1. **Abrir en navegador:**
   - Ve a `https://hcen-periferico-frontend.onrender.com`
   - Debe cargar la aplicación React

2. **Verificar conexión:**
   - Abre DevTools (F12) → Network
   - Debe hacer requests a tu backend sin errores CORS

---

## 🔄 Paso 6: Actualizar URLs después del Primer Deploy

Después del primer despliegue, necesitas actualizar estas variables:

1. **Backend → NODO_BASE_URL:**
   - Ve a `hcen-periferico-backend` → Environment
   - Actualiza `NODO_BASE_URL` con la URL real:
     ```
     NODO_BASE_URL=https://hcen-periferico-backend.onrender.com
     ```
   - Guarda y reinicia el servicio

2. **Frontend → REACT_APP_BACKEND_URL:**
   - Ve a `hcen-periferico-frontend` → Environment
   - Verifica que `REACT_APP_BACKEND_URL` sea correcta:
     ```
     REACT_APP_BACKEND_URL=https://hcen-periferico-backend.onrender.com/hcen-web
     ```
   - Guarda y vuelve a hacer deploy

---

## ⚠️ Consideraciones Importantes

### Free Tier de Render:

1. **Web Services se duermen:**
   - Los servicios gratuitos se duermen después de 15 minutos de inactividad
   - El primer request puede tardar 30-60 segundos (cold start)
   - Solución: Usar un ping periódico o upgrade a Starter plan

2. **PostgreSQL Free Tier:**
   - Válido por 90 días
   - Después requiere upgrade a plan de pago

### MongoDB Atlas:

- El plan gratuito tiene límites, pero suficiente para desarrollo/testing
- Para producción, considera upgrade

### CORS:

- El backend ya está configurado para permitir dominios de Render (`.onrender.com`)
- Si usas un dominio personalizado, actualiza el filtro CORS

---

## 🐛 Troubleshooting

### Backend no inicia:

1. **Revisar logs:**
   - Render Dashboard → Service → Logs
   - Busca errores de conexión a MongoDB o PostgreSQL

2. **Verificar variables de entorno:**
   - Asegúrate de que `MONGODB_URI` y `DATABASE_URL` están configuradas
   - Verifica que `NODO_BASE_URL` apunta a la URL correcta

3. **Verificar puerto:**
   - Render usa `PORT` automáticamente
   - El entrypoint ya está configurado para usar esta variable

### Frontend no se conecta al backend:

1. **Verificar CORS:**
   - Abre DevTools → Console
   - Busca errores de CORS
   - Verifica que `REACT_APP_BACKEND_URL` está configurada correctamente

2. **Verificar URL del backend:**
   - Asegúrate de que la URL incluye `/hcen-web`
   - Ejemplo: `https://backend.onrender.com/hcen-web`

### MongoDB no conecta:

1. **Verificar Network Access:**
   - MongoDB Atlas → Network Access
   - Asegúrate de que `0.0.0.0/0` está permitido

2. **Verificar connection string:**
   - Debe incluir el nombre de la base de datos
   - Ejemplo: `mongodb+srv://user:pass@cluster.mongodb.net/hcen_db?retryWrites=true&w=majority`

---

## 📚 Recursos Adicionales

- [Render Documentation](https://render.com/docs)
- [MongoDB Atlas Documentation](https://docs.atlas.mongodb.com/)
- [WildFly Documentation](https://docs.wildfly.org/)

---

## 🎉 ¡Despliegue Completo!

Una vez completados todos los pasos, tu aplicación estará disponible en:

- **Frontend:** `https://hcen-periferico-frontend.onrender.com`
- **Backend:** `https://hcen-periferico-backend.onrender.com`
- **API Base:** `https://hcen-periferico-backend.onrender.com/hcen-web`

¡Felicitaciones! 🚀

