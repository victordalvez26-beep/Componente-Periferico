hcen — README de configuración y arquitectura
=============================================

Resumen rápido
--------------
Este repositorio contiene una aplicación Jakarta EE (EAR) compuesta por módulos EJB, Web y EAR. Provee un servicio REST para gestionar "nodos periféricos" y un flujo de mensajería con RabbitMQ para notificar a los nodos cuando se crean (flow: persist -> publicar evento -> consumer llama al nodo -> actualiza estado).



Requisitos
----------
- JDK 11+ (el proyecto se compiló con target Java 17 en este workspace)
- Maven 3.6+
- WildFly (o servidor compatible Jakarta EE) para desplegar el EAR, o usar el plugin WildFly maven para deploy automático
- Docker (opcional) para levantar RabbitMQ con docker-compose

Levantar RabbitMQ (opcional, recomendado para pruebas E2E)
--------------------------------------------------------
Se incluye un `docker-compose.yml` en la raíz que define un servicio RabbitMQ con management UI. Por defecto las credenciales usadas en ejemplos son `hcen:hcen`.

1. Desde la raíz del repo:

```powershell
# en Windows PowerShell
docker compose up -d
```

2. Ver la management UI en: http://localhost:15672 (user/password: `hcen` / `hcen` si usás el compose incluido)

Configuración de conexión a RabbitMQ
------------------------------------
La configuración usada por el código busca las siguientes propiedades (leer `RabbitConfig` en el módulo EJB):
- Host
- Port
- Usuario
- Password

Por defecto el client usa `localhost:5672` y las credenciales del compose si están disponibles.

Endpoints REST principales
--------------------------
La aplicación expone una API JAX-RS bajo `@ApplicationPath("/api")` y el recurso de nodos está en `/nodos`.

- POST /nodo-periferico/api/nodos
  - Crea un nuevo `NodoPeriferico`. El servidor ignora el campo `estado` enviado por el cliente y decide internamente el estado inicial (por defecto `PENDIENTE`).
  - Ejemplo de payload: usa el archivo `post_nodo.json` incluido en el workspace (si existe) o construí uno similar.

- GET /nodo-periferico/api/nodos
  - Lista todos los nodos.

- GET /nodo-periferico/api/nodos/{id}
  - Recupera un nodo por id.

Flujo de mensajería (resumen)
-----------------------------
1. Usuario hace POST para crear un nodo.
2. `NodoService#createAndNotify` (EJB transaccional) persiste el nodo y publica un mensaje a un Exchange de RabbitMQ con routing key `alta.clinica`.
3. `NodoRegistrationConsumer` (EJB Singleton) está suscripto a la cola `nodo_config_queue` y al iniciar el EJB crea un consumidor AMQP.
4. Al recibir el mensaje, el consumer busca el nodo en BD y ejecuta una llamada HTTP al endpoint del nodo periférico (`{base}/config/init`) usando `NodoPerifericoHttpClient`.
5. Si la llamada HTTP responde con 2xx, el consumer marca el nodo como `ACTIVO`. Si falla, deja el nodo como `PENDIENTE` (no se marca ACTIVO) y registra la falla. Esto evita que un cliente externo marque ACTIVO sin confirmación.

Notas sobre transacciones y comportamiento esperado
--------------------------------------------------
- El servicio `NodoService` actualmente persiste el nodo y luego intenta publicar. Si la publicación falla y lanza una excepción, la transacción se revierte (por diseño actual). Esto significa que si deseás que el nodo quede persistido siempre aunque la mensajería falle, hay que cambiar la estrategia (persistir en una transacción y hacer el publish en otra, o persistir y luego, en caso de fallo, actualizar el estado en una nueva transacción).

- El `estado` del nodo está controlado por el servidor; el DTO ya no exige que el cliente envíe `estado`.

Clases y ficheros relevantes (mapa rápido)
------------------------------------------
- pom.xml (raíz): aggregator multi-module (modules: `ejb`, `web`, `ear`). Incluye un profile `skip-tests` para acelerar iteración local.
- ejb/
  - src/main/java/uy/edu/tse/hcen/messaging/
    - `RabbitSender.java` — bean que encapsula envío a RabbitMQ (intenta JMS si hay ConnectionFactory, fallback AMQP cliente directo).
    - `NodoRegistrationConsumer.java` — Singleton que arranca el consumer AMQP en `@PostConstruct` y procesa mensajes.
  - src/main/java/uy/edu/tse/hcen/service/
    - `NodoService.java` — EJB transaccional para crear y notificar nodos.
  - src/main/java/uy/edu/tse/hcen/repository/
    - `NodoPerifericoRepository.java` — acceso a BD; `create()` deja `PENDIENTE` por defecto si estado == null.
  - src/main/java/uy/edu/tse/hcen/utils/
    - `NodoPerifericoHttpClient.java` — util para llamar al nodo periférico (Java 11 HttpClient), ahora es un bean CDI `@ApplicationScoped`.

- web/
  - src/main/java/uy/edu/tse/hcen/rest/
    - `NodoPerifericoResource.java` — controlador REST; 
  - src/main/java/uy/edu/tse/hcen/rest/dto/
    - `NodoPerifericoDTO.java` y `NodoPerifericoConverter.java` — DTO y mapeo a entidad.

- ear/
  - pom.xml del EAR y configuración para empaquetado y deploy.

-------------------------------------------------------
1. Levantar RabbitMQ si querés pruebas E2E:

```powershell
docker compose up -d
```

2. Compilar y desplegar sin tests (iteración rápida):

```powershell
mvn -Pskip-tests -pl ear -am clean package wildfly:deploy
```

3. Hacer un POST de prueba (PowerShell + curl ejemplo):

```powershell
curl.exe -v -X POST -H "Content-Type: application/json" --data-binary "@C:\path\to\post_nodo.json" "http://127.0.0.1:8080/nodo-periferico/api/nodos"
```

4. Comprobar el estado vía GET:

```powershell
curl.exe -v http://127.0.0.1:8080/nodo-periferico/api/nodos
```

Siguientes pasos recomendados
----------------------------
- Si querés resiliencia mayor, agrego reintentos con backoff en `NodoRegistrationConsumer` y/o una Dead Letter Queue para mensajes fallidos.
- Si querés que el nodo quede persistido siempre (aunque falle la publicación), cambio la estrategia transaccional en `NodoService`.

