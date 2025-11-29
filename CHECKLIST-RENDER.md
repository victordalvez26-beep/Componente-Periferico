# ✅ Checklist de Despliegue en Render

Usa este checklist para asegurarte de completar todos los pasos:

## 📋 Pre-despliegue

- [ ] Cuenta creada en MongoDB Atlas
- [ ] Cluster MongoDB creado (Free tier M0)
- [ ] Usuario de base de datos creado en MongoDB Atlas
- [ ] Network Access configurado (0.0.0.0/0)
- [ ] Connection string de MongoDB copiada y guardada
- [ ] Cuenta creada en Render

## 🐘 PostgreSQL en Render

- [ ] Servicio PostgreSQL creado
- [ ] Nombre: `hcen-periferico-db`
- [ ] Database: `hcen_db`
- [ ] User: `hcen_user`
- [ ] Connection string disponible (DATABASE_URL)

## 🔧 Backend en Render

- [ ] Web Service creado
- [ ] Repositorio conectado (GitLab)
- [ ] Branch: `feature/deploy`
- [ ] Dockerfile: `clinica_backend/wildfly/Dockerfile.render`
- [ ] Root Directory: `componente-periferico/clinica_backend`
- [ ] Build Command: `mvn clean package -DskipTests`
- [ ] PostgreSQL vinculado como resource
- [ ] Variables de entorno configuradas:
  - [ ] `JWT_SECRET_BASE64`
  - [ ] `MONGODB_URI` (de MongoDB Atlas)
  - [ ] `MONGODB_DB=hcen_db`
  - [ ] `NODO_BASE_URL` (después del primer deploy)
- [ ] Build exitoso
- [ ] Logs muestran "WildFly iniciado correctamente"
- [ ] Health check funciona: `/hcen-web/api/config/health`

## 🎨 Frontend en Render

- [ ] Static Site creado
- [ ] Repositorio conectado (GitLab)
- [ ] Branch: `feature/deploy`
- [ ] Build Command: `cd componente-periferico/frontend && npm ci && npm run build`
- [ ] Publish Directory: `componente-periferico/frontend/build`
- [ ] Variable de entorno configurada:
  - [ ] `REACT_APP_BACKEND_URL` (URL del backend en Render)
- [ ] Build exitoso
- [ ] Frontend accesible en navegador
- [ ] Sin errores CORS en DevTools

## ✅ Post-despliegue

- [ ] `NODO_BASE_URL` actualizada con URL real del backend
- [ ] Backend reiniciado después de actualizar `NODO_BASE_URL`
- [ ] Frontend conecta correctamente al backend
- [ ] Login funciona desde el frontend
- [ ] Endpoints de API responden correctamente

## 🐛 Troubleshooting

Si algo falla:

- [ ] Revisar logs del backend en Render
- [ ] Verificar que todas las variables de entorno están configuradas
- [ ] Verificar que MongoDB Atlas permite conexiones (Network Access)
- [ ] Verificar que PostgreSQL está vinculado al backend
- [ ] Probar health check endpoint
- [ ] Revisar errores en DevTools del navegador (CORS, etc.)

---

**Estado:** ⬜ No iniciado | 🟡 En progreso | ✅ Completado

