package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.tablero.port.out.FindAllTablerosPort;
import com.ar.crm2.application.tablero.query.TableroFilterCriteria;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.ColumnaTablero;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TableroId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAllTablerosAuthorizationCompatibilityTest {

    @Test
    void deniesListingBeforeQueryWhenReadIsNotGranted() {
        FindAllTablerosPort rows = mock(FindAllTablerosPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).require(RecursoCrm.TABLERO, AccionCrm.LEER);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new GetAllTablerosService(authorization, rows).getAll(TableroFilterCriteria.empty()));
        verifyNoInteractions(rows);
    }

    @Test
    void filtersUnauthorizedBoardsAndBoardsWithUnreadableColumns() {
        Tablero hiddenBoard = board(ColumnaId.create());
        Tablero unreadableColumnBoard = board(ColumnaId.create());
        Tablero visibleBoard = board(ColumnaId.create());
        FindAllTablerosPort rows = mock(FindAllTablerosPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        TableroFilterCriteria criteria = new TableroFilterCriteria(TipoTablero.TRATOS);
        when(rows.findAll(criteria)).thenReturn(List.of(hiddenBoard, unreadableColumnBoard, visibleBoard));
        when(authorization.permitsRecord(RecursoCrm.TABLERO, AccionCrm.LEER, hiddenBoard.getId().value())).thenReturn(false);
        when(authorization.permitsRecord(RecursoCrm.TABLERO, AccionCrm.LEER, unreadableColumnBoard.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.COLUMNA, AccionCrm.LEER,
                unreadableColumnBoard.getColumnasTablero().getFirst().getColumnaId().value())).thenReturn(false);
        when(authorization.permitsRecord(RecursoCrm.TABLERO, AccionCrm.LEER, visibleBoard.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.COLUMNA, AccionCrm.LEER,
                visibleBoard.getColumnasTablero().getFirst().getColumnaId().value())).thenReturn(true);

        var result = new GetAllTablerosService(authorization, rows).getAll(criteria);

        assertEquals(List.of(visibleBoard), result);
        verify(rows).findAll(criteria);
    }

    private static Tablero board(ColumnaId columnId) {
        Tablero board = mock(Tablero.class);
        when(board.getId()).thenReturn(TableroId.create());
        when(board.getColumnasTablero()).thenReturn(List.of(
                ColumnaTablero.reconstitute(columnId, TipoTablero.TRATOS, 1, null, BigDecimal.ZERO)));
        return board;
    }
}