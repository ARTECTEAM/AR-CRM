package com.ar.crm2.application.columna.service;

import com.ar.crm2.application.security.AllowAllCrmAuthorization;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.columna.command.CreateColumnaCommand;
import com.ar.crm2.application.columna.port.out.FindAllColumnasPort;
import com.ar.crm2.application.columna.port.out.SaveColumnaPort;
import com.ar.crm2.exception.NombreColumnaYaExisteException;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.SuperUsuarioId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class CreateColumnaServiceTest {

    @Test
    void create_deniesScopedGlobalColumnCandidateBeforeCatalogReadOrSave() {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        FindAllColumnasPort findPort = mock(FindAllColumnasPort.class);
        SaveColumnaPort savePort = mock(SaveColumnaPort.class);
        ResourceScopeCandidate candidate = new ResourceScopeCandidate(null, null, null, null);
        doThrow(new com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException("Denied"))
            .when(authorization).requireCandidate(RecursoCrm.COLUMNA, AccionCrm.CREAR, candidate);
        CreateColumnaService service = new CreateColumnaService(authorization, savePort, findPort);

        assertThrows(com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException.class,
            () -> service.create(command("Scoped", TipoTablero.TAREAS)));
        verifyNoInteractions(findPort, savePort);
    }

    @Test
    void create_rejectsTrimmedDuplicateNameWithinSameBoardType() {
        Columna existing = Columna.create(
            SuperUsuarioId.create(),
            "Pendiente",
            TipoTablero.TAREAS,
            TipoColumna.PREDETERMINADA,
            "#FFFFFF",
            false
        );

        CreateColumnaService service = new CreateColumnaService(new AllowAllCrmAuthorization(), passThroughSavePort(), fixedCatalog(existing));

        assertThrows(
            NombreColumnaYaExisteException.class,
            () -> service.create(command("  Pendiente  ", TipoTablero.TAREAS))
        );
    }

    @Test
    void create_allowsCaseVariantBecauseNormalizationIsTrimOnly() {
        Columna existing = Columna.create(
            SuperUsuarioId.create(),
            "Pendiente",
            TipoTablero.TAREAS,
            TipoColumna.PREDETERMINADA,
            "#FFFFFF",
            false
        );

        CreateColumnaService service = new CreateColumnaService(new AllowAllCrmAuthorization(), passThroughSavePort(), fixedCatalog(existing));

        Columna created = service.create(command("pendiente", TipoTablero.TAREAS));

        assertEquals("pendiente", created.getColumnanombre());
    }

    @Test
    void create_allowsSameNameForDifferentBoardType() {
        Columna existing = Columna.create(
            SuperUsuarioId.create(),
            "Pendiente",
            TipoTablero.TAREAS,
            TipoColumna.PREDETERMINADA,
            "#FFFFFF",
            false
        );

        CreateColumnaService service = new CreateColumnaService(new AllowAllCrmAuthorization(), passThroughSavePort(), fixedCatalog(existing));

        Columna created = service.create(command("Pendiente", TipoTablero.TRATOS));

        assertEquals(TipoTablero.TRATOS, created.getTipoTablero());
        assertEquals("Pendiente", created.getColumnanombre());
    }

    private static CreateColumnaCommand command(String nombre, TipoTablero tipoTablero) {
        return new CreateColumnaCommand(
            Optional.of(UUID.randomUUID()),
            nombre,
            "#FFFFFF",
            tipoTablero,
            TipoColumna.PREDETERMINADA
        );
    }

    private static SaveColumnaPort passThroughSavePort() {
        return columna -> columna;
    }

    private static FindAllColumnasPort fixedCatalog(Columna... columnas) {
        return () -> List.of(columnas);
    }
}
