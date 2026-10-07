package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.notatrato.port.out.SaveNotaTratoPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CambiarEstadoTratoAuthorizationTest {

    @Test
    void deniesWinBeforeLoadingOrWritingDeal() {
        Trato deal = deal();
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        SaveTratoPort save = mock(SaveTratoPort.class);
        SaveNotaTratoPort notes = mock(SaveNotaTratoPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).requireRecord(RecursoCrm.TRATO, AccionCrm.ACTUALIZAR, deal.getId().value());

        var service = new CambiarEstadoTratoService(authorization, find, save, notes);

        assertThrows(CrmAuthorizationDeniedException.class, () -> service.ganar(deal.getId().value()));
        verifyNoInteractions(find, save, notes);
    }

    @Test
    void deniesDealMutationWhenLinkedContactIsUnreadableBeforeAnyWrite() {
        Trato deal = deal();
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        when(find.findById(deal.getId())).thenReturn(Optional.of(deal));
        SaveTratoPort save = mock(SaveTratoPort.class);
        SaveNotaTratoPort notes = mock(SaveNotaTratoPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).requireRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, deal.getContactoId().value());

        var service = new CambiarEstadoTratoService(authorization, find, save, notes);

        assertThrows(CrmAuthorizationDeniedException.class, () -> service.perder(deal.getId().value(), "lost"));
        verifyNoInteractions(save, notes);
    }

    private static Trato deal() {
        return Trato.create(ContactoId.create(), UsuarioId.create(), "Deal", null, null, null, null);
    }
}