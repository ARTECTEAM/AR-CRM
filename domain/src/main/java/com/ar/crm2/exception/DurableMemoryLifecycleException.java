package com.ar.crm2.exception;

public class DurableMemoryLifecycleException extends DomainException {
    private DurableMemoryLifecycleException(String message) {
        super(message);
    }
    public static DurableMemoryLifecycleException invalidTransition() {
        return new DurableMemoryLifecycleException("La memoria durable no se encuentra activa para esta transición.");
    }
    public static DurableMemoryLifecycleException ownerOrTargetMismatch() {
        return new DurableMemoryLifecycleException("El propietario o destino explícito de la memoria durable no coincide.");
    }
    public static DurableMemoryLifecycleException invalidSuccessor() {
        return new DurableMemoryLifecycleException("La memoria sucesora debe estar activa y vigente.");
    }
}
