package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.tablero.port.out.FindInitialColumnPort;
import com.ar.crm2.application.trato.command.CreateTratoCommand;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CreateTratoSensitiveFieldsAuthorizationTest {

    @Test
    void createWithoutExplicitFinancialValuesDoesNotRequireFinancialWriteAccess() {
        Fixture fixture = new Fixture();

        assertDoesNotThrow(() -> fixture.service.create(fixture.command(null, null, null)));

        verify(fixture.authorization, never()).requireWritableGroups(any(), any());
        verify(fixture.saveTrato).save(any());
        verify(fixture.saveFicha).save(any());
    }

    @Test
    void createWithAnExplicitFinancialValueRequiresFinancialWriteAccessBeforeSaving() {
        Fixture fixture = new Fixture();
        doThrow(new SecurityException("Financial writes are not allowed"))
            .when(fixture.authorization).requireWritableGroups(eq(com.ar.crm2.model.autorizacion.RecursoCrm.TRATO), any());

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
            () -> fixture.service.create(fixture.command(new BigDecimal("50.00"), null, null)));

        verifyNoInteractions(fixture.saveTrato, fixture.saveFicha);
    }

    @Test
    void createRequiresTheResultingAssigneeCandidateBeforeLoadingTheInitialColumn() {
        Fixture fixture = new Fixture();
        UUID responsibleId = UUID.randomUUID();
        doThrow(new SecurityException("Assignee outside scope"))
            .when(fixture.authorization).requireCandidate(eq(RecursoCrm.TRATO), eq(AccionCrm.CREAR),
                any(ResourceScopeCandidate.class));

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
            () -> fixture.service.create(fixture.command(responsibleId, null, null, null)));

        verify(fixture.authorization).requireCandidate(eq(RecursoCrm.TRATO), eq(AccionCrm.CREAR),
            eq(new ResourceScopeCandidate(null, null, responsibleId, null)));
        verifyNoInteractions(fixture.findInitialColumn, fixture.saveTrato, fixture.saveFicha);
    }

    @Test
    void generatedFichaCandidateIsCheckedBeforeSavingTheDeal() {
        Fixture fixture = new Fixture();
        doThrow(new SecurityException("Initial Ficha outside scope"))
            .when(fixture.authorization).requireCandidate(eq(RecursoCrm.FICHA), eq(AccionCrm.CREAR),
                any(ResourceScopeCandidate.class));

        org.junit.jupiter.api.Assertions.assertThrows(SecurityException.class,
            () -> fixture.service.create(fixture.command(null, null, null)));

        verifyNoInteractions(fixture.saveTrato, fixture.saveFicha);
    }

    private static final class Fixture {
        private final CrmAuthorization authorization = mock(CrmAuthorization.class);
        private final SaveTratoPort saveTrato = mock(SaveTratoPort.class);
        private final SaveFichaPort saveFicha = mock(SaveFichaPort.class);
        private final FindInitialColumnPort findInitialColumn = mock(FindInitialColumnPort.class);
        private final CreateTratoService service = new CreateTratoService(
            authorization, saveTrato, saveFicha, findInitialColumn
        );

        private Fixture() {
            Columna initial = mock(Columna.class);
            when(initial.getId()).thenReturn(ColumnaId.from(UUID.randomUUID()));
            when(findInitialColumn.findInitialColumn(TipoTablero.TRATOS)).thenReturn(Optional.of(initial));
            when(saveTrato.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(saveFicha.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        }

        private CreateTratoCommand command(BigDecimal value, Integer probability, LocalDate closeDate) {
            return command(UUID.randomUUID(), value, probability, closeDate);
        }

        private CreateTratoCommand command(
            UUID responsibleId, BigDecimal value, Integer probability, LocalDate closeDate) {
            return new CreateTratoCommand(UUID.randomUUID(), responsibleId, "Deal",
                value, probability, closeDate, TipoContrato.OTRO);
        }
    }
}
