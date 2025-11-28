**Resumen de Ejecución de Tests (27-Nov-2025)**

- **Comando ejecutado**: `mvn test` en `clinica_backend` (ejecución completa multi-módulo).
- **Total tests ejecutados**: 868
- **Fallos (Failures)**: 2
- **Errores (Errors)**: 0

Cambios y fixes aplicados antes de esta ejecución
- Dependencias: añadida una implementación JAX‑RS en scope `test` para resolver `RuntimeDelegate` durante tests.
- `TenantAdminService`: adaptaciones (validaciones, manejo de ResultSet, llamada temprana a `createStatement()` en `createTenantSchema`).
- `SchemaMultiTenantProvider`: trata cadenas vacías como ausencia de tenant y `releaseConnection` lanza NPE si `connection==null`.
- Tests ajustados: `ProfesionalPersistenceHelperTest`, `LoginServiceTest`, `MongoDBProducerTest`.

Fallos actuales (2) — resumen y recomendaciones
- `DocumentoPdfServiceTest.testProcesarYGuardarPdfWithHcenUnavailable`
  - Síntoma: expected `<false>` but was `<true>`.
  - Posible causa: mock de HCEN no fuerza la indisponibilidad o la lógica interpreta la falla como éxito parcial.
  - Recomendación: revisar mock/impl; ejecutar aislado:
    ```powershell
    Set-Location 'C:\Users\CaroH\Documents\fing\TSE\lab\componente-periferico\clinica_backend\ejb'
    mvn -Dtest=uy.edu.tse.hcen.service.DocumentoPdfServiceTest#testProcesarYGuardarPdfWithHcenUnavailable test
    ```

- `HcenClientTest.testHandleTokenRejectionWithNullNewToken`
  - Síntoma: esperaba `HcenUnavailableException` pero no se lanzó.
  - Posible causa: flujo de refresh/reintento no lanza excepción cuando nuevo token es `null`, o el test no configuró el mock correctamente.
  - Recomendación: revisar cliente y test; ejecutar aislado:
    ```powershell
    mvn -Dtest=uy.edu.tse.hcen.client.HcenClientTest#testHandleTokenRejectionWithNullNewToken test
    ```

Advertencias importantes (instrumentación JaCoCo)
- Observado: `Unsupported class file major version 68` al instrumentar mocks (Mockito/ByteBuddy).
- Recomiendo:
  - Temporal: desactivar JaCoCo localmente con `-Djacoco.skip=true`.
  - Permanente: actualizar `jacoco-maven-plugin`/agente a versión compatible con el JDK usado.

Siguientes acciones sugeridas (priorizadas)
1. Corregir los 2 tests que fallan (`DocumentoPdfServiceTest`, `HcenClientTest`).
2. Actualizar JaCoCo en `pom.xml` a una versión compatible con la JVM local o habilitar un perfil que lo omita en runs de depuración.
3. Limpiar stubbings innecesarios en `LoginServiceTest` y usar `doThrow(...)` para `void` cuando corresponda.

Comandos útiles
```powershell
cd C:/Users/CaroH/Documents/fing/TSE/lab/componente-periferico/clinica_backend
mvn test
```

Desactivar JaCoCo temporalmente:
```powershell
mvn -Djacoco.skip=true test
```

Archivos modificados por mí (resumen)
- `ejb/src/main/java/uy/edu/tse/hcen/multitenancy/SchemaMultiTenantProvider.java`
- `ejb/src/main/java/uy/edu/tse/hcen/service/TenantAdminService.java`
- Tests: `ProfesionalPersistenceHelperTest`, `LoginServiceTest`, `MongoDBProducerTest`

Opciones para que continúe
- A: Arreglar los 2 fallos restantes ahora.
- B: Actualizar `jacoco-maven-plugin` en POM y volver a ejecutar.
- C: Ambos A + B.

Indica la opción y procedo.