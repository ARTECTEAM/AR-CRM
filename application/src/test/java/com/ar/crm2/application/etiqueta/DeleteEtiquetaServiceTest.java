package com.ar.crm2.application.etiqueta;

import com.ar.crm2.application.etiqueta.command.DeleteEtiquetaCommand;
import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.exception.EtiquetaRequiresConfirmationException;
import com.ar.crm2.application.etiqueta.port.out.CountFichaEtiquetasByEtiquetaIdPort;
import com.ar.crm2.application.etiqueta.port.out.DeleteEtiquetaByIdPort;
import com.ar.crm2.application.etiqueta.port.out.DeleteFichaEtiquetasByEtiquetaIdPort;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetaByIdPort;
import com.ar.crm2.application.etiqueta.port.out.FindFichaIdsByEtiquetaIdPort;
import com.ar.crm2.application.etiqueta.service.DeleteEtiquetaService;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteEtiquetaServiceTest {

    @Test
    void delete_rejectsWithoutConfirmationWhenRelationsExist() {
        EtiquetaId id = EtiquetaId.create();
        Etiqueta etiqueta = Etiqueta.reconstitute(
            id, "X", TipoEtiqueta.TAREA, "#FFFFFF", LocalDateTime.now()
        );
        InMemoryFindById findPort = new InMemoryFindById(id, etiqueta);
        InMemoryCount countPort = new InMemoryCount(3);
        InMemoryDeleteRelations deleteRelPort = new InMemoryDeleteRelations();
        InMemoryDeleteById deleteByIdPort = new InMemoryDeleteById();

        DeleteEtiquetaService service = new DeleteEtiquetaService(
            findPort, countPort, deleteRelPort, deleteByIdPort,
            fichaIdsPort(List.of(UUID.randomUUID())), new TestCrmAuthorization()
        );

        assertThrows(EtiquetaRequiresConfirmationException.class,
            () -> service.delete(new DeleteEtiquetaCommand(id.value(), false)));
        assertTrue(deleteRelPort.deletedIds.isEmpty());
        assertTrue(deleteByIdPort.deletedIds.isEmpty());
    }

    @Test
    void delete_cascadesAndDeletesWithConfirmation() {
        EtiquetaId id = EtiquetaId.create();
        Etiqueta etiqueta = Etiqueta.reconstitute(
            id, "X", TipoEtiqueta.TAREA, "#FFFFFF", LocalDateTime.now()
        );
        InMemoryFindById findPort = new InMemoryFindById(id, etiqueta);
        InMemoryCount countPort = new InMemoryCount(3);
        InMemoryDeleteRelations deleteRelPort = new InMemoryDeleteRelations();
        InMemoryDeleteById deleteByIdPort = new InMemoryDeleteById();

        DeleteEtiquetaService service = new DeleteEtiquetaService(
            findPort, countPort, deleteRelPort, deleteByIdPort,
            fichaIdsPort(List.of(UUID.randomUUID())), new TestCrmAuthorization()
        );

        service.delete(new DeleteEtiquetaCommand(id.value(), true));

        assertEquals(List.of(id), deleteRelPort.deletedIds);
        assertEquals(List.of(id), deleteByIdPort.deletedIds);
    }

    @Test
    void delete_allowsUnconfirmedDeleteWhenNoRelations() {
        EtiquetaId id = EtiquetaId.create();
        Etiqueta etiqueta = Etiqueta.reconstitute(
            id, "X", TipoEtiqueta.TAREA, "#FFFFFF", LocalDateTime.now()
        );
        InMemoryFindById findPort = new InMemoryFindById(id, etiqueta);
        InMemoryCount countPort = new InMemoryCount(0);
        InMemoryDeleteRelations deleteRelPort = new InMemoryDeleteRelations();
        InMemoryDeleteById deleteByIdPort = new InMemoryDeleteById();

        DeleteEtiquetaService service = new DeleteEtiquetaService(
            findPort, countPort, deleteRelPort, deleteByIdPort,
            fichaIdsPort(List.of()), new TestCrmAuthorization()
        );

        service.delete(new DeleteEtiquetaCommand(id.value(), false));

        assertEquals(List.of(id), deleteByIdPort.deletedIds);
    }

    @Test
    void delete_throwsEtiquetaNotFoundWhenIdDoesNotExist() {
        DeleteEtiquetaService service = new DeleteEtiquetaService(
            new InMemoryFindById(EtiquetaId.create(), null),
            new InMemoryCount(0),
            new InMemoryDeleteRelations(),
            new InMemoryDeleteById(),
            fichaIdsPort(List.of()),
            new TestCrmAuthorization()
        );

        assertThrows(EtiquetaNotFoundException.class,
            () -> service.delete(new DeleteEtiquetaCommand(UUID.randomUUID(), true)));
    }

    @Test
    void delete_rejectsCascadeWhenAnyLinkedFichaIsUnauthorized() {
        EtiquetaId id = EtiquetaId.create();
        Etiqueta etiqueta = Etiqueta.reconstitute(
                id, "X", TipoEtiqueta.TAREA, "#FFFFFF", LocalDateTime.now());
        UUID allowedFicha = UUID.randomUUID();
        UUID deniedFicha = UUID.randomUUID();
        InMemoryDeleteRelations deleteRelPort = new InMemoryDeleteRelations();
        InMemoryDeleteById deleteByIdPort = new InMemoryDeleteById();
        TestCrmAuthorization authorization = new TestCrmAuthorization().denyRecord(deniedFicha);
        DeleteEtiquetaService service = new DeleteEtiquetaService(
                new InMemoryFindById(id, etiqueta),
                new InMemoryCount(2),
                deleteRelPort,
                deleteByIdPort,
                fichaIdsPort(List.of(allowedFicha, deniedFicha)),
                authorization);

        assertThrows(com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException.class,
                () -> service.delete(new DeleteEtiquetaCommand(id.value(), true)));
        assertTrue(deleteRelPort.deletedIds.isEmpty());
        assertTrue(deleteByIdPort.deletedIds.isEmpty());
    }

    private static FindFichaIdsByEtiquetaIdPort fichaIdsPort(List<UUID> fichaIds) {
        return ignored -> fichaIds;
    }

    // ── Test doubles ──────────────────────────────────────────────

    private static final class InMemoryFindById implements FindEtiquetaByIdPort {
        private final EtiquetaId id;
        private final Etiqueta etiqueta;

        InMemoryFindById(EtiquetaId id, Etiqueta etiqueta) {
            this.id = id;
            this.etiqueta = etiqueta;
        }

        @Override
        public Optional<Etiqueta> findById(EtiquetaId etiquetaId) {
            return etiqueta != null && etiquetaId.equals(id) ? Optional.of(etiqueta) : Optional.empty();
        }
    }

    private static final class InMemoryCount implements CountFichaEtiquetasByEtiquetaIdPort {
        private final long count;

        InMemoryCount(long count) {
            this.count = count;
        }

        @Override
        public long countByEtiquetaId(EtiquetaId etiquetaId) {
            return count;
        }
    }

    private static final class InMemoryDeleteRelations implements DeleteFichaEtiquetasByEtiquetaIdPort {
        private final List<EtiquetaId> deletedIds = new ArrayList<>();

        @Override
        public void deleteByEtiquetaId(EtiquetaId etiquetaId) {
            deletedIds.add(etiquetaId);
        }
    }

    private static final class InMemoryDeleteById implements DeleteEtiquetaByIdPort {
        private final List<EtiquetaId> deletedIds = new ArrayList<>();

        @Override
        public void deleteById(EtiquetaId etiquetaId) {
            deletedIds.add(etiquetaId);
        }
    }
}
