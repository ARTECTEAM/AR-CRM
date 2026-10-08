package com.ar.crm2.model.agent.tool.vo;

import com.ar.crm2.shared.DomainAssert;

/**
 * Server-owned reference to the canonical resource produced by a
 * completed tool action. Once the owning action is {@code COMPLETED}, both
 * {@code resourceType} and {@code resourceId} are required and immutable:
 * the type names the CRM entity that was affected, and the id carries the
 * persisted CRM identifier.
 */
public record AgentToolResource(String resourceType, String resourceId) {

    public AgentToolResource {
        DomainAssert.notBlank(resourceType, "resourceType");
        DomainAssert.notBlank(resourceId, "resourceId");
        resourceType = resourceType.trim();
        resourceId = resourceId.trim();
    }
}
