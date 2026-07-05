package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.TareaEntity;
import com.ar.crm2.adapter.out.persistence.mapper.TareaMapper;
import com.ar.crm2.adapter.out.persistence.repository.TareaRepository;
import com.ar.crm2.application.tarea.port.out.DeleteTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.SaveTareaPort;
import com.ar.crm2.application.tarea.query.TareaFilterCriteria;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.vo.TareaId;
import lombok.RequiredArgsConstructor;

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
        return repository.findAll().stream()
            .map(TareaMapper::toDomain)
            .filter(tarea -> matches(tarea, criteria))
            .toList();
    }

    private boolean matches(Tarea tarea, TareaFilterCriteria criteria) {
        if (criteria == null) criteria = TareaFilterCriteria.empty();
        String term = normalize(criteria.search());
        if (!term.isEmpty() && !normalize(tarea.getTitulo()).contains(term)) return false;
        if (criteria.prioridad() != null && tarea.getPrioridad() != criteria.prioridad()) return false;
        if (criteria.responsableId() != null && !criteria.responsableId().equals(tarea.getResponsableId())) return false;
        if (criteria.tratoId() != null && !criteria.tratoId().equals(tarea.getTratoId())) return false;
        if (criteria.tipo() != null && tarea.getTipo() != criteria.tipo()) return false;
        return matchesVencimiento(tarea, criteria.vencimiento());
    }

    private boolean matchesVencimiento(Tarea tarea, TareaFilterCriteria.VencimientoFilter filter) {
        if (filter == null) return true;
        LocalDateTime now = LocalDateTime.now();
        if (filter == TareaFilterCriteria.VencimientoFilter.VENCIDAS) {
            return tarea.getFechaCompletada() == null && tarea.getFechaLimite().isBefore(now);
        }
        LocalDateTime limit = now.plusDays(7);
        return !tarea.getFechaLimite().isBefore(now) && !tarea.getFechaLimite().isAfter(limit);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) return "";
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
