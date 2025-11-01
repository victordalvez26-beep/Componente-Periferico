package uy.edu.tse.hcen.dto;

import uy.edu.tse.hcen.model.UsuarioPeriferico;

public class UsuarioPerifericoDTO extends UsuarioDTO {

    private String nickname;
    private String password;

    public UsuarioPerifericoDTO() {
        super();
    }

    public UsuarioPerifericoDTO(Long id, String nombre, String email, String nickname, String password) {
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
    public static UsuarioPerifericoDTO fromEntity(UsuarioPeriferico u) {
        if (u == null) return null;
        return new UsuarioPerifericoDTO(u.getId(), u.getNombre(), u.getEmail(), u.getNickname(), u.getPassword());
    }

    /**
     * Convert this DTO back to an entity. Note: password should be hashed before persisting in production.
     */
    public UsuarioPeriferico toEntity() {
        UsuarioPeriferico u = new UsuarioPeriferico(getNombre(), getEmail(), this.nickname, this.password);
        if (getId() != null) u.setId(getId());
        return u;
    }
}
