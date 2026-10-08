package com.ar.crm2.adapter.out.security;

import com.ar.crm2.adapter.out.persistence.entity.FichaEntity;
import com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository;
import com.ar.crm2.adapter.out.persistence.repository.FichaRepository;
import com.ar.crm2.adapter.out.persistence.repository.TableroRepository;
import com.ar.crm2.adapter.out.persistence.repository.TareaRepository;
import com.ar.crm2.adapter.out.persistence.repository.TratoRepository;
import com.ar.crm2.application.security.port.out.ResourceScopePort;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/** Resolves commercial record scope from persisted board links and responsibility fields. */
@Component
@RequiredArgsConstructor
public class CommercialResourceScopeAdapter implements ResourceScopePort {

    private final TableroRepository tableroRepository;
    private final ColumnaRepository columnaRepository;
    private final FichaRepository fichaRepository;
    private final TratoRepository tratoRepository;
    private final TareaRepository tareaRepository;

    @Override
    public boolean supports(RecursoCrm recurso) {
        return switch (recurso) {
            case TABLERO, COLUMNA, FICHA, TRATO, TAREA -> true;
            default -> false;
        };
    }

    @Override
    public boolean permits(
        UUID usuarioId,
        RecursoCrm recurso,
        UUID recordId,
        AlcanceCrm scope,
        Set<UUID> allowedIds
    ) {
        if (usuarioId == null || recurso == null || recordId == null || scope == null) {
            return false;
        }

        return switch (scope) {
            case TODO_COMPARTIDO -> true;
            case PROPIOS_O_ASIGNADOS -> permitsOwnedOrAssigned(usuarioId, recurso, recordId);
            case TABLEROS_PERMITIDOS -> permitsAllowedBoard(recurso, recordId, allowedIds);
        };
    }

    @Override
    public boolean permits(
        UUID usuarioId,
        RecursoCrm recurso,
        AccionCrm accion,
        UUID recordId,
        AlcanceCrm scope,
        Set<UUID> allowedIds
    ) {
        if (recurso == RecursoCrm.COLUMNA && isMutation(accion)
            && scope == AlcanceCrm.TABLEROS_PERMITIDOS) {
            return permitsColumnMutationOnAllBoards(recordId, allowedIds);
        }
        return permits(usuarioId, recurso, recordId, scope, allowedIds);
    }

    @Override
    public boolean permitsCandidate(
        UUID usuarioId,
        RecursoCrm recurso,
        ResourceScopeCandidate candidate,
        AlcanceCrm scope,
        Set<UUID> allowedIds
    ) {
        if (usuarioId == null || recurso == null || candidate == null || scope == null) {
            return false;
        }

        return switch (scope) {
            case TODO_COMPARTIDO -> true;
            case PROPIOS_O_ASIGNADOS -> permitsOwnedCandidate(usuarioId, recurso, candidate);
            case TABLEROS_PERMITIDOS -> permitsBoardCandidate(recurso, candidate, allowedIds);
        };
    }

    private boolean permitsOwnedCandidate(UUID usuarioId, RecursoCrm recurso, ResourceScopeCandidate candidate) {
        return (recurso == RecursoCrm.TRATO || recurso == RecursoCrm.TAREA)
            && candidate.responsibleUserId() != null
            && usuarioId.equals(candidate.responsibleUserId());
    }

    private boolean permitsBoardCandidate(
        RecursoCrm recurso,
        ResourceScopeCandidate candidate,
        Set<UUID> allowedIds
    ) {
        if (allowedIds == null || allowedIds.isEmpty()) {
            return false;
        }
        return switch (recurso) {
            case FICHA -> candidate.targetResource() == RecursoCrm.COLUMNA
                && columnaRepository.existsById(candidate.targetId().toString())
                && columnBelongsToAllowedBoard(candidate.targetId().toString(), allowedIds);
            // No other commercial candidate relationship is currently modeled.
            case TABLERO, COLUMNA, TRATO, TAREA -> false;
            default -> false;
        };
    }

    private boolean permitsOwnedOrAssigned(UUID usuarioId, RecursoCrm recurso, UUID recordId) {
        String recordKey = recordId.toString();
        String actorKey = usuarioId.toString();
        return switch (recurso) {
            case TRATO -> tratoRepository.findById(recordKey)
                .map(trato -> actorKey.equals(trato.getResponsableId()))
                .orElse(false);
            case TAREA -> tareaRepository.findById(recordKey)
                .map(tarea -> actorKey.equals(tarea.getResponsableId()))
                .orElse(false);
            // These resources have no owner/responsible field; never infer ownership.
            case TABLERO, COLUMNA, FICHA -> false;
            default -> false;
        };
    }

    private boolean permitsAllowedBoard(RecursoCrm recurso, UUID recordId, Set<UUID> allowedIds) {
        if (allowedIds == null || allowedIds.isEmpty()) {
            return false;
        }
        String recordKey = recordId.toString();
        return switch (recurso) {
            case TABLERO -> allowedIds.contains(recordId) && tableroRepository.existsById(recordKey);
            case COLUMNA -> columnaRepository.existsById(recordKey)
                && allowedIds.stream().anyMatch(boardId -> tableroRepository
                    .existsByIdAndColumnasTableroColumnaId(boardId.toString(), recordKey));
            case FICHA -> fichaRepository.findById(recordKey)
                .map(FichaEntity::getColumnaId)
                .filter(columnId -> columnBelongsToAllowedBoard(columnId, allowedIds))
                .isPresent();
            // Deals and tasks have no canonical board relationship of their own.
            case TRATO, TAREA -> false;
            default -> false;
        };
    }

    private boolean permitsColumnMutationOnAllBoards(UUID columnId, Set<UUID> allowedIds) {
        if (columnId == null || allowedIds == null || allowedIds.isEmpty()
            || !columnaRepository.existsById(columnId.toString())) {
            return false;
        }

        Set<String> allowedBoardIds = allowedIds.stream().map(UUID::toString).collect(java.util.stream.Collectors.toSet());
        java.util.List<String> referencingBoardIds = tableroRepository
            .findBoardIdsByColumnasTableroColumnaId(columnId.toString());
        return referencingBoardIds != null && !referencingBoardIds.isEmpty()
            && referencingBoardIds.stream().allMatch(allowedBoardIds::contains);
    }

    private boolean isMutation(AccionCrm accion) {
        return accion == AccionCrm.ACTUALIZAR || accion == AccionCrm.ELIMINAR
            || accion == AccionCrm.ADMINISTRAR;
    }

    private boolean columnBelongsToAllowedBoard(String columnId, Set<UUID> allowedIds) {
        return columnId != null && allowedIds.stream().anyMatch(boardId -> tableroRepository
            .existsByIdAndColumnasTableroColumnaId(boardId.toString(), columnId));
    }
}
