# Nodo Periférico — Guía rápida

Este README describe la utilidad de los archivos clave en `componente-periferico`, cómo levantar el periphery (frontend/backend), y cómo probar la integración con HCEN (E2E).


## Archivos y carpetas importantes

- `backend/` — Microservicio Java que expone la API REST del nodo periférico y contiene la lógica de integración con RabbitMQ (cuando aplica).
  - `web/src/main/java/uy/edu/tse/hcen/rest/NodoInitResource.java` — Recurso JAX-RS con endpoints de prueba:
    - `POST /api/config/init` — Simula la inicialización del nodo (espera payload con `id`) y devuelve 200 OK.
    - `POST /api/config/update` — Simula actualización de configuración.
    - `POST /api/config/delete` — Simula baja/limpieza.
  - `web/src/main/java/uy/edu/tse/hcen/rest/NodoPerifericoResource.java` — Recursos para CRUD locales del nodo cuando aplica.
  - `tools/` — Scripts CLI para WildFly (crear recursos JMS, datasources, etc.).
  - `README.md` / `README.txt` — Documentación de deploy local específica del backend.

- `frontend/` — Interfaz React (o similar) para administración de clínicas/nodos.
  - `package.json` — Dependencias y scripts (`npm start`, `npm ci`).
  - `src/components/` — Componentes relevantes (`ClinicAdmin.js`, `Login.js`).
  - `build/` y `public/` — Resultado del build para producción.

- `post_nodo.json`, `post_payload.ps1` — Ejemplos/payloads y scripts para probar endpoints manualmente.

## Cómo levantar localmente

### 1) Backend (WAR/EAR en WildFly)
- Compilar y generar el EAR (desde la raíz `componente-periferico/backend` o la raíz del monorepo si está conectado):
```powershell
cd componente-periferico/backend
mvn clean package
```
- Desplegar el EAR en tu WildFly (copiar a `standalone/deployments` o usar la consola web `http://localhost:9990`).
- Verificar que el contexto está desplegado (ej: `http://localhost:8080/nodo-periferico/api/config/init`).

### 2) Frontend (desarrollo)
```powershell
cd componente-periferico/frontend
npm ci
npm start
```
- Normalmente el dev server corre en `http://localhost:3000` y hace llamadas al backend configurado.

## Pruebas E2E rápidas (manualmente)
1. Verificar que HCEN está desplegado y que existe un nodo con `id=1` o crea uno con el payload apropiado.
2. Desde el Management UI de RabbitMQ o usando scripts, publicar un mensaje a `clinica_config_exchange` con `routing_key=alta.clinica` y `payload={"id":1,"action":"alta"}` para forzar la llamada desde el consumer.
3. Si el periphery está en WildFly, la URL que HCEN debe usar será algo como `http://localhost:8080/nodo-periferico/api`.
4. Comprobar logs de WildFly para ver la llamada entrante y la respuesta (`server.log`).

## Debugging y notas
- Si ves `JSON Binding deserialization error`, revisa que el body enviado sea JSON válido (usar `-InFile` con `Invoke-RestMethod` o `curl --data-binary @file` evita problemas de quoting).
- Credenciales RabbitMQ management: si necesitas publicar desde scripts, usa las credenciales del management (en nuestro entorno por defecto pueden ser `hcen:hcen`).

## Archivos de prueba útiles
- `smoke-temp/create-nodo.json` — payload para crear nodo en HCEN.
- `smoke-temp/test-init.json` — payload para probar `POST /config/init`.
- `smoke-test.ps1` — script PowerShell creado para pruebas rápidas (envía test-init, create-nodo, etc.).
- `publish-rabbit-message.ps1` — script para publicar mensajes via RabbitMQ HTTP API (actualiza credenciales si corresponde).

---

Si querés, puedo:
- Añadir comandos concretos para publicar `update.clinica` y `delete.clinica` y ejemplos de payload.
- Generar un script de PowerShell que ejecute el flujo E2E completo (publicar + poll estado)."