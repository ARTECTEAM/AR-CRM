package com.ar.crm2.adapter.out.security;

import com.ar.crm2.adapter.out.persistence.entity.FichaEntity;
import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import com.ar.crm2.adapter.out.persistence.entity.TareaEntity;
import com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository;
import com.ar.crm2.adapter.out.persistence.repository.FichaRepository;
import com.ar.crm2.adapter.out.persistence.repository.TableroRepository;
import com.ar.crm2.adapter.out.persistence.repository.TareaRepository;
import com.ar.crm2.adapter.out.persistence.repository.TratoRepository;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommercialResourceScopeAdapterTest {

    @Test
    void ownOrAssignedScopeAllowsOnlyResponsibleUsersAndNeverClaimsCatalogColumnOwnership() {
        TableroRepository tableros = mock(TableroRepository.class);
        ColumnaRepository columnas = mock(ColumnaRepository.class);
        FichaRepository fichas = mock(FichaRepository.class);
        TratoRepository tratos = mock(TratoRepository.class);
        TareaRepository tareas = mock(TareaRepository.class);
        CommercialResourceScopeAdapter adapter = new CommercialResourceScopeAdapter(
            tableros, columnas, fichas, tratos, tareas
        );
        UUID actorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID tratoId = UUID.randomUUID();
        UUID tareaId = UUID.randomUUID();
        TratoEntity trato = TratoEntity.builder().id(tratoId.toString()).responsableId(actorId.toString()).build();
        TareaEntity tarea = TareaEntity.builder().id(tareaId.toString()).responsableId(actorId.toString()).build();
        when(tratos.findById(tratoId.toString())).thenReturn(Optional.of(trato));
        when(tareas.findById(tareaId.toString())).thenReturn(Optional.of(tarea));

        assertTrue(adapter.permits(actorId, RecursoCrm.TRATO, tratoId, AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertFalse(adapter.permits(otherUserId, RecursoCrm.TRATO, tratoId, AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertTrue(adapter.permits(actorId, RecursoCrm.TAREA, tareaId, AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertFalse(adapter.permits(otherUserId, RecursoCrm.TAREA, tareaId, AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertFalse(adapter.permits(UUID.randomUUID(), RecursoCrm.COLUMNA, UUID.randomUUID(),
            AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
    }

    @Test
    void allowedBoardScopeIncludesCardsOnlyWhenTheirColumnBelongsToAnAllowedBoard() {
        TableroRepository tableros = mock(TableroRepository.class);
        ColumnaRepository columnas = mock(ColumnaRepository.class);
        FichaRepository fichas = mock(FichaRepository.class);
        TratoRepository tratos = mock(TratoRepository.class);
        TareaRepository tareas = mock(TareaRepository.class);
        CommercialResourceScopeAdapter adapter = new CommercialResourceScopeAdapter(
            tableros, columnas, fichas, tratos, tareas
        );
        UUID actorId = UUID.randomUUID();
        UUID fichaId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        UUID allowedBoardId = UUID.randomUUID();
        UUID otherBoardId = UUID.randomUUID();
        FichaEntity ficha = FichaEntity.builder().id(fichaId.toString()).columnaId(columnId.toString()).build();
        when(fichas.findById(fichaId.toString())).thenReturn(Optional.of(ficha));
        when(tableros.existsByIdAndColumnasTableroColumnaId(allowedBoardId.toString(), columnId.toString()))
            .thenReturn(true);

        assertTrue(adapter.permits(actorId, RecursoCrm.FICHA, fichaId, AlcanceCrm.TABLEROS_PERMITIDOS,
            Set.of(allowedBoardId)));
        assertFalse(adapter.permits(actorId, RecursoCrm.FICHA, fichaId, AlcanceCrm.TABLEROS_PERMITIDOS,
            Set.of(otherBoardId)));
    }

    @Test
    void candidateScopeUsesResponsibleUserAndPersistedDestinationColumnMembership() {
        TableroRepository tableros = mock(TableroRepository.class);
        ColumnaRepository columnas = mock(ColumnaRepository.class);
        FichaRepository fichas = mock(FichaRepository.class);
        TratoRepository tratos = mock(TratoRepository.class);
        TareaRepository tareas = mock(TareaRepository.class);
        CommercialResourceScopeAdapter adapter = new CommercialResourceScopeAdapter(
            tableros, columnas, fichas, tratos, tareas
        );
        UUID actorId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID allowedBoardId = UUID.randomUUID();
        UUID otherBoardId = UUID.randomUUID();
        UUID targetColumnId = UUID.randomUUID();
        when(columnas.existsById(targetColumnId.toString())).thenReturn(true);
        when(tableros.existsByIdAndColumnasTableroColumnaId(allowedBoardId.toString(), targetColumnId.toString()))
            .thenReturn(true);
        ResourceScopeCandidate fichaCandidate = new ResourceScopeCandidate(
            RecursoCrm.COLUMNA, targetColumnId, null, null);
        ResourceScopeCandidate ownerCandidate = new ResourceScopeCandidate(null, null, actorId, null);

        assertTrue(adapter.permitsCandidate(actorId, RecursoCrm.FICHA, fichaCandidate,
            AlcanceCrm.TABLEROS_PERMITIDOS, Set.of(allowedBoardId)));
        assertFalse(adapter.permitsCandidate(actorId, RecursoCrm.FICHA, fichaCandidate,
            AlcanceCrm.TABLEROS_PERMITIDOS, Set.of(otherBoardId)));
        assertTrue(adapter.permitsCandidate(actorId, RecursoCrm.TRATO, ownerCandidate,
            AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertFalse(adapter.permitsCandidate(otherUserId, RecursoCrm.TRATO, ownerCandidate,
            AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertTrue(adapter.permitsCandidate(actorId, RecursoCrm.TAREA, ownerCandidate,
            AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertFalse(adapter.permitsCandidate(actorId, RecursoCrm.FICHA, ownerCandidate,
            AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
    }

    @Test
    void allowedBoardColumnReadAllowsAnyMembershipButWritesRequireEveryMembership() {
        TableroRepository tableros = mock(TableroRepository.class);
        ColumnaRepository columnas = mock(ColumnaRepository.class);
        CommercialResourceScopeAdapter adapter = new CommercialResourceScopeAdapter(
            tableros, columnas, mock(FichaRepository.class), mock(TratoRepository.class), mock(TareaRepository.class)
        );
        UUID actorId = UUID.randomUUID();
        UUID columnId = UUID.randomUUID();
        UUID allowedBoardId = UUID.randomUUID();
        UUID inaccessibleBoardId = UUID.randomUUID();
        Set<UUID> allowedBoards = Set.of(allowedBoardId);
        when(columnas.existsById(columnId.toString())).thenReturn(true);
        when(tableros.existsByIdAndColumnasTableroColumnaId(allowedBoardId.toString(), columnId.toString()))
            .thenReturn(true);
        when(tableros.findBoardIdsByColumnasTableroColumnaId(columnId.toString()))
            .thenReturn(List.of(allowedBoardId.toString(), inaccessibleBoardId.toString()));

        assertTrue(adapter.permits(actorId, RecursoCrm.COLUMNA, AccionCrm.LEER, columnId,
            AlcanceCrm.TABLEROS_PERMITIDOS, allowedBoards));
        for (AccionCrm action : List.of(AccionCrm.ACTUALIZAR, AccionCrm.ELIMINAR, AccionCrm.ADMINISTRAR)) {
            assertFalse(adapter.permits(actorId, RecursoCrm.COLUMNA, action, columnId,
                AlcanceCrm.TABLEROS_PERMITIDOS, allowedBoards));
        }

        when(tableros.findBoardIdsByColumnasTableroColumnaId(columnId.toString()))
            .thenReturn(List.of(allowedBoardId.toString()));
        assertTrue(adapter.permits(actorId, RecursoCrm.COLUMNA, AccionCrm.ACTUALIZAR, columnId,
            AlcanceCrm.TABLEROS_PERMITIDOS, allowedBoards));

        when(tableros.findBoardIdsByColumnasTableroColumnaId(columnId.toString())).thenReturn(List.of());
        assertFalse(adapter.permits(actorId, RecursoCrm.COLUMNA, AccionCrm.ACTUALIZAR, columnId,
            AlcanceCrm.TABLEROS_PERMITIDOS, allowedBoards));
    }
}
