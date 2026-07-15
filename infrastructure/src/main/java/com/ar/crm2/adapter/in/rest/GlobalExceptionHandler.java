package com.ar.crm2.adapter.in.rest;

import com.ar.crm2.application.agenda.exception.AgendaNotFoundException;
import com.ar.crm2.application.columna.exception.ColumnaHasAssociatedFichasException;
import com.ar.crm2.application.columna.exception.ColumnaNotFoundException;
import com.ar.crm2.application.contacto.exception.ContactoHasAssociatedTratosException;
import com.ar.crm2.application.contacto.exception.ContactoNotFoundException;
import com.ar.crm2.application.empresa.exception.EmpresaHasAssociatedTratosException;
import com.ar.crm2.application.empresa.exception.EmpresaNotFoundException;
import com.ar.crm2.application.etiqueta.exception.EtiquetaNotFoundException;
import com.ar.crm2.application.etiqueta.exception.EtiquetaRequiresConfirmationException;
import com.ar.crm2.application.ficha.exception.FichaMovimientoIncompatibleException;
import com.ar.crm2.application.ficha.exception.FichaNotFoundException;
import com.ar.crm2.application.identity.model.IdentityProvisioningException;
import com.ar.crm2.application.rol.exception.RolHasAssociatedUsuariosException;
import com.ar.crm2.application.rol.exception.RolNotFoundException;
import com.ar.crm2.application.security.exception.AuthenticatedUsuarioRequiredException;
import com.ar.crm2.application.superusuario.exception.SuperUsuarioNotFoundException;
import com.ar.crm2.application.tablero.exception.TableroNotFoundException;
import com.ar.crm2.application.tarea.exception.TareaNotFoundException;
import com.ar.crm2.application.trato.exception.TratoNotFoundException;
import com.ar.crm2.application.usuario.exception.UsuarioNotFoundException;
import com.ar.crm2.exception.ColumnaConFichasNoPuedeEliminarseException;
import com.ar.crm2.exception.ColumnaYaExisteEnTableroException;
import com.ar.crm2.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST adapters.
 * Translates domain exceptions to HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles RolNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(RolNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRolNotFoundException(RolNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ColumnaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleColumnaNotFoundException(ColumnaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles UsuarioNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(UsuarioNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUsuarioNotFoundException(UsuarioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles SuperUsuarioNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(SuperUsuarioNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSuperUsuarioNotFoundException(SuperUsuarioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles RolHasAssociatedUsuariosException as 409 Conflict.
     */
    @ExceptionHandler(RolHasAssociatedUsuariosException.class)
    public ResponseEntity<Map<String, String>> handleRolHasAssociatedUsuariosException(RolHasAssociatedUsuariosException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ColumnaHasAssociatedFichasException.class)
    public ResponseEntity<Map<String, String>> handleColumnaHasAssociatedFichasException(ColumnaHasAssociatedFichasException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles EmpresaNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(EmpresaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEmpresaNotFoundException(EmpresaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles ContactoNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(ContactoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleContactoNotFoundException(ContactoNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles TratoNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(TratoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTratoNotFoundException(TratoNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles TareaNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(TareaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTareaNotFoundException(TareaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles FichaNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(FichaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFichaNotFoundException(FichaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(FichaMovimientoIncompatibleException.class)
    public ResponseEntity<Map<String, String>> handleFichaMovimientoIncompatibleException(FichaMovimientoIncompatibleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles EtiquetaNotFoundException as 404 Not Found.
     * When the exception aggregates missing ids (resolver path), the
     * response still uses 404 since the request asked for an entity the
     * catalog could not resolve.
     */
    @ExceptionHandler(EtiquetaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEtiquetaNotFoundException(EtiquetaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles EtiquetaRequiresConfirmationException as 409 Conflict.
     * Caller must re-issue the delete with confirm=true to cascade.
     */
    @ExceptionHandler(EtiquetaRequiresConfirmationException.class)
    public ResponseEntity<Map<String, String>> handleEtiquetaRequiresConfirmationException(EtiquetaRequiresConfirmationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AgendaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAgendaNotFoundException(AgendaNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles EmpresaHasAssociatedTratosException as 409 Conflict.
     */
    @ExceptionHandler(EmpresaHasAssociatedTratosException.class)
    public ResponseEntity<Map<String, String>> handleEmpresaHasAssociatedTratosException(EmpresaHasAssociatedTratosException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles ContactoHasAssociatedTratosException as 409 Conflict.
     */
    @ExceptionHandler(ContactoHasAssociatedTratosException.class)
    public ResponseEntity<Map<String, String>> handleContactoHasAssociatedTratosException(ContactoHasAssociatedTratosException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles TableroNotFoundException as 404 Not Found.
     */
    @ExceptionHandler(TableroNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleTableroNotFoundException(TableroNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles ColumnaConFichasNoPuedeEliminarseException as 409 Conflict.
     * This domain exception is thrown when a column deletion is attempted
     * on a column that contains fichas.
     */
    @ExceptionHandler(ColumnaConFichasNoPuedeEliminarseException.class)
    public ResponseEntity<Map<String, String>> handleColumnaConFichasNoPuedeEliminarse(ColumnaConFichasNoPuedeEliminarseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles ColumnaYaExisteEnTableroException as 409 Conflict.
     * Thrown when the caller attempts to assign a Columna that is already
     * part of the target Tablero. Surfaced by
     * POST /api/tableros/asignar-columna; must NOT fall through to the
     * generic DomainException 400 handler (would hide the conflict
     * semantics and confuse clients).
     */
    @ExceptionHandler(ColumnaYaExisteEnTableroException.class)
    public ResponseEntity<Map<String, String>> handleColumnaYaExisteEnTablero(ColumnaYaExisteEnTableroException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles domain business rule violations as 400 Bad Request.
     * Covers DomainException and all subclasses (InvariantViolationException,
     * UnauthorizedDomainActionException, TipoTableroMismatchException, etc.).
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(DomainException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles illegal argument exceptions as 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles @Valid body validation failures as 400 Bad Request, con el detalle
     * por campo, en el mismo formato {"error": ...} que el resto de respuestas.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + (fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalido"))
            .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", detalle.isBlank() ? "Datos invalidos" : detalle));
    }

    /**
     * Handles IllegalStateException as 502 Bad Gateway.
     * Used when an upstream integration (e.g. Evolution API) fails to return
     * the data we need, distinct from a client input error.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles AuthenticatedUsuarioRequiredException as 403 Forbidden.
     * This exception is thrown when the JWT token lacks the required usuario_id claim.
     */
    @ExceptionHandler(AuthenticatedUsuarioRequiredException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticatedUsuarioRequiredException(AuthenticatedUsuarioRequiredException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handles IdentityProvisioningException due to connection/auth issues as 502 Bad Gateway.
     */
    @ExceptionHandler(IdentityProvisioningException.class)
    public ResponseEntity<Map<String, String>> handleIdentityProvisioningException(IdentityProvisioningException ex) {
        HttpStatus status = switch (ex.getReason()) {
            case USER_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONNECTION_FAILURE, AUTHENTICATION_FAILURE, SERVER_ERROR -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status)
            .body(Map.of("error", ex.getMessage()));
    }
}
