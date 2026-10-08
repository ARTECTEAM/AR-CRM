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
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Database-lock-aware last-manager invariant shared by role and user persistence writes. */
@Component
public final class RoleManagerGovernance {
    private final RolRepository roleRepository;
    private final UsuarioRepository userRepository;

    public RoleManagerGovernance(RolRepository roleRepository, UsuarioRepository userRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public List<RolEntity> lockRoles() {
        return roleRepository.findAllForAuthorizationUpdate();
    }

    public void assertBootstrapManagerPromotion(List<RolEntity> lockedRoles,
                                               Usuario existingUser,
                                               Usuario promotedUser) {
        if (existingUser == null || !existingUser.isActivo() || !promotedUser.isActivo()
                || !existingUser.getId().equals(promotedUser.getId())
                || existingUser.getRolId().equals(promotedUser.getRolId())) {
            throw new BootstrapPromotionUnavailableException("Invalid bootstrap role promotion request");
        }
        boolean targetIsManager = lockedRoles.stream()
                .filter(role -> role.getId().equals(promotedUser.getRolId().value().toString()))
                .anyMatch(RoleManagerGovernance::isManager);
        if (!targetIsManager) {
            throw new BootstrapPromotionUnavailableException(
                    "Bootstrap self-assignment requires an active shared role-manager role");
        }
        Set<String> managers = lockedRoles.stream().filter(RoleManagerGovernance::isManager)
                .map(RolEntity::getId).collect(Collectors.toSet());
        if (!managers.isEmpty() && userRepository.countByActivoTrueAndRolIdIn(managers) > 0) {
            throw new BootstrapPromotionUnavailableException(
                    "Bootstrap self-promotion is available only before an active role manager exists");
        }
    }

    public void assertRoleTransition(List<RolEntity> lockedRoles, Rol oldRole, Rol newRole) {
        if (oldRole == null || !isManager(oldRole) || isManager(newRole)) return;
        Set<String> managers = lockedRoles.stream().filter(RoleManagerGovernance::isManager)
                .map(RolEntity::getId).collect(Collectors.toSet());
        long total = userRepository.countByActivoTrueAndRolIdIn(managers);
        long affected = userRepository.countByActivoTrueAndRolId(oldRole.getId().value().toString());
        if (total - affected < 1) throw new LastActiveRoleManagerRequiredException();
    }

    public void assertUserTransition(List<RolEntity> lockedRoles, Usuario oldUser, Usuario newUser) {
        if (oldUser == null || !oldUser.isActivo() || oldUser.getRolId().equals(newUser.getRolId())) return;
        boolean wasManager = lockedRoles.stream().filter(role -> role.getId().equals(oldUser.getRolId().value().toString()))
                .anyMatch(RoleManagerGovernance::isManager);
        boolean remainsManager = newUser.isActivo() && lockedRoles.stream()
                .filter(role -> role.getId().equals(newUser.getRolId().value().toString()))
                .anyMatch(RoleManagerGovernance::isManager);
        if (!wasManager || remainsManager) return;
        Set<String> managers = lockedRoles.stream().filter(RoleManagerGovernance::isManager)
                .map(RolEntity::getId).collect(Collectors.toSet());
        if (userRepository.countByActivoTrueAndRolIdIn(managers) <= 1) {
            throw new LastActiveRoleManagerRequiredException();
        }
    }

    public void assertUserRemoval(List<RolEntity> lockedRoles, Usuario oldUser) {
        if (oldUser == null || !oldUser.isActivo()) return;
        boolean manager = lockedRoles.stream().filter(role -> role.getId().equals(oldUser.getRolId().value().toString()))
                .anyMatch(RoleManagerGovernance::isManager);
        if (!manager) return;
        Set<String> managers = lockedRoles.stream().filter(RoleManagerGovernance::isManager)
                .map(RolEntity::getId).collect(Collectors.toSet());
        if (userRepository.countByActivoTrueAndRolIdIn(managers) <= 1) {
            throw new LastActiveRoleManagerRequiredException();
        }
    }

    private static boolean isManager(Rol role) {
        return role.isActivo() && role.getPermisos().stream().anyMatch(grant ->
                grant.recurso() == RecursoCrm.ROL && grant.permite(AccionCrm.ADMINISTRAR)
                        && grant.alcance() == AlcanceCrm.TODO_COMPARTIDO && grant.idsPermitidos().isEmpty());
    }

    private static boolean isManager(RolEntity role) {
        if (!role.isActivo()) return false;
        RolPermisoEmbeddable roleGrant = role.getPermisos() == null
                ? null : role.getPermisos().get(RecursoCrm.ROL.name());
        if (roleGrant == null || roleGrant.getAcciones() == null
                || !AlcanceCrm.TODO_COMPARTIDO.name().equals(roleGrant.getAlcance())
                || (roleGrant.getIdsPermitidos() != null && !roleGrant.getIdsPermitidos().isBlank())) {
            return false;
        }
        return Set.of(roleGrant.getAcciones().split(",")).contains(AccionCrm.ADMINISTRAR.name());
    }
}
