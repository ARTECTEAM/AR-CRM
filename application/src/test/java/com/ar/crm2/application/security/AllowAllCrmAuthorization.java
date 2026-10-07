package com.ar.crm2.application.security;

import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** Test fixture for application service tests unrelated to authorization decisions. */
public final class AllowAllCrmAuthorization implements CrmAuthorization {

    private static final ResourceReadPolicy POLICY = new ResourceReadPolicy(
        AlcanceCrm.TODO_COMPARTIDO,
        Set.of(GrupoCampoSensible.values()),
        Set.of(),
        Set.of(GrupoCampoSensible.values())
    );

    @Override
    public void require(RecursoCrm recurso, AccionCrm accion) {
    }

    @Override
    public boolean permitsRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
        return true;
    }

    @Override
    public void requireRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
    }

    @Override
    public void requireCandidate(RecursoCrm recurso, AccionCrm accion, ResourceScopeCandidate candidate) {
    }

    @Override
    public void requireCanDelegateGrants(Collection<PermisoRecurso> requestedGrants) {
    }

    @Override
    public ResourceReadPolicy readPolicy(RecursoCrm recurso) {
        return POLICY;
    }

    @Override
    public ResourceReadPolicy fieldPolicy(RecursoCrm recurso) {
        return POLICY;
    }

    @Override
    public void requireWritableGroups(RecursoCrm recurso, Set<GrupoCampoSensible> groups) {
    }

    @Override
    public String revision() {
        return "test";
    }
}
