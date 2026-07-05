package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.FichaEntity;
import com.ar.crm2.adapter.out.persistence.mapper.FichaMapper;
import com.ar.crm2.adapter.out.persistence.repository.FichaRepository;
import com.ar.crm2.application.ficha.port.out.DeleteFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.ExistsFichasByColumnaIdPort;
import com.ar.crm2.application.ficha.port.out.FindAllFichasPort;
import com.ar.crm2.application.ficha.port.out.FindFichaByIdPort;
import com.ar.crm2.application.ficha.port.out.SaveFichaPort;
import com.ar.crm2.application.ficha.query.FichaFilterCriteria;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.FichaId;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class FichaRepositoryAdapter
        implements SaveFichaPort, FindAllFichasPort, FindFichaByIdPort, DeleteFichaByIdPort,
                   ExistsFichasByColumnaIdPort {

    private final FichaRepository repository;

    @Override
    public Ficha save(Ficha ficha) {
        FichaEntity entity = FichaMapper.toEntity(ficha);
        FichaEntity saved = repository.save(entity);
        return FichaMapper.toDomain(saved);
    }

    @Override
    public List<Ficha> findAll() {
        return findAll(FichaFilterCriteria.empty());
    }

    @Override
    public List<Ficha> findAll(FichaFilterCriteria criteria) {
        return repository.findAll().stream()
            .map(FichaMapper::toDomain)
            .filter(ficha -> matches(ficha, criteria))
            .toList();
    }

    private boolean matches(Ficha ficha, FichaFilterCriteria criteria) {
        if (criteria == null) criteria = FichaFilterCriteria.empty();
        if (criteria.tipoFicha() != null && ficha.getTipoFicha() != criteria.tipoFicha()) return false;
        if (criteria.tratoId() != null && !criteria.tratoId().equals(ficha.getTratoId())) return false;
        if (criteria.tareaId() != null && !criteria.tareaId().equals(ficha.getTareaId())) return false;
        if (criteria.tratoIds() != null && !criteria.tratoIds().isEmpty() && !criteria.tratoIds().contains(ficha.getTratoId())) return false;
        if (criteria.tareaIds() != null && !criteria.tareaIds().isEmpty() && !criteria.tareaIds().contains(ficha.getTareaId())) return false;
        return true;
    }

    @Override
    public Optional<Ficha> findById(FichaId id) {
        return repository.findById(id.value().toString())
            .map(FichaMapper::toDomain);
    }

    @Override
    public void deleteById(FichaId id) {
        repository.deleteById(id.value().toString());
    }

    @Override
    public boolean existsFichasByColumnaId(ColumnaId id) {
        return repository.existsByColumnaId(id.value().toString());
    }
}
