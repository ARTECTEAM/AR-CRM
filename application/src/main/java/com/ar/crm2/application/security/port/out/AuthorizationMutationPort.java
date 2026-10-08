package com.ar.crm2.application.security.port.out;

import java.util.function.Supplier;

/** Runs a privileged authorization check and its local mutation in one serialized transaction. */
public interface AuthorizationMutationPort {
    <T> T execute(Supplier<T> mutation);
}
