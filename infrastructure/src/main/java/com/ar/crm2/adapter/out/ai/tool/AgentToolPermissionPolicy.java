package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.application.security.AuthorizationCapabilities;
import com.ar.crm2.application.security.ResourceCapabilities;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Static upper bound for model tool discovery; use cases remain authoritative at invocation time. */
final class AgentToolPermissionPolicy {
    private static final Permission COLUMN_READ = permission(RecursoCrm.COLUMNA, AccionCrm.LEER);
    private static final Permission DEAL_READ = permission(RecursoCrm.TRATO, AccionCrm.LEER);
    private static final Permission TASK_READ = permission(RecursoCrm.TAREA, AccionCrm.LEER);
    private static final Set<Permission> FICHA_PARENT_READ = Set.of(DEAL_READ, TASK_READ);
    private static final Map<String, Requirement> REQUIREMENTS = requirements();

    private AgentToolPermissionPolicy() {
    }

    static Set<String> toolNames() {
        return REQUIREMENTS.keySet();
    }

    static Set<String> allowedTools(AuthorizationCapabilities capabilities) {
        if (capabilities == null) {
            return Set.of();
        }
        return REQUIREMENTS.entrySet().stream()
                .filter(entry -> entry.getValue().isSatisfiedBy(capabilities))
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }

    static String describe(AuthorizationCapabilities capabilities) {
        if (capabilities == null || capabilities.resources().isEmpty()) {
            return "No CRM tool capabilities are available for this turn.";
        }
        List<String> lines = new ArrayList<>();
        for (RecursoCrm resource : RecursoCrm.values()) {
            ResourceCapabilities capability = capabilities.resources().get(resource);
            if (capability == null || capability.actions().isEmpty()) {
                continue;
            }
            List<String> details = new ArrayList<>();
            details.add("actions=" + capability.actions().stream()
                    .sorted()
                    .map(AgentToolPermissionPolicy::actionLabel)
                    .collect(Collectors.joining(",")));
            details.add("record_scope=" + capability.scope());
            details.add("readable_sensitive_groups=" + namesOrNone(capability.readableGroups()));
            details.add("writable_sensitive_groups=" + namesOrNone(capability.writableGroups()));
            lines.add(resource + " {" + String.join("; ", details) + "}");
        }
        if (lines.isEmpty()) {
            return "No CRM tool capabilities are available for this turn.";
        }
        return "Role-level capabilities are an upper bound, not a promise of access to any record. "
                + "Every tool call remains subject to authoritative record, relationship, and field checks; "
                + "sensitive groups apply only after those checks permit the record.\n"
                + String.join("\n", lines);
    }

    private static String actionLabel(AccionCrm action) {
        return switch (action) {
            case LEER -> "read";
            case CREAR -> "create";
            case ACTUALIZAR -> "update";
            case ELIMINAR -> "delete";
            case ADMINISTRAR -> "administer";
        };
    }

    private static String namesOrNone(Set<? extends Enum<?>> values) {
        return values.isEmpty()
                ? "none"
                : values.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    private static Map<String, Requirement> requirements() {
        Map<String, Requirement> map = new LinkedHashMap<>();

        add(map, "find_contacts", all(
                permission(RecursoCrm.CONTACTO, AccionCrm.LEER),
                permission(RecursoCrm.EMPRESA, AccionCrm.LEER)));
        add(map, "create_contact", all(permission(RecursoCrm.CONTACTO, AccionCrm.CREAR),
                permission(RecursoCrm.EMPRESA, AccionCrm.LEER)));
        add(map, "get_contact", all(permission(RecursoCrm.CONTACTO, AccionCrm.LEER),
                permission(RecursoCrm.EMPRESA, AccionCrm.LEER)));
        add(map, "edit_contact", all(permission(RecursoCrm.CONTACTO, AccionCrm.ACTUALIZAR),
                permission(RecursoCrm.EMPRESA, AccionCrm.LEER)));
        add(map, "change_contact_state", all(permission(RecursoCrm.CONTACTO, AccionCrm.ACTUALIZAR),
                permission(RecursoCrm.EMPRESA, AccionCrm.LEER)));
        add(map, "delete_contact", resource(RecursoCrm.CONTACTO, AccionCrm.ELIMINAR));

        add(map, "create_company", resource(RecursoCrm.EMPRESA, AccionCrm.CREAR));
        add(map, "list_companies", resource(RecursoCrm.EMPRESA, AccionCrm.LEER));
        add(map, "edit_company", resource(RecursoCrm.EMPRESA, AccionCrm.ACTUALIZAR));
        add(map, "change_company_state", resource(RecursoCrm.EMPRESA, AccionCrm.ACTUALIZAR));
        add(map, "delete_company", resource(RecursoCrm.EMPRESA, AccionCrm.ELIMINAR));

        add(map, "create_trato", all(permission(RecursoCrm.TRATO, AccionCrm.CREAR),
                permission(RecursoCrm.CONTACTO, AccionCrm.LEER),
                permission(RecursoCrm.FICHA, AccionCrm.CREAR), COLUMN_READ));
        add(map, "list_tratos", resource(RecursoCrm.TRATO, AccionCrm.LEER));
        add(map, "get_trato", all(DEAL_READ, permission(RecursoCrm.CONTACTO, AccionCrm.LEER)));
        add(map, "edit_trato", all(permission(RecursoCrm.TRATO, AccionCrm.ACTUALIZAR),
                permission(RecursoCrm.CONTACTO, AccionCrm.LEER)));
        add(map, "delete_trato", resource(RecursoCrm.TRATO, AccionCrm.ELIMINAR));

        add(map, "create_tarea", all(permission(RecursoCrm.TAREA, AccionCrm.CREAR), DEAL_READ,
                permission(RecursoCrm.FICHA, AccionCrm.CREAR), COLUMN_READ));
        add(map, "list_tareas", resource(RecursoCrm.TAREA, AccionCrm.LEER));
        add(map, "get_tarea", all(permission(RecursoCrm.TAREA, AccionCrm.LEER), DEAL_READ));
        add(map, "edit_tarea", all(permission(RecursoCrm.TAREA, AccionCrm.ACTUALIZAR), DEAL_READ));
        add(map, "delete_tarea", resource(RecursoCrm.TAREA, AccionCrm.ELIMINAR));

        add(map, "create_etiqueta", resource(RecursoCrm.ETIQUETA, AccionCrm.CREAR));
        add(map, "list_etiquetas", resource(RecursoCrm.ETIQUETA, AccionCrm.LEER));
        add(map, "get_etiqueta", resource(RecursoCrm.ETIQUETA, AccionCrm.LEER));
        add(map, "edit_etiqueta", resource(RecursoCrm.ETIQUETA, AccionCrm.ACTUALIZAR));
        add(map, "delete_etiqueta", resource(RecursoCrm.ETIQUETA, AccionCrm.ELIMINAR));

        add(map, "create_agenda", resource(RecursoCrm.AGENDA, AccionCrm.CREAR));
        add(map, "list_agendas", resource(RecursoCrm.AGENDA, AccionCrm.LEER));
        add(map, "get_agenda", resource(RecursoCrm.AGENDA, AccionCrm.LEER));
        add(map, "edit_agenda", resource(RecursoCrm.AGENDA, AccionCrm.ACTUALIZAR));
        add(map, "delete_agenda", resource(RecursoCrm.AGENDA, AccionCrm.ELIMINAR));

        add(map, "list_tableros", resource(RecursoCrm.TABLERO, AccionCrm.LEER));
        add(map, "get_tablero", resource(RecursoCrm.TABLERO, AccionCrm.LEER));
        add(map, "create_tablero", all(permission(RecursoCrm.TABLERO, AccionCrm.CREAR),
                permission(RecursoCrm.COLUMNA, AccionCrm.CREAR)));
        add(map, "edit_tablero", resource(RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR));
        add(map, "delete_tablero", resource(RecursoCrm.TABLERO, AccionCrm.ELIMINAR));
        add(map, "eliminar_columna_del_tablero", all(
                permission(RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR), COLUMN_READ));
        add(map, "assign_columna_to_tablero", all(
                permission(RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR), COLUMN_READ));
        add(map, "reorder_tablero_columns", all(
                permission(RecursoCrm.TABLERO, AccionCrm.ACTUALIZAR), COLUMN_READ));

        add(map, "list_columnas", resource(RecursoCrm.COLUMNA, AccionCrm.LEER));
        add(map, "get_columna", resource(RecursoCrm.COLUMNA, AccionCrm.LEER));
        add(map, "create_columna", resource(RecursoCrm.COLUMNA, AccionCrm.CREAR));
        add(map, "edit_columna", resource(RecursoCrm.COLUMNA, AccionCrm.ACTUALIZAR));
        add(map, "delete_columna", resource(RecursoCrm.COLUMNA, AccionCrm.ELIMINAR));

        add(map, "list_fichas", resource(RecursoCrm.FICHA, AccionCrm.LEER));
        add(map, "get_ficha", fichaRead());
        add(map, "create_ficha", allWithAny(Set.of(
                permission(RecursoCrm.FICHA, AccionCrm.CREAR), COLUMN_READ), FICHA_PARENT_READ));
        add(map, "edit_ficha", allWithAny(Set.of(
                permission(RecursoCrm.FICHA, AccionCrm.ACTUALIZAR), COLUMN_READ), FICHA_PARENT_READ));
        add(map, "delete_ficha", allWithAny(Set.of(
                permission(RecursoCrm.FICHA, AccionCrm.ELIMINAR), COLUMN_READ), FICHA_PARENT_READ));
        add(map, "move_ficha_to_columna", allWithAny(Set.of(
                permission(RecursoCrm.FICHA, AccionCrm.ACTUALIZAR), COLUMN_READ), FICHA_PARENT_READ));

        if (map.size() != 50) {
            throw new IllegalStateException("Every registered CRM tool must have exactly one permission mapping");
        }
        return Collections.unmodifiableMap(map);
    }

    private static Requirement fichaRead() {
        return allWithAny(Set.of(permission(RecursoCrm.FICHA, AccionCrm.LEER), COLUMN_READ), FICHA_PARENT_READ);
    }

    private static Permission permission(RecursoCrm resource, AccionCrm action) {
        return new Permission(resource, action);
    }

    private static Requirement resource(RecursoCrm resource, AccionCrm action) {
        return all(permission(resource, action));
    }

    private static Requirement all(Permission... permissions) {
        return new Requirement(Set.of(permissions), List.of());
    }

    @SafeVarargs
    private static Requirement allWithAny(Set<Permission> allOf, Set<Permission>... anyOf) {
        return new Requirement(allOf, List.of(anyOf));
    }

    private static void add(Map<String, Requirement> map, String toolName, Requirement requirement) {
        if (map.putIfAbsent(toolName, requirement) != null) {
            throw new IllegalStateException("Duplicate CRM tool permission mapping: " + toolName);
        }
    }

    private record Permission(RecursoCrm resource, AccionCrm action) {
        boolean permittedBy(AuthorizationCapabilities capabilities) {
            return capabilities.permits(resource, action);
        }
    }

    private record Requirement(Set<Permission> allOf, List<Set<Permission>> anyOf) {
        private Requirement {
            allOf = Set.copyOf(allOf);
            anyOf = anyOf.stream().map(Set::copyOf).toList();
        }

        boolean isSatisfiedBy(AuthorizationCapabilities capabilities) {
            return allOf.stream().allMatch(permission -> permission.permittedBy(capabilities))
                    && anyOf.stream().allMatch(group -> group.stream()
                    .anyMatch(permission -> permission.permittedBy(capabilities)));
        }
    }
}
