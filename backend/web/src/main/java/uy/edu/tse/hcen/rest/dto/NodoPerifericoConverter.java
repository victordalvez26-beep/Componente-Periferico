package uy.edu.tse.hcen.rest.dto;

import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.model.enums.Departamento;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

public class NodoPerifericoConverter {

    public static NodoPeriferico toEntity(NodoPerifericoDTO dto) {
        if (dto == null) return null;
        NodoPeriferico e = new NodoPeriferico();
        // dto.id is Long (internal DB id). Map directly.
        e.setId(dto.getId());
        e.setNombre(dto.getNombre());
        e.setRUT(dto.getRUT());
        if (dto.getDepartamento() != null) {
            e.setDepartamento(Departamento.valueOf(dto.getDepartamento()));
        }
        e.setLocalidad(dto.getLocalidad());
        e.setDireccion(dto.getDireccion());
        e.setContacto(dto.getContacto());
        e.setUrl(dto.getUrl());
            e.setNodoPerifericoUrlBase(dto.getNodoPerifericoUrlBase());
            e.setNodoPerifericoUsuario(dto.getNodoPerifericoUsuario());
            e.setNodoPerifericoPassword(dto.getNodoPerifericoPassword());
            if (dto.getEstado() != null) {
                e.setEstado(EstadoNodoPeriferico.valueOf(dto.getEstado()));
            }
        return e;
    }

    public static NodoPerifericoDTO toDTO(NodoPeriferico e) {
        if (e == null) return null;
        NodoPerifericoDTO dto = new NodoPerifericoDTO();
    dto.setId(e.getId() != null ? e.getId() : null);
        dto.setNombre(e.getNombre());
        dto.setRUT(e.getRUT());
        if (e.getDepartamento() != null) dto.setDepartamento(e.getDepartamento().name());
        dto.setLocalidad(e.getLocalidad());
        dto.setDireccion(e.getDireccion());
        dto.setContacto(e.getContacto());
        dto.setUrl(e.getUrl());
        dto.setNodoPerifericoUrlBase(e.getNodoPerifericoUrlBase());
        dto.setNodoPerifericoUsuario(e.getNodoPerifericoUsuario());
            if (e.getEstado() != null) dto.setEstado(e.getEstado().name());
        return dto;
    }
}
