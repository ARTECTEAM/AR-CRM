package com.ar.crm2.adapter.out.ai.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;

/** Redacts all tool failures except explicitly marked safe validation messages. */
public final class SafeToolExecutionExceptionProcessor implements ToolExecutionExceptionProcessor {

    private static final ToolError VALIDATION_ERROR =
            new ToolError(false, "TOOL_VALIDATION_FAILED", "The tool input is invalid.");
    private static final ToolError EXECUTION_ERROR =
            new ToolError(false, "TOOL_EXECUTION_FAILED", "The tool could not be completed.");

    private final ObjectMapper objectMapper;

    public SafeToolExecutionExceptionProcessor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String process(ToolExecutionException exception) {
        ToolError error = hasSafeValidationCause(exception) ? VALIDATION_ERROR : EXECUTION_ERROR;
        try {
            return objectMapper.writeValueAsString(error);
        } catch (JsonProcessingException serializationFailure) {
            return "{\"success\":false,\"code\":\"TOOL_EXECUTION_FAILED\","
                    + "\"message\":\"The tool could not be completed.\"}";
        }
    }

    private static boolean hasSafeValidationCause(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof SafeToolValidationException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private record ToolError(boolean success, String code, String message) {
    }
}
