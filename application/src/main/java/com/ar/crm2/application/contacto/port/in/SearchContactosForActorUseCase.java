package com.ar.crm2.application.contacto.port.in;

import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.model.entity.Contacto;

import java.util.List;

/** Searches contacts visible to a trusted CRM actor for agent tools. */
public interface SearchContactosForActorUseCase {

    List<Contacto> search(GetAllContactosCommand command);
}