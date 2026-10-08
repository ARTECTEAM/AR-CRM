package com.ar.crm2.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Storage form for a bounded role/resource policy row. Enum names are validated on load. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPermisoEmbeddable {
    @Column(name = "acciones", nullable = false, length = 255)
    private String acciones;

    @Column(name = "alcance", nullable = false, length = 40)
    private String alcance;

    @Column(name = "ids_permitidos", columnDefinition = "TEXT")
    private String idsPermitidos;

    @Column(name = "grupos_lectura", nullable = false, length = 100)
    private String gruposLectura;

    @Column(name = "grupos_escritura", nullable = false, length = 100)
    private String gruposEscritura;
}
