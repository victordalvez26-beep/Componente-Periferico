# Documentación Componente Periférico

Esta carpeta contiene documentación adicional del componente periférico.

## 📚 Contenido

### APIs REST

- **`openapi-periferico.yaml`**: Especificación OpenAPI 3.0 completa de la API del componente periférico
  - Incluye todos los endpoints REST
  - Documenta autenticación JWT
  - Endpoints de documentos con verificación de permisos
  - Endpoint de solicitud de acceso

### Guías de Uso

- **`README-POLITICAS.md`**: Guía completa de integración con políticas de acceso
  - Verificación automática de permisos
  - Solicitud de acceso desde el componente periférico
  - Registro de accesos para auditoría
  - Ejemplos de uso con PowerShell
  - Flujos completos paso a paso

**⚠️ Nota:** Usar `127.0.0.1:8080` en lugar de `localhost:8080` para las pruebas.

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

## Endpoints Principales

### Documentos

- `GET /api/documentos/{id}/contenido` - Descarga contenido con verificación de permisos
- `POST /api/documentos/solicitar-acceso` - Solicitar acceso a documentos específicos de un paciente
- `POST /api/documentos/solicitar-acceso-historia-clinica` - Solicitar acceso a toda la historia clínica de un paciente
- `POST /api/documentos/solicitudes/{id}/aprobar` - Aprobar una solicitud de acceso
- `POST /api/documentos/solicitudes/{id}/rechazar` - Rechazar una solicitud de acceso
- `POST /api/documentos/completo` - Crear documento completo
- `GET /api/documentos/paciente/{documentoIdPaciente}/metadatos` - Metadatos del paciente
- `GET /api/documentos/paciente/{documentoIdPaciente}/resumen` - Generar resumen de historia clínica con IA (OpenAI o3)

### Políticas de Acceso

- `POST /api/documentos/politicas` - Crear política de acceso para un profesional específico
- `POST /api/documentos/politicas/global` - Crear política global (acceso a todos los pacientes)
- `POST /api/documentos/politicas/especialidad` - Crear políticas por especialidad (masivo)
- `GET /api/documentos/politicas` - Listar todas las políticas
- `GET /api/documentos/politicas/paciente/{ci}` - Listar políticas por paciente
- `GET /api/documentos/politicas/profesional/{id}` - Listar políticas por profesional
- `DELETE /api/documentos/politicas/{id}` - Eliminar política

### Autenticación

- `POST /api/auth/login` - Login y obtención de token JWT
- `POST /api/config/init` - Crear tenant y usuario admin
- `POST /api/config/activate-simple` - Activar usuario admin

## Integración con Servicios

El componente periférico se integra con:

- **HCEN Central**: Para envío de metadatos y consulta de información
- **Servicio de Políticas**: Para verificación de permisos y solicitudes de acceso
- **MongoDB**: Para almacenamiento de contenido de documentos

## Referencias

- [README Principal](../README.md) - Guía completa del componente periférico
- [Guía de Compilación y Despliegue](../../../GUIA-COMPILACION-DESPLIEGUE.md) - Instrucciones detalladas
