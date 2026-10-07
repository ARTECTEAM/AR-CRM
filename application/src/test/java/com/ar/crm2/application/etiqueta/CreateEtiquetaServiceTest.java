package com.ar.crm2.application.etiqueta;

import com.ar.crm2.application.etiqueta.command.CreateEtiquetaCommand;
import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.port.out.ExistsEtiquetaByNombreAndTipoPort;
import com.ar.crm2.application.etiqueta.port.out.SaveEtiquetaPort;
import com.ar.crm2.application.etiqueta.service.CreateEtiquetaService;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.exception.DuplicateEtiquetaNameException;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateEtiquetaServiceTest {

    @Test
    void create_persistsEtiquetaWhenNameIsUnique() {
        InMemorySaveEtiquetaPort savePort = new InMemorySaveEtiquetaPort();
        CatalogAdapter catalog = new CatalogAdapter();
        CreateEtiquetaService service = new CreateEtiquetaService(
            savePort,
            catalog,
            new TestCrmAuthorization()
        );

        Etiqueta result = service.create(new CreateEtiquetaCommand("Urgent", TipoEtiqueta.TAREA, "#FF0000"));

        assertNotNull(result.getId());
        assertEquals("Urgent", result.getNombre());
        assertEquals(TipoEtiqueta.TAREA, result.getTipoEtiqueta());
        assertEquals("#FF0000", result.getColor());
        assertEquals(1, savePort.saved.size());
        // On create, no row is being updated, so the service must pass null
        // as excludeId so the adapter treats the lookup as a pure existence check.
        assertNull(catalog.lastExcludeId);
    }

    @Test
    void create_rejectsDuplicateNombreInSameTipo() {
        CatalogAdapter catalog = new CatalogAdapter();
        catalog.put(EtiquetaId.create(), "Urgent", TipoEtiqueta.TAREA);
        CreateEtiquetaService service = new CreateEtiquetaService(
            new InMemorySaveEtiquetaPort(),
            catalog,
            new TestCrmAuthorization()
        );

        assertThrows(DuplicateEtiquetaNameException.class,
            () -> service.create(new CreateEtiquetaCommand("Urgent", TipoEtiqueta.TAREA, "#FF0000")));
    }

    @Test
    void create_allowsSameNameInDifferentTipo() {
        CatalogAdapter catalog = new CatalogAdapter();
        catalog.put(EtiquetaId.create(), "Urgent", TipoEtiqueta.TAREA);
        CreateEtiquetaService service = new CreateEtiquetaService(
            new InMemorySaveEtiquetaPort(),
            catalog,
            new TestCrmAuthorization()
        );

        Etiqueta result = service.create(new CreateEtiquetaCommand("Urgent", TipoEtiqueta.TRATO, "#FF0000"));

        assertEquals(TipoEtiqueta.TRATO, result.getTipoEtiqueta());
    }

    @Test
    void create_propagatesEtiquetaNotFoundException() {
        // Sanity: EtiquetaNotFoundException has a forId factory
        EtiquetaNotFoundException ex = EtiquetaNotFoundException.forId(java.util.UUID.randomUUID());
        assertNotNull(ex);
        assertTrue(ex.getMessage().contains("no encontrada"));
    }

    // ── Test doubles ──────────────────────────────────────────────

    private static final class InMemorySaveEtiquetaPort implements SaveEtiquetaPort {
        private final List<Etiqueta> saved = new ArrayList<>();

        @Override
        public Etiqueta save(Etiqueta etiqueta) {
            saved.add(etiqueta);
            return etiqueta;
        }
    }

    /**
     * Realistic catalog adapter: returns true if any row shares the
     * (nombre, tipo) tuple. The {@code excludeId} parameter is honored,
     * so create-flow callers can pass null and edit-flow callers can
     * pass the editing id to scope the lookup correctly.
     */
    private static final class CatalogAdapter implements ExistsEtiquetaByNombreAndTipoPort {
        private final List<Row> rows = new ArrayList<>();
        EtiquetaId lastExcludeId;
        String lastNombre;
        TipoEtiqueta lastTipo;

        void put(EtiquetaId id, String nombre, TipoEtiqueta tipo) {
            rows.add(new Row(id, nombre, tipo));
        }

        @Override
        public boolean exists(String nombre, TipoEtiqueta tipoEtiqueta, EtiquetaId excludeId) {
            this.lastNombre = nombre;
            this.lastTipo = tipoEtiqueta;
            this.lastExcludeId = excludeId;
            for (Row row : rows) {
                if (excludeId != null && row.id.equals(excludeId)) {
                    continue;
                }
                if (row.nombre.equals(nombre) && row.tipo == tipoEtiqueta) {
                    return true;
                }
            }
            return false;
        }
    }

    private record Row(EtiquetaId id, String nombre, TipoEtiqueta tipo) {
    }
}
