-- Campos y tablas de la seccion 2 de la especificacion que nunca se habian creado.
-- Cada bloque dice que HU/criterio quedaba inalcanzable sin el.

-- usuario.dni ---------------------------------------------------------------
-- Precondicion de E1A-HU03 y E1A-HU04 ("no debe existir una cuenta activa con el
-- mismo correo electronico o DNI") y credencial alternativa de login (nota de la
-- epica E1A). Nullable porque las cuentas ya creadas no lo tienen; el indice
-- unico es parcial para que la baja logica libere el DNI, igual que con el email.
ALTER TABLE usuario ADD COLUMN dni VARCHAR(20);
CREATE UNIQUE INDEX ux_usuario_dni ON usuario (dni) WHERE deleted = false AND dni IS NOT NULL;

-- agenda_clases -------------------------------------------------------------
-- E2I-HU06 criterios 6 y 7: el check "Repetir cada semana" tiene que generar la
-- recurrencia, no N clases sueltas. La agenda es la que define dia, horario,
-- cupo y vigencia; las Clases se materializan una semana antes de dictarse.
CREATE TABLE agenda_clases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actividad_id    UUID NOT NULL REFERENCES actividad(id),
    dia_semana      SMALLINT NOT NULL CHECK (dia_semana BETWEEN 1 AND 7),
    hora_inicio     TIME NOT NULL,
    hora_fin        TIME NOT NULL,
    edad_min        INT,
    edad_max        INT,
    cupos_max       INT NOT NULL CHECK (cupos_max > 0),
    vigencia_desde  DATE NOT NULL,
    vigencia_hasta  DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    CHECK (hora_fin > hora_inicio),
    CHECK (vigencia_hasta IS NULL OR vigencia_hasta >= vigencia_desde),
    CHECK (edad_min IS NULL OR edad_max IS NULL OR edad_max >= edad_min)
);
CREATE INDEX ix_agenda_clases_actividad ON agenda_clases (actividad_id);

-- clase.hora_fin / clase.agenda_clases_id ------------------------------------
-- E2I-HU06 criterios 1 y 4: el modal pide "Hora fin*" y exige validar que sea
-- posterior al inicio. Sin la columna, el campo no se podia ni guardar.
-- Backfill con +1h para las clases ya cargadas (duracion por defecto), y recien
-- despues NOT NULL: dejarla nullable haria opcional un dato que la HU pide.
ALTER TABLE clase ADD COLUMN hora_fin TIMESTAMPTZ;
UPDATE clase SET hora_fin = fecha_hora + INTERVAL '1 hour' WHERE hora_fin IS NULL;
ALTER TABLE clase ALTER COLUMN hora_fin SET NOT NULL;
ALTER TABLE clase ADD CONSTRAINT ck_clase_hora_fin CHECK (hora_fin > fecha_hora);

ALTER TABLE clase ADD COLUMN agenda_clases_id UUID REFERENCES agenda_clases(id);
CREATE INDEX ix_clase_agenda ON clase (agenda_clases_id);
-- Una agenda no puede materializar dos veces la misma fecha.
CREATE UNIQUE INDEX ux_clase_agenda_fecha ON clase (agenda_clases_id, fecha_hora)
    WHERE agenda_clases_id IS NOT NULL AND deleted = false;

-- actividad.duracion_min ------------------------------------------------------
-- E2I-HU03 criterio 1: el formulario ya pedia la duracion y no se guardaba en
-- ningun lado. 60 como default para las actividades existentes.
ALTER TABLE actividad ADD COLUMN duracion_min INT NOT NULL DEFAULT 60;
ALTER TABLE actividad ADD CONSTRAINT ck_actividad_duracion CHECK (duracion_min > 0);

-- actividad.cupos_max: se va ---------------------------------------------------
-- La seccion 2 pone el cupo en Clase y en AgendaClases, nunca en Actividad, y el
-- campo era peligroso: el formulario del frontend le mandaba 20 fijo hardcodeado
-- y ese 20 terminaba siendo el cupo real de toda clase creada sin cupo explicito
-- (CrearClaseService caia a actividad.getCuposMax()). El cupo ahora es obligatorio
-- al crear la clase.
ALTER TABLE actividad DROP COLUMN cupos_max;

-- actividad_imagen ------------------------------------------------------------
-- Seccion 2 pide `imagenes[]`, en plural. Hoy solo existe actividad.foto_path
-- (una sola imagen), que se mantiene como portada para no romper lo que ya anda.
CREATE TABLE actividad_imagen (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actividad_id  UUID NOT NULL REFERENCES actividad(id),
    path          VARCHAR(255) NOT NULL,
    orden         INT NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_actividad_imagen_actividad ON actividad_imagen (actividad_id, orden);

-- Migra la portada existente para que `imagenes[]` no arranque vacia donde ya hay foto.
INSERT INTO actividad_imagen (actividad_id, path, orden)
SELECT id, foto_path, 0 FROM actividad WHERE foto_path IS NOT NULL;

-- audit_log.ip ----------------------------------------------------------------
-- Seccion 2 y checklist de cierre. 45 caracteres = IPv6 con sufijo IPv4 mapeado.
ALTER TABLE audit_log ADD COLUMN ip VARCHAR(45);

-- denuncia: denunciar una resenia, y la resolucion visible para el alumno ------
-- E2I-HU11 criterio 4: el instructor denuncia una RESENIA, no una clase. Con
-- clase_id NOT NULL eso era directamente imposible de guardar.
-- E3A-HU11 criterios 2 y 7: el alumno tiene que ver como se resolvio la suya.
ALTER TABLE denuncia ALTER COLUMN clase_id DROP NOT NULL;
ALTER TABLE denuncia ADD COLUMN resenia_id UUID REFERENCES resenia(id);
ALTER TABLE denuncia ADD COLUMN denunciante_id UUID REFERENCES usuario(id);
ALTER TABLE denuncia ADD COLUMN resolucion VARCHAR(30)
    CHECK (resolucion IS NULL OR resolucion IN
        ('REINTEGRAR','SUSPENDER','PENALIZAR','DESESTIMAR','OCULTAR_RESENIA'));
ALTER TABLE denuncia ADD COLUMN detalle TEXT;
ALTER TABLE denuncia ADD CONSTRAINT ck_denuncia_objeto
    CHECK ((clase_id IS NOT NULL) <> (resenia_id IS NOT NULL));

-- alumno_id sigue siendo el denunciante en las denuncias de clase ya cargadas.
UPDATE denuncia SET denunciante_id = alumno_id WHERE denunciante_id IS NULL;
ALTER TABLE denuncia ALTER COLUMN denunciante_id SET NOT NULL;
-- A partir de aca alumno_id solo aplica a las denuncias de clase.
ALTER TABLE denuncia ALTER COLUMN alumno_id DROP NOT NULL;

CREATE INDEX ix_denuncia_resenia ON denuncia (resenia_id);
CREATE INDEX ix_denuncia_denunciante ON denuncia (denunciante_id);
-- El indice unico viejo (clase_id, alumno_id) no cubre las de resenia: en Postgres
-- dos filas con clase_id NULL no chocan entre si.
CREATE UNIQUE INDEX ux_denuncia_denunciante_resenia ON denuncia (resenia_id, denunciante_id)
    WHERE resenia_id IS NOT NULL;

-- resenia.respuesta_instructor ------------------------------------------------
-- E2I-HU11 criterio 4: "Enviar respuesta" no tenia donde guardar nada.
ALTER TABLE resenia ADD COLUMN respuesta_instructor TEXT;
ALTER TABLE resenia ADD COLUMN respuesta_instructor_at TIMESTAMPTZ;
-- Ocultar una resenia por denuncia resuelta sin borrarla (E2I-HU11).
ALTER TABLE resenia ADD COLUMN oculta BOOLEAN NOT NULL DEFAULT false;
