package com.ar.crm2.application.ficha;

import com.ar.crm2.application.security.AllowAllCrmAuthorization;
import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetasByIdsPort;
import com.ar.crm2.application.ficha.command.CreateFichaCommand;
import com.ar.crm2.application.ficha.exception.FichaNotFoundException;
import com.ar.crm2.application.ficha.port.out.FindFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.application.ficha.service.CreateFichaService;
import com.ar.crm2.application.ficha.service.EditFichaService;
import com.ar.crm2.exception.EtiquetaTypeMismatchException;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.vo.EtiquetaId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FichaEtiquetaResolutionTest {

    @Test
    void create_resolvesEtiquetaIdsAndAttachesRelations() {
        EtiquetaId e1 = EtiquetaId.create();
        EtiquetaId e2 = EtiquetaId.create();
        InMemoryFindEtiquetasByIds findEtiquetas = new InMemoryFindEtiquetasByIds(List.of(
            Etiqueta.reconstitute(e1, "A", TipoEtiqueta.TAREA, "#FFFFFF", java.time.LocalDateTime.now()),
            Etiqueta.reconstitute(e2, "B", TipoEtiqueta.TAREA, "#000000", java.time.LocalDateTime.now())
        ));
        InMemorySaveFichaPort savePort = new InMemorySaveFichaPort();
        CreateFichaService service = new CreateFichaService(new AllowAllCrmAuthorization(), savePort, findEtiquetas);

        Ficha ficha = service.create(new CreateFichaCommand(
            UUID.randomUUID(), TipoFicha.TAREA, null, UUID.randomUUID(), List.of(e1.value(), e2.value())
        ));

        assertEquals(2, ficha.getEtiquetas().size());
        List<EtiquetaId> ids = ficha.getEtiquetas().stream().map(e -> e.getEtiquetaId()).toList();
        assertTrue(ids.contains(e1));
        assertTrue(ids.contains(e2));
    }

    @Test
    void create_rejectsWhenEtiquetaTypeDoesNotMatchFichaType() {
        EtiquetaId e1 = EtiquetaId.create();
        InMemoryFindEtiquetasByIds findEtiquetas = new InMemoryFindEtiquetasByIds(List.of(
            // Etiqueta is TRATO, but Ficha is TAREA → mismatch
            Etiqueta.reconstitute(e1, "X", TipoEtiqueta.TRATO, "#FFFFFF", java.time.LocalDateTime.now())
        ));
        InMemorySaveFichaPort savePort = new InMemorySaveFichaPort();
        CreateFichaService service = new CreateFichaService(new AllowAllCrmAuthorization(), savePort, findEtiquetas);

        assertThrows(EtiquetaTypeMismatchException.class,
            () -> service.create(new CreateFichaCommand(
                UUID.randomUUID(), TipoFicha.TAREA, null, UUID.randomUUID(), List.of(e1.value())
            )));
    }

    @Test
    void create_withEmptyEtiquetaIdsLeavesFichaWithoutTags() {
        InMemoryFindEtiquetasByIds findEtiquetas = new InMemoryFindEtiquetasByIds(List.of());
        InMemorySaveFichaPort savePort = new InMemorySaveFichaPort();
        CreateFichaService service = new CreateFichaService(new AllowAllCrmAuthorization(), savePort, findEtiquetas);

        Ficha ficha = service.create(new CreateFichaCommand(
            UUID.randomUUID(), TipoFicha.TAREA, null, UUID.randomUUID(), List.of()
        ));

        assertTrue(ficha.getEtiquetas().isEmpty());
    }

    @Test
    void edit_resolvesEtiquetaIdsAndReplacesRelations() {
        // Existing ficha has one etiqueta
        FichaIdHolder holder = new FichaIdHolder();
        Ficha existing = Ficha.create(
            com.ar.crm2.model.vo.ColumnaId.create(),
            TipoFicha.TAREA,
            null,
            com.ar.crm2.model.vo.TareaId.create()
        );
        holder.ficha = existing;
        EtiquetaId newE = EtiquetaId.create();
        InMemoryFindEtiquetasByIds findEtiquetas = new InMemoryFindEtiquetasByIds(List.of(
            Etiqueta.reconstitute(newE, "New", TipoEtiqueta.TAREA, "#FFFFFF", java.time.LocalDateTime.now())
        ));
        InMemoryFindFichaById findFichaPort = new InMemoryFindFichaById(holder);
        InMemorySaveFichaPort savePort = new InMemorySaveFichaPort();
        EditFichaService service = new EditFichaService(new AllowAllCrmAuthorization(), findFichaPort, savePort, findEtiquetas);

        Ficha updated = service.edit(new com.ar.crm2.application.ficha.command.EditFichaCommand(
            holder.ficha.getId().value(),
            UUID.randomUUID(),
            TipoFicha.TAREA,
            null,
            UUID.randomUUID(),
            List.of(newE.value())
        ));

        assertEquals(1, updated.getEtiquetas().size());
        assertEquals(newE, updated.getEtiquetas().get(0).getEtiquetaId());
    }

    @Test
    void edit_throwsFichaNotFoundWhenIdDoesNotExist() {
        InMemoryFindEtiquetasByIds findEtiquetas = new InMemoryFindEtiquetasByIds(List.of());
        InMemorySaveFichaPort savePort = new InMemorySaveFichaPort();
        InMemoryFindFichaById findFichaPort = new InMemoryFindFichaById(new FichaIdHolder()); // empty holder
        EditFichaService service = new EditFichaService(new AllowAllCrmAuthorization(), findFichaPort, savePort, findEtiquetas);

        assertThrows(FichaNotFoundException.class,
            () -> service.edit(new com.ar.crm2.application.ficha.command.EditFichaCommand(
                UUID.randomUUID(), UUID.randomUUID(), TipoFicha.TAREA, null, UUID.randomUUID(), List.of()
            )));
    }

    @Test
    void create_rejectsWhenCatalogReturnsFewerEtiquetasThanRequested() {
        // Realistic adapter: the catalog returns fewer rows than the caller
        // asked for (e.g. one etiqueta was deleted out-of-band). The service
        // must NOT silently drop the missing tag — it must reject the save
        // with the missing-id list so the caller can react.
        EtiquetaId present = EtiquetaId.create();
        EtiquetaId missing = EtiquetaId.create();
        InMemoryFindEtiquetasByIds findEtiquetas = new InMemoryFindEtiquetasByIds(List.of(
            Etiqueta.reconstitute(present, "A", TipoEtiqueta.TAREA, "#FFFFFF", java.time.LocalDateTime.now())
        ));
        InMemorySaveFichaPort savePort = new InMemorySaveFichaPort();
        CreateFichaService service = new CreateFichaService(new AllowAllCrmAuthorization(), savePort, findEtiquetas);

        EtiquetaNotFoundException ex = assertThrows(EtiquetaNotFoundException.class,
            () -> service.create(new CreateFichaCommand(
                UUID.randomUUID(), TipoFicha.TAREA, null, UUID.randomUUID(),
                List.of(present.value(), missing.value())
            )));
        assertNotNull(ex.getMissingIds());
        assertTrue(ex.getMissingIds().contains(missing.value()));
        assertTrue(ex.getMissingIds().stream().noneMatch(id -> id.equals(present.value())));
        // The save must NOT have been called when resolution fails.
        assertTrue(savePort.saved.isEmpty());
    }

    // ── Test doubles ──────────────────────────────────────────────

    private static final class FichaIdHolder {
        Ficha ficha;
    }

    private static final class InMemorySaveFichaPort implements SaveFichaPort {
        final java.util.List<Ficha> saved = new java.util.ArrayList<>();

        @Override
        public Ficha save(Ficha ficha) {
            saved.add(ficha);
            return ficha;
        }
    }

    private static final class InMemoryFindFichaById implements FindFichaByIdPort {
        private final FichaIdHolder holder;

        InMemoryFindFichaById(FichaIdHolder holder) {
            this.holder = holder;
        }

        @Override
        public Optional<Ficha> findById(com.ar.crm2.model.vo.FichaId id) {
            Ficha f = holder.ficha;
            return f != null && f.getId().equals(id) ? Optional.of(f) : Optional.empty();
        }
    }

    private static final class InMemoryFindEtiquetasByIds implements FindEtiquetasByIdsPort {
        private final List<Etiqueta> etiquetas;

        InMemoryFindEtiquetasByIds(List<Etiqueta> etiquetas) {
            this.etiquetas = etiquetas;
        }

        @Override
        public List<Etiqueta> findByIds(List<EtiquetaId> ids) {
            return etiquetas.stream()
                .filter(e -> ids.contains(e.getId()))
                .toList();
        }
    }
}
