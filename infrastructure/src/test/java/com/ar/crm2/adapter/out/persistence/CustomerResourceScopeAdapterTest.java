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
import static org.mockito.Mockito.when;

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

    @Test
    void ownOrAssignedRowScopeRequiresPersistedCreatorOrResponsibleMembership() {
        ContactoRepository contactos = mock(ContactoRepository.class);
        EmpresaRepository empresas = mock(EmpresaRepository.class);
        AgendaRepository agendas = mock(AgendaRepository.class);
        CustomerResourceScopeAdapter scopedAdapter = new CustomerResourceScopeAdapter(contactos, empresas, agendas);
        UUID actorId = UUID.randomUUID();
        UUID contactoId = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        when(contactos.isVisibleToActor(contactoId.toString(), actorId.toString())).thenReturn(true);
        when(empresas.isVisibleToActor(empresaId.toString(), actorId.toString())).thenReturn(false);

        assertTrue(scopedAdapter.permits(actorId, RecursoCrm.CONTACTO, contactoId,
                AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertFalse(scopedAdapter.permits(actorId, RecursoCrm.EMPRESA, empresaId,
                AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
        assertFalse(scopedAdapter.permits(UUID.randomUUID(), RecursoCrm.CONTACTO, contactoId,
                AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of()));
    }}
