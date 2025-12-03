package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.dto.ConfiguracionPortalDTO;
import uy.edu.tse.hcen.model.PortalConfiguracion;
import uy.edu.tse.hcen.repository.PortalConfiguracionRepository;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

@Stateless
public class PortalConfiguracionService {

    private final PortalConfiguracionRepository configRepository;

    @Inject
    public PortalConfiguracionService(PortalConfiguracionRepository configRepository) {
        this.configRepository = configRepository;
    }
     
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
    public PortalConfiguracion updateConfiguracion(ConfiguracionPortalDTO dto) {
        PortalConfiguracion config = configRepository.findCurrentConfig()
            .orElseGet(this::createDefaultConfig);

        // Mapeo DTO a Entidad
        if (dto.getColorPrimario() != null) config.setColorPrimario(dto.getColorPrimario());
        if (dto.getColorSecundario() != null) config.setColorSecundario(dto.getColorSecundario());
        if (dto.getLogoUrl() != null) config.setLogoUrl(dto.getLogoUrl());
        if (dto.getNombrePortal() != null) config.setNombrePortal(dto.getNombrePortal());

        return configRepository.save(config);
    }

    private PortalConfiguracion createDefaultConfig() {

        PortalConfiguracion defaultConfig = new PortalConfiguracion();
        return configRepository.save(defaultConfig);
    }
}