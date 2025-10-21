package com.clinica.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "clinicas")
public class Clinica implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", unique = true, nullable = false)
    private String tenantId; // Usaremos este como el identificador de tenencia en el JWT

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "habilitada", nullable = false)
    private boolean habilitada = true;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public boolean isHabilitada() { return habilitada; }
    public void setHabilitada(boolean habilitada) { this.habilitada = habilitada; }
}
