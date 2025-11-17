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

    public LoginResponse authenticateAndGenerateToken(String nickname, String rawPassword) throws SecurityException {
    // Resolve authentication against the GLOBAL schema (public).
    tenantResolver.setTenantIdentifier(null);
    TenantContext.clear();

    LOG.debugf("TenantContext in LoginService before query: '%s'", TenantContext.getCurrentTenant());

    // 1) Buscar usuario en el schema por defecto (global)
    UsuarioPeriferico user = userRepository.findByNickname(nickname);

        // DEBUG: show stored hash and result of verification
        if (user != null) {
            LOG.debugf("Retrieved user id=%s, nickname=%s", user.getId(), user.getNickname());
            LOG.debugf("Stored password hash='%s'", user.getPasswordHash());
            boolean matches = PasswordUtils.verifyPassword(rawPassword, user.getPasswordHash());
            LOG.debugf("PasswordUtils.verifyPassword returned: %s", matches);
            if (!matches) {
                throw new SecurityException("Credenciales inválidas.");
            }
        } else {
            throw new SecurityException("Credenciales inválidas.");
        }

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

        // Determine tenant id for the token. Prefer explicit tenant_id stored in the
        // global public.usuarioperiferico row. Fall back to the heuristic lookup if
        // the stored value is missing.
        String tenantId = null;
        if (user.getTenantId() != null && !user.getTenantId().isBlank()) {
            tenantId = user.getTenantId();
        } else {
            tenantId = lookupTenantIdByNickname(nickname);
        }

        // Set the resolved tenant into the TenantContext for downstream calls
        if (tenantId != null) {
            TenantContext.setCurrentTenant(tenantId);
        }

        String token = TokenUtils.generateToken(nickname, role, tenantId);
        return new LoginResponse(token, role);
    }
}
