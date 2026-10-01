package com.ar.crm2.application.trato.service;

import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.application.security.ResourceScopeCandidate;
import com.ar.crm2.application.trato.command.EditTratoCommand;
import com.ar.crm2.application.trato.port.out.FindTratoByIdPort;
import com.ar.crm2.application.trato.port.out.SaveTratoPort;
import com.ar.crm2.model.autorizacion.AccionCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.entity.Trato;
import com.ar.crm2.model.enums.TipoContrato;
import com.ar.crm2.model.vo.ContactoId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EditTratoSensitiveFieldsAuthorizationTest {

    @Test
    void editWithoutFinancialInputsPreservesHiddenFinancialValues() {
        UUID responsableId = UUID.randomUUID();
        Trato existing = Trato.create(
            ContactoId.create(),
            UsuarioId.from(responsableId),
            "Before",
            new BigDecimal("1250.00"),
            75,
            LocalDate.of(2026, 11, 15),
            TipoContrato.values()[0]
        );
        AtomicReference<Trato> saved = new AtomicReference<>();
        FindTratoByIdPort find = id -> Optional.of(existing);
        SaveTratoPort save = trato -> {
            saved.set(trato);
            return trato;
        };
        CrmAuthorization authorization = new CrmAuthorization() {
            @Override
            public void require(RecursoCrm recurso, AccionCrm accion) {
            }

            @Override
            public boolean permitsRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
                return true;
            }

            @Override
            public void requireRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
            }

            @Override
            public void requireCandidate(RecursoCrm recurso, AccionCrm accion, ResourceScopeCandidate candidate) {
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
                throw new SecurityException("Financial writes are not allowed");
            }

            @Override
            public String revision() {
                return "test";
            }
        };
        EditTratoService service = new EditTratoService(authorization, find, save);

        Trato updated = service.edit(new EditTratoCommand(
            existing.getId().value(), responsableId, "After", null, null, null, TipoContrato.values()[0]
        ));

        assertEquals(new BigDecimal("1250.00"), updated.getValorEstimado());
        assertEquals(75, updated.getProbabilidad());
        assertEquals(LocalDate.of(2026, 11, 15), updated.getFechaCierreEsperada());
        assertEquals(updated, saved.get());
    }

    @Test
    void editWithAnExplicitFinancialInputRequiresFinancialWriteAccess() {
        UUID responsableId = UUID.randomUUID();
        Trato existing = Trato.create(
            ContactoId.create(), UsuarioId.from(responsableId), "Before", null, null, null, TipoContrato.values()[0]
        );
        AtomicReference<Trato> saved = new AtomicReference<>();
        FindTratoByIdPort find = id -> Optional.of(existing);
        SaveTratoPort save = trato -> {
            saved.set(trato);
            return trato;
        };
        CrmAuthorization authorization = new CrmAuthorization() {
            @Override
            public void require(RecursoCrm recurso, AccionCrm accion) {
            }

            @Override
            public boolean permitsRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
                return true;
            }

            @Override
            public void requireRecord(RecursoCrm recurso, AccionCrm accion, UUID recordId) {
            }

            @Override
            public void requireCandidate(RecursoCrm recurso, AccionCrm accion, ResourceScopeCandidate candidate) {
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
                throw new SecurityException("Financial writes are not allowed");
            }

            @Override
            public String revision() {
                return "test";
            }
        };
        EditTratoService service = new EditTratoService(authorization, find, save);

        assertThrows(SecurityException.class, () -> service.edit(new EditTratoCommand(
            existing.getId().value(), responsableId, "After", new BigDecimal("25.00"), null, null, TipoContrato.values()[0]
        )));
        assertEquals(null, saved.get());
    }
}
