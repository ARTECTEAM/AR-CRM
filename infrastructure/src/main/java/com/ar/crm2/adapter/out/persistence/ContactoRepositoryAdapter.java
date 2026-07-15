package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.ContactoEntity;
import com.ar.crm2.adapter.out.persistence.mapper.ContactoMapper;
import com.ar.crm2.adapter.out.persistence.repository.ContactoRepository;
import com.ar.crm2.application.contacto.port.out.DeleteContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.ExistsTratosByContactoIdPort;
import com.ar.crm2.application.contacto.port.out.FindAllContactosPort;
import com.ar.crm2.application.contacto.port.out.FindContactoByIdPort;
import com.ar.crm2.application.contacto.port.out.SaveContactoPort;
import com.ar.crm2.application.contacto.query.ContactoFilterCriteria;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.vo.ContactoId;
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
public class ContactoRepositoryAdapter implements SaveContactoPort, FindAllContactosPort, FindContactoByIdPort, DeleteContactoByIdPort, ExistsTratosByContactoIdPort {

    private final ContactoRepository repository;

    @Override
    public Contacto save(Contacto contacto) {
        ContactoEntity entity = ContactoMapper.toEntity(contacto);
        ContactoEntity saved = repository.save(entity);
        return ContactoMapper.toDomain(saved);
    }

    @Override
    public List<Contacto> findAll() {
        return findAll(ContactoFilterCriteria.empty());
    }

    @Override
    public List<Contacto> findAll(ContactoFilterCriteria criteria) {
        return repository.findAll(spec(criteria), sort(criteria))
            .stream()
            .map(ContactoMapper::toDomain)
            .toList();
    }

    @Override
    public PagedResult<Contacto> findPage(ContactoFilterCriteria criteria) {
        ContactoFilterCriteria resolved = criteria == null ? ContactoFilterCriteria.empty() : criteria;
        ListPageRequest pageRequest = resolved.pageRequest() == null ? ListPageRequest.unpaged() : resolved.pageRequest();
        Pageable pageable = PageRequest.of(pageRequest.normalizedPage(), pageRequest.normalizedPageSize(), sort(resolved));
        Page<Contacto> page = repository.findAll(spec(resolved), pageable).map(ContactoMapper::toDomain);
        return new PagedResult<>(page.getContent(), page.getTotalElements(), page.getNumber(), page.getSize(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
    }

    private Specification<ContactoEntity> spec(ContactoFilterCriteria criteria) {
        ContactoFilterCriteria resolved = criteria == null ? ContactoFilterCriteria.empty() : criteria;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String term = normalize(resolved.search());
            if (term != null) {
                String like = "%" + term + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("nombre")), like),
                    cb.like(cb.lower(root.get("correo")), like),
                    cb.like(cb.lower(root.get("cargo")), like),
                    cb.like(cb.lower(root.get("telefono")), like)
                ));
            }
            if (resolved.estadoRelacion() != null) predicates.add(cb.equal(root.get("estadoRelacion"), resolved.estadoRelacion()));
            if (resolved.empresaId() != null) predicates.add(cb.equal(root.get("empresaId"), resolved.empresaId().value().toString()));
            if (resolved.responsableId() != null) predicates.add(cb.equal(root.get("responsableId"), resolved.responsableId().value().toString()));
            String origen = normalize(resolved.comoNosConocio());
            if (origen != null) predicates.add(cb.equal(cb.lower(root.get("comoNosConocio")), origen));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Sort sort(ContactoFilterCriteria criteria) {
        ListPageRequest pageRequest = criteria == null || criteria.pageRequest() == null ? ListPageRequest.unpaged() : criteria.pageRequest();
        String property = switch (pageRequest.sortBy() == null ? "creadoEn" : pageRequest.sortBy()) {
            case "nombre" -> "nombre";
            case "correo" -> "correo";
            case "empresaId" -> "empresaId";
            case "estadoRelacion" -> "estadoRelacion";
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
    public Optional<Contacto> findById(ContactoId id) {
        return repository.findById(id.value().toString())
            .map(ContactoMapper::toDomain);
    }

    @Override
    public void deleteById(ContactoId id) {
        repository.deleteById(id.value().toString());
    }

    @Override
    public boolean existsTratosByContactoId(ContactoId contactoId) {
        return repository.existsTratosByContactoId(contactoId.value().toString());
    }
}
