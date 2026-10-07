package com.ar.crm2.application.agenda.command;

import com.ar.crm2.model.enums.TipoAgenda;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EditAgendaCommand(
    UUID id,
    TipoAgenda tipo,
    String asunto,
    String descripcion,
    LocalDate fecha,
    LocalTime horaInicio,
    LocalTime horaFin,
    UUID tareaId,
    UUID tratoId,
    String ubicacion,
    String linkVideollamada,
    Boolean recordatorioHabilitado,
    Integer minutosAntes
) {

    public EditAgendaCommand {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("tipo is required");
        }
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("asunto is required");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("fecha is required");
        }
        if (horaInicio == null) {
            throw new IllegalArgumentException("horaInicio is required");
        }
    }
}
