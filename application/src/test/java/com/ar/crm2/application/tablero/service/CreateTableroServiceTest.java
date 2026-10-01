package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.AllowAllCrmAuthorization;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.columna.command.CreateColumnaCommand;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.out.FindAllColumnasPort;
import com.ar.crm2.application.tablero.command.CreateTableroCommand;
import com.ar.crm2.application.tablero.port.out.SaveTableroPort;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.SuperUsuarioId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CreateTableroServiceTest {

    @Test
    void create_deniesScopedBoardCandidateBeforeCatalogAccess() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        SaveTableroPort savePort = mock(SaveTableroPort.class);
        FindAllColumnasPort findPort = mock(FindAllColumnasPort.class);
        CreateColumnaUseCase createColumn = mock(CreateColumnaUseCase.class);
        ResourceScopeCandidate candidate = new ResourceScopeCandidate(null, null, null, null);
        doThrow(new com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException("Denied"))
            .when(authorization).requireCandidate(RecursoCrm.TABLERO, AccionCrm.CREAR, candidate);
        CreateTableroService service = new CreateTableroService(authorization, savePort, findPort, createColumn);

        assertThrows(com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException.class,
            () -> service.create(command(TipoTablero.TAREAS)));
        verifyNoInteractions(findPort, createColumn, savePort);
    }

    @Test
    void create_deniesScopedDefaultColumnCandidateBeforeCatalogAccess() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        SaveTableroPort savePort = mock(SaveTableroPort.class);
        FindAllColumnasPort findPort = mock(FindAllColumnasPort.class);
        CreateColumnaUseCase createColumn = mock(CreateColumnaUseCase.class);
        ResourceScopeCandidate candidate = new ResourceScopeCandidate(null, null, null, null);
        doNothing().when(authorization)
            .requireCandidate(RecursoCrm.TABLERO, AccionCrm.CREAR, candidate);
        doThrow(new com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException("Denied"))
            .when(authorization).requireCandidate(RecursoCrm.COLUMNA, AccionCrm.CREAR, candidate);
        CreateTableroService service = new CreateTableroService(authorization, savePort, findPort, createColumn);

        assertThrows(com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException.class,
            () -> service.create(command(TipoTablero.TAREAS)));
        verifyNoInteractions(findPort, createColumn, savePort);
    }

    @Test
    void create_reusesExistingDefaultCatalogColumnsAcrossBoards() {
        InMemoryColumnaCatalog catalog = new InMemoryColumnaCatalog();
        CreateTableroService service = new CreateTableroService(new AllowAllCrmAuthorization(), new PassThroughSaveTableroPort(), catalog, catalog);

        CreateTableroCommand firstCommand = command(TipoTablero.TAREAS);
        CreateTableroCommand secondCommand = command(TipoTablero.TAREAS);

        Tablero firstBoard = service.create(firstCommand);
        Tablero secondBoard = service.create(secondCommand);

        assertEquals(4, catalog.createdCount());
        assertEquals(4, catalog.findAll().size());
        assertEquals(
            firstBoard.getColumnasTablero().stream().map(columna -> columna.getColumnaId().value()).toList(),
            secondBoard.getColumnasTablero().stream().map(columna -> columna.getColumnaId().value()).toList()
        );
    }

    @Test
    void create_createsOnlyMissingDefaultCatalogColumns() {
        InMemoryColumnaCatalog catalog = new InMemoryColumnaCatalog();
        catalog.seedDefault(TipoTablero.TRATOS, "Abierto");
        catalog.seedDefault(TipoTablero.TRATOS, "Ganado");

        CreateTableroService service = new CreateTableroService(new AllowAllCrmAuthorization(), new PassThroughSaveTableroPort(), catalog, catalog);

        Tablero board = service.create(command(TipoTablero.TRATOS));

        assertNotNull(board);
        assertEquals(2, catalog.createdCount());
        assertEquals(4, catalog.findAll().size());
        assertEquals(
            List.of("Abierto", "Archived", "Ganado", "Perdido"),
            catalog.findAll().stream().map(Columna::getColumnanombre).sorted().toList()
        );
    }

    private static CreateTableroCommand command(TipoTablero tipoTablero) {
        return new CreateTableroCommand(
            "Board " + tipoTablero,
            "Board description",
            tipoTablero,
            true,
            UUID.randomUUID()
        );
    }

    private static final class PassThroughSaveTableroPort implements SaveTableroPort {
        @Override
        public Tablero save(Tablero tablero) {
            return tablero;
        }
    }

    private static final class InMemoryColumnaCatalog implements FindAllColumnasPort, CreateColumnaUseCase {
        private final List<Columna> columnas = new ArrayList<>();
        private int createdCount;

        @Override
        public List<Columna> findAll() {
            return List.copyOf(columnas);
        }

        @Override
        public Columna create(CreateColumnaCommand command) {
            createdCount++;

            Columna columna = Columna.create(
                command.superUsuarioId().map(SuperUsuarioId::from).orElse(null),
                command.nombre(),
                command.tipoTablero(),
                command.tipoColumna(),
                command.color(),
                false
            );
            columnas.add(columna);
            columnas.sort(Comparator.comparing(Columna::getColumnanombre));
            return columna;
        }

        void seedDefault(TipoTablero tipoTablero, String nombre) {
            columnas.add(Columna.create(
                SuperUsuarioId.create(),
                nombre,
                tipoTablero,
                TipoColumna.PREDETERMINADA,
                "#FFFFFF",
                false
            ));
            columnas.sort(Comparator.comparing(Columna::getColumnanombre));
        }

        int createdCount() {
            return createdCount;
        }
    }
}
