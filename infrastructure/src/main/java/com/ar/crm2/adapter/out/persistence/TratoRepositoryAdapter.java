package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import com.ar.crm2.adapter.out.persistence.mapper.TratoMapper;
import com.ar.crm2.adapter.out.persistence.repository.TratoRepository;
import com.ar.crm2.application.trato.port.out.DeleteTratoByIdPort;
import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.TratoId;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

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
        return repository.findAll().stream()
            .map(TratoMapper::toDomain)
            .toList();
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
