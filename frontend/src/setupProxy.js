const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // Proxy TODAS las llamadas a /hcen-web/* hacia el backend periférico
  // Usa variable de entorno o valor por defecto
  const backendUrl = process.env.REACT_APP_BACKEND_URL || 'http://localhost:8081';
  
  app.use(
    '/hcen-web',
    createProxyMiddleware({
      target: backendUrl,
      changeOrigin: true,
      logLevel: 'debug',
      timeout: 180000, // 3 minutos de timeout para operaciones largas como activación
      proxyTimeout: 180000,
      // Configurar timeouts más largos para operaciones pesadas
      xfwd: true,
      secure: false, // Para desarrollo local
      // NO remover /hcen-web porque es el context root de la aplicación web
      onProxyReq: (proxyReq, req, res) => {
        const fullUrl = `${backendUrl}${req.url}`;
        console.log(`[Proxy] ${req.method} ${req.originalUrl} -> ${fullUrl}`);
        // Aumentar timeout del request
        proxyReq.setTimeout(180000);
        // Agregar headers útiles
        proxyReq.setHeader('X-Forwarded-For', req.ip || req.connection.remoteAddress);
        proxyReq.setHeader('X-Forwarded-Proto', req.protocol || 'http');
      },
      onProxyRes: (proxyRes, req, res) => {
        console.log(`[Proxy] Respuesta: ${proxyRes.statusCode} para ${req.originalUrl}`);
        // Aumentar timeout de la respuesta
        res.setTimeout(180000);
      },
      onError: (err, req, res) => {
        console.error('[Proxy] Error:', err.message);
        console.error(`[Proxy] No se pudo conectar a ${backendUrl}`);
        console.error(`[Proxy] Verifica que el backend esté corriendo en ${backendUrl}`);
        if (!res.headersSent) {
          // Si es un timeout, devolver 504
          if (err.code === 'ETIMEDOUT' || err.code === 'ECONNRESET') {
            res.status(504).json({ 
              error: 'Gateway Timeout', 
              message: `El backend en ${backendUrl} no respondió a tiempo. Verifica que esté corriendo y accesible.`,
              details: err.message
            });
          } else {
            res.status(503).json({ 
              error: 'Backend service unavailable', 
              message: `Cannot connect to ${backendUrl}. Please ensure the backend is running.`,
              details: err.message
            });
          }
        }
      }
    })
  );
};

