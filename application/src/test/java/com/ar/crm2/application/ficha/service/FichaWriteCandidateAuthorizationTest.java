package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.etiqueta.port.out.FindEtiquetasByIdsPort;
import com.ar.crm2.application.ficha.command.CreateFichaCommand;
import com.ar.crm2.application.ficha.command.EditFichaCommand;
import com.ar.crm2.application.ficha.command.MoverColumnaFichaCommand;
import com.ar.crm2.application.ficha.port.out.FindFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.enums.TipoFicha;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class FichaWriteCandidateAuthorizationTest {

    @Test
    void createChecksBoardScopedColumnCandidateBeforeResolvingOrSaving() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        SaveFichaPort save = mock(SaveFichaPort.class);
        FindEtiquetasByIdsPort findLabels = mock(FindEtiquetasByIdsPort.class);
        UUID columnId = UUID.randomUUID();
        UUID tratoId = UUID.randomUUID();
        denyCandidate(authorization, AccionCrm.CREAR);
        CreateFichaService service = new CreateFichaService(authorization, save, findLabels);

        assertThrows(SecurityException.class, () -> service.create(new CreateFichaCommand(
            columnId, TipoFicha.TRATO, tratoId, null, List.of())));

        var candidate = ArgumentCaptor.forClass(ResourceScopeCandidate.class);
        verify(authorization).requireCandidate(eq(RecursoCrm.FICHA), eq(AccionCrm.CREAR), candidate.capture());
        assertEquals(new ResourceScopeCandidate(RecursoCrm.COLUMNA, columnId, null, null), candidate.getValue());
        verifyNoInteractions(save, findLabels);
    }

    @Test
    void editChecksResultingColumnCandidateBeforeLoadingOrSavingTheFicha() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        FindFichaByIdPort find = mock(FindFichaByIdPort.class);
        SaveFichaPort save = mock(SaveFichaPort.class);
        FindEtiquetasByIdsPort findLabels = mock(FindEtiquetasByIdsPort.class);
        UUID columnId = UUID.randomUUID();
        denyCandidate(authorization, AccionCrm.ACTUALIZAR);
        EditFichaService service = new EditFichaService(authorization, find, save, findLabels);

        assertThrows(SecurityException.class, () -> service.edit(new EditFichaCommand(
            UUID.randomUUID(), columnId, TipoFicha.TRATO, UUID.randomUUID(), null, List.of())));

        verifyNoInteractions(find, save, findLabels);
    }

    @Test
    void moveChecksDestinationCandidateBeforeLoadingOrSavingTheFicha() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        FindFichaByIdPort find = mock(FindFichaByIdPort.class);
        SaveFichaPort save = mock(SaveFichaPort.class);
        UUID targetColumnId = UUID.randomUUID();
        denyCandidate(authorization, AccionCrm.ACTUALIZAR);
        MoverColumnaFichaService service = new MoverColumnaFichaService(authorization, find, save);

        assertThrows(SecurityException.class, () -> service.moverAColumna(new MoverColumnaFichaCommand(
            UUID.randomUUID(), targetColumnId)));

        verifyNoInteractions(find, save);
    }

    private static void denyCandidate(CrmAuthorization authorization, AccionCrm accion) {
        doThrow(new SecurityException("Candidate is outside the assigned boards"))
            .when(authorization).requireCandidate(eq(RecursoCrm.FICHA), eq(accion), any(ResourceScopeCandidate.class));
    }
}
