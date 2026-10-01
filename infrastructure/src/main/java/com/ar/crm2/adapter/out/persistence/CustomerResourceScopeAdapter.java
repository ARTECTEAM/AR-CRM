package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.repository.AgendaRepository;
import com.ar.crm2.adapter.out.persistence.repository.ContactoRepository;
import com.ar.crm2.adapter.out.persistence.repository.EmpresaRepository;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.security.port.out.ResourceScopePort;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

/** Evaluates creator/responsible relationships for customer-owned CRM resources. */
@Component
@RequiredArgsConstructor
public class CustomerResourceScopeAdapter implements ResourceScopePort {

    private final ContactoRepository contactoRepository;
    private final EmpresaRepository empresaRepository;
    private final AgendaRepository agendaRepository;

    @Override
    public boolean supports(RecursoCrm resource) {
        return resource == RecursoCrm.CONTACTO
                || resource == RecursoCrm.EMPRESA
                || resource == RecursoCrm.AGENDA;
    }

    @Override
    public boolean permits(
            UUID usuarioId,
            RecursoCrm resource,
            UUID recordId,
            AlcanceCrm scope,
            Set<UUID> allowedIds
    ) {
        if (usuarioId == null || resource == null || recordId == null || scope == null || !supports(resource)) {
            return false;
        }

        if (scope == AlcanceCrm.TODO_COMPARTIDO) {
            return true;
        }
        if (scope != AlcanceCrm.PROPIOS_O_ASIGNADOS) {
            return false;
        }

        String actorId = usuarioId.toString();
        String resourceId = recordId.toString();
        return switch (resource) {
            case CONTACTO -> contactoRepository.isVisibleToActor(resourceId, actorId);
            case EMPRESA -> empresaRepository.isVisibleToActor(resourceId, actorId);
            case AGENDA -> agendaRepository.existsByIdAndCreadoPor(resourceId, actorId);
            default -> false;
        };
    }

    @Override
    public boolean permitsCandidate(
            UUID usuarioId,
            RecursoCrm resource,
            ResourceScopeCandidate candidate,
            AlcanceCrm scope,
            Set<UUID> allowedIds
    ) {
        if (usuarioId == null || candidate == null || scope != AlcanceCrm.PROPIOS_O_ASIGNADOS
                || !supports(resource) || candidate.targetResource() != null) {
            return false;
        }

        boolean creatorMatches = usuarioId.equals(candidate.creatorUserId());
        boolean responsibleMatches = usuarioId.equals(candidate.responsibleUserId());
        return switch (resource) {
            case CONTACTO, EMPRESA -> creatorMatches || responsibleMatches;
            case AGENDA -> creatorMatches;
            default -> false;
        };
    }
}
