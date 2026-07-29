package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.ContactoEntity;
import com.ar.crm2.adapter.out.persistence.repository.ContactoRepository;
import com.ar.crm2.model.enums.EstadoRelacion;
import com.ar.crm2.model.vo.EmpresaId;
import com.ar.crm2.model.vo.UsuarioId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * H2 integration tests for {@link ContactoRepositoryAdapter}'s
 * implementation of the actor-scoped
 * {@code SearchContactosPort.search(...)} contract.
 *
 * <p>Each scenario seeds {@link ContactoEntity} rows directly via the
 * {@link ContactoRepository} (with explicit {@code creadoPor},
 * {@code responsableId}, and {@code creadoEn}) and then asserts what
 * the adapter returns for a given actor and filter set. Seed data
 * uses lex-sortable UUIDs and is intentionally inserted in an order
 * that differs from the expected {@code creadoEn DESC, id ASC} return
 * order so a {@code findAll().stream().subList(...)} implementation
 * would fail the limit and tie-order tests.
 *
 * <p>Every assertion verifies behavior the adapter must guarantee at
 * the database boundary:
 * <ul>
 *   <li>creator-or-responsible visibility — the actor sees contacts
 *       they created plus contacts where they are the responsable.</li>
 *   <li>invisible exclusion — contacts owned by other actors are never
 *       returned for the actor's scope.</li>
 *   <li>responsible intersection — an optional {@code responsableId}
 *       filter narrows the visible set without ever replacing the
 *       actor scope.</li>
 *   <li>every optional predicate and combinations — search text,
 *       {@code estadoRelacion}, {@code empresaId}, and
 *       {@code comoNosConocio} filter at the same DB query.</li>
 *   <li>case behavior — search text is matched case-insensitively
 *       over the searchable string fields.</li>
 *   <li>read-only — repeated calls return the same set without
 *       mutating any persisted row.</li>
 *   <li>tie order — contacts sharing {@code creadoEn} are returned
 *       {@code id ASC}.</li>
 *   <li>DB limit — the {@code maxResults} cap is applied before
 *       materialization, taking the most-recent rows per the
 *       deterministic order, not the first N from insertion order.</li>
 * </ul>
 */
@DataJpaTest
@Import(ContactoRepositoryAdapter.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:contacto-search-it;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ContactoRepositoryAdapterTest {

    @Autowired
    private ContactoRepositoryAdapter adapter;

    @Autowired
    private ContactoRepository repository;

    private static final UUID ACTOR_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID ACTOR_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ACTOR_C = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    // Lex-sortable UUIDs for deterministic tie-order assertions
    private static final UUID ID_C1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_C2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ID_C3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ID_C4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID ID_C5 = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID ID_C6 = UUID.fromString("00000000-0000-0000-0000-000000000006");
    private static final UUID ID_C7 = UUID.fromString("00000000-0000-0000-0000-000000000007");

    // Lex-sortable empresa UUIDs
    private static final UUID EMPRESA_E1 = UUID.fromString("11111111-0000-0000-0000-000000000001");
    private static final UUID EMPRESA_E2 = UUID.fromString("11111111-0000-0000-0000-000000000002");

    // ── Helpers ─────────────────────────────────────────────────────

    private ContactoEntity saveContacto(
            UUID id,
            String creadoPor,
            String responsableId,
            String nombre,
            String correo,
            String telefono,
            String cargo,
            String comoNosConocio,
            EstadoRelacion estado,
            UUID empresaId,
            LocalDateTime creadoEn
    ) {
        ContactoEntity entity = ContactoEntity.builder()
                .id(id.toString())
                .creadoPor(creadoPor)
                .responsableId(responsableId)
                .nombre(nombre)
                .correo(correo)
                .telefono(telefono)
                .cargo(cargo)
                .comoNosConocio(comoNosConocio)
                .estadoRelacion(estado)
                .empresaId(empresaId.toString())
                .creadoEn(creadoEn)
                .build();
        return repository.saveAndFlush(entity);
    }

    private static String idOf(UUID uuid) {
        return uuid.toString();
    }

    // ── Visibility scope (creator OR responsable) ──────────────────

    @Test
    void search_returnsCreatorAndResponsableRowsForActor() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 1, 0, 0);
        // Actor A created these two
        saveContacto(ID_C1, ACTOR_A.toString(), ACTOR_A.toString(),
                "Alice", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_A.toString(), ACTOR_B.toString(),
                "Bob", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        // Actor C created this one but assigned it to Actor A
        saveContacto(ID_C3, ACTOR_C.toString(), ACTOR_A.toString(),
                "Carol", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        // Actor B owns these two — invisible to Actor A
        saveContacto(ID_C4, ACTOR_B.toString(), ACTOR_B.toString(),
                "Dave", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C5, ACTOR_B.toString(), null,
                "Eve", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(3, ids.size(),
                "Actor A must see exactly the 3 rows where creadoPor=A or responsableId=A");
        assertTrue(ids.contains(idOf(ID_C1)));
        assertTrue(ids.contains(idOf(ID_C2)));
        assertTrue(ids.contains(idOf(ID_C3)));
        assertFalse(ids.contains(idOf(ID_C4)),
                "Contact owned by B with responsable=B must be invisible to A");
        assertFalse(ids.contains(idOf(ID_C5)),
                "Contact owned by B with no responsable must be invisible to A");
    }

    @Test
    void search_invisibleExclusion_doesNotLeakOtherActorRows() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 2, 0, 0);
        saveContacto(ID_C1, ACTOR_B.toString(), ACTOR_B.toString(),
                "Bob1", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_B.toString(), null,
                "Bob2", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C3, ACTOR_C.toString(), null,
                "Carol1", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertTrue(ids.isEmpty(),
                "Actor A must not see any of B's or C's contacts");
    }

    // ── Optional responsableId intersects scope, never replaces it ──

    @Test
    void search_responsableIdFilterIntersectsScopeWithoutReplacingActor() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 3, 0, 0);
        // Visible to A (created by A) but assigned to B
        saveContacto(ID_C1, ACTOR_A.toString(), ACTOR_B.toString(),
                "AliceBobResp", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        // Visible to A (assigned to A), not assigned to B
        saveContacto(ID_C2, ACTOR_C.toString(), ACTOR_A.toString(),
                "CarolAssignedA", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        // Visible to A, assigned to B too
        saveContacto(ID_C3, ACTOR_A.toString(), ACTOR_B.toString(),
                "AliceBobBoth", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        // Created by B, assigned to B — invisible to A
        saveContacto(ID_C4, ACTOR_B.toString(), ACTOR_B.toString(),
                "BobInvisible", null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, UsuarioId.from(ACTOR_B), null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(2, ids.size(),
                "Filter responsableId=B must intersect scope — only c1 and c3 "
                        + "are visible to A AND assigned to B");
        assertTrue(ids.contains(idOf(ID_C1)));
        assertTrue(ids.contains(idOf(ID_C3)));
        assertFalse(ids.contains(idOf(ID_C2)),
                "c2 is visible to A but assigned to A, not B");
        assertFalse(ids.contains(idOf(ID_C4)),
                "c4 is invisible to A even though assigned to B");
    }

    // ── Optional filters ────────────────────────────────────────────

    @Test
    void search_estadoRelacionFilterIntersectsScope() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 4, 0, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Alice",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "Alice",
                null, null, null, null,
                EstadoRelacion.INACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C3, ACTOR_A.toString(), null, "Alice",
                null, null, null, null,
                EstadoRelacion.PROSPECTO, EMPRESA_E1, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, EstadoRelacion.ACTIVO, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C1)), ids);
    }

    @Test
    void search_searchFilterIsCaseInsensitiveAcrossTextColumns() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 5, 0, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Alice",
                "alice@example.com", "1111", "Dev", null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "Bob",
                "bob@example.com", "2222", "Sales", null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C3, ACTOR_A.toString(), null, "Carol",
                "carol@example.com", "3333", "PM", null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C4, ACTOR_A.toString(), null, "Dave",
                null, null, "Engineer", null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        // Lowercase query against uppercase-stored email
        List<String> byEmail = adapter.search(UsuarioId.from(ACTOR_A),
                "ALICE@EXAMPLE.COM", null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();
        assertEquals(List.of(idOf(ID_C1)), byEmail);

        // Partial token across multiple columns — telefono
        List<String> byPhone = adapter.search(UsuarioId.from(ACTOR_A),
                "3333", null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();
        assertEquals(List.of(idOf(ID_C3)), byPhone);

        // Token across cargo
        List<String> byCargo = adapter.search(UsuarioId.from(ACTOR_A),
                "engineer", null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();
        assertEquals(List.of(idOf(ID_C4)), byCargo);
    }

    @Test
    void search_percentInSearchIsTreatedLiterally() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 5, 1, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Discount 10%",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "Arbitrary visible contact",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C3, ACTOR_B.toString(), null, "Invisible 20%",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                "%", null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C1)), ids);
    }

    @Test
    void search_underscoreInSearchIsTreatedLiterally() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 5, 2, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Account_A",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "Arbitrary visible contact",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C3, ACTOR_B.toString(), null, "Invisible_B",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                "_", null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C1)), ids);
    }

    @Test
    void search_normalizedComoNosConocioMatchesPersistedPaddedValue() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 5, 3, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Visible match",
                null, null, null, " LinkedIn ",
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "Visible non-match",
                null, null, null, "Referral",
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C3, ACTOR_B.toString(), null, "Invisible match",
                null, null, null, "LinkedIn",
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, "LinkedIn", null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C1)), ids);
    }

    @Test
    void search_empresaIdAndComoNosConocioFiltersAreAppliedAtSameQuery() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 6, 0, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Alice",
                null, null, null, "linkedin",
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "Alice",
                null, null, null, "referral",
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C3, ACTOR_A.toString(), null, "Alice",
                null, null, null, "linkedin",
                EstadoRelacion.ACTIVO, EMPRESA_E2, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, null,
                EmpresaId.from(EMPRESA_E1),
                null, "linkedin", null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C1)), ids,
                "Only c1 matches both empresaId=e1 AND comoNosConocio=linkedin");
    }

    @Test
    void search_combinedFiltersIntersectAllDimensions() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 7, 0, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Alice",
                null, null, null, "linkedin",
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "Alice",
                null, null, null, "linkedin",
                EstadoRelacion.INACTIVO, EMPRESA_E1, t);
        saveContacto(ID_C3, ACTOR_A.toString(), null, "Bob",
                null, null, null, "linkedin",
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                "ali", EstadoRelacion.ACTIVO, null, null, "linkedin", null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C1)), ids,
                "Only c1 satisfies search+estado+comoNosConocio together");
    }

    // ── Read-only ───────────────────────────────────────────────────

    @Test
    void search_isReadOnlyAcrossRepeatedInvocations() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 8, 0, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Alice",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        int before = (int) repository.count();

        List<String> first = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();
        List<String> second = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(first, second);
        assertEquals(before, repository.count(),
                "Repeated adapter.search calls must not change the row count");
    }

    // ── Tie order: creadoEn DESC, id ASC ───────────────────────────

    @Test
    void search_ordersByCreadoEnDescAndIdAscOnTies() {
        // Same creadoEn across all three rows — order must be id ASC
        LocalDateTime same = LocalDateTime.of(2026, 7, 9, 12, 0, 0);
        // Insert in REVERSE expected order to catch a findAll().stream()
        // approach that does not enforce id ASC on ties.
        saveContacto(ID_C3, ACTOR_A.toString(), null, "Carol",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, same);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "Alice",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, same);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "Bob",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, same);

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C1), idOf(ID_C2), idOf(ID_C3)), ids,
                "Same creadoEn → id ASC must be applied by the DB query, "
                        + "not by Java insertion order");
    }

    // ── DB-level limit: maxResults applied BEFORE materialization ──

    @Test
    void search_appliesDbLevelLimitAgainstDeterministicOrder() {
        LocalDateTime base = LocalDateTime.of(2026, 7, 10, 0, 0);
        // Insert in OLDEST-first order (reverse of expected return order)
        // so a findAll().stream().subList() implementation returns the
        // wrong rows. We then expect maxResults=3 to return the
        // THREE NEWEST rows by creadoEn DESC, id ASC.
        saveContacto(ID_C1, ACTOR_A.toString(), null, "oldest1",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "oldest2",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base.plusSeconds(1));
        saveContacto(ID_C3, ACTOR_A.toString(), null, "oldest3",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base.plusSeconds(2));
        saveContacto(ID_C4, ACTOR_A.toString(), null, "middle",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base.plusSeconds(3));
        saveContacto(ID_C5, ACTOR_A.toString(), null, "newest1",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base.plusSeconds(4));
        saveContacto(ID_C6, ACTOR_A.toString(), null, "newest2",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base.plusSeconds(5));
        saveContacto(ID_C7, ACTOR_A.toString(), null, "newest3",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base.plusSeconds(6));

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, null, 3)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C7), idOf(ID_C6), idOf(ID_C5)), ids,
                "DB-pushed LIMIT must return the three most-recent rows per "
                        + "creadoEn DESC, id ASC — not the first 3 from insertion order");
    }

    @Test
    void search_maxResultsNullReturnsEverythingInOrder() {
        LocalDateTime base = LocalDateTime.of(2026, 7, 11, 0, 0);
        saveContacto(ID_C1, ACTOR_A.toString(), null, "older",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base);
        saveContacto(ID_C2, ACTOR_A.toString(), null, "newer",
                null, null, null, null,
                EstadoRelacion.ACTIVO, EMPRESA_E1, base.plusSeconds(1));

        List<String> ids = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, null, null)
                .stream().map(c -> c.getId().value().toString()).toList();

        assertEquals(List.of(idOf(ID_C2), idOf(ID_C1)), ids,
                "Without limit, the adapter still returns the deterministic order");
    }

    // ── Domain round-trip sanity ───────────────────────────────────

    @Test
    void search_returnsReconstitutedDomainEntities() {
        LocalDateTime t = LocalDateTime.of(2026, 7, 12, 0, 0);
        ContactoEntity saved = saveContacto(ID_C1, ACTOR_A.toString(), null,
                "Alice", "alice@example.com", "1111", "Dev", "linkedin",
                EstadoRelacion.ACTIVO, EMPRESA_E1, t);

        var result = adapter.search(UsuarioId.from(ACTOR_A),
                null, null, null, null, null, null);

        assertEquals(1, result.size());
        var contacto = result.get(0);
        assertNotNull(contacto.getId());
        assertEquals("Alice", contacto.getNombre());
        assertEquals("alice@example.com", contacto.getCorreo());
        assertEquals("1111", contacto.getTelefono());
        assertEquals("Dev", contacto.getCargo());
        assertEquals("linkedin", contacto.getComoNosConocio());
        assertEquals(EstadoRelacion.ACTIVO, contacto.getEstadoRelacion());
        assertEquals(t, contacto.getCreadoEn());
        assertEquals(saved.getId(), contacto.getId().value().toString());
    }
}
