-- Señales de comportamiento del alumno, para el motor de recomendaciones.
--
-- Hasta ahora "Recomendado para vos" cruzaba los intereses declarados contra el tipo de cada
-- actividad y nada mas: no puntuaba, no ordenaba y dos alumnos con los mismos intereses veian
-- exactamente lo mismo. Las senales que faltaban estaban repartidas en tablas que ya existen
-- (inscripcion, actividad_favorita, resenia) y dos que no se guardaban en ningun lado: que
-- actividades mira el alumno y que busca. Esta tabla cubre esas dos.
--
-- Por que UNA tabla y no dos: son el mismo hecho -- "el alumno mostro interes en algo, en tal
-- momento" -- y el motor las consume juntas y con el mismo decaimiento temporal. Separarlas
-- obligaria a dos consultas y dos repositorios para sumar dos numeros.
--
-- Es de SOLO APPEND e inmutable, como audit_log y por la misma razon: no tiene sentido
-- actualizar ni "soft-deletar" un hecho ya ocurrido. Tampoco extiende BaseEntity.
--
-- `tipo` no lleva CHECK, por la misma razon que notificacion.tipo: el enum crece sin
-- migracion. Hoy son BUSQUEDA y VISTA_ACTIVIDAD.
--
-- Columnas excluyentes segun el tipo, a proposito y sin CHECK que lo fuerce: `actividad_id`
-- solo lo usa VISTA_ACTIVIDAD y `termino` solo BUSQUEDA. Un CHECK aca obligaria a migrar cada
-- vez que aparezca un tipo nuevo con otra forma, que es justo lo que se quiso evitar.
--
-- Privacidad: se guarda el termino tal cual lo tipeo el alumno, recortado a 120 caracteres y
-- validado con @SinHtml en el DTO. Es dato personal de comportamiento: no se expone en ningun
-- endpoint de lectura (ni siquiera al propio alumno) y solo lo lee el motor, en memoria.
--
-- Idempotente: la base es Supabase y las migraciones viajan por el pooler, que no garantiza
-- DDL transaccional (ver la nota de V18).

CREATE TABLE IF NOT EXISTS interaccion_alumno (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id   UUID        NOT NULL REFERENCES usuario (id),
    tipo         VARCHAR(30) NOT NULL,
    actividad_id UUID        REFERENCES actividad (id),
    termino      VARCHAR(120),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- La consulta caliente es "las ultimas N interacciones de este alumno": el motor aplica un
-- decaimiento por antiguedad, asi que siempre lee ordenado por fecha descendente.
CREATE INDEX IF NOT EXISTS ix_interaccion_alumno_usuario
    ON interaccion_alumno (usuario_id, created_at DESC);
