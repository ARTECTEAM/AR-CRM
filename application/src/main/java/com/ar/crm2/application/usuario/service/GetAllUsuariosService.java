package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.usuario.port.in.GetAllUsuariosUseCase;
import com.ar.crm2.application.usuario.port.out.FindAllUsuariosPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Usuario;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Application service implementing GetAllUsuariosUseCase.
 * Returns all Usuarios from the outbound port.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetAllUsuariosService implements GetAllUsuariosUseCase {

    private final FindAllUsuariosPort findAllPort;
    private final CrmAuthorization authorization;

    @Override
    public List<Usuario> getAll() {
        authorization.readPolicy(RecursoCrm.USUARIO);
        return findAllPort.findAll().stream()
                .filter(user -> authorization.permitsRecord(RecursoCrm.USUARIO, AccionCrm.LEER, user.getId().value()))
                .toList();
    }
}
