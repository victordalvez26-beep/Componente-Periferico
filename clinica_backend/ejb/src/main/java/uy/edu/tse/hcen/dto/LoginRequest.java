package uy.edu.tse.hcen.dto;

public class LoginRequest {
    private String nickname;
    private String password;
    private String tenantId;  // ID de la clínica desde la URL

    public LoginRequest() {
    }

    public LoginRequest(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
    }

    public LoginRequest(String nickname, String password, String tenantId) {
        this.nickname = nickname;
        this.password = password;
        this.tenantId = tenantId;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
