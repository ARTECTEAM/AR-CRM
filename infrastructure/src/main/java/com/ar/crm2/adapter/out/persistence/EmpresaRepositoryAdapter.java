package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.EmpresaEntity;
import com.ar.crm2.adapter.out.persistence.mapper.EmpresaMapper;
import com.ar.crm2.adapter.out.persistence.repository.EmpresaRepository;
import com.ar.crm2.application.empresa.port.out.DeleteEmpresaByIdPort;
import com.ar.crm2.application.empresa.port.out.ExistsTratosByEmpresaIdPort;
import com.ar.crm2.application.empresa.port.out.FindAllEmpresasPort;
import com.ar.crm2.application.empresa.port.out.FindEmpresaByIdPort;
import com.ar.crm2.application.empresa.port.out.SaveEmpresaPort;
import com.ar.crm2.application.empresa.query.EmpresaFilterCriteria;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.vo.EmpresaId;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class EmpresaRepositoryAdapter implements SaveEmpresaPort, FindAllEmpresasPort, FindEmpresaByIdPort, DeleteEmpresaByIdPort, ExistsTratosByEmpresaIdPort {

    private final EmpresaRepository repository;

    @Override
    public Empresa save(Empresa empresa) {
        EmpresaEntity entity = EmpresaMapper.toEntity(empresa);
        EmpresaEntity saved = repository.save(entity);
        return EmpresaMapper.toDomain(saved);
    }

    @Override
    public List<Empresa> findAll() {
        return findAll(EmpresaFilterCriteria.empty());
    }

    @Override
    public List<Empresa> findAll(EmpresaFilterCriteria criteria) {
        return repository.findAll().stream()
            .map(EmpresaMapper::toDomain)
            .filter(empresa -> matches(empresa, criteria))
            .toList();
    }

    private boolean matches(Empresa empresa, EmpresaFilterCriteria criteria) {
        if (criteria == null) criteria = EmpresaFilterCriteria.empty();
        String term = normalize(criteria.search());
        String sector = normalize(criteria.sector());

        if (term != null && !containsAny(term, empresa.getNombre(), empresa.getSector(), empresa.getTelefono(), empresa.getPaginaWeb())) return false;
        if (criteria.estadoRelacion() != null && empresa.getEstadoRelacion() != criteria.estadoRelacion()) return false;
        if (sector != null && !sector.equals(normalize(empresa.getSector()))) return false;
        if (criteria.responsableId() != null && !criteria.responsableId().equals(empresa.getResponsableId())) return false;
        if (criteria.web() == EmpresaFilterCriteria.WebFilter.CON_WEB && normalize(empresa.getPaginaWeb()) == null) return false;
        if (criteria.web() == EmpresaFilterCriteria.WebFilter.SIN_WEB && normalize(empresa.getPaginaWeb()) != null) return false;
        return true;
    }

    private boolean containsAny(String term, String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null && normalized.contains(term)) return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public Optional<Empresa> findById(EmpresaId id) {
        return repository.findById(id.value().toString())
            .map(EmpresaMapper::toDomain);
    }

    @Override
    public void deleteById(EmpresaId id) {
        repository.deleteById(id.value().toString());
    }

    @Override
    public boolean existsTratosByEmpresaId(EmpresaId empresaId) {
        return repository.existsTratosByEmpresaId(empresaId.value().toString());
    }
}
