package com.ar.crm2.application.tablero.service;

import com.ar.crm2.application.tablero.port.out.FindAllTablerosPort;
import com.ar.crm2.application.tablero.port.in.GetAllTablerosUseCase;
import com.ar.crm2.model.entity.Tablero;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllTablerosUseCase.
 * Delegates listing directly to the outbound FindAllTablerosPort.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllTablerosService implements GetAllTablerosUseCase {

    private final FindAllTablerosPort findAllPort;

    @Override
    public List<Tablero> getAll() {
        return findAllPort.findAll();
    }
}
