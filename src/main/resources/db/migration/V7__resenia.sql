-- resenia -------------------------------------------------------------------
CREATE TABLE resenia (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clase_id      UUID NOT NULL REFERENCES clase(id),
    alumno_id     UUID NOT NULL REFERENCES usuario(id),
    puntaje       INT NOT NULL CHECK (puntaje BETWEEN 1 AND 5),
    comentario    TEXT NOT NULL,
    en_moderacion BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted       BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX ix_resenia_clase ON resenia (clase_id);
CREATE INDEX ix_resenia_alumno ON resenia (alumno_id);
-- evita mas de una reseña activa del mismo alumno para la misma clase
CREATE UNIQUE INDEX ux_resenia_alumno_clase_activa
    ON resenia (clase_id, alumno_id) WHERE deleted = false;
