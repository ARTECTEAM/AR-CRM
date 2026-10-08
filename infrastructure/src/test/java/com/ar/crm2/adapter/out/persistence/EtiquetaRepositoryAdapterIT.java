package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.EtiquetaEntity;
import com.ar.crm2.adapter.out.persistence.entity.FichaEntity;
import com.ar.crm2.adapter.out.persistence.entity.FichaEtiquetaEntity;
import com.ar.crm2.adapter.out.persistence.repository.EtiquetaRepository;
import com.ar.crm2.adapter.out.persistence.repository.FichaEtiquetaRepository;
import com.ar.crm2.adapter.out.persistence.repository.FichaRepository;
import com.ar.crm2.application.etiqueta.exception.EtiquetaRequiresConfirmationException;
import com.ar.crm2.application.etiqueta.service.DeleteEtiquetaService;
import com.ar.crm2.application.security.CrmAuthorization;
import com.ar.crm2.model.entity.Etiqueta;
import com.ar.crm2.model.enums.TipoEtiqueta;
import com.ar.crm2.model.enums.TipoFicha;
import com.ar.crm2.model.vo.EtiquetaId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link EtiquetaRepositoryAdapter} against H2 in-memory
 * database, wiring the adapter as a Spring bean through {@code @Import}.
 *
 * <p>Validates the contract that the application services rely on:
 * <ul>
 *   <li>id-aware uniqueness check (the create-flow / edit-flow contract of
 *       {@code ExistsEtiquetaByNombreAndTipoPort})</li>
 *   <li>find-all filter by type</li>
 *   <li>findByIds and countByEtiquetaId round-trip</li>
 *   <li>cascade-delete atomicity: when the catalog row is deleted, every
 *       FichaEtiqueta row that referenced it is also removed (FK + explicit
 *       bulk-delete, both inside the same Spring transaction at the
 *       adapter boundary)</li>
 *   <li>unique constraint violation surfaces as
 *       {@link org.springframework.dao.DataIntegrityViolationException} with
 *       a clear message (final safety net behind the application pre-check)</li>
 * </ul>
 */
@DataJpaTest
@Import(EtiquetaRepositoryAdapter.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EtiquetaRepositoryAdapterIT {

    @Autowired
    private EtiquetaRepositoryAdapter adapter;

    @Autowired
    private EtiquetaRepository etiquetaRepository;

    @Autowired
    private FichaEtiquetaRepository fichaEtiquetaRepository;

    @Autowired
    private FichaRepository fichaRepository;

    // ── Helpers ─────────────────────────────────────────────────────

    private EtiquetaEntity saveEtiqueta(String nombre, TipoEtiqueta tipo) {
        EtiquetaEntity entity = EtiquetaEntity.builder()
            .id(UUID.randomUUID().toString())
            .nombre(nombre)
            .tipoEtiqueta(tipo)
            .color("#FFFFFF")
            .creadoEn(LocalDateTime.now())
            .build();
        return etiquetaRepository.saveAndFlush(entity);
    }

    private EtiquetaEntity saveEtiquetaWithId(String id, String nombre, TipoEtiqueta tipo) {
        return etiquetaRepository.saveAndFlush(
            EtiquetaEntity.builder()
                .id(id)
                .nombre(nombre)
                .tipoEtiqueta(tipo)
                .color("#FFFFFF")
                .creadoEn(LocalDateTime.now())
                .build()
        );
    }

    private FichaEntity saveFichaWithEtiqueta(String etiquetaId, TipoEtiqueta tipo) {
        String fichaId = UUID.randomUUID().toString();
        FichaEntity ficha = FichaEntity.builder()
            .id(fichaId)
            .columnaId(UUID.randomUUID().toString())
            .tipoFicha(TipoFicha.TAREA)
            .tratoId(null)
            .tareaId(UUID.randomUUID().toString())
            .actualizadoEn(Instant.now())
            .etiquetas(new ArrayList<>())
            .build();
        ficha.getEtiquetas().add(FichaEtiquetaEntity.builder()
            .id(UUID.randomUUID().toString())
            .ficha(ficha)
            .etiquetaId(etiquetaId)
            .tipoEtiqueta(tipo)
            .build());
        return fichaRepository.saveAndFlush(ficha);
    }

    // ── ExistsEtiquetaByNombreAndTipoPort ─────────────────────────

    @Test
    void exists_withNullExcludeId_shouldTreatAsCreateFlowCheck() {
        saveEtiqueta("Urgent", TipoEtiqueta.TAREA);
        assertTrue(adapter.exists("Urgent", TipoEtiqueta.TAREA, null));
    }

    @Test
    void exists_withExcludeId_shouldIgnoreThatRow() {
        EtiquetaEntity other = saveEtiqueta("Other", TipoEtiqueta.TAREA);
        EtiquetaEntity editing = saveEtiqueta("Urgent", TipoEtiqueta.TAREA);

        // Exclude the row being edited → no other row matches → false
        assertFalse(adapter.exists("Urgent", TipoEtiqueta.TAREA, EtiquetaId.from(UUID.fromString(editing.getId()))));
        // Without exclude → match (the catalog has it)
        assertTrue(adapter.exists("Urgent", TipoEtiqueta.TAREA, null));
        // Without exclude → the "Other" row's name still matches
        assertTrue(adapter.exists("Other", TipoEtiqueta.TAREA, null));
        // Excluding the "Other" row → no other row matches → false
        assertFalse(adapter.exists("Other", TipoEtiqueta.TAREA, EtiquetaId.from(UUID.fromString(other.getId()))));
    }

    @Test
    void exists_withDifferentTipo_shouldReturnFalse() {
        saveEtiqueta("Urgent", TipoEtiqueta.TAREA);
        assertFalse(adapter.exists("Urgent", TipoEtiqueta.TRATO, null));
    }

    // ── FindAllEtiquetasPort ──────────────────────────────────────

    @Test
    void findAll_withoutFilter_shouldReturnEverything() {
        saveEtiqueta("A", TipoEtiqueta.TAREA);
        saveEtiqueta("B", TipoEtiqueta.TRATO);
        saveEtiqueta("C", TipoEtiqueta.TAREA);

        List<Etiqueta> all = adapter.findAll(Optional.empty());

        assertEquals(3, all.size());
    }

    @Test
    void findAll_withFilter_shouldReturnOnlyMatchingTipo() {
        saveEtiqueta("A", TipoEtiqueta.TAREA);
        saveEtiqueta("B", TipoEtiqueta.TRATO);
        saveEtiqueta("C", TipoEtiqueta.TAREA);

        List<Etiqueta> tareas = adapter.findAll(Optional.of(TipoEtiqueta.TAREA));

        assertEquals(2, tareas.size());
        assertTrue(tareas.stream().allMatch(e -> e.getTipoEtiqueta() == TipoEtiqueta.TAREA));
    }

    // ── FindEtiquetasByIdsPort ─────────────────────────────────────

    @Test
    void findByIds_shouldReturnAllMatchingRows() {
        EtiquetaEntity a = saveEtiqueta("A", TipoEtiqueta.TAREA);
        EtiquetaEntity b = saveEtiqueta("B", TipoEtiqueta.TAREA);
        saveEtiqueta("C", TipoEtiqueta.TRATO);

        List<Etiqueta> result = adapter.findByIds(List.of(
            EtiquetaId.from(UUID.fromString(a.getId())),
            EtiquetaId.from(UUID.fromString(b.getId()))
        ));

        assertEquals(2, result.size());
    }

    // ── CountFichaEtiquetasByEtiquetaIdPort ───────────────────────

    @Test
    void countByEtiquetaId_shouldReturnNumberOfFichasReferencing() {
        EtiquetaEntity e = saveEtiqueta("E", TipoEtiqueta.TAREA);
        saveFichaWithEtiqueta(e.getId(), TipoEtiqueta.TAREA);
        saveFichaWithEtiqueta(e.getId(), TipoEtiqueta.TAREA);

        long count = adapter.countByEtiquetaId(EtiquetaId.from(UUID.fromString(e.getId())));

        assertEquals(2L, count);
    }

    @Test
    void findFichaIdsByEtiquetaId_shouldReturnOnlyLinkedFichaIds() {
        EtiquetaEntity etiqueta = saveEtiqueta("E", TipoEtiqueta.TAREA);
        FichaEntity linkedFicha = saveFichaWithEtiqueta(etiqueta.getId(), TipoEtiqueta.TAREA);
        EtiquetaEntity otherEtiqueta = saveEtiqueta("Other", TipoEtiqueta.TAREA);
        saveFichaWithEtiqueta(otherEtiqueta.getId(), TipoEtiqueta.TAREA);

        List<UUID> fichaIds = adapter.findFichaIdsByEtiquetaId(
                EtiquetaId.from(UUID.fromString(etiqueta.getId())));

        assertEquals(List.of(UUID.fromString(linkedFicha.getId())), fichaIds);
    }

    // ── DeleteEtiquetaByIdPort (cascade atomicity) ────────────────

    @Test
    void deleteById_shouldCascadeDeleteAllRelationsAndCatalogRowAtomically() {
        EtiquetaEntity e = saveEtiqueta("E", TipoEtiqueta.TAREA);
        saveFichaWithEtiqueta(e.getId(), TipoEtiqueta.TAREA);
        saveFichaWithEtiqueta(e.getId(), TipoEtiqueta.TAREA);
        saveFichaWithEtiqueta(e.getId(), TipoEtiqueta.TAREA);

        // Sanity: 3 relation rows
        assertEquals(3L, fichaEtiquetaRepository.countByEtiquetaId(e.getId()));

        adapter.deleteById(EtiquetaId.from(UUID.fromString(e.getId())));

        // The catalog row is gone
        assertTrue(etiquetaRepository.findById(e.getId()).isEmpty());
        // All relation rows are gone too (atomic cascade)
        assertEquals(0L, fichaEtiquetaRepository.countByEtiquetaId(e.getId()));
    }

    @Test
    void deleteById_withNoRelations_shouldOnlyRemoveCatalogRow() {
        EtiquetaEntity e = saveEtiqueta("E", TipoEtiqueta.TAREA);

        adapter.deleteById(EtiquetaId.from(UUID.fromString(e.getId())));

        assertTrue(etiquetaRepository.findById(e.getId()).isEmpty());
    }

    // ── SaveEtiquetaPort: unique constraint safety net ─────────────

    @Test
    void save_duplicateNombreAndTipo_shouldRaiseDataIntegrityViolation() {
        saveEtiqueta("Urgent", TipoEtiqueta.TAREA);
        Etiqueta duplicate = Etiqueta.reconstitute(
            EtiquetaId.create(), "Urgent", TipoEtiqueta.TAREA, "#FF0000", LocalDateTime.now());

        org.springframework.dao.DataIntegrityViolationException thrown = assertThrows(
            org.springframework.dao.DataIntegrityViolationException.class,
            () -> adapter.save(duplicate)
        );

        assertNotNull(thrown.getMessage());
        assertTrue(thrown.getMessage().contains("Ya existe una etiqueta con el mismo nombre y tipo"));
    }

    // ── Application service integration smoke (sanity) ────────────

    @Test
    void deleteEtiquetaService_withAdapterWired_shouldRejectUnconfirmedDeleteWhenInUse() {
        // Sanity check: the application service (built inline with the
        // adapter as its ports) honors the confirm=false contract.
        EtiquetaEntity e = saveEtiqueta("E", TipoEtiqueta.TAREA);
        saveFichaWithEtiqueta(e.getId(), TipoEtiqueta.TAREA);

        DeleteEtiquetaService service = new DeleteEtiquetaService(
            adapter, adapter, adapter, adapter, adapter, org.mockito.Mockito.mock(CrmAuthorization.class));

        assertThrows(EtiquetaRequiresConfirmationException.class, () ->
            service.delete(new com.ar.crm2.application.etiqueta.command.DeleteEtiquetaCommand(
                UUID.fromString(e.getId()), false))
        );
    }

    // ── SaveFicha round-trip through the Ficha persistence adapter ─

    @Test
    void saveFicha_throughFichaRepository_shouldPersistChildEtiquetasThroughCascade() {
        // Slice 2 keeps SaveFichaPort on FichaRepositoryAdapter (single Spring
        // tx for the Ficha aggregate), not on EtiquetaRepositoryAdapter. This
        // test exercises the FichaEntity + FichaEtiquetaEntity one-to-many
        // cascade directly, which is what backs the Ficha save flow.
        EtiquetaEntity e1 = saveEtiqueta("A", TipoEtiqueta.TAREA);
        EtiquetaEntity e2 = saveEtiqueta("B", TipoEtiqueta.TAREA);

        FichaEntity ficha = FichaEntity.builder()
            .id(UUID.randomUUID().toString())
            .columnaId(UUID.randomUUID().toString())
            .tipoFicha(TipoFicha.TAREA)
            .tratoId(null)
            .tareaId(UUID.randomUUID().toString())
            .actualizadoEn(Instant.now())
            .etiquetas(new ArrayList<>())
            .build();
        ficha.getEtiquetas().add(FichaEtiquetaEntity.builder()
            .id(UUID.randomUUID().toString())
            .ficha(ficha)
            .etiquetaId(e1.getId())
            .tipoEtiqueta(TipoEtiqueta.TAREA)
            .build());
        ficha.getEtiquetas().add(FichaEtiquetaEntity.builder()
            .id(UUID.randomUUID().toString())
            .ficha(ficha)
            .etiquetaId(e2.getId())
            .tipoEtiqueta(TipoEtiqueta.TAREA)
            .build());
        fichaRepository.saveAndFlush(ficha);

        FichaEntity reloaded = fichaRepository.findById(ficha.getId()).orElseThrow();
        assertEquals(2, reloaded.getEtiquetas().size());
        assertNotNull(reloaded.getEtiquetas().get(0).getEtiquetaId());
    }
}
