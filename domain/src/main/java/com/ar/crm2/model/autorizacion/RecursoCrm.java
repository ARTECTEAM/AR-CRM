package com.ar.crm2.model.autorizacion;

/** Fixed catalog of CRM resources that can be granted independently. */
public enum RecursoCrm {
    TABLERO,
    COLUMNA,
    FICHA,
    TRATO,
    TAREA,
    CONTACTO,
    EMPRESA,
    ETIQUETA,
    AGENDA,
    ROL,
    USUARIO;

    /** Whether this resource has a concrete evaluator for the given non-shared boundary. */
    public boolean supportsScope(AlcanceCrm scope) {
        if (scope == null) {
            return false;
        }
        if (scope == AlcanceCrm.TODO_COMPARTIDO) {
            return true;
        }
        return switch (scope) {
            case TODO_COMPARTIDO -> true;
            case PROPIOS_O_ASIGNADOS -> switch (this) {
                case TRATO, TAREA, CONTACTO, EMPRESA, AGENDA -> true;
                case TABLERO, COLUMNA, FICHA, ETIQUETA, ROL, USUARIO -> false;
            };
            case TABLEROS_PERMITIDOS -> switch (this) {
                case TABLERO, COLUMNA, FICHA -> true;
                case TRATO, TAREA, CONTACTO, EMPRESA, ETIQUETA, AGENDA, ROL, USUARIO -> false;
            };
        };
    }
}
