package com.ar.crm2.application.security;

import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.ResourceScopePort;
import com.ar.crm2.application.security.service.DefaultCrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.vo.RolId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrmAuthorizationCapabilitiesTest {

    @Test
    void bootstrapAdministratorStillGetsOrdinaryRoleGrantsOutsideTheBootstrapBypass() {
        UUID userId = UUID.randomUUID();
        RolId roleId = RolId.create();
        Rol role = Rol.reconstitute(roleId, "Configured", null, true, List.of(
                new PermisoRecurso(RecursoCrm.CONTACTO, Set.of(AccionCrm.LEER),
                        AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of())
        ));
        CrmAuthorization authorization = new DefaultCrmAuthorization(
                () -> Optional.of(new CurrentActor(userId, roleId.value(), true)),
                id -> Optional.of(role),
                List.of());

        AuthorizationCapabilities capabilities = authorization.authorizationCapabilities();

        assertTrue(capabilities.permits(RecursoCrm.CONTACTO, AccionCrm.LEER));
        assertTrue(capabilities.permits(RecursoCrm.ROL, AccionCrm.CREAR));
        assertTrue(capabilities.permits(RecursoCrm.USUARIO, AccionCrm.ELIMINAR));
    }

    @Test
    void snapshotsOnlyEffectiveActionsAndCarriesGeneralScopeAndFieldGroupsWithoutRecordIds() {
        UUID userId = UUID.randomUUID();
        RolId roleId = RolId.create();
        UUID allowedBoardId = UUID.randomUUID();
        AtomicInteger recordChecks = new AtomicInteger();
        ResourceScopePort boardScope = new ResourceScopePort() {
            @Override
            public boolean supports(RecursoCrm resource) {
                return resource == RecursoCrm.TABLERO;
            }

            @Override
            public boolean permits(UUID actorId, RecursoCrm resource, UUID recordId,
                                   AlcanceCrm scope, Set<UUID> allowedIds) {
                recordChecks.incrementAndGet();
                return allowedIds.contains(recordId);
            }
        };
        Rol role = Rol.reconstitute(roleId, "Configured", null, true, List.of(
                new PermisoRecurso(RecursoCrm.CONTACTO,
                        Set.of(AccionCrm.LEER, AccionCrm.CREAR, AccionCrm.ACTUALIZAR),
                        AlcanceCrm.TODO_COMPARTIDO, Set.of(),
                        Set.of(GrupoCampoSensible.CONTACTO_PRIVADO),
                        Set.of(GrupoCampoSensible.CONTACTO_PRIVADO)),
                new PermisoRecurso(RecursoCrm.TABLERO,
                        Set.of(AccionCrm.LEER, AccionCrm.ACTUALIZAR),
                        AlcanceCrm.TABLEROS_PERMITIDOS, Set.of(allowedBoardId),
                        Set.of(GrupoCampoSensible.FINANCIERO), Set.of())
        ));
        CurrentActorPort actor = () -> Optional.of(new CurrentActor(userId, roleId.value(), false));
        FindRolByIdPort roles = id -> Optional.of(role);
        CrmAuthorization authorization = new DefaultCrmAuthorization(actor, roles, List.of(boardScope));

        AuthorizationCapabilities capabilities = authorization.authorizationCapabilities();

        assertTrue(capabilities.permits(RecursoCrm.CONTACTO, AccionCrm.LEER));
        assertTrue(capabilities.permits(RecursoCrm.CONTACTO, AccionCrm.CREAR));
        assertFalse(capabilities.permits(RecursoCrm.CONTACTO, AccionCrm.ELIMINAR));
        assertFalse(capabilities.permits(RecursoCrm.ETIQUETA, AccionCrm.LEER));
        ResourceCapabilities board = capabilities.forResource(RecursoCrm.TABLERO).orElseThrow();
        assertEquals(AlcanceCrm.TABLEROS_PERMITIDOS, board.scope());
        assertEquals(Set.of(GrupoCampoSensible.FINANCIERO), board.readableGroups());
        assertEquals(Set.of(), board.writableGroups());
        assertFalse(board.toString().contains(allowedBoardId.toString()),
                "A model-facing capability snapshot must not disclose allowlisted record IDs");
        assertEquals(0, recordChecks.get(), "Snapshot creation must not probe individual records");
    }
}
