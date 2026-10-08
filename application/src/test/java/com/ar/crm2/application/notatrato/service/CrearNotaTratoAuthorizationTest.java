package com.ar.crm2.application.notatrato.service;

import com.ar.crm2.application.notatrato.port.out.SaveNotaTratoPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CrearNotaTratoAuthorizationTest {

    @Test
    void deniesCreationBeforeDealReadOrSave() {
        UUID dealId = UUID.randomUUID();
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        SaveNotaTratoPort save = mock(SaveNotaTratoPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).requireRecord(RecursoCrm.TRATO, AccionCrm.ACTUALIZAR, dealId);

        var service = new CrearNotaTratoService(authorization, mock(CurrentActorPort.class), find, save);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> service.crear(dealId, UUID.randomUUID(), "note"));
        verifyNoInteractions(find, save);
    }

    @Test
    void ignoresCallerAuthorAndUsesCanonicalCurrentActor() {
        Trato deal = Trato.create(ContactoId.create(), UsuarioId.create(), "Deal", null, null, null, null);
        CurrentActor actor = new CurrentActor(UUID.randomUUID(), UUID.randomUUID(), false);
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        when(find.findById(deal.getId())).thenReturn(Optional.of(deal));
        SaveNotaTratoPort save = mock(SaveNotaTratoPort.class);
        CurrentActorPort actorPort = mock(CurrentActorPort.class);
        when(actorPort.currentActor()).thenReturn(Optional.of(actor));

        new CrearNotaTratoService(mock(CrmAuthorization.class), actorPort, find, save)
                .crear(deal.getId().value(), UUID.randomUUID(), "trusted content");

        ArgumentCaptor<com.ar.crm2.model.entity.NotaTrato> captor =
                ArgumentCaptor.forClass(com.ar.crm2.model.entity.NotaTrato.class);
        verify(save).save(captor.capture());
        assertEquals(actor.usuarioId(), captor.getValue().getAutorId().value());
    }
}