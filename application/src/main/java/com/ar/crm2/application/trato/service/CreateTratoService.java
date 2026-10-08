package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;

import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.application.tablero.port.out.FindInitialColumnPort;
import com.ar.crm2.application.trato.command.CreateTratoCommand;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.application.trato.port.in.CreateTratoUseCase;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class CreateTratoService implements CreateTratoUseCase {

    private final CrmAuthorization crmAuthorization;

    private final SaveTratoPort savePort;
    private final SaveFichaPort saveFichaPort;
    private final FindInitialColumnPort findInitialColumnPort;

    @Override
    public Trato create(CreateTratoCommand command) {
        crmAuthorization.require(RecursoCrm.TRATO, AccionCrm.CREAR);
        crmAuthorization.requireCandidate(RecursoCrm.TRATO, AccionCrm.CREAR,
            new ResourceScopeCandidate(null, null, command.responsableId(), null));
        crmAuthorization.requireRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, command.contactoId());
        crmAuthorization.require(RecursoCrm.FICHA, AccionCrm.CREAR);
        if (command.valorEstimado() != null || command.probabilidad() != null
            || command.fechaCierreEsperada() != null) {
            crmAuthorization.requireWritableGroups(RecursoCrm.TRATO, Set.of(GrupoCampoSensible.FINANCIERO));
        }
        Columna columnaInicial = findInitialColumnPort.findInitialColumn(TipoTablero.TRATOS)
            .orElseThrow(() -> new IllegalStateException(
                "No initial column found for global TRATOS board. Cannot create Trato without Kanban Ficha."
            ));
        crmAuthorization.requireCandidate(RecursoCrm.FICHA, AccionCrm.CREAR,
            new ResourceScopeCandidate(RecursoCrm.COLUMNA, columnaInicial.getId().value(), null, null));
        crmAuthorization.requireRecord(RecursoCrm.COLUMNA, AccionCrm.LEER, columnaInicial.getId().value());

        Trato trato = Trato.create(
            ContactoId.from(command.contactoId()),
            UsuarioId.from(command.responsableId()),
            command.nombre(),
            command.valorEstimado(),
            command.probabilidad(),
            command.fechaCierreEsperada(),
            command.tipoContrato()
        );
        Trato savedTrato = savePort.save(trato);

        Ficha ficha = Ficha.create(
            columnaInicial.getId(),
            TipoFicha.TRATO,
            savedTrato.getId(),
            null
        );
        saveFichaPort.save(ficha);

        return savedTrato;
    }
}
