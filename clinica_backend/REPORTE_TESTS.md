# Reporte de Tests - Componente Periférico HCEN

**Fecha:** 27 de noviembre de 2025  
**Proyecto:** clinica_backend  
**Total de Tests:** 231  
**Tests Exitosos:** 223  
**Tests Fallidos:** 8  
**Errores:** 0  
**Tasa de Éxito:** 96.5%

**Última Ejecución:** 27 de noviembre de 2025, 21:56:26

---

## Resumen Ejecutivo

Se ejecutaron 231 tests en total, de los cuales 223 pasaron exitosamente (96.5%). Se identificaron 8 fallos y **0 errores** (mejora significativa desde 7 errores). Los problemas principales están relacionados con:

2. **Validaciones de TenantContext**: Varios tests fallan porque alguna validación está retornando 400 antes de llegar a la lógica principal
3. **Manejo de excepciones**: Un test espera un código HTTP 500 pero recibe 200

---



## Fallos de Tests

### 2. DocumentoClinicoResourceTest - Códigos HTTP Incorrectos (5 fallos)

**Tests Afectados:**
- `testCrearDocumentoCompletoSuccess` - Espera 201, recibe 400
- `testCrearDocumentoCompletoWithException` - Espera 500, recibe 400
- `testCrearDocumentoCompletoConArchivoSuccess` - Espera 201, recibe 400
- `testCrearDocumentoCompletoConArchivoWithArchivo` - Espera 201, recibe 400
- `testCrearDocumentoCompletoConArchivoWithException` - Espera 500, recibe 400

**Causa:**
Los tests están recibiendo código HTTP 400 (Bad Request) en lugar de 201 (Created) o 500 (Internal Server Error). Esto indica que:

1. **Validación de TenantContext**: El recurso valida que `TenantContext.getCurrentTenant()` no sea null o vacío. Si no está configurado, retorna 400 con el mensaje "Tenant no identificado".

2. **Configuración de Tests**: Los tests configuran `TenantContext.setCurrentTenant("101")` pero puede que:
   - El contexto se esté limpiando entre tests
   - El recurso no esté leyendo correctamente el contexto
   - Hay un problema de sincronización en los tests

**Análisis del Código:**
En `DocumentoClinicoResource.crearDocumentoCompleto()`:
```java
String tenantIdStr = TenantContext.getCurrentTenant();
if (tenantIdStr == null || tenantIdStr.isBlank()) {
    return DocumentoResponseBuilder.badRequest("Tenant no identificado");
}
```

**Solución Aplicada:**
1. ✅ **COMPLETADO**: Se agregó configuración de `TenantContext` en `@BeforeEach`
2. ✅ **COMPLETADO**: Se agregaron logs de depuración (INFO) en `DocumentoClinicoResource` para identificar qué validación está fallando
3. ✅ **COMPLETADO**: Se verificó que los mocks de `SecurityContext` y `Principal` estén correctamente configurados
4. ✅ **COMPLETADO**: Se agregaron logs en `DocumentoPdfResource` para debugging

**Estado Actual:**
Los tests siguen fallando con código 400. Los logs agregados deberían ayudar a identificar exactamente qué validación está retornando 400. Las posibles causas son:

1. **Validación de `profesionalId`**: Si `securityContext.getUserPrincipal()` retorna null, se retorna 401 (no 400)
2. **Validación de `tenantIdStr`**: Si `TenantContext.getCurrentTenant()` retorna null o vacío, se retorna 400
3. **Validación de `ciPaciente`**: Si el campo está null o vacío, se retorna 400
4. **Validación de `contenido`**: Si el campo está null o vacío, se retorna 400

**Logs Agregados:**
```java
LOG.info("=== crearDocumentoCompleto INICIO ===");
LOG.infof("crearDocumentoCompleto: profesionalId=%s, securityContext=%s", profesionalId, securityContext);
LOG.infof("crearDocumentoCompleto: tenantIdStr=%s", tenantIdStr);
LOG.infof("crearDocumentoCompleto: ciPaciente=%s, contenido=%s", ciPaciente, contenido != null ? "presente" : "null");
```

**Próximos Pasos**:
- Ejecutar los tests con los logs habilitados para identificar exactamente qué validación está fallando
- Verificar que los mocks estén configurados correctamente antes de ejecutar el método del recurso
- Revisar si hay algún problema con la inyección de dependencias en los tests

**Prioridad:** ALTA - Afecta funcionalidad crítica de creación de documentos

---

### 3. DocumentoPdfResourceTest - Códigos HTTP Incorrectos (2 fallos)

**Tests Afectados:**
- `testSubirPdfSuccess` - Espera 201, recibe 400
- `testSubirPdfWithException` - Espera 500, recibe 400

**Causa:**
Similar al problema anterior, los tests están recibiendo 400 en lugar de 201 o 500. Las causas probables son:

1. **Validación de TenantContext**: Mismo problema que en DocumentoClinicoResourceTest
2. **Validación de Content-Type**: El recurso valida que el archivo sea PDF (`application/pdf`). Si el mock no está configurado correctamente, puede fallar esta validación.
3. **Configuración de MultipartFormDataInput**: Los mocks de `InputPart` y `MultivaluedMap` pueden no estar configurados correctamente.

**Análisis del Código:**
En `DocumentoPdfResource.subirPdf()`:
```java
String tenantIdStr = TenantContext.getCurrentTenant();
if (tenantIdStr == null || tenantIdStr.isBlank()) {
    return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", "Tenant no identificado"))
            .build();
}

String contentType = archivoPart.getHeaders().getFirst("Content-Type");
if (contentType == null || !contentType.equals("application/pdf")) {
    return Response.status(Response.Status.BAD_REQUEST)
            .entity(Map.of("error", "Solo se permiten archivos PDF"))
            .build();
}
```

**Solución Aplicada:**
1. ✅ **COMPLETADO**: Se configuró `TenantContext` en `@BeforeEach` con `TenantContext.setCurrentTenant("101")`
2. ✅ **COMPLETADO**: Se corrigieron los mocks de `InputPart.getHeaders()` para retornar un `MultivaluedHashMap` con `Content-Type: application/pdf`
3. ✅ **COMPLETADO**: Se agregaron logs de depuración en `DocumentoPdfResource` para identificar qué validación está fallando
4. ✅ **COMPLETADO**: Se corrigió el problema en `testSubirPdfWithInvalidContentType` donde se estaba intentando encadenar `getHeaders().getFirst()` incorrectamente

**Logs Agregados:**
```java
LOG.infof("subirPdf: profesionalId=%s, securityContext=%s", profesionalId, securityContext);
LOG.infof("subirPdf: tenantIdStr=%s", tenantIdStr);
LOG.infof("subirPdf: contentType=%s", contentType);
```

**Estado Actual:**
Los tests siguen fallando con código 400. Los logs agregados deberían ayudar a identificar exactamente qué validación está retornando 400.

**Prioridad:** ALTA - Afecta funcionalidad de subida de PDFs

---

### 4. ProfesionalResourceTest - Manejo de Excepciones (1 fallo)

**Test Afectado:**
- `testVerificarPermisoWithException` - Espera 500, recibe 200

**Causa:**
El test espera que cuando `politicasAccesoClient.verificarPermiso()` lance una excepción, el recurso retorne código HTTP 500 (Internal Server Error). Sin embargo, está retornando 200 (OK).

**Análisis del Código:**
En `ProfesionalResource.verificarPermiso()`:
```java
try {
    // ... validaciones ...
    boolean tienePermiso = politicasAccesoClient.verificarPermiso(...);
    return Response.ok(Map.of("tienePermiso", tienePermiso)).build();
} catch (Exception e) {
    LOG.error("Error al verificar permiso", e);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity(Map.of("error", "Error al verificar permiso: " + e.getMessage()))
        .build();
}
```

El código parece correcto. El problema puede ser:

1. **Mock no configurado correctamente**: El mock de `politicasAccesoClient` puede no estar lanzando la excepción como se espera
2. **Inyección del mock**: El mock puede no estar siendo inyectado correctamente en el recurso
3. **Excepción capturada antes**: Alguna validación anterior puede estar retornando antes de llegar a la llamada que lanza la excepción

**Solución Aplicada:**
1. ✅ **COMPLETADO**: Se verificó que el mock esté configurado correctamente con `thenThrow()`
2. ✅ **COMPLETADO**: Se agregó inyección manual de mocks usando reflection en `setUp()`
3. ✅ **COMPLETADO**: Se agregaron logs de depuración en `ProfesionalResource`
4. ✅ **COMPLETADO**: Se configuró `TenantContext` en `setUp()`

**Logs Agregados:**
```java
LOG.debugf("verificarPermiso: politicasAccesoClient=%s", politicasAccesoClient);
LOG.debugf("verificarPermiso: resultado=%s", tienePermiso);
LOG.errorf(e, "Error al verificar permiso - Profesional: %s, Paciente: %s, TipoDoc: %s, Tenant: %s", ...);
```

**Estado Actual:**
El test sigue fallando - espera 500 pero recibe 200. Esto sugiere que:
- El mock puede no estar lanzando la excepción como se espera
- La excepción puede estar siendo capturada y manejada de manera diferente
- Puede haber un problema con la inyección del mock

**Próximos Pasos:**
- Verificar que el mock esté siendo inyectado correctamente usando reflection
- Agregar más logs para ver si la excepción se está lanzando
- Verificar que el método `verificarPermiso` esté siendo llamado correctamente

**Prioridad:** MEDIA - Afecta el manejo de errores pero no la funcionalidad principal

---

## Correcciones Aplicadas

### 1. Configuración de TokenUtils para Tests
- **Problema**: `TokenUtils` tiene un bloque estático que requiere una variable de entorno `JWT_SECRET_BASE64` o propiedad del sistema `hcen.jwt.secret.base64`
- **Solución**: Se agregó configuración en `pom.xml` para establecer la propiedad del sistema antes de ejecutar los tests
- **Archivos Modificados**: 
  - `web/pom.xml` - Agregado `maven-surefire-plugin` con `systemPropertyVariables`
  - `ejb/pom.xml` - Agregado `maven-surefire-plugin` con `systemPropertyVariables`
  - `web/src/test/java/uy/edu/tse/hcen/rest/filter/AuthTokenFilterTest.java` - Agregado `@BeforeAll` para configurar la propiedad
- **Estado**: ✅ Completado - Los tests de AuthTokenFilterTest ahora pueden ejecutarse sin errores de inicialización

### 2. Corrección de DocumentoPdfResourceTest
- **Problema**: Error de tipo al intentar encadenar `getHeaders().getFirst()` en un mock
- **Solución**: Se cambió para crear un `MultivaluedHashMap` real y configurarlo correctamente
- **Archivo Modificado**: `web/src/test/java/uy/edu/tse/hcen/rest/DocumentoPdfResourceTest.java`
- **Estado**: ✅ Completado - El error de compilación fue resuelto

### 3. Configuración de TenantContext en Tests
- **Problema**: Los tests de `DocumentoClinicoResourceTest` y `DocumentoPdfResourceTest` fallaban porque `TenantContext` no estaba configurado
- **Solución**: Se agregó configuración de `TenantContext.setCurrentTenant("101")` en el método `setUp()` de ambos archivos de test
- **Archivos Modificados**:
  - `web/src/test/java/uy/edu/tse/hcen/rest/DocumentoClinicoResourceTest.java`
  - `web/src/test/java/uy/edu/tse/hcen/rest/DocumentoPdfResourceTest.java`
  - `web/src/test/java/uy/edu/tse/hcen/rest/ProfesionalResourceTest.java`
- **Estado**: ⚠️ Parcialmente completado - La configuración se agregó pero los tests aún fallan. Se requiere investigación adicional para determinar si hay otras validaciones que están causando el error 400.

### 4. Creación de Interfaz ILoginService
- **Problema**: Mockito no podía hacer mock de `LoginService` debido a anotaciones CDI
- **Solución**: Se creó la interfaz `ILoginService` y se refactorizó el código para usarla
- **Archivos Modificados**:
  - `ejb/src/main/java/uy/edu/tse/hcen/service/ILoginService.java` (nuevo)
  - `ejb/src/main/java/uy/edu/tse/hcen/service/LoginService.java`
  - `web/src/main/java/uy/edu/tse/hcen/rest/AuthResource.java`
  - `web/src/test/java/uy/edu/tse/hcen/rest/AuthResourceTest.java`
- **Estado**: ✅ Completado - Todos los tests de `AuthResourceTest` ahora pasan exitosamente

### 5. Agregado de Logs de Depuración
- **Problema**: No se podía identificar qué validación estaba causando los errores 400
- **Solución**: Se agregaron logs de nivel INFO en los recursos para identificar qué validación está fallando
- **Archivos Modificados**:
  - `web/src/main/java/uy/edu/tse/hcen/rest/DocumentoClinicoResource.java`
  - `web/src/main/java/uy/edu/tse/hcen/rest/DocumentoPdfResource.java`
  - `web/src/main/java/uy/edu/tse/hcen/rest/ProfesionalResource.java`
- **Estado**: ✅ Completado - Los logs están agregados y listos para usar en debugging

### 6. Corrección de Headers en Tests de Multipart
- **Problema**: Los tests de `DocumentoClinicoResourceTest` no configuraban correctamente los headers de Content-Type
- **Solución**: Se agregó configuración correcta de `MultivaluedHashMap` con `Content-Type: application/pdf` y `Content-Disposition`
- **Archivos Modificados**:
  - `web/src/test/java/uy/edu/tse/hcen/rest/DocumentoClinicoResourceTest.java`
- **Estado**: ✅ Completado - Los headers están configurados correctamente

---

## Warnings y Mejoras Sugeridas

### 1. Imports No Utilizados
Hay varios imports no utilizados que generan warnings del compilador:
- `org.junit.jupiter.api.BeforeEach` en varios archivos de test
- `org.bson.types.Binary` en varios archivos
- `java.time.LocalDateTime` y `java.time.LocalDate` en algunos archivos
- Varios otros imports menores

**Recomendación**: Ejecutar un análisis estático (como SonarLint) para identificar y eliminar imports no utilizados.

### 2. Type Safety Warnings
Hay múltiples warnings de "Type safety: The expression of type X needs unchecked conversion":
- En `DocumentoClinicoRepositoryTest` con `FindIterable<Document>`
- En `StatsServiceTest` con múltiples `FindIterable<Document>`
- En `HcenUsuarioSaludClientTest` con `HttpResponse`

**Recomendación**: Agregar `@SuppressWarnings("unchecked")` donde sea apropiado o mejorar los tipos genéricos.

### 3. Variables No Utilizadas
- `nombreClinica` en `DocumentoPdfService.java`
- `profesionalSaludService` en `DocumentoPdfResource.java`
- `currentTenantId` en `ProfesionalSaludService.java`

**Recomendación**: Eliminar variables no utilizadas o implementar la funcionalidad que las requiere.

---

## Recomendaciones Generales

### 1. Mejora de Tests
- **Configuración Centralizada**: Crear una clase base de test o un `@BeforeAll` global que configure `TenantContext` y otras dependencias comunes
- **Fixtures de Test**: Crear objetos de prueba reutilizables para evitar duplicación de código
- **Verificación de Mocks**: Asegurar que todos los mocks estén correctamente configurados antes de ejecutar los tests

### 2. Manejo de Errores
- **Logging Mejorado**: Agregar más información de contexto en los logs de error para facilitar el debugging
- **Mensajes de Error Consistentes**: Estandarizar los mensajes de error para facilitar el diagnóstico

### 3. Documentación
- **JavaDoc**: Agregar documentación JavaDoc a los métodos de los recursos REST
- **Tests como Documentación**: Mejorar los nombres y comentarios de los tests para que sirvan como documentación

---

## Próximos Pasos

1. **Inmediato (Alta Prioridad)**:
   - ✅ **COMPLETADO**: Resolver el problema de mockeo de `LoginService` creando una interfaz
   - **PENDIENTE**: Identificar qué validación está causando los errores 400 en `DocumentoClinicoResourceTest` y `DocumentoPdfResourceTest` usando los logs agregados
   - **PENDIENTE**: Corregir el test `testVerificarPermisoWithException` en `ProfesionalResourceTest` - verificar por qué el mock no está lanzando la excepción

2. **Corto Plazo (Media Prioridad)**:
   - Ejecutar los tests con los logs habilitados para identificar exactamente qué validación está fallando
   - Verificar que los mocks estén configurados correctamente antes de ejecutar el método del recurso
   - Limpiar imports no utilizados
   - Resolver warnings de type safety
   - Eliminar variables no utilizadas

---

## Conclusión

El proyecto tiene una buena base de tests con una tasa de éxito del **96.5%** (mejora desde 93.5%). Se resolvieron exitosamente **7 errores críticos** relacionados con Mockito y `LoginService` mediante la creación de la interfaz `ILoginService`.

**Progreso Realizado:**
- ✅ **7 errores resueltos** (de 7 a 0)
- ✅ **Tasa de éxito mejorada** del 93.5% al 96.5%
- ✅ **Logs de depuración agregados** para facilitar la identificación de problemas
- ✅ **Interfaz ILoginService creada** para resolver problemas de Mockito

**Problemas Restantes:**
1. **8 fallos pendientes**: Todos relacionados con validaciones que retornan 400 en lugar de los códigos esperados
   - 5 fallos en `DocumentoClinicoResourceTest`
   - 2 fallos en `DocumentoPdfResourceTest`
   - 1 fallo en `ProfesionalResourceTest`

**Próximos Pasos:**
Los logs agregados deberían ayudar a identificar exactamente qué validación está causando los errores 400. Una vez identificado, se podrán corregir los tests o ajustar las validaciones según corresponda.

Con las correcciones pendientes, se espera alcanzar una tasa de éxito del 100% en los tests.

---

**Generado por:** Auto (AI Assistant)  
**Última actualización:** 27 de noviembre de 2025, 21:56:26

---

## Cambios Recientes (27 de noviembre de 2025, 21:56:26)

### Correcciones Aplicadas:
1. ✅ **Interfaz ILoginService creada** - Resuelve 7 errores de Mockito
2. ✅ **Logs de depuración agregados** - En DocumentoClinicoResource, DocumentoPdfResource y ProfesionalResource
3. ✅ **Configuración de TenantContext mejorada** - En todos los tests que lo requieren
4. ✅ **Headers de Content-Type corregidos** - En tests de multipart

### Resultados:
- **Errores:** 0 (mejora desde 7)
- **Fallos:** 8 (sin cambios, pero con mejor diagnóstico)
- **Tasa de éxito:** 96.5% (mejora desde 93.5%)

