package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.application.tarea.command.GetTareaByIdCommand;
import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.enums.PrioridadTarea;
import com.ar.crm2.model.enums.TipoTarea;
import com.ar.crm2.model.vo.TareaId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TareaLinkedDealAuthorizationTest {

    @Test
    void taskListHidesRowsWhoseLinkedDealIsNotReadable() {
        Tarea visible = tarea();
        Tarea hidden = tarea();
        TestCrmAuthorization authorization = new TestCrmAuthorization()
                .denyRecord(hidden.getTratoId().value());
        FindAllTareasPort findAll = () -> List.of(visible, hidden);

        List<Tarea> result = new GetAllTareasService(authorization, findAll).getAll();

        assertEquals(List.of(visible), result);
    }

    @Test
    void taskDetailRequiresReadAccessToLinkedDeal() {
        Tarea task = tarea();
        TestCrmAuthorization authorization = new TestCrmAuthorization()
                .denyRecord(task.getTratoId().value());
        FindTareaByIdPort find = id -> Optional.of(task);

        assertThrows(CrmAuthorizationDeniedException.class, () -> new GetTareaByIdService(authorization, find)
                .getById(new GetTareaByIdCommand(task.getId().value())));
    }

    private static Tarea tarea() {
        return Tarea.create(TratoId.create(), UsuarioId.create(), "Task", null,
                TipoTarea.GENERAL, PrioridadTarea.MEDIA, LocalDateTime.now().plusDays(1));
    }
}
