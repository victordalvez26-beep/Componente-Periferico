package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.multitenancy.SchemaTenantResolver;
import uy.edu.tse.hcen.multitenancy.TenantContext;
import uy.edu.tse.hcen.repository.UsuarioPerifericoRepository;
import uy.edu.tse.hcen.utils.TokenUtils;
import uy.edu.tse.hcen.utils.PasswordUtils;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.AdministradorClinica;
import uy.edu.tse.hcen.model.UsuarioPeriferico;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import uy.edu.tse.hcen.dto.LoginResponse;

@RequestScoped
public class LoginService implements ILoginService {

    @Inject
    private UsuarioPerifericoRepository userRepository;

    @Inject
    private SchemaTenantResolver tenantResolver;

    private static final Logger LOG = Logger.getLogger(LoginService.class);
    
    // Constantes para literales duplicados
    private static final String ROLE_PROFESIONAL = "PROFESIONAL";
    private static final String ROLE_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROLE_OTRO = "OTRO";
    private static final String SCHEMA_PREFIX = "schema_clinica_";

    // Public no-arg constructor required for CDI proxyability
    public LoginService() {
        // Constructor vacío requerido por CDI para crear proxies
    }



    public LoginResponse authenticateAndGenerateToken(String nickname, String rawPassword, String tenantId) throws SecurityException {
        LOG.debugf("Login attempt for nickname=%s, tenantId=%s", nickname, tenantId);
        
        // Buscar usuario en public o en tenant
        UserSearchResult searchResult = buscarUsuario(nickname, tenantId);
        
        // Validar credenciales
        validarCredenciales(searchResult.usuario, rawPassword);
        
        // Determinar role para el token
        String role = determinarRole(searchResult.usuario);
        
        // Configurar tenant context para llamadas downstream
        if (searchResult.tenantId != null) {
            TenantContext.setCurrentTenant(searchResult.tenantId);
        }
        
        String token = TokenUtils.generateToken(nickname, role, searchResult.tenantId);
        return new LoginResponse(token, role, searchResult.tenantId);
    }

    private UserSearchResult buscarUsuario(String nickname, String tenantId) throws SecurityException {
        // 1) Buscar en schema público (admins)
        tenantResolver.setTenantIdentifier("public");
        TenantContext.clear();
        UsuarioPeriferico user = userRepository.findByNicknameForLogin(nickname);
        
        if (user != null) {
            LOG.debugf("Usuario encontrado en public.usuarioperiferico (ADMIN)");
            return new UserSearchResult(user, user.getTenantId());
        }
        
        // 2) Buscar en schema del tenant (profesionales)
        if (tenantId != null && !tenantId.isBlank()) {
            user = buscarEnSchemaTenant(nickname, tenantId);
            if (user != null) {
                LOG.debugf("Usuario encontrado en schema_clinica_%s (PROFESIONAL)", tenantId);
                configurarTenantContext(tenantId);
                return new UserSearchResult(user, tenantId);
            }
        }
        
        LOG.debug("LoginService: User NOT found");
        throw new SecurityException("Credenciales inválidas.");
    }

    private UsuarioPeriferico buscarEnSchemaTenant(String nickname, String tenantId) {
        LOG.debugf("No encontrado en public, buscando en schema_clinica_%s", tenantId);
        String schemaName = SCHEMA_PREFIX + tenantId;
        return userRepository.findByNicknameInTenantSchema(nickname, schemaName);
    }

    private void configurarTenantContext(String tenantId) {
        tenantResolver.setTenantIdentifier(tenantId);
        TenantContext.setCurrentTenant(tenantId);
    }

    private void validarCredenciales(UsuarioPeriferico user, String rawPassword) throws SecurityException {
        LOG.debugf("LoginService: User found: %s", user.getNickname());
        LOG.debugf("LoginService: User ID: %d", user.getId());
        
        String storedHash = user.getPasswordHash();
        logPasswordDebugInfo(storedHash, rawPassword);
        
        boolean matches = PasswordUtils.verifyPassword(rawPassword, storedHash);
        LOG.debugf("LoginService: Password matches: %b", matches);
        
        if (!matches) {
            throw new SecurityException("Credenciales inválidas.");
        }
    }

    private void logPasswordDebugInfo(String storedHash, String rawPassword) {
        if (storedHash != null) {
            LOG.debugf("LoginService: Stored hash: %s", 
                storedHash.substring(0, Math.min(20, storedHash.length())) + "...");
            LOG.debugf("LoginService: Hash length: %d", storedHash.length());
        } else {
            LOG.debugf("LoginService: Stored hash: NULL");
        }
        LOG.debugf("LoginService: Raw password length: %d", rawPassword.length());
    }

    private String determinarRole(UsuarioPeriferico user) {
        // Preferir role explícito almacenado
        if (user.getRole() != null && !user.getRole().isBlank()) {
            return user.getRole();
        }
        
        // Fallback a instanceof checks
        if (user instanceof ProfesionalSalud) {
            return ROLE_PROFESIONAL;
        }
        if (user instanceof AdministradorClinica) {
            return ROLE_ADMINISTRADOR;
        }
        return ROLE_OTRO;
    }

    // Clase interna para encapsular resultado de búsqueda
    private static class UserSearchResult {
        final UsuarioPeriferico usuario;
        final String tenantId;

        UserSearchResult(UsuarioPeriferico usuario, String tenantId) {
            this.usuario = usuario;
            this.tenantId = tenantId;
        }
    }
}
