package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.tablero.port.out.FindInitialColumnPort;
import com.ar.crm2.application.tarea.command.CreateTareaCommand;
import com.ar.crm2.application.tarea.port.out.SaveTareaPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateTareaCandidateAuthorizationTest {

    @Test
    void generatedFichaMustBeInAnAllowedBoardBeforeTheTaskIsPersisted() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        SaveTareaPort saveTarea = mock(SaveTareaPort.class);
        SaveFichaPort saveFicha = mock(SaveFichaPort.class);
        FindInitialColumnPort findInitialColumn = mock(FindInitialColumnPort.class);
        UUID columnId = UUID.randomUUID();
        Columna initialColumn = mock(Columna.class);
        when(initialColumn.getId()).thenReturn(ColumnaId.from(columnId));
        when(findInitialColumn.findInitialColumn(TipoTablero.TAREAS)).thenReturn(Optional.of(initialColumn));
        doThrow(new SecurityException("Initial Ficha is outside the assigned boards"))
            .when(authorization).requireCandidate(eq(RecursoCrm.FICHA), eq(AccionCrm.CREAR),
                any(ResourceScopeCandidate.class));
        CreateTareaService service = new CreateTareaService(
            authorization, saveTarea, saveFicha, findInitialColumn);
        UUID responsibleId = UUID.randomUUID();

        assertThrows(SecurityException.class, () -> service.create(new CreateTareaCommand(
            UUID.randomUUID(), responsibleId, "Task", null, null, null, null)));

        var candidate = ArgumentCaptor.forClass(ResourceScopeCandidate.class);
        verify(authorization).requireCandidate(eq(RecursoCrm.TAREA), eq(AccionCrm.CREAR), candidate.capture());
        assertEquals(new ResourceScopeCandidate(null, null, responsibleId, null), candidate.getValue());
        verify(authorization).requireCandidate(eq(RecursoCrm.FICHA), eq(AccionCrm.CREAR),
            eq(new ResourceScopeCandidate(RecursoCrm.COLUMNA, columnId, null, null)));
        verifyNoInteractions(saveTarea, saveFicha);
    }
}
