package com.ar.crm2.adapter.in.rest.projection;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import com.ar.crm2.application.security.port.out.CurrentActorPort;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Optional;

/**
 * Applies the same grouped-field policy to REST DTOs after canonical use cases have enforced
 * action and row scope. Resource lists are filtered earlier in the application layer.
 */
@ControllerAdvice
public final class CrmSensitiveFieldResponseAdvice implements ResponseBodyAdvice<Object> {
    private final CurrentActorPort currentActorPort;
    private final CrmAuthorization authorization;

    public CrmSensitiveFieldResponseAdvice(CurrentActorPort currentActorPort, CrmAuthorization authorization) {
        this.currentActorPort = currentActorPort;
        this.authorization = authorization;
    }

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return resourceFor(returnType.getContainingClass()).isPresent();
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        Optional<RecursoCrm> resource = resourceFor(returnType.getContainingClass());
        if (body == null || resource.isEmpty()) {
            return body;
        }
        currentActorPort.currentActor().orElseThrow(() -> new CrmActorUnavailableException(
                "No active CRM user is linked to this request"));
        RecursoCrm crmResource = resource.get();
        return CrmSensitiveFieldProjector.project(body, crmResource, authorization.fieldPolicy(crmResource));
    }

    private static Optional<RecursoCrm> resourceFor(Class<?> controller) {
        return switch (controller.getSimpleName()) {
            case "TratoController" -> Optional.of(RecursoCrm.TRATO);
            case "TableroController" -> Optional.of(RecursoCrm.TABLERO);
            case "ContactoController" -> Optional.of(RecursoCrm.CONTACTO);
            case "EmpresaController" -> Optional.of(RecursoCrm.EMPRESA);
            case "UsuarioController" -> Optional.of(RecursoCrm.USUARIO);
            default -> Optional.empty();
        };
    }
}
