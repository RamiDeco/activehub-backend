CREATE TABLE actividad_favorita (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   UUID NOT NULL REFERENCES usuario(id),
    actividad_id UUID NOT NULL REFERENCES actividad(id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (usuario_id, actividad_id)
);

CREATE INDEX ix_actividad_favorita_usuario ON actividad_favorita (usuario_id);
CREATE INDEX ix_actividad_favorita_actividad ON actividad_favorita (actividad_id);
