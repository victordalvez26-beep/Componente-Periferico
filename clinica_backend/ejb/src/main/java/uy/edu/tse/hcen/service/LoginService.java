package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.multitenancy.SchemaTenantResolver;
import uy.edu.tse.hcen.repository.UsuarioPerifericoRepository;
import uy.edu.tse.hcen.utils.TokenUtils;
import uy.edu.tse.hcen.model.ProfesionalSalud;
import uy.edu.tse.hcen.model.AdministradorClinica;
import uy.edu.tse.hcen.model.UsuarioPeriferico;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class LoginService {

    @Inject
    private UsuarioPerifericoRepository userRepository;

    @Inject
    private SchemaTenantResolver tenantResolver;

    // Public no-arg constructor required for CDI proxyability
    public LoginService() {
    }

    // Lógica ficticia para determinar tenantId a partir del nickname.
    private String lookupTenantIdByNickname(String nickname) {
        if (nickname == null) return null;
        if (nickname.contains("c1")) return "1";
        if (nickname.contains("c2")) return "2";
        return null;
    }

    public String authenticateAndGenerateToken(String nickname, String rawPassword) throws SecurityException {
        String tenantId = lookupTenantIdByNickname(nickname);
        if (tenantId == null) {
            throw new SecurityException("Usuario no encontrado.");
        }

        // 1) Establecer el tenant antes de consultar la BD
        tenantResolver.setTenantIdentifier(tenantId);

        // 2) Buscar usuario en el schema correspondiente
        UsuarioPeriferico user = userRepository.findByNickname(nickname);

        if (user == null || !user.checkPassword(rawPassword)) {
            throw new SecurityException("Credenciales inválidas.");
        }

        String role;
        if (user instanceof ProfesionalSalud) {
            role = "PROFESIONAL";
        } else if (user instanceof AdministradorClinica) {
            role = "ADMINISTRADOR";
        } else {
            role = "OTRO";
        }

        return TokenUtils.generateToken(nickname, role, tenantId);
    }
}
