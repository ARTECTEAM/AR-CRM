package com.ar.crm2.model.entity;

import com.ar.crm2.model.enums.EstadoTrato;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
import com.ar.crm2.shared.DomainAssert;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Rich domain entity for Trato (deal/opportunity).
 *
 * Identity: TratoId (wraps UUID).
 * Equality: by id only (not full attribute equality).
 * No public setters — state changes go through business methods that preserve invariants.
 * Constructor is private; use static factory methods create() and reconstitute().
 */
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Trato {

    @EqualsAndHashCode.Include
    private final TratoId id;

    private final ContactoId contactoId;
    private final UsuarioId responsableId;
    private final String nombre;
    private final BigDecimal valorEstimado;
    private final Integer probabilidad;
    private final LocalDate fechaCierreEsperada;
    private final TipoContrato tipoContrato;
    private final EstadoTrato estado;
    private final LocalDateTime creadoEn;
    private final LocalDateTime actualizadoEn;

    // ── Factory ──────────────────────────────────────────────────

    /**
     * Creates a new Trato.
     * Generates id and timestamps internally.
     * Non-null fields are validated: nombre is mandatory.
     */
    public static Trato create(
        ContactoId contactoId,
        UsuarioId responsableId,
        String nombre,
        BigDecimal valorEstimado,
        Integer probabilidad,
        LocalDate fechaCierreEsperada,
        TipoContrato tipoContrato
    ) {
        DomainAssert.notNull(contactoId, "contactoId");
        DomainAssert.notNull(responsableId, "responsableId");
        DomainAssert.lengthBetween(nombre, "nombre", 1, 200);

        return new Trato(
            TratoId.create(),
            contactoId,
            responsableId,
            nombre.trim(),
            valorEstimado,
            probabilidad,
            fechaCierreEsperada,
            tipoContrato,
            EstadoTrato.ABIERTO,
            LocalDateTime.now(),
            null
        );
    }

    /**
     * Reconstitutes an existing Trato from persistence.
     */
    public static Trato reconstitute(
        TratoId id,
        ContactoId contactoId,
        UsuarioId responsableId,
        String nombre,
        BigDecimal valorEstimado,
        Integer probabilidad,
        LocalDate fechaCierreEsperada,
        TipoContrato tipoContrato,
        EstadoTrato estado,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
    ) {
        DomainAssert.notNull(id, "id");
        DomainAssert.notNull(contactoId, "contactoId");
        DomainAssert.notNull(responsableId, "responsableId");
        DomainAssert.notNull(creadoEn, "creadoEn");
        DomainAssert.lengthBetween(nombre, "nombre", 1, 200);

        return new Trato(
            id,
            contactoId,
            responsableId,
            nombre.trim(),
            valorEstimado,
            probabilidad,
            fechaCierreEsperada,
            tipoContrato,
            estado != null ? estado : EstadoTrato.ABIERTO,
            creadoEn,
            actualizadoEn
        );
    }
}
