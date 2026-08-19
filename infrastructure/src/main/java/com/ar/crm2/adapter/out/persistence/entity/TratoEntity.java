package com.ar.crm2.adapter.out.persistence.entity;

import com.ar.crm2.model.enums.EstadoTrato;
import com.ar.crm2.model.enums.TipoContrato;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
     * JPA entity for current tratos persistence.
 * Maps to the 'tratos' table with all documented columns.
 * Uses String id to match database convention (UUID stored as VARCHAR).
 */
@Entity
@Table(name = "tratos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TratoEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "contacto_id", nullable = false)
    private String contactoId;

    @Column(name = "responsable_id", nullable = false)
    private String responsableId;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "valor_estimado", precision = 15, scale = 2)
    private BigDecimal valorEstimado;

    @Column(name = "probabilidad")
    private Integer probabilidad;

    @Column(name = "fecha_cierre_esperada")
    private LocalDate fechaCierreEsperada;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contrato", length = 50)
    private TipoContrato tipoContrato;

    // Nullable a propósito: con ddl-auto=update, una columna NOT NULL nueva
    // rompería una tabla con datos. El dominio trata null como ABIERTO
    // (ver Trato.reconstitute).
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", length = 20)
    private EstadoTrato estado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;
}
