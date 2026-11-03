-- =============================================================================
-- ActiveHub - 02. Restaurar la copia de seguridad (volver atras)
-- =============================================================================
-- Deja las tablas de public exactamente como estaban al ejecutar
-- 01-copia-de-seguridad.sql. Se usa SOLO si la carga de demostracion salio mal
-- o si se quiere deshacer.
--
-- Todo corre en UNA transaccion: si algo falla, no cambia nada.
--
-- Que hace:
--   1. Vacia (TRUNCATE) todas las tablas de public que estan en la copia, menos
--      flyway_schema_history (el historial de migraciones no se toca).
--   2. Las vuelve a llenar desde respaldo_pre_demo, padres antes que hijos,
--      copiando por nombre de columna (no por posicion).
--
-- Importante:
--   - Pierde todo lo que se haya hecho en la base DESPUES de la copia (por
--     ejemplo, alguien del equipo que se registro o se inscribio).
--   - Si despues de la copia se aplico una migracion que agrego una columna
--     NOT NULL sin default, la restauracion falla y no cambia nada.
--   - Los archivos (fotos, documentos) no estan en la base: no se tocan.
-- =============================================================================

BEGIN;

DO $$
DECLARE
    pendientes text[];
    hechas text[] := ARRAY[]::text[];
    t text;
    avanzo boolean;
    columnas text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'respaldo_pre_demo') THEN
        RAISE EXCEPTION 'No existe el esquema respaldo_pre_demo: no hay copia para restaurar.';
    END IF;

    -- Tablas que estan en las dos partes (la copia y public).
    SELECT array_agg(p.tablename ORDER BY p.tablename) INTO pendientes
    FROM pg_tables p
    JOIN pg_tables r ON r.schemaname = 'respaldo_pre_demo' AND r.tablename = p.tablename
    WHERE p.schemaname = 'public' AND p.tablename <> 'flyway_schema_history';

    EXECUTE 'TRUNCATE ' || (SELECT string_agg(format('public.%I', x), ', ') FROM unnest(pendientes) x);

    -- Orden topologico por claves foraneas: se inserta una tabla cuando todas
    -- sus tablas padre (dentro del conjunto) ya se insertaron.
    WHILE array_length(pendientes, 1) > 0 LOOP
        avanzo := false;
        FOREACH t IN ARRAY pendientes LOOP
            IF NOT EXISTS (
                SELECT 1 FROM pg_constraint c
                WHERE c.contype = 'f'
                  AND c.conrelid = format('public.%I', t)::regclass
                  AND c.confrelid <> c.conrelid
                  AND (SELECT relname FROM pg_class WHERE oid = c.confrelid) = ANY (pendientes)
            ) THEN
                SELECT string_agg(format('%I', a.column_name), ', ' ORDER BY a.ordinal_position) INTO columnas
                FROM information_schema.columns a
                JOIN information_schema.columns b
                  ON b.table_schema = 'respaldo_pre_demo' AND b.table_name = a.table_name AND b.column_name = a.column_name
                WHERE a.table_schema = 'public' AND a.table_name = t;

                EXECUTE format('INSERT INTO public.%I (%s) SELECT %s FROM respaldo_pre_demo.%I', t, columnas, columnas, t);
                pendientes := array_remove(pendientes, t);
                hechas := hechas || t;
                avanzo := true;
            END IF;
        END LOOP;
        IF NOT avanzo THEN
            RAISE EXCEPTION 'Dependencia circular entre tablas: %', pendientes;
        END IF;
    END LOOP;

    RAISE NOTICE 'Restauradas % tablas.', array_length(hechas, 1);
END $$;

COMMIT;
