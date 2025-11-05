# Documentación Componente Periférico

Esta carpeta contiene documentación adicional del componente periférico (backend de clínica).

## Archivos

- **`openapi-periferico.yaml`** - Especificación OpenAPI 3.0 completa de la API REST del componente periférico
- **`README-TESTING.md`** - Guía de testing y pruebas adicionales

## Uso de la Documentación OpenAPI

Para visualizar la documentación OpenAPI, puedes usar:

1. **Swagger UI**:
   ```bash
   # Instalar swagger-ui-serve
   npm install -g swagger-ui-serve
   
   # Servir la documentación
   swagger-ui-serve openapi-periferico.yaml
   ```

2. **Postman**: Importar el archivo YAML directamente en Postman

3. **Editor Online**: Usar [Swagger Editor](https://editor.swagger.io/) para ver y editar

## Verificación del Flujo

Ver el README principal (`../README.md`) para instrucciones completas sobre cómo probar el flujo end-to-end, incluyendo:
- Creación de tenant y usuario admin
- Login y obtención de token JWT
- Creación de documentos completos
- Verificación de creación automática de pacientes

