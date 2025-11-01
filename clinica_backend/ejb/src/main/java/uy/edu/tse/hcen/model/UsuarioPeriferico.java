package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import uy.edu.tse.hcen.utils.PasswordUtils;

@Entity
public class UsuarioPeriferico extends Usuario {

    @Column(nullable = false, unique = true)
    private String nickname; // Atributo de UsuarioPeriferico

    @Column(nullable = false)
    private String passwordHash; // Almacenamos el hash, no la contraseña plana

    public UsuarioPeriferico() {
        /* Default constructor required by JPA/Hibernate; intentionally left empty.
           The persistence provider uses this constructor via reflection when instantiating entities. */
    }
    public void setPassword(String rawPassword) {
        this.passwordHash = PasswordUtils.hashPassword(rawPassword);
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


 
}
