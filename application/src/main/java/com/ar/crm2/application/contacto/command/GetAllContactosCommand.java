package com.ar.crm2.application.contacto.command;

import com.ar.crm2.application.contacto.exception.InvalidGetAllContactosCommandException;
import com.ar.crm2.application.shared.ApplicationAssert;
import com.ar.crm2.model.enums.EstadoRelacion;

import java.util.UUID;

/**
 * Command for retrieving Contactos with optional listing filters
 * scoped to a trusted actor.
 *
 * <p>The Command owns its structural input contract:
 * <ul>
 *   <li>{@code actorUsuarioId} is <strong>mandatory</strong>:
 *       identity is security scope, not a filter. A null actor is
 *       rejected during Command construction with
 *       {@link InvalidGetAllContactosCommandException#missingActor()}.
 *       The Command preserves the supplied {@link UUID} exactly so the
 *       Service can convert it to a Domain {@code UsuarioId} and the
 *       adapter can apply it as the scope predicate.</li>
 *   <li>Optional text fields ({@code search}, {@code estadoRelacion},
 *       {@code comoNosConocio}) collapse to {@code null} when blank or
 *       whitespace-only via
 *       {@link ApplicationAssert#optionalTrimmed(String)}.</li>
 *   <li>A present {@code estadoRelacion} must equal an
 *       {@link EstadoRelacion} {@code name()} exactly; any other value
 *       is rejected during Command construction with
 *       {@link InvalidGetAllContactosCommandException#invalidEstadoRelacion(String)}.
 *       The Service therefore receives only validated Strings (or null)
 *       and converts them to Domain enums via {@code valueOf}.</li>
 *   <li>A present {@code maxResults} must be a positive integer.</li>
 *   <li>{@code responsableId} is an OPTIONAL filter. It narrows the
 *       visible set inside the actor scope but never replaces the
 *       actor scope itself.</li>
 * </ul>
 */
public record GetAllContactosCommand(
        UUID actorUsuarioId,
        String search,
        String estadoRelacion,
        UUID empresaId,
        UUID responsableId,
        String comoNosConocio,
        Integer maxResults
) {

    public GetAllContactosCommand {
        if (actorUsuarioId == null) {
            throw InvalidGetAllContactosCommandException.missingActor();
        }
        search = ApplicationAssert.optionalTrimmed(search);
        estadoRelacion = validateEstadoRelacion(ApplicationAssert.optionalTrimmed(estadoRelacion));
        comoNosConocio = ApplicationAssert.optionalTrimmed(comoNosConocio);
        if (maxResults != null) {
            ApplicationAssert.positive(maxResults, "maxResults");
        }
    }

    /**
     * Structural exact-name membership validation: a present
     * {@code estadoRelacion} must equal one of the
     * {@link EstadoRelacion} {@code name()} values exactly. The Command
     * performs no Domain conversion; the Service does that after
     * receiving the already-validated String.
     */
    private static String validateEstadoRelacion(String value) {
        if (value == null) {
            return null;
        }
        for (EstadoRelacion estado : EstadoRelacion.values()) {
            if (estado.name().equals(value)) {
                return value;
            }
        }
        throw InvalidGetAllContactosCommandException.invalidEstadoRelacion(value);
    }
}
