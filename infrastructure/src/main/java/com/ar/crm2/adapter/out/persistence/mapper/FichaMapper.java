package com.ar.crm2.adapter.out.persistence.mapper;

import com.ar.crm2.adapter.out.persistence.entity.FichaEntity;
import com.ar.crm2.adapter.out.persistence.entity.FichaEtiquetaEntity;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.entity.FichaEtiqueta;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.EtiquetaId;
import com.ar.crm2.model.vo.FichaId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.TareaId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mapper between persistence entity and domain entity for Ficha.
 * Handles UUID/String conversion and the etiqueta child collection at the
 * persistence boundary.
 *
 * <p><b>Child row id strategy</b>: each {@code FichaEtiquetaEntity} row
 * generated here owns its own fresh UUID technical id. The catalog
 * {@code etiquetaId} is stored in its own {@code etiqueta_id} column so the
 * row can still reference the Etiqueta catalog. The row id MUST NOT be a
 * copy of the catalog id — it is a technical id, not a domain identity.
 * This contract is enforced by
 * {@code FichaMapperTest#toEntity_childRowId_isGeneratedUuidNotEqualToEtiquetaId}
 * and is the same strategy used by {@code TableroMapper#toEntity} for
 * {@code ColumnaTableroEntity}.
 *
 * <p>For existing Fichas, callers may pass a map of
 * {@code etiquetaId → existing relation row id} so unchanged etiqueta
 * relations keep their persisted primary key during updates. Without that,
 * JPA may treat unchanged relation rows as new inserts and collide with
 * {@code uk_fichas_etiquetas_ficha_etiqueta}.
 */
public final class FichaMapper {

    private FichaMapper() {}

    public static FichaEntity toEntity(Ficha domain) {
        return toEntity(domain, java.util.Collections.emptyMap());
    }

    public static FichaEntity toEntity(Ficha domain, Map<String, String> existingChildRowIds) {
        if (domain == null) {
            return null;
        }
        Map<String, String> safeExisting = existingChildRowIds != null
            ? existingChildRowIds
            : java.util.Collections.emptyMap();

        FichaEntity entity = FichaEntity.builder()
            .id(domain.getId().value().toString())
            .columnaId(domain.getColumnaId().value().toString())
            .tipoFicha(domain.getTipoFicha())
            .tratoId(domain.getTratoId() != null ? domain.getTratoId().value().toString() : null)
            .tareaId(domain.getTareaId() != null ? domain.getTareaId().value().toString() : null)
            .actualizadoEn(domain.getActualizadoEn())
            .etiquetas(new ArrayList<>())
            .build();

        for (FichaEtiqueta fe : domain.getEtiquetas()) {
            String etiquetaId = fe.getEtiquetaId().value().toString();
            FichaEtiquetaEntity child = FichaEtiquetaEntity.builder()
                .id(safeExisting.getOrDefault(etiquetaId, UUID.randomUUID().toString()))
                .ficha(entity)
                .etiquetaId(etiquetaId)
                .tipoEtiqueta(fe.getTipoEtiqueta())
                .build();
            entity.getEtiquetas().add(child);
        }
        return entity;
    }

    public static Ficha toDomain(FichaEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Ficha id must not be null");
        }
        if (entity.getColumnaId() == null) {
            throw new IllegalArgumentException("Ficha columnaId must not be null");
        }
        if (entity.getActualizadoEn() == null) {
            throw new IllegalArgumentException("Ficha actualizadoEn must not be null");
        }

        List<FichaEtiqueta> etiquetas = new ArrayList<>();
        if (entity.getEtiquetas() != null) {
            for (FichaEtiquetaEntity child : entity.getEtiquetas()) {
                if (child.getEtiquetaId() == null || child.getTipoEtiqueta() == null) {
                    throw new IllegalArgumentException(
                        "FichaEtiqueta child row is missing required fields");
                }
                etiquetas.add(FichaEtiqueta.reconstitute(
                    EtiquetaId.from(UUID.fromString(child.getEtiquetaId())),
                    child.getTipoEtiqueta()
                ));
            }
        }

        return Ficha.reconstitute(
            FichaId.from(UUID.fromString(entity.getId())),
            ColumnaId.from(UUID.fromString(entity.getColumnaId())),
            entity.getTipoFicha(),
            entity.getTratoId() != null ? TratoId.from(UUID.fromString(entity.getTratoId())) : null,
            entity.getTareaId() != null ? TareaId.from(UUID.fromString(entity.getTareaId())) : null,
            entity.getActualizadoEn(),
            etiquetas
        );
    }
}
