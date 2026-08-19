package com.ar.crm2.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring Security configuration for CRM2 REST API.
 *
 * Security boundary in infrastructure layer — keeps Spring Security types
 * out of application/domain layers per Clean/Hexagonal architecture.
 *
 * Route policy:
 * - Public: /swagger-ui/**, /v3/api-docs/**, /actuator/health, OPTIONS (CORS preflight)
 * - Authenticated: /api/** (JWT Bearer token required)
 * - Deny all else
 *
 * Session: stateless (no server-side sessions; JWT carries identity).
 * CSRF: disabled for REST API (stateless + Bearer token authentication).
 */
@Configuration
@EnableWebSecurity
@Profile("!noauth")
public class SecurityConfig {

    private static final RequestMatcher SWAGGER_UI = PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/swagger-ui/**");
    private static final RequestMatcher OPEN_API_DOCS = PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/v3/api-docs/**");
    private static final RequestMatcher ACTUATOR_HEALTH = PathPatternRequestMatcher.pathPattern("/actuator/health");
    private static final RequestMatcher FORGOT_PASSWORD = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/usuarios/forgot-password");
    private static final RequestMatcher CORS_PREFLIGHT = PathPatternRequestMatcher.pathPattern(HttpMethod.OPTIONS, "/**");
    private static final RequestMatcher SUPERUSUARIO_CREATE = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/superusuarios/create");
    private static final RequestMatcher API_ENDPOINTS = PathPatternRequestMatcher.pathPattern("/api/**");

    @Bean
    @Order(0)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            ActorContextRequestAttributeFilter actorContextRequestAttributeFilter
    ) throws Exception {
        http
            // Route authorization
            .authorizeHttpRequests(authorize -> authorize
                // Public OpenAPI / actuator health
                .requestMatchers(SWAGGER_UI).permitAll()
                .requestMatchers(OPEN_API_DOCS).permitAll()
                .requestMatchers(ACTUATOR_HEALTH).permitAll()
                // Forgot password - public endpoint, no email enumeration
                .requestMatchers(FORGOT_PASSWORD).permitAll()
                // Preflight CORS
                .requestMatchers(CORS_PREFLIGHT).permitAll()
                // SuperUsuario bootstrap: requires authenticated + SUPER_USUARIO technical role.
                // This is a technical guard only — CRM2 business authorization is separate.
                .requestMatchers(SUPERUSUARIO_CREATE)
                    .hasRole("SUPER_USUARIO")
                // All other API endpoints require authentication (no role restriction here)
                .requestMatchers(API_ENDPOINTS).authenticated()
                // Deny everything else
                .anyRequest().denyAll()
            )
            // Stateless session — no HttpSession, no server-side session store
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Disable CSRF — not needed for stateless Bearer-token APIs
            .csrf(csrf -> csrf.disable())
// CORS is handled by CorsConfig WebMvcConfigurer
            .cors(cors -> {})
            // JWT Resource Server — validates Bearer tokens via configured issuer-uri
            // KeycloakJwtAuthoritiesConverter maps Keycloak roles (realm + resource_access)
            // to Spring ROLE_* authorities so hasRole() works against Keycloak identity.
            .oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt ->
                    jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter())
                )
            )
            .addFilterAfter(actorContextRequestAttributeFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    JwtAuthenticationConverter keycloakJwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtAuthoritiesConverter());
        return converter;
    }
}
