package com.ar.crm2.application.columna.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;

import com.ar.crm2.application.columna.command.CreateColumnaCommand;
import com.ar.crm2.application.columna.port.in.CreateColumnaUseCase;
import com.ar.crm2.application.columna.port.out.FindAllColumnasPort;
import com.ar.crm2.application.columna.port.out.SaveColumnaPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.vo.SuperUsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Application service implementing CreateColumnaUseCase.
 *
 * <p>Coordination responsibility only:
 * <ol>
 *   <li>Scan the catalog through {@link FindAllColumnasPort}.</li>
 *   <li>Delegate duplicate-scope evaluation to
 *       {@link Columna#hasDuplicateForCreate(java.util.List, com.ar.crm2.model.enums.TipoTablero, String)}.</li>
 *   <li>Delegate entity creation to {@link Columna#create}.</li>
 *   <li>Persist via {@link SaveColumnaPort}.</li>
 * </ol>
 *
 * <p>The previous implementation delegated duplicate evaluation to the
 * now-removed {@code ColumnaNamePolicy} application helper. Duplicate
 * detection is a domain rule (the catalog identity lives in
 * {@link Columna}) and now lives in {@link Columna#hasDuplicateForCreate}.
 *
 * <p>No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class CreateColumnaService implements CreateColumnaUseCase {

    private final CrmAuthorization crmAuthorization;

    private final SaveColumnaPort savePort;
    private final FindAllColumnasPort findAllPort;

    @Override
    public Columna create(CreateColumnaCommand command) {
        crmAuthorization.require(RecursoCrm.COLUMNA, AccionCrm.CREAR);
        crmAuthorization.requireCandidate(RecursoCrm.COLUMNA, AccionCrm.CREAR,
            new ResourceScopeCandidate(null, null, null, null));
        if (command.tipoColumna() == TipoColumna.PREDETERMINADA
            && !command.defaultCatalogBootstrap()
            && command.superUsuarioId().isEmpty()) {
            throw new IllegalArgumentException("superUsuarioId is required to create PREDETERMINADA columns");
        }

        boolean existeDuplicado = Columna.hasDuplicateForCreate(
            findAllPort.findAll(),
            command.tipoTablero(),
            command.nombre()
        );

        Columna columna = Columna.create(
            command.superUsuarioId()
                .map(SuperUsuarioId::from)
                .orElse(null),
            command.nombre(),
            command.tipoTablero(),
            command.tipoColumna(),
            command.color(),
            existeDuplicado
        );

        return savePort.save(columna);
    }
}
