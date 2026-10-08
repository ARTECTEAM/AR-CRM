package com.ar.crm2.application.usuario.service;

import com.ar.crm2.application.usuario.command.GetUsuarioByIdCommand;
import com.ar.crm2.application.usuario.exception.UsuarioNotFoundException;
import com.ar.crm2.application.usuario.port.in.GetUsuarioByIdUseCase;
import com.ar.crm2.application.usuario.port.out.FindUsuarioByIdPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Usuario;
import com.ar.crm2.model.vo.UsuarioId;
import lombok.RequiredArgsConstructor;

/**
 * Application service implementing GetUsuarioByIdUseCase.
 * Retrieves a Usuario by id or throws UsuarioNotFoundException.
 * No Spring annotations — constructor injection via Lombok.
 */
@RequiredArgsConstructor
public class GetUsuarioByIdService implements GetUsuarioByIdUseCase {

    private final FindUsuarioByIdPort findPort;
    private final CrmAuthorization authorization;

    @Override
    public Usuario getById(GetUsuarioByIdCommand command) {
        authorization.requireRecord(RecursoCrm.USUARIO, AccionCrm.LEER, command.id());
        UsuarioId usuarioId = UsuarioId.from(command.id());

        return findPort.findById(usuarioId)
                .orElseThrow(() -> UsuarioNotFoundException.forId(command.id()));
    }
}
