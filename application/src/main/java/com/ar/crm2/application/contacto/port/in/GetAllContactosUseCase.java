package com.ar.crm2.application.contacto.port.in;

import com.ar.crm2.application.contacto.command.GetAllContactosCommand;
import com.ar.crm2.model.entity.Contacto;

import java.util.List;

/**
 * Inbound input port for retrieving Contactos with optional filters.
 */
public interface GetAllContactosUseCase {

    List<Contacto> getAll(GetAllContactosCommand command);
}
