package com.ar.crm2.adapter.in.rest.dto.request;

import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** One resource row in the role editor; all values come from fixed server enums. */
public record PermisoRolRequest(
        @NotNull
        RecursoCrm recurso,
        Set<AccionCrm> acciones,
        @NotNull
        AlcanceCrm alcance,
        Set<UUID> idsPermitidos,
        Set<GrupoCampoSensible> gruposLectura,
        Set<GrupoCampoSensible> gruposEscritura
) {
}
