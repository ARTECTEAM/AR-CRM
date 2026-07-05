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
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.vo.ContactoId;
import lombok.RequiredArgsConstructor;

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
        return repository.findAll().stream()
            .map(ContactoMapper::toDomain)
            .filter(contacto -> matches(contacto, criteria))
            .toList();
    }

    private boolean matches(Contacto contacto, ContactoFilterCriteria criteria) {
        if (criteria == null) criteria = ContactoFilterCriteria.empty();
        String term = normalize(criteria.search());
        String origen = normalize(criteria.comoNosConocio());

        if (term != null && !containsAny(term, contacto.getNombre(), contacto.getCorreo(), contacto.getCargo(), contacto.getTelefono())) return false;
        if (criteria.estadoRelacion() != null && contacto.getEstadoRelacion() != criteria.estadoRelacion()) return false;
        if (criteria.empresaId() != null && !criteria.empresaId().equals(contacto.getEmpresaId())) return false;
        if (criteria.responsableId() != null && !criteria.responsableId().equals(contacto.getResponsableId())) return false;
        if (origen != null && !origen.equals(normalize(contacto.getComoNosConocio()))) return false;
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
