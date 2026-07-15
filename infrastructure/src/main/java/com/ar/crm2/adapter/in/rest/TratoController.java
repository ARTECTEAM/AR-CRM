package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.dto.request.CreateTratoRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditTratoRequest;
import com.ar.crm2.adapter.in.rest.dto.response.TratoResponse;
import com.ar.crm2.adapter.in.rest.dto.response.PageResponse;
import com.ar.crm2.adapter.in.rest.mapper.TratoCommandMapper;
import com.ar.crm2.application.trato.command.CreateTratoCommand;
import com.ar.crm2.application.trato.command.DeleteTratoCommand;
import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.application.trato.command.GetTratoByIdCommand;
import com.ar.crm2.application.notatrato.port.in.CrearNotaTratoUseCase;
import com.ar.crm2.application.notatrato.port.in.GetNotasByTratoUseCase;
import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.security.ActorContextRequestAttributeFilter;
import com.ar.crm2.adapter.in.rest.dto.response.NotaTratoResponse;
import com.ar.crm2.application.trato.port.in.CambiarEstadoTratoUseCase;
import com.ar.crm2.application.trato.port.in.CreateTratoUseCase;
import com.ar.crm2.application.trato.port.in.DeleteTratoUseCase;
import com.ar.crm2.application.trato.port.in.EditTratoUseCase;
import com.ar.crm2.application.trato.port.in.GetAllTratosUseCase;
import com.ar.crm2.application.trato.port.in.GetTratoByIdUseCase;
import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.model.enums.EstadoTrato;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.UsuarioId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

/**
 * REST controller for Trato operations.
 * Implements inbound adapter pattern, delegates to application UseCase contracts.
 */
@RestController
@RequestMapping("/api/tratos")
@RequiredArgsConstructor
public class TratoController {

    private final CreateTratoUseCase createUseCase;
    private final GetAllTratosUseCase getAllUseCase;
    private final GetTratoByIdUseCase getByIdUseCase;
    private final EditTratoUseCase editUseCase;
    private final DeleteTratoUseCase deleteUseCase;
    private final CambiarEstadoTratoUseCase cambiarEstadoUseCase;
    private final CrearNotaTratoUseCase crearNotaUseCase;
    private final GetNotasByTratoUseCase getNotasUseCase;

    public record PerderTratoRequest(@jakarta.validation.constraints.NotBlank String motivo) {}
    public record CrearNotaRequest(@jakarta.validation.constraints.NotBlank String contenido) {}

    /**
     * Creates a new Trato.
     */
    @PostMapping("/create")
    public ResponseEntity<TratoResponse> create(@Valid @RequestBody CreateTratoRequest request) {
        CreateTratoCommand command = TratoCommandMapper.toCommand(request);
        Trato trato = createUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TratoResponse.fromDomain(trato));
    }

    /**
     * Retrieves all Tratos.
     */
    @GetMapping("/get-all")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EstadoTrato estado,
            @RequestParam(required = false) TipoContrato tipoContrato,
            @RequestParam(required = false) UUID responsableId,
            @RequestParam(required = false) UUID contactoId,
            @RequestParam(required = false) BigDecimal valorMin,
            @RequestParam(required = false) BigDecimal valorMax,
            @RequestParam(required = false) String cierreEsperado,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        ListPageRequest pageRequest = pageRequest(page, pageSize, sortBy, sortDirection);
        TratoFilterCriteria criteria = new TratoFilterCriteria(
                search,
                estado,
                tipoContrato,
                responsableId != null ? UsuarioId.from(responsableId) : null,
                contactoId != null ? ContactoId.from(contactoId) : null,
                valorMin,
                valorMax,
                parseCierreEsperado(cierreEsperado),
                pageRequest
        );
        if (pageRequest.isPaged()) {
            return ResponseEntity.ok(PageResponse.from(getAllUseCase.getPage(criteria), TratoResponse::fromDomain));
        }
        List<Trato> tratos = getAllUseCase.getAll(criteria);
        List<TratoResponse> responses = tratos.stream().map(TratoResponse::fromDomain).toList();
        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<List<TratoResponse>> getAll() {
        List<Trato> tratos = getAllUseCase.getAll();
        List<TratoResponse> responses = tratos.stream().map(TratoResponse::fromDomain).toList();
        return ResponseEntity.ok(responses);
    }

    private TratoFilterCriteria.CierreEsperadoFilter parseCierreEsperado(String value) {
        if (value == null || value.isBlank()) return null;
        return switch (value) {
            case "todas" -> TratoFilterCriteria.CierreEsperadoFilter.TODAS;
            case "vencidas" -> TratoFilterCriteria.CierreEsperadoFilter.VENCIDAS;
            case "proximos-7" -> TratoFilterCriteria.CierreEsperadoFilter.PROXIMOS_7;
            case "proximos-30" -> TratoFilterCriteria.CierreEsperadoFilter.PROXIMOS_30;
            case "sin-fecha" -> TratoFilterCriteria.CierreEsperadoFilter.SIN_FECHA;
            default -> null;
        };
    }

    private ListPageRequest pageRequest(Integer page, Integer pageSize, String sortBy, String sortDirection) {
        return new ListPageRequest(page, pageSize, sortBy, parseSortDirection(sortDirection));
    }

    private ListPageRequest.SortDirection parseSortDirection(String value) {
        if (value == null || value.isBlank()) return null;
        return "asc".equalsIgnoreCase(value) ? ListPageRequest.SortDirection.ASC : ListPageRequest.SortDirection.DESC;
    }

    /**
     * Retrieves a Trato by its id.
     */
    @GetMapping("/get-by-id")
    public ResponseEntity<TratoResponse> getById(@RequestParam UUID id) {
        GetTratoByIdCommand command = TratoCommandMapper.toGetByIdCommand(id);
        Trato trato = getByIdUseCase.getById(command);
        return ResponseEntity.ok(TratoResponse.fromDomain(trato));
    }

    /**
     * Updates an existing Trato.
     */
    @PutMapping("/edit")
    public ResponseEntity<TratoResponse> edit(@RequestParam UUID id, @Valid @RequestBody EditTratoRequest request) {
        EditTratoCommand command = TratoCommandMapper.toCommand(id, request);
        Trato trato = editUseCase.edit(command);
        return ResponseEntity.ok(TratoResponse.fromDomain(trato));
    }

    /** Marca la oportunidad como ganada. */
    @PutMapping("/ganar")
    public ResponseEntity<TratoResponse> ganar(@RequestParam UUID id) {
        return ResponseEntity.ok(TratoResponse.fromDomain(cambiarEstadoUseCase.ganar(id)));
    }

    /** Marca la oportunidad como perdida, con motivo. */
    @PutMapping("/perder")
    public ResponseEntity<TratoResponse> perder(@RequestParam UUID id, @Valid @RequestBody PerderTratoRequest request) {
        return ResponseEntity.ok(TratoResponse.fromDomain(cambiarEstadoUseCase.perder(id, request.motivo())));
    }

    /** Lista las notas/eventos del timeline de un trato (más recientes primero). */
    @GetMapping("/notas/get-all")
    public ResponseEntity<List<NotaTratoResponse>> getNotas(@RequestParam UUID tratoId) {
        return ResponseEntity.ok(
            getNotasUseCase.getByTrato(tratoId).stream().map(NotaTratoResponse::fromDomain).toList());
    }

    /** Crea una nota manual en el trato (autor = usuario del JWT). */
    @PostMapping("/notas/create")
    public ResponseEntity<NotaTratoResponse> crearNota(
            HttpServletRequest httpRequest,
            @RequestParam UUID tratoId,
            @Valid @RequestBody CrearNotaRequest request) {
        ActorContext actor = (ActorContext) httpRequest.getAttribute(
                ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE);
        var nota = crearNotaUseCase.crear(tratoId, actor.usuarioId().orElseThrow(), request.contenido());
        return ResponseEntity.status(HttpStatus.CREATED).body(NotaTratoResponse.fromDomain(nota));
    }

    /**
     * Deletes an existing Trato.
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam UUID id) {
        DeleteTratoCommand command = TratoCommandMapper.toDeleteCommand(id);
        deleteUseCase.delete(command);
        return ResponseEntity.noContent().build();
    }
}
