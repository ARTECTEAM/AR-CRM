package com.ar.crm2.security;

import com.ar.crm2.adapter.out.persistence.repository.RolRepository;
import com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Resolves the validated JWT subject to its current active local CRM identity. */
@Component
public final class CurrentActorAdapter implements CurrentActorPort {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final String bootstrapAdminSubject;

    public CurrentActorAdapter(UsuarioRepository usuarioRepository,
                               RolRepository rolRepository,
                               @Value("${crm.authorization.bootstrap-admin-subject:}") String bootstrapAdminSubject) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.bootstrapAdminSubject = bootstrapAdminSubject == null ? "" : bootstrapAdminSubject.trim();
    }

    @Override
    public Optional<CurrentActor> currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return Optional.empty();
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }
        var user = usuarioRepository.findByKeycloakId(subject).orElse(null);
        if (user == null || !user.isActivo() || user.getId() == null || user.getId().isBlank()
                || user.getRolId() == null || user.getRolId().isBlank()) {
            return Optional.empty();
        }
        var role = rolRepository.findById(user.getRolId()).orElse(null);
        if (role == null || !role.isActivo() || role.getId() == null || role.getId().isBlank()) {
            return Optional.empty();
        }

        try {
            UUID usuarioId = UUID.fromString(user.getId());
            UUID rolId = UUID.fromString(role.getId());
            boolean bootstrapAdmin = !bootstrapAdminSubject.isBlank()
                    && bootstrapAdminSubject.equals(subject);
            return Optional.of(new CurrentActor(usuarioId, rolId, bootstrapAdmin));
        } catch (IllegalArgumentException invalidLocalIdentity) {
            return Optional.empty();
        }
    }
}