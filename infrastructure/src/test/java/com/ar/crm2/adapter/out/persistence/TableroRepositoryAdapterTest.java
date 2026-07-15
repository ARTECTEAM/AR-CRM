package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.ColumnaEntity;
import com.ar.crm2.adapter.out.persistence.entity.ColumnaTableroEntity;
import com.ar.crm2.adapter.out.persistence.entity.TableroEntity;
import com.ar.crm2.adapter.out.persistence.mapper.TableroMapper;
import com.ar.crm2.adapter.out.persistence.repository.ColumnaRepository;
import com.ar.crm2.adapter.out.persistence.repository.TableroRepository;
import com.ar.crm2.model.entity.Columna;
import com.ar.crm2.model.entity.ColumnaTablero;
import com.ar.crm2.model.entity.Tablero;
import com.ar.crm2.model.enums.TipoColumna;
import com.ar.crm2.model.enums.TipoTablero;
import com.ar.crm2.model.vo.ColumnaId;
import com.ar.crm2.model.vo.TableroId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TableroRepositoryAdapter}.
 * Verifies delegation to repositories, UUID/String conversion, and
 * ExistsColumnaEnTableroPort / FindColumnaByIdPort behaviors.
 */
@ExtendWith(MockitoExtension.class)
class TableroRepositoryAdapterTest {

    @Mock
    private TableroRepository repository;

    @Mock
    private ColumnaRepository columnaRepository;

    @Mock
    private TableroMapper mapper;

    @InjectMocks
    private TableroRepositoryAdapter adapter;

    // ── Helpers ─────────────────────────────────────────────────────

    private Tablero createDomainTablero(UUID id, String nombre, List<ColumnaTablero> columnasTablero) {
        return Tablero.reconstitute(
            TableroId.from(id),
            nombre,
            "Test description",
            columnasTablero,
            TipoTablero.TAREAS,
            LocalDateTime.now()
        );
    }

    private TableroEntity createTableroEntity(String id, String nombre) {
        return TableroEntity.builder()
            .id(id)
            .nombre(nombre)
            .descripcion("Test description")
            .tipoTablero(TipoTablero.TAREAS)
            .creadoEn(LocalDateTime.now())
            .columnasTablero(new java.util.ArrayList<>())
            .build();
    }

    private Columna createCatalogColumna(UUID id, String nombre) {
        return Columna.reconstitute(
            ColumnaId.from(id),
            nombre,
            "#FFFFFF",
            TipoTablero.TAREAS,
            TipoColumna.PREDETERMINADA,
            false
        );
    }

    // ── save ─────────────────────────────────────────────────────────

    @Test
    void save_shouldDelegateToRepositoryWithMappedEntity() {
        UUID id = UUID.randomUUID();
        Tablero tablero = createDomainTablero(id, "Sprint Board", List.of());
        TableroEntity savedEntity = createTableroEntity(id.toString(), "Sprint Board");

        when(repository.findById(id.toString())).thenReturn(Optional.empty());
        when(mapper.toEntity(eq(tablero), any(java.util.Map.class))).thenReturn(savedEntity);
        when(repository.save(savedEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(tablero);

        Tablero result = adapter.save(tablero);

        assertSame(tablero, result);
        verify(mapper).toEntity(eq(tablero), any(java.util.Map.class));
        verify(repository).save(savedEntity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void save_shouldReturnDomainFromPersistedEntity() {
        UUID id = UUID.randomUUID();
        Tablero tablero = createDomainTablero(id, "Board", List.of());
        TableroEntity savedEntity = createTableroEntity(id.toString(), "Board");
        Tablero reconstituted = createDomainTablero(id, "Board", List.of());

        when(repository.findById(id.toString())).thenReturn(Optional.empty());
        when(mapper.toEntity(eq(tablero), any(java.util.Map.class))).thenReturn(savedEntity);
        when(repository.save(savedEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(reconstituted);

        Tablero result = adapter.save(tablero);

        assertEquals(id, result.getId().value());
    }

    // ── findAll ─────────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnEmptyListWhenRepositoryIsEmpty() {
        when(repository.findAll()).thenReturn(List.of());

        List<Tablero> result = adapter.findAll();

        assertTrue(result.isEmpty());
        verify(repository).findAll();
    }

    @Test
    void findAll_shouldMapAllEntitiesToDomain() {
        TableroEntity e1 = createTableroEntity(UUID.randomUUID().toString(), "Board 1");
        TableroEntity e2 = createTableroEntity(UUID.randomUUID().toString(), "Board 2");
        Tablero t1 = createDomainTablero(UUID.fromString(e1.getId()), "Board 1", List.of());
        Tablero t2 = createDomainTablero(UUID.fromString(e2.getId()), "Board 2", List.of());

        when(repository.findAll()).thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(t1);
        when(mapper.toDomain(e2)).thenReturn(t2);

        List<Tablero> result = adapter.findAll();

        assertEquals(2, result.size());
        assertEquals("Board 1", result.get(0).getNombre());
        assertEquals("Board 2", result.get(1).getNombre());
    }

    // ── findById (Tablero) ───────────────────────────────────────────

    @Test
    void findByIdTablero_shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(repository.findById(id.toString())).thenReturn(Optional.empty());

        Optional<Tablero> result = adapter.findById(TableroId.from(id));

        assertTrue(result.isEmpty());
        verify(repository).findById(id.toString());
    }

    @Test
    void findByIdTablero_shouldReturnDomainWhenFound() {
        UUID id = UUID.randomUUID();
        TableroEntity entity = createTableroEntity(id.toString(), "Sprint Board");
        Tablero tablero = createDomainTablero(id, "Sprint Board", List.of());

        when(repository.findById(id.toString())).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(tablero);

        Optional<Tablero> result = adapter.findById(TableroId.from(id));

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId().value());
    }

    @Test
    void findByIdTablero_shouldConvertTableroIdToString() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        TableroEntity entity = createTableroEntity(id.toString(), "Board");

        when(repository.findById("550e8400-e29b-41d4-a716-446655440000"))
            .thenReturn(Optional.of(entity));

        adapter.findById(TableroId.from(id));

        verify(repository).findById("550e8400-e29b-41d4-a716-446655440000");
    }

    // ── deleteById ─────────────────────────────────────────────────

    @Test
    void deleteById_shouldDelegateToRepositoryWithStringId() {
        UUID id = UUID.randomUUID();

        doNothing().when(repository).deleteById(id.toString());

        adapter.deleteById(TableroId.from(id));

        verify(repository).deleteById(id.toString());
    }

    // ── existsByTableroIdAndColumnaId ───────────────────────────────
    // Implements ExistsColumnaEnTableroPort

    @Test
    void existsByTableroIdAndColumnaId_shouldReturnTrueWhenExists() {
        UUID tableroId = UUID.randomUUID();
        UUID columnaId = UUID.randomUUID();

        when(repository.existsByIdAndColumnasTableroColumnaId(
            tableroId.toString(),
            columnaId.toString()
        )).thenReturn(true);

        boolean result = adapter.existsByTableroIdAndColumnaId(
            TableroId.from(tableroId),
            ColumnaId.from(columnaId)
        );

        assertTrue(result);
        verify(repository).existsByIdAndColumnasTableroColumnaId(
            tableroId.toString(),
            columnaId.toString()
        );
    }

    @Test
    void existsByTableroIdAndColumnaId_shouldReturnFalseWhenNotExists() {
        UUID tableroId = UUID.randomUUID();
        UUID columnaId = UUID.randomUUID();

        when(repository.existsByIdAndColumnasTableroColumnaId(
            tableroId.toString(),
            columnaId.toString()
        )).thenReturn(false);

        boolean result = adapter.existsByTableroIdAndColumnaId(
            TableroId.from(tableroId),
            ColumnaId.from(columnaId)
        );

        assertFalse(result);
    }

    @Test
    void existsByTableroIdAndColumnaId_shouldConvertBothIdsToString() {
        UUID tableroId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID columnaId = UUID.fromString("660e8400-e29b-41d4-a716-446655440001");

        when(repository.existsByIdAndColumnasTableroColumnaId(
            "550e8400-e29b-41d4-a716-446655440000",
            "660e8400-e29b-41d4-a716-446655440001"
        )).thenReturn(false);

        adapter.existsByTableroIdAndColumnaId(
            TableroId.from(tableroId),
            ColumnaId.from(columnaId)
        );

        verify(repository).existsByIdAndColumnasTableroColumnaId(
            "550e8400-e29b-41d4-a716-446655440000",
            "660e8400-e29b-41d4-a716-446655440001"
        );
    }

    @Test
    void existsByTableroIdAndColumnaId_shouldDelegateToRepository() {
        UUID tableroId = UUID.randomUUID();
        UUID columnaId = UUID.randomUUID();

        adapter.existsByTableroIdAndColumnaId(
            TableroId.from(tableroId),
            ColumnaId.from(columnaId)
        );

        verify(repository).existsByIdAndColumnasTableroColumnaId(
            tableroId.toString(),
            columnaId.toString()
        );
    }

    @Test
    void existsCompatibleColumn_shouldDelegateWithColumnaIdAndTipoTablero() {
        UUID columnaId = UUID.randomUUID();

        when(repository.existsByColumnasTableroColumnaIdAndColumnasTableroTipoTablero(
            columnaId.toString(),
            TipoTablero.TRATOS
        )).thenReturn(true);

        boolean result = adapter.existsCompatibleColumn(ColumnaId.from(columnaId), TipoTablero.TRATOS);

        assertTrue(result);
        verify(repository).existsByColumnasTableroColumnaIdAndColumnasTableroTipoTablero(
            columnaId.toString(),
            TipoTablero.TRATOS
        );
    }

    @Test
    void existsCompatibleColumn_shouldReturnFalseWhenColumnIsNotAssignedToExpectedType() {
        UUID columnaId = UUID.randomUUID();

        when(repository.existsByColumnasTableroColumnaIdAndColumnasTableroTipoTablero(
            columnaId.toString(),
            TipoTablero.TAREAS
        )).thenReturn(false);

        boolean result = adapter.existsCompatibleColumn(ColumnaId.from(columnaId), TipoTablero.TAREAS);

        assertFalse(result);
    }

    // ── findById (Columna) ──────────────────────────────────────────
    // Implements FindColumnaByIdPort from tablero package

    @Test
    void findByIdColumna_shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();

        when(columnaRepository.findById(id.toString())).thenReturn(Optional.empty());

        Optional<Columna> result = adapter.findById(ColumnaId.from(id));

        assertTrue(result.isEmpty());
        verify(columnaRepository).findById(id.toString());
    }

    @Test
    void findByIdColumna_shouldReturnDomainWhenFound() {
        UUID id = UUID.randomUUID();
        ColumnaEntity entity = ColumnaEntity.builder()
            .id(id.toString())
            .columnaNombre("Backlog")
            .color("#FFFFFF")
            .tipoTablero(TipoTablero.TAREAS.name())
            .tipoColumna(TipoColumna.PREDETERMINADA.name())
            .build();
        Columna columna = createCatalogColumna(id, "Backlog");

        when(columnaRepository.findById(id.toString())).thenReturn(Optional.of(entity));

        Optional<Columna> result = adapter.findById(ColumnaId.from(id));

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId().value());
        assertEquals("Backlog", result.get().getColumnanombre());
    }

    @Test
    void findByIdColumna_shouldUseColumnaRepository() {
        UUID id = UUID.randomUUID();

        when(columnaRepository.findById(id.toString())).thenReturn(Optional.empty());

        adapter.findById(ColumnaId.from(id));

        verify(columnaRepository).findById(id.toString());
        verifyNoInteractions(repository);
    }

    @Test
    void findByIdColumna_shouldConvertColumnaIdToString() {
        UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        ColumnaEntity entity = ColumnaEntity.builder()
            .id(id.toString())
            .columnaNombre("In Progress")
            .color("#FF0000")
            .tipoTablero(TipoTablero.TAREAS.name())
            .tipoColumna(TipoColumna.PREDETERMINADA.name())
            .build();

        when(columnaRepository.findById("550e8400-e29b-41d4-a716-446655440000"))
            .thenReturn(Optional.of(entity));

        adapter.findById(ColumnaId.from(id));

        verify(columnaRepository).findById("550e8400-e29b-41d4-a716-446655440000");
    }

    // ── save: existing-child-row-id preservation (regression for
    //    uk_columnas_tablero_tablero_columna duplicate-key violations) ─

    @Test
    void save_existingTablero_passesExistingChildRowIdsToMapper() {
        UUID tableroId = UUID.randomUUID();
        UUID existingColumnaId = UUID.randomUUID();
        String persistedChildRowId = "persisted-row-id-1";

        // Domain aggregate that includes the already-persisted column
        Tablero tablero = createDomainTablero(tableroId, "Existing Board", List.of(
            ColumnaTablero.reconstitute(ColumnaId.from(existingColumnaId),
                TipoTablero.TAREAS, 5, null, BigDecimal.ZERO)
        ));

        // Persisted Tablero row with one child carrying a stable row id.
        // This is what repository.findById will return — and the adapter
        // must build its existing-child-row-ids map from THESE children.
        TableroEntity reloaded = createTableroEntity(tableroId.toString(), "Existing Board");
        reloaded.getColumnasTablero().add(ColumnaTableroEntity.builder()
            .id(persistedChildRowId)
            .tablero(reloaded)
            .columnaId(existingColumnaId.toString())
            .tipoTablero(TipoTablero.TAREAS)
            .limiteWip(5)
            .nota(null)
            .totalValorEstimado(BigDecimal.ZERO)
            .orden(0)
            .build());

        when(repository.findById(tableroId.toString())).thenReturn(Optional.of(reloaded));
        when(mapper.toEntity(eq(tablero), any(java.util.Map.class))).thenAnswer(inv -> {
            java.util.Map<String, String> map = inv.getArgument(1);
            // CRITICAL: the persisted row id for the already-existing column
            // must be present so the mapper can reuse it on save.
            assertEquals(persistedChildRowId, map.get(existingColumnaId.toString()),
                "adapter must pass the persisted child row id keyed by columnaId");
            return reloaded;
        });
        when(repository.save(reloaded)).thenReturn(reloaded);
        when(mapper.toDomain(reloaded)).thenReturn(tablero);

        adapter.save(tablero);
    }

    @Test
    void save_existingTablero_newAssignment_mixesPreservedAndFreshChildRowIds() {
        UUID tableroId = UUID.randomUUID();
        UUID existingColumnaId = UUID.randomUUID();
        UUID newColumnaId = UUID.randomUUID();
        String persistedRowIdForExisting = "existing-row-1";

        // Domain has BOTH the existing and the new column
        Tablero tablero = createDomainTablero(tableroId, "Board", List.of(
            ColumnaTablero.reconstitute(ColumnaId.from(existingColumnaId),
                TipoTablero.TAREAS, 5, null, BigDecimal.ZERO),
            ColumnaTablero.reconstitute(ColumnaId.from(newColumnaId),
                TipoTablero.TAREAS, 3, null, BigDecimal.ZERO)
        ));

        // Persisted row carries only the already-existing column; the new
        // column is genuinely new and must NOT be in the existing-row map.
        TableroEntity reloaded = createTableroEntity(tableroId.toString(), "Board");
        reloaded.getColumnasTablero().add(ColumnaTableroEntity.builder()
            .id(persistedRowIdForExisting)
            .tablero(reloaded)
            .columnaId(existingColumnaId.toString())
            .tipoTablero(TipoTablero.TAREAS)
            .limiteWip(5)
            .nota(null)
            .totalValorEstimado(BigDecimal.ZERO)
            .orden(0)
            .build());

        when(repository.findById(tableroId.toString())).thenReturn(Optional.of(reloaded));
        when(mapper.toEntity(eq(tablero), any(java.util.Map.class))).thenAnswer(inv -> {
            java.util.Map<String, String> map = inv.getArgument(1);
            assertEquals(persistedRowIdForExisting, map.get(existingColumnaId.toString()),
                "existing column must carry its persisted row id");
            assertNull(map.get(newColumnaId.toString()),
                "new column must NOT be present in the existing-row-id map");
            return reloaded;
        });
        when(repository.save(reloaded)).thenReturn(reloaded);
        when(mapper.toDomain(reloaded)).thenReturn(tablero);

        adapter.save(tablero);
    }

    @Test
    void save_newTablero_passesEmptyMapToMapper() {
        UUID tableroId = UUID.randomUUID();
        Tablero tablero = createDomainTablero(tableroId, "Brand New Board", List.of());
        TableroEntity savedEntity = createTableroEntity(tableroId.toString(), "Brand New Board");

        when(repository.findById(tableroId.toString())).thenReturn(Optional.empty());
        when(mapper.toEntity(eq(tablero), any(java.util.Map.class))).thenAnswer(inv -> {
            java.util.Map<String, String> map = inv.getArgument(1);
            assertTrue(map.isEmpty(),
                "for a brand-new Tablero the existing-child-row-ids map must be empty");
            return savedEntity;
        });
        when(repository.save(savedEntity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(tablero);

        adapter.save(tablero);
    }

    @Test
    void save_existingTablero_noChildren_passesEmptyMapToMapper() {
        UUID tableroId = UUID.randomUUID();
        Tablero tablero = createDomainTablero(tableroId, "Board", List.of());
        TableroEntity reloaded = createTableroEntity(tableroId.toString(), "Board");

        when(repository.findById(tableroId.toString())).thenReturn(Optional.of(reloaded));
        when(mapper.toEntity(eq(tablero), any(java.util.Map.class))).thenAnswer(inv -> {
            java.util.Map<String, String> map = inv.getArgument(1);
            assertTrue(map.isEmpty(),
                "an existing Tablero with no children yields an empty map");
            return reloaded;
        });
        when(repository.save(reloaded)).thenReturn(reloaded);
        when(mapper.toDomain(reloaded)).thenReturn(tablero);

        adapter.save(tablero);
    }
}
