package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.port.in.GetAllContactosUseCase;
import com.ar.crm2.application.contacto.port.out.FindAllContactosPort;
import com.ar.crm2.application.contacto.query.ContactoFilterCriteria;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.shared.query.AuthorizedListPage;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.application.shared.query.PagedResult;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Contacto;

import java.util.List;
import java.util.Locale;

public final class GetAllContactosService implements GetAllContactosUseCase {
    private final CrmAuthorization authorization;
    private final FindAllContactosPort findAllPort;

    public GetAllContactosService(CrmAuthorization authorization, FindAllContactosPort findAllPort) {
        this.authorization = authorization;
        this.findAllPort = findAllPort;
    }

    @Override
    public List<Contacto> getAll(ContactoFilterCriteria criteria) {
        return authorizedRows(criteria);
    }

    @Override
    public PagedResult<Contacto> getPage(ContactoFilterCriteria criteria) {
        ContactoFilterCriteria resolved = criteria == null ? ContactoFilterCriteria.empty() : criteria;
        return AuthorizedListPage.slice(authorizedRows(resolved), resolved.pageRequest());
    }

    private List<Contacto> authorizedRows(ContactoFilterCriteria criteria) {
        ContactoFilterCriteria resolved = criteria == null ? ContactoFilterCriteria.empty() : criteria;
        authorization.require(RecursoCrm.CONTACTO, AccionCrm.LEER);
        ResourceReadPolicy policy = authorization.readPolicy(RecursoCrm.CONTACTO);
        boolean canReadPrivateFields = policy.readableGroups().contains(GrupoCampoSensible.CONTACTO_PRIVADO);
        denyPrivateSortWithoutGrant(resolved, canReadPrivateFields);
        String search = normalize(resolved.search());
        return findAllPort.findAll(resolved).stream()
                .filter(contacto -> authorization.permitsRecord(
                        RecursoCrm.CONTACTO, AccionCrm.LEER, contacto.getId().value()))
                .filter(contacto -> authorization.permitsRecord(
                        RecursoCrm.EMPRESA, AccionCrm.LEER, contacto.getEmpresaId().value()))
                .filter(contacto -> canReadPrivateFields || matchesPublicSearch(contacto, search))
                .toList();
    }

    private static void denyPrivateSortWithoutGrant(ContactoFilterCriteria criteria, boolean canReadPrivateFields) {
        ListPageRequest page = criteria.pageRequest();
        String sortBy = page == null || page.sortBy() == null ? "" : page.sortBy().toLowerCase(Locale.ROOT);
        if (sortBy.equals("correo") && !canReadPrivateFields) {
            throw new CrmAuthorizationDeniedException("Contact email sorting requires CONTACTO_PRIVADO read access");
        }
    }

    private static boolean matchesPublicSearch(Contacto contacto, String search) {
        if (search == null) {
            return true;
        }
        return contains(contacto.getNombre(), search) || contains(contacto.getCargo(), search);
    }

    private static boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}