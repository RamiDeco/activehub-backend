-- categoria -----------------------------------------------------------
CREATE TABLE categoria (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted     BOOLEAN NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX ux_categoria_nombre_activa ON categoria (lower(nombre)) WHERE deleted = false;

-- tipo_actividad ------------------------------------------------------------
CREATE TABLE tipo_actividad (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre        VARCHAR(100) NOT NULL,
    categoria_id  UUID NOT NULL REFERENCES categoria(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted       BOOLEAN NOT NULL DEFAULT false
);
CREATE UNIQUE INDEX ux_tipo_actividad_nombre_cat_activo
    ON tipo_actividad (categoria_id, lower(nombre)) WHERE deleted = false;
CREATE INDEX ix_tipo_actividad_categoria ON tipo_actividad (categoria_id);

-- actividad -------------------------------------------------------------
CREATE TABLE actividad (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre             VARCHAR(150) NOT NULL,
    descripcion        TEXT NOT NULL,
    tipo_actividad_id  UUID NOT NULL REFERENCES tipo_actividad(id),
    nivel_intensidad   VARCHAR(20) NOT NULL
        CHECK (nivel_intensidad IN ('Física baja','Física media','Física alta')),
    instructor_id      UUID NOT NULL REFERENCES usuario(id),
    precio             NUMERIC(10,2) NOT NULL CHECK (precio >= 0),
    ubicacion          VARCHAR(255) NOT NULL,
    photo_tint         VARCHAR(255) NOT NULL,
    rating             NUMERIC(3,2) NOT NULL DEFAULT 0,
    cupos_max          INT NOT NULL CHECK (cupos_max > 0),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted            BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX ix_actividad_tipo ON actividad (tipo_actividad_id);
CREATE INDEX ix_actividad_instructor ON actividad (instructor_id);

-- clase -------------------------------------------------------------------
CREATE TABLE clase (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actividad_id     UUID NOT NULL REFERENCES actividad(id),
    fecha_hora       TIMESTAMPTZ NOT NULL,
    estado           VARCHAR(20) NOT NULL DEFAULT 'Programada'
        CHECK (estado IN ('Programada','Habilitada','Cancelada','Finalizada')),
    cupos_max        INT NOT NULL CHECK (cupos_max > 0),
    cupos_ocupados   INT NOT NULL DEFAULT 0 CHECK (cupos_ocupados >= 0),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted          BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX ix_clase_actividad ON clase (actividad_id);
CREATE INDEX ix_clase_fecha_hora ON clase (fecha_hora);
