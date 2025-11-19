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
public class LoginService {

    @Inject
    private UsuarioPerifericoRepository userRepository;

    @Inject
    private SchemaTenantResolver tenantResolver;

    private static final Logger LOG = Logger.getLogger(LoginService.class);

    // Public no-arg constructor required for CDI proxyability
    public LoginService() {
    }

    // Lógica ficticia para determinar tenantId a partir del nickname.
    private String lookupTenantIdByNickname(String nickname) {
        if (nickname == null) return null;
        // map the nickname suffix to the tenant identifiers created in the DB (101/102)
        if (nickname.contains("c1")) return "101";
        if (nickname.contains("c2")) return "102";
        return null;
    }

    public LoginResponse authenticateAndGenerateToken(String nickname, String rawPassword, String tenantId) throws SecurityException {
    UsuarioPeriferico user = null;
    String actualTenantId = null;
    
    // Estrategia de búsqueda:
    // 1. Buscar primero en public (admins globales)
    // 2. Si no encuentra Y hay tenantId, buscar en schema del tenant (profesionales)
    
    LOG.debugf("Login attempt for nickname=%s, tenantId=%s", nickname, tenantId);
    
    // 1) Buscar en schema público (admins)
    tenantResolver.setTenantIdentifier("public");
    TenantContext.clear();
    user = userRepository.findByNicknameForLogin(nickname);
    
    if (user != null) {
        System.out.println("=== Usuario encontrado en public.usuarioperiferico (ADMIN)");
        actualTenantId = user.getTenantId();
    } else if (tenantId != null && !tenantId.isBlank()) {
        // 2) Buscar en schema del tenant (profesionales)
        System.out.println("=== No encontrado en public, buscando en schema_clinica_" + tenantId);
        String schemaName = "schema_clinica_" + tenantId;
        
        // Usar query nativa para evitar problemas con herencia JOINED
        user = userRepository.findByNicknameInTenantSchema(nickname, schemaName);
        
        if (user != null) {
            System.out.println("=== Usuario encontrado en schema_clinica_" + tenantId + " (PROFESIONAL)");
            actualTenantId = tenantId;
            // Setear el tenant en el contexto
            tenantResolver.setTenantIdentifier(tenantId);
            TenantContext.setCurrentTenant(tenantId);
        }
    }

        // Validar credenciales
        if (user == null) {
            System.out.println("[LoginService] Usuario no encontrado: " + nickname);
            LOG.debugf("Usuario no encontrado: %s", nickname);
            throw new SecurityException("Credenciales inválidas.");
        }
        
        System.out.println("[LoginService] Usuario encontrado - ID: " + user.getId() + ", Hash: " + (user.getPasswordHash() != null ? user.getPasswordHash().substring(0, Math.min(20, user.getPasswordHash().length())) + "..." : "null"));
        String storedHash = user.getPasswordHash();
        System.out.println("[LoginService] Verificando contraseña...");
        boolean matches = PasswordUtils.verifyPassword(rawPassword, storedHash);
        System.out.println("[LoginService] Resultado verificación: " + matches);
        
        if (!matches) {
            System.out.println("[LoginService] Contraseña incorrecta para usuario: " + nickname);
            LOG.debugf("Contraseña incorrecta para usuario: %s", nickname);
            throw new SecurityException("Credenciales inválidas.");
        }
        
        System.out.println("[LoginService] Credenciales válidas para: " + nickname);

        // Determine role for token. Prefer an explicit stored role when present
        // (added to public.usuarioperiferico). Fallback to instanceof checks.
        String role = null;
        if (user.getRole() != null && !user.getRole().isBlank()) {
            role = user.getRole();
        } else if (user instanceof ProfesionalSalud) {
            role = "PROFESIONAL";
        } else if (user instanceof AdministradorClinica) {
            role = "ADMINISTRADOR";
        } else {
            role = "OTRO";
        }

        // Usar el tenant_id que ya determinamos al buscar el usuario
        // Set the resolved tenant into the TenantContext for downstream calls
        if (actualTenantId != null) {
            TenantContext.setCurrentTenant(actualTenantId);
        }

        String token = TokenUtils.generateToken(nickname, role, actualTenantId);
        return new LoginResponse(token, role, actualTenantId);
    }
}
