-- V36: la multa se paga con comprobante y la suspensión se puede levantar (2026-11-02).
--
-- Decisión del usuario:
--   - Una penalización Económica bloquea al instructor hasta que el administrador aprueba el
--     comprobante de la transferencia. Estados: PENDIENTE_PAGO -> EN_REVISION -> PAGADA (y
--     EN_REVISION -> PENDIENTE_PAGO si el comprobante es inválido).
--   - El administrador puede levantar una Suspensión temporal antes de tiempo, con motivo.
--
-- Todo es aditivo y opcional: la base es compartida y el código anterior la sigue usando. Las
-- multas que ya existían quedan con estado_pago en NULL y no bloquean a nadie.

ALTER TABLE penalizacion ADD COLUMN IF NOT EXISTS estado_pago VARCHAR(20);
ALTER TABLE penalizacion ADD COLUMN IF NOT EXISTS pago_aprobado_at TIMESTAMPTZ;
ALTER TABLE penalizacion ADD COLUMN IF NOT EXISTS pago_aprobado_por_id UUID REFERENCES usuario(id);
ALTER TABLE penalizacion ADD COLUMN IF NOT EXISTS levantada_at TIMESTAMPTZ;
ALTER TABLE penalizacion ADD COLUMN IF NOT EXISTS levantada_por_id UUID REFERENCES usuario(id);
ALTER TABLE penalizacion ADD COLUMN IF NOT EXISTS motivo_levantamiento TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_penalizacion_estado_pago') THEN
        ALTER TABLE penalizacion ADD CONSTRAINT ck_penalizacion_estado_pago
            CHECK (estado_pago IS NULL OR estado_pago IN ('PENDIENTE_PAGO', 'EN_REVISION', 'PAGADA'));
    END IF;
END $$;

-- Cada comprobante que manda el instructor, con su resultado. Se conservan todos: si el
-- administrador marca uno como inválido, el siguiente se agrega y el historial queda.
CREATE TABLE IF NOT EXISTS comprobante_penalizacion (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    penalizacion_id  UUID NOT NULL REFERENCES penalizacion(id),
    ruta_archivo     VARCHAR(500) NOT NULL,
    nombre_archivo   VARCHAR(255) NOT NULL,
    tipo_contenido   VARCHAR(100) NOT NULL,
    tamanio_bytes    BIGINT NOT NULL,
    estado           VARCHAR(20) NOT NULL,
    motivo_invalidez TEXT,
    revisado_por_id  UUID REFERENCES usuario(id),
    revisado_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_comprobante_penalizacion_estado CHECK (estado IN ('EN_REVISION', 'APROBADO', 'INVALIDO'))
);

CREATE INDEX IF NOT EXISTS ix_comprobante_penalizacion_penalizacion ON comprobante_penalizacion (penalizacion_id);
