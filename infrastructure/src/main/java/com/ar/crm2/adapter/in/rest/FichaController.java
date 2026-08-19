package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.dto.request.CreateFichaRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditFichaRequest;
import com.ar.crm2.adapter.in.rest.dto.request.MoverColumnaRequest;
import com.ar.crm2.adapter.in.rest.dto.response.FichaResponse;
import com.ar.crm2.adapter.in.rest.mapper.FichaCommandMapper;
import com.ar.crm2.application.ficha.command.CreateFichaCommand;
import com.ar.crm2.application.ficha.command.DeleteFichaCommand;
import com.ar.crm2.application.ficha.command.EditFichaCommand;
import com.ar.crm2.application.ficha.command.GetFichaByIdCommand;
import com.ar.crm2.application.ficha.command.MoverColumnaFichaCommand;
import com.ar.crm2.application.ficha.port.in.CreateFichaUseCase;
import com.ar.crm2.application.ficha.port.in.DeleteFichaUseCase;
import com.ar.crm2.application.ficha.port.in.EditFichaUseCase;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.application.ficha.port.in.GetFichaByIdUseCase;
import com.ar.crm2.application.ficha.port.in.MoverColumnaFichaUseCase;
import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.security.ActorContextRequestAttributeFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Ficha operations.
 * Implements inbound adapter pattern, delegates to application UseCase contracts.
 */
@RestController
@RequestMapping("/api/fichas")
@RequiredArgsConstructor
public class FichaController {

    private final CreateFichaUseCase createUseCase;
    private final GetAllFichasUseCase getAllUseCase;
    private final GetFichaByIdUseCase getByIdUseCase;
    private final EditFichaUseCase editUseCase;
    private final DeleteFichaUseCase deleteUseCase;
    private final MoverColumnaFichaUseCase moverAColumnaUseCase;

    /**
     * Creates a new Ficha.
     * The creadoPor is derived from the authenticated actor context (JWT).
     */
    @PostMapping("/create")
    public ResponseEntity<FichaResponse> create(
            HttpServletRequest httpRequest,
            @Valid @RequestBody CreateFichaRequest request
    ) {
        ActorContext actorContext = (ActorContext) httpRequest.getAttribute(
                ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE);
        CreateFichaCommand command = FichaCommandMapper.toCommand(request, actorContext);
        Ficha ficha = createUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(FichaResponse.fromDomain(ficha));
    }

    /**
     * Retrieves all Fichas.
     */
    @GetMapping("/get-all")
    public ResponseEntity<List<FichaResponse>> getAll() {
        List<Ficha> fichas = getAllUseCase.getAll();
        List<FichaResponse> responses = fichas.stream().map(FichaResponse::fromDomain).toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves a Ficha by its id.
     */
    @GetMapping("/get-by-id")
    public ResponseEntity<FichaResponse> getById(@RequestParam UUID id) {
        GetFichaByIdCommand command = FichaCommandMapper.toGetByIdCommand(id);
        Ficha ficha = getByIdUseCase.getById(command);
        return ResponseEntity.ok(FichaResponse.fromDomain(ficha));
    }

    /**
     * Updates an existing Ficha.
     */
    @PutMapping("/edit")
    public ResponseEntity<FichaResponse> edit(@RequestParam UUID id, @Valid @RequestBody EditFichaRequest request) {
        EditFichaCommand command = FichaCommandMapper.toCommand(id, request);
        Ficha ficha = editUseCase.edit(command);
        return ResponseEntity.ok(FichaResponse.fromDomain(ficha));
    }

    /**
     * Deletes an existing Ficha.
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam UUID id) {
        DeleteFichaCommand command = FichaCommandMapper.toDeleteCommand(id);
        deleteUseCase.delete(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * Moves a Ficha to a different Columna.
     */
    @PutMapping("/mover-columna")
    public ResponseEntity<FichaResponse> moverAColumna(
            @RequestParam UUID id,
            @Valid @RequestBody MoverColumnaRequest request
    ) {
        MoverColumnaFichaCommand command = FichaCommandMapper.toMoverColumnaCommand(id, request);
        Ficha ficha = moverAColumnaUseCase.moverAColumna(command);
        return ResponseEntity.ok(FichaResponse.fromDomain(ficha));
    }
}
