package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.adapter.in.rest.dto.request.CambiarEstadoEmpresaRequest;
import com.ar.crm2.adapter.in.rest.dto.request.CreateEmpresaRequest;
import com.ar.crm2.adapter.in.rest.dto.request.EditEmpresaRequest;
import com.ar.crm2.adapter.in.rest.dto.response.EmpresaResponse;
import com.ar.crm2.adapter.in.rest.mapper.EmpresaCommandMapper;
import com.ar.crm2.application.empresa.command.CambiarEstadoEmpresaCommand;
import com.ar.crm2.application.empresa.command.CreateEmpresaCommand;
import com.ar.crm2.application.empresa.command.DeleteEmpresaCommand;
import com.ar.crm2.application.empresa.command.EditEmpresaCommand;
import com.ar.crm2.application.empresa.port.in.CambiarEstadoEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.CreateEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.DeleteEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.EditEmpresaUseCase;
import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.security.ActorContext;
import com.ar.crm2.model.entity.Empresa;
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
 * REST controller for Empresa operations.
 * Implements inbound adapter pattern, delegates to application UseCase contracts.
 */
@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
public class EmpresaController {

    private final CreateEmpresaUseCase createUseCase;
    private final GetAllEmpresasUseCase getAllUseCase;
    private final EditEmpresaUseCase editUseCase;
    private final DeleteEmpresaUseCase deleteUseCase;
    private final CambiarEstadoEmpresaUseCase cambiarEstadoUseCase;

    /**
     * Creates a new Empresa.
     * The creadoPor is derived from the authenticated actor context (JWT).
     */
    @PostMapping("/create")
    public ResponseEntity<EmpresaResponse> create(
            HttpServletRequest httpRequest,
            @Valid @RequestBody CreateEmpresaRequest request
    ) {
        ActorContext actorContext = (ActorContext) httpRequest.getAttribute(
                ActorContextRequestAttributeFilter.ACTOR_CONTEXT_ATTRIBUTE);
        CreateEmpresaCommand command = EmpresaCommandMapper.toCommand(request, actorContext);
        Empresa empresa = createUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(EmpresaResponse.fromDomain(empresa));
    }

    /**
     * Retrieves all Empresas.
     */
    @GetMapping("/get-all")
    public ResponseEntity<List<EmpresaResponse>> getAll() {
        List<Empresa> empresas = getAllUseCase.getAll();
        List<EmpresaResponse> responses = empresas.stream().map(EmpresaResponse::fromDomain).toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Updates an existing Empresa.
     */
    @PutMapping("/edit")
    public ResponseEntity<EmpresaResponse> edit(@RequestParam UUID id, @Valid @RequestBody EditEmpresaRequest request) {
        EditEmpresaCommand command = EmpresaCommandMapper.toCommand(id, request);
        Empresa empresa = editUseCase.edit(command);
        return ResponseEntity.ok(EmpresaResponse.fromDomain(empresa));
    }

    /**
     * Deletes an existing Empresa.
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam UUID id) {
        DeleteEmpresaCommand command = EmpresaCommandMapper.toDeleteCommand(id);
        deleteUseCase.delete(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes the relation state of an Empresa.
     */
    @PutMapping("/cambiar-estado")
    public ResponseEntity<EmpresaResponse> cambiarEstado(
            @RequestParam UUID id,
            @Valid @RequestBody CambiarEstadoEmpresaRequest request
    ) {
        CambiarEstadoEmpresaCommand command = EmpresaCommandMapper.toCambiarEstadoCommand(id, request);
        Empresa empresa = cambiarEstadoUseCase.cambiarEstado(command);
        return ResponseEntity.ok(EmpresaResponse.fromDomain(empresa));
    }
}
