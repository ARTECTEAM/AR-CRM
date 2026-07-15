package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.TareaEntity;
import com.ar.crm2.adapter.out.persistence.mapper.TareaMapper;
import com.ar.crm2.adapter.out.persistence.repository.TareaRepository;
import com.ar.crm2.application.tarea.port.out.DeleteTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.SaveTareaPort;
import com.ar.crm2.application.tarea.query.TareaFilterCriteria;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.vo.TareaId;
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
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class TareaRepositoryAdapter implements SaveTareaPort, FindAllTareasPort, FindTareaByIdPort, DeleteTareaByIdPort {

    private final TareaRepository repository;

    @Override
    public Tarea save(Tarea tarea) {
        TareaEntity entity = TareaMapper.toEntity(tarea);
        TareaEntity saved = repository.save(entity);
        return TareaMapper.toDomain(saved);
    }

    @Override
    public List<Tarea> findAll() {
        return findAll(TareaFilterCriteria.empty());
    }

    @Override
    public List<Tarea> findAll(TareaFilterCriteria criteria) {
        return repository.findAll(spec(criteria), sort(criteria))
            .stream()
            .map(TareaMapper::toDomain)
            .toList();
    }

    @Override
    public PagedResult<Tarea> findPage(TareaFilterCriteria criteria) {
        TareaFilterCriteria resolved = criteria == null ? TareaFilterCriteria.empty() : criteria;
        ListPageRequest pageRequest = resolved.pageRequest() == null ? ListPageRequest.unpaged() : resolved.pageRequest();
        Pageable pageable = PageRequest.of(pageRequest.normalizedPage(), pageRequest.normalizedPageSize(), sort(resolved));
        Page<Tarea> page = repository.findAll(spec(resolved), pageable).map(TareaMapper::toDomain);
        return new PagedResult<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }

    private Specification<TareaEntity> spec(TareaFilterCriteria criteria) {
        TareaFilterCriteria resolved = criteria == null ? TareaFilterCriteria.empty() : criteria;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String term = normalize(resolved.search());
            if (term != null) {
                String like = "%" + term + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("titulo")), like),
                    cb.like(cb.lower(root.get("descripcion")), like)
                ));
            }
            if (resolved.prioridad() != null) predicates.add(cb.equal(root.get("prioridad"), resolved.prioridad()));
            if (resolved.responsableId() != null) predicates.add(cb.equal(root.get("responsableId"), resolved.responsableId().value().toString()));
            if (resolved.tratoId() != null) predicates.add(cb.equal(root.get("tratoId"), resolved.tratoId().value().toString()));
            if (resolved.tipo() != null) predicates.add(cb.equal(root.get("tipo"), resolved.tipo()));
            addVencimientoPredicate(predicates, root, cb, resolved.vencimiento());
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void addVencimientoPredicate(List<Predicate> predicates, jakarta.persistence.criteria.Root<TareaEntity> root,
            jakarta.persistence.criteria.CriteriaBuilder cb, TareaFilterCriteria.VencimientoFilter filter) {
        if (filter == null) return;
        LocalDateTime now = LocalDateTime.now();
        if (filter == TareaFilterCriteria.VencimientoFilter.VENCIDAS) {
            predicates.add(cb.and(cb.isNull(root.get("fechaCompletada")), cb.lessThan(root.get("fechaLimite"), now)));
            return;
        }
        predicates.add(cb.between(root.get("fechaLimite"), now, now.plusDays(7)));
    }

    private Sort sort(TareaFilterCriteria criteria) {
        ListPageRequest pageRequest = criteria == null || criteria.pageRequest() == null ? ListPageRequest.unpaged() : criteria.pageRequest();
        String property = switch (pageRequest.sortBy() == null ? "creadoEn" : pageRequest.sortBy()) {
            case "titulo" -> "titulo";
            case "prioridad" -> "prioridad";
            case "fechaLimite" -> "fechaLimite";
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
    public Optional<Tarea> findById(TareaId id) {
        return repository.findById(id.value().toString())
            .map(TareaMapper::toDomain);
    }

    @Override
    public void deleteById(TareaId id) {
        repository.deleteById(id.value().toString());
    }
}
