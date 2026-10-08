package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.EtiquetaEntity;
import com.ar.crm2.adapter.out.persistence.mapper.EtiquetaMapper;
import com.ar.crm2.adapter.out.persistence.repository.EtiquetaRepository;
import com.ar.crm2.adapter.out.persistence.repository.FichaEtiquetaRepository;
import com.ar.crm2.application.etiqueta.port.out.CountFichaEtiquetasByEtiquetaIdPort;
import com.ar.crm2.application.etiqueta.port.out.DeleteEtiquetaByIdPort;
import com.ar.crm2.application.etiqueta.port.out.DeleteFichaEtiquetasByEtiquetaIdPort;
import com.ar.crm2.application.etiqueta.port.out.ExistsEtiquetaByNombreAndTipoPort;
import com.ar.crm2.application.etiqueta.port.out.FindAllEtiquetasPort;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetaByIdPort;
import com.ar.crm2.application.etiqueta.port.out.FindFichaIdsByEtiquetaIdPort;
import com.ar.crm2.application.etiqueta.port.out.FindEtiquetasByIdsPort;
import com.ar.crm2.application.etiqueta.port.out.SaveEtiquetaPort;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.vo.EtiquetaId;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Single adapter implementing every Etiqueta outbound port defined in the
 * application layer. Aggregates the catalog and the owned-relation
 * repositories so the cascade-delete flow can be wrapped in a single Spring
 * transaction at this boundary.
 *
 * <p>The application module is Spring-free by project rule, so the
 * transaction ownership cannot live on the use case class. The
 * cascade-delete methods are therefore annotated with
 * {@code @Transactional} at this adapter level. Read-only port methods
 * inherit the caller's transaction (or run without one).
 *
 * <p>This class is intentionally NOT annotated with {@code @Component}.
 * Wiring is explicit in {@code boot/.../WiringConfig#etiquetaRepositoryAdapter}
 * to match the project convention for every other outbound adapter
 * (e.g. {@code FichaRepositoryAdapter}, {@code TableroRepositoryAdapter}).
 */
@RequiredArgsConstructor
public class EtiquetaRepositoryAdapter implements
    SaveEtiquetaPort,
    FindEtiquetaByIdPort,
    FindAllEtiquetasPort,
    ExistsEtiquetaByNombreAndTipoPort,
    DeleteEtiquetaByIdPort,
    DeleteFichaEtiquetasByEtiquetaIdPort,
    CountFichaEtiquetasByEtiquetaIdPort,
    FindEtiquetasByIdsPort,
    FindFichaIdsByEtiquetaIdPort {

    private final EtiquetaRepository etiquetaRepository;
    private final FichaEtiquetaRepository fichaEtiquetaRepository;

    // ── SaveEtiquetaPort ──────────────────────────────────────────

    @Override
    public Etiqueta save(Etiqueta etiqueta) {
        EtiquetaEntity entity = EtiquetaMapper.toEntity(etiqueta);
        EtiquetaEntity saved;
        try {
            saved = etiquetaRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // The DB unique constraint uk_etiquetas_nombre_tipo is the final
            // safety net behind the application pre-check. Surface a clear
            // message to the caller rather than leaking the raw DB exception.
            throw new DataIntegrityViolationException(
                "Ya existe una etiqueta con el mismo nombre y tipo.", e);
        }
        return EtiquetaMapper.toDomain(saved);
    }

    @Override
    public Optional<Etiqueta> findById(EtiquetaId id) {
        return etiquetaRepository.findById(id.value().toString())
            .map(EtiquetaMapper::toDomain);
    }



    @Override
    public List<Etiqueta> findAll(Optional<TipoEtiqueta> tipoEtiqueta) {
        List<EtiquetaEntity> rows = tipoEtiqueta
            .map(etiquetaRepository::findByTipoEtiqueta)
            .orElseGet(etiquetaRepository::findAll);
        List<Etiqueta> result = new ArrayList<>(rows.size());
        for (EtiquetaEntity e : rows) {
            result.add(EtiquetaMapper.toDomain(e));
        }
        return result;
    }

    // ── ExistsEtiquetaByNombreAndTipoPort ─────────────────────────

    @Override
    public boolean exists(String nombre, TipoEtiqueta tipoEtiqueta, EtiquetaId excludeId) {
        String excludeIdStr = excludeId == null ? null : excludeId.value().toString();
        return etiquetaRepository
            .existsByNombreAndTipoEtiquetaAndIdNot(nombre, tipoEtiqueta, excludeIdStr);
    }

    // ── DeleteEtiquetaByIdPort (cascades relation rows in one tx) ─

    /**
     * Cascade-deletes the catalog Etiqueta row. The application service
     * already verified the row exists and that the caller confirmed the
     * delete when relations exist; the relation rows are removed first
     * so the FK from {@code fichas_etiquetas.etiqueta_id} does not block
     * the catalog row deletion.
     *
     * <p>Wrapped in a single Spring transaction so the bulk-delete of
     * relation rows and the catalog row delete are atomic.
     */
    @Override
    @Transactional
    public void deleteById(EtiquetaId id) {
        fichaEtiquetaRepository.deleteByEtiquetaId(id.value().toString());
        etiquetaRepository.deleteById(id.value().toString());
    }

    // ── DeleteFichaEtiquetasByEtiquetaIdPort ──────────────────────

    @Override
    @Transactional
    public void deleteByEtiquetaId(EtiquetaId id) {
        fichaEtiquetaRepository.deleteByEtiquetaId(id.value().toString());
    }

    // ── CountFichaEtiquetasByEtiquetaIdPort ───────────────────────

    @Override
    public long countByEtiquetaId(EtiquetaId id) {
        return fichaEtiquetaRepository.countByEtiquetaId(id.value().toString());
    }

    @Override
    public List<UUID> findFichaIdsByEtiquetaId(EtiquetaId id) {
        return fichaEtiquetaRepository.findFichaIdsByEtiquetaId(id.value().toString())
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    // ── FindEtiquetasByIdsPort ────────────────────────────────────

    @Override
    public List<Etiqueta> findByIds(List<EtiquetaId> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> idStrings = new ArrayList<>(ids.size());
        for (EtiquetaId id : ids) {
            idStrings.add(id.value().toString());
        }
        List<EtiquetaEntity> rows = etiquetaRepository.findAllById(idStrings);
        List<Etiqueta> result = new ArrayList<>(rows.size());
        for (EtiquetaEntity e : rows) {
            result.add(EtiquetaMapper.toDomain(e));
        }
        return result;
    }
}
