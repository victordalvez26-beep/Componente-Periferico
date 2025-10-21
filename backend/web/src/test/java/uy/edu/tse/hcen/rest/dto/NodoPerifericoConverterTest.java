package uy.edu.tse.hcen.rest.dto;

import org.junit.jupiter.api.Test;

import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;

import static org.junit.jupiter.api.Assertions.*;

public class NodoPerifericoConverterTest {

    @Test
    public void testDtoToEntityAndBack() {
        NodoPerifericoDTO dto = new NodoPerifericoDTO();
        dto.setId(1L);
        dto.setNombre("Centro A");
        dto.setRUT("12345678-9");
        dto.setDepartamento("MONTEVIDEO");
        dto.setLocalidad("Ciudad");
        dto.setDireccion("Calle Falsa 123");
        dto.setContacto("contacto@example.com");
        dto.setUrl("http://example.com");
        dto.setEstado("ACTIVO");

        NodoPeriferico entity = NodoPerifericoConverter.toEntity(dto);
        assertNotNull(entity);
        assertEquals(dto.getId(), entity.getId());
        assertEquals(dto.getNombre(), entity.getNombre());
        assertEquals(dto.getRUT(), entity.getRUT());
        assertNotNull(entity.getEstado());
        assertEquals(EstadoNodoPeriferico.ACTIVO, entity.getEstado());
        assertEquals(dto.getDireccion(), entity.getDireccion());

        NodoPerifericoDTO back = NodoPerifericoConverter.toDTO(entity);
        assertNotNull(back);
        assertEquals(dto.getNombre(), back.getNombre());
        assertEquals(dto.getDepartamento(), back.getDepartamento());
        assertEquals(dto.getEstado(), back.getEstado());
    }
}
