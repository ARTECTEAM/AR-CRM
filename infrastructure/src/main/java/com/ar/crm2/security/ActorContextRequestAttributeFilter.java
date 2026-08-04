package com.ar.crm2.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates the authenticated {@link ActorContext} into the current request as a
 * request attribute ({@code actorContext}), making it available to downstream
 * controllers without leaking Spring Security types into the application layer.
 *
 * <p>Runs after the JWT validation filter in the Spring Security chain so
 * {@link SecurityContextHolder} already holds the validated authentication.
 *
 * <p>Fail-closed: the attribute is only set when the principal is a validated
 * {@link Jwt}. No development fallback — non-authenticated requests must reach
 * the controller with no actor and trigger
 * {@link com.ar.crm2.application.security.exception.AuthenticatedUsuarioRequiredException}
 * via the controller mapper. The {@code noauth} profile uses a separate
 * {@link SecurityNoAuthConfig} chain that intentionally bypasses this filter.
 */
@Component
public class ActorContextRequestAttributeFilter extends OncePerRequestFilter {

    public static final String ACTOR_CONTEXT_ATTRIBUTE = "actorContext";

    private final KeycloakJwtActorContextMapper actorContextMapper;

    public ActorContextRequestAttributeFilter(KeycloakJwtActorContextMapper actorContextMapper) {
        this.actorContextMapper = actorContextMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof Jwt) {
            request.setAttribute(ACTOR_CONTEXT_ATTRIBUTE, actorContextMapper.map(authentication));
        }
        // Intentionally no fallback: missing/invalid principal leaves the attribute
        // unset. Controllers MUST reject the request via requireAuthenticatedActor.
        filterChain.doFilter(request, response);
    }
}