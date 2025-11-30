package uy.edu.tse.hcen.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uy.edu.tse.hcen.context.TenantContext;
import uy.edu.tse.hcen.multitenancy.SchemaTenantResolver;
import uy.edu.tse.hcen.utils.TokenUtils;

import java.io.IOException;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantAuthFilterTest {

    @Mock
    private SchemaTenantResolver tenantResolver;

    @Mock
    private TenantContext tenantContext;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private UriInfo uriInfo;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private TenantAuthFilter filter;

    @BeforeEach
    void setUp() {
        uy.edu.tse.hcen.multitenancy.TenantContext.clear();
    }

    @Test
    void testFilterLoginPath() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/auth/login");
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        filter.filter(requestContext);
        
        verify(requestContext).setSecurityContext(any(SecurityContext.class));
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterConfigPath() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("config/init");
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        filter.filter(requestContext);
        
        verify(requestContext).setSecurityContext(any(SecurityContext.class));
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterPdfDownload() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/documentos-pdf/123");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        filter.filter(requestContext);
        
        verify(requestContext).setSecurityContext(any(SecurityContext.class));
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterNoAuthorizationHeader() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        
        filter.filter(requestContext);
        
        verify(requestContext).abortWith(any(Response.class));
    }

    @Test
    void testFilterInvalidTokenFormat() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn("Invalid token");
        
        filter.filter(requestContext);
        
        verify(requestContext).abortWith(any(Response.class));
    }

    @Test
    void testFilterValidToken() throws IOException {
        String token = "Bearer valid.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        // Mock TokenUtils.parseToken
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("valid.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("testuser");
            
            filter.filter(requestContext);
            
            verify(tenantResolver).setTenantIdentifier("123");
            verify(tenantContext).setTenantId("123");
            verify(tenantContext).setRole("PROFESIONAL");
            verify(tenantContext).setNickname("testuser");
            verify(requestContext).setSecurityContext(any(SecurityContext.class));
            verify(requestContext, never()).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterServiceToken() throws IOException {
        String token = "Bearer service.token.here";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("service.token.here")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn(null);
            when(claims.get("serviceName", String.class)).thenReturn("componente-periferico");
            when(claims.getIssuer()).thenReturn("HCEN-Service");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any(SecurityContext.class));
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
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("invalid.token"))
                    .thenThrow(new JwtException("Invalid token"));
            
            filter.filter(requestContext);
            
            verify(requestContext).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterConfigHealthPath() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("config/health");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        filter.filter(requestContext);
        
        verify(requestContext).setSecurityContext(any(SecurityContext.class));
        verify(requestContext, never()).abortWith(any(Response.class));
    }

    @Test
    void testFilterPdfDownloadWithPacientePath() throws IOException {
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/documentos-pdf/paciente/12345678");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        
        filter.filter(requestContext);
        
        // No debe permitir acceso sin token para /documentos-pdf/paciente/
        verify(requestContext).abortWith(any(Response.class));
    }

    @Test
    void testFilterTokenWithEmptyTenantId() throws IOException {
        String token = "Bearer token.without.tenant";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("token.without.tenant")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("");
            when(claims.get("serviceName", String.class)).thenReturn(null);
            when(claims.getIssuer()).thenReturn(null);
            
            filter.filter(requestContext);
            
            verify(requestContext).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterTokenWithServiceIssuer() throws IOException {
        String token = "Bearer service.token";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("service.token")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn(null);
            when(claims.get("serviceName", String.class)).thenReturn(null);
            when(claims.getIssuer()).thenReturn("HCEN-Service");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any(SecurityContext.class));
            verify(requestContext, never()).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterTokenWithNullRole() throws IOException {
        String token = "Bearer valid.token";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("valid.token")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn(null);
            when(claims.getSubject()).thenReturn("testuser");
            
            filter.filter(requestContext);
            
            verify(tenantResolver).setTenantIdentifier("123");
            verify(tenantContext).setTenantId("123");
            verify(tenantContext).setRole(null);
            verify(requestContext).setSecurityContext(any(SecurityContext.class));
        }
    }

    @Test
    void testFilterTokenWithNullNickname() throws IOException {
        String token = "Bearer valid.token";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("valid.token")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn(null);
            
            filter.filter(requestContext);
            
            verify(tenantContext).setNickname(null);
            verify(requestContext).setSecurityContext(any(SecurityContext.class));
        }
    }

    @Test
    void testFilterTokenWithException() throws IOException {
        String token = "Bearer token";
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("token"))
                    .thenThrow(new RuntimeException("Unexpected error"));
            
            filter.filter(requestContext);
            
            verify(requestContext).abortWith(any(Response.class));
        }
    }

    @Test
    void testFilterPdfDownloadWithDifferentPaths() throws IOException {
        String[] pdfPaths = {
            "/hcen-web/api/documentos-pdf/123",
            "/api/documentos-pdf/456",
            "documentos-pdf/789"
        };
        
        for (String path : pdfPaths) {
            when(requestContext.getUriInfo()).thenReturn(uriInfo);
            when(uriInfo.getPath()).thenReturn(path);
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getSecurityContext()).thenReturn(securityContext);
            when(securityContext.isSecure()).thenReturn(true);
            
            filter.filter(requestContext);
            
            verify(requestContext, atLeastOnce()).setSecurityContext(any(SecurityContext.class));
            reset(requestContext);
        }
    }

    @Test
    void testFilterSecurityContextIsUserInRole() throws IOException {
        String token = "Bearer valid.token";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(requestContext.getSecurityContext()).thenReturn(securityContext);
        when(securityContext.isSecure()).thenReturn(true);
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("valid.token")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("ADMINISTRADOR");
            when(claims.getSubject()).thenReturn("admin");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(argThat(sc -> {
                SecurityContext ctx = (SecurityContext) sc;
                assertTrue(ctx.isUserInRole("ADMINISTRADOR"));
                assertFalse(ctx.isUserInRole("PROFESIONAL"));
                assertFalse(ctx.isUserInRole(null));
                return true;
            }));
        }
    }

    @Test
    void testFilterSecurityContextWithNullPrevious() throws IOException {
        String token = "Bearer valid.token";
        Claims claims = mock(Claims.class);
        
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("/api/profesionales");
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(token);
        when(requestContext.getSecurityContext()).thenReturn(null);
        
        try (var mockedStatic = mockStatic(TokenUtils.class)) {
            mockedStatic.when(() -> TokenUtils.parseToken("valid.token")).thenReturn(claims);
            when(claims.get("tenantId", String.class)).thenReturn("123");
            when(claims.get("role", String.class)).thenReturn("PROFESIONAL");
            when(claims.getSubject()).thenReturn("user");
            
            filter.filter(requestContext);
            
            verify(requestContext).setSecurityContext(any(SecurityContext.class));
        }
    }
}

