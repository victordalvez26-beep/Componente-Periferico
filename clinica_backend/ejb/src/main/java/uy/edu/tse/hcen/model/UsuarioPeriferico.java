package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import uy.edu.tse.hcen.utils.PasswordUtils;

@Entity
public class UsuarioPeriferico extends Usuario {

    @Column(nullable = false, unique = true)
    private String nickname; // Atributo de UsuarioPeriferico

    @Column(name = "password_hash", nullable = false)
    private String passwordHash; // Almacenamos el hash, no la contraseña plana

    @Column(name = "tenant_id")
    private String tenantId; // Optional tenant id when the user is stored in public schema

    @Column(name = "role")
    private String role; // Optional explicit role stored in the global user row

    public UsuarioPeriferico() {
        /* Default constructor required by JPA/Hibernate; intentionally left empty.
           The persistence provider uses this constructor via reflection when instantiating entities. */
    }
    public void setPassword(String rawPassword) {
        this.passwordHash = PasswordUtils.hashPassword(rawPassword);
    }

    /**
     * Set an already-hashed password value (used when reading the stored hash
     * from the database and creating a detached instance).
     */
    public void setPasswordHash(String hash) {
        this.passwordHash = hash;
    }

    /**
     * Return the stored password hash (for debugging or read-only checks).
     */
    public String getPasswordHash() {
        return this.passwordHash;
    }

    public boolean checkPassword(String rawPassword) {
        return PasswordUtils.verifyPassword(rawPassword, this.passwordHash);
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


 
}
