package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.support.TestCrmAuthorization;
import com.ar.crm2.application.trato.port.out.FindAllTratosPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetAllTratosServiceAuthorizationTest {

    @Test
    void getAll_returnsOnlyRowsPermittedByCurrentRoleScope() {
        Trato visible = Trato.create(
            ContactoId.create(), UsuarioId.create(), "Visible", null, null, null, null
        );
        Trato hidden = Trato.create(
            ContactoId.create(), UsuarioId.create(), "Hidden", null, null, null, null
        );
        FindAllTratosPort findAll = () -> List.of(visible, hidden);
        CrmAuthorization authorization = new CrmAuthorization() {
            @Override
            public void require(RecursoCrm recurso, AccionCrm accion) {
                assertEquals(RecursoCrm.TRATO, recurso);
                assertEquals(AccionCrm.LEER, accion);
            }

            @Override
            public boolean permitsRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
                return recurso == RecursoCrm.TRATO
                    ? visible.getId().value().equals(recordId)
                    : recurso == RecursoCrm.CONTACTO;
            }

            @Override
            public void requireRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
                if (!permitsRecord(recurso, accion, recordId)) {
                    throw new IllegalStateException("Denied");
                }
            }

            @Override
            public ResourceReadPolicy readPolicy(RecursoCrm recurso) {
                return null;
            }

            @Override
            public ResourceReadPolicy fieldPolicy(RecursoCrm recurso) {
                return null;
            }

            @Override
            public void requireWritableGroups(RecursoCrm recurso, Set<com.ar.crm2.model.autorizacion.GrupoCampoSensible> groups) {
            }

            @Override
            public String revision() {
                return "test";
            }
        };

        GetAllTratosService service = new GetAllTratosService(authorization, findAll);

        assertEquals(List.of(visible), service.getAll());
    }

    @Test
    void getAllHidesDealsWithUnreadableContactIds() {
        UUID contactoId = UUID.randomUUID();
        Trato deal = Trato.create(
            ContactoId.from(contactoId), UsuarioId.create(), "Deal", null, null, null, null
        );
        FindAllTratosPort findAll = () -> List.of(deal);
        GetAllTratosService service = new GetAllTratosService(
            new TestCrmAuthorization().denyRecord(contactoId), findAll);

        assertEquals(List.of(), service.getAll());
    }
}
