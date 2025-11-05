package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.repository.DocumentoClinicoRepository;
import uy.edu.tse.hcen.context.TenantContext;
import uy.edu.tse.hcen.dto.DTMetadatos;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.bson.Document;
import java.util.List;

/**
 * Servicio que encapsula la lógica de negocio de documentos clínicos.
 * - valida que existan metadatos para un documento antes de aceptar contenido
 * - verifica que el tenant del metadato coincida con el TenantContext actual
 *
 * Se expone como EJB Stateless para facilidad de integración y posibles necesidades
 * transaccionales en el futuro.
 */
@Stateless
public class DocumentoService {

    @Inject
    private DocumentoClinicoRepository repo;

    @Inject
    private TenantContext tenantContext;

    public DocumentoService() {
    }

    /**
     * Guarda el contenido asociado a un documento previamente registrado en los metadatos.
     * Lanza IllegalArgumentException si no hay metadatos o si el tenant difiere.
     */
    public Document guardarContenido(String documentoId, String contenido) {
        // Verificar existencia de metadatos para el documentoId
        //VER
    
        // Validar tenant
        String metaTenant = ""; //VER
        String currentTenant = tenantContext.getTenantId();
        if (currentTenant == null || !currentTenant.equals(metaTenant)) {
            throw new SecurityException("Tenant mismatch: usuario no autorizado para subir este contenido");
        }


        //
        String documentoIdLocal = ""; //VER
        String url = "URL" + documentoIdLocal; //VER
    DTMetadatos metadatos = new DTMetadatos(); 
    metadatos.setDocumentoIdPaciente(documentoId);
        metadatos.setDocumentoId(documentoId);
        metadatos.setUrlAcceso(url);
        //seguir
        boolean registroExitoso = false;

        try {
             registroExitoso = false;
           //boolean registroExitoso = hcenRegistroCliente.registrarDocumento(metadatos, FAKE_JWT_PROFESIONAL);


        }
        catch (RuntimeException e){
            //manejar

        }   

        // --- LÓGICA DE DECISIÓN DEL REINTENTO ---
        if (!registroExitoso) {
            // Si hubo un error de comunicación o el Central rechazó la solicitud (código de error).
            
            // 1. Guardar el objeto Metadatos en la cola local de reintentos
            //reintentoService.guardarParaReintento(datos); 
            
            return repo.guardarContenido(documentoId, contenido); // Retorna falso indicando que el registro central no se completó.
        }
        // Persistir contenido
        return repo.guardarContenido(documentoId, contenido);
    }

    public Document buscarPorId(String idHex) {
        return repo.buscarPorId(idHex);
    }

    public List<String> buscarIdsPorDocumentoPaciente(String documentoId) {
        return repo.buscarIdsPorDocumentoPaciente(documentoId);
    }
}
