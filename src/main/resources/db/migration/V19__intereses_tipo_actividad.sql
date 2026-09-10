-- Los intereses del alumno dejan de ser texto libre y pasan a apuntar a TipoActividad.
--
-- Antes eran strings sueltos ("Trekking", "Gimnasia") sin ninguna relación con la taxonomía,
-- así que el sistema no sabía que Trekking es de Aventura y "Recomendado para vos" tenía que
-- adivinar comparando texto contra el nombre de la actividad.
--
-- Idempotente a propósito: la base es Supabase y las migraciones viajan por el pooler, que no
-- garantiza DDL transaccional (ver la nota de V18).

-- 1) Tipos que existían solo como texto en los intereses. Se crean para no perder los datos
--    de nadie al migrar: cada interés viejo tiene que encontrar su tipo.
INSERT INTO tipo_actividad (id, nombre, categoria_id)
SELECT gen_random_uuid(), v.tipo, c.id
FROM (VALUES
    ('Gimnasia',   'Deportivas'),
    ('Ciclismo',   'Deportivas'),
    ('Senderismo', 'Aventura'),
    ('Meditación', 'Bienestar')
) AS v(tipo, categoria)
JOIN categoria c ON lower(c.nombre) = lower(v.categoria) AND c.deleted = false
WHERE NOT EXISTS (
  SELECT 1 FROM tipo_actividad t
  WHERE t.categoria_id = c.id AND lower(t.nombre) = lower(v.tipo) AND t.deleted = false
);

-- 2) La columna nueva
ALTER TABLE perfil_alumno_interes ADD COLUMN IF NOT EXISTS tipo_actividad_id UUID REFERENCES tipo_actividad(id);

-- 3) Backfill por nombre (case-insensitive), que es como estaban guardados
UPDATE perfil_alumno_interes i
SET tipo_actividad_id = t.id
FROM tipo_actividad t
WHERE lower(t.nombre) = lower(i.interes) AND t.deleted = false AND i.tipo_actividad_id IS NULL;

-- 4) Lo que no encontró tipo se descarta: un interés que no es un tipo de actividad no
--    puede recomendar nada. Después del paso 1 no debería quedar ninguno.
DELETE FROM perfil_alumno_interes WHERE tipo_actividad_id IS NULL;

-- 5) Dos textos distintos pudieron mapear al mismo tipo: se deduplica antes del índice único
DELETE FROM perfil_alumno_interes a
USING perfil_alumno_interes b
WHERE a.perfil_alumno_id = b.perfil_alumno_id
  AND a.tipo_actividad_id = b.tipo_actividad_id
  AND a.id > b.id;

-- 6) La relación pasa a ser obligatoria y el texto libre se va
ALTER TABLE perfil_alumno_interes ALTER COLUMN tipo_actividad_id SET NOT NULL;
ALTER TABLE perfil_alumno_interes DROP CONSTRAINT IF EXISTS uq_perfil_alumno_interes;
CREATE UNIQUE INDEX IF NOT EXISTS ux_perfil_alumno_interes ON perfil_alumno_interes (perfil_alumno_id, tipo_actividad_id);
CREATE INDEX IF NOT EXISTS ix_perfil_alumno_interes_tipo ON perfil_alumno_interes (tipo_actividad_id);
ALTER TABLE perfil_alumno_interes DROP COLUMN IF EXISTS interes;
