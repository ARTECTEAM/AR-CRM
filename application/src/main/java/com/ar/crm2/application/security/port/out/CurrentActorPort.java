package com.ar.crm2.application.security.port.out;

import com.ar.crm2.application.security.CurrentActor;

import java.util.Optional;

/** Resolves the current request subject to its current, active local CRM user and role. */
public interface CurrentActorPort {
    Optional<CurrentActor> currentActor();
}
