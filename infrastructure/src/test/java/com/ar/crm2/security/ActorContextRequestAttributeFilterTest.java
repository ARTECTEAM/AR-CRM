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
        ActorContextRequestAttributeFilter filter = new ActorContextRequestAttributeFilter(mapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute(ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE))
                .isNull();
        verifyNoInteractions(mapper);
        verify(chain).doFilter(request, response);
    }
}