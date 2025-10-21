package uy.edu.tse.hcen.rest;

import jakarta.ejb.EJB;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.model.enums.Departamento;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;
import uy.edu.tse.hcen.repository.NodoPerifericoRepository;
import uy.edu.tse.hcen.rest.dto.NodoPerifericoConverter;
import uy.edu.tse.hcen.rest.dto.NodoPerifericoDTO;

import java.util.List;

import java.util.stream.Collectors;


@Path("/nodos")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class NodoPerifericoResource {

    @EJB
    private NodoPerifericoRepository repo;

    @EJB
    private uy.edu.tse.hcen.service.NodoService nodoService;


    @GET
    public List<NodoPerifericoDTO> list() {
        return repo.findAll().stream().map(NodoPerifericoConverter::toDTO).collect(Collectors.toList());
    }

    @GET
    @Path("{rut}")
    public Response get(@PathParam("rut") String rut) {
        NodoPeriferico n = repo.findByRUT(rut);
        if (n == null) return Response.status(Response.Status.NOT_FOUND).build();
        return Response.ok(NodoPerifericoConverter.toDTO(n)).build();
    }

    @POST
    public Response create(@Valid NodoPerifericoDTO nodoDto) {
        try {

            validateEnumValues(nodoDto);
            // check duplicate RUT
            if (nodoDto.getRUT() != null && repo.findByRUT(nodoDto.getRUT()) != null) {
                return Response.status(Response.Status.CONFLICT).entity("Ya existe un nodo con el mismo RUT").build();
            }

            nodoDto.setEstado(null);
            NodoPeriferico nodo = NodoPerifericoConverter.toEntity(nodoDto);
            try {
                NodoPeriferico created = nodoService.createAndNotify(nodo);
                return Response.status(Response.Status.CREATED).entity(NodoPerifericoConverter.toDTO(created)).build();
            } catch (RuntimeException re) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(re.getMessage()).build();
            }

        } catch (IllegalArgumentException e) {

            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();

        } catch (ConstraintViolationException e) {
            String msg = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
        } catch (jakarta.persistence.PersistenceException pe) {

            Throwable cause = pe.getCause();
            if (cause != null && (cause.getMessage() != null && cause.getMessage().toLowerCase().contains("unique") || cause instanceof java.sql.SQLIntegrityConstraintViolationException)) {
                return Response.status(Response.Status.CONFLICT).entity("Nodo with same RUT already exists").build();
            }
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(pe.getMessage()).build();
        }
    }

   

    @PUT
    @Path("{rut}")
    public Response update(@PathParam("rut") String rut, @Valid NodoPerifericoDTO nodoDto) {
        NodoPeriferico existing = repo.findByRUT(rut);
        if (existing == null) return Response.status(Response.Status.NOT_FOUND).build();
        try {
            validateEnumValues(nodoDto);
            if (nodoDto.getRUT() != null && !nodoDto.getRUT().equals(existing.getRUT()) && repo.findByRUT(nodoDto.getRUT()) != null) {
                return Response.status(Response.Status.CONFLICT).entity("Nodo with same RUT already exists").build();
            }
            NodoPeriferico nodo = NodoPerifericoConverter.toEntity(nodoDto);
            nodo.setId(existing.getId());
            NodoPeriferico updated = repo.update(nodo);
            return Response.ok(NodoPerifericoConverter.toDTO(updated)).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (ConstraintViolationException e) {
            String msg = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(", "));
            return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
        }
    }

    @DELETE
    @Path("{rut}")
    public Response delete(@PathParam("rut") String rut) {
        NodoPeriferico existing = repo.findByRUT(rut);
        if (existing == null) return Response.status(Response.Status.NOT_FOUND).build();
        repo.delete(existing.getId());
        return Response.noContent().build();
    }

    private void validateEnumValues(NodoPerifericoDTO dto) {
        if (dto.getDepartamento() != null) {
            try {
                Departamento.valueOf(dto.getDepartamento());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Valor de departamento invalido: " + dto.getDepartamento());
            }
        }
        if (dto.getEstado() != null) {
            try {
                EstadoNodoPeriferico.valueOf(dto.getEstado());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Valor de estado invalido: " + dto.getEstado());
            }
        }
    }
}