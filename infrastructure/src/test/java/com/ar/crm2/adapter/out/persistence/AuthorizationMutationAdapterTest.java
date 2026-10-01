package com.ar.crm2.adapter.out.persistence;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationMutationAdapterTest {

    @Test
    void locksRolesBeforeInvokingPrivilegedMutation() {
        RoleManagerGovernance governance = mock(RoleManagerGovernance.class);
        when(governance.lockRoles()).thenReturn(List.of());
        AuthorizationMutationAdapter adapter = new AuthorizationMutationAdapter(governance);

        String result = adapter.execute(() -> {
            verify(governance).lockRoles();
            return "saved";
        });

        assertEquals("saved", result);
        verify(governance).lockRoles();
    }
}
