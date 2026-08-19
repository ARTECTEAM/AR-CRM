package com.ar.crm2.adapter.out.persistence.mapper;

import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;

/**
 * Mapper between persistence entity and domain entity.
 * Handles UUID/String conversion at the persistence boundary.
 */
public final class TratoMapper {

    private TratoMapper() {}

    /**
     * Maps a domain Trato to a persistence entity.
     * Used for save operations.
     */
    public static TratoEntity toEntity(Trato domain) {
        return TratoEntity.builder()
            .id(domain.getId().value().toString())
            .contactoId(domain.getContactoId().value().toString())
            .responsableId(domain.getResponsableId().value().toString())
            .nombre(domain.getNombre())
            .valorEstimado(domain.getValorEstimado())
            .probabilidad(domain.getProbabilidad())
            .fechaCierreEsperada(domain.getFechaCierreEsperada())
            .tipoContrato(domain.getTipoContrato())
            .estado(domain.getEstado())
            .creadoEn(domain.getCreadoEn())
            .actualizadoEn(domain.getActualizadoEn())
            .build();
    }

    /**
     * Maps a persistence entity to a domain Trato.
     * Used for find/load operations.
     */
    public static Trato toDomain(TratoEntity entity) {
        if (entity == null) return null;
        return Trato.reconstitute(
            TratoId.from(java.util.UUID.fromString(entity.getId())),
            ContactoId.from(java.util.UUID.fromString(entity.getContactoId())),
            UsuarioId.from(java.util.UUID.fromString(entity.getResponsableId())),
            entity.getNombre(),
            entity.getValorEstimado(),
            entity.getProbabilidad(),
            entity.getFechaCierreEsperada(),
            entity.getTipoContrato(),
            entity.getEstado(),
            entity.getCreadoEn(),
            entity.getActualizadoEn()
        );
    }
}
