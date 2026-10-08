package com.ar.crm2.adapter.out.persistence.mapper;

import com.ar.crm2.adapter.out.persistence.entity.RolEntity;
import com.ar.crm2.adapter.out.persistence.entity.RolPermisoEmbeddable;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.PermisoRecurso;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Rol;
import com.ar.crm2.model.vo.RolId;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper between persistence entity and domain entity.
 * Handles UUID/String conversion at the persistence boundary.
 */
public final class RolMapper {

    private RolMapper() {}

    /**
     * Maps a domain Rol to a persistence entity.
     * Used for save operations.
     */
    public static RolEntity toEntity(Rol domain) {
        if (domain == null) {
            return null;
        }
        return RolEntity.builder()
            .id(domain.getId().value().toString())
            .nombre(domain.getNombre())
            .descripcion(domain.getDescripcion())
            .activo(domain.isActivo())
            .permisos(domain.getPermisos().stream().collect(Collectors.toMap(
                    permiso -> permiso.recurso().name(), RolMapper::toEmbeddable)))
            .build();
    }

    /**
     * Maps a persistence entity to a domain Rol.
     * Used for find/load operations.
     */
    public static Rol toDomain(RolEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getId() == null) {
            throw new IllegalArgumentException("Rol id must not be null");
        }
        List<PermisoRecurso> permissions = entity.getPermisos() == null
                ? List.of()
                : entity.getPermisos().entrySet().stream()
                    .map(entry -> toDomain(entry.getKey(), entry.getValue()))
                    .toList();
        return Rol.reconstitute(
            RolId.from(java.util.UUID.fromString(entity.getId())),
            entity.getNombre(),
            entity.getDescripcion(),
            entity.isActivo(),
            permissions
        );
    }

    private static RolPermisoEmbeddable toEmbeddable(PermisoRecurso grant) {
        return new RolPermisoEmbeddable(
                names(grant.acciones()), grant.alcance().name(),
                grant.idsPermitidos().stream().map(UUID::toString).sorted().collect(Collectors.joining(",")),
                names(grant.gruposLectura()), names(grant.gruposEscritura()));
    }

    private static PermisoRecurso toDomain(String resourceName, RolPermisoEmbeddable stored) {
        return new PermisoRecurso(RecursoCrm.valueOf(resourceName),
                parseEnums(stored.getAcciones(), AccionCrm.class),
                AlcanceCrm.valueOf(stored.getAlcance()),
                parseIds(stored.getIdsPermitidos()),
                parseEnums(stored.getGruposLectura(), GrupoCampoSensible.class),
                parseEnums(stored.getGruposEscritura(), GrupoCampoSensible.class));
    }

    private static String names(Set<? extends Enum<?>> values) {
        return values.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private static <E extends Enum<E>> Set<E> parseEnums(String serialized, Class<E> type) {
        if (serialized == null || serialized.isBlank()) return Set.of();
        return Arrays.stream(serialized.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> Enum.valueOf(type, value))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<UUID> parseIds(String serialized) {
        if (serialized == null || serialized.isBlank()) return Set.of();
        return Arrays.stream(serialized.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
    }
}
