package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;

import com.ar.crm2.application.columna.command.CreateColumnaCommand;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.out.FindAllColumnasPort;
import com.ar.crm2.application.tablero.command.CreateTableroCommand;
import com.ar.crm2.application.tablero.port.out.SaveTableroPort;
import com.ar.crm2.application.tablero.port.in.CreateTableroUseCase;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.entity.ColumnaTablero;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoTablero;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Application service implementing CreateTableroUseCase.
 *
 * <p>Coordination responsibility only:
 * <ol>
 *   <li>Resolve the default board shape from {@link Tablero#requiredDefaultColumns(TipoTablero)}.</li>
 *   <li>For each default column, look up the existing PREDETERMINADA catalog
 *       entry through {@link Columna#matchesDefaultCatalog(TipoTablero, String)}
 *       or create one through {@link CreateColumnaUseCase}.</li>
 *   <li>Build the {@link ColumnaTablero} list with the catalog id and the
 *       contextual WIP from the spec.</li>
 *   <li>Delegate aggregate creation to {@link Tablero#create(String, String, TipoTablero, List)}.</li>
 *   <li>Persist via {@link SaveTableroPort}.</li>
 * </ol>
 *
 * <p>Business invariants (column identity, duplicate rules, default catalog
 * matching, board shape) are owned by the domain. Authorization (which
 * authenticated actor may create a board) is owned by the application/security
     * layer; this service receives the resolved actor id via the command for
     * logging/audit purposes only. Default catalog creation does not pretend
     * a normal user id is a {@code SuperUsuarioId}; catalog authorization is
     * outside the Tablero aggregate creation invariant.
 *
 * <p>No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class CreateTableroService implements CreateTableroUseCase {

    private final CrmAuthorization crmAuthorization;

    private final SaveTableroPort savePort;
    private final FindAllColumnasPort findAllColumnasPort;
    private final CreateColumnaUseCase createColumnaUseCase;

    @Override
    public Tablero create(CreateTableroCommand command) {
        crmAuthorization.require(RecursoCrm.TABLERO, AccionCrm.CREAR);
        crmAuthorization.requireCandidate(RecursoCrm.TABLERO, AccionCrm.CREAR,
            new ResourceScopeCandidate(null, null, null, null));
        // Board creation materializes default catalog columns; require both
        // resource capabilities regardless of whether the defaults already exist.
        crmAuthorization.require(RecursoCrm.COLUMNA, AccionCrm.CREAR);
        crmAuthorization.requireCandidate(RecursoCrm.COLUMNA, AccionCrm.CREAR,
            new ResourceScopeCandidate(null, null, null, null));

        List<ColumnaTablero> columnasPredeterminadas = buildDefaultColumns(command);

        Tablero tablero = Tablero.create(
            command.nombre(),
            command.descripcion(),
            command.tipoTablero(),
            columnasPredeterminadas
        );

        return savePort.save(tablero);
    }

    /**
     * Builds the default {@link ColumnaTablero} list for the given board type.
     *
     * <p>Reads the canonical board shape (catalog name + default WIP) from
     * {@link Tablero#requiredDefaultColumns(TipoTablero)}, resolves each name
     * to an existing PREDETERMINADA catalog column via
     * {@link Columna#matchesDefaultCatalog(TipoTablero, String)}, and creates
     * a new catalog column via {@link CreateColumnaUseCase} if no match is
     * found.
     *
     * @param command the board creation command
     * @return list of 4 default ColumnaTablero contextual wrappers
     */
    private List<ColumnaTablero> buildDefaultColumns(CreateTableroCommand command) {
        TipoTablero tipoTablero = command.tipoTablero();
        List<Columna> existingColumnas = findAllColumnasPort.findAll();
        List<Tablero.DefaultColumnSpec> specs = Tablero.requiredDefaultColumns(tipoTablero);
        List<ColumnaTablero> columnas = new ArrayList<>(specs.size());

        for (Tablero.DefaultColumnSpec spec : specs) {
            columnas.add(makeColumnaTablero(existingColumnas, command, spec));
        }

        return columnas;
    }

    private ColumnaTablero makeColumnaTablero(
        List<Columna> existingColumnas,
        CreateTableroCommand command,
        Tablero.DefaultColumnSpec spec
    ) {
        TipoTablero tipoTablero = command.tipoTablero();
        Columna persisted = resolveDefaultCatalogColumn(existingColumnas, command, spec.name(), tipoTablero);

        return ColumnaTablero.create(
            persisted.getId(),
            tipoTablero,
            spec.defaultLimiteWip(),
            null,
            BigDecimal.ZERO
        );
    }

    private Columna resolveDefaultCatalogColumn(
        List<Columna> existingColumnas,
        CreateTableroCommand command,
        String nombre,
        TipoTablero tipoTablero
    ) {
        for (Columna candidata : existingColumnas) {
            if (candidata.matchesDefaultCatalog(tipoTablero, nombre)) {
                return candidata;
            }
        }

        return createColumnaUseCase.create(new CreateColumnaCommand(
            Optional.empty(),
            nombre,
            "#FFFFFF",
            tipoTablero,
            TipoColumna.PREDETERMINADA,
            true
        ));
    }
}
