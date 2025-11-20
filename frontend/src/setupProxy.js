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
  
  // Proxy para /nodo-periferico/* (usado por ClinicAdmin.js)
  app.use(
    '/nodo-periferico',
    createProxyMiddleware({
      target: 'http://localhost:8081',
      changeOrigin: true,
      logLevel: 'debug',
      pathRewrite: {
        '^/nodo-periferico': '/hcen-web' // Reescribir /nodo-periferico a /hcen-web
      }
    })
  );
};

