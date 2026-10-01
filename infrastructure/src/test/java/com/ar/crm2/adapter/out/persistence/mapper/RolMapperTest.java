package com.ar.crm2.adapter.out.persistence.mapper;

import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RolMapperTest {
    @Test
    void mapsFixedCatalogGrantsAcrossThePersistenceBoundary() {
        UUID boardId = UUID.randomUUID();
        Rol role = Rol.create("Sales", null, List.of(new PermisoRecurso(RecursoCrm.TABLERO,
                Set.of(AccionCrm.LEER), AlcanceCrm.TABLEROS_PERMITIDOS, Set.of(boardId), Set.of(), Set.of()),
                new PermisoRecurso(RecursoCrm.TRATO, Set.of(AccionCrm.LEER), AlcanceCrm.TODO_COMPARTIDO,
                        Set.of(), Set.of(GrupoCampoSensible.FINANCIERO), Set.of())));

        var persisted = RolMapper.toEntity(role);
        Rol restored = RolMapper.toDomain(persisted);

        assertThat(restored.getPermisos()).containsExactlyInAnyOrderElementsOf(role.getPermisos());
        var tratoPermission = restored.getPermisos().stream()
                .filter(permission -> permission.recurso() == RecursoCrm.TRATO)
                .findFirst()
                .orElseThrow();
        assertThat(tratoPermission.gruposLectura()).containsExactly(GrupoCampoSensible.FINANCIERO);
    }
}
