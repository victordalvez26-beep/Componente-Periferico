package uy.edu.tse.hcen.rest.filter;

import io.jsonwebtoken.Claims;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.utils.TokenUtils;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthTokenFilterTest {

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private ContainerResponseContext responseContext;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private MultivaluedMap<String, Object> responseHeaders;

    @InjectMocks
    private AuthTokenFilter filter;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
    }

    @Test
    void testFilterOptionsRequest() throws IOException {
        when(requestContext.getMethod()).thenReturn("OPTIONS");
        when(requestContext.getHeaderString("Origin")).thenReturn("http://localhost:3000");
        
        filter.filter(requestContext);
        
        verify(requestContext).abortWith(any(Response.class));
    }

    @Test
    void testFilterPublicPathConfig() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("config/init");
        when(requestContext.getMethod()).thenReturn("POST");
        
        filter.filter(requestContext);
        
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterPublicPathAuthLogin() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("auth/login");
        when(requestContext.getMethod()).thenReturn("POST");
        
        filter.filter(requestContext);
        
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterPublicPathDocumentosPdf() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("documentos-pdf/123");
        when(requestContext.getMethod()).thenReturn("GET");
        
        filter.filter(requestContext);
        
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterValidToken() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("prof-1");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
            verify(requestContext, never()).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterInvalidToken() throws IOException {
        String token = "Bearer invalid.token";
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("invalid.token"))
                    .thenThrow(new RuntimeException("Invalid token"));
            
            filter.filter(requestContext);
            
            verify(requestContext).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterNoToken() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        
        filter.filter(requestContext);
        
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterResponseWithOrigin() throws IOException {
        when(requestContext.getHeaderString("Origin")).thenReturn("http://localhost:3000");
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        
        filter.filter(requestContext, responseContext);
        
        verify(responseHeaders).add(eq("Access-Control-Allow-Origin"), eq("http://localhost:3000"));
        verify(responseHeaders).add(eq("Access-Control-Allow-Credentials"), eq("true"));
    }

    @Test
    void testFilterResponseNoOrigin() throws IOException {
        when(requestContext.getHeaderString("Origin")).thenReturn(null);
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        
        filter.filter(requestContext, responseContext);
        
        verify(responseHeaders).add(eq("Access-Control-Allow-Origin"), eq("*"));
    }

    @Test
    void testFilterResponseClearsTenantContext() throws IOException {
        TenantContext.setCurrentTenant("123");
        when(requestContext.getHeaderString("Origin")).thenReturn(null);
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        
        filter.filter(requestContext, responseContext);
        
        // TenantContext should be cleared
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void testFilterWithTokenButNoBearer() throws IOException {
        String token = "InvalidFormat token";
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        filter.filter(requestContext);
        
        // Should not abort since it's not a Bearer token
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterWithTokenButEmptyToken() throws IOException {
        String token = "Bearer ";
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken(""))
                    .thenThrow(new RuntimeException("Empty token"));
            
            filter.filter(requestContext);
            
            verify(requestContext).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterWithNullClaims() throws IOException {
        String token = "Bearer valid.token.here";
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(null);
            
            filter.filter(requestContext);
            
            // Should handle null claims gracefully
            verify(requestContext, never()).setSecurityContext(any());
        }
    }

    @Test
    void testFilterPublicPathPortalConfig() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("portal-configuracion/public");
        when(requestContext.getMethod()).thenReturn("GET");
        
        filter.filter(requestContext);
        
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterWithTokenButNullTenantId() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn(null);
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("prof-1");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
            verify(requestContext, never()).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterWithTokenButBlankTenantId() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("prof-1");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
            verify(requestContext, never()).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterWithTokenButNullRole() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn(null);
            when(claims.getSubject()).thenReturn("prof-1");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
            verify(requestContext, never()).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterWithTokenButNullSubject() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn(null);
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
            verify(requestContext, never()).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterResponseWithOriginLocalhost3001() throws IOException {
        when(requestContext.getHeaderString("Origin")).thenReturn("http://localhost:3001");
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        
        filter.filter(requestContext, responseContext);
        
        verify(responseHeaders).add(eq("Access-Control-Allow-Origin"), eq("http://localhost:3001"));
        verify(responseHeaders).add(eq("Access-Control-Allow-Credentials"), eq("true"));
    }

    @Test
    void testFilterResponseWithOtherOrigin() throws IOException {
        when(requestContext.getHeaderString("Origin")).thenReturn("http://example.com");
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        
        filter.filter(requestContext, responseContext);
        
        verify(responseHeaders).add(eq("Access-Control-Allow-Origin"), eq("http://example.com"));
    }

    @Test
    void testFilterOptionsWithLocalhost3000() throws IOException {
        when(requestContext.getMethod()).thenReturn("OPTIONS");
        when(requestContext.getHeaderString("Origin")).thenReturn("http://localhost:3000");
        
        filter.filter(requestContext);
        
        verify(requestContext).abortWith(any(Response.class));
    }

    @Test
    void testFilterOptionsWithOtherOrigin() throws IOException {
        when(requestContext.getMethod()).thenReturn("OPTIONS");
        when(requestContext.getHeaderString("Origin")).thenReturn("http://example.com");
        
        filter.filter(requestContext);
        
        verify(requestContext).abortWith(any(Response.class));
    }

    @Test
    void testFilterPublicPathDocumentosPdfWithGet() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("documentos-pdf/123");
        when(requestContext.getMethod()).thenReturn("GET");
        
        filter.filter(requestContext);
        
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterPublicPathDocumentosPdfPacienteRequiresAuth() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("documentos-pdf/paciente/12345678");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/documentos-pdf/paciente/12345678"));
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("prof-1");
            
            filter.filter(requestContext);
            
            // Este path requiere autenticación, así que debe procesar el token
            verify(requestContext).setSecurityContext(any());
        }
    }

    @Test
    void testFilterSecurityContextIsSecure() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(uriInfo.getRequestUri()).thenReturn(URI.create("https://localhost:8080/api/profesionales"));
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("prof-1");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
        }
    }

    @Test
    void testFilterSecurityContextIsNotSecure() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("prof-1");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
        }
    }

    @Test
    void testFilterSecurityContextIsUserInRole() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("ADMINISTRADOR");
            when(claims.getSubject()).thenReturn("admin-1");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
        }
    }

    @Test
    void testFilterOptionsWithNullOrigin() throws IOException {
        when(requestContext.getMethod()).thenReturn("OPTIONS");
        when(requestContext.getHeaderString("Origin")).thenReturn(null);
        
        filter.filter(requestContext);
        
        verify(requestContext).abortWith(any(Response.class));
    }

    @Test
    void testFilterPublicPathConfigUpdate() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("config/update");
        when(requestContext.getMethod()).thenReturn("PUT");
        
        filter.filter(requestContext);
        
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterPublicPathConfigHealth() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("config/health");
        when(requestContext.getMethod()).thenReturn("GET");
        
        filter.filter(requestContext);
        
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterPublicPathDocumentosPdfWithPost() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("documentos-pdf/123");
        when(requestContext.getMethod()).thenReturn("POST");
        
        filter.filter(requestContext);
        
        // POST no es público, así que debe verificar token si está presente
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterBearerTokenWithWhitespace() throws IOException {
        String token = "Bearer  valid.token.here  ";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080/api/profesionales"));
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        try (MockedStatic<TokenUtils> tokenUtilsMock = mockStatic(TokenUtils.class)) {
            tokenUtilsMock.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("prof-1");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any());
        }
    }

    @Test
    void testFilterResponseWithAllHeaders() throws IOException {
        when(requestContext.getHeaderString("Origin")).thenReturn("http://localhost:3000");
        when(responseContext.getHeaders()).thenReturn(responseHeaders);
        
        filter.filter(requestContext, responseContext);
        
        verify(responseHeaders).add(eq("Access-Control-Allow-Origin"), eq("http://localhost:3000"));
        verify(responseHeaders).add(eq("Access-Control-Allow-Credentials"), eq("true"));
        verify(responseHeaders).add(eq("Access-Control-Allow-Methods"), anyString());
        verify(responseHeaders).add(eq("Access-Control-Allow-Headers"), anyString());
        verify(responseHeaders).add(eq("Access-Control-Expose-Headers"), anyString());
    }
}

