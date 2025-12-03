package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.LoginResponse;

/**
 * Interfaz para el servicio de autenticación y generación de tokens.
 * Permite hacer mock de LoginService en tests sin problemas con anotaciones CDI.
 */
public interface ILoginService {
    
    /**
     * Autentica un usuario y genera un token JWT.
     * 
     * @param nickname Nombre de usuario (nickname)
     * @param rawPassword Contraseña en texto plano
     * @param tenantId ID del tenant (clínica)
     * @return LoginResponse con el token, rol y tenant ID
     * @throws SecurityException Si las credenciales son inválidas
     */
    LoginResponse authenticateAndGenerateToken(String nickname, String rawPassword, String tenantId) throws SecurityException;
}

