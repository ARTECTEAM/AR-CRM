package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.repository.AgendaRepository;
import com.ar.crm2.adapter.out.persistence.repository.ContactoRepository;
import com.ar.crm2.adapter.out.persistence.repository.EmpresaRepository;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CustomerResourceScopeAdapterTest {

    private final CustomerResourceScopeAdapter adapter = new CustomerResourceScopeAdapter(
            mock(ContactoRepository.class), mock(EmpresaRepository.class), mock(AgendaRepository.class));

    @Test
    void contactCandidateCannotBeReassignedAwayFromActorWhenActorIsNotCreator() {
        UUID actor = UUID.randomUUID();
        ResourceScopeCandidate candidate = new ResourceScopeCandidate(
                null, null, UUID.randomUUID(), UUID.randomUUID());

        assertFalse(adapter.permitsCandidate(actor, RecursoCrm.CONTACTO, candidate,
                AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
    }

    @Test
    void contactCandidateRemainsInScopeWhenActorIsCreator() {
        UUID actor = UUID.randomUUID();
        ResourceScopeCandidate candidate = new ResourceScopeCandidate(
                null, null, UUID.randomUUID(), actor);

        assertTrue(adapter.permitsCandidate(actor, RecursoCrm.CONTACTO, candidate,
                AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
    }

    @Test
    void agendaCandidateRequiresTheActorToRemainItsCreator() {
        UUID actor = UUID.randomUUID();
        ResourceScopeCandidate candidate = new ResourceScopeCandidate(
                null, null, actor, UUID.randomUUID());

        assertFalse(adapter.permitsCandidate(actor, RecursoCrm.AGENDA, candidate,
                AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
    }
}
