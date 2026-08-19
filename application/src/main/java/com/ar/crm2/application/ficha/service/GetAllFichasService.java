package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.ficha.port.out.FindAllFichasPort;
import com.ar.crm2.application.ficha.port.in.GetAllFichasUseCase;
import com.ar.crm2.model.entity.Ficha;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllFichasUseCase.
 * Delegates listing directly to the outbound FindAllFichasPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllFichasService implements GetAllFichasUseCase {

    private final FindAllFichasPort findAllPort;

    @Override
    public List<Ficha> getAll() {
        return findAllPort.findAll();
    }
}
