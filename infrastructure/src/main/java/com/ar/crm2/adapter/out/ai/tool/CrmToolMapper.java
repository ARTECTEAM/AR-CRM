package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.UpdateDealStageOutput;
import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.EstadoTrato;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure, deterministic mapper between the A3 annotation-driven tool
 * DTOs and the existing Application use-case commands/output shapes.
 *
 * <p>The mapper is the single point where:
 * <ul>
 *     <li>Raw tool parameter values plus the trusted actor map into
 *         the existing Application command objects
 *         ({@link GetAllContactosCommand} and
 *         {@link CreateContactoCommand}) and into the typed
 *         {@code CambiarEstadoTratoUseCase} argument tuple. The
 *         trusted actor/owner identity is threaded from the caller
 *         (the {@code ToolContext}-resolved server-side UUID) —
 *         never from the model.</li>
 *     <li>Domain {@link Contacto} / {@link Trato} entities become the
 *         bounded, safe {@link FindContactsOutput},
 *         {@link CreateContactOutput}, and
 *         {@link UpdateDealStageOutput} records. No SQL, credentials,
 *         stack traces, or internal handles ever reach the model.</li>
 * </ul>
 *
 * <p>The mapper also enforces the
 * {@code find_contacts = 20} hard cap and the {@code PERDIDO}
 * requires {@code motivo} rule at the trust boundary, before any
 * use case is invoked.
 */
public final class CrmToolMapper {

    /**
     * Hard cap applied to every {@code find_contacts} invocation.
     * The Application search contract supports a positive bound;
     * supplying this value overrides any upstream default and
     * guarantees the cap.
     */
    public static final int FIND_CONTACTS_MAX_RESULTS = 20;

    private CrmToolMapper() {
    }

    // ── raw values → command ────────────────────────────────────────

    /**
     * Maps raw {@code find_contacts} parameter values plus the trusted
     * actor into the existing {@link GetAllContactosCommand} with the
     * hard cap of 20 and trim-to-null normalization on string filters.
     */
    public static GetAllContactosCommand toGetAllContactosCommand(
            String search, String estadoRelacion, UUID empresaId, UUID responsableId,
            String comoNosConocio, UUID trustedActorUsuarioId) {
        if (trustedActorUsuarioId == null) {
            throw new IllegalArgumentException("trustedActorUsuarioId is required");
        }
        return new GetAllContactosCommand(
                trustedActorUsuarioId, trimToNull(search), trimToNull(estadoRelacion),
                empresaId, responsableId, trimToNull(comoNosConocio), FIND_CONTACTS_MAX_RESULTS);
    }

    /**
     * Maps raw {@code create_contact} parameter values plus the
     * trusted actor into the existing {@link CreateContactoCommand}.
     * Validates required fields and the {@link EstadoRelacion} name
     * before the use case runs.
     */
    public static CreateContactoCommand toCreateContactoCommand(
            UUID empresaId, String nombre, String correo, String estadoRelacion,
            UUID responsableId, String telefono, String cargo, String comoNosConocio,
            UUID trustedActorUsuarioId) {
        if (trustedActorUsuarioId == null) {
            throw new IllegalArgumentException("trustedActorUsuarioId is required");
        }
        if (empresaId == null) {
            throw new IllegalArgumentException("create_contact requires empresaId");
        }
        String trimmedNombre = requireNonBlank(nombre, "create_contact requires nombre");
        String trimmedEstadoRelacion = requireNonBlank(estadoRelacion, "create_contact requires estadoRelacion");
        EstadoRelacion estado;
        try {
            estado = EstadoRelacion.valueOf(trimmedEstadoRelacion);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "create_contact estadoRelacion must equal an EstadoRelacion name, was: "
                            + trimmedEstadoRelacion);
        }
        return new CreateContactoCommand(
                empresaId, trimmedNombre, trimToNull(correo), estado,
                responsableId, trustedActorUsuarioId,
                trimToNull(telefono), trimToNull(cargo), trimToNull(comoNosConocio));
    }

    /**
     * Maps raw {@code update_deal_stage} parameter values into the
     * typed tuple consumed by the existing
     * {@code CambiarEstadoTratoUseCase}. Validates deal id, status
     * membership (only {@code GANADO} or {@code PERDIDO}), and the
     * {@code motivo} requirement for {@code PERDIDO}.
     */
    public static UpdateDealStageArguments toUpdateDealStageArguments(
            UUID id, String status, String motivo) {
        if (id == null) {
            throw new IllegalArgumentException("update_deal_stage requires id");
        }
        String trimmedStatus = requireNonBlank(status, "update_deal_stage requires status");
        if (!"GANADO".equals(trimmedStatus) && !"PERDIDO".equals(trimmedStatus)) {
            throw new IllegalArgumentException(
                    "update_deal_stage status must be GANADO or PERDIDO, was: " + trimmedStatus);
        }
        String trimmedMotivo = trimToNull(motivo);
        if ("PERDIDO".equals(trimmedStatus) && trimmedMotivo == null) {
            throw new IllegalArgumentException(
                    "update_deal_stage requires motivo when status is PERDIDO");
        }
        return new UpdateDealStageArguments(id, trimmedStatus, trimmedMotivo);
    }

    // ── entity → output ─────────────────────────────────────────────

    /**
     * Projects the {@code find_contacts} domain result to the bounded
     * model-visible output. Null or empty inputs map to an empty
     * {@link FindContactsOutput}; internal fields are stripped.
     */
    public static FindContactsOutput toFindContactsOutput(List<Contacto> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return new FindContactsOutput(List.of());
        }
        List<FindContactsOutput.ContactSummary> summaries = new ArrayList<>(contacts.size());
        for (Contacto contact : contacts) {
            if (contact == null) {
                continue;
            }
            summaries.add(new FindContactsOutput.ContactSummary(
                    String.valueOf(contact.getId().value()),
                    contact.getNombre(),
                    contact.getEstadoRelacion() == null ? null : contact.getEstadoRelacion().name(),
                    contact.getCorreo()
            ));
        }
        return new FindContactsOutput(List.copyOf(summaries));
    }

    /**
     * Projects the {@code create_contact} domain entity to the
     * bounded model-visible output. Internal fields are stripped.
     */
    public static CreateContactOutput toCreateContactOutput(Contacto contact) {
        if (contact == null) {
            return new CreateContactOutput(null, null, null, null);
        }
        return new CreateContactOutput(
                String.valueOf(contact.getId().value()),
                contact.getNombre(),
                contact.getEstadoRelacion() == null ? null : contact.getEstadoRelacion().name(),
                contact.getCorreo()
        );
    }

    /**
     * Projects the {@code update_deal_stage} domain entity to the
     * bounded model-visible output. Internal fields are stripped.
     */
    public static UpdateDealStageOutput toUpdateDealStageOutput(Trato trato) {
        if (trato == null) {
            return new UpdateDealStageOutput(null, null);
        }
        EstadoTrato estado = trato.getEstado();
        return new UpdateDealStageOutput(
                String.valueOf(trato.getId().value()),
                estado == null ? null : estado.name()
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireNonBlank(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    /**
     * Argument tuple for the existing {@code CambiarEstadoTratoUseCase}.
     * The mapper produces this struct so the {@code SpringAiCrmTools}
     * annotations can map directly without relying on positional
     * arguments at the use case boundary.
     */
    public record UpdateDealStageArguments(UUID tratoId, String status, String motivo) {
    }
}