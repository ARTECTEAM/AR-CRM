package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.dto.request.CreateTareaRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditTareaRequest;
import com.ar.crm2.adapter.in.rest.dto.response.TareaResponse;
import com.ar.crm2.adapter.in.rest.mapper.TareaCommandMapper;
import com.ar.crm2.application.tarea.command.CreateTareaCommand;
import com.ar.crm2.application.tarea.command.DeleteTareaCommand;
import com.ar.crm2.application.tarea.command.EditTareaCommand;
import com.ar.crm2.application.tarea.command.GetTareaByIdCommand;
import com.ar.crm2.application.tarea.port.in.CreateTareaUseCase;
import com.ar.crm2.application.tarea.port.in.DeleteTareaUseCase;
import com.ar.crm2.application.tarea.port.in.EditTareaUseCase;
import com.ar.crm2.application.tarea.port.in.GetAllTareasUseCase;
import com.ar.crm2.application.tarea.port.in.GetTareaByIdUseCase;
import com.ar.crm2.application.tarea.query.TareaFilterCriteria;
import com.ar.crm2.model.enums.PrioridadTarea;
import com.ar.crm2.model.enums.TipoTarea;
import com.ar.crm2.model.entity.Tarea;
import com.ar.crm2.model.vo.TratoId;
import com.ar.crm2.model.vo.UsuarioId;
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

/**
 * REST controller for Tarea operations.
 * Implements inbound adapter pattern, delegates to application UseCase contracts.
 */
@RestController
@RequestMapping("/api/tareas")
@RequiredArgsConstructor
public class TareaController {

    private final CreateTareaUseCase createUseCase;
    private final GetAllTareasUseCase getAllUseCase;
    private final GetTareaByIdUseCase getByIdUseCase;
    private final EditTareaUseCase editUseCase;
    private final DeleteTareaUseCase deleteUseCase;

    /**
     * Creates a new Tarea.
     */
    @PostMapping("/create")
    public ResponseEntity<TareaResponse> create(@Valid @RequestBody CreateTareaRequest request) {
        CreateTareaCommand command = TareaCommandMapper.toCommand(request);
        Tarea tarea = createUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TareaResponse.fromDomain(tarea));
    }

    /**
     * Retrieves all Tareas.
     */
    @GetMapping("/get-all")
    public ResponseEntity<List<TareaResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PrioridadTarea prioridad,
            @RequestParam(required = false) UUID responsableId,
            @RequestParam(required = false) UUID tratoId,
            @RequestParam(required = false) TipoTarea tipo,
            @RequestParam(required = false) String vencimiento
    ) {
        TareaFilterCriteria criteria = new TareaFilterCriteria(
                search,
                prioridad,
                responsableId != null ? UsuarioId.from(responsableId) : null,
                tratoId != null ? TratoId.from(tratoId) : null,
                tipo,
                parseVencimiento(vencimiento)
        );
        List<Tarea> tareas = getAllUseCase.getAll(criteria);
        List<TareaResponse> responses = tareas.stream().map(TareaResponse::fromDomain).toList();
        return ResponseEntity.ok(responses);
    }

    public ResponseEntity<List<TareaResponse>> getAll() {
        List<Tarea> tareas = getAllUseCase.getAll();
        List<TareaResponse> responses = tareas.stream().map(TareaResponse::fromDomain).toList();
        return ResponseEntity.ok(responses);
    }

    private TareaFilterCriteria.VencimientoFilter parseVencimiento(String value) {
        if (value == null || value.isBlank() || "todas".equals(value)) return null;
        return switch (value) {
            case "vencidas" -> TareaFilterCriteria.VencimientoFilter.VENCIDAS;
            case "proximas" -> TareaFilterCriteria.VencimientoFilter.PROXIMAS;
            default -> null;
        };
    }

    /**
     * Retrieves a Tarea by its id.
     */
    @GetMapping("/get-by-id")
    public ResponseEntity<TareaResponse> getById(@RequestParam UUID id) {
        GetTareaByIdCommand command = TareaCommandMapper.toGetByIdCommand(id);
        Tarea tarea = getByIdUseCase.getById(command);
        return ResponseEntity.ok(TareaResponse.fromDomain(tarea));
    }

    /**
     * Updates an existing Tarea.
     */
    @PutMapping("/edit")
    public ResponseEntity<TareaResponse> edit(@RequestParam UUID id, @Valid @RequestBody EditTareaRequest request) {
        EditTareaCommand command = TareaCommandMapper.toCommand(id, request);
        Tarea tarea = editUseCase.edit(command);
        return ResponseEntity.ok(TareaResponse.fromDomain(tarea));
    }

    /**
     * Deletes an existing Tarea.
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam UUID id) {
        DeleteTareaCommand command = TareaCommandMapper.toDeleteCommand(id);
        deleteUseCase.delete(command);
        return ResponseEntity.noContent().build();
    }
}
