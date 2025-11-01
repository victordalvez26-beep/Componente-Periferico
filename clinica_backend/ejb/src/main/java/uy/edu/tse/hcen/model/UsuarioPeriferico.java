package uy.edu.tse.hcen.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class UsuarioPeriferico extends Usuario {

    @Column(nullable = false, unique = true)
    private String nickname; // Atributo de UsuarioPeriferico

    @Column(nullable = false)
    private String password; // Atributo de UsuarioPeriferico (se recomienda hash)

    public UsuarioPeriferico() {
    }

    public UsuarioPeriferico(String nombre, String email, String nickname, String password) {
        super(nombre, email);
        this.nickname = nickname;
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
