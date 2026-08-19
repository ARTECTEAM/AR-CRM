package com.ar.crm2.adapter.out.persistence;

import com.ar.crm2.adapter.out.persistence.entity.TratoEntity;
import com.ar.crm2.adapter.out.persistence.repository.TratoRepository;
import com.ar.crm2.model.enums.EstadoTrato;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Persistence regression coverage for the removal of legacy deal states.
 *
 * H2 executes the same normalization UPDATE used by schema.sql, while the
 * repository read proves the resulting value can hydrate as EstadoTrato.
 * PostgreSQL-specific conditional DDL syntax is not exercised by this H2 test.
 */
@DataJpaTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:trato-migration-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TratoRepositoryIT {

    private static final String LEGACY_STATE_MIGRATION = """
            UPDATE tratos
            SET estado = 'CERRADO'
            WHERE estado IN ('GANADO', 'PERDIDO')
            """;

    @Autowired
    private TratoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void allowLegacyValuesInH2Simulation() {
        // Hibernate creates an H2 enum constrained to current values. PostgreSQL
        // EnumType.STRING storage is VARCHAR, so use that shape to simulate legacy rows.
        jdbcTemplate.execute("ALTER TABLE tratos ALTER COLUMN estado VARCHAR(20)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"GANADO", "PERDIDO"})
    void legacyStateMigration_normalizesBeforeEnumHydration(String legacyState) {
        String id = insertTrato(legacyState);

        jdbcTemplate.update(LEGACY_STATE_MIGRATION);
        jdbcTemplate.update(LEGACY_STATE_MIGRATION);

        TratoEntity hydrated = repository.findById(id).orElseThrow();

        assertEquals(EstadoTrato.CERRADO, hydrated.getEstado());
    }

    @Test
    void legacyStateMigration_preservesCurrentOpenState() {
        String id = insertTrato("ABIERTO");

        jdbcTemplate.update(LEGACY_STATE_MIGRATION);

        TratoEntity hydrated = repository.findById(id).orElseThrow();

        assertEquals(EstadoTrato.ABIERTO, hydrated.getEstado());
    }

    private String insertTrato(String estado) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                INSERT INTO tratos (
                    id, contacto_id, responsable_id, nombre, valor_estimado,
                    probabilidad, fecha_cierre_esperada, tipo_contrato, estado,
                    creado_en, actualizado_en
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                "Migration test deal", null, null, null, null, estado,
                LocalDateTime.now(), null);
        return id;
    }
}
