package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.application.trato.command.GetTratoByIdCommand;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetTratoByIdServiceAuthorizationTest {

    @Test
    void deniedOrUnknownRecordIsRejectedBeforeUnscopedLookup() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        FindTratoByIdPort findPort = mock(FindTratoByIdPort.class);
        UUID targetId = UUID.randomUUID();
        doThrow(new SecurityException("Denied"))
            .when(authorization).requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, targetId);
        GetTratoByIdService service = new GetTratoByIdService(authorization, findPort);

        assertThrows(SecurityException.class, () -> service.getById(new GetTratoByIdCommand(targetId)));
        verifyNoInteractions(findPort);
    }

    @Test
    void getByIdDeniesDealsWhoseContactIsNotReadable() {
        UUID contactId = UUID.randomUUID();
        Trato deal = Trato.create(
            ContactoId.from(contactId), UsuarioId.create(), "Deal", null, null, null, null
        );
        FindTratoByIdPort findPort = mock(FindTratoByIdPort.class);
        when(findPort.findById(TratoId.from(deal.getId().value()))).thenReturn(Optional.of(deal));
        GetTratoByIdService service = new GetTratoByIdService(
            new TestCrmAuthorization().denyRecord(contactId), findPort);

        assertThrows(TratoNotFoundException.class,
            () -> service.getById(new GetTratoByIdCommand(deal.getId().value())));
    }
}
