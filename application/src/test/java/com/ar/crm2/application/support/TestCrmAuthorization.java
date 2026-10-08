package com.ar.crm2.application.support;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;

import java.util.EnumMap;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/** Small deterministic test double for application service authorization boundaries. */
public class TestCrmAuthorization implements CrmAuthorization {
    private final Map<RecursoCrm, ResourceReadPolicy> policies = new EnumMap<>(RecursoCrm.class);
    private final Set<UUID> deniedRecordIds = new java.util.HashSet<>();
    private final Set<String> deniedActions = new java.util.HashSet<>();
    private String revision = "test-authorization-revision";

    public TestCrmAuthorization() {
        ResourceReadPolicy allowAll = new ResourceReadPolicy(
                AlcanceCrm.TODO_COMPARTIDO,
                Set.of(GrupoCampoSensible.values()),
                Set.of(),
                Set.of(GrupoCampoSensible.values()));
        for (RecursoCrm resource : RecursoCrm.values()) {
            policies.put(resource, allowAll);
        }
    }

    public TestCrmAuthorization policy(RecursoCrm resource, ResourceReadPolicy policy) {
        policies.put(resource, policy);
        return this;
    }

    public TestCrmAuthorization denyRecord(UUID recordId) {
        deniedRecordIds.add(recordId);
        return this;
    }

    public TestCrmAuthorization denyAction(RecursoCrm resource, AccionCrm action) {
        deniedActions.add(resource + ":" + action);
        return this;
    }

    public TestCrmAuthorization revision(String revision) {
        this.revision = revision;
        return this;
    }

    @Override
    public void require(RecursoCrm recurso, AccionCrm accion) {
        if (deniedActions.contains(recurso + ":" + accion)) {
            throw new CrmAuthorizationDeniedException("Denied test action");
        }
    }

    @Override
    public boolean permitsRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
        return !deniedRecordIds.contains(recordId);
    }

    @Override
    public void requireRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
        require(recurso, accion);
        if (!permitsRecord(recurso, accion, recordId)) {
            throw new CrmAuthorizationDeniedException("Denied test record");
        }
    }

    @Override
    public void requireCandidate(RecursoCrm recurso, AccionCrm accion, ResourceScopeCandidate candidate) {
        require(recurso, accion);
    }

    @Override
    public void requireCanDelegateGrants(Collection<PermisoRecurso> requestedGrants) {
    }

    @Override
    public ResourceReadPolicy readPolicy(RecursoCrm recurso) {
        require(recurso, AccionCrm.LEER);
        return policies.get(recurso);
    }

    @Override
    public ResourceReadPolicy fieldPolicy(RecursoCrm recurso) {
        return policies.get(recurso);
    }

    @Override
    public void requireWritableGroups(RecursoCrm recurso, Set<GrupoCampoSensible> groups) {
        if (!policies.get(recurso).writableGroups().containsAll(groups)) {
            throw new CrmAuthorizationDeniedException("Denied test field group");
        }
    }

    @Override
    public String revision() {
        return revision;
    }
}
