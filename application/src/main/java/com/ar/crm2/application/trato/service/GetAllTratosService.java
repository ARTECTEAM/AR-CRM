package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.shared.query.AuthorizedListPage;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.application.trato.port.in.GetAllTratosUseCase;
import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;

import java.util.List;
import java.util.Locale;

public final class GetAllTratosService implements GetAllTratosUseCase {
    private final CrmAuthorization authorization;
    private final FindAllTratosPort findAllPort;

    public GetAllTratosService(CrmAuthorization authorization, FindAllTratosPort findAllPort) {
        this.authorization = authorization;
        this.findAllPort = findAllPort;
    }

    @Override
    public List<Trato> getAll(TratoFilterCriteria criteria) {
        return authorizedRows(criteria);
    }

    @Override
    public PagedResult<Trato> getPage(TratoFilterCriteria criteria) {
        TratoFilterCriteria resolved = criteria == null ? TratoFilterCriteria.empty() : criteria;
        return AuthorizedListPage.slice(authorizedRows(resolved), resolved.pageRequest());
    }

    private List<Trato> authorizedRows(TratoFilterCriteria criteria) {
        TratoFilterCriteria resolved = criteria == null ? TratoFilterCriteria.empty() : criteria;
        authorization.require(RecursoCrm.TRATO, AccionCrm.LEER);
        ResourceReadPolicy policy = authorization.readPolicy(RecursoCrm.TRATO);
        requireVisibleFinancialQuery(resolved, policy);
        return findAllPort.findAll(resolved).stream()
                .filter(trato -> authorization.permitsRecord(
                        RecursoCrm.TRATO, AccionCrm.LEER, trato.getId().value()))
                .filter(trato -> authorization.permitsRecord(
                        RecursoCrm.CONTACTO, AccionCrm.LEER, trato.getContactoId().value()))
                .toList();
    }

    private static void requireVisibleFinancialQuery(TratoFilterCriteria criteria, ResourceReadPolicy policy) {
        boolean asksForFinancialValues = criteria.valorMin() != null || criteria.valorMax() != null
                || (criteria.cierreEsperado() != null
                    && criteria.cierreEsperado() != TratoFilterCriteria.CierreEsperadoFilter.TODAS);
        ListPageRequest page = criteria.pageRequest();
        String sortBy = page == null || page.sortBy() == null ? "" : page.sortBy().toLowerCase(Locale.ROOT);
        boolean sortsByFinancialValue = sortBy.equals("valorestimado") || sortBy.equals("fechacierreesperada");
        if ((asksForFinancialValues || sortsByFinancialValue)
                && !policy.readableGroups().contains(GrupoCampoSensible.FINANCIERO)) {
            throw new CrmAuthorizationDeniedException("Financial deal filters require FINANCIERO read access");
        }
    }
}