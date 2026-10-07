package com.ar.crm2.application.security;

import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.ResourceScopePort;
import com.ar.crm2.application.security.service.DefaultCrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.vo.RolId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultCrmAuthorizationTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final RolId ROLE_ID = RolId.create();

    @Test
    void missingOrInactiveRoleFailsClosedInsteadOfUsingLegacyAllAccess() {
        CurrentActorPort actor = () -> Optional.of(new CurrentActor(USER_ID, ROLE_ID.value(), false));
        FindRolByIdPort missingRole = id -> Optional.empty();
        CrmAuthorization missing = new DefaultCrmAuthorization(actor, missingRole, List.of());
        assertThrows(CrmActorUnavailableException.class,
                () -> missing.require(RecursoCrm.TRATO, AccionCrm.LEER));

        Rol inactive = Rol.reconstitute(ROLE_ID, "Inactive", null, false, List.of());
        CrmAuthorization inactiveAuth = new DefaultCrmAuthorization(actor, id -> Optional.of(inactive), List.of());
        assertThrows(CrmActorUnavailableException.class,
                () -> inactiveAuth.require(RecursoCrm.TRATO, AccionCrm.LEER));

        Rol empty = role(List.of());
        CrmAuthorization noGrants = new DefaultCrmAuthorization(actor, id -> Optional.of(empty), List.of());
        assertThrows(CrmAuthorizationDeniedException.class,
                () -> noGrants.require(RecursoCrm.TRATO, AccionCrm.LEER));
    }

    @Test
    void scopedRowsFailClosedWithoutProviderAndUseExplicitAllowedBoardsWhenPresent() {
        UUID allowedBoard = UUID.randomUUID();
        UUID deniedBoard = UUID.randomUUID();
        PermisoRecurso grant = new PermisoRecurso(RecursoCrm.TABLERO, Set.of(AccionCrm.LEER),
                AlcanceCrm.TABLEROS_PERMITIDOS, Set.of(allowedBoard), Set.of(), Set.of());
        Rol role = role(List.of(grant));
        CurrentActorPort actor = () -> Optional.of(new CurrentActor(USER_ID, ROLE_ID.value(), false));
        FindRolByIdPort repository = id -> Optional.of(role);

        CrmAuthorization missingProvider = new DefaultCrmAuthorization(actor, repository, List.of());
        assertFalse(missingProvider.permitsRecord(RecursoCrm.TABLERO, AccionCrm.LEER, allowedBoard));

        ResourceScopePort boardScope = new ResourceScopePort() {
            @Override public boolean supports(RecursoCrm resource) { return resource == RecursoCrm.TABLERO; }
            @Override public boolean permits(UUID userId, RecursoCrm resource, UUID recordId,
                                             AlcanceCrm scope, Set<UUID> allowedIds) {
                return scope == AlcanceCrm.TABLEROS_PERMITIDOS && allowedIds.contains(recordId);
            }
        };
        CrmAuthorization scoped = new DefaultCrmAuthorization(actor, repository, List.of(boardScope));
        assertTrue(scoped.permitsRecord(RecursoCrm.TABLERO, AccionCrm.LEER, allowedBoard));
        assertFalse(scoped.permitsRecord(RecursoCrm.TABLERO, AccionCrm.LEER, deniedBoard));
    }

    @Test
    void recordScopeProviderReceivesRequestedActionForActionAwarePolicies() {
        UUID columnId = UUID.randomUUID();
        PermisoRecurso grant = new PermisoRecurso(RecursoCrm.COLUMNA,
                Set.of(AccionCrm.LEER, AccionCrm.ACTUALIZAR), AlcanceCrm.TABLEROS_PERMITIDOS,
                Set.of(UUID.randomUUID()), Set.of(), Set.of());
        Rol role = role(List.of(grant));
        CurrentActorPort actor = () -> Optional.of(new CurrentActor(USER_ID, ROLE_ID.value(), false));
        FindRolByIdPort repository = id -> Optional.of(role);
        AtomicReference<AccionCrm> checkedAction = new AtomicReference<>();
        ResourceScopePort actionScope = new ResourceScopePort() {
            @Override public boolean supports(RecursoCrm resource) { return resource == RecursoCrm.COLUMNA; }
            @Override public boolean permits(UUID userId, RecursoCrm resource, UUID recordId,
                                             AlcanceCrm scope, Set<UUID> allowedIds) {
                return false;
            }
            @Override public boolean permits(UUID userId, RecursoCrm resource, AccionCrm action, UUID recordId,
                                             AlcanceCrm scope, Set<UUID> allowedIds) {
                checkedAction.set(action);
                return true;
            }
        };
        CrmAuthorization authorization = new DefaultCrmAuthorization(actor, repository, List.of(actionScope));

        assertTrue(authorization.permitsRecord(RecursoCrm.COLUMNA, AccionCrm.ACTUALIZAR, columnId));
        org.junit.jupiter.api.Assertions.assertEquals(AccionCrm.ACTUALIZAR, checkedAction.get());
    }

    @Test
    void candidateAuthorizationRequiresActionGrantAndOneExplicitScopeProvider() {
        UUID assigneeId = UUID.randomUUID();
        ResourceScopeCandidate candidate = new ResourceScopeCandidate(null, null, assigneeId, null);
        PermisoRecurso grant = new PermisoRecurso(RecursoCrm.TAREA,
                Set.of(AccionCrm.LEER, AccionCrm.ACTUALIZAR), AlcanceCrm.PROPIOS_O_ASIGNADOS,
                Set.of(), Set.of(), Set.of());
        Rol role = role(List.of(grant));
        CurrentActorPort actor = () -> Optional.of(new CurrentActor(USER_ID, ROLE_ID.value(), false));
        FindRolByIdPort repository = id -> Optional.of(role);

        CrmAuthorization missingProvider = new DefaultCrmAuthorization(actor, repository, List.of());
        assertThrows(CrmAuthorizationDeniedException.class, () -> missingProvider.requireCandidate(
                RecursoCrm.TAREA, AccionCrm.ACTUALIZAR, candidate));

        ResourceScopePort taskScope = new ResourceScopePort() {
            @Override public boolean supports(RecursoCrm resource) { return resource == RecursoCrm.TAREA; }
            @Override public boolean permits(UUID userId, RecursoCrm resource, UUID recordId,
                                             AlcanceCrm scope, Set<UUID> allowedIds) { return false; }
            @Override public boolean permitsCandidate(UUID userId, RecursoCrm resource,
                                                      ResourceScopeCandidate candidate,
                                                      AlcanceCrm scope, Set<UUID> allowedIds) {
                return userId.equals(USER_ID) && candidate.responsibleUserId().equals(assigneeId)
                        && scope == AlcanceCrm.PROPIOS_O_ASIGNADOS;
            }
        };
        CrmAuthorization scoped = new DefaultCrmAuthorization(actor, repository, List.of(taskScope));
        scoped.requireCandidate(RecursoCrm.TAREA, AccionCrm.ACTUALIZAR, candidate);
        assertThrows(CrmAuthorizationDeniedException.class, () -> scoped.requireCandidate(
                RecursoCrm.TAREA, AccionCrm.ACTUALIZAR,
                new ResourceScopeCandidate(null, null, UUID.randomUUID(), null)));
        assertThrows(CrmAuthorizationDeniedException.class, () -> scoped.requireCandidate(
                RecursoCrm.TAREA, AccionCrm.ELIMINAR, candidate));

        ResourceScopePort duplicateScope = new ResourceScopePort() {
            @Override public boolean supports(RecursoCrm resource) { return resource == RecursoCrm.TAREA; }
            @Override public boolean permits(UUID userId, RecursoCrm resource, UUID recordId,
                                             AlcanceCrm scope, Set<UUID> allowedIds) { return true; }
        };
        CrmAuthorization ambiguous = new DefaultCrmAuthorization(actor, repository,
                List.of(taskScope, duplicateScope));
        assertThrows(CrmAuthorizationDeniedException.class, () -> ambiguous.requireCandidate(
                RecursoCrm.TAREA, AccionCrm.ACTUALIZAR, candidate));
    }

    @Test
    void delegationIsBoundByActionsFieldsScopeAndAllowedBoardIds() {
        UUID allowedBoard = UUID.randomUUID();
        UUID anotherBoard = UUID.randomUUID();
        Rol role = role(List.of(
                new PermisoRecurso(RecursoCrm.TRATO, Set.of(AccionCrm.LEER),
                        AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of(),
                        Set.of(com.ar.crm2.model.autorizacion.GrupoCampoSensible.FINANCIERO), Set.of()),
                new PermisoRecurso(RecursoCrm.TABLERO, Set.of(AccionCrm.LEER),
                        AlcanceCrm.TABLEROS_PERMITIDOS, Set.of(allowedBoard), Set.of(), Set.of())
        ));
        CrmAuthorization authorization = new DefaultCrmAuthorization(
                () -> Optional.of(new CurrentActor(USER_ID, ROLE_ID.value(), false)),
                id -> Optional.of(role), List.of());

        authorization.requireCanDelegateGrants(List.of(new PermisoRecurso(RecursoCrm.TRATO,
                Set.of(AccionCrm.LEER), AlcanceCrm.PROPIOS_O_ASIGNADOS, Set.of(),
                Set.of(com.ar.crm2.model.autorizacion.GrupoCampoSensible.FINANCIERO), Set.of())));
        authorization.requireCanDelegateGrants(List.of(new PermisoRecurso(RecursoCrm.TABLERO,
                Set.of(AccionCrm.LEER), AlcanceCrm.TABLEROS_PERMITIDOS, Set.of(allowedBoard), Set.of(), Set.of())));

        PermisoRecurso broaderGrant = new PermisoRecurso(
                RecursoCrm.TRATO, Set.of(AccionCrm.ADMINISTRAR), AlcanceCrm.TODO_COMPARTIDO,
                Set.of(), Set.of(com.ar.crm2.model.autorizacion.GrupoCampoSensible.FINANCIERO),
                Set.of(com.ar.crm2.model.autorizacion.GrupoCampoSensible.FINANCIERO));
        assertThrows(CrmAuthorizationDeniedException.class,
                () -> authorization.requireCanDelegateGrants(List.of(broaderGrant)));
        PermisoRecurso boardOutsideCeiling = new PermisoRecurso(
                RecursoCrm.TABLERO, Set.of(AccionCrm.LEER), AlcanceCrm.TABLEROS_PERMITIDOS,
                Set.of(anotherBoard), Set.of(), Set.of());
        assertThrows(CrmAuthorizationDeniedException.class,
                () -> authorization.requireCanDelegateGrants(List.of(boardOutsideCeiling)));
    }

    @Test
    void configuredBootstrapAdministratorCanGrantInitialRolePermissions() {
        CurrentActorPort bootstrapActor = () -> Optional.of(
                new CurrentActor(USER_ID, ROLE_ID.value(), true));
        CrmAuthorization authorization = new DefaultCrmAuthorization(
                bootstrapActor, id -> Optional.empty(), List.of());
        PermisoRecurso administrator = new PermisoRecurso(RecursoCrm.TRATO,
                Set.of(AccionCrm.ADMINISTRAR), AlcanceCrm.TODO_COMPARTIDO, Set.of(),
                Set.of(com.ar.crm2.model.autorizacion.GrupoCampoSensible.values()),
                Set.of(com.ar.crm2.model.autorizacion.GrupoCampoSensible.values()));

        authorization.requireCanDelegateGrants(List.of(administrator));
    }

    @Test
    void permissionRevisionChangesWhenStoredGrantsChange() {
        AtomicReference<Rol> role = new AtomicReference<>(role(List.of(
                new PermisoRecurso(RecursoCrm.TRATO, Set.of(AccionCrm.LEER), AlcanceCrm.TODO_COMPARTIDO,
                        Set.of(), Set.of(), Set.of()))));
        CrmAuthorization authorization = new DefaultCrmAuthorization(
                () -> Optional.of(new CurrentActor(USER_ID, ROLE_ID.value(), false)),
                id -> Optional.of(role.get()), List.of());

        String before = authorization.revision();
        role.set(role(List.of(new PermisoRecurso(RecursoCrm.TRATO,
                Set.of(AccionCrm.LEER, AccionCrm.ELIMINAR), AlcanceCrm.TODO_COMPARTIDO,
                Set.of(), Set.of(), Set.of()))));

        assertNotEquals(before, authorization.revision());
    }

    private static Rol role(List<PermisoRecurso> grants) {
        return Rol.reconstitute(ROLE_ID, "Configured", null, true, grants);
    }
}
