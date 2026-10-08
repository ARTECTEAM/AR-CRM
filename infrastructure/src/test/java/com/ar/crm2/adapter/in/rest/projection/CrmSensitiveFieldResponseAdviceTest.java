package com.ar.crm2.adapter.in.rest.projection;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.exception.CrmActorUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CrmSensitiveFieldResponseAdviceTest {

    @Test
    void missingCurrentActorFailsClosedInsteadOfReturningSensitiveBodyUnchanged() throws Exception {
        CrmSensitiveFieldResponseAdvice advice = new CrmSensitiveFieldResponseAdvice(
                () -> Optional.empty(), mock(CrmAuthorization.class));
        Method controllerMethod = TratoController.class.getDeclaredMethod("body");
        MethodParameter returnType = new MethodParameter(controllerMethod, -1);

        assertThatThrownBy(() -> advice.beforeBodyWrite(
                new TratoResponse("Deal", new BigDecimal("1000.00")), returnType,
                MediaType.APPLICATION_JSON, MappingJackson2HttpMessageConverter.class,
                null, null))
                .isInstanceOf(CrmActorUnavailableException.class);
    }

    private static final class TratoController {
        @SuppressWarnings("unused")
        TratoResponse body() {
            return new TratoResponse("Deal", new BigDecimal("1000.00"));
        }
    }

    private record TratoResponse(String nombre, BigDecimal valorEstimado) {
    }
}
