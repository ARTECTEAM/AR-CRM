package com.ar.crm2.application.empresa.service;

import com.ar.crm2.application.empresa.port.out.FindAllEmpresasPort;
import com.ar.crm2.application.empresa.query.EmpresaFilterCriteria;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Empresa;
import com.ar.crm2.model.enums.EstadoRelacion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAllEmpresasAuthorizationCompatibilityTest {

    @Test
    void deniesListingBeforeQueryWhenReadIsNotGranted() {
        FindAllEmpresasPort rows = mock(FindAllEmpresasPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).require(RecursoCrm.EMPRESA, AccionCrm.LEER);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new GetAllEmpresasService(authorization, rows).getAll(EmpresaFilterCriteria.empty()));
        verifyNoInteractions(rows);
    }

    @Test
    void filtersUnauthorizedRowsBeforeComputingPageTotalsAndPreservesCriteria() {
        EmpresaFilterCriteria criteria = new EmpresaFilterCriteria("visible", EstadoRelacion.ACTIVO,
                "retail", null, EmpresaFilterCriteria.WebFilter.CON_WEB,
                new ListPageRequest(0, 1, "nombre", ListPageRequest.SortDirection.ASC));
        Empresa hidden = company("Hidden");
        Empresa visible = company("Visible");
        FindAllEmpresasPort rows = mock(FindAllEmpresasPort.class);
        CrmAuthorization authorization = authorization(Set.of());
        when(rows.findAll(criteria)).thenReturn(List.of(hidden, visible));
        when(authorization.permitsRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, hidden.getId().value())).thenReturn(false);
        when(authorization.permitsRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, visible.getId().value())).thenReturn(true);

        var page = new GetAllEmpresasService(authorization, rows).getPage(criteria);

        assertEquals(List.of(visible), page.items());
        assertEquals(1, page.totalItems());
        assertEquals(1, page.totalPages());
        assertEquals(0, page.page());
        assertEquals(1, page.pageSize());
        verify(rows).findAll(criteria);
        verify(rows, never()).findPage(criteria);
    }

    @Test
    void excludesPhoneOnlySearchMatchesWithoutPrivateFieldGrant() {
        EmpresaFilterCriteria criteria = new EmpresaFilterCriteria("555-0199", null, null, null, null);
        Empresa matchedByPhoneOnly = companyWithPhone("Public name", "555-0199");
        FindAllEmpresasPort rows = mock(FindAllEmpresasPort.class);
        when(rows.findAll(criteria)).thenReturn(List.of(matchedByPhoneOnly));

        CrmAuthorization authorization = authorization(Set.of());
        when(authorization.permitsRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, matchedByPhoneOnly.getId().value())).thenReturn(true);

        var result = new GetAllEmpresasService(authorization, rows).getAll(criteria);

        assertTrue(result.isEmpty(), "a hidden phone must not reveal row membership through search");
    }

    @Test
    void allowsPhoneSearchWhenPrivateFieldGroupIsGranted() {
        EmpresaFilterCriteria criteria = new EmpresaFilterCriteria("555-0199", null, null, null, null);
        Empresa matchedByPhoneOnly = companyWithPhone("Public name", "555-0199");
        FindAllEmpresasPort rows = mock(FindAllEmpresasPort.class);
        when(rows.findAll(criteria)).thenReturn(List.of(matchedByPhoneOnly));

        CrmAuthorization authorization = authorization(Set.of(GrupoCampoSensible.CONTACTO_PRIVADO));
        when(authorization.permitsRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, matchedByPhoneOnly.getId().value())).thenReturn(true);

        var result = new GetAllEmpresasService(authorization, rows).getAll(criteria);

        assertEquals(List.of(matchedByPhoneOnly), result);
    }

    private static CrmAuthorization authorization(Set<GrupoCampoSensible> readable) {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        when(authorization.readPolicy(RecursoCrm.EMPRESA)).thenReturn(new ResourceReadPolicy(
                AlcanceCrm.PROPIOS_O_ASIGNADOS, readable, Set.of(), Set.of()));
        return authorization;
    }

    private static Empresa company(String name) {
        return companyWithPhone(name, null);
    }

    private static Empresa companyWithPhone(String name, String phone) {
        return Empresa.create(name, "retail", phone, null, null, null, null,
                EstadoRelacion.ACTIVO, null, null, null);
    }
}