package com.ar.crm2.application.etiqueta;

import com.ar.crm2.application.etiqueta.command.EditEtiquetaCommand;
import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.port.out.ExistsEtiquetaByNombreAndTipoPort;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetaByIdPort;
import com.ar.crm2.application.etiqueta.port.out.SaveEtiquetaPort;
import com.ar.crm2.application.etiqueta.service.EditEtiquetaService;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.exception.DuplicateEtiquetaNameException;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EditEtiquetaServiceTest {

    @Test
    void edit_renamesAndRecolorsEtiqueta() {
        EtiquetaId id = EtiquetaId.create();
        Etiqueta original = Etiqueta.reconstitute(
            id, "Old", TipoEtiqueta.TAREA, "#FFFFFF", LocalDateTime.now().minusDays(1)
        );
        // Adapter is empty: no other row shares (nombre, tipo), so even an
        // unfiltered exists() check would return false. Used here only to
        // confirm the happy path persists the new state.
        CatalogAdapter catalog = new CatalogAdapter();
        EditEtiquetaService service = new EditEtiquetaService(
            new InMemoryFindByIdPort(id, original),
            new InMemorySaveEtiquetaPort(),
            catalog,
            new TestCrmAuthorization()
        );

        Etiqueta updated = service.edit(new EditEtiquetaCommand(id.value(), "New", "#000000"));

        assertEquals(id, updated.getId());
        assertEquals("New", updated.getNombre());
        assertEquals("#000000", updated.getColor());
        assertEquals(TipoEtiqueta.TAREA, updated.getTipoEtiqueta());
        // The service must pass the editing id as excludeId so the adapter
        // can ignore the row being updated.
        assertEquals(id, catalog.lastExcludeId);
    }

    @Test
    void edit_throwsEtiquetaNotFoundWhenIdDoesNotExist() {
        EditEtiquetaService service = new EditEtiquetaService(
            new InMemoryFindByIdPort(EtiquetaId.create(), null),
            new InMemorySaveEtiquetaPort(),
            new CatalogAdapter(),
            new TestCrmAuthorization()
        );

        assertThrows(EtiquetaNotFoundException.class,
            () -> service.edit(new EditEtiquetaCommand(UUID.randomUUID(), "X", "#FFFFFF")));
    }

    @Test
    void edit_rejectsDuplicateNombreInSameTipo() {
        EtiquetaId id = EtiquetaId.create();
        Etiqueta original = Etiqueta.reconstitute(
            id, "Old", TipoEtiqueta.TAREA, "#FFFFFF", LocalDateTime.now().minusDays(1)
        );
        // A different etiqueta ("Existing") of the same type exists.
        CatalogAdapter catalog = new CatalogAdapter();
        catalog.put(EtiquetaId.create(), "Existing", TipoEtiqueta.TAREA);
        EditEtiquetaService service = new EditEtiquetaService(
            new InMemoryFindByIdPort(id, original),
            new InMemorySaveEtiquetaPort(),
            catalog,
            new TestCrmAuthorization()
        );

        assertThrows(DuplicateEtiquetaNameException.class,
            () -> service.edit(new EditEtiquetaCommand(id.value(), "Existing", "#000000")));
    }

    @Test
    void edit_allowsRenameToSameNameForSameId_evenIfAdapterSeesTheRow() {
        // Realistic adapter: a naive JPA `existsByNombreAndTipo` returns true
        // whenever the (nombre, tipo) tuple exists, including the row being
        // edited. The service must exclude the current id from the check so
        // that rename-to-same-name (idempotent rename) and recolor-only
        // operations are not falsely rejected.
        EtiquetaId id = EtiquetaId.create();
        Etiqueta original = Etiqueta.reconstitute(
            id, "Stable", TipoEtiqueta.TAREA, "#FFFFFF", LocalDateTime.now().minusDays(1)
        );
        CatalogAdapter catalog = new CatalogAdapter();
        catalog.put(id, "Stable", TipoEtiqueta.TAREA);
        EditEtiquetaService service = new EditEtiquetaService(
            new InMemoryFindByIdPort(id, original),
            new InMemorySaveEtiquetaPort(),
            catalog,
            new TestCrmAuthorization()
        );

        Etiqueta updated = service.edit(new EditEtiquetaCommand(id.value(), "Stable", "#123456"));

        assertEquals("Stable", updated.getNombre());
        assertEquals("#123456", updated.getColor());
    }

    @Test
    void edit_allowsRecolorOnlyWhenNameIsUnchanged_evenIfAdapterSeesTheRow() {
        EtiquetaId id = EtiquetaId.create();
        Etiqueta original = Etiqueta.reconstitute(
            id, "Blue", TipoEtiqueta.TAREA, "#0000FF", LocalDateTime.now().minusDays(1)
        );
        CatalogAdapter catalog = new CatalogAdapter();
        catalog.put(id, "Blue", TipoEtiqueta.TAREA);
        EditEtiquetaService service = new EditEtiquetaService(
            new InMemoryFindByIdPort(id, original),
            new InMemorySaveEtiquetaPort(),
            catalog,
            new TestCrmAuthorization()
        );

        Etiqueta updated = service.edit(new EditEtiquetaCommand(id.value(), "Blue", "#00FF00"));

        assertEquals("Blue", updated.getNombre());
        assertEquals("#00FF00", updated.getColor());
    }

    @Test
    void edit_rejectsWhenOtherRowClaimsTheNewName() {
        EtiquetaId id = EtiquetaId.create();
        Etiqueta original = Etiqueta.reconstitute(
            id, "Old", TipoEtiqueta.TAREA, "#FFFFFF", LocalDateTime.now().minusDays(1)
        );
        EtiquetaId otherId = EtiquetaId.create();
        CatalogAdapter catalog = new CatalogAdapter();
        catalog.put(id, "Old", TipoEtiqueta.TAREA);
        catalog.put(otherId, "Existing", TipoEtiqueta.TAREA);
        EditEtiquetaService service = new EditEtiquetaService(
            new InMemoryFindByIdPort(id, original),
            new InMemorySaveEtiquetaPort(),
            catalog,
            new TestCrmAuthorization()
        );

        assertThrows(DuplicateEtiquetaNameException.class,
            () -> service.edit(new EditEtiquetaCommand(id.value(), "Existing", "#FFFFFF")));
    }

    // ── Test doubles ──────────────────────────────────────────────

    private static final class InMemoryFindByIdPort implements FindEtiquetaByIdPort {
        private final EtiquetaId id;
        private final Etiqueta etiqueta;

        InMemoryFindByIdPort(EtiquetaId id, Etiqueta etiqueta) {
            this.id = id;
            this.etiqueta = etiqueta;
        }

        @Override
        public Optional<Etiqueta> findById(EtiquetaId etiquetaId) {
            return etiqueta != null && etiquetaId.equals(id) ? Optional.of(etiqueta) : Optional.empty();
        }
    }

    private static final class InMemorySaveEtiquetaPort implements SaveEtiquetaPort {
        @Override
        public Etiqueta save(Etiqueta etiqueta) {
            return etiqueta;
        }
    }

    /**
     * Realistic catalog adapter: simulates a JPA
     * `existsByNombreAndTipoAndIdNot` query. It returns true if any row
     * shares the (nombre, tipo) tuple, except when that row's id matches
     * the supplied {@code excludeId} (the row being edited).
     */
    private static final class CatalogAdapter implements ExistsEtiquetaByNombreAndTipoPort {
        private final Map<EtiquetaId, EtiquetaSnapshot> rows = new HashMap<>();
        EtiquetaId lastExcludeId;
        String lastNombre;
        TipoEtiqueta lastTipo;

        void put(EtiquetaId id, String nombre, TipoEtiqueta tipo) {
            rows.put(id, new EtiquetaSnapshot(nombre, tipo));
        }

        @Override
        public boolean exists(String nombre, TipoEtiqueta tipoEtiqueta, EtiquetaId excludeId) {
            this.lastNombre = nombre;
            this.lastTipo = tipoEtiqueta;
            this.lastExcludeId = excludeId;
            for (Map.Entry<EtiquetaId, EtiquetaSnapshot> entry : rows.entrySet()) {
                if (excludeId != null && entry.getKey().equals(excludeId)) {
                    continue;
                }
                EtiquetaSnapshot snapshot = entry.getValue();
                if (snapshot.nombre.equals(nombre) && snapshot.tipo == tipoEtiqueta) {
                    return true;
                }
            }
            return false;
        }
    }

    private record EtiquetaSnapshot(String nombre, TipoEtiqueta tipo) {
    }
}
