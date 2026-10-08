package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.application.tarea.command.EditTareaCommand;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.SaveTareaPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.enums.PrioridadTarea;
import com.ar.crm2.model.enums.TipoTarea;
import com.ar.crm2.model.vo.TareaId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EditTareaServiceAuthorizationTest {

    @Test
    void updateOnlyGrantCanPreserveOmittedDescriptionWithoutTaskReadPermission() {
        UUID taskId = UUID.randomUUID();
        LocalDateTime due = LocalDateTime.of(2026, 10, 15, 12, 0);
        Tarea existing = Tarea.create(TratoId.create(), UsuarioId.create(), "Existing", "keep me",
                TipoTarea.GENERAL, PrioridadTarea.MEDIA, due);
        Tarea stored = Tarea.reconstitute(TareaId.from(taskId), existing.getTratoId(),
                existing.getResponsableId(), existing.getTitulo(), existing.getDescripcion(), existing.getTipo(),
                existing.getPrioridad(), existing.getFechaLimite(), existing.getFechaCompletada(),
                existing.getCreadoEn(), existing.getActualizadoEn());
        FindTareaByIdPort findPort = mock(FindTareaByIdPort.class);
        SaveTareaPort savePort = mock(SaveTareaPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        when(findPort.findById(TareaId.from(taskId))).thenReturn(Optional.of(stored));
        when(savePort.save(any(Tarea.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tarea result = new EditTareaService(authorization, findPort, savePort).edit(
                new EditTareaCommand(taskId, stored.getResponsableId().value(), "Updated", null,
                        TipoTarea.GENERAL, PrioridadTarea.MEDIA, due));

        assertEquals("keep me", result.getDescripcion());
        verify(authorization).requireRecord(RecursoCrm.TAREA, AccionCrm.ACTUALIZAR, taskId);
        verify(authorization).requireCandidate(org.mockito.ArgumentMatchers.eq(RecursoCrm.TAREA),
                org.mockito.ArgumentMatchers.eq(AccionCrm.ACTUALIZAR),
                org.mockito.ArgumentMatchers.argThat(candidate ->
                        candidate.responsibleUserId().equals(stored.getResponsableId().value())));
        verify(authorization).requireRecord(RecursoCrm.TRATO, AccionCrm.LEER,
                stored.getTratoId().value());
        verify(authorization, never()).require(RecursoCrm.TAREA, AccionCrm.LEER);
    }

    @Test
    void updateDoesNotReturnTaskWhenItsLinkedDealIsUnreadable() {
        Tarea existing = Tarea.create(TratoId.create(), UsuarioId.create(), "Existing", "keep me",
                TipoTarea.GENERAL, PrioridadTarea.MEDIA, LocalDateTime.now().plusDays(1));
        FindTareaByIdPort findPort = id -> Optional.of(existing);
        SaveTareaPort savePort = mock(SaveTareaPort.class);
        TestCrmAuthorization authorization = new TestCrmAuthorization()
                .denyRecord(existing.getTratoId().value());

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new EditTareaService(authorization, findPort, savePort).edit(
                new EditTareaCommand(existing.getId().value(), existing.getResponsableId().value(),
                        "Updated", null, existing.getTipo(), existing.getPrioridad(), existing.getFechaLimite())));

        verify(savePort, never()).save(any(Tarea.class));
    }
}
