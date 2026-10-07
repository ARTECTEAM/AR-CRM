package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.EditContactoCommand;
import com.ar.crm2.application.contacto.port.out.FindContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.SaveContactoPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class EditContactoServiceAuthorizationTest {

    @Test
    void deniedOrUnknownRecordIsRejectedBeforeUnscopedLookup() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        FindContactoByIdPort findPort = mock(FindContactoByIdPort.class);
        SaveContactoPort savePort = mock(SaveContactoPort.class);
        UUID targetId = UUID.randomUUID();
        doThrow(new SecurityException("Denied"))
                .when(authorization).requireRecord(RecursoCrm.CONTACTO, AccionCrm.ACTUALIZAR, targetId);
        EditContactoService service = new EditContactoService(findPort, savePort, authorization);

        assertThrows(SecurityException.class, () -> service.edit(new EditContactoCommand(
                targetId, "Contact", null, null, null, null, null, null)));
        verifyNoInteractions(findPort, savePort);
    }
}
