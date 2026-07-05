package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.port.out.FindAllContactosPort;
import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.contacto.query.ContactoFilterCriteria;
import com.ar.crm2.model.entity.Contacto;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllContactosUseCase.
 * Delegates listing directly to the outbound FindAllContactosPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllContactosService implements GetAllContactosUseCase {

    private final FindAllContactosPort findAllPort;

    @Override
    public List<Contacto> getAll(ContactoFilterCriteria criteria) {
        return findAllPort.findAll(criteria);
    }
}
