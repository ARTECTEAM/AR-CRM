package com.ar.crm2.adapter.in.rest.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SuperUsuarioResponseTest {

    @Test
    void serializationDoesNotExposeKeycloakId() throws Exception {
        SuperUsuarioResponse response = new SuperUsuarioResponse(
                "super-usuario-id",
                "admin@example.com",
                null,
                true,
                "internal-keycloak-id"
        );

        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(response));

        assertFalse(json.has("keycloakId"));
        assertFalse(json.toString().contains("internal-keycloak-id"));
        assertEquals("super-usuario-id", json.path("id").asText());
    }
}
