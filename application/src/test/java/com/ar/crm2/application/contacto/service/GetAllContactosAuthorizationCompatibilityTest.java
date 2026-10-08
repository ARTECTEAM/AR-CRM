package com.ar.crm2.application.contacto.service;

import com.ar.crm2.application.contacto.port.out.FindAllContactosPort;
import com.ar.crm2.application.contacto.query.ContactoFilterCriteria;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Contacto;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAllContactosAuthorizationCompatibilityTest {

    @Test
    void deniesListingBeforeQueryWhenReadIsNotGranted() {
        FindAllContactosPort rows = mock(FindAllContactosPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new CrmAuthorizationDeniedException("denied"))
                .when(authorization).require(RecursoCrm.CONTACTO, AccionCrm.LEER);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new GetAllContactosService(authorization, rows).getAll(ContactoFilterCriteria.empty()));
        verifyNoInteractions(rows);
    }

    @Test
    void filtersContactAndCompanyScopeBeforeComputingPageTotals() {
        ContactoFilterCriteria criteria = new ContactoFilterCriteria(null, null, null, null, null,
                new ListPageRequest(0, 1, "nombre", ListPageRequest.SortDirection.ASC));
        Contacto deniedParent = contact("Denied parent", "parent@example.test", EmpresaId.create());
        Contacto visible = contact("Visible", "visible@example.test", EmpresaId.create());
        FindAllContactosPort rows = mock(FindAllContactosPort.class);
        CrmAuthorization authorization = authorization(Set.of(GrupoCampoSensible.CONTACTO_PRIVADO));
        when(rows.findAll(criteria)).thenReturn(List.of(deniedParent, visible));
        when(authorization.permitsRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, deniedParent.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, deniedParent.getEmpresaId().value())).thenReturn(false);
        when(authorization.permitsRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, visible.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, visible.getEmpresaId().value())).thenReturn(true);

        var page = new GetAllContactosService(authorization, rows).getPage(criteria);

        assertEquals(List.of(visible), page.items());
        assertEquals(1, page.totalItems());
        assertEquals(1, page.totalPages());
        verify(rows).findAll(criteria);
        verify(rows, never()).findPage(criteria);
    }

    @Test
    void excludesEmailOrPhoneOnlySearchMatchesWithoutPrivateReadGrant() {
        ContactoFilterCriteria criteria = new ContactoFilterCriteria("secret-match", null, null, null, null);
        Contacto matchedByPrivateField = contact("Public name", "secret-match@example.test", EmpresaId.create());
        FindAllContactosPort rows = mock(FindAllContactosPort.class);
        when(rows.findAll(criteria)).thenReturn(List.of(matchedByPrivateField));

        CrmAuthorization authorization = authorization(Set.of());
        when(authorization.permitsRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, matchedByPrivateField.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.EMPRESA, AccionCrm.LEER, matchedByPrivateField.getEmpresaId().value())).thenReturn(true);

        var result = new GetAllContactosService(authorization, rows).getAll(criteria);

        assertTrue(result.isEmpty(), "search must not reveal contact membership through hidden correo/telefono");
    }

    @Test
    void deniesSortingByPrivateEmailWithoutPrivateReadGrantBeforeQuery() {
        ContactoFilterCriteria criteria = new ContactoFilterCriteria(null, null, null, null, null,
                new ListPageRequest(0, 10, "correo", ListPageRequest.SortDirection.ASC));
        FindAllContactosPort rows = mock(FindAllContactosPort.class);

        assertThrows(CrmAuthorizationDeniedException.class,
                () -> new GetAllContactosService(authorization(Set.of()), rows).getPage(criteria));
        verifyNoInteractions(rows);
    }

    private static CrmAuthorization authorization(Set<GrupoCampoSensible> readable) {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        when(authorization.readPolicy(RecursoCrm.CONTACTO)).thenReturn(new ResourceReadPolicy(
                AlcanceCrm.PROPIOS_O_ASIGNADOS, readable, Set.of(), Set.of()));
        return authorization;
    }

    private static Contacto contact(String name, String email, EmpresaId empresaId) {
        return Contacto.create(empresaId, name, email, EstadoRelacion.ACTIVO, null, null,
                "555-secret", "manager", "referral");
    }
}