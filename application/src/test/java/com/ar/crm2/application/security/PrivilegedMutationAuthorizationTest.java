package com.ar.crm2.application.security;

import com.ar.crm2.application.identity.model.ProvisionedIdentity;
import com.ar.crm2.application.identity.port.out.DeleteIdentityPort;
import com.ar.crm2.application.identity.port.out.ProvisionIdentityPort;
import com.ar.crm2.application.identity.port.out.SetIdentityAttributesPort;
import com.ar.crm2.application.rol.command.EditRolCommand;
import com.ar.crm2.application.rol.command.DeleteRolCommand;
import com.ar.crm2.application.rol.exception.RolHasAssociatedUsuariosException;
import com.ar.crm2.application.rol.port.out.DeleteRolByIdPort;
import com.ar.crm2.application.rol.port.out.ExistsUsuariosByRolIdPort;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.rol.port.out.SaveRolPort;
import com.ar.crm2.application.rol.service.DeleteRolService;
import com.ar.crm2.application.rol.service.EditRolService;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.usuario.command.CreateUsuarioCommand;
import com.ar.crm2.application.usuario.command.EditUsuarioCommand;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.PromoteBootstrapAdministratorPort;
import com.ar.crm2.application.usuario.port.out.SaveUsuarioPort;
import com.ar.crm2.application.usuario.service.CreateUsuarioService;
import com.ar.crm2.application.usuario.service.EditUsuarioService;
import com.ar.crm2.application.identity.port.out.SetIdentityEnabledPort;
import com.ar.crm2.application.identity.port.out.SyncIdentityEmailPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrivilegedMutationAuthorizationTest {

    @Test
    void roleEditMergesOmittedPermissionsFromTheRoleReadInsideTheMutationBoundary() {
        UUID roleId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID actorRoleId = UUID.randomUUID();
        var oldGrant = new PermisoRecurso(RecursoCrm.CONTACTO, Set.of(AccionCrm.LEER),
                AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of());
        Rol beforeConcurrentRevoke = role(roleId, List.of(oldGrant));
        Rol afterConcurrentRevoke = role(roleId, List.of());
        AtomicBoolean insideBoundary = new AtomicBoolean();
        FindRolByIdPort findRole = mock(FindRolByIdPort.class);
        when(findRole.findById(RolId.from(roleId))).thenAnswer(invocation -> Optional.of(
                insideBoundary.get() ? afterConcurrentRevoke : beforeConcurrentRevoke));
        SaveRolPort saveRole = mock(SaveRolPort.class);
        when(saveRole.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        CurrentActorPort currentActor = () -> Optional.of(
                new CurrentActor(actorId, actorRoleId, false));
        AuthorizationMutationPort mutation = new AuthorizationMutationPort() {
            @Override
            public <T> T execute(Supplier<T> callback) {
                insideBoundary.set(true);
                return callback.get();
            }
        };

        EditRolService service = new EditRolService(findRole, saveRole, authorization, currentActor, mutation);
        Rol updated = service.edit(new EditRolCommand(roleId, "Renamed", null));

        assertEquals(List.of(), updated.getPermisos(),
                "a rename after concurrent revocation must not restore the stale permission");
        verify(findRole, org.mockito.Mockito.times(1)).findById(RolId.from(roleId));
    }

    @Test
    void userCreationRevalidatesTargetRoleGrantsAfterEnteringMutationBoundary() {
        UUID actorRoleId = UUID.randomUUID();
        UUID targetRoleId = UUID.randomUUID();
        Rol lowPrivilegeRole = role(targetRoleId, List.of());
        Rol expandedRole = role(targetRoleId, List.of(new PermisoRecurso(RecursoCrm.ROL,
                Set.of(AccionCrm.ADMINISTRAR), AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of())));
        AtomicBoolean insideBoundary = new AtomicBoolean();
        FindRolByIdPort findRole = mock(FindRolByIdPort.class);
        when(findRole.findById(RolId.from(targetRoleId))).thenAnswer(invocation -> Optional.of(
                insideBoundary.get() ? expandedRole : lowPrivilegeRole));
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doAnswer(invocation -> {
            List<PermisoRecurso> grants = invocation.getArgument(0);
            if (grants.stream().anyMatch(grant -> grant.recurso() == RecursoCrm.ROL
                    && grant.permite(AccionCrm.ADMINISTRAR))) {
                throw new CrmAuthorizationDeniedException("Cannot delegate expanded role grants");
            }
            return null;
        }).when(authorization).requireCanDelegateGrants(any());
        CurrentActorPort currentActor = () -> Optional.of(
                new CurrentActor(UUID.randomUUID(), actorRoleId, false));
        AuthorizationMutationPort mutation = new AuthorizationMutationPort() {
            @Override
            public <T> T execute(Supplier<T> callback) {
                insideBoundary.set(true);
                return callback.get();
            }
        };
        ProvisionIdentityPort provision = mock(ProvisionIdentityPort.class);
        DeleteIdentityPort deleteIdentity = mock(DeleteIdentityPort.class);
        SetIdentityAttributesPort setAttributes = mock(SetIdentityAttributesPort.class);
        SaveUsuarioPort saveUsuario = mock(SaveUsuarioPort.class);
        when(provision.provision("new@example.com", "password", true))
                .thenReturn(new ProvisionedIdentity("kc-new-user", "new@example.com"));
        when(saveUsuario.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUsuarioService service = new CreateUsuarioService(saveUsuario, provision, deleteIdentity,
                setAttributes, authorization, currentActor, findRole, mutation);

        assertThrows(CrmAuthorizationDeniedException.class, () -> service.create(
                new CreateUsuarioCommand("New User", "new@example.com", targetRoleId, null, "password")));
        verify(saveUsuario, never()).save(any());
        verify(deleteIdentity).delete("kc-new-user");
    }

    @Test
    void userEditRevalidatesTargetRoleGrantsAfterEnteringMutationBoundary() {
        UUID userId = UUID.randomUUID();
        UUID actorRoleId = UUID.randomUUID();
        UUID previousRoleId = UUID.randomUUID();
        UUID targetRoleId = UUID.randomUUID();
        Usuario existing = Usuario.reconstitute(UsuarioId.from(userId), "Existing", "old@example.com",
                RolId.from(previousRoleId), LocalDateTime.now(), true, null);
        Rol lowPrivilegeRole = role(targetRoleId, List.of());
        Rol expandedRole = role(targetRoleId, List.of(new PermisoRecurso(RecursoCrm.ROL,
                Set.of(AccionCrm.ADMINISTRAR), AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of())));
        AtomicBoolean insideBoundary = new AtomicBoolean();
        FindUsuarioByIdPort findUser = mock(FindUsuarioByIdPort.class);
        when(findUser.findById(UsuarioId.from(userId))).thenReturn(Optional.of(existing));
        FindRolByIdPort findRole = mock(FindRolByIdPort.class);
        when(findRole.findById(RolId.from(targetRoleId))).thenAnswer(invocation -> Optional.of(
                insideBoundary.get() ? expandedRole : lowPrivilegeRole));
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doAnswer(invocation -> {
            List<PermisoRecurso> grants = invocation.getArgument(0);
            if (grants.stream().anyMatch(grant -> grant.recurso() == RecursoCrm.ROL
                    && grant.permite(AccionCrm.ADMINISTRAR))) {
                throw new CrmAuthorizationDeniedException("Cannot delegate expanded role grants");
            }
            return null;
        }).when(authorization).requireCanDelegateGrants(any());
        CurrentActorPort currentActor = () -> Optional.of(
                new CurrentActor(UUID.randomUUID(), actorRoleId, false));
        AuthorizationMutationPort mutation = new AuthorizationMutationPort() {
            @Override
            public <T> T execute(Supplier<T> callback) {
                insideBoundary.set(true);
                return callback.get();
            }
        };
        SaveUsuarioPort saveUser = mock(SaveUsuarioPort.class);
        SyncIdentityEmailPort syncEmail = mock(SyncIdentityEmailPort.class);
        SetIdentityEnabledPort setEnabled = mock(SetIdentityEnabledPort.class);
        PromoteBootstrapAdministratorPort promote = mock(PromoteBootstrapAdministratorPort.class);
        EditUsuarioService service = new EditUsuarioService(findUser, saveUser, syncEmail, setEnabled,
                authorization, currentActor, findRole, promote, mutation);

        assertThrows(CrmAuthorizationDeniedException.class, () -> service.edit(new EditUsuarioCommand(userId,
                "Existing", "old@example.com", targetRoleId, null)));
        verify(saveUser, never()).save(any());
    }

    @Test
    void roleDeletionChecksAssignmentsInsideMutationBoundary() {
        UUID roleId = UUID.randomUUID();
        AtomicBoolean insideBoundary = new AtomicBoolean();
        FindRolByIdPort findRole = mock(FindRolByIdPort.class);
        when(findRole.findById(RolId.from(roleId))).thenReturn(Optional.of(role(roleId, List.of())));
        ExistsUsuariosByRolIdPort existsUsers = mock(ExistsUsuariosByRolIdPort.class);
        when(existsUsers.existsUsuariosByRolId(RolId.from(roleId))).thenAnswer(invocation -> insideBoundary.get());
        DeleteRolByIdPort deleteRole = mock(DeleteRolByIdPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        AuthorizationMutationPort mutation = new AuthorizationMutationPort() {
            @Override
            public <T> T execute(Supplier<T> callback) {
                insideBoundary.set(true);
                return callback.get();
            }
        };
        DeleteRolService service = new DeleteRolService(findRole, existsUsers, deleteRole, authorization, mutation);

        assertThrows(RolHasAssociatedUsuariosException.class,
                () -> service.delete(new DeleteRolCommand(roleId)));
        verify(deleteRole, never()).deleteById(any());
    }

    private static Rol role(UUID id, List<PermisoRecurso> grants) {
        return Rol.reconstitute(RolId.from(id), "Role", null, true, grants);
    }
}
