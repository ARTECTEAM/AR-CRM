package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import com.ar.crm2.adapter.out.persistence.mapper.TratoMapper;
import com.ar.crm2.adapter.out.persistence.repository.TratoRepository;
import com.ar.crm2.application.trato.port.out.DeleteTratoByIdPort;
import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;
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
import java.time.LocalDate;

@RequiredArgsConstructor
public class TratoRepositoryAdapter implements SaveTratoPort, FindAllTratosPort, FindTratoByIdPort, DeleteTratoByIdPort {

    private final TratoRepository repository;

    @Override
    public Trato save(Trato trato) {
        TratoEntity entity = TratoMapper.toEntity(trato);
        TratoEntity saved = repository.save(entity);
        return TratoMapper.toDomain(saved);
    }

    @Override
    public List<Trato> findAll() {
        return findAll(TratoFilterCriteria.empty());
    }

    @Override
    public List<Trato> findAll(TratoFilterCriteria criteria) {
        return repository.findAll(spec(criteria), sort(criteria))
            .stream()
            .map(TratoMapper::toDomain)
            .toList();
    }

    @Override
    public PagedResult<Trato> findPage(TratoFilterCriteria criteria) {
        TratoFilterCriteria resolved = criteria == null ? TratoFilterCriteria.empty() : criteria;
        ListPageRequest pageRequest = resolved.pageRequest() == null ? ListPageRequest.unpaged() : resolved.pageRequest();
        Pageable pageable = PageRequest.of(pageRequest.normalizedPage(), pageRequest.normalizedPageSize(), sort(resolved));
        Page<Trato> page = repository.findAll(spec(resolved), pageable).map(TratoMapper::toDomain);
        return new PagedResult<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }

    private Specification<TratoEntity> spec(TratoFilterCriteria criteria) {
        TratoFilterCriteria resolved = criteria == null ? TratoFilterCriteria.empty() : criteria;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String term = normalize(resolved.search());
            if (term != null) predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + term + "%"));
            if (resolved.estado() != null) predicates.add(cb.equal(root.get("estado"), resolved.estado()));
            if (resolved.tipoContrato() != null) predicates.add(cb.equal(root.get("tipoContrato"), resolved.tipoContrato()));
            if (resolved.responsableId() != null) predicates.add(cb.equal(root.get("responsableId"), resolved.responsableId().value().toString()));
            if (resolved.contactoId() != null) predicates.add(cb.equal(root.get("contactoId"), resolved.contactoId().value().toString()));
            if (resolved.valorMin() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("valorEstimado"), resolved.valorMin()));
            if (resolved.valorMax() != null) predicates.add(cb.lessThanOrEqualTo(root.get("valorEstimado"), resolved.valorMax()));
            addCierrePredicate(predicates, root, cb, resolved.cierreEsperado());
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void addCierrePredicate(List<Predicate> predicates, jakarta.persistence.criteria.Root<TratoEntity> root,
            jakarta.persistence.criteria.CriteriaBuilder cb, TratoFilterCriteria.CierreEsperadoFilter filter) {
        if (filter == null || filter == TratoFilterCriteria.CierreEsperadoFilter.TODAS) return;
        if (filter == TratoFilterCriteria.CierreEsperadoFilter.SIN_FECHA) {
            predicates.add(cb.isNull(root.get("fechaCierreEsperada")));
            return;
        }
        LocalDate today = LocalDate.now();
        if (filter == TratoFilterCriteria.CierreEsperadoFilter.VENCIDAS) {
            predicates.add(cb.lessThan(root.get("fechaCierreEsperada"), today));
            return;
        }
        int days = filter == TratoFilterCriteria.CierreEsperadoFilter.PROXIMOS_7 ? 7 : 30;
        predicates.add(cb.between(root.get("fechaCierreEsperada"), today, today.plusDays(days)));
    }

    private Sort sort(TratoFilterCriteria criteria) {
        ListPageRequest pageRequest = criteria == null || criteria.pageRequest() == null ? ListPageRequest.unpaged() : criteria.pageRequest();
        String property = switch (pageRequest.sortBy() == null ? "creadoEn" : pageRequest.sortBy()) {
            case "nombre" -> "nombre";
            case "valorEstimado" -> "valorEstimado";
            case "fechaCierreEsperada" -> "fechaCierreEsperada";
            case "estado" -> "estado";
            case "actualizadoEn" -> "actualizadoEn";
            default -> "creadoEn";
        };
        Sort.Direction direction = pageRequest.normalizedSortDirection() == ListPageRequest.SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public Optional<Trato> findById(TratoId id) {
        return repository.findById(id.value().toString())
            .map(TratoMapper::toDomain);
    }

    @Override
    public void deleteById(TratoId id) {
        repository.deleteById(id.value().toString());
    }
}
