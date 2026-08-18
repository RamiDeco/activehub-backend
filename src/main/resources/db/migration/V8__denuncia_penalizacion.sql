-- denuncia -------------------------------------------------------------------
CREATE TABLE denuncia (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clase_id    UUID NOT NULL REFERENCES clase(id),
    alumno_id   UUID NOT NULL REFERENCES usuario(id),
    motivo      TEXT NOT NULL,
    estado      VARCHAR(20) NOT NULL DEFAULT 'Pendiente'
        CHECK (estado IN ('Pendiente','En Auditoría','Resuelta')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_denuncia_clase ON denuncia (clase_id);
-- un alumno solo puede denunciar una vez la misma clase
CREATE UNIQUE INDEX ux_denuncia_alumno_clase ON denuncia (clase_id, alumno_id);

-- penalizacion ------------------------------------------------------------------
CREATE TABLE penalizacion (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario(id),
    tipo        VARCHAR(30) NOT NULL
        CHECK (tipo IN ('Económica','Suspensión temporal')),
    motivo      TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_penalizacion_usuario ON penalizacion (usuario_id);
