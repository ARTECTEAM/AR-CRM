package com.ar.crm2.application.tarea.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.query.TareaFilterCriteria;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.enums.PrioridadTarea;
import com.ar.crm2.model.enums.TipoTarea;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAllTareasAuthorizationCompatibilityTest {

    @Test
    void deniesListingBeforeQueryWhenReadIsNotGranted() {
        FindAllTareasPort rows = mock(FindAllTareasPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).require(RecursoCrm.TAREA, AccionCrm.LEER);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new GetAllTareasService(authorization, rows).getAll(TareaFilterCriteria.empty()));
        verifyNoInteractions(rows);
    }

    @Test
    void filtersTaskAndDealScopeBeforeComputingPageTotals() {
        TareaFilterCriteria criteria = new TareaFilterCriteria(null, null, null, null, null, null,
                new ListPageRequest(0, 1, "titulo", ListPageRequest.SortDirection.ASC));
        Tarea deniedDeal = task("Denied deal", TratoId.create());
        Tarea visible = task("Visible", TratoId.create());
        FindAllTareasPort rows = mock(FindAllTareasPort.class);
        CrmAuthorization authorization = authorization();
        when(rows.findAll(criteria)).thenReturn(List.of(deniedDeal, visible));
        when(authorization.permitsRecord(RecursoCrm.TAREA, AccionCrm.LEER, deniedDeal.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.TRATO, AccionCrm.LEER, deniedDeal.getTratoId().value())).thenReturn(false);
        when(authorization.permitsRecord(RecursoCrm.TAREA, AccionCrm.LEER, visible.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.TRATO, AccionCrm.LEER, visible.getTratoId().value())).thenReturn(true);

        var page = new GetAllTareasService(authorization, rows).getPage(criteria);

        assertEquals(List.of(visible), page.items());
        assertEquals(1, page.totalItems());
        assertEquals(1, page.totalPages());
        verify(rows).findAll(criteria);
        verify(rows, never()).findPage(criteria);
    }

    private static CrmAuthorization authorization() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        when(authorization.readPolicy(RecursoCrm.TAREA)).thenReturn(new ResourceReadPolicy(
                AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of(), Set.of(), Set.of()));
        return authorization;
    }

    private static Tarea task(String title, TratoId tratoId) {
        return Tarea.create(tratoId, UsuarioId.create(), title, null, TipoTarea.GENERAL,
                PrioridadTarea.MEDIA, LocalDateTime.now().plusDays(1));
    }
}