package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.TareaEntity;
import com.ar.crm2.adapter.out.persistence.mapper.TareaMapper;
import com.ar.crm2.adapter.out.persistence.repository.TareaRepository;
import com.ar.crm2.application.tarea.port.out.DeleteTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.FindAllTareasPort;
import com.ar.crm2.application.tarea.port.out.FindTareaByIdPort;
import com.ar.crm2.application.tarea.port.out.SaveTareaPort;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.vo.TareaId;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

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
        return repository.findAll().stream()
            .map(TareaMapper::toDomain)
            .toList();
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
