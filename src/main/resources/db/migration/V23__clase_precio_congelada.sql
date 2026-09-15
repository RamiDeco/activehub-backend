-- El precio pasa a ser un dato de la CLASE, no solo de la actividad.
--
-- Reportado: "una vez que la clase entro en inscripcion habilitada y tiene al menos un
-- inscripto, no debe poder editarse ningun dato, y si cambia el monto de la actividad
-- tampoco debe cambiar el monto de la clase".
--
-- Hasta hoy `Clase` no tenia precio: `InscribirseService` leia `clase.getActividad().getPrecio()`
-- en el momento de cobrar. Con eso, editar el precio de la actividad reescribia el precio de
-- TODAS sus clases, incluidas las que ya tenian gente anotada pagando otro numero — dos
-- alumnos de la misma clase podian terminar con montos distintos segun cuando se inscribieron.
-- Ahora cada clase nace con su propio precio (copiado de la actividad) y editar la actividad
-- solo lo propaga a las clases que todavia no estan congeladas.
--
-- Idempotente: la base es Supabase y las migraciones viajan por el pooler, que no garantiza
-- DDL transaccional (ver la nota de V18).

ALTER TABLE clase ADD COLUMN IF NOT EXISTS precio NUMERIC(10,2);

-- Backfill: el precio vigente de la actividad es el mejor valor conocido para lo ya creado.
UPDATE clase c
SET precio = a.precio
FROM actividad a
WHERE c.actividad_id = a.id AND c.precio IS NULL;

ALTER TABLE clase ALTER COLUMN precio SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_clase_precio') THEN
        ALTER TABLE clase ADD CONSTRAINT ck_clase_precio CHECK (precio >= 0);
    END IF;
END $$;
