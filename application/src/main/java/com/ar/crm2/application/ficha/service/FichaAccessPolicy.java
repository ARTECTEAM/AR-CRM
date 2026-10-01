package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.enums.TipoFicha;

import java.util.List;
import java.util.UUID;

/** Authorization checks for the commercial references carried by a Ficha. */
final class FichaAccessPolicy {

    private FichaAccessPolicy() {
    }

    static boolean permitsLinkedRecords(CrmAuthorization authorization, Ficha ficha) {
        boolean parentReadable = switch (ficha.getTipoFicha()) {
            case TRATO -> ficha.getTratoId() != null && authorization.permitsRecord(
                RecursoCrm.TRATO, AccionCrm.LEER, ficha.getTratoId().value());
            case TAREA -> ficha.getTareaId() != null && authorization.permitsRecord(
                RecursoCrm.TAREA, AccionCrm.LEER, ficha.getTareaId().value());
        };
        return parentReadable
            && authorization.permitsRecord(RecursoCrm.COLUMNA, AccionCrm.LEER, ficha.getColumnaId().value())
            && ficha.getEtiquetas().stream().allMatch(etiqueta -> authorization.permitsRecord(
                RecursoCrm.ETIQUETA, AccionCrm.LEER, etiqueta.getEtiquetaId().value()));
    }

    static void requireLinkedRecords(CrmAuthorization authorization, Ficha ficha) {
        authorization.requireRecord(RecursoCrm.COLUMNA, AccionCrm.LEER, ficha.getColumnaId().value());
        if (ficha.getTipoFicha() == TipoFicha.TRATO) {
            authorization.requireRecord(RecursoCrm.TRATO, AccionCrm.LEER, ficha.getTratoId().value());
        } else {
            authorization.requireRecord(RecursoCrm.TAREA, AccionCrm.LEER, ficha.getTareaId().value());
        }
    }

    static void requireReadableReferences(CrmAuthorization authorization, Ficha ficha) {
        requireLinkedRecords(authorization, ficha);
        ficha.getEtiquetas().forEach(etiqueta -> authorization.requireRecord(
            RecursoCrm.ETIQUETA, AccionCrm.LEER, etiqueta.getEtiquetaId().value()));
    }

    static void requireWriteTargets(CrmAuthorization authorization, Ficha ficha, List<UUID> etiquetaIds) {
        requireLinkedRecords(authorization, ficha);
        if (etiquetaIds != null) {
            etiquetaIds.forEach(id -> authorization.requireRecord(RecursoCrm.ETIQUETA, AccionCrm.LEER, id));
        }
    }

    static void requireDestinationCandidate(CrmAuthorization authorization, AccionCrm action, UUID columnId) {
        authorization.requireCandidate(RecursoCrm.FICHA, action,
            new ResourceScopeCandidate(RecursoCrm.COLUMNA, columnId, null, null));
    }
}
