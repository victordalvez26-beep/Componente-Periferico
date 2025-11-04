const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  // Proxy TODAS las llamadas a /hcen-web/* hacia el backend periférico en puerto 8081
  app.use(
    '/hcen-web',
    createProxyMiddleware({
      target: 'http://localhost:8081',
      changeOrigin: true,
      logLevel: 'debug'
    })
  );
};

