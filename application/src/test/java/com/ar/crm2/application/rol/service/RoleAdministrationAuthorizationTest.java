package com.ar.crm2.application.rol.service;

import com.ar.crm2.application.rol.command.CreateRolCommand;
import com.ar.crm2.application.rol.command.DeleteRolCommand;
import com.ar.crm2.application.rol.command.EditRolCommand;
import com.ar.crm2.application.rol.command.GetRolByIdCommand;
import com.ar.crm2.application.rol.port.out.DeleteRolByIdPort;
import com.ar.crm2.application.rol.port.out.ExistsUsuariosByRolIdPort;
import com.ar.crm2.application.rol.port.out.FindAllRolesPort;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.rol.port.out.SaveRolPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoleAdministrationAuthorizationTest {
    private final CrmAuthorization authorization = mock(CrmAuthorization.class);
    private final AuthorizationMutationPort mutationPort = new AuthorizationMutationPort() {
        @Override
        public <T> T execute(Supplier<T> mutation) {
            return mutation.get();
        }
    };

    @Test
    void roleListDenialStopsBeforeReading() {
        FindAllRolesPort findAll = mock(FindAllRolesPort.class);
        deny(RecursoCrm.ROL, AccionCrm.LEER);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new GetAllRolesService(findAll, authorization).getAll());

        verifyNoInteractions(findAll);
    }

    @Test
    void singleRoleReadDenialStopsBeforeLookup() {
        FindRolByIdPort find = mock(FindRolByIdPort.class);
        deny(RecursoCrm.ROL, AccionCrm.LEER);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new GetRolByIdService(find, authorization).getById(new GetRolByIdCommand(UUID.randomUUID())));

        verifyNoInteractions(find);
    }

    @Test
    void createDenialStopsBeforeGrantDelegationAndPersistence() {
        SaveRolPort save = mock(SaveRolPort.class);
        deny(RecursoCrm.ROL, AccionCrm.ADMINISTRAR);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new CreateRolService(save, authorization, mutationPort).create(new CreateRolCommand("Operator", "")));

        verify(save, never()).save(any());
        verify(authorization, never()).requireCanDelegateGrants(any());
    }

    @Test
    void createRejectsUndelegableGrantsBeforePersistence() {
        SaveRolPort save = mock(SaveRolPort.class);
        doThrow(new CrmAuthorizationDeniedException("grant outside delegable permissions"))
                .when(authorization).requireCanDelegateGrants(any());
        var grant = new PermisoRecurso(RecursoCrm.CONTACTO, Set.of(AccionCrm.LEER),
                AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of());

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new CreateRolService(save, authorization, mutationPort)
                        .create(new CreateRolCommand("Operator", "", List.of(grant))));

        verify(save, never()).save(any());
    }

    @Test
    void roleEditAndDeleteDenialsStopBeforeReadingOrWriting() {
        UUID roleId = UUID.randomUUID();
        FindRolByIdPort find = mock(FindRolByIdPort.class);
        SaveRolPort save = mock(SaveRolPort.class);
        ExistsUsuariosByRolIdPort existsUsers = mock(ExistsUsuariosByRolIdPort.class);
        DeleteRolByIdPort delete = mock(DeleteRolByIdPort.class);
        deny(RecursoCrm.ROL, AccionCrm.ADMINISTRAR);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new EditRolService(find, save, authorization, mock(CurrentActorPort.class), mutationPort)
                        .edit(new EditRolCommand(roleId, "Renamed", "", null, null)));
        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new DeleteRolService(find, existsUsers, delete, authorization, mutationPort)
                        .delete(new DeleteRolCommand(roleId)));

        verifyNoInteractions(find, save, existsUsers, delete);
    }

    @Test
    void roleCannotChangeOwnPermissions() {
        UUID roleId = UUID.randomUUID();
        FindRolByIdPort find = mock(FindRolByIdPort.class);
        SaveRolPort save = mock(SaveRolPort.class);
        CurrentActorPort actorPort = mock(CurrentActorPort.class);
        Rol currentRole = Rol.reconstitute(RolId.from(roleId), "Operator", "", true, List.of());
        when(find.findById(RolId.from(roleId))).thenReturn(Optional.of(currentRole));
        when(actorPort.currentActor()).thenReturn(Optional.of(
                new CurrentActor(UUID.randomUUID(), roleId, false)));
        var grant = new PermisoRecurso(RecursoCrm.CONTACTO, Set.of(AccionCrm.LEER),
                AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of());

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new EditRolService(find, save, authorization, actorPort, mutationPort)
                        .edit(new EditRolCommand(roleId, "Operator", "", null, List.of(grant))));

        verify(save, never()).save(any());
    }

    private void deny(RecursoCrm resource, AccionCrm action) {
        doThrow(new CrmAuthorizationDeniedException("denied")).when(authorization).require(resource, action);
    }
}