package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.RolEntity;
import com.ar.crm2.adapter.out.persistence.entity.RolPermisoEmbeddable;
import com.ar.crm2.adapter.out.persistence.exception.LastActiveRoleManagerRequiredException;
import com.ar.crm2.adapter.out.persistence.exception.BootstrapPromotionUnavailableException;
import com.ar.crm2.adapter.out.persistence.repository.RolRepository;
import com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleManagerGovernanceTest {
    private final RolRepository roles = mock(RolRepository.class);
    private final UsuarioRepository users = mock(UsuarioRepository.class);

    @Test
    void refusesToRemoveTheLastActiveRoleManager() {
        String roleId = java.util.UUID.randomUUID().toString();
        RolEntity oldEntity = managerEntity(roleId);
        List<RolEntity> lockedRoles = List.of(oldEntity);
        Rol oldRole = managerRole(roleId);
        Rol replacement = Rol.reconstitute(oldRole.getId(), "Viewer", null, true, List.of());
        when(users.countByActivoTrueAndRolIdIn(Set.of(roleId))).thenReturn(1L);
        when(users.countByActivoTrueAndRolId(roleId)).thenReturn(1L);

        RoleManagerGovernance governance = new RoleManagerGovernance(roles, users);
        assertThatThrownBy(() -> governance.assertRoleTransition(lockedRoles, oldRole, replacement))
                .isInstanceOf(LastActiveRoleManagerRequiredException.class);
    }

    @Test
    void allowsManagerChangesWhenAnotherActiveManagerRemains() {
        String roleId = java.util.UUID.randomUUID().toString();
        RolEntity oldEntity = managerEntity(roleId);
        Rol oldRole = managerRole(roleId);
        Rol replacement = Rol.reconstitute(oldRole.getId(), "Viewer", null, true, List.of());
        when(users.countByActivoTrueAndRolIdIn(Set.of(roleId))).thenReturn(2L);
        when(users.countByActivoTrueAndRolId(roleId)).thenReturn(1L);

        RoleManagerGovernance governance = new RoleManagerGovernance(roles, users);
        governance.assertRoleTransition(List.of(oldEntity), oldRole, replacement);
    }

    @Test
    void bootstrapPromotionIsAllowedOnlyWhenNoActiveManagerExists() {
        String managerId = UUID.randomUUID().toString();
        RolEntity manager = managerEntity(managerId);
        Usuario existing = user(UUID.randomUUID(), UUID.randomUUID());
        Usuario promoted = Usuario.reconstitute(existing.getId(), existing.getNombre(), existing.getCorreo(),
                RolId.from(UUID.fromString(managerId)), existing.getCreadoEn(), existing.isActivo(), null);
        when(users.countByActivoTrueAndRolIdIn(Set.of(managerId))).thenReturn(0L);

        RoleManagerGovernance governance = new RoleManagerGovernance(roles, users);
        governance.assertBootstrapManagerPromotion(List.of(manager), existing, promoted);

        verify(users).countByActivoTrueAndRolIdIn(Set.of(managerId));
    }

    @Test
    void bootstrapPromotionIsDeniedWhenAnyActiveManagerAlreadyExists() {
        String managerId = UUID.randomUUID().toString();
        RolEntity manager = managerEntity(managerId);
        Usuario existing = user(UUID.randomUUID(), UUID.randomUUID());
        Usuario promoted = Usuario.reconstitute(existing.getId(), existing.getNombre(), existing.getCorreo(),
                RolId.from(UUID.fromString(managerId)), existing.getCreadoEn(), existing.isActivo(), null);
        when(users.countByActivoTrueAndRolIdIn(Set.of(managerId))).thenReturn(1L);

        RoleManagerGovernance governance = new RoleManagerGovernance(roles, users);
        assertThatThrownBy(() -> governance.assertBootstrapManagerPromotion(List.of(manager), existing, promoted))
                .isInstanceOf(BootstrapPromotionUnavailableException.class);
    }

    @Test
    void legacyScopedRoleAdminDoesNotCountAsManagerOrWeakenLastManagerGuard() {
        String managerId = UUID.randomUUID().toString();
        String malformedScopedId = UUID.randomUUID().toString();
        RolEntity manager = managerEntity(managerId);
        RolEntity malformed = scopedManagerEntity(malformedScopedId);
        Rol oldRole = managerRole(managerId);
        Rol replacement = Rol.reconstitute(oldRole.getId(), "Viewer", null, true, List.of());
        when(users.countByActivoTrueAndRolIdIn(Set.of(managerId))).thenReturn(1L);
        when(users.countByActivoTrueAndRolId(managerId)).thenReturn(1L);

        RoleManagerGovernance governance = new RoleManagerGovernance(roles, users);
        assertThatThrownBy(() -> governance.assertRoleTransition(List.of(manager, malformed), oldRole, replacement))
                .isInstanceOf(LastActiveRoleManagerRequiredException.class);

        verify(users).countByActivoTrueAndRolIdIn(Set.of(managerId));
    }

    @Test
    void refusesRemovalOfTheLastActiveManagerUser() {
        String managerId = UUID.randomUUID().toString();
        RolEntity manager = managerEntity(managerId);
        Usuario existingManager = user(UUID.randomUUID(), UUID.fromString(managerId));
        when(users.countByActivoTrueAndRolIdIn(Set.of(managerId))).thenReturn(1L);

        RoleManagerGovernance governance = new RoleManagerGovernance(roles, users);
        assertThatThrownBy(() -> governance.assertUserRemoval(List.of(manager), existingManager))
                .isInstanceOf(LastActiveRoleManagerRequiredException.class);

        verify(users).countByActivoTrueAndRolIdIn(Set.of(managerId));
    }

    private static RolEntity managerEntity(String roleId) {
        return RolEntity.builder().id(roleId).nombre("Manager").activo(true)
                .permisos(Map.of(RecursoCrm.ROL.name(), new RolPermisoEmbeddable(
                        AccionCrm.ADMINISTRAR.name(), AlcanceCrm.TODO_COMPARTIDO.name(), "", "", "")))
                .build();
    }

    private static RolEntity scopedManagerEntity(String roleId) {
        return RolEntity.builder().id(roleId).nombre("Legacy scoped manager").activo(true)
                .permisos(Map.of(RecursoCrm.ROL.name(), new RolPermisoEmbeddable(
                        AccionCrm.ADMINISTRAR.name(), AlcanceCrm.PROPIOS_O_ASIGNADOS.name(), "", "", "")))
                .build();
    }

    private static Usuario user(UUID userId, UUID roleId) {
        return Usuario.reconstitute(UsuarioId.from(userId), "User", "user@example.com",
                RolId.from(roleId), LocalDateTime.now(), true, null);
    }

    private static Rol managerRole(String roleId) {
        return Rol.reconstitute(RolId.from(java.util.UUID.fromString(roleId)), "Manager", null, true,
                List.of(new PermisoRecurso(RecursoCrm.ROL, Set.of(AccionCrm.ADMINISTRAR),
                        AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of())));
    }
}
