package com.ar.crm2.adapter.out.persistence.mapper;

import com.ar.crm2.adapter.out.persistence.entity.FichaEntity;
import com.ar.crm2.adapter.out.persistence.entity.FichaEtiquetaEntity;
import com.ar.crm2.model.entity.Ficha;
import com.ar.crm2.model.entity.FichaEtiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.EtiquetaId;
import com.ar.crm2.model.vo.FichaId;
import com.ar.crm2.model.vo.TareaId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FichaMapper}.
 *
 * <p>The mapper is a pure static utility, so no mocks are needed. These tests
 * cover the slice-4 corrective: the {@code FichaEtiquetaEntity} relation row
 * MUST own its own UUID technical id and MUST NOT reuse/copy the catalog
 * {@code etiquetaId} into the row {@code id}. The same contract is asserted
 * for {@code ColumnaTableroEntity} in {@code TableroMapperTest}.
 */
class FichaMapperTest {

    private static final UUID COLUMNA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    // ── Helpers ──────────────────────────────────────────────────────

    private static Ficha domainFichaWithEtiquetas(List<FichaEtiqueta> etiquetas) {
        return Ficha.reconstitute(
            FichaId.create(),
            ColumnaId.from(COLUMNA_ID),
            TipoFicha.TAREA,
            null,
            TareaId.create(),
            Instant.now(),
            etiquetas
        );
    }

    // ── toEntity: child row id ownership ─────────────────────────────

    @Nested
    @DisplayName("toEntity() — child row id ownership")
    class ToEntityChildIdOwnership {

        @Test
        @DisplayName("generates a fresh UUID for the child row id (does not copy etiquetaId)")
        void toEntity_childRowId_isGeneratedUuidNotEqualToEtiquetaId() {
            UUID etiquetaCatalogId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
            FichaEtiqueta fe = FichaEtiqueta.create(EtiquetaId.from(etiquetaCatalogId), TipoEtiqueta.TAREA);

            FichaEntity entity = FichaMapper.toEntity(domainFichaWithEtiquetas(List.of(fe)));

            assertThat(entity.getEtiquetas()).hasSize(1);
            FichaEtiquetaEntity row = entity.getEtiquetas().get(0);
            assertThat(row.getId())
                .as("child row id must be its own generated UUID, never a copy of etiquetaId")
                .isNotNull()
                .isNotEqualTo(etiquetaCatalogId.toString());
        }

        @Test
        @DisplayName("each child row gets a distinct generated UUID even when the catalog ids repeat")
        void toEntity_eachChildRow_getsDistinctUuid() {
            UUID e1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
            UUID e2 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb2");
            UUID e3 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb3");
            List<FichaEtiqueta> etiquetas = List.of(
                FichaEtiqueta.create(EtiquetaId.from(e1), TipoEtiqueta.TAREA),
                FichaEtiqueta.create(EtiquetaId.from(e2), TipoEtiqueta.TAREA),
                FichaEtiqueta.create(EtiquetaId.from(e3), TipoEtiqueta.TAREA)
            );

            FichaEntity entity = FichaMapper.toEntity(domainFichaWithEtiquetas(etiquetas));

            assertThat(entity.getEtiquetas()).hasSize(3);
            List<String> childIds = entity.getEtiquetas().stream().map(FichaEtiquetaEntity::getId).toList();
            assertThat(childIds)
                .as("each child row must own a distinct UUID; no id is reused across siblings")
                .doesNotContainNull()
                .doesNotHaveDuplicates()
                .allSatisfy(id -> {
                    assertThat(id).isNotEqualTo(e1.toString());
                    assertThat(id).isNotEqualTo(e2.toString());
                    assertThat(id).isNotEqualTo(e3.toString());
                });
        }

        @Test
        @DisplayName("child row id is a valid UUID string")
        void toEntity_childRowId_isValidUuid() {
            FichaEtiqueta fe = FichaEtiqueta.create(EtiquetaId.create(), TipoEtiqueta.TAREA);

            FichaEntity entity = FichaMapper.toEntity(domainFichaWithEtiquetas(List.of(fe)));

            String childId = entity.getEtiquetas().get(0).getId();
            assertThat(childId).isNotNull();
            // Throws if not a valid UUID
            UUID.fromString(childId);
        }

        @Test
        @DisplayName("preserves etiquetaId and tipoEtiqueta as separate fields (catalog linkage is intact)")
        void toEntity_preservesEtiquetaIdAndTipoAsSeparateFields() {
            UUID etiquetaCatalogId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
            FichaEtiqueta fe = FichaEtiqueta.create(EtiquetaId.from(etiquetaCatalogId), TipoEtiqueta.TAREA);

            FichaEntity entity = FichaMapper.toEntity(domainFichaWithEtiquetas(List.of(fe)));

            FichaEtiquetaEntity row = entity.getEtiquetas().get(0);
            assertThat(row.getEtiquetaId()).isEqualTo(etiquetaCatalogId.toString());
            assertThat(row.getTipoEtiqueta()).isEqualTo(TipoEtiqueta.TAREA);
            // And, critically, the row id is NOT the catalog id
            assertThat(row.getId()).isNotEqualTo(row.getEtiquetaId());
        }

        @Test
        @DisplayName("reuses persisted child row id when existingChildRowIds contains etiquetaId")
        void toEntity_existingChildRowIds_preservesPersistedRowId() {
            UUID etiquetaCatalogId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
            String persistedRowId = UUID.fromString("99999999-9999-9999-9999-999999999991").toString();
            FichaEtiqueta fe = FichaEtiqueta.create(EtiquetaId.from(etiquetaCatalogId), TipoEtiqueta.TAREA);

            FichaEntity entity = FichaMapper.toEntity(
                domainFichaWithEtiquetas(List.of(fe)),
                java.util.Map.of(etiquetaCatalogId.toString(), persistedRowId)
            );

            assertThat(entity.getEtiquetas()).hasSize(1);
            assertThat(entity.getEtiquetas().get(0).getId())
                .as("existing etiqueta relation must keep its persisted row id on updates")
                .isEqualTo(persistedRowId);
        }

        @Test
        @DisplayName("generates UUID for new child when existingChildRowIds lacks etiquetaId")
        void toEntity_existingChildRowIds_newChildGeneratesUuid() {
            UUID existingEtiquetaId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1");
            UUID newEtiquetaId = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2");
            String persistedRowId = UUID.fromString("99999999-9999-9999-9999-999999999992").toString();
            List<FichaEtiqueta> etiquetas = List.of(
                FichaEtiqueta.create(EtiquetaId.from(existingEtiquetaId), TipoEtiqueta.TAREA),
                FichaEtiqueta.create(EtiquetaId.from(newEtiquetaId), TipoEtiqueta.TAREA)
            );

            FichaEntity entity = FichaMapper.toEntity(
                domainFichaWithEtiquetas(etiquetas),
                java.util.Map.of(existingEtiquetaId.toString(), persistedRowId)
            );

            FichaEtiquetaEntity existingRow = entity.getEtiquetas().get(0);
            FichaEtiquetaEntity newRow = entity.getEtiquetas().get(1);

            assertThat(existingRow.getId()).isEqualTo(persistedRowId);
            assertThat(newRow.getId())
                .isNotNull()
                .isNotEqualTo(persistedRowId)
                .isNotEqualTo(newEtiquetaId.toString());
            UUID.fromString(newRow.getId());
        }
    }

    // ── toEntity: other fields ──────────────────────────────────────

    @Test
    @DisplayName("handles a ficha with empty etiquetas list")
    void toEntity_handlesEmptyEtiquetas() {
        FichaEntity entity = FichaMapper.toEntity(domainFichaWithEtiquetas(List.of()));

        assertThat(entity.getEtiquetas()).isEmpty();
    }

    @Test
    @DisplayName("handles null domain")
    void toEntity_handlesNullDomain() {
        assertThat(FichaMapper.toEntity(null)).isNull();
    }
}
