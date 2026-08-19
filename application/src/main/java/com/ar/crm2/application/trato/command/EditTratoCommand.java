package com.ar.crm2.application.trato.command;

import com.ar.crm2.model.enums.TipoContrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Command to edit an existing Trato.
 * Validates id and nombre at construction time.
 * Does NOT include non-editable deal state or audit timestamps; those are preserved from the existing entity.
 */
public record EditTratoCommand(
    UUID id,
    UUID responsableId,
    String nombre,
    BigDecimal valorEstimado,
    Integer probabilidad,
    LocalDate fechaCierreEsperada,
    TipoContrato tipoContrato
) {

    public EditTratoCommand {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre is required");
        }
    }
}
