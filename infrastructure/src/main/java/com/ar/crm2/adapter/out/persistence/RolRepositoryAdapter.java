package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.RolEntity;
import com.ar.crm2.adapter.out.persistence.mapper.RolMapper;
import com.ar.crm2.adapter.out.persistence.repository.RolRepository;
import com.ar.crm2.application.rol.port.out.DeleteRolByIdPort;
import com.ar.crm2.application.rol.port.out.FindAllRolesPort;
import com.ar.crm2.application.rol.port.out.FindRolByIdPort;
import com.ar.crm2.application.rol.port.out.SaveRolPort;
import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.vo.RolId;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class RolRepositoryAdapter implements SaveRolPort, FindAllRolesPort, FindRolByIdPort, DeleteRolByIdPort {

    private final RolRepository repository;
    private final RoleManagerGovernance roleManagerGovernance;

    @Override
    @Transactional
    public Rol save(Rol rol) {
        var lockedRoles = roleManagerGovernance.lockRoles();
        Rol existing = lockedRoles.stream()
                .filter(item -> item.getId().equals(rol.getId().value().toString()))
                .findFirst()
                .map(RolMapper::toDomain)
                .orElse(null);
        roleManagerGovernance.assertRoleTransition(lockedRoles, existing, rol);
        RolEntity entity = RolMapper.toEntity(rol);
        RolEntity saved = repository.save(entity);
        return RolMapper.toDomain(saved);
    }

    @Override
    public List<Rol> findAll() {
        return repository.findAll().stream()
            .map(RolMapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Rol> findById(RolId id) {
        return repository.findById(id.value().toString())
            .map(RolMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteById(RolId id) {
        roleManagerGovernance.lockRoles();
        repository.deleteById(id.value().toString());
    }
}
