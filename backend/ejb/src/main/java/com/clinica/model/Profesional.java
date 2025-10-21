package com.clinica.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "profesionales")
// TODO: usar filtros de Hibernate para centralizar el aislamiento por tenant.
// Requiere dependencias de Hibernate y configuración en el runtime.
// @FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = "string"))
// @Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
 // Nota: para activar el filtro en tiempo de ejecución, desde el EntityManager/Hibernate Session:
 // session.enableFilter("tenantFilter").setParameter("tenantId", currentTenantId);
public class Profesional implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //  Aísla al profesional por clínica
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;

    @Column(name = "matricula", unique = true, nullable = false)
    private String matricula;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "especialidad")
    private String especialidad;
    
    @Column(name = "usuario_gubuy")
    private String usuarioGubUy; 

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEspecialidad() { return especialidad; }
    public void setEspecialidad(String especialidad) { this.especialidad = especialidad; }
    
}
