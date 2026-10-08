package com.ar.crm2.exception;

public class MemorySafetyViolationException extends DomainException {
    private MemorySafetyViolationException(String message) {
        super(message);
    }
    public static MemorySafetyViolationException explicitRequestRequired() {
        return new MemorySafetyViolationException("La memoria durable requiere una solicitud explícita del usuario.");
    }
    public static MemorySafetyViolationException sensitiveOrInferredContent() {
        return new MemorySafetyViolationException("La memoria durable no puede almacenar contenido inferido, sensible o payloads de herramientas.");
    }
}
