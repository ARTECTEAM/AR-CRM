package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.application.tablero.command.GetTableroByIdCommand;
import com.ar.crm2.application.tablero.exception.TableroNotFoundException;
import com.ar.crm2.application.tablero.port.out.FindAllTablerosPort;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.model.entity.ColumnaTablero;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TableroId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TableroNestedColumnAuthorizationTest {

    @Test
    void listExcludesBoardsWithAnyUnreadableAssignedColumn() {
        UUID hiddenColumnId = UUID.randomUUID();
        Tablero visibleBoard = board(UUID.randomUUID());
        Tablero partiallyVisibleBoard = board(UUID.randomUUID(), hiddenColumnId);
        FindAllTablerosPort findAll = () -> List.of(visibleBoard, partiallyVisibleBoard);
        GetAllTablerosService service = new GetAllTablerosService(
            new TestCrmAuthorization().denyRecord(hiddenColumnId), findAll);

        assertEquals(List.of(visibleBoard), service.getAll());
    }

    @Test
    void directReadUsesNotFoundWhenAnyAssignedColumnIsUnreadable() {
        UUID hiddenColumnId = UUID.randomUUID();
        Tablero board = board(UUID.randomUUID(), hiddenColumnId);
        FindTableroByIdPort find = mock(FindTableroByIdPort.class);
        when(find.findById(TableroId.from(board.getId().value()))).thenReturn(Optional.of(board));
        GetTableroByIdService service = new GetTableroByIdService(
            new TestCrmAuthorization().denyRecord(hiddenColumnId), find);

        assertThrows(TableroNotFoundException.class,
            () -> service.getById(new GetTableroByIdCommand(board.getId().value())));
    }

    private static Tablero board(UUID... columnIds) {
        List<ColumnaTablero> columns = java.util.Arrays.stream(columnIds)
            .map(id -> ColumnaTablero.create(ColumnaId.from(id), TipoTablero.TAREAS, 1, null, BigDecimal.ZERO))
            .toList();
        return Tablero.reconstitute(TableroId.from(UUID.randomUUID()), "Board", "Description",
            columns, TipoTablero.TAREAS, LocalDateTime.now());
    }
}
