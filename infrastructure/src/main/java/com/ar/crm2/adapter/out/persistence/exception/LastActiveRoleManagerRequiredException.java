package com.ar.crm2.adapter.out.persistence.exception;

/** Prevents concurrent role edits or user changes from removing the final active role manager. */
public final class LastActiveRoleManagerRequiredException extends RuntimeException {
    public LastActiveRoleManagerRequiredException() {
        super("At least one active user with role-management permission must remain");
    }
}
