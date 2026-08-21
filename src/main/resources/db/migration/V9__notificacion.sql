CREATE TABLE notificacion (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario(id),
    tipo        VARCHAR(40) NOT NULL,
    mensaje     TEXT NOT NULL,
    entidad_id  UUID,
    leida       BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_notificacion_usuario ON notificacion (usuario_id, created_at DESC);
