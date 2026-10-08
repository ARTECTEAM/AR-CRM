package com.ar.crm2.application.etiqueta;

import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.port.in.GetEtiquetaByIdUseCase;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetaByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.etiqueta.service.GetEtiquetaByIdService;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GetEtiquetaByIdService.
 * Verifies the existing service honors the id-aware find path and the
 * not-found exception factory. Closes a runtime-facing gap noted in
 * the slice-2 verify report.
 */
@ExtendWith(MockitoExtension.class)
class GetEtiquetaByIdServiceTest {

    @Mock
    private FindEtiquetaByIdPort findPort;

    @Mock
    private CrmAuthorization authorization;

    @InjectMocks
    private GetEtiquetaByIdService service;

    private Etiqueta buildEtiqueta(UUID id, String nombre, TipoEtiqueta tipo, String color) {
        return Etiqueta.reconstitute(
            EtiquetaId.from(id),
            nombre,
            tipo,
            color,
            LocalDateTime.now()
        );
    }

    private void authorizeRead(UUID id) {
        doNothing().when(authorization).require(RecursoCrm.ETIQUETA, AccionCrm.LEER);
        doNothing().when(authorization).requireRecord(RecursoCrm.ETIQUETA, AccionCrm.LEER, id);
    }

    @Test
    void getById_shouldReturnEtiquetaWhenFound() {
        UUID id = UUID.randomUUID();
        authorizeRead(id);
        Etiqueta etiqueta = buildEtiqueta(id, "Urgent", TipoEtiqueta.TAREA, "#FF0000");
        when(findPort.findById(EtiquetaId.from(id))).thenReturn(Optional.of(etiqueta));

        Etiqueta result = service.getById(new com.ar.crm2.application.etiqueta.command.GetEtiquetaByIdCommand(id));

        assertNotNull(result);
        assertEquals(id, result.getId().value());
        assertEquals("Urgent", result.getNombre());
        verify(findPort).findById(EtiquetaId.from(id));
    }

    @Test
    void getById_shouldThrowNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        authorizeRead(id);
        when(findPort.findById(EtiquetaId.from(id))).thenReturn(Optional.empty());

        EtiquetaNotFoundException ex = assertThrows(
            EtiquetaNotFoundException.class,
            () -> service.getById(new com.ar.crm2.application.etiqueta.command.GetEtiquetaByIdCommand(id))
        );

        assertEquals(id, ex.getEtiquetaId());
    }

    @Test
    void getById_shouldImplementInboundContract() {
        // Compile-time + behavior contract: GetEtiquetaByIdService must implement
        // the inbound use case port. If the class signature drifts, this test fails.
        assertTrue(GetEtiquetaByIdUseCase.class.isAssignableFrom(GetEtiquetaByIdService.class));
    }
}
