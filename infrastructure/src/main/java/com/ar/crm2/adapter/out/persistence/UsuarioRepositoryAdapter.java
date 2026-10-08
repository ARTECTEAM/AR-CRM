package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.UsuarioEntity;
import com.ar.crm2.adapter.out.persistence.mapper.UsuarioMapper;
import com.ar.crm2.adapter.out.persistence.repository.UsuarioRepository;
import com.ar.crm2.application.rol.port.out.ExistsUsuariosByRolIdPort;
import com.ar.crm2.application.usuario.port.out.DeleteUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.FindAllUsuariosPort;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByCorreoPort;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByKeycloakIdPort;
import com.ar.crm2.application.usuario.port.out.PromoteBootstrapAdministratorPort;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.usuario.exception.UsuarioNotFoundException;
import com.ar.crm2.application.usuario.port.out.SaveUsuarioPort;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.RolId;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class UsuarioRepositoryAdapter implements SaveUsuarioPort, FindAllUsuariosPort, FindUsuarioByIdPort, DeleteUsuarioByIdPort, ExistsUsuariosByRolIdPort, FindUsuarioByKeycloakIdPort, FindUsuarioByCorreoPort, PromoteBootstrapAdministratorPort {

    private final UsuarioRepository repository;
    private final RoleManagerGovernance roleManagerGovernance;

    @Override
    @Transactional
    public Usuario save(Usuario usuario) {
        var lockedRoles = roleManagerGovernance.lockRoles();
        Usuario existing = repository.findById(usuario.getId().value().toString())
                .map(UsuarioMapper::toDomain).orElse(null);
        roleManagerGovernance.assertUserTransition(lockedRoles, existing, usuario);
        UsuarioEntity entity = UsuarioMapper.toEntity(usuario);
        UsuarioEntity saved = repository.save(entity);
        return UsuarioMapper.toDomain(saved);
    }

    @Override
    @Transactional
    public Usuario promoteIfNoActiveManager(Usuario usuario, UUID bootstrapActorUsuarioId) {
        if (bootstrapActorUsuarioId == null || !bootstrapActorUsuarioId.equals(usuario.getId().value())) {
            throw new CrmAuthorizationDeniedException("Bootstrap promotion must target the authenticated CRM user");
        }
        var lockedRoles = roleManagerGovernance.lockRoles();
        Usuario existing = repository.findById(usuario.getId().value().toString())
                .map(UsuarioMapper::toDomain)
                .orElseThrow(() -> UsuarioNotFoundException.forId(usuario.getId().value()));
        roleManagerGovernance.assertBootstrapManagerPromotion(lockedRoles, existing, usuario);
        UsuarioEntity saved = repository.save(UsuarioMapper.toEntity(usuario));
        return UsuarioMapper.toDomain(saved);
    }

    @Override
    public List<Usuario> findAll() {
        return repository.findAll().stream()
            .map(UsuarioMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Usuario> findById(UsuarioId id) {
        return repository.findById(id.value().toString())
            .map(UsuarioMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(UsuarioId id) {
        var lockedRoles = roleManagerGovernance.lockRoles();
        Usuario existing = repository.findById(id.value().toString())
                .map(UsuarioMapper::toDomain).orElse(null);
        roleManagerGovernance.assertUserRemoval(lockedRoles, existing);
        repository.deleteById(id.value().toString());
    }

    @Override
    public boolean existsUsuariosByRolId(RolId rolId) {
        return repository.existsByRolId(rolId.value().toString());
    }

    @Override
    public Optional<Usuario> findByKeycloakId(String keycloakId) {
        return repository.findByKeycloakId(keycloakId)
            .map(UsuarioMapper::toDomain);
    }

    @Override
    public Optional<Usuario> findByCorreo(String correo) {
        return repository.findByCorreo(correo)
            .map(UsuarioMapper::toDomain);
    }
}
