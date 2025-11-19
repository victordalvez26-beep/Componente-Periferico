package uy.edu.tse.hcen.rest;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Helper para procesar datos multipart/form-data.
 */
public final class MultipartHelper {

    private static final Logger LOG = Logger.getLogger(MultipartHelper.class);

    private MultipartHelper() {
        // Clase de utilidad, no instanciable
    }

    /**
     * Extrae un campo de texto del formulario multipart.
     */
    public static String extractTextField(MultipartFormDataInput input, String fieldName) {
        if (!input.getFormDataMap().containsKey(fieldName)) {
            return null;
        }
        List<InputPart> parts = input.getFormDataMap().get(fieldName);
        if (parts == null || parts.isEmpty()) {
            return null;
        }
        try {
            return parts.get(0).getBodyAsString();
        } catch (java.io.IOException e) {
            LOG.errorf(e, "Error al leer campo de texto '%s'", fieldName);
            return null;
        }
    }

    /**
     * Extrae un archivo del formulario multipart.
     */
    public static ArchivoAdjunto extractFile(MultipartFormDataInput input, String fieldName) {
        if (!input.getFormDataMap().containsKey(fieldName)) {
            LOG.infof("No se encontró el campo '%s' en el formulario multipart", fieldName);
            return null;
        }

        List<InputPart> archivoParts = input.getFormDataMap().get(fieldName);
        LOG.infof("Archivo parts encontrados: %d", archivoParts != null ? archivoParts.size() : 0);

        if (archivoParts == null || archivoParts.isEmpty()) {
            LOG.warnf("La lista de archivoParts está vacía o es null");
            return null;
        }

        try {
            InputPart archivoPart = archivoParts.get(0);
            MultivaluedMap<String, String> headers = archivoPart.getHeaders();
            String contentDisposition = headers.getFirst(DocumentoConstants.HEADER_CONTENT_DISPOSITION);
            String tipoArchivo = archivoPart.getMediaType() != null ? archivoPart.getMediaType().toString() : null;

            LOG.infof("Headers del archivo - Content-Disposition: %s, MediaType: %s", contentDisposition, tipoArchivo);

            String nombreArchivo = extractFileName(contentDisposition);
            byte[] archivoBytes = readFileBytes(archivoPart);

            if (archivoBytes != null && archivoBytes.length > 0) {
                if (nombreArchivo == null || nombreArchivo.isBlank()) {
                    nombreArchivo = DocumentoConstants.PREFIX_ARCHIVO_ADJUNTO + System.currentTimeMillis();
                }
                LOG.infof("Archivo adjunto recibido exitosamente: %s, tamaño: %d bytes, tipo: %s",
                        nombreArchivo, archivoBytes.length, tipoArchivo);
                return new ArchivoAdjunto(archivoBytes, nombreArchivo, tipoArchivo);
            } else {
                LOG.warnf("Archivo adjunto está vacío o no se pudo leer: nombre=%s, tamaño=%d",
                        nombreArchivo, archivoBytes != null ? archivoBytes.length : 0);
                return null;
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error procesando archivo adjunto: %s", e.getMessage());
            return null;
        }
    }

    private static String extractFileName(String contentDisposition) {
        if (contentDisposition == null) {
            return null;
        }

        String[] parts = contentDisposition.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("filename")) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    String nombreArchivo = keyValue[1].trim();
                    // Remover comillas si las hay
                    if (nombreArchivo.startsWith("\"") && nombreArchivo.endsWith("\"")) {
                        nombreArchivo = nombreArchivo.substring(1, nombreArchivo.length() - 1);
                    }
                    return nombreArchivo;
                }
            }
        }
        return null;
    }

    private static byte[] readFileBytes(InputPart archivoPart) {
        try {
            // Primero intentar leer directamente como byte[]
            byte[] archivoBytes = archivoPart.getBody(byte[].class, null);
            if (archivoBytes != null && archivoBytes.length > 0) {
                LOG.infof("Archivo leído como byte[]: %d bytes", archivoBytes.length);
                return archivoBytes;
            }

            // Si no funcionó, leer como InputStream y convertir a byte[]
            java.io.InputStream inputStream = archivoPart.getBody(java.io.InputStream.class, null);
            if (inputStream != null) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[8192];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                archivoBytes = buffer.toByteArray();
                inputStream.close();
                LOG.infof("Archivo leído como InputStream: %d bytes",
                        archivoBytes != null ? archivoBytes.length : 0);
                return archivoBytes;
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error al leer el archivo: %s", e.getMessage());
        }
        return null;
    }

    /**
     * Clase para representar un archivo adjunto.
     */
    public static class ArchivoAdjunto {
        private final byte[] bytes;
        private final String nombreArchivo;
        private final String tipoArchivo;

        public ArchivoAdjunto(byte[] bytes, String nombreArchivo, String tipoArchivo) {
            this.bytes = bytes;
            this.nombreArchivo = nombreArchivo;
            this.tipoArchivo = tipoArchivo;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public String getNombreArchivo() {
            return nombreArchivo;
        }

        public String getTipoArchivo() {
            return tipoArchivo;
        }
    }
}

