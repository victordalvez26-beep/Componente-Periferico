package uy.edu.tse.hcen.dto;

public class LoginResponse {
    private String token;
    private String role;
    private String tenant_id;

    public LoginResponse() {
    }

    public LoginResponse(String token, String role) {
        this.token = token;
        this.role = role;
    }

    public LoginResponse(String token, String role, String tenantId) {
        this.token = token;
        this.role = role;
        this.tenant_id = tenantId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTenant_id() {
        return tenant_id;
    }

    public void setTenant_id(String tenant_id) {
        this.tenant_id = tenant_id;
    }
}
