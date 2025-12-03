/**
 * Obtiene la URL base del backend desde la configuración en tiempo de ejecución.
 * Prioridad:
 * 1. window.APP_CONFIG.BACKEND_URL (configuración en tiempo de ejecución)
 * 2. process.env.REACT_APP_BACKEND_URL (variable de entorno en build)
 * 3. Valor por defecto
 */
export function getBackendUrl(defaultValue = 'http://localhost:8081') {
  if (window.APP_CONFIG && window.APP_CONFIG.BACKEND_URL) {
    return window.APP_CONFIG.BACKEND_URL;
  }
  if (process.env.REACT_APP_BACKEND_URL) {
    return process.env.REACT_APP_BACKEND_URL;
  }
  return defaultValue;
}

