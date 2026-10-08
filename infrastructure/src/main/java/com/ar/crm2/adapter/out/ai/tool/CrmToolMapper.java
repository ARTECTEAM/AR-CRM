package com.ar.crm2.adapter.out.ai.tool;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.CreateContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditCompanyOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditContactOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.EditTratoOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FindContactsOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ColumnaOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.ColumnasOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FichaOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.FichasOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TableroOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TablerosOutput;
import com.ar.crm2.application.contacto.command.CreateContactoCommand;
import com.ar.crm2.application.contacto.command.EditContactoCommand;
import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.application.empresa.command.CreateEmpresaCommand;
import com.ar.crm2.application.empresa.command.EditEmpresaCommand;
import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.application.tablero.command.AsignarColumnaTableroCommand;
import com.ar.crm2.application.tablero.command.CreateTableroCommand;
import com.ar.crm2.application.tablero.command.EditTableroCommand;
import com.ar.crm2.application.tablero.command.GetTableroByIdCommand;
import com.ar.crm2.application.tablero.command.ReordenarColumnasCommand;
import com.ar.crm2.application.columna.command.CreateColumnaCommand;
import com.ar.crm2.application.columna.command.EditColumnaCommand;
import com.ar.crm2.application.columna.command.GetColumnaByIdCommand;
import com.ar.crm2.application.ficha.command.CreateFichaCommand;
import com.ar.crm2.application.ficha.command.EditFichaCommand;
import com.ar.crm2.application.ficha.command.GetFichaByIdCommand;
import com.ar.crm2.application.ficha.command.MoverColumnaFichaCommand;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.entity.ColumnaTablero;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.entity.Agenda;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.enums.TipoContrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Optional;

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
     * Hard model-visible cap applied to every {@code find_contacts} invocation.
     * The query fetches one additional sentinel row so truncation can be
     * reported without exposing more than this cap.
     */
    public static final int FIND_CONTACTS_MAX_RESULTS = 20;
    public static final int FIND_CONTACTS_QUERY_LIMIT = FIND_CONTACTS_MAX_RESULTS + 1;
    public static final int LIST_MAX_RESULTS = 50;
    public static final int NESTED_MAX_RESULTS = 25;

    private CrmToolMapper() {
    }

    public static GetTableroByIdCommand toGetTableroByIdCommand(UUID id) {
        return new GetTableroByIdCommand(id);
    }

    public static CreateTableroCommand toCreateTableroCommand(
            String nombre, String descripcion, TipoTablero tipoTablero, UUID actorId) {
        return new CreateTableroCommand(requireNonBlank(nombre, "create_tablero requires nombre"),
                requireNonBlank(descripcion, "create_tablero requires descripcion"),
                requireEnum(tipoTablero, "create_tablero tipoTablero"), true, actorId);
    }

    public static EditTableroCommand toEditTableroCommand(UUID id, String nombre, String descripcion) {
        return new EditTableroCommand(id, nombre, descripcion);
    }

    public static AsignarColumnaTableroCommand toAsignarColumnaTableroCommand(
            UUID tableroId, UUID columnaId, Integer limiteWip, String nota, BigDecimal totalValorEstimado) {
        return new AsignarColumnaTableroCommand(tableroId, columnaId, limiteWip, trimToNull(nota), totalValorEstimado);
    }

    public static ReordenarColumnasCommand toReordenarColumnasCommand(UUID tableroId, List<UUID> nuevoOrden) {
        List<ColumnaId> ids = nuevoOrden == null ? null : nuevoOrden.stream()
                .map(ColumnaId::from).toList();
        return new ReordenarColumnasCommand(tableroId, ids);
    }

    public static GetColumnaByIdCommand toGetColumnaByIdCommand(UUID id) {
        return new GetColumnaByIdCommand(id);
    }

    public static CreateColumnaCommand toCreateColumnaCommand(
            String nombre, String color, TipoTablero tipoTablero, TipoColumna tipoColumna, UUID trustedSuperUsuario) {
        return new CreateColumnaCommand(Optional.ofNullable(trustedSuperUsuario),
                requireNonBlank(nombre, "create_columna requires nombre"), trimToNull(color),
                requireEnum(tipoTablero, "create_columna tipoTablero"),
                requireEnum(tipoColumna, "create_columna tipoColumna"));
    }

    public static EditColumnaCommand toEditColumnaCommand(
            UUID id, String nombre, String color, TipoTablero tipoTablero, TipoColumna tipoColumna) {
        return new EditColumnaCommand(id, nombre, trimToNull(color),
                requireEnum(tipoTablero, "edit_columna tipoTablero"),
                requireEnum(tipoColumna, "edit_columna tipoColumna"));
    }

    public static GetFichaByIdCommand toGetFichaByIdCommand(UUID id) {
        return new GetFichaByIdCommand(id);
    }

    public static CreateFichaCommand toCreateFichaCommand(
            UUID columnaId, TipoFicha tipoFicha, UUID tratoId, UUID tareaId, List<UUID> etiquetaIds) {
        return new CreateFichaCommand(columnaId, requireEnum(tipoFicha, "create_ficha tipoFicha"),
                tratoId, tareaId, immutableIds(etiquetaIds));
    }

    public static EditFichaCommand toEditFichaCommand(
            UUID id, UUID columnaId, TipoFicha tipoFicha, UUID tratoId, UUID tareaId, List<UUID> etiquetaIds) {
        if (etiquetaIds == null) {
            throw new SafeToolValidationException("edit_ficha requires etiquetaIds; [] clears all labels");
        }
        return new EditFichaCommand(id, columnaId, requireEnum(tipoFicha, "edit_ficha tipoFicha"),
                tratoId, tareaId, immutableIds(etiquetaIds));
    }

    public static MoverColumnaFichaCommand toMoverColumnaFichaCommand(UUID fichaId, UUID targetColumnaId) {
        return new MoverColumnaFichaCommand(fichaId, targetColumnaId);
    }

    public static TablerosOutput toTablerosOutput(List<Tablero> tableros) {
        List<Tablero> ordered = ordered(tableros, Comparator.comparing(tablero -> tablero.getId().value()));
        return new TablerosOutput(ordered.stream().limit(LIST_MAX_RESULTS)
                .map(CrmToolMapper::toTableroOutput).toList(), ordered.size(), ordered.size() > LIST_MAX_RESULTS);
    }

    public static TableroOutput toTableroOutput(Tablero tablero) {
        if (tablero == null) return new TableroOutput(null, null, null, null, List.of(), 0, false);
        List<ColumnaTablero> columnsInConfiguredOrder = tablero.getColumnasTablero() == null
                ? List.of() : tablero.getColumnasTablero().stream().filter(Objects::nonNull).toList();
        var columns = columnsInConfiguredOrder.stream().limit(NESTED_MAX_RESULTS)
                .map(c -> new TableroOutput.ColumnaAssignment(
                        c.getColumnaId().value().toString(), name(c.getTipoTablero()), c.getLimiteWip(), c.getNota(),
                        c.getTotalValorEstimado())).toList();
        return new TableroOutput(tablero.getId().value().toString(), tablero.getNombre(), tablero.getDescripcion(),
                name(tablero.getTipoTablero()), columns, columnsInConfiguredOrder.size(),
                columnsInConfiguredOrder.size() > NESTED_MAX_RESULTS);
    }

    public static ColumnasOutput toColumnasOutput(List<Columna> columnas) {
        List<Columna> ordered = ordered(columnas, Comparator.comparing(columna -> columna.getId().value()));
        return new ColumnasOutput(ordered.stream().limit(LIST_MAX_RESULTS)
                .map(CrmToolMapper::toColumnaOutput).toList(), ordered.size(), ordered.size() > LIST_MAX_RESULTS);
    }

    public static ColumnaOutput toColumnaOutput(Columna columna) {
        if (columna == null) return new ColumnaOutput(null, null, null, null, null);
        return new ColumnaOutput(columna.getId().value().toString(), columna.getColumnanombre(), columna.getColor(),
                name(columna.getTipoTablero()), name(columna.getTipoColumna()));
    }

    public static FichasOutput toFichasOutput(List<Ficha> fichas) {
        List<Ficha> ordered = ordered(fichas, Comparator.comparing(ficha -> ficha.getId().value()));
        return new FichasOutput(ordered.stream().limit(LIST_MAX_RESULTS)
                .map(CrmToolMapper::toFichaOutput).toList(), ordered.size(), ordered.size() > LIST_MAX_RESULTS);
    }

    public static FichaOutput toFichaOutput(Ficha ficha) {
        if (ficha == null) return new FichaOutput(null, null, null, null, null, List.of(), 0, false);
        var ordered = ordered(ficha.getEtiquetas(),
                Comparator.comparing(etiqueta -> etiqueta.getEtiquetaId().value()));
        var etiquetaIds = ordered.stream().limit(NESTED_MAX_RESULTS)
                .map(etiqueta -> etiqueta.getEtiquetaId().value().toString()).toList();
        return new FichaOutput(ficha.getId().value().toString(), ficha.getColumnaId().value().toString(),
                name(ficha.getTipoFicha()),
                ficha.getTratoId() == null ? null : ficha.getTratoId().value().toString(),
                ficha.getTareaId() == null ? null : ficha.getTareaId().value().toString(),
                etiquetaIds, ordered.size(), ordered.size() > NESTED_MAX_RESULTS);
    }

    // ── raw values → command ────────────────────────────────────────

    /**
     * Maps raw {@code find_contacts} parameter values plus the trusted
     * actor into the existing {@link GetAllContactosCommand} with the
     * hard cap of 20 and trim-to-null normalization on string filters.
     */
    public static GetAllContactosCommand toGetAllContactosCommand(
            String search, EstadoRelacion estadoRelacion, UUID empresaId, UUID responsableId,
            String comoNosConocio, UUID trustedActorUsuarioId) {
        if (trustedActorUsuarioId == null) {
            throw new SafeToolValidationException("Trusted actor context is required");
        }
        return new GetAllContactosCommand(
                trustedActorUsuarioId, trimToNull(search), name(estadoRelacion),
                empresaId, responsableId, trimToNull(comoNosConocio), FIND_CONTACTS_QUERY_LIMIT);
    }

    /**
     * Maps raw {@code create_contact} parameter values plus the
     * trusted actor into the existing {@link CreateContactoCommand}.
     * Validates required fields and the {@link EstadoRelacion} name
     * before the use case runs.
     */
    public static CreateContactoCommand toCreateContactoCommand(
            UUID empresaId, String nombre, String correo, EstadoRelacion estadoRelacion,
            UUID responsableId, String telefono, String cargo, String comoNosConocio,
            UUID trustedActorUsuarioId) {
        if (trustedActorUsuarioId == null) {
            throw new SafeToolValidationException("Trusted actor context is required");
        }
        if (empresaId == null) {
            throw new SafeToolValidationException("create_contact requires empresaId");
        }
        String trimmedNombre = requireNonBlank(nombre, "create_contact requires nombre");
        EstadoRelacion estado = requireEnum(estadoRelacion, "create_contact estadoRelacion");
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
            UUID id, String nombre, String correo, EstadoRelacion estadoRelacion,
            UUID responsableId, String telefono, String cargo, String comoNosConocio) {
        if (id == null) {
            throw new SafeToolValidationException("edit_contact requires id");
        }
        String trimmedNombre = requireNonBlank(nombre, "edit_contact requires nombre");
        EstadoRelacion parsedEstado = requireEnum(estadoRelacion, "edit_contact estadoRelacion");
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
            LocalDate fechaCierreEsperada, TipoContrato tipoContrato) {
        if (id == null) {
            throw new SafeToolValidationException("edit_trato requires id");
        }
        if (responsableId == null) {
            throw new SafeToolValidationException("edit_trato requires responsableId");
        }
        String trimmedNombre = requireNonBlank(nombre, "edit_trato requires nombre");
        return new EditTratoCommand(
                id, responsableId, trimmedNombre,
                valorEstimado, probabilidad, fechaCierreEsperada,
                requireEnum(tipoContrato, "edit_trato tipoContrato"));
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
            EstadoRelacion estadoRelacion, UUID responsableId, String notas,
            UUID trustedActorUsuarioId) {
        if (trustedActorUsuarioId == null) {
            throw new SafeToolValidationException("Trusted actor context is required");
        }
        String trimmedNombre = requireNonBlank(nombre, "create_company requires nombre");
        return new CreateEmpresaCommand(
                trimmedNombre, trimToNull(sector), trimToNull(telefono), trimToNull(paginaWeb),
                trimToNull(facebook), trimToNull(instagram), trimToNull(twitter),
                estadoRelacion, responsableId, trustedActorUsuarioId, trimToNull(notas));
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
            EstadoRelacion estadoRelacion, UUID responsableId, String notas) {
        if (id == null) {
            throw new SafeToolValidationException("edit_company requires id");
        }
        String trimmedNombre = requireNonBlank(nombre, "edit_company requires nombre");
        return new EditEmpresaCommand(
                id, trimmedNombre, trimToNull(sector), trimToNull(telefono), trimToNull(paginaWeb),
                trimToNull(facebook), trimToNull(instagram), trimToNull(twitter),
                estadoRelacion, responsableId, trimToNull(notas));
    }

    // ── entity → output ─────────────────────────────────────────────

    /**
     * Projects the {@code find_contacts} domain result to the bounded
     * model-visible output. Null or empty inputs map to an empty
     * {@link FindContactsOutput}; internal fields are stripped.
     */
    public static FindContactsOutput toFindContactsOutput(List<Contacto> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            return new FindContactsOutput(List.of(), 0, false);
        }
        List<Contacto> ordered = ordered(contacts, Comparator.comparing(contact -> contact.getId().value()));
        List<FindContactsOutput.ContactSummary> summaries = new ArrayList<>(
                Math.min(ordered.size(), FIND_CONTACTS_MAX_RESULTS));
        for (Contacto contact : ordered.stream().limit(FIND_CONTACTS_MAX_RESULTS).toList()) {
            summaries.add(new FindContactsOutput.ContactSummary(
                    contact.getId().value().toString(),
                    contact.getNombre(),
                    contact.getEstadoRelacion() == null ? null : contact.getEstadoRelacion().name(),
                    contact.getCorreo()
            ));
        }
        return new FindContactsOutput(summaries, summaries.size(), ordered.size() > FIND_CONTACTS_MAX_RESULTS);
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
                contact.getId().value().toString(),
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
                contact.getId().value().toString(),
                contact.getNombre(),
                contact.getCorreo(),
                contact.getEstadoRelacion() == null ? null : contact.getEstadoRelacion().name(),
                contact.getResponsableId() == null ? null : contact.getResponsableId().value().toString(),
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
                trato.getId().value().toString(),
                trato.getNombre(),
                trato.getResponsableId() == null ? null : trato.getResponsableId().value().toString(),
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
                company.getId().value().toString(),
                company.getNombre(),
                company.getSector(),
                company.getEstadoRelacion() == null ? null : company.getEstadoRelacion().name(),
                company.getResponsableId() == null ? null : company.getResponsableId().value().toString()
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
                company.getId().value().toString(),
                company.getNombre(),
                company.getSector(),
                company.getEstadoRelacion() == null ? null : company.getEstadoRelacion().name(),
                company.getResponsableId() == null ? null : company.getResponsableId().value().toString(),
                company.getPaginaWeb(),
                company.getTelefono(),
                company.getNotas()
        );
    }

    public static ResourceToolOutput.Contact toContactOutput(Contacto contact) {
        if (contact == null) return new ResourceToolOutput.Contact(null, null, null);
        return new ResourceToolOutput.Contact(contact.getId().value().toString(), contact.getNombre(),
                name(contact.getEstadoRelacion()));
    }

    public static ResourceToolOutput.Companies toCompaniesOutput(List<Empresa> companies) {
        List<Empresa> ordered = ordered(companies, Comparator.comparing(company -> company.getId().value()));
        return new ResourceToolOutput.Companies(ordered.stream().limit(LIST_MAX_RESULTS)
                .map(CrmToolMapper::toCompanyOutput).toList(), ordered.size(), ordered.size() > LIST_MAX_RESULTS);
    }

    public static ResourceToolOutput.Company toCompanyOutput(Empresa company) {
        if (company == null) return new ResourceToolOutput.Company(null, null, null, null);
        return new ResourceToolOutput.Company(company.getId().value().toString(), company.getNombre(),
                company.getSector(), name(company.getEstadoRelacion()));
    }

    public static ResourceToolOutput.Deals toDealsOutput(List<Trato> deals) {
        List<Trato> ordered = ordered(deals, Comparator.comparing(deal -> deal.getId().value()));
        return new ResourceToolOutput.Deals(ordered.stream().limit(LIST_MAX_RESULTS)
                .map(CrmToolMapper::toDealOutput).toList(), ordered.size(), ordered.size() > LIST_MAX_RESULTS);
    }

    public static ResourceToolOutput.Deal toDealOutput(Trato deal) {
        if (deal == null) return new ResourceToolOutput.Deal(null, null, null, null, null, null, null, null);
        LocalDate closeDate = deal.getFechaCierreEsperada();
        return new ResourceToolOutput.Deal(deal.getId().value().toString(), deal.getNombre(),
                name(deal.getEstado()), deal.getContactoId() == null ? null : deal.getContactoId().value().toString(),
                name(deal.getTipoContrato()), deal.getValorEstimado(), deal.getProbabilidad(),
                closeDate == null ? null : closeDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    public static ResourceToolOutput.Tasks toTasksOutput(List<Tarea> tasks) {
        List<Tarea> ordered = ordered(tasks, Comparator.comparing(task -> task.getId().value()));
        return new ResourceToolOutput.Tasks(ordered.stream().limit(LIST_MAX_RESULTS)
                .map(CrmToolMapper::toTaskOutput).toList(), ordered.size(), ordered.size() > LIST_MAX_RESULTS);
    }

    public static ResourceToolOutput.Task toTaskOutput(Tarea task) {
        if (task == null) return new ResourceToolOutput.Task(null, null, null, null, null, null);
        return new ResourceToolOutput.Task(task.getId().value().toString(), task.getTitulo(), name(task.getTipo()),
                name(task.getPrioridad()), task.getFechaLimite() == null ? null : task.getFechaLimite().toString(),
                task.getTratoId() == null ? null : task.getTratoId().value().toString());
    }

    public static ResourceToolOutput.Labels toLabelsOutput(List<Etiqueta> labels) {
        List<Etiqueta> ordered = ordered(labels, Comparator.comparing(label -> label.getId().value()));
        return new ResourceToolOutput.Labels(ordered.stream().limit(LIST_MAX_RESULTS)
                .map(CrmToolMapper::toLabelOutput).toList(), ordered.size(), ordered.size() > LIST_MAX_RESULTS);
    }

    public static ResourceToolOutput.Label toLabelOutput(Etiqueta label) {
        if (label == null) return new ResourceToolOutput.Label(null, null, null, null);
        return new ResourceToolOutput.Label(label.getId().value().toString(), label.getNombre(),
                name(label.getTipoEtiqueta()), label.getColor());
    }

    public static ResourceToolOutput.AgendaItems toAgendaItemsOutput(List<Agenda> agendas) {
        List<Agenda> ordered = ordered(agendas, Comparator.comparing(agenda -> agenda.getId().value()));
        return new ResourceToolOutput.AgendaItems(ordered.stream().limit(LIST_MAX_RESULTS)
                .map(CrmToolMapper::toAgendaItemOutput).toList(), ordered.size(), ordered.size() > LIST_MAX_RESULTS);
    }

    public static ResourceToolOutput.AgendaItem toAgendaItemOutput(Agenda agenda) {
        if (agenda == null) return new ResourceToolOutput.AgendaItem(null, null, null, null, null, null, null, null);
        return new ResourceToolOutput.AgendaItem(agenda.getId().value().toString(), name(agenda.getTipo()),
                agenda.getAsunto(), agenda.getFecha() == null ? null : agenda.getFecha().toString(),
                agenda.getHoraInicio() == null ? null : agenda.getHoraInicio().toString(),
                agenda.getHoraFin() == null ? null : agenda.getHoraFin().toString(),
                agenda.getTareaId() == null ? null : agenda.getTareaId().value().toString(),
                agenda.getTratoId() == null ? null : agenda.getTratoId().value().toString());
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String requireNonBlank(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new SafeToolValidationException(message);
        }
        return trimmed;
    }

    static <E extends Enum<E>> E requireEnum(E value, String field) {
        if (value == null) throw new SafeToolValidationException(field + " is required");
        return value;
    }

    private static List<UUID> immutableIds(List<UUID> ids) {
        return ids == null ? List.of() : List.copyOf(ids);
    }

    private static String name(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private static <T> List<T> ordered(List<T> values, Comparator<T> comparator) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).sorted(comparator).toList();
    }
}
