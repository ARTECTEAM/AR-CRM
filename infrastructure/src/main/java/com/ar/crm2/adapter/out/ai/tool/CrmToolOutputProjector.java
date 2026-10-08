package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.in.rest.projection.CrmSensitiveFieldProjector;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import lombok.RequiredArgsConstructor;

/** Projects typed CRM tool results using the authenticated actor's current field grants. */
@RequiredArgsConstructor
public final class CrmToolOutputProjector {

    private final CrmAuthorization authorization;

    public <T> T project(T output, RecursoCrm resource) {
        ResourceReadPolicy policy = authorization.fieldPolicy(resource);
        if (policy == null) {
            throw new CrmActorUnavailableException("CRM field policy is unavailable for tool output");
        }
        return CrmSensitiveFieldProjector.project(output, resource, policy);
    }
}