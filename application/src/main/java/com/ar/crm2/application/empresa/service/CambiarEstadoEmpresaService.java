package com.ar.crm2.application.empresa.service;

import com.ar.crm2.application.empresa.command.CambiarEstadoEmpresaCommand;
import com.ar.crm2.application.empresa.exception.EmpresaNotFoundException;
import com.ar.crm2.application.empresa.port.in.CambiarEstadoEmpresaUseCase;
import com.ar.crm2.application.empresa.port.out.ExistsTratosByEmpresaIdPort;
import com.ar.crm2.application.empresa.port.out.FindEmpresaByIdPort;
import com.ar.crm2.application.empresa.port.out.SaveEmpresaPort;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.vo.EmpresaId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CambiarEstadoEmpresaService implements CambiarEstadoEmpresaUseCase {

    private final FindEmpresaByIdPort findPort;
    private final SaveEmpresaPort savePort;
    private final ExistsTratosByEmpresaIdPort existsTratosPort;
    private final CrmAuthorization authorization;

    @Override
    public Empresa cambiarEstado(CambiarEstadoEmpresaCommand command) {
        authorization.require(RecursoCrm.EMPRESA, AccionCrm.ACTUALIZAR);
        authorization.requireRecord(RecursoCrm.EMPRESA, AccionCrm.ACTUALIZAR, command.empresaId());
        EmpresaId empresaId = EmpresaId.from(command.empresaId());

        Empresa existing = findPort.findById(empresaId)
                .orElseThrow(() -> EmpresaNotFoundException.forId(command.empresaId()));

        boolean tieneTratosActivos = existsTratosPort.existsTratosByEmpresaId(empresaId);

        Empresa updated = existing.cambiarEstadoRelacion(
                command.nuevoEstado(),
                tieneTratosActivos
        );

        return savePort.save(updated);
    }
}
