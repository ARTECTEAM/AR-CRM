package com.ar.crm2.application.notatrato.service;

import com.ar.crm2.application.notatrato.port.out.FindNotasByTratoPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetNotasByTratoAuthorizationTest {

    @Test
    void deniesNoteReadBeforeQueryingNotes() {
        UUID dealId = UUID.randomUUID();
        FindTratoByIdPort findDeal = mock(FindTratoByIdPort.class);
        FindNotasByTratoPort findNotes = mock(FindNotasByTratoPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, dealId);

        var service = new GetNotasByTratoService(authorization, findDeal, findNotes);

        assertThrows(CrmAuthorizationDeniedException.class, () -> service.getByTrato(dealId));
        verifyNoInteractions(findDeal, findNotes);
    }

    @Test
    void requiresLinkedContactReadBeforeQueryingNotes() {
        Trato deal = Trato.create(ContactoId.create(), UsuarioId.create(), "Deal", null, null, null, null);
        FindTratoByIdPort findDeal = mock(FindTratoByIdPort.class);
        when(findDeal.findById(deal.getId())).thenReturn(Optional.of(deal));
        FindNotasByTratoPort findNotes = mock(FindNotasByTratoPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).requireRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, deal.getContactoId().value());

        var service = new GetNotasByTratoService(authorization, findDeal, findNotes);

        assertThrows(CrmAuthorizationDeniedException.class, () -> service.getByTrato(deal.getId().value()));
        verifyNoInteractions(findNotes);
    }
}