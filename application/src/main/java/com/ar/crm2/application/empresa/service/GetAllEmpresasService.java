package com.ar.crm2.application.empresa.service;

import com.ar.crm2.application.empresa.port.in.GetAllEmpresasUseCase;
import com.ar.crm2.application.empresa.port.out.FindAllEmpresasPort;
import com.ar.crm2.application.empresa.query.EmpresaFilterCriteria;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.shared.query.AuthorizedListPage;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Empresa;

import java.util.List;
import java.util.Locale;

public final class GetAllEmpresasService implements GetAllEmpresasUseCase {
    private final CrmAuthorization authorization;
    private final FindAllEmpresasPort findAllPort;

    public GetAllEmpresasService(CrmAuthorization authorization, FindAllEmpresasPort findAllPort) {
        this.authorization = authorization;
        this.findAllPort = findAllPort;
    }

    @Override
    public List<Empresa> getAll(EmpresaFilterCriteria criteria) {
        return authorizedRows(criteria);
    }

    @Override
    public PagedResult<Empresa> getPage(EmpresaFilterCriteria criteria) {
        EmpresaFilterCriteria resolved = criteria == null ? EmpresaFilterCriteria.empty() : criteria;
        return AuthorizedListPage.slice(authorizedRows(resolved), resolved.pageRequest());
    }

    private List<Empresa> authorizedRows(EmpresaFilterCriteria criteria) {
        EmpresaFilterCriteria resolved = criteria == null ? EmpresaFilterCriteria.empty() : criteria;
        authorization.require(RecursoCrm.EMPRESA, AccionCrm.LEER);
        ResourceReadPolicy policy = authorization.readPolicy(RecursoCrm.EMPRESA);
        boolean canReadPrivateContactFields = policy.readableGroups().contains(GrupoCampoSensible.CONTACTO_PRIVADO);
        String search = normalize(resolved.search());
        return findAllPort.findAll(resolved).stream()
                .filter(empresa -> authorization.permitsRecord(
                        RecursoCrm.EMPRESA, AccionCrm.LEER, empresa.getId().value()))
                .filter(empresa -> canReadPrivateContactFields || matchesPublicSearch(empresa, search))
                .toList();
    }

    private static boolean matchesPublicSearch(Empresa empresa, String search) {
        if (search == null) {
            return true;
        }
        return contains(empresa.getNombre(), search)
                || contains(empresa.getSector(), search)
                || contains(empresa.getPaginaWeb(), search);
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}