package uy.edu.tse.hcen.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filtro de servlet para manejar CORS a nivel de servlet.
 * Se ejecuta antes que los filtros JAX-RS para asegurar que las peticiones OPTIONS
 * se manejen correctamente.
 * 
 * Este filtro está registrado en web.xml para asegurar que se ejecute.
 */
public class CORSFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String origin = httpRequest.getHeader("Origin");
        String method = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();
        
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(CORSFilter.class.getName());
        logger.info(String.format("CORSFilter - Method: %s, URI: %s, Origin: %s", method, requestURI, origin));
        
        // Agregar headers CORS a todas las respuestas
        if (origin != null) {
            // Permitir cualquier origen de localhost para desarrollo
            if (origin.startsWith("http://localhost:") || origin.startsWith("http://127.0.0.1:")) {
                httpResponse.setHeader("Access-Control-Allow-Origin", origin);
                httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
                logger.info(String.format("CORSFilter - Setting CORS headers for localhost origin: %s", origin));
            } else {
                // Para otros orígenes, también permitir (desarrollo)
                httpResponse.setHeader("Access-Control-Allow-Origin", origin);
                httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
                logger.info(String.format("CORSFilter - Setting CORS headers for origin: %s", origin));
            }
        } else {
            // Si no hay Origin header, permitir cualquier origen (solo desarrollo)
            httpResponse.setHeader("Access-Control-Allow-Origin", "*");
            logger.info("CORSFilter - No origin header, setting Access-Control-Allow-Origin: *");
        }
        
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
        httpResponse.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, Accept, Origin");
        httpResponse.setHeader("Access-Control-Expose-Headers", "Content-Type, Authorization");
        httpResponse.setHeader("Access-Control-Max-Age", "3600");
        
        // Manejar peticiones OPTIONS (preflight) - responder inmediatamente sin pasar al siguiente filtro
        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.info(String.format("CORSFilter - Handling OPTIONS preflight for: %s", requestURI));
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            httpResponse.flushBuffer();
            return; // No continuar con la cadena de filtros
        }
        
        // Continuar con la cadena de filtros para otras peticiones
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
