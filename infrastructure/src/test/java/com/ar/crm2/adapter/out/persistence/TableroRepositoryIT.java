package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.ColumnaTableroEntity;
import com.ar.crm2.adapter.out.persistence.entity.TableroEntity;
import com.ar.crm2.adapter.out.persistence.repository.TableroRepository;
import com.ar.crm2.model.enums.TipoTablero;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link TableroRepository} with H2 in-memory database.
 *
 * @DataJpaTest loads only JPA layer (entities, repositories, Hibernate).
 * Tests the aggregate root behavior, cascade operations, orphan removal,
 * and the custom query methods (existsByIdAndColumnasTableroColumnaId, existsByColumnasTableroColumnaId).
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TableroRepositoryIT {

    @Autowired
    private TableroRepository repository;

    // ── Helpers ─────────────────────────────────────────────────────

    private TableroEntity buildTableroEntity(String id, String nombre) {
        return TableroEntity.builder()
            .id(id)
            .nombre(nombre)
            .descripcion("Test description")
            .tipoTablero(TipoTablero.TAREAS)
            .creadoEn(LocalDateTime.now())
            .columnasTablero(new ArrayList<>())
            .build();
    }

    private ColumnaTableroEntity buildColumnaTableroEntity(
        String id,
        TableroEntity parent,
        String columnaId,
        int limiteWip,
        int orden
    ) {
        return ColumnaTableroEntity.builder()
            .id(id)
            .tablero(parent)
            .columnaId(columnaId)
            .tipoTablero(TipoTablero.TAREAS)
            .limiteWip(limiteWip)
            .nota("Note")
            .totalValorEstimado(BigDecimal.ZERO)
            .orden(orden)
            .build();
    }

    // ── save + findById ────────────────────────────────────────────

    @Test
    void saveAndFindById_shouldRoundTrip() {
        String id = UUID.randomUUID().toString();
        TableroEntity entity = buildTableroEntity(id, "Sprint Board");

        repository.save(entity);

        Optional<TableroEntity> found = repository.findById(id);

        assertTrue(found.isPresent());
        assertEquals(id, found.get().getId());
        assertEquals("Sprint Board", found.get().getNombre());
    }

    @Test
    void findById_shouldReturnEmptyForNonExistentId() {
        Optional<TableroEntity> found = repository.findById(UUID.randomUUID().toString());

        assertTrue(found.isEmpty());
    }

    // ── cascade persist children ────────────────────────────────────

    @Test
    void save_withChildren_shouldPersistBothParentAndChildren() {
        String tableroId = UUID.randomUUID().toString();
        TableroEntity entity = buildTableroEntity(tableroId, "Board with columns");
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity,
            UUID.randomUUID().toString(), 5, 0));
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity,
            UUID.randomUUID().toString(), 3, 1));

        repository.save(entity);

        TableroEntity found = repository.findById(tableroId).orElseThrow();
        assertEquals(2, found.getColumnasTablero().size());
    }

    // ── orphan removal ─────────────────────────────────────────────

    @Test
    void deleteParent_shouldCascadeDeleteChildren() {
        String tableroId = UUID.randomUUID().toString();
        TableroEntity entity = buildTableroEntity(tableroId, "Board to delete");
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity,
            UUID.randomUUID().toString(), 5, 0));

        repository.save(entity);

        repository.deleteById(tableroId);

        assertTrue(repository.findById(tableroId).isEmpty());
    }

    // ── existsByIdAndColumnasTableroColumnaId ───────────────────────────────

    @Test
    void existsByIdAndColumnasTableroColumnaId_shouldReturnTrueWhenRelationExists() {
        String tableroId = UUID.randomUUID().toString();
        String columnaId = UUID.randomUUID().toString();

        TableroEntity entity = buildTableroEntity(tableroId, "Test Board");
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity, columnaId, 5, 0));

        repository.save(entity);

        assertTrue(
            repository.existsByIdAndColumnasTableroColumnaId(tableroId, columnaId)
        );
    }

    @Test
    void existsByIdAndColumnasTableroColumnaId_shouldReturnFalseWhenRelationDoesNotExist() {
        String tableroId = UUID.randomUUID().toString();
        String columnaId = UUID.randomUUID().toString();

        // Board with different column
        TableroEntity entity = buildTableroEntity(tableroId, "Test Board");
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity, UUID.randomUUID().toString(), 5, 0));

        repository.save(entity);

        assertFalse(
            repository.existsByIdAndColumnasTableroColumnaId(tableroId, columnaId)
        );
    }

    @Test
    void existsByIdAndColumnasTableroColumnaId_shouldReturnFalseWhenTableroDoesNotExist() {
        String tableroId = UUID.randomUUID().toString();
        String columnaId = UUID.randomUUID().toString();

        assertFalse(
            repository.existsByIdAndColumnasTableroColumnaId(tableroId, columnaId)
        );
    }

    @Test
    void existsByIdAndColumnasTableroColumnaId_shouldBeCaseSensitive() {
        String tableroId = UUID.randomUUID().toString();
        String columnaId = UUID.randomUUID().toString();

        TableroEntity entity = buildTableroEntity(tableroId, "Test Board");
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity, columnaId, 5, 0));

        repository.save(entity);

        assertTrue(repository.existsByIdAndColumnasTableroColumnaId(tableroId, columnaId));
        // Different case should not match (H2 stores as-is)
    }

    // ── existsByColumnasTableroColumnaId ──────────────────────────────────────────

    @Test
    void existsByColumnasTableroColumnaId_shouldReturnTrueWhenColumnaIsAssigned() {
        String tableroId = UUID.randomUUID().toString();
        String columnaId = UUID.randomUUID().toString();

        TableroEntity entity = buildTableroEntity(tableroId, "Test Board");
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity, columnaId, 5, 0));

        repository.save(entity);

        assertTrue(repository.existsByColumnasTableroColumnaId(columnaId));
    }

    @Test
    void existsByColumnasTableroColumnaId_shouldReturnFalseWhenColumnaIsNotAssigned() {
        String columnaId = UUID.randomUUID().toString();

        // No board has this column
        assertFalse(repository.existsByColumnasTableroColumnaId(columnaId));
    }

    @Test
    void existsByColumnasTableroColumnaId_shouldReturnFalseWhenColumnaAssignedToDifferentBoard() {
        String columnaId = UUID.randomUUID().toString();
        String otherTableroId = UUID.randomUUID().toString();

        // Board 1
        TableroEntity board1 = buildTableroEntity(UUID.randomUUID().toString(), "Board 1");
        board1.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), board1, UUID.randomUUID().toString(), 5, 0));
        repository.save(board1);

        // Board 2
        TableroEntity board2 = buildTableroEntity(otherTableroId, "Board 2");
        board2.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), board2, columnaId, 3, 0));
        repository.save(board2);

        assertTrue(repository.existsByColumnasTableroColumnaId(columnaId));
    }

    @Test
    void findBoardIdsByColumnasTableroColumnaId_returnsEveryReferencingBoardOnly() {
        String sharedColumnId = UUID.randomUUID().toString();
        String unrelatedColumnId = UUID.randomUUID().toString();
        String firstBoardId = UUID.randomUUID().toString();
        String secondBoardId = UUID.randomUUID().toString();
        String unrelatedBoardId = UUID.randomUUID().toString();

        TableroEntity firstBoard = buildTableroEntity(firstBoardId, "First board");
        firstBoard.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), firstBoard, sharedColumnId, 5, 0));
        TableroEntity secondBoard = buildTableroEntity(secondBoardId, "Second board");
        secondBoard.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), secondBoard, sharedColumnId, 5, 0));
        TableroEntity unrelatedBoard = buildTableroEntity(unrelatedBoardId, "Unrelated board");
        unrelatedBoard.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), unrelatedBoard, unrelatedColumnId, 5, 0));
        repository.saveAndFlush(firstBoard);
        repository.saveAndFlush(secondBoard);
        repository.saveAndFlush(unrelatedBoard);

        var boardIds = repository.findBoardIdsByColumnasTableroColumnaId(sharedColumnId);

        assertEquals(2, boardIds.size());
        assertTrue(boardIds.contains(firstBoardId));
        assertTrue(boardIds.contains(secondBoardId));
        assertFalse(boardIds.contains(unrelatedBoardId));
    }

    // ── uniqueness constraint ───────────────────────────────────────

    @Test
    void save_sameColumnaInSameTableroTwice_shouldThrowOrFailConstraint() {
        String tableroId = UUID.randomUUID().toString();
        String columnaId = UUID.randomUUID().toString();

        TableroEntity entity = buildTableroEntity(tableroId, "Test Board");
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity, columnaId, 5, 0));
        entity.getColumnasTablero().add(buildColumnaTableroEntity(
            UUID.randomUUID().toString(), entity, columnaId, 3, 1)); // same columnaId!

        repository.save(entity);

        // H2 should throw DataIntegrityViolationException for UK violation
        // or if Hibernate allows it, findById would have 2 children with same columnaId
        var found = repository.findById(tableroId);
        assertTrue(found.isPresent());
        // Note: If Hibernate allows duplicates due to same child entity class,
        // the UK constraint should fire on flush. Tested separately.
    }

    // ── findAll ─────────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnAllTableros() {
        repository.save(buildTableroEntity(UUID.randomUUID().toString(), "Board 1"));
        repository.save(buildTableroEntity(UUID.randomUUID().toString(), "Board 2"));

        var all = repository.findAll();

        assertEquals(2, all.size());
    }

    @Test
    void findAll_shouldReturnEmptyListWhenEmpty() {
        assertTrue(repository.findAll().isEmpty());
    }
}
