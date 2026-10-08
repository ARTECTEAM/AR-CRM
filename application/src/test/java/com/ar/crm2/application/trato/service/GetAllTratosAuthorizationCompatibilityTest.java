package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.shared.query.ListPageRequest;
import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.application.trato.query.TratoFilterCriteria;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetAllTratosAuthorizationCompatibilityTest {

    @Test
    void filtersDealsAndUnreadableContactsBeforeComputingMainPageTotals() {
        Trato first = deal("First");
        Trato hidden = deal("Hidden");
        Trato second = deal("Second");
        TratoFilterCriteria criteria = criteria(new ListPageRequest(1, 1, "nombre", ListPageRequest.SortDirection.ASC));
        FindAllTratosPort rows = mock(FindAllTratosPort.class);
        CrmAuthorization authorization = authorization(Set.of(GrupoCampoSensible.FINANCIERO));
        when(rows.findAll(criteria)).thenReturn(List.of(first, hidden, second));
        when(authorization.permitsRecord(RecursoCrm.TRATO, AccionCrm.LEER, first.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, first.getContactoId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.TRATO, AccionCrm.LEER, hidden.getId().value())).thenReturn(false);
        when(authorization.permitsRecord(RecursoCrm.TRATO, AccionCrm.LEER, second.getId().value())).thenReturn(true);
        when(authorization.permitsRecord(RecursoCrm.CONTACTO, AccionCrm.LEER, second.getContactoId().value())).thenReturn(true);

        var page = new GetAllTratosService(authorization, rows).getPage(criteria);

        assertEquals(List.of(second), page.items());
        assertEquals(2, page.totalItems());
        assertEquals(1, page.page());
        assertEquals(1, page.pageSize());
        assertEquals(2, page.totalPages());
        assertFalse(page.hasNext());
        assertTrue(page.hasPrevious());
        verify(rows, never()).findPage(criteria);
    }

    @Test
    void requiresReadPermissionBeforeFetchingRows() {
        FindAllTratosPort rows = mock(FindAllTratosPort.class);
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        doThrow(new com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException("denied"))
                .when(authorization).require(RecursoCrm.TRATO, AccionCrm.LEER);

        assertThrows(com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException.class,
                () -> new GetAllTratosService(authorization, rows).getAll(TratoFilterCriteria.empty()));
        verifyNoInteractions(rows);
    }

    @Test
    void deniesFinancialFiltersWithoutFinancialReadGrantBeforeQuerying() {
        FindAllTratosPort rows = mock(FindAllTratosPort.class);
        CrmAuthorization authorization = authorization(Set.of());
        TratoFilterCriteria criteria = new TratoFilterCriteria(null, null, null, null, null,
                BigDecimal.ONE, null, TratoFilterCriteria.CierreEsperadoFilter.TODAS);

        assertThrows(com.ar.crm2.application.security.exception.CrmAuthorizationDeniedException.class,
                () -> new GetAllTratosService(authorization, rows).getAll(criteria));
        verifyNoInteractions(rows);
    }

    private static CrmAuthorization authorization(Set<GrupoCampoSensible> readable) {
        CrmAuthorization authorization = mock(CrmAuthorization.class);
        when(authorization.readPolicy(RecursoCrm.TRATO)).thenReturn(new ResourceReadPolicy(
                AlcanceCrm.TODO_COMPARTIDO, readable, Set.of(), Set.of()));
        return authorization;
    }

    private static TratoFilterCriteria criteria(ListPageRequest request) {
        return new TratoFilterCriteria(null, null, null, null, null, null, null,
                TratoFilterCriteria.CierreEsperadoFilter.TODAS, request);
    }

    private static Trato deal(String name) {
        return Trato.create(ContactoId.create(), UsuarioId.create(), name, null, null, null, null);
    }
}