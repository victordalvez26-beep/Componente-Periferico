package uy.edu.tse.hcen.auth;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Very small internal authentication service for development/demo purposes.
 * Notes:
 * - Passwords are stored as PBKDF2 hashes in-memory here. For production use a
 *   proper persistent store (database), use BCrypt or Argon2, add salt per user,
 *   and move authentication to a dedicated identity provider (OpenID Connect / OAuth2).
 * - This service exposes minimal operations used by the web layer.
 */
@Singleton
public class AuthService {
    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    // username -> user
    private final Map<String, User> users = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // For demonstration we create one admin user with password 'admin'.
        // Password hash generated with PBKDF2 (see verifyPassword)
        String adminHash = PasswordUtil.generatePBKDF2Hash("admin");
        users.put("admin", new User("admin", adminHash, "Administrador Local", new String[]{"ADMIN"}));

        // Another example professional user
        String profHash = PasswordUtil.generatePBKDF2Hash("prof123");
        users.put("prof1", new User("prof1", profHash, "Profesional", new String[]{"PROFESIONAL"}));

        LOG.info("AuthService initialized with sample users (development only)");
    }

    @Lock(LockType.READ)
    public UserDTO authenticate(String username, String password) {
        User u = users.get(username);
        if (u == null) return null;
        if (PasswordUtil.verifyPBKDF2(password, u.getPasswordHash())) {
            return new UserDTO(u.getUsername(), u.getNombre(), u.getRoles());
        }
        return null;
    }

    // For demo/testing only: allow adding users programmatically
    @Lock(LockType.WRITE)
    public void addUser(String username, String rawPassword, String nombre, String[] roles) {
        String h = PasswordUtil.generatePBKDF2Hash(rawPassword);
        users.put(username, new User(username, h, nombre, roles));
    }
}
