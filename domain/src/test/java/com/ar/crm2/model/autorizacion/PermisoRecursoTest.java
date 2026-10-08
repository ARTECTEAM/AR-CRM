package com.ar.crm2.model.autorizacion;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermisoRecursoTest {

    @Test
    void noAccessIsTheDefaultAndPresetsAreEditableSeeds() {
        assertThat(PlantillasPermisosCrm.grantsFor(PlantillaRol.SIN_ACCESO)).isEmpty();

        List<PermisoRecurso> preset = PlantillasPermisosCrm.grantsFor(PlantillaRol.SOLO_LECTURA);
        assertThat(preset).isNotEmpty().allSatisfy(grant -> {
            assertThat(grant.permite(AccionCrm.LEER)).isTrue();
            assertThat(grant.permite(AccionCrm.ACTUALIZAR)).isFalse();
        });

        PermisoRecurso edited = new PermisoRecurso(RecursoCrm.TRATO,
                EnumSet.of(AccionCrm.LEER, AccionCrm.ACTUALIZAR), AlcanceCrm.TODO_COMPARTIDO,
                Set.of(), Set.of(GrupoCampoSensible.FINANCIERO), Set.of());
        assertThat(edited.permite(AccionCrm.ACTUALIZAR)).isTrue();
        assertThat(edited.gruposEscritura()).isEmpty();
    }

    @Test
    void writesRequireReadOfSameSensitiveGroupAndBoardScopeNeedsIds() {
        assertThatThrownBy(() -> new PermisoRecurso(RecursoCrm.TRATO,
                Set.of(AccionCrm.ACTUALIZAR), AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(),
                Set.of(GrupoCampoSensible.FINANCIERO)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PermisoRecurso(RecursoCrm.TABLERO,
                Set.of(AccionCrm.LEER), AlcanceCrm.TABLEROS_PERMITIDOS, Set.of(), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new PermisoRecurso(RecursoCrm.TABLERO,
                Set.of(AccionCrm.LEER), AlcanceCrm.TODO_COMPARTIDO, Set.of(UUID.randomUUID()), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ownerlessResourcesAndUnsupportedBoardResourcesRejectScopedGrants() {
        assertThatThrownBy(() -> new PermisoRecurso(RecursoCrm.ROL,
                Set.of(AccionCrm.ADMINISTRAR), AlcanceCrm.PROPIOS_O_ASIGNADOS,
                Set.of(), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PermisoRecurso(RecursoCrm.USUARIO,
                Set.of(AccionCrm.ADMINISTRAR), AlcanceCrm.TABLEROS_PERMITIDOS,
                Set.of(UUID.randomUUID()), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PermisoRecurso(RecursoCrm.ETIQUETA,
                Set.of(AccionCrm.ADMINISTRAR), AlcanceCrm.PROPIOS_O_ASIGNADOS,
                Set.of(), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PermisoRecurso(RecursoCrm.TAREA,
                Set.of(AccionCrm.LEER), AlcanceCrm.TABLEROS_PERMITIDOS,
                Set.of(UUID.randomUUID()), Set.of(), Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
