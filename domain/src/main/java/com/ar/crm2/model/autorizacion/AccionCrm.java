package com.ar.crm2.model.autorizacion;

/** Actions understood by the CRM authorization policy. */
public enum AccionCrm {
    LEER,
    CREAR,
    ACTUALIZAR,
    ELIMINAR,
    /** Full control of one resource, including its grants. */
    ADMINISTRAR
}
