package uy.edu.tse.hcen.cli;

import uy.edu.tse.hcen.model.NodoPeriferico;
import uy.edu.tse.hcen.model.enums.EstadoNodoPeriferico;
import uy.edu.tse.hcen.service.NodoIntegrationService;

import java.util.*;

/**
 * Local CLI runner that simulates sending a JMS TextMessage by invoking the
 * repository and integration service logic with in-memory fakes. Useful to
 * quickly test the parsing, create and estado-update flow without a full
 * application server.
 */
public class LocalTestRunner {

    public static void main(String[] args) {
        // Sample payload to test
        String payload = "RUT:123;NOMBRE:Clinica X;URL:https://clinica-x/simulate-ok;DEPARTAMENTO:MONTEVIDEO";
        if (args != null && args.length > 0) {
            payload = String.join(" ", args);
        }
        System.out.println("Using payload: " + payload);

        InMemoryNodoRepo repo = new InMemoryNodoRepo();
        InMemoryIntegrationService integration = new InMemoryIntegrationService(repo);

        NodoPeriferico nodo = parsePayload(payload);
        System.out.println("Parsed nodo (before create): " + nodoToString(nodo));

        NodoPeriferico created = repo.create(nodo);
        System.out.println("After create: " + nodoToString(created));

        integration.checkAndUpdateEstado(created);
        System.out.println("After integration check: " + nodoToString(created));

        // show all stored nodes
        System.out.println("All nodes in repo:");
        for (NodoPeriferico n : repo.findAll()) {
            System.out.println(" - " + nodoToString(n));
        }
    }

    private static NodoPeriferico parsePayload(String payload) {
        NodoPeriferico n = new NodoPeriferico();
        String[] parts = payload.split(";");
        for (String p : parts) {
            String[] kv = p.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim().toUpperCase();
            String value = kv[1].trim();
            switch (key) {
                case "RUT":
                    n.setRUT(value);
                    break;
                case "NOMBRE":
                    n.setNombre(value);
                    break;
                case "URL":
                    n.setNodoPerifericoUrlBase(value);
                    break;
                case "DEPARTAMENTO":
                    try {
                        n.setDepartamento(uy.edu.tse.hcen.model.enums.Departamento.valueOf(value));
                    } catch (Exception ex) {
                        // ignore
                    }
                    break;
                case "LOCALIDAD":
                    n.setLocalidad(value);
                    break;
                case "DIRECCION":
                    n.setDireccion(value);
                    break;
                case "CONTACTO":
                    n.setContacto(value);
                    break;
                case "USUARIO":
                    n.setNodoPerifericoUsuario(value);
                    break;
                case "PASSWORD":
                    n.setNodoPerifericoPassword(value);
                    break;
                default:
                    // ignore
            }
        }
        return n;
    }

    private static String nodoToString(NodoPeriferico n) {
        return String.format("id=%s, RUT=%s, nombre=%s, url=%s, estado=%s",
                n.getId(), n.getRUT(), n.getNombre(), n.getNodoPerifericoUrlBase(), n.getEstado());
    }

    // Simple in-memory repo suitable for local testing
    static class InMemoryNodoRepo {
        private final Map<Long, NodoPeriferico> store = new LinkedHashMap<>();
        private long seq = 1L;

        public NodoPeriferico create(NodoPeriferico nodo) {
            if (nodo.getId() == null) nodo.setId(seq++);
            if (nodo.getEstado() == null) nodo.setEstado(EstadoNodoPeriferico.PENDIENTE);
            store.put(nodo.getId(), nodo);
            return nodo;
        }

        public NodoPeriferico update(NodoPeriferico nodo) {
            if (nodo.getId() == null) {
                return create(nodo);
            }
            store.put(nodo.getId(), nodo);
            return nodo;
        }

        public void delete(Long id) {
            store.remove(id);
        }

        public NodoPeriferico find(Long id) {
            return store.get(id);
        }

        public NodoPeriferico findByRUT(String rut) {
            return store.values().stream().filter(n -> rut.equals(n.getRUT())).findFirst().orElse(null);
        }

        public List<NodoPeriferico> findAll() {
            return new ArrayList<>(store.values());
        }
    }

    // In-memory integration service that mirrors the simulation rules used in the bean
    static class InMemoryIntegrationService implements NodoIntegrationService {
        private final InMemoryNodoRepo repo;

        InMemoryIntegrationService(InMemoryNodoRepo repo) {
            this.repo = repo;
        }

        @Override
        public void checkAndUpdateEstado(NodoPeriferico nodo) {
            String url = nodo.getNodoPerifericoUrlBase();
            EstadoNodoPeriferico nuevo;
            if (url == null || url.isBlank()) {
                nuevo = EstadoNodoPeriferico.PENDIENTE;
            } else if (url.contains("simulate-ok") || url.contains("/ok") || url.contains("200")) {
                nuevo = EstadoNodoPeriferico.ACTIVO;
            } else if (url.contains("simulate-maint") || url.contains("maintenance")) {
                nuevo = EstadoNodoPeriferico.MANTENIMIENTO;
            } else {
                nuevo = EstadoNodoPeriferico.INACTIVO;
            }
            nodo.setEstado(nuevo);
            repo.update(nodo);
            System.out.println("[Integration] nodo id=" + nodo.getId() + " estado=" + nuevo);
        }
    }
}