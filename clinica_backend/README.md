# Componente Periférico Multi-tenant - Backend

Sistema multi-tenant para gestión de clínicas con infraestructura automatizada.

## 🚀 Instalación y Despliegue (Primera Vez)

### Prerrequisitos
- Docker y Docker Compose instalados
- Maven 3.6+
- Java 21+

### Pasos de Instalación

#### 1. Clonar el repositorio
```bash
git clone <repo-url>
cd componente-periferico/clinica_backend
```

#### 2. Compilar el proyecto
```bash
mvn clean package
```

#### 3. Configurar módulos de WildFly (UNA SOLA VEZ)
```bash
chmod +x setup-modules.sh
./setup-modules.sh
```

Este script:
- Crea el directorio `wildfly-modules/` con librerías de JWT, Spring Security y MongoDB
- **Solo se ejecuta UNA VEZ**; los módulos persisten entre reinicios

#### 4. Iniciar servicios
```bash
docker-compose up -d
```

Esperar ~90 segundos para que WildFly complete el despliegue.

#### 5. Verificar que está funcionando
```bash
curl http://localhost:8081/hcen-web/api/config/clinic/1
```

Si responde con JSON, ¡todo está funcionando! 🎉

---

## 🗄️ Tablas de Base de Datos

### Tablas Públicas (se crean automáticamente al iniciar WildFly)
- `public.nodoperiferico` - Clínicas registradas en el servidor
- `public.usuario` - Información básica de usuarios (ID auto-generado)
- `public.usuarioperiferico` - Autenticación de administradores globales
- `public.administradorclinica` - Vínculo admin-clínica

### Tablas de Tenant (se crean al activar una clínica)
Cada clínica tiene su propio esquema `schema_clinica_XXX` con:
- `usuarioperiferico` - Usuarios de la clínica (profesionales, admins)
- `usuario` - Información básica de usuarios
- `portal_configuracion` - Configuración personalizada (look & feel)
- `profesionalsalud` - Datos específicos de profesionales
- `administradorclinica` - Datos específicos de admins
- `nodoperiferico` - Referencia a la clínica

---

## 🧪 Crear una Clínica de Prueba

### Opción 1: Desde HCEN Backend (flujo completo)
1. Crear clínica desde el frontend de HCEN
2. El admin recibe email con link de activación
3. Admin completa el formulario de activación (RUT, dirección, username, password)

### Opción 2: Directamente (para testing)
```bash
curl -X POST http://localhost:8081/hcen-web/api/config/activate \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": "100",
    "token": "test-token",
    "username": "admin_c100",
    "password": "Admin123!",
    "rut": "100100100100",
    "departamento": "MONTEVIDEO",
    "localidad": "Montevideo",
    "direccion": "Av. Test 123",
    "telefono": "099100100"
  }'
```

### Verificar clínica creada
```bash
# Ver tablas creadas
docker exec periferico-postgres-db psql -U postgres -d hcen_db \
  -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'schema_clinica_100' ORDER BY table_name;"

# Login como admin de la clínica
curl -X POST http://localhost:8081/hcen-web/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nickname":"admin_c100","password":"Admin123!","tenantId":"100"}'
```

---

## 🏥 Crear Profesionales de Salud

```bash
# 1. Obtener JWT del admin (del login anterior)
TOKEN="<jwt-obtenido>"

# 2. Crear profesional
curl -X POST http://localhost:8081/hcen-web/api/profesionales \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nickname": "dr_juan",
    "nombre": "Dr. Juan Pérez",
    "email": "juan@clinica100.com",
    "password": "Doctor123!",
    "especialidad": "PEDIATRIA"
  }'
```

Los profesionales se guardan en `schema_clinica_100.usuarioperiferico`.

---

## 🔐 Login Multi-tenant

### Admin de Clínica (desde `public.usuarioperiferico`)
```bash
curl -X POST http://localhost:8081/hcen-web/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nickname":"admin_c100","password":"Admin123!","tenantId":"100"}'
```

### Profesional de Salud (desde `schema_clinica_100.usuarioperiferico`)
```bash
curl -X POST http://localhost:8081/hcen-web/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"nickname":"dr_juan","password":"Doctor123!","tenantId":"100"}'
```

---

## 🌐 Frontend Multi-tenant

El frontend se accede por path-based routing:
```
http://localhost:3001/portal/clinica/100/login
http://localhost:3001/portal/clinica/100/profesionales
```

---

## 🛠️ Comandos Útiles

### Ver logs de WildFly
```bash
docker logs -f hcen-wildfly-app
```

### Acceder a PostgreSQL
```bash
docker exec -it periferico-postgres-db psql -U postgres -d hcen_db
```

### Reiniciar servicios (mantiene BD)
```bash
docker-compose restart
```

### Reiniciar desde cero (borra BD)
```bash
docker-compose down -v
docker-compose up -d
```

---

## 📊 Arquitectura Multi-tenant

```
┌─────────────────────────────────────────┐
│          public (Schema Global)         │
│  • nodoperiferico (clínicas del server) │
│  • usuarioperiferico (admins globales)  │
│  • usuario (info básica)                │
└─────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        │           │           │
┌───────▼──────┐ ┌──▼──────┐ ┌─▼──────┐
│schema_clinica│ │schema_  │ │schema_ │
│     _100     │ │clinica_ │ │clinica_│
│              │ │  _200   │ │  _300  │
│• usuario     │ │         │ │        │
│• usuarioper  │ │  ...    │ │  ...   │
│• profesional │ │         │ │        │
└──────────────┘ └─────────┘ └────────┘
```

---

## ⚠️ Importante

1. **NO** ejecutar `setup-modules.sh` después de `mvn clean` sin recompilar primero
2. Los módulos en `wildfly-modules/` **NO** se eliminan con `mvn clean`
3. Para actualizar módulos: `mvn package && ./setup-modules.sh`
4. Los módulos persisten entre reinicios de Docker
5. El puerto 8081 es para acceso externo; dentro de Docker los servicios usan 8080

---

## 🧹 Limpiar Todo

```bash
# Detener servicios y eliminar volúmenes
docker-compose down -v

# Eliminar módulos locales (solo si quieres empezar de cero)
rm -rf wildfly-modules/

# Recompilar y reconfigurar
mvn clean package
./setup-modules.sh
docker-compose up -d
```
