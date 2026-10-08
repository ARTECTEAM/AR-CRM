package com.ar.crm2.adapter.out.ai.tool;

import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;

/** Redacts all tool failures except explicitly marked safe validation messages. */
public final class SafeToolExecutionExceptionProcessor implements ToolExecutionExceptionProcessor {

    private static final String VALIDATION_ERROR =
            "{\"success\":false,\"code\":\"TOOL_VALIDATION_FAILED\","
                    + "\"message\":\"The tool input is invalid.\"}";
    private static final String EXECUTION_ERROR =
            "{\"success\":false,\"code\":\"TOOL_EXECUTION_FAILED\","
                    + "\"message\":\"The tool could not be completed.\"}";

    @Override
    public String process(ToolExecutionException exception) {
        return hasSafeValidationCause(exception) ? VALIDATION_ERROR : EXECUTION_ERROR;
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
}