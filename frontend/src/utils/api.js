/**
 * Utilidad centralizada para construir URLs de API
 * 
 * Si REACT_APP_BACKEND_URL está definida, se usa directamente.
 * Si no, se usa ruta relativa para que el proxy (setupProxy.js) la maneje.
 */
export const getApiUrl = (endpoint) => {
  const backendBase = process.env.REACT_APP_BACKEND_URL || '';
  
  // Si endpoint ya incluye el protocolo, devolverlo tal cual
  if (endpoint.startsWith('http://') || endpoint.startsWith('https://')) {
    return endpoint;
  }
  
  // Si hay backendBase, construir URL completa
  if (backendBase) {
    // Asegurar que endpoint empiece con /
    const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    // Asegurar que backendBase no termine con /
    const normalizedBase = backendBase.endsWith('/') ? backendBase.slice(0, -1) : backendBase;
    return `${normalizedBase}${normalizedEndpoint}`;
  }
  
  // Si no hay backendBase, usar ruta relativa (el proxy la manejará)
  return endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
};

