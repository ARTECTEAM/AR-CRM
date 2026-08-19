package com.ar.crm2.adapter.in.rest.dto.response;

import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoTrato;
import com.ar.crm2.model.enums.TipoContrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * REST response DTO for Trato.
 * Exposes all fields needed for front-end list/create views.
 */
public record TratoResponse(
    UUID id,
    UUID contactoId,
    UUID responsableId,
    String nombre,
    BigDecimal valorEstimado,
    Integer probabilidad,
    LocalDate fechaCierreEsperada,
    TipoContrato tipoContrato,
    EstadoTrato estado,
    LocalDateTime creadoEn,
    LocalDateTime actualizadoEn
) {
    /**
     * Maps a domain Trato to this response DTO.
     */
    public static TratoResponse fromDomain(Trato trato) {
        return new TratoResponse(
            trato.getId().value(),
            trato.getContactoId().value(),
            trato.getResponsableId().value(),
            trato.getNombre(),
            trato.getValorEstimado(),
            trato.getProbabilidad(),
            trato.getFechaCierreEsperada(),
            trato.getTipoContrato(),
            trato.getEstado(),
            trato.getCreadoEn(),
            trato.getActualizadoEn()
        );
    }
}
