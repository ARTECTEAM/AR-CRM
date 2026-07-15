package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.ficha.command.MoverColumnaFichaCommand;
import com.ar.crm2.application.ficha.exception.FichaMovimientoIncompatibleException;
import com.ar.crm2.application.ficha.exception.FichaNotFoundException;
import com.ar.crm2.application.ficha.port.out.ExistsColumnaCompatibleConFichaPort;
import com.ar.crm2.application.ficha.port.out.FindFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.FichaId;
import com.ar.crm2.model.vo.TareaId;
import com.ar.crm2.model.vo.TratoId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoverColumnaFichaServiceTest {

    @Mock
    private FindFichaByIdPort findPort;

    @Mock
    private SaveFichaPort savePort;

    @Mock
    private ExistsColumnaCompatibleConFichaPort compatibilityPort;

    private MoverColumnaFichaService service;

    @BeforeEach
    void setUp() {
        service = new MoverColumnaFichaService(findPort, savePort, compatibilityPort);
    }

    @Test
    void moverTareaAColumnaTareas_permiteMovimiento() {
        Ficha ficha = Ficha.create(ColumnaId.create(), TipoFicha.TAREA, null, TareaId.create());
        ColumnaId target = ColumnaId.create();

        when(findPort.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(compatibilityPort.existsCompatibleColumn(target, TipoTablero.TAREAS)).thenReturn(true);
        when(savePort.save(org.mockito.ArgumentMatchers.any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        Ficha result = service.moverAColumna(new MoverColumnaFichaCommand(ficha.getId().value(), target.value()));

        assertThat(result.getColumnaId()).isEqualTo(target);
        assertThat(result.getTipoFicha()).isEqualTo(TipoFicha.TAREA);
        verify(savePort).save(org.mockito.ArgumentMatchers.any(Ficha.class));
    }

    @Test
    void moverTareaAColumnaTratos_rechazaMovimiento() {
        Ficha ficha = Ficha.create(ColumnaId.create(), TipoFicha.TAREA, null, TareaId.create());
        ColumnaId target = ColumnaId.create();

        when(findPort.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(compatibilityPort.existsCompatibleColumn(target, TipoTablero.TAREAS)).thenReturn(false);

        assertThatThrownBy(() -> service.moverAColumna(new MoverColumnaFichaCommand(ficha.getId().value(), target.value())))
            .isInstanceOf(FichaMovimientoIncompatibleException.class);

        verify(savePort, never()).save(org.mockito.ArgumentMatchers.any(Ficha.class));
    }

    @Test
    void moverTratoAColumnaTratos_permiteMovimientoSinCambiarTipo() {
        Ficha ficha = Ficha.create(ColumnaId.create(), TipoFicha.TRATO, TratoId.create(), null);
        ColumnaId target = ColumnaId.create();

        when(findPort.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(compatibilityPort.existsCompatibleColumn(target, TipoTablero.TRATOS)).thenReturn(true);
        when(savePort.save(org.mockito.ArgumentMatchers.any(Ficha.class))).thenAnswer(inv -> inv.getArgument(0));

        Ficha result = service.moverAColumna(new MoverColumnaFichaCommand(ficha.getId().value(), target.value()));

        assertThat(result.getColumnaId()).isEqualTo(target);
        assertThat(result.getTipoFicha()).isEqualTo(TipoFicha.TRATO);
        assertThat(result.getTratoId()).isEqualTo(ficha.getTratoId());
    }

    @Test
    void moverTratoAColumnaTareas_rechazaMovimiento() {
        Ficha ficha = Ficha.create(ColumnaId.create(), TipoFicha.TRATO, TratoId.create(), null);
        ColumnaId target = ColumnaId.create();

        when(findPort.findById(ficha.getId())).thenReturn(Optional.of(ficha));
        when(compatibilityPort.existsCompatibleColumn(target, TipoTablero.TRATOS)).thenReturn(false);

        assertThatThrownBy(() -> service.moverAColumna(new MoverColumnaFichaCommand(ficha.getId().value(), target.value())))
            .isInstanceOf(FichaMovimientoIncompatibleException.class);

        verify(savePort, never()).save(org.mockito.ArgumentMatchers.any(Ficha.class));
    }

    @Test
    void fichaInexistente_lanzaNotFoundYNoValidaColumna() {
        FichaId fichaId = FichaId.create();
        ColumnaId target = ColumnaId.create();
        when(findPort.findById(fichaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.moverAColumna(new MoverColumnaFichaCommand(fichaId.value(), target.value())))
            .isInstanceOf(FichaNotFoundException.class);

        verify(compatibilityPort, never()).existsCompatibleColumn(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(savePort, never()).save(org.mockito.ArgumentMatchers.any(Ficha.class));
    }
}
