package com.ar.crm2.adapter.in.rest.projection;

import com.ar.crm2.adapter.out.ai.tool.dto.output.ResourceToolOutput;
import com.ar.crm2.adapter.out.ai.tool.dto.output.TableroOutput;
import com.ar.crm2.application.security.ResourceReadPolicy;
import com.ar.crm2.model.autorizacion.AlcanceCrm;
import com.ar.crm2.model.autorizacion.GrupoCampoSensible;
import com.ar.crm2.model.autorizacion.RecursoCrm;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CrmSensitiveFieldProjectorTest {

    @Test
    void hidesDealAndNestedBoardFinancialValuesWithoutSubstitutingZero() {
        ResourceReadPolicy hidden = policy(Set.of());
        ResourceToolOutput.Deal deal = new ResourceToolOutput.Deal("id", "deal", "OPEN", null,
                "SERVICE", new BigDecimal("100.00"), 50, "2026-12-31");
        ResourceToolOutput.Deal projectedDeal = CrmSensitiveFieldProjector.project(deal, RecursoCrm.TRATO, hidden);
        assertThat(projectedDeal.nombre()).isEqualTo("deal");
        assertThat(projectedDeal.valorEstimado()).isNull();
        assertThat(projectedDeal.probabilidad()).isNull();
        assertThat(projectedDeal.fechaCierreEsperada()).isNull();

        TableroOutput board = new TableroOutput("id", "board", null, "TRATOS",
                List.of(new TableroOutput.ColumnaAssignment("column", "TRATOS", 10, null,
                        new BigDecimal("100.00"))), 1, false);
        TableroOutput projectedBoard = CrmSensitiveFieldProjector.project(board, RecursoCrm.TABLERO, hidden);
        assertThat(projectedBoard.columnas()).singleElement()
                .extracting(TableroOutput.ColumnaAssignment::totalValorEstimado).isNull();
    }

    @Test
    void fieldGroupsRestoreOnlyTheirConfiguredValues() {
        ResourceToolOutput.Deal deal = new ResourceToolOutput.Deal("id", "deal", "OPEN", null,
                "SERVICE", new BigDecimal("100.00"), 50, "2026-12-31");
        ResourceToolOutput.Deal projected = CrmSensitiveFieldProjector.project(
                deal, RecursoCrm.TRATO, policy(Set.of(GrupoCampoSensible.FINANCIERO)));
        assertThat(projected.valorEstimado()).isEqualByComparingTo("100.00");
        assertThat(projected.probabilidad()).isEqualTo(50);
    }

    private static ResourceReadPolicy policy(Set<GrupoCampoSensible> groups) {
        return new ResourceReadPolicy(AlcanceCrm.TODO_COMPARTIDO, groups, Set.of(), Set.of());
    }
}
