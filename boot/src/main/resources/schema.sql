-- Schema compatibility fix for the Ficha Kanban-card refactor.
--
-- Ficha no longer owns duplicated business metadata. These fields now belong to
-- Tarea/Trato, while Ficha stores only card position/state:
-- id, columna_id, tipo_ficha, trato_id, tarea_id, actualizado_en.
--
-- Hibernate ddl-auto=update does not drop obsolete columns or old NOT NULL
-- constraints, so existing local databases can still reject inserts because
-- legacy columns are missing values. Keep this script idempotent so it is safe
-- on fresh databases and repeated startups.

ALTER TABLE IF EXISTS fichas DROP COLUMN IF EXISTS responsable_id;
ALTER TABLE IF EXISTS fichas DROP COLUMN IF EXISTS creado_por;
ALTER TABLE IF EXISTS fichas DROP COLUMN IF EXISTS creado_en;

-- Agenda reminder fields for email notifications
ALTER TABLE IF EXISTS agendas ADD COLUMN IF NOT EXISTS recordatorio_habilitado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE IF EXISTS agendas ADD COLUMN IF NOT EXISTS minutos_antes INTEGER;
ALTER TABLE IF EXISTS agendas ADD COLUMN IF NOT EXISTS recordatorio_estado VARCHAR(20);
ALTER TABLE IF EXISTS agendas ADD COLUMN IF NOT EXISTS recordatorio_enviado_en TIMESTAMP;
ALTER TABLE IF EXISTS agendas ADD COLUMN IF NOT EXISTS ultimo_intento_en TIMESTAMP;

-- ColumnaTablero: drop the obsolete contextual semantic state columns. The
-- column-board relation no longer carries estadoTarea/estadoTrato; only the
-- catalog Columna holds semantic state.
ALTER TABLE IF EXISTS columnas_tablero DROP COLUMN IF EXISTS estado_tarea;
ALTER TABLE IF EXISTS columnas_tablero DROP COLUMN IF EXISTS estado_trato;

-- Etiqueta catalog (add-ficha-etiquetas slice 2)
-- Idempotent: safe on fresh databases and repeated startups.
-- Hibernate ddl-auto=update creates the table but not the index
-- on fichas_etiquetas.etiqueta_id, which is needed for the
-- cascade-delete "delete all relations referencing this etiqueta"
-- path. We add it explicitly here.
CREATE TABLE IF NOT EXISTS etiquetas (
    id              VARCHAR(36)  NOT NULL,
    nombre          VARCHAR(50)  NOT NULL,
    tipo_etiqueta   VARCHAR(20)  NOT NULL,
    color           VARCHAR(7)   NOT NULL,
    creado_en       TIMESTAMP    NOT NULL,
    CONSTRAINT pk_etiquetas PRIMARY KEY (id),
    CONSTRAINT uk_etiquetas_nombre_tipo UNIQUE (nombre, tipo_etiqueta)
);

CREATE TABLE IF NOT EXISTS fichas_etiquetas (
    id              VARCHAR(36)  NOT NULL,
    ficha_id        VARCHAR(36)  NOT NULL,
    etiqueta_id     VARCHAR(36)  NOT NULL,
    tipo_etiqueta   VARCHAR(20)  NOT NULL,
    CONSTRAINT pk_fichas_etiquetas PRIMARY KEY (id),
    CONSTRAINT uk_fichas_etiquetas_ficha_etiqueta UNIQUE (ficha_id, etiqueta_id),
    CONSTRAINT fk_fichas_etiquetas_ficha
        FOREIGN KEY (ficha_id) REFERENCES fichas (id) ON DELETE CASCADE,
    CONSTRAINT fk_fichas_etiquetas_etiqueta
        FOREIGN KEY (etiqueta_id) REFERENCES etiquetas (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_fichas_etiquetas_etiqueta
    ON fichas_etiquetas (etiqueta_id);

-- Normalize legacy deal states before EnumType.STRING hydration; safe to rerun.
UPDATE tratos
SET estado = 'CERRADO'
WHERE estado IN ('GANADO', 'PERDIDO');
