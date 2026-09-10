-- E4Ad-HU05: "Administrar Niveles de Intensidad" pide un ABM completo (crear, editar,
-- eliminar, con control de duplicados y de niveles en uso). Hasta acá NivelIntensidad era
-- un `enum` de tres valores fijos en el código y la pestaña era de sólo lectura: los 8
-- criterios de la HU no se podían cumplir sin volver a compilar.
--
-- Pasa a ser una entidad, igual que Categoría y TipoActividad. La sección 2 del backlog las
-- trata como tres cosas distintas y ahora las tres tienen el mismo tratamiento.
--
-- La columna vieja `actividad.nivel_intensidad` guardaba la ETIQUETA ("Física baja"), no el
-- nombre del enum, así que el backfill puede hacerse por texto sin tabla de traducción.
--
-- Idempotente a propósito: la base es Supabase y las migraciones viajan por el pooler, que no
-- garantiza DDL transaccional (ver la nota de V18).

CREATE TABLE IF NOT EXISTS nivel_intensidad (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre      VARCHAR(60) NOT NULL,
    descripcion VARCHAR(300) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted     BOOLEAN NOT NULL DEFAULT false
);

-- Único sobre los vivos: un nombre borrado lógicamente no debe bloquear el alta de uno nuevo
-- (RN-13 dice baja lógica siempre, así que las filas borradas se quedan para siempre).
CREATE UNIQUE INDEX IF NOT EXISTS ux_nivel_intensidad_nombre
    ON nivel_intensidad (lower(nombre)) WHERE deleted = false;

-- Los tres niveles que hoy existen, con las descripciones que ya mostraba la pantalla.
INSERT INTO nivel_intensidad (nombre, descripcion)
SELECT v.nombre, v.descripcion
FROM (VALUES
    ('Física baja',  'Bajo impacto físico, apto para todo público. Ej: meditación, pilates, estiramiento.'),
    ('Física media', 'Esfuerzo moderado, requiere algo de condición física previa. Ej: senderismo, gimnasia funcional.'),
    ('Física alta',  'Alta exigencia física y cardiovascular. Recomendado para participantes entrenados.')
) AS v(nombre, descripcion)
WHERE NOT EXISTS (
    SELECT 1 FROM nivel_intensidad n WHERE lower(n.nombre) = lower(v.nombre) AND n.deleted = false
);

ALTER TABLE actividad ADD COLUMN IF NOT EXISTS nivel_intensidad_id UUID REFERENCES nivel_intensidad(id);

UPDATE actividad a
SET nivel_intensidad_id = n.id
FROM nivel_intensidad n
WHERE lower(n.nombre) = lower(a.nivel_intensidad) AND n.deleted = false AND a.nivel_intensidad_id IS NULL;

-- Red de seguridad: una actividad sin nivel reconocible cae al más bajo en vez de romper el
-- NOT NULL de abajo. No debería entrar ninguna — la columna vieja era NOT NULL con sólo
-- esos tres valores posibles.
UPDATE actividad
SET nivel_intensidad_id = (SELECT id FROM nivel_intensidad WHERE nombre = 'Física baja' AND deleted = false)
WHERE nivel_intensidad_id IS NULL;

ALTER TABLE actividad ALTER COLUMN nivel_intensidad_id SET NOT NULL;
CREATE INDEX IF NOT EXISTS ix_actividad_nivel_intensidad ON actividad (nivel_intensidad_id);
ALTER TABLE actividad DROP COLUMN IF EXISTS nivel_intensidad;
