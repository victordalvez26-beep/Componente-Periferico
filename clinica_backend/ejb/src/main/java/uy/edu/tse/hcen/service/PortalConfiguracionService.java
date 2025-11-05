package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.DTConfiguracionPortal;
import uy.edu.tse.hcen.model.PortalConfiguracion;
import uy.edu.tse.hcen.repository.PortalConfiguracionRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class PortalConfiguracionService {

    @Inject
    private PortalConfiguracionRepository configRepository;
     
    /**
     * Obtiene la configuración actual del tenant, o crea la configuración por defecto si no existe.
     */
    public PortalConfiguracion getConfiguracion() {
        return configRepository.findCurrentConfig()
                .orElseGet(this::createDefaultConfig);
    }

    /**
     * Actualiza la configuración existente o crea una nueva si no existe.
     */
    public PortalConfiguracion updateConfiguracion(DTConfiguracionPortal dto) {
        PortalConfiguracion config = configRepository.findCurrentConfig()
            .orElseGet(this::createDefaultConfig);

        // Mapeo DTO a Entidad
        if (dto.colorPrimario != null) config.setColorPrimario(dto.colorPrimario);
        if (dto.colorSecundario != null) config.setColorSecundario(dto.colorSecundario);
        if (dto.logoUrl != null) config.setLogoUrl(dto.logoUrl);
        if (dto.nombrePortal != null) config.setNombrePortal(dto.nombrePortal);

        return configRepository.save(config);
    }

    private PortalConfiguracion createDefaultConfig() {

        PortalConfiguracion defaultConfig = new PortalConfiguracion();
        return configRepository.save(defaultConfig);
    }
}