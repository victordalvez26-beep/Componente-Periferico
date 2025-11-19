package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.UsuarioPeriferico;

public class DTUsuarioPeriferico extends DTUsuario {

    private String nickname;
    private String password;

    public DTUsuarioPeriferico() {
        super();
    }

    public DTUsuarioPeriferico(Long id, String nombre, String email, String nickname, String password) {
        super(id, nombre, email);
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

    /**
     * Create a DTO from an entity instance.
     */
    public static DTUsuarioPeriferico fromEntity(UsuarioPeriferico u) {
        if (u == null) return null;
        // For security reasons the plain password is not available from the entity.
        // We return a DTO without the password field populated.
        return new DTUsuarioPeriferico(u.getId(), u.getNombre(), u.getEmail(), u.getNickname(), null);
    }

    /**
     * Convert this DTO back to an entity. Note: password should be hashed before persisting in production.
     */
    public UsuarioPeriferico toEntity() {
        UsuarioPeriferico u = new UsuarioPeriferico();
        if (getId() != null) u.setId(getId());
        u.setNombre(getNombre());
        u.setEmail(getEmail());
        u.setNickname(this.nickname);
        // If a raw password was provided in the DTO, hash it via the entity setter
        if (this.password != null) {
            u.setPassword(this.password);
        }
        return u;
    }
}
