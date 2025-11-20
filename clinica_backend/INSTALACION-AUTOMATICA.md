# ✅ Instalación Completamente Automatizada

## 🎯 Resumen: ¿Qué se automatizó?

### 1️⃣ **Tablas de Base de Datos**
✅ **Se crean automáticamente al iniciar WildFly** (via `@Startup` en `DatabaseInitializer.java`):
- `public.nodoperiferico`
- `public.usuario`
- `public.usuarioperiferico`
- `public.administradorclinica`

✅ **Se crean automáticamente al activar cada clínica** (via `TenantAdminService.createTenantSchema()`):
- `schema_clinica_XXX.usuarioperiferico` (con **auto-increment** y **todas las columnas** de herencia SINGLE_TABLE)
- `schema_clinica_XXX.usuario`
- `schema_clinica_XXX.portal_configuracion`
- `schema_clinica_XXX.profesionalsalud`
- `schema_clinica_XXX.administradorclinica`
- `schema_clinica_XXX.nodoperiferico`

### 2️⃣ **Módulos de WildFly (JWT, Spring Security, MongoDB)**
✅ **Se configuran UNA SOLA VEZ** ejecutando `./setup-modules.sh` después del primer `mvn package`
✅ **Persisten entre reinicios de Docker** gracias a volúmenes montados desde `wildfly-modules/`
✅ **No requieren configuración manual** ni copias manuales

### 3️⃣ **Multi-tenancy**
✅ **Administradores** se guardan en `public.usuarioperiferico` (accesibles globalmente)
✅ **Profesionales de Salud** se guardan en `schema_clinica_XXX.usuarioperiferico` (aislados por tenant)
✅ **Login dual**: busca primero en public, luego en el schema del tenant especificado
✅ **IDs auto-generados**: tanto en public como en cada schema de tenant

---

## 📋 **Instrucciones para NUEVA INSTALACIÓN**

### Paso 1: Clonar y compilar
```bash
git clone <repo-url>
cd componente-periferico/clinica_backend
mvn clean package
```

### Paso 2: Configurar módulos (UNA SOLA VEZ)
```bash
chmod +x setup-modules.sh
./setup-modules.sh
```

### Paso 3: Iniciar servicios
```bash
docker-compose up -d
```

### Paso 4: Esperar ~90 segundos
```bash
# Opcional: seguir logs
docker logs -f hcen-wildfly-app
```

### Paso 5: Verificar funcionamiento
```bash
curl http://localhost:8081/hcen-web/api/config/clinic/1
```

---

## ✨ **Lo que NO necesitas hacer manualmente**

❌ Crear tablas en PostgreSQL  
❌ Copiar JARs a WildFly manualmente  
❌ Configurar `module.xml` manualmente  
❌ Crear esquemas de tenants  
❌ Configurar secuencias de auto-increment  
❌ Preocuparte por persistencia de módulos  

---

## 🧪 **Prueba End-to-End (desde BD vacía)**

```bash
# 1. Crear clínica
curl -X POST http://localhost:8081/hcen-web/api/config/activate \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "999",
    "token": "test",
    "username": "admin_c999",
    "password": "Admin123!",
    "rut": "999999999999",
    "departamento": "MONTEVIDEO",
    "localidad": "Mvd",
    "direccion": "Test 999",
    "telefono": "099999"
  }'

# 2. Login como admin
curl -X POST http://localhost:8081/hcen-web/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nickname":"admin_c999","password":"Admin123!","tenantId":"999"}'

# 3. Crear profesional (usando el JWT del paso 2)
TOKEN="<jwt-del-paso-2>"
curl -X POST http://localhost:8081/hcen-web/api/profesionales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nickname": "dr_test",
    "nombre": "Dr. Test",
    "email": "test@c999.com",
    "password": "Doctor123!",
    "especialidad": "MEDICINA_GENERAL"
  }'

# 4. Login como profesional
curl -X POST http://localhost:8081/hcen-web/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nickname":"dr_test","password":"Doctor123!","tenantId":"999"}'
```

✅ **Si todos estos pasos funcionan, la instalación es correcta**

---

## 🔄 **Actualizaciones Futuras**

### Si cambias código Java:
```bash
mvn clean package
docker-compose restart
```

### Si cambias dependencias:
```bash
mvn clean package
./setup-modules.sh  # Solo si cambiaste librerías de JWT/Spring/MongoDB
docker-compose restart
```

### Para empezar completamente de cero:
```bash
docker-compose down -v  # Elimina BD
rm -rf wildfly-modules/  # Elimina módulos locales
mvn clean package
./setup-modules.sh
docker-compose up -d
```

---

## 🎓 **Arquitectura Implementada**

```
┌──────────────────────────────────────────────────────┐
│                 INSTALACIÓN NUEVA                     │
└────────────────┬─────────────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────────────┐
│  1. mvn package → Compila EJB/WAR/EAR               │
│  2. setup-modules.sh → Copia JARs a wildfly-modules/ │
│  3. docker-compose up → Monta wildfly-modules/       │
└────────────────┬─────────────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────────────┐
│         INICIO DE WILDFLY (@Startup)                 │
│  • DatabaseInitializer.init() ejecuta                │
│  • Crea tablas public.* si no existen                │
└────────────────┬─────────────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────────────┐
│        ACTIVACIÓN DE CLÍNICA (por HTTP)              │
│  • TenantAdminService.createTenantSchema()           │
│  • Crea schema_clinica_XXX con todas sus tablas      │
│  • Crea admin en public.usuarioperiferico            │
└────────────────┬─────────────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────────────┐
│              SISTEMA OPERATIVO                        │
│  • Admins pueden crear profesionales                 │
│  • Profesionales se guardan en schema del tenant     │
│  • Login multi-tenant funciona correctamente         │
└──────────────────────────────────────────────────────┘
```

---

## ❓ **Preguntas Frecuentes**

### ¿Tengo que ejecutar setup-modules.sh cada vez?
**No**, solo la primera vez. Los módulos persisten en `wildfly-modules/` y se montan automáticamente.

### ¿Qué pasa si hago `mvn clean`?
`mvn clean` **NO** elimina `wildfly-modules/`. Solo elimina `target/`. Los módulos siguen funcionando.

### ¿Puedo cambiar los puertos?
Sí, edita `docker-compose.yml`:
- Puerto externo PostgreSQL: `5433:5432`
- Puerto externo WildFly: `8081:8080`

### ¿Cómo accedo a la BD?
```bash
docker exec -it periferico-postgres-db psql -U postgres -d hcen_db
```

### ¿Los módulos están en .gitignore?
No necesariamente. Puedes incluir `wildfly-modules/` en el repo para que otros clonen y ejecuten directamente sin `setup-modules.sh`.

---

## 🏆 **Resultado Final**

✅ **Cualquier persona** puede clonar el repo  
✅ Ejecutar 3 comandos (`mvn package`, `setup-modules.sh`, `docker-compose up`)  
✅ **Sin configuración manual de nada**  
✅ **Todas las tablas se crean automáticamente**  
✅ **Sistema 100% funcional en ~90 segundos**  

---

**¡Éxito! 🎉**

