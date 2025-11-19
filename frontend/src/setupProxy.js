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
      logLevel: 'debug'
    })
  );
};

