package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.application.trato.port.in.GetAllTratosUseCase;
import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.model.entity.Trato;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllTratosUseCase.
 * Delegates listing directly to the outbound FindAllTratosPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllTratosService implements GetAllTratosUseCase {

    private final FindAllTratosPort findAllPort;

    @Override
    public List<Trato> getAll(TratoFilterCriteria criteria) {
        return findAllPort.findAll(criteria);
    }
}
