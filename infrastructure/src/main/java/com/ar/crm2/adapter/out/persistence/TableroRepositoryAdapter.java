package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.ColumnaTableroEntity;
import com.ar.crm2.adapter.out.persistence.entity.TableroEntity;
import com.ar.crm2.adapter.out.persistence.mapper.ColumnaMapper;
import com.ar.crm2.adapter.out.persistence.mapper.TableroMapper;
import com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository;
import com.ar.crm2.adapter.out.persistence.repository.TableroRepository;
import com.ar.crm2.application.tablero.port.out.DeleteTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.ExistsColumnaEnTableroPort;
import com.ar.crm2.application.tablero.port.out.FindAllTablerosPort;
import com.ar.crm2.application.tablero.port.out.FindColumnaByIdPort;
import com.ar.crm2.application.tablero.port.out.FindInitialColumnPort;
import com.ar.crm2.application.tablero.port.out.FindTableroByIdPort;
import com.ar.crm2.application.tablero.port.out.SaveTableroPort;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TableroId;
import com.ar.crm2.model.enums.TipoTablero;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class TableroRepositoryAdapter implements SaveTableroPort, FindAllTablerosPort, FindTableroByIdPort, DeleteTableroByIdPort, ExistsColumnaEnTableroPort, FindColumnaByIdPort, FindInitialColumnPort {

    private final TableroRepository repository;
    private final ColumnaRepository columnaRepository;
    private final TableroMapper mapper;

    @Override
    public Tablero save(Tablero tablero) {
        Map<String, String> existingChildRowIds = loadExistingChildRowIds(tablero);
        TableroEntity entity = mapper.toEntity(tablero, existingChildRowIds);
        TableroEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    /**
     * Returns a map of {@code columnaId → persisted row id} for the
     * children that already exist on the given Tablero's row, or an
     * empty map when the Tablero is new (no row in the database) or
     * has no children.
     *
     * <p>This is required so that updating an existing Tablero (e.g.
     * POST /api/tableros/asignar-columna) reuses each child's persisted
     * primary key. Otherwise {@link TableroMapper#toEntity} would
     * regenerate a UUID per child on every save, and JPA would emit
     * INSERTs that collide with
     * {@code uk_columnas_tablero_tablero_columna}.
     */
    private Map<String, String> loadExistingChildRowIds(Tablero tablero) {
        Optional<TableroEntity> existing = repository.findById(tablero.getId().value().toString());
        if (existing.isEmpty() || existing.get().getColumnasTablero() == null
            || existing.get().getColumnasTablero().isEmpty()) {
            return Map.of();
        }
        Map<String, String> map = new HashMap<>(existing.get().getColumnasTablero().size());
        for (ColumnaTableroEntity child : existing.get().getColumnasTablero()) {
            if (child.getColumnaId() != null && child.getId() != null) {
                map.put(child.getColumnaId(), child.getId());
            }
        }
        return map;
    }

    @Override
    public List<Tablero> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public Optional<Tablero> findById(TableroId id) {
        return repository.findById(id.value().toString())
            .map(mapper::toDomain);
    }

    @Override
    public void deleteById(TableroId id) {
        repository.deleteById(id.value().toString());
    }

    @Override
    public boolean existsByTableroIdAndColumnaId(TableroId tableroId, ColumnaId columnaId) {
        return repository.existsByIdAndColumnasTableroColumnaId(
            tableroId.value().toString(),
            columnaId.value().toString()
        );
    }

    @Override
    public Optional<Columna> findById(ColumnaId id) {
        return columnaRepository.findById(id.value().toString())
            .map(ColumnaMapper::toDomain);
    }

    @Override
    public Optional<Columna> findInitialColumn(TipoTablero tipoTablero) {
        return repository.findFirstByTipoTablero(tipoTablero)
            .map(mapper::toDomain)
            .flatMap(tablero -> tablero.getColumnasTablero().stream()
                .findFirst()
                .flatMap(ct -> findById(ct.getColumnaId()))
            );
    }
}
