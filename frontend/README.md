# Nodo Periférico - Frontend (Admin)

Aplicación React para administración del componente periférico HCEN. Permite gestionar clínicas, profesionales, usuarios de salud y documentos clínicos.

## Configuración

### Variables de Entorno

El frontend usa la variable de entorno `REACT_APP_BACKEND_URL` para configurar la URL del backend.

**Desarrollo (con proxy):**
- Si no se define `REACT_APP_BACKEND_URL`, el frontend usa rutas relativas (`/hcen-web/api/...`) que son manejadas por el proxy configurado en `setupProxy.js`.
- El proxy redirige automáticamente a `http://localhost:8081` (o el valor configurado en `setupProxy.js`).

**Producción o desarrollo sin proxy:**
- Define `REACT_APP_BACKEND_URL` con la URL completa del backend:
  ```bash
  REACT_APP_BACKEND_URL=http://localhost:8081
  ```

### Archivo .env

Copia `.env.example` a `.env` y configura la variable:

```bash
cp .env.example .env
```

Luego edita `.env` y configura:
```
REACT_APP_BACKEND_URL=http://localhost:8081
```

## Inicio Rápido

1. Instalar dependencias:
```bash
npm install
```

2. (Opcional) Configurar `.env` con la URL del backend si no quieres usar el proxy.

3. Iniciar el servidor de desarrollo:
```bash
npm start
```

El frontend estará disponible en `http://localhost:3001` (o el puerto configurado).

## Arquitectura

- **Rutas relativas**: Si `REACT_APP_BACKEND_URL` no está definida, se usan rutas relativas que el proxy maneja.
- **URLs absolutas**: Si `REACT_APP_BACKEND_URL` está definida, se construyen URLs completas.
- **Helper centralizado**: Todas las llamadas API usan `src/utils/api.js` que maneja la construcción de URLs de forma consistente.
