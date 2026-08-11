-- inscripcion ---------------------------------------------------------------
CREATE TABLE inscripcion (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clase_id    UUID NOT NULL REFERENCES clase(id),
    alumno_id   UUID NOT NULL REFERENCES usuario(id),
    estado      VARCHAR(20) NOT NULL
        CHECK (estado IN ('PreInscripción','PagoPendiente','Inscripto','Cancelada')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_inscripcion_clase ON inscripcion (clase_id);
CREATE INDEX ix_inscripcion_alumno ON inscripcion (alumno_id);
-- evita dos filas "activas" (no Canceladas) del mismo alumno para la misma clase
CREATE UNIQUE INDEX ux_inscripcion_alumno_clase_activa
    ON inscripcion (clase_id, alumno_id) WHERE estado <> 'Cancelada';

-- pago ------------------------------------------------------------------------
CREATE TABLE pago (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inscripcion_id      UUID NOT NULL REFERENCES inscripcion(id),
    estado              VARCHAR(20) NOT NULL
        CHECK (estado IN ('Retenido','Liberado','Cancelado','Efectivo')),
    monto               NUMERIC(10,2) NOT NULL CHECK (monto >= 0),
    metodo              VARCHAR(20) NOT NULL
        CHECK (metodo IN ('Mercado Pago','Efectivo')),
    referencia_externa  VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_pago_inscripcion UNIQUE (inscripcion_id)
);
CREATE INDEX ix_pago_inscripcion ON pago (inscripcion_id);
