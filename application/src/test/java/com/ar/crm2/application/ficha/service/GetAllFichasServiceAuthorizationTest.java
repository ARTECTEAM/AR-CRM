package com.ar.crm2.application.ficha.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.ficha.port.out.FindAllFichasPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.entity.FichaEtiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.EtiquetaId;
import com.ar.crm2.model.vo.TratoId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetAllFichasServiceAuthorizationTest {

    @Test
    void getAllHidesCardWhenItsLinkedDealIsNotReadable() {
        Ficha ficha = Ficha.create(ColumnaId.create(), TipoFicha.TRATO, TratoId.create(), null);
        FindAllFichasPort findAll = () -> List.of(ficha);
        CrmAuthorization authorization = new CrmAuthorization() {
            @Override
            public void require(RecursoCrm recurso, AccionCrm accion) {
            }

            @Override
            public boolean permitsRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
                return recurso == RecursoCrm.FICHA || recurso == RecursoCrm.COLUMNA;
            }

            @Override
            public void requireRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
                throw new IllegalStateException("Not used by this list operation");
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
        GetAllFichasService service = new GetAllFichasService(authorization, findAll);

        assertEquals(List.of(), service.getAll());
    }

    @Test
    void getAllHidesCardWhenItsLabelIsNotReadable() {
        Ficha ficha = Ficha.create(ColumnaId.create(), TipoFicha.TRATO, TratoId.create(), null)
            .withEtiquetas(List.of(FichaEtiqueta.create(EtiquetaId.create(), TipoEtiqueta.TRATO)));
        FindAllFichasPort findAll = () -> List.of(ficha);
        CrmAuthorization authorization = new CrmAuthorization() {
            @Override
            public void require(RecursoCrm recurso, AccionCrm accion) {
            }

            @Override
            public boolean permitsRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
                return recurso != RecursoCrm.ETIQUETA;
            }

            @Override
            public void requireRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
                throw new IllegalStateException("Not used by this list operation");
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
        GetAllFichasService service = new GetAllFichasService(authorization, findAll);

        assertEquals(List.of(), service.getAll());
    }
}
