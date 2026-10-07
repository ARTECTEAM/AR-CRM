package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.ficha.port.out.FindAllFichasPort;
import com.ar.crm2.application.ficha.query.FichaFilterCriteria;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TratoId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAllFichasAuthorizationCompatibilityTest {

    @Test
    void deniesListingBeforeQueryWhenReadIsNotGranted() {
        FindAllFichasPort rows = mock(FindAllFichasPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).require(RecursoCrm.FICHA, AccionCrm.LEER);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new GetAllFichasService(authorization, rows).getAll(FichaFilterCriteria.empty()));
        verifyNoInteractions(rows);
    }

    @Test
    void filtersFichaByBoardScopeAndLinkedDealRead() {
        Ficha unreadableDeal = ficha(TratoId.create(), ColumnaId.create());
        Ficha visible = ficha(TratoId.create(), ColumnaId.create());
        FindAllFichasPort rows = mock(FindAllFichasPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        FichaFilterCriteria criteria = new FichaFilterCriteria(TipoFicha.TRATO, null, null, null, null);
        when(rows.findAll(criteria)).thenReturn(List.of(unreadableDeal, visible));
        when(authorization.permitsRecord(RecursoCrm.FICHA, AccionCrm.LEER, unreadableDeal.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.TRATO, AccionCrm.LEER, unreadableDeal.getTratoId().value())).thenReturn(false);
        when(authorization.permitsRecord(RecursoCrm.FICHA, AccionCrm.LEER, visible.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.TRATO, AccionCrm.LEER, visible.getTratoId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.COLUMNA, AccionCrm.LEER, visible.getColumnaId().value())).thenReturn(true);

        var result = new GetAllFichasService(authorization, rows).getAll(criteria);

        assertEquals(List.of(visible), result);
        verify(rows).findAll(criteria);
    }

    private static Ficha ficha(TratoId dealId, ColumnaId columnId) {
        return Ficha.create(columnId, TipoFicha.TRATO, dealId, null);
    }
}