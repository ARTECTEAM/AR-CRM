package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.command.GetContactoByIdCommand;
import com.ar.crm2.application.contacto.exception.ContactoNotFoundException;
import com.ar.crm2.application.contacto.port.in.GetContactoByIdUseCase;
import com.ar.crm2.application.contacto.port.out.FindContactoByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.vo.ContactoId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing GetContactoByIdUseCase.
 * Loads a Contacto by id or throws ContactoNotFoundException.
 */
@RequiredArgsConstructor
public class GetContactoByIdService implements GetContactoByIdUseCase {

    private final FindContactoByIdPort findPort;
    private final CrmAuthorization authorization;

    @Override
    public Contacto getById(GetContactoByIdCommand command) {
        authorization.require(RecursoCrm.CONTACTO, AccionCrm.LEER);
        authorization.requireRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, command.id());
        ContactoId contactoId = ContactoId.from(command.id());

        Contacto contacto = findPort.findById(contactoId)
                .orElseThrow(() -> ContactoNotFoundException.forId(command.id()));
        authorization.requireRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, contacto.getEmpresaId().value());
        return contacto;
    }
}
