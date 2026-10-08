package com.ar.crm2.model.autorizacion;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Fixed starter profiles. Callers may edit the resulting grants immediately. */
public final class PlantillasPermisosCrm {
    private static final Set<RecursoCrm> WORK_RESOURCES = EnumSet.of(
            RecursoCrm.TABLERO, RecursoCrm.COLUMNA, RecursoCrm.FICHA, RecursoCrm.TRATO,
            RecursoCrm.TAREA, RecursoCrm.CONTACTO, RecursoCrm.EMPRESA, RecursoCrm.ETIQUETA,
            RecursoCrm.AGENDA);

    private PlantillasPermisosCrm() {
    }

    public static List<PermisoRecurso> grantsFor(PlantillaRol template) {
        if (template == null || template == PlantillaRol.SIN_ACCESO) {
            return List.of();
        }

        List<PermisoRecurso> grants = new ArrayList<>();
        for (RecursoCrm resource : RecursoCrm.values()) {
            boolean manageableResource = template == PlantillaRol.ADMINISTRADOR;
            boolean workResource = WORK_RESOURCES.contains(resource);
            Set<AccionCrm> actions;
            Set<GrupoCampoSensible> readable = Set.of();
            Set<GrupoCampoSensible> writable = Set.of();

            if (manageableResource) {
                actions = EnumSet.allOf(AccionCrm.class);
                readable = EnumSet.allOf(GrupoCampoSensible.class);
                writable = EnumSet.allOf(GrupoCampoSensible.class);
            } else if (workResource) {
                actions = template == PlantillaRol.SOLO_LECTURA
                        ? EnumSet.of(AccionCrm.LEER)
                        : EnumSet.of(AccionCrm.LEER, AccionCrm.CREAR, AccionCrm.ACTUALIZAR);
            } else {
                continue;
            }

            grants.add(new PermisoRecurso(resource, actions, AlcanceCrm.TODO_COMPARTIDO,
                    Set.of(), readable, writable));
        }
        return List.copyOf(grants);
    }
}
