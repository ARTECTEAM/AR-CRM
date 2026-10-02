package com.ar.crm2.application.security.service;

import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.AuthorizationCapabilities;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.ResourceCapabilities;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.ResourceScopePort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.vo.RolId;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Policy evaluator backed by the current local role and resource-owned row-scope providers. */
public final class DefaultCrmAuthorization implements CrmAuthorization {
    private final CurrentActorPort currentActorPort;
    private final FindRolByIdPort findRolByIdPort;
    private final List<ResourceScopePort> scopePorts;

    public DefaultCrmAuthorization(CurrentActorPort currentActorPort,
                                   FindRolByIdPort findRolByIdPort,
                                   List<ResourceScopePort> scopePorts) {
        this.currentActorPort = currentActorPort;
        this.findRolByIdPort = findRolByIdPort;
        this.scopePorts = scopePorts == null ? List.of() : List.copyOf(scopePorts);
    }

    @Override
    public AuthorizationCapabilities authorizationCapabilities() {
        CurrentActor actor = requireCurrentActor();
        Rol role = findRole(actor)
                .orElseThrow(() -> new CrmActorUnavailableException("CRM role is unavailable"));
        if (!role.isActivo()) {
            throw new CrmActorUnavailableException("CRM role is inactive");
        }

        EnumMap<RecursoCrm, ResourceCapabilities> capabilities = new EnumMap<>(RecursoCrm.class);
        for (RecursoCrm resource : RecursoCrm.values()) {
            if (isBootstrapAdministrator(actor, resource)) {
                EnumSet<AccionCrm> actions = EnumSet.allOf(AccionCrm.class);
                actions.remove(AccionCrm.ADMINISTRAR);
                capabilities.put(resource, new ResourceCapabilities(
                        AlcanceCrm.TODO_COMPARTIDO, actions,
                        Set.of(GrupoCampoSensible.values()), Set.of(GrupoCampoSensible.values())));
                continue;
            }

            PermisoRecurso grant = grantFor(role, resource);
            if (grant == null || !scopeCanBeEvaluated(resource, grant.alcance())) {
                continue;
            }
            EnumSet<AccionCrm> actions = EnumSet.noneOf(AccionCrm.class);
            for (AccionCrm action : AccionCrm.values()) {
                if (action != AccionCrm.ADMINISTRAR && grant.permite(action)) {
                    actions.add(action);
                }
            }
            if (actions.isEmpty()) {
                continue;
            }
            Set<GrupoCampoSensible> readableGroups = actions.contains(AccionCrm.LEER)
                    ? grant.gruposLectura() : Set.of();
            Set<GrupoCampoSensible> writableGroups = actions.contains(AccionCrm.CREAR)
                    || actions.contains(AccionCrm.ACTUALIZAR) ? grant.gruposEscritura() : Set.of();
            capabilities.put(resource, new ResourceCapabilities(
                    grant.alcance(), actions, readableGroups, writableGroups));
        }
        return new AuthorizationCapabilities(capabilities);
    }

    @Override
    public void require(RecursoCrm resource, AccionCrm action) {
        CurrentActor actor = requireCurrentActor();
        if (isBootstrapAdministrator(actor, resource)) {
            return;
        }
        PermisoRecurso grant = currentGrant(actor, resource);
        if (grant == null || !grant.permite(action)) {
            throw denied(resource, action);
        }
        if (!scopeCanBeEvaluated(resource, grant.alcance())) {
            throw new CrmAuthorizationDeniedException(
                    "No unique scope evaluator is available for " + resource + " under " + grant.alcance());
        }
    }

    @Override
    public boolean permitsRecord(RecursoCrm resource, AccionCrm action, UUID recordId) {
        if (resource == null || action == null || recordId == null) {
            return false;
        }
        CurrentActor actor = currentActorPort.currentActor().orElse(null);
        if (actor == null) {
            return false;
        }
        if (isBootstrapAdministrator(actor, resource)) {
            return true;
        }
        Rol role = findRole(actor).orElse(null);
        if (role == null || !role.isActivo()) {
            return false;
        }
        PermisoRecurso grant = grantFor(role, resource);
        if (grant == null || !grant.permite(action)) {
            return false;
        }
        if (grant.alcance() == AlcanceCrm.TODO_COMPARTIDO) {
            return true;
        }
        List<ResourceScopePort> providers = supportingScopePorts(resource);
        if (!scopeCanBeEvaluated(resource, grant.alcance())) {
            return false;
        }
        return providers.getFirst().permits(actor.usuarioId(), resource, action, recordId,
                grant.alcance(), grant.idsPermitidos());
    }

    @Override
    public void requireRecord(RecursoCrm resource, AccionCrm action, UUID recordId) {
        require(resource, action);
        if (!permitsRecord(resource, action, recordId)) {
            throw new CrmAuthorizationDeniedException(
                    "Access denied to " + resource + " record " + recordId);
        }
    }

    @Override
    public void requireCandidate(RecursoCrm resource, AccionCrm action, ResourceScopeCandidate candidate) {
        if (candidate == null) {
            throw new CrmAuthorizationDeniedException("Candidate is required for " + resource);
        }
        CurrentActor actor = requireCurrentActor();
        if (isBootstrapAdministrator(actor, resource)) {
            return;
        }
        PermisoRecurso grant = currentGrant(actor, resource);
        if (grant == null || !grant.permite(action)) {
            throw denied(resource, action);
        }
        if (grant.alcance() == AlcanceCrm.TODO_COMPARTIDO) {
            return;
        }
        List<ResourceScopePort> providers = supportingScopePorts(resource);
        if (providers.size() != 1 || !providers.getFirst().permitsCandidate(
                actor.usuarioId(), resource, candidate, grant.alcance(), grant.idsPermitidos())) {
            throw new CrmAuthorizationDeniedException(
                    "Access denied to candidate " + resource + " under " + grant.alcance());
        }
    }

    @Override
    public void requireCanDelegateGrants(Collection<PermisoRecurso> requestedGrants) {
        if (requestedGrants == null) {
            throw new CrmAuthorizationDeniedException("Requested role grants are required");
        }
        CurrentActor actor = requireCurrentActor();
        if (actor.bootstrapAdmin()) {
            return;
        }
        Rol currentRole = findRole(actor)
                .orElseThrow(() -> new CrmActorUnavailableException("CRM role is unavailable"));
        if (!currentRole.isActivo()) {
            throw new CrmActorUnavailableException("CRM role is inactive");
        }
        for (PermisoRecurso requested : requestedGrants) {
            if (requested == null) {
                throw new CrmAuthorizationDeniedException("Requested role grants cannot contain null");
            }
            PermisoRecurso authority = grantFor(currentRole, requested.recurso());
            if (authority == null || !isWithinDelegationCeiling(authority, requested)) {
                throw new CrmAuthorizationDeniedException(
                        "Cannot delegate permissions beyond the current grant for " + requested.recurso());
            }
        }
    }

    @Override
    public ResourceReadPolicy readPolicy(RecursoCrm resource) {
        require(resource, AccionCrm.LEER);
        return fieldPolicy(resource);
    }

    @Override
    public ResourceReadPolicy fieldPolicy(RecursoCrm resource) {
        CurrentActor actor = requireCurrentActor();
        if (isBootstrapAdministrator(actor, resource)) {
            return new ResourceReadPolicy(AlcanceCrm.TODO_COMPARTIDO,
                    Set.of(GrupoCampoSensible.values()), Set.of(), Set.of(GrupoCampoSensible.values()));
        }
        PermisoRecurso grant = currentGrant(actor, resource);
        if (grant == null) {
            return new ResourceReadPolicy(AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of());
        }
        return new ResourceReadPolicy(grant.alcance(), grant.gruposLectura(), grant.idsPermitidos(),
                grant.gruposEscritura());
    }

    @Override
    public void requireWritableGroups(RecursoCrm resource, Set<GrupoCampoSensible> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        CurrentActor actor = requireCurrentActor();
        if (isBootstrapAdministrator(actor, resource)) {
            return;
        }
        PermisoRecurso grant = currentGrant(actor, resource);
        if (grant == null || !grant.gruposLectura().containsAll(groups)
                || !grant.gruposEscritura().containsAll(groups)) {
            throw new CrmAuthorizationDeniedException(
                    "Write denied for sensitive field group on " + resource);
        }
    }

    @Override
    public String revision() {
        CurrentActor actor = requireCurrentActor();
        Rol role = findRole(actor).orElseThrow(() -> new CrmActorUnavailableException("CRM role is unavailable"));
        if (!role.isActivo()) {
            throw new CrmActorUnavailableException("CRM role is inactive");
        }
        String grantState = role.getPermisos().stream()
                .sorted(Comparator.comparing(g -> g.recurso().name()))
                .map(DefaultCrmAuthorization::canonicalGrant)
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
        String material = actor.usuarioId() + "|" + actor.rolId() + "|" + actor.bootstrapAdmin()
                + "|" + role.isActivo() + "|" + grantState;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", impossible);
        }
    }

    private static String canonicalGrant(PermisoRecurso grant) {
        return grant.recurso().name() + ":" + sortedNames(grant.acciones())
                + ":" + grant.alcance().name() + ":" + grant.idsPermitidos().stream().map(UUID::toString).sorted().toList()
                + ":" + sortedNames(grant.gruposLectura()) + ":" + sortedNames(grant.gruposEscritura());
    }

    private List<ResourceScopePort> supportingScopePorts(RecursoCrm resource) {
        return scopePorts.stream().filter(port -> port.supports(resource)).toList();
    }

    private boolean scopeCanBeEvaluated(RecursoCrm resource, AlcanceCrm scope) {
        return scope == AlcanceCrm.TODO_COMPARTIDO
                || (resource.supportsScope(scope) && supportingScopePorts(resource).size() == 1);
    }

    private static boolean isWithinDelegationCeiling(PermisoRecurso authority, PermisoRecurso requested) {
        if (!authority.recurso().equals(requested.recurso())
                || !requested.acciones().stream().allMatch(authority::permite)
                || !authority.gruposLectura().containsAll(requested.gruposLectura())
                || !authority.gruposEscritura().containsAll(requested.gruposEscritura())) {
            return false;
        }
        return switch (authority.alcance()) {
            case TODO_COMPARTIDO -> true;
            case PROPIOS_O_ASIGNADOS -> requested.alcance() == AlcanceCrm.PROPIOS_O_ASIGNADOS;
            case TABLEROS_PERMITIDOS -> requested.alcance() == AlcanceCrm.TABLEROS_PERMITIDOS
                    && authority.idsPermitidos().containsAll(requested.idsPermitidos());
        };
    }

    private static <E extends Enum<E>> List<String> sortedNames(Set<E> values) {
        return values.stream().map(Enum::name).sorted().toList();
    }

    private CurrentActor requireCurrentActor() {
        return currentActorPort.currentActor()
                .orElseThrow(() -> new CrmActorUnavailableException("No active CRM user is linked to this request"));
    }

    private PermisoRecurso currentGrant(CurrentActor actor, RecursoCrm resource) {
        Rol role = findRole(actor).orElseThrow(() -> new CrmActorUnavailableException("CRM role is unavailable"));
        if (!role.isActivo()) {
            throw new CrmActorUnavailableException("CRM role is inactive");
        }
        return grantFor(role, resource);
    }

    private java.util.Optional<Rol> findRole(CurrentActor actor) {
        return findRolByIdPort.findById(RolId.from(actor.rolId()));
    }

    private static PermisoRecurso grantFor(Rol role, RecursoCrm resource) {
        return role.getPermisos().stream().filter(grant -> grant.recurso() == resource).findFirst().orElse(null);
    }

    private static boolean isBootstrapAdministrator(CurrentActor actor, RecursoCrm resource) {
        return actor.bootstrapAdmin() && (resource == RecursoCrm.ROL || resource == RecursoCrm.USUARIO);
    }

    private static CrmAuthorizationDeniedException denied(RecursoCrm resource, AccionCrm action) {
        return new CrmAuthorizationDeniedException("Action " + action + " denied for " + resource);
    }
}
