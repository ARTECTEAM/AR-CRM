package com.ar.crm2.application.contacto.port.in;

import com.ar.crm2.application.contacto.query.ContactoFilterCriteria;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.model.entity.Contacto;

import java.util.List;

/**
 * Inbound input port for retrieving all Contactos.
 */
public interface GetAllContactosUseCase {

    /**
     * Retrieves all existing Contactos.
     *
     * @return list of all Contactos
     */
    default List<Contacto> getAll() {
        return getAll(ContactoFilterCriteria.empty());
    }

    List<Contacto> getAll(ContactoFilterCriteria criteria);

    PagedResult<Contacto> getPage(ContactoFilterCriteria criteria);
}
