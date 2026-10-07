package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.application.security.port.out.AuthorizationMutationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Supplier;

/** Acquires the shared ordered role lock before any privileged mutation reads authorization state. */
@RequiredArgsConstructor
public class AuthorizationMutationAdapter implements AuthorizationMutationPort {
    private final RoleManagerGovernance roleManagerGovernance;

    @Override
    @Transactional
    public <T> T execute(Supplier<T> mutation) {
        roleManagerGovernance.lockRoles();
        return mutation.get();
    }
}
