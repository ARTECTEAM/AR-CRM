package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.identity.port.out.SetIdentityEnabledPort;
import com.ar.crm2.application.identity.port.out.SyncIdentityEmailPort;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.CurrentActor;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import com.ar.crm2.application.usuario.command.EditUsuarioCommand;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.PromoteBootstrapAdministratorPort;
import com.ar.crm2.application.usuario.port.out.SaveUsuarioPort;
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
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EditUsuarioServiceBootstrapPromotionTest {
    private final FindUsuarioByIdPort findUsuario = mock(FindUsuarioByIdPort.class);
    private final SaveUsuarioPort saveUsuario = mock(SaveUsuarioPort.class);
    private final SyncIdentityEmailPort syncEmail = mock(SyncIdentityEmailPort.class);
    private final SetIdentityEnabledPort setEnabled = mock(SetIdentityEnabledPort.class);
    private final CrmAuthorization authorization = mock(CrmAuthorization.class);
    private final CurrentActorPort currentActor = mock(CurrentActorPort.class);
    private final FindRolByIdPort findRole = mock(FindRolByIdPort.class);
    private final PromoteBootstrapAdministratorPort promote = mock(PromoteBootstrapAdministratorPort.class);
    private final AuthorizationMutationPort mutationPort = new AuthorizationMutationPort() {
        @Override
        public <T> T execute(Supplier<T> mutation) {
            return mutation.get();
        }
    };

    @Test
    void bootstrapMayPromoteOnlyThroughTheLockedOneTimePromotionPort() {
        UUID userId = UUID.randomUUID();
        UUID oldRoleId = UUID.randomUUID();
        UUID managerRoleId = UUID.randomUUID();
        Usuario existing = usuario(userId, oldRoleId);
        Usuario promoted = usuario(userId, managerRoleId);
        Rol manager = Rol.reconstitute(RolId.from(managerRoleId), "Manager", null, true, List.of(
                new PermisoRecurso(RecursoCrm.ROL, Set.of(AccionCrm.ADMINISTRAR),
                        AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of())));
        when(currentActor.currentActor()).thenReturn(Optional.of(new CurrentActor(userId, oldRoleId, true)));
        when(findUsuario.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(findRole.findById(RolId.from(managerRoleId))).thenReturn(Optional.of(manager));
        when(promote.promoteIfNoActiveManager(any(), org.mockito.ArgumentMatchers.eq(userId)))
                .thenReturn(promoted);

        EditUsuarioService service = service();
        Usuario result = service.edit(new EditUsuarioCommand(userId, "Bootstrap", "bootstrap@example.com",
                managerRoleId, null));

        org.junit.jupiter.api.Assertions.assertSame(promoted, result);
        verify(promote).promoteIfNoActiveManager(any(), org.mockito.ArgumentMatchers.eq(userId));
        verify(saveUsuario, never()).save(any());
    }

    @Test
    void bootstrapCannotUseSelfPromotionForAResourceScopedRoleManager() {
        UUID userId = UUID.randomUUID();
        UUID oldRoleId = UUID.randomUUID();
        UUID scopedManagerRoleId = UUID.randomUUID();
        Usuario existing = usuario(userId, oldRoleId);
        Rol nonManager = Rol.reconstitute(RolId.from(scopedManagerRoleId), "Operator", null, true,
                List.of(new PermisoRecurso(RecursoCrm.USUARIO, Set.of(AccionCrm.LEER),
                        AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of())));
        when(currentActor.currentActor()).thenReturn(Optional.of(new CurrentActor(userId, oldRoleId, true)));
        when(findUsuario.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(findRole.findById(RolId.from(scopedManagerRoleId))).thenReturn(Optional.of(nonManager));

        EditUsuarioService service = service();
        assertThrows(CrmAuthorizationDeniedException.class, () -> service.edit(new EditUsuarioCommand(userId, "Bootstrap",
                "bootstrap@example.com", scopedManagerRoleId, null)));

        verifyNoInteractions(promote);
        verify(saveUsuario, never()).save(any());
    }

    @Test
    void ordinaryUserCannotChangeTheirOwnRole() {
        UUID userId = UUID.randomUUID();
        UUID oldRoleId = UUID.randomUUID();
        UUID requestedRoleId = UUID.randomUUID();
        Usuario existing = usuario(userId, oldRoleId);
        when(currentActor.currentActor()).thenReturn(Optional.of(new CurrentActor(userId, oldRoleId, false)));
        when(findUsuario.findById(existing.getId())).thenReturn(Optional.of(existing));

        EditUsuarioService service = service();
        assertThrows(CrmAuthorizationDeniedException.class, () -> service.edit(new EditUsuarioCommand(userId,
                "Bootstrap", "bootstrap@example.com", requestedRoleId, null)));

        verifyNoInteractions(findRole, promote);
        verify(saveUsuario, never()).save(any());
    }

    private EditUsuarioService service() {
        return new EditUsuarioService(findUsuario, saveUsuario, syncEmail, setEnabled, authorization,
                currentActor, findRole, promote, mutationPort);
    }

    private static Usuario usuario(UUID userId, UUID roleId) {
        return Usuario.reconstitute(UsuarioId.from(userId), "Bootstrap", "bootstrap@example.com",
                RolId.from(roleId), LocalDateTime.now(), true, null);
    }
}
