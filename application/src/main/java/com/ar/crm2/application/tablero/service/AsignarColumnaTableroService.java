package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;

import com.ar.crm2.application.tablero.command.AsignarColumnaTableroCommand;
import com.ar.crm2.application.tablero.exception.TableroNotFoundException;
import com.ar.crm2.application.tablero.port.in.AsignarColumnaTableroUseCase;
import com.ar.crm2.application.tablero.port.out.ExistsColumnaEnTableroPort;
import com.ar.crm2.application.tablero.port.out.FindColumnaByIdPort;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.SaveTableroPort;
import com.ar.crm2.exception.ColumnaYaExisteEnTableroException;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.entity.ColumnaTablero;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TableroId;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * Application service implementing AsignarColumnaTableroUseCase.
 *
 * <p>Flow:
 * <ol>
 *   <li>Load the Tablero aggregate via FindTableroByIdPort</li>
 *   <li>Load the catalog Columna via FindColumnaByIdPort</li>
 *   <li>Check for duplicate assignment via ExistsColumnaEnTableroPort</li>
 *   <li>Build the ColumnaTablero contextual wrapper</li>
 *   <li>Delegate to {@link Tablero#agregarColumnaTablero}</li>
 *   <li>Persist the updated aggregate via SaveTableroPort</li>
 * </ol>
 */
@RequiredArgsConstructor
public class AsignarColumnaTableroService implements AsignarColumnaTableroUseCase {

    private final CrmAuthorization crmAuthorization;

    private final FindTableroByIdPort findTableroPort;
    private final FindColumnaByIdPort findColumnaPort;
    private final ExistsColumnaEnTableroPort existsColumnaEnTableroPort;
    private final SaveTableroPort savePort;

    @Override
    public Tablero asignarColumna(AsignarColumnaTableroCommand command) {
        TableroId tableroId = TableroId.from(command.tableroId());
        ColumnaId columnaId = ColumnaId.from(command.columnaId());
        crmAuthorization.requireRecord(RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR, command.tableroId());
        crmAuthorization.requireRecord(RecursoCrm.COLUMNA, AccionCrm.LEER, command.columnaId());

        // Load Tablero
        Tablero existing = findTableroPort.findById(tableroId)
                .orElseThrow(() -> TableroNotFoundException.forId(command.tableroId()));
        if (existing.getTipoTablero() == com.ar.crm2.model.enums.TipoTablero.TRATOS) {
            crmAuthorization.requireWritableGroups(RecursoCrm.TABLERO, Set.of(GrupoCampoSensible.FINANCIERO));
        }

        // Load catalog Columna to get tipoTablero for ColumnaTablero factory
        Columna columna = findColumnaPort.findById(columnaId)
                .orElseThrow(() -> com.ar.crm2.application.columna.exception.ColumnaNotFoundException.forId(columnaId.value()));

        // Duplicate guard: check if column is already assigned to this board
        if (existsColumnaEnTableroPort.existsByTableroIdAndColumnaId(tableroId, columnaId)) {
            throw new ColumnaYaExisteEnTableroException();
        }

        // Build the contextual ColumnaTablero wrapper (uses tipoTablero from Columna)
        ColumnaTablero columnaTablero = ColumnaTablero.create(
                columnaId,
                columna.getTipoTablero(),
                command.limiteWip(),
                command.nota(),
                command.totalValorEstimado()
        );

        // Delegate to domain aggregate
        Tablero updated = existing.agregarColumnaTablero(columnaTablero);

        return savePort.save(updated);
    }
}
