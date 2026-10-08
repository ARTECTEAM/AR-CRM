package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EditTratoRelatedAuthorizationTest {

    @Test
    void reassignmentMustPassTheUpdateScopeCandidateBeforeLookupOrSave() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        SaveTratoPort save = mock(SaveTratoPort.class);
        UUID responsibleId = UUID.randomUUID();
        doThrow(new CrmAuthorizationDeniedException("Assignee outside scope"))
            .when(authorization).requireCandidate(eq(RecursoCrm.TRATO), eq(AccionCrm.ACTUALIZAR),
                any(ResourceScopeCandidate.class));
        EditTratoService service = new EditTratoService(authorization, find, save);

        assertThrows(CrmAuthorizationDeniedException.class, () -> service.edit(new EditTratoCommand(
            UUID.randomUUID(), responsibleId, "Deal", null, null, null, TipoContrato.OTRO)));

        verifyNoInteractions(find, save);
    }

    @Test
    void editDeniesWhenItsContactReferenceIsNotReadable() {
        UUID contactId = UUID.randomUUID();
        UUID responsibleId = UUID.randomUUID();
        Trato existing = Trato.create(
            ContactoId.from(contactId), UsuarioId.from(responsibleId), "Before", null, null, null, TipoContrato.OTRO
        );
        FindTratoByIdPort find = mock(FindTratoByIdPort.class);
        SaveTratoPort save = mock(SaveTratoPort.class);
        TratoId tratoId = existing.getId();
        when(find.findById(tratoId)).thenReturn(Optional.of(existing));
        EditTratoService service = new EditTratoService(
            new TestCrmAuthorization().denyRecord(contactId), find, save);

        assertThrows(TratoNotFoundException.class, () -> service.edit(new EditTratoCommand(
            existing.getId().value(), responsibleId, "After", null, null, null, TipoContrato.OTRO)));

        verify(find).findById(tratoId);
        verifyNoInteractions(save);
    }
}
