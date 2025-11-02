
Requisitos
- Docker y Docker Compose
- Java 17 (para compilar con Maven)
- Maven
- PowerShell (ejemplos incluidos)

1) Levantar los contenedores (Postgres, Mongo y WildFly)

Windows PowerShell (desde la carpeta `clinica_backend`):

```powershell
cd C:\Users\CaroH\Documents\fing\TSE\lab\componente-periferico\clinica_backend
docker compose up -d
```

Observa que el compose crea los contenedores llamados (ejemplos): `hcen-postgres-db`, `mongodb`, `hcen-wildfly-app`.

2) Inicializar la base de datos (script )

Copia y ejecuta el script `db/init-db.sql` dentro del contenedor Postgres y ejecútalo con psql (esto es idempotente y puede volver a ejecutarse):

```powershell
# copia el script
docker cp .\db\init-db.sql hcen-postgres-db:/tmp/init-db.sql
# ejecutar dentro del contenedor
docker exec -i hcen-postgres-db psql -U postgres -d hcen_db -f /tmp/init-db.sql
```

Salida esperada: varias `CREATE TABLE` / `INSERT` y mensajes `ON CONFLICT DO NOTHING` — sin errores fatales.

3) Compilar el proyecto y desplegar la EAR

Desde `clinica_backend` compila con Maven:

```powershell
mvn -DskipTests package -f .\pom.xml
```

Opciones para desplegar en WildFly:
- Si tu `docker-compose` monta la carpeta `ear/target` en el contenedor WildFly, reinicia WildFly para que recoja la nueva EAR:
  ```powershell
  docker restart hcen-wildfly-app
  ```
- Si no está montada, copia la EAR generada al contenedor (deploy manual):
  ```powershell
  docker cp .\ear\target\hcen.ear hcen-wildfly-app:/opt/jboss/wildfly/standalone/deployments/
  ```
  (Ajusta la ruta según tu contenedor/permits)

4) Endpoints relevantes y prueba rápida

  POST http://localhost:8080/hcen-web/api/auth/login
  Body JSON:
  { "nickname": "admin_c1", "password": "password123" }

PowerShell ejemplo:

```powershell
$body = '{"nickname":"admin_c1","password":"password123"}'
$r = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/hcen-web/api/auth/login' -ContentType 'application/json' -Body $body
# ver token y rol retornados
$r.token
$r.role
```

## Endpoints administrativos

Hay dos endpoints administrativos bajo `/api/admin/tenants` protegidos por la claim `role` en el JWT (el `AuthTokenFilter` configura un SecurityContext simple a partir del token): solo usuarios con el rol `ADMINISTRADOR` pueden llamarlos.

- POST /api/admin/tenants
    - Crea un tenant en tiempo de ejecución. Body JSON:
        ```json
        { "tenantId": "107", "nombrePortal": "NombreClinica", "colorPrimario": "#007bff" }
        ```
    - Devuelve: 201 Created en caso de éxito.
    - Ejemplo (PowerShell):
        ```powershell
        $body = '{"tenantId":"107","nombrePortal":"Clinica Test","colorPrimario":"#00aa88"}'
        $token = (Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/hcen-web/api/auth/login' -ContentType 'application/json' -Body '{"nickname":"admin_c3","password":"password123"}').token
        Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/hcen-web/api/admin/tenants' -ContentType 'application/json' -Body $body -Headers @{ Authorization = "Bearer $token" }
        ```

- GET /api/admin/tenants
    - Devuelve un arreglo JSON con los tenants registrados en `public.nodoperiferico` (id, nombre, rut).
    - Ejemplo (PowerShell):
        ```powershell
        $token = (Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/hcen-web/api/auth/login' -ContentType 'application/json' -Body '{"nickname":"admin_c3","password":"password123"}').token
        Invoke-RestMethod -Method Get -Uri 'http://localhost:8080/hcen-web/api/admin/tenants' -Headers @{ Authorization = "Bearer $token" } | ConvertTo-Json
        ```

Notas:
- El endpoint REST de administración crea el esquema del tenant y las tablas mínimas (portal_configuracion, usuario, usuarioperiferico, nodoperiferico). No inserta usuarios locales del tenant por defecto — usa la opción `-createGlobalAdmin` en el helper de PowerShell si quieres crear un administrador global al mismo tiempo.
- El script auxiliar de PowerShell `db/create-tenant.ps1` puede usarse desde el host para crear un tenant sin llamar al endpoint REST.
Respuesta esperada (ejemplo):
{
  "role": "ADMINISTRADOR",
  "token": "<JWT>"
}

- Llamada a un endpoint tenant-scoped usando el token (ejemplo `/portal-configuracion/public`):

```powershell
$t = $r.token
Invoke-RestMethod -Uri 'http://localhost:8080/hcen-web/api/portal-configuracion/public' -Headers @{ Authorization = "Bearer $t" } | ConvertTo-Json -Depth 5
```

Respuesta esperada (para admin_c1 / tenant 101): objeto con `colorPrimario`, `colorSecundario`, `nombrePortal`.

5) Decodificar el JWT (opcional)
- El token contiene las claims `role` y `tenantId`. Puedes decodificarlo en un sitio JWT debugger o localmente (splitting base64 parts). No exponer el secret.

Resumen:

```powershell
# arrancar servicios
cd <path>/clinica_backend
docker compose up -d

# inicializar BD
docker cp .\db\init-db.sql hcen-postgres-db:/tmp/init-db.sql
docker exec -i hcen-postgres-db psql -U postgres -d hcen_db -f /tmp/init-db.sql

# compilar y desplegar
mvn -DskipTests package -f .\pom.xml
docker restart hcen-wildfly-app

# probar login (PowerShell)
$body = '{"nickname":"admin_c1","password":"password123"}'
$r = Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/hcen-web/api/auth/login' -ContentType 'application/json' -Body $body
$t = $r.token
Invoke-RestMethod -Uri 'http://localhost:8080/hcen-web/api/portal-configuracion/public' -Headers @{ Authorization = "Bearer $t" }
```
