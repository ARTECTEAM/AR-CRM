package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import com.ar.crm2.adapter.out.persistence.mapper.TratoMapper;
import com.ar.crm2.adapter.out.persistence.repository.TratoRepository;
import com.ar.crm2.application.trato.port.out.DeleteTratoByIdPort;
import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;
import lombok.RequiredArgsConstructor;

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
        return repository.findAll().stream()
            .map(TratoMapper::toDomain)
            .filter(trato -> matches(trato, criteria))
            .toList();
    }

    private boolean matches(Trato trato, TratoFilterCriteria criteria) {
        if (criteria == null) criteria = TratoFilterCriteria.empty();
        String term = normalize(criteria.search());
        if (term != null && !normalize(trato.getNombre()).contains(term)) return false;
        if (criteria.estado() != null && trato.getEstado() != criteria.estado()) return false;
        if (criteria.tipoContrato() != null && trato.getTipoContrato() != criteria.tipoContrato()) return false;
        if (criteria.responsableId() != null && !criteria.responsableId().equals(trato.getResponsableId())) return false;
        if (criteria.contactoId() != null && !criteria.contactoId().equals(trato.getContactoId())) return false;
        if (criteria.valorMin() != null && (trato.getValorEstimado() == null || trato.getValorEstimado().compareTo(criteria.valorMin()) < 0)) return false;
        if (criteria.valorMax() != null && (trato.getValorEstimado() == null || trato.getValorEstimado().compareTo(criteria.valorMax()) > 0)) return false;
        return matchesCierre(trato.getFechaCierreEsperada(), criteria.cierreEsperado());
    }

    private boolean matchesCierre(LocalDate fecha, TratoFilterCriteria.CierreEsperadoFilter filter) {
        if (filter == null || filter == TratoFilterCriteria.CierreEsperadoFilter.TODAS) return true;
        if (filter == TratoFilterCriteria.CierreEsperadoFilter.SIN_FECHA) return fecha == null;
        if (fecha == null) return false;
        LocalDate today = LocalDate.now();
        if (filter == TratoFilterCriteria.CierreEsperadoFilter.VENCIDAS) return fecha.isBefore(today);
        int days = filter == TratoFilterCriteria.CierreEsperadoFilter.PROXIMOS_7 ? 7 : 30;
        LocalDate limit = today.plusDays(days);
        return !fecha.isBefore(today) && !fecha.isAfter(limit);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return "";
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
