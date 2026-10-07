package com.ar.crm2.adapter.in.rest.projection;

import com.ar.crm2.adapter.in.rest.dto.response.ContactoResponse;
import com.ar.crm2.adapter.in.rest.dto.response.EmpresaResponse;
import com.ar.crm2.adapter.in.rest.dto.response.TratoResponse;
import com.ar.crm2.adapter.in.rest.dto.response.UsuarioResponse;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TableroOutput;
import com.ar.crm2.adapter.in.rest.dto.response.PageResponse;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import com.ar.crm2.model.enums.EstadoRelacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CrmSensitiveFieldProjectorPageResponseTest {

    @Test
    void masksPageItemsWithoutChangingAuthorizedPaginationMetadata() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 6, 12, 30);
        ContactoResponse item = new ContactoResponse(
            UUID.randomUUID(), UUID.randomUUID(), "Alice", "alice@example.test", EstadoRelacion.ACTIVO,
            UUID.randomUUID(), UUID.randomUUID(), "+1-555-0100", "Director", "Referral", timestamp, timestamp
        );
        PageResponse<ContactoResponse> page = new PageResponse<>(List.of(item), 41, 1, 20, 3, true, true);
        ResourceReadPolicy publicOnly = new ResourceReadPolicy(
            AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of()
        );

        PageResponse<ContactoResponse> projected = CrmSensitiveFieldProjector.project(
            page, RecursoCrm.CONTACTO, publicOnly
        );

        assertThat(projected.items()).singleElement().satisfies(contact -> {
            assertThat(contact.nombre()).isEqualTo("Alice");
            assertThat(contact.correo()).isNull();
            assertThat(contact.telefono()).isNull();
        });
        assertThat(projected.totalItems()).isEqualTo(41);
        assertThat(projected.page()).isEqualTo(1);
        assertThat(projected.pageSize()).isEqualTo(20);
        assertThat(projected.totalPages()).isEqualTo(3);
        assertThat(projected.hasNext()).isTrue();
        assertThat(projected.hasPrevious()).isTrue();
    }

    @Test
    void masksCompanyUserAndDealPageItems() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 6, 12, 30);
        ResourceReadPolicy publicOnly = publicOnly();

        EmpresaResponse company = new EmpresaResponse(UUID.randomUUID(), "Acme", null, "private phone",
                "https://example.test", null, null, null, null, null, null, "private notes", timestamp, timestamp);
        PageResponse<EmpresaResponse> companyPage = CrmSensitiveFieldProjector.project(
                page(company), RecursoCrm.EMPRESA, publicOnly);
        assertThat(companyPage.items().getFirst().nombre()).isEqualTo("Acme");
        assertThat(companyPage.items().getFirst().telefono()).isNull();
        assertThat(companyPage.items().getFirst().notas()).isNull();

        UsuarioResponse user = new UsuarioResponse(UUID.randomUUID(), "Alice", "private@example.test",
                UUID.randomUUID(), timestamp, true, "keycloak-private-id");
        PageResponse<UsuarioResponse> userPage = CrmSensitiveFieldProjector.project(
                page(user), RecursoCrm.USUARIO, publicOnly);
        assertThat(userPage.items().getFirst().nombre()).isEqualTo("Alice");
        assertThat(userPage.items().getFirst().correo()).isNull();
        assertThat(userPage.items().getFirst().keycloakId()).isNull();

        TratoResponse deal = new TratoResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Deal",
                new BigDecimal("250.00"), 80, java.time.LocalDate.of(2026, 12, 31), null, null,
                null, timestamp, timestamp);
        PageResponse<TratoResponse> dealPage = CrmSensitiveFieldProjector.project(
                page(deal), RecursoCrm.TRATO, publicOnly);
        assertThat(dealPage.items().getFirst().nombre()).isEqualTo("Deal");
        assertThat(dealPage.items().getFirst().valorEstimado()).isNull();
        assertThat(dealPage.items().getFirst().probabilidad()).isNull();
        assertThat(dealPage.items().getFirst().fechaCierreEsperada()).isNull();
    }

    @Test
    void masksNestedBoardFinancialTotalsInsidePageItems() {
        TableroOutput board = new TableroOutput("board-id", "Pipeline", null, "TRATOS",
                List.of(new TableroOutput.ColumnaAssignment("column-id", "TRATOS", 5, null,
                        new BigDecimal("900.00"))), 1, false);

        PageResponse<TableroOutput> projected = CrmSensitiveFieldProjector.project(
                page(board), RecursoCrm.TABLERO, publicOnly());

        assertThat(projected.items().getFirst().nombre()).isEqualTo("Pipeline");
        assertThat(projected.items().getFirst().columnas().getFirst().totalValorEstimado()).isNull();
        assertThat(projected.totalItems()).isEqualTo(1);
    }

    private static ResourceReadPolicy publicOnly() {
        return new ResourceReadPolicy(AlcanceCrm.TODO_COMPARTIDO, Set.of(), Set.of(), Set.of());
    }

    private static <T> PageResponse<T> page(T item) {
        return new PageResponse<>(List.of(item), 1, 0, 25, 1, false, false);
    }
}
