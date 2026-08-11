CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- rol -----------------------------------------------------------------
CREATE TABLE rol (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted     BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
);

-- usuario ---------------------------------------------------------------
CREATE TABLE usuario (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre                    VARCHAR(100) NOT NULL,
    apellido                  VARCHAR(100) NOT NULL,
    email                     VARCHAR(255) NOT NULL,
    password_hash             VARCHAR(255) NOT NULL,
    telefono                  VARCHAR(30),
    fecha_nacimiento          DATE,
    rol_id                    UUID NOT NULL REFERENCES rol(id),
    estado                    VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    cantidad_penalizaciones   INT NOT NULL DEFAULT 0,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted                   BOOLEAN NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX ux_usuario_email_activo ON usuario (lower(email)) WHERE deleted = false;
CREATE INDEX ix_usuario_rol_id ON usuario (rol_id);

-- perfil_alumno -----------------------------------------------------------
CREATE TABLE perfil_alumno (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID NOT NULL REFERENCES usuario(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted     BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_perfil_alumno_usuario UNIQUE (usuario_id)
);

CREATE TABLE perfil_alumno_interes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    perfil_alumno_id UUID NOT NULL REFERENCES perfil_alumno(id),
    interes          VARCHAR(100) NOT NULL,
    CONSTRAINT uq_perfil_alumno_interes UNIQUE (perfil_alumno_id, interes)
);

-- perfil_instructor -------------------------------------------------------
CREATE TABLE perfil_instructor (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id            UUID NOT NULL REFERENCES usuario(id),
    especialidad          VARCHAR(150) NOT NULL,
    anios_experiencia     INT,
    descripcion           TEXT,
    estado_verificacion   VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo        TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted               BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_perfil_instructor_usuario UNIQUE (usuario_id)
);

-- documento_instructor (slice futuro; minima/plausible) --------------------
CREATE TABLE documento_instructor (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    perfil_instructor_id   UUID NOT NULL REFERENCES perfil_instructor(id),
    tipo_documento         VARCHAR(50) NOT NULL,
    url_archivo            VARCHAR(500) NOT NULL,
    estado                 VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted                BOOLEAN NOT NULL DEFAULT false
);

-- permiso / configuracion_rol (slice futuro; minima/plausible) -------------
CREATE TABLE permiso (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo       VARCHAR(100) NOT NULL,
    descripcion  VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted      BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_permiso_codigo UNIQUE (codigo)
);

CREATE TABLE configuracion_rol (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rol_id      UUID NOT NULL REFERENCES rol(id),
    permiso_id  UUID NOT NULL REFERENCES permiso(id),
    habilitado  BOOLEAN NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted     BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_configuracion_rol UNIQUE (rol_id, permiso_id)
);

-- audit_log (log de solo-append e inmutable; sin updated_at/deleted) -------
CREATE TABLE audit_log (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID REFERENCES usuario(id),
    accion      VARCHAR(50) NOT NULL,
    entidad     VARCHAR(100) NOT NULL,
    entidad_id  UUID,
    metadata    TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_log_actor ON audit_log (actor_id);
CREATE INDEX ix_audit_log_entidad ON audit_log (entidad, entidad_id);
