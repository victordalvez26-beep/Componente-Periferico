# Guía para manejar tests de métodos privados

## Problema
Después de refactorizar StatsService y LoginService para mejorar su calidad según SonarQube, algunos métodos se convirtieron de `public` a `private`. Los tests que llamaban directamente a estos métodos dejarán de compilar.

## Solución

### Opción 1: Probar a través de la API pública (RECOMENDADO)
Los métodos privados son detalles de implementación y NO deberían testearse directamente. En su lugar, debes:

1. Testear los métodos públicos que usan estos métodos privados
2. Los métodos privados se testearán indirectamente

**Ejemplo:**
```java
// ❌ MAL (testea método privado directamente)
@Test
void testContarProfesionales() {
    int count = statsService.contarProfesionales("101"); // Ya no compila
    assertEquals(5, count);
}

// ✅ BIEN (testea a través del método público)
@Test
void testObtenerEstadisticas() {
    Map<String, Object> stats = statsService.obtenerEstadisticas("101");
    assertEquals(5, stats.get("profesionales")); // Indirectamente testea contarProfesionales
}
```

### Opción 2: Comentar el test con @Disabled
Si el test es específico y no puede reemplazarse fácilmente:

```java
@Disabled("Método convertido a privado - funcionalidad probada en testObtenerEstadisticas")
@Test
void testContarProfesionales() {
    // Test comentado porque contarProfesionales() ahora es privado
    // La funcionalidad se sigue probando indirectamente en testObtenerEstadisticas()
}
```

### Opción 3: Usar Reflection (NO RECOMENDADO)
Solo si absolutamente necesitas testear la lógica específica de un método privado:

```java
@Test
void testContarProfesionalesConReflection() throws Exception {
    Method metodo = StatsService.class.getDeclaredMethod("contarProfesionales", String.class);
    metodo.setAccessible(true);
    int result = (int) metodo.invoke(statsService, "101");
    assertEquals(5, result);
}
```

**⚠️ Advertencia:** Esta opción es frágil y acopla los tests a la implementación interna.

## Métodos afectados en StatsService

Los siguientes métodos cambiaron de `public` a `private`:

- `contarProfesionales(String tenantId)`
- `contarUsuariosSalud(Long tenantId)`
- `contarDocumentosTotales(Long tenantId)`
- `contarDocumentosHoy(Long tenantId)`
- `obtenerUltimosDocumentos(Long tenantId, int limite)`
- `obtenerUltimosUsuarios(Long tenantId, int limite)`
- `obtenerUltimosProfesionales(String tenantId, int limite)`
- `obtenerNombreProfesional(String profesionalId, Long tenantId)`
- `crearEstadisticasVacias()`

## Métodos afectados en LoginService

Los siguientes métodos se crearon como `private`:

- `buscarUsuario(String nickname, String tenantId)`
- `buscarEnSchemaTenant(String nickname, String tenantId)`
- `configurarTenantContext(String tenantId)`
- `validarCredenciales(UsuarioPeriferico user, String rawPassword)`
- `logPasswordDebugInfo(String storedHash, String rawPassword)`
- `determinarRole(UsuarioPeriferico user)`

## Métodos públicos para testear (API pública)

### StatsService
- `obtenerEstadisticas(String tenantId)` - Testea indirectamente todos los métodos de conteo
- `obtenerActividadReciente(String tenantId, int limite)` - Testea indirectamente todos los métodos obtenerUltimos*

### LoginService
- `authenticateAndGenerateToken(String nickname, String rawPassword, String tenantId)` - Testea indirectamente todo el flujo de autenticación

## Recomendación Final
✅ **Enfócate en testear el comportamiento público, no la implementación privada**

Los tests deben verificar QUÉ hace el código (comportamiento observable), no CÓMO lo hace (detalles de implementación). Esto hace que los tests sean más robustos ante refactorizaciones.
