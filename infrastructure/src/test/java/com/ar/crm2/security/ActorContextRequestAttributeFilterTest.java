package com.ar.crm2.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ActorContextRequestAttributeFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedRequestDoesNotReceiveDevelopmentActorFallback() throws Exception {
        KeycloakJwtActorContextMapper mapper = mock(KeycloakJwtActorContextMapper.class);
        com.ar.crm2.application.security.port.out.CurrentActorPort currentActorPort = mock(com.ar.crm2.application.security.port.out.CurrentActorPort.class);
        ActorContextRequestAttributeFilter filter = new ActorContextRequestAttributeFilter(mapper, currentActorPort);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE))
                .isNull();
        verifyNoInteractions(mapper, currentActorPort);
        verify(chain).doFilter(request, response);
    }

    @Test
    void authenticatedRequestReplacesUntrustedClaimWithCurrentLocalActor() throws Exception {
        java.util.UUID localId = java.util.UUID.randomUUID();
        java.util.UUID forgedClaimId = java.util.UUID.randomUUID();
        KeycloakJwtActorContextMapper mapper = mock(KeycloakJwtActorContextMapper.class);
        com.ar.crm2.application.security.port.out.CurrentActorPort currentActorPort =
                mock(com.ar.crm2.application.security.port.out.CurrentActorPort.class);
        ActorContextRequestAttributeFilter filter = new ActorContextRequestAttributeFilter(mapper, currentActorPort);
        com.ar.crm2.application.security.ActorContext mappedFromJwt = new com.ar.crm2.application.security.ActorContext(
                "subject", "user", null, java.util.Optional.of(forgedClaimId), java.util.Optional.empty(), java.util.Set.of());
        org.mockito.Mockito.when(mapper.map(org.mockito.ArgumentMatchers.any(org.springframework.security.core.Authentication.class)))
                .thenReturn(mappedFromJwt);
        org.mockito.Mockito.when(currentActorPort.currentActor()).thenReturn(java.util.Optional.of(
                new com.ar.crm2.application.security.CurrentActor(localId, java.util.UUID.randomUUID(), false)));
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("token").header("alg", "none").subject("subject")
                .claim("usuario_id", forgedClaimId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                        jwt, java.util.List.of(), "subject"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        com.ar.crm2.application.security.ActorContext resolved = (com.ar.crm2.application.security.ActorContext)
                request.getAttribute(ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE);
        assertThat(resolved.usuarioId()).contains(localId);
        org.mockito.Mockito.verify(currentActorPort).currentActor();
    }

    @Test
    void missingLocalActorClearsClaimIdentityInsteadOfFallingBack() throws Exception {
        java.util.UUID forgedClaimId = java.util.UUID.randomUUID();
        KeycloakJwtActorContextMapper mapper = mock(KeycloakJwtActorContextMapper.class);
        com.ar.crm2.application.security.port.out.CurrentActorPort currentActorPort =
                mock(com.ar.crm2.application.security.port.out.CurrentActorPort.class);
        ActorContextRequestAttributeFilter filter = new ActorContextRequestAttributeFilter(mapper, currentActorPort);
        org.mockito.Mockito.when(mapper.map(org.mockito.ArgumentMatchers.any(org.springframework.security.core.Authentication.class)))
                .thenReturn(new com.ar.crm2.application.security.ActorContext("subject", "user", null,
                        java.util.Optional.of(forgedClaimId), java.util.Optional.empty(), java.util.Set.of()));
        org.mockito.Mockito.when(currentActorPort.currentActor()).thenReturn(java.util.Optional.empty());
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt
                .withTokenValue("token").header("alg", "none").subject("subject").build();
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                        jwt, java.util.List.of(), "subject"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        com.ar.crm2.application.security.ActorContext resolved = (com.ar.crm2.application.security.ActorContext)
                request.getAttribute(ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE);
        assertThat(resolved.usuarioId()).isEmpty();
    }}