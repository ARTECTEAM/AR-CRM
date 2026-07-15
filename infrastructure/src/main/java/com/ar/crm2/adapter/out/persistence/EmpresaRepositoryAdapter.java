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
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.vo.EmpresaId;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
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
        return repository.findAll(spec(criteria), sort(criteria))
            .stream()
            .map(EmpresaMapper::toDomain)
            .toList();
    }

    @Override
    public PagedResult<Empresa> findPage(EmpresaFilterCriteria criteria) {
        EmpresaFilterCriteria resolved = criteria == null ? EmpresaFilterCriteria.empty() : criteria;
        ListPageRequest pageRequest = resolved.pageRequest() == null ? ListPageRequest.unpaged() : resolved.pageRequest();
        Pageable pageable = PageRequest.of(
            pageRequest.normalizedPage(),
            pageRequest.normalizedPageSize(),
            sort(resolved)
        );
        Page<Empresa> page = repository.findAll(spec(resolved), pageable).map(EmpresaMapper::toDomain);
        return new PagedResult<>(
            page.getContent(),
            page.getTotalElements(),
            page.getNumber(),
            page.getSize(),
            page.getTotalPages(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    private Specification<EmpresaEntity> spec(EmpresaFilterCriteria criteria) {
        EmpresaFilterCriteria resolved = criteria == null ? EmpresaFilterCriteria.empty() : criteria;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String term = normalize(resolved.search());
            if (term != null) {
                String like = "%" + term + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("nombre")), like),
                    cb.like(cb.lower(root.get("sector")), like),
                    cb.like(cb.lower(root.get("telefono")), like),
                    cb.like(cb.lower(root.get("paginaWeb")), like)
                ));
            }
            if (resolved.estadoRelacion() != null) predicates.add(cb.equal(root.get("estadoRelacion"), resolved.estadoRelacion()));
            String sector = normalize(resolved.sector());
            if (sector != null) predicates.add(cb.equal(cb.lower(root.get("sector")), sector));
            if (resolved.responsableId() != null) predicates.add(cb.equal(root.get("responsableId"), resolved.responsableId().value().toString()));
            if (resolved.web() == EmpresaFilterCriteria.WebFilter.CON_WEB) predicates.add(cb.and(cb.isNotNull(root.get("paginaWeb")), cb.notEqual(cb.trim(root.get("paginaWeb")), "")));
            if (resolved.web() == EmpresaFilterCriteria.WebFilter.SIN_WEB) predicates.add(cb.or(cb.isNull(root.get("paginaWeb")), cb.equal(cb.trim(root.get("paginaWeb")), "")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Sort sort(EmpresaFilterCriteria criteria) {
        ListPageRequest pageRequest = criteria == null || criteria.pageRequest() == null ? ListPageRequest.unpaged() : criteria.pageRequest();
        String property = switch (pageRequest.sortBy() == null ? "creadoEn" : pageRequest.sortBy()) {
            case "nombre" -> "nombre";
            case "sector" -> "sector";
            case "estadoRelacion" -> "estadoRelacion";
            case "actualizadoEn" -> "actualizadoEn";
            default -> "creadoEn";
        };
        Sort.Direction direction = pageRequest.normalizedSortDirection() == ListPageRequest.SortDirection.ASC
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;
        return Sort.by(direction, property);
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
