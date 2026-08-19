package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditTratoOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.command.EditContactoCommand;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.empresa.command.CreateEmpresaCommand;
import com.ar.crm2.application.empresa.command.EditEmpresaCommand;
import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.TipoContrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure, deterministic mapper between the Spring AI 2.0 tool
 * DTOs and the existing Application use-case commands/output shapes.
 *
 * <p>The mapper is the single point where:
 * <ul>
 *     <li>Raw tool parameter values plus the trusted actor map into
 *         the existing Application command objects
 *         ({@link GetAllContactosCommand},
 *         {@link CreateContactoCommand},
 *         {@link EditContactoCommand},
 *         {@link CreateEmpresaCommand},
 *         {@link EditEmpresaCommand}, and the canonical
 *         {@link EditTratoCommand}). The trusted actor identity is
 *         threaded from the caller (the
 *         {@code ToolContext}-resolved server-side UUID) — never from
 *         the model.</li>
 *     <li>Domain {@link Contacto} / {@link Empresa} / {@link Trato}
 *         entities become the bounded, safe {@link FindContactsOutput},
 *         {@link CreateContactOutput}, {@link EditContactOutput},
 *         {@link CreateCompanyOutput}, {@link EditCompanyOutput},
 *         and {@link EditTratoOutput} records. No SQL, credentials,
 *         stack traces, or internal handles ever reach the model.</li>
 * </ul>
 *
 * <p>The mapper enforces the {@code find_contacts = 20} hard cap and
 * every other tool's required-field rules at the trust boundary,
 * before any use case is invoked.
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
     * Maps raw {@code edit_contact} parameter values into the
     * canonical {@link EditContactoCommand} consumed by
     * {@code EditContactoUseCase}. Validates required fields and
     * normalizes optional strings and the {@link EstadoRelacion}
     * name. {@code creadoPor} is intentionally absent from this tool
     * — the canonical use case preserves the original creator.
     *
     * <p>The {@code estadoRelacion} argument is required and
     * non-blank, mirroring the REST {@code EditContactoRequest} bean
     * validation and the Domain {@code Contacto.reconstitute} null
     * assertion. Surfacing the requirement at the trust boundary
     * fails closed before the use case can throw a domain-level
     * exception.
     */
    public static EditContactoCommand toEditContactoCommand(
            UUID id, String nombre, String correo, String estadoRelacion,
            UUID responsableId, String telefono, String cargo, String comoNosConocio) {
        if (id == null) {
            throw new IllegalArgumentException("edit_contact requires id");
        }
        String trimmedNombre = requireNonBlank(nombre, "edit_contact requires nombre");
        String trimmedEstado = requireNonBlank(estadoRelacion, "edit_contact requires estadoRelacion");
        EstadoRelacion parsedEstado;
        try {
            parsedEstado = EstadoRelacion.valueOf(trimmedEstado);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "edit_contact estadoRelacion must equal an EstadoRelacion name, was: "
                            + trimmedEstado);
        }
        return new EditContactoCommand(
                id, trimmedNombre, trimToNull(correo), parsedEstado,
                responsableId, trimToNull(telefono), trimToNull(cargo), trimToNull(comoNosConocio));
    }

    /**
     * Maps raw {@code edit_trato} parameter values into the canonical
     * {@link EditTratoCommand} consumed by {@code EditTratoUseCase}.
     * Validates the {@code id} / {@code responsableId} / {@code nombre}
     * triple required by the contract and normalizes optional strings
     * and the {@link TipoContrato} name. The deal's {@code estado} is
     * intentionally absent from this tool — the canonical use case
     * preserves it.
     */
    public static EditTratoCommand toEditTratoCommand(
            UUID id, UUID responsableId, String nombre,
            BigDecimal valorEstimado, Integer probabilidad,
            LocalDate fechaCierreEsperada, String tipoContrato) {
        if (id == null) {
            throw new IllegalArgumentException("edit_trato requires id");
        }
        if (responsableId == null) {
            throw new IllegalArgumentException("edit_trato requires responsableId");
        }
        String trimmedNombre = requireNonBlank(nombre, "edit_trato requires nombre");
        TipoContrato parsedTipoContrato = parseTipoContrato(tipoContrato);
        return new EditTratoCommand(
                id, responsableId, trimmedNombre,
                valorEstimado, probabilidad, fechaCierreEsperada, parsedTipoContrato);
    }

    /**
     * Maps raw {@code create_company} parameter values plus the
     * trusted actor into the existing {@link CreateEmpresaCommand}.
     * Validates required fields and the {@link EstadoRelacion} name
     * before the use case runs. The trusted actor becomes
     * {@code creadoPor} on the canonical command.
     */
    public static CreateEmpresaCommand toCreateEmpresaCommand(
            String nombre, String sector, String telefono, String paginaWeb,
            String facebook, String instagram, String twitter,
            String estadoRelacion, UUID responsableId, String notas,
            UUID trustedActorUsuarioId) {
        if (trustedActorUsuarioId == null) {
            throw new IllegalArgumentException("trustedActorUsuarioId is required");
        }
        String trimmedNombre = requireNonBlank(nombre, "create_company requires nombre");
        EstadoRelacion parsedEstado = parseEstadoRelacion(estadoRelacion, "create_company");
        return new CreateEmpresaCommand(
                trimmedNombre, trimToNull(sector), trimToNull(telefono), trimToNull(paginaWeb),
                trimToNull(facebook), trimToNull(instagram), trimToNull(twitter),
                parsedEstado, responsableId, trustedActorUsuarioId, trimToNull(notas));
    }

    /**
     * Maps raw {@code edit_company} parameter values into the
     * canonical {@link EditEmpresaCommand} consumed by
     * {@code EditEmpresaUseCase}. Validates required fields and
     * normalizes optional strings and the {@link EstadoRelacion}
     * name. {@code creadoPor} is intentionally absent from this tool
     * — the canonical use case preserves the original creator.
     */
    public static EditEmpresaCommand toEditEmpresaCommand(
            UUID id, String nombre, String sector, String telefono, String paginaWeb,
            String facebook, String instagram, String twitter,
            String estadoRelacion, UUID responsableId, String notas) {
        if (id == null) {
            throw new IllegalArgumentException("edit_company requires id");
        }
        String trimmedNombre = requireNonBlank(nombre, "edit_company requires nombre");
        EstadoRelacion parsedEstado = parseEstadoRelacion(estadoRelacion, "edit_company");
        return new EditEmpresaCommand(
                id, trimmedNombre, trimToNull(sector), trimToNull(telefono), trimToNull(paginaWeb),
                trimToNull(facebook), trimToNull(instagram), trimToNull(twitter),
                parsedEstado, responsableId, trimToNull(notas));
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
     * Projects the {@code edit_contact} domain entity to the bounded
     * model-visible output. Internal fields are stripped.
     */
    public static EditContactOutput toEditContactOutput(Contacto contact) {
        if (contact == null) {
            return new EditContactOutput(null, null, null, null, null, null, null, null);
        }
        return new EditContactOutput(
                String.valueOf(contact.getId().value()),
                contact.getNombre(),
                contact.getCorreo(),
                contact.getEstadoRelacion() == null ? null : contact.getEstadoRelacion().name(),
                contact.getResponsableId() == null ? null : String.valueOf(contact.getResponsableId().value()),
                contact.getTelefono(),
                contact.getCargo(),
                contact.getComoNosConocio()
        );
    }

    /**
     * Projects the {@code edit_trato} domain entity to the bounded
     * model-visible output. The deal's {@code estado} is intentionally
     * omitted because the canonical edit use case preserves it —
     * surfacing it in the tool output would falsely imply the tool
     * changed it.
     */
    public static EditTratoOutput toEditTratoOutput(Trato trato) {
        if (trato == null) {
            return new EditTratoOutput(null, null, null, null, null, null, null);
        }
        LocalDate fecha = trato.getFechaCierreEsperada();
        return new EditTratoOutput(
                String.valueOf(trato.getId().value()),
                trato.getNombre(),
                trato.getResponsableId() == null ? null : String.valueOf(trato.getResponsableId().value()),
                trato.getValorEstimado(),
                trato.getProbabilidad(),
                fecha == null ? null : fecha.format(DateTimeFormatter.ISO_LOCAL_DATE),
                trato.getTipoContrato() == null ? null : trato.getTipoContrato().name()
        );
    }

    /**
     * Projects the {@code create_company} domain entity to the bounded
     * model-visible output. Internal fields are stripped.
     */
    public static CreateCompanyOutput toCreateCompanyOutput(Empresa company) {
        if (company == null) {
            return new CreateCompanyOutput(null, null, null, null, null);
        }
        return new CreateCompanyOutput(
                String.valueOf(company.getId().value()),
                company.getNombre(),
                company.getSector(),
                company.getEstadoRelacion() == null ? null : company.getEstadoRelacion().name(),
                company.getResponsableId() == null ? null : String.valueOf(company.getResponsableId().value())
        );
    }

    /**
     * Projects the {@code edit_company} domain entity to the bounded
     * model-visible output. Internal fields are stripped; social
     * handles are intentionally omitted from this summary.
     */
    public static EditCompanyOutput toEditCompanyOutput(Empresa company) {
        if (company == null) {
            return new EditCompanyOutput(null, null, null, null, null, null, null, null);
        }
        return new EditCompanyOutput(
                String.valueOf(company.getId().value()),
                company.getNombre(),
                company.getSector(),
                company.getEstadoRelacion() == null ? null : company.getEstadoRelacion().name(),
                company.getResponsableId() == null ? null : String.valueOf(company.getResponsableId().value()),
                company.getPaginaWeb(),
                company.getTelefono(),
                company.getNotas()
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

    private static EstadoRelacion parseEstadoRelacion(String value, String toolName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return EstadoRelacion.valueOf(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    toolName + " estadoRelacion must equal an EstadoRelacion name, was: " + trimmed);
        }
    }

    private static TipoContrato parseTipoContrato(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            return TipoContrato.valueOf(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "edit_trato tipoContrato must equal a TipoContrato name, was: " + trimmed);
        }
    }
}
