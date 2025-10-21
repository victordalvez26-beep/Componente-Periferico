package uy.edu.tse.hcen.service;

import uy.edu.tse.hcen.model.NodoPeriferico;

public interface NodoIntegrationService {
    /**
     * Performs a (simulated) integration check against the peripheral node and
     * updates the nodo.estado accordingly by persisting the change.
     */
    void checkAndUpdateEstado(NodoPeriferico nodo);
}
