package com.ar.crm2.adapter.in.rest.projection;

import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared typed projection for REST and Spring AI outputs; denied values are nulled, never zero-filled. */
public final class CrmSensitiveFieldProjector {
    private CrmSensitiveFieldProjector() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T project(T output, RecursoCrm resource, ResourceReadPolicy policy) {
        if (output == null || resource == null || policy == null) return output;
        return (T) projectValue(output, resource, policy.readableGroups());
    }

    private static Object projectValue(Object value, RecursoCrm resource, Set<GrupoCampoSensible> readableGroups) {
        if (value == null) return null;
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> projected = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                String field = key == null ? "" : key.toString();
                projected.put(key, shouldMask(resource, field, readableGroups)
                        ? null : projectValue(item, resource, readableGroups));
            });
            return projected;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> projected = new ArrayList<>();
            iterable.forEach(item -> projected.add(projectValue(item, resource, readableGroups)));
            return List.copyOf(projected);
        }
        if (!value.getClass().isRecord()) return value;

        RecordComponent[] components = value.getClass().getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Object[] arguments = new Object[components.length];
        try {
            for (int i = 0; i < components.length; i++) {
                RecordComponent component = components[i];
                parameterTypes[i] = component.getType();
                Object fieldValue = component.getAccessor().invoke(value);
                arguments[i] = shouldMask(resource, component.getName(), readableGroups)
                        ? null : projectValue(fieldValue, resource, readableGroups);
            }
            Constructor<?> constructor = value.getClass().getDeclaredConstructor(parameterTypes);
            if (!constructor.canAccess(null)) constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            throw new IllegalStateException("Could not safely project " + value.getClass().getName(), failure);
        }
    }

    private static boolean shouldMask(RecursoCrm resource, String field, Set<GrupoCampoSensible> readableGroups) {
        if (field.equals("keycloakId") || field.equals("password") || field.equals("passwordHash")) {
            return true;
        }
        GrupoCampoSensible group = groupFor(resource, field);
        return group != null && !readableGroups.contains(group);
    }

    private static GrupoCampoSensible groupFor(RecursoCrm resource, String field) {
        return switch (resource) {
            case TRATO -> Set.of("valorEstimado", "probabilidad", "fechaCierreEsperada").contains(field)
                    ? GrupoCampoSensible.FINANCIERO : null;
            case TABLERO -> field.equals("totalValorEstimado") ? GrupoCampoSensible.FINANCIERO : null;
            case CONTACTO -> Set.of("correo", "telefono").contains(field)
                    ? GrupoCampoSensible.CONTACTO_PRIVADO : null;
            case EMPRESA -> Set.of("telefono", "notas").contains(field)
                    ? GrupoCampoSensible.CONTACTO_PRIVADO : null;
            case USUARIO -> field.equals("correo") ? GrupoCampoSensible.CONTACTO_PRIVADO : null;
            default -> null;
        };
    }
}
