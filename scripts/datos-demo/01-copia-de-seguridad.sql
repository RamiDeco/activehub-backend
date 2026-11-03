-- =============================================================================
-- ActiveHub - 01. Copia de seguridad antes de cargar los datos de demostracion
-- =============================================================================
-- Copia TODAS las tablas del esquema public a un esquema aparte de la misma base,
-- "respaldo_pre_demo". No modifica nada de public.
--
-- Uso: pegar el archivo entero en el SQL Editor de Supabase y ejecutar. Al final
-- muestra cuantas filas copio de cada tabla.
--
-- Si el esquema ya existe (una copia anterior), el script se detiene sin tocarlo:
-- para pisar una copia vieja hay que borrarla a mano con
--     DROP SCHEMA respaldo_pre_demo CASCADE;
-- y recien despues volver a ejecutar este archivo.
--
-- Para volver atras: 02-restaurar-copia.sql.
-- Cuando ya no haga falta la copia: DROP SCHEMA respaldo_pre_demo CASCADE;
-- (la copia incluye password_hash de los usuarios: no dejarla para siempre).
-- =============================================================================

DO $$
DECLARE
    t record;
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'respaldo_pre_demo') THEN
        RAISE EXCEPTION 'Ya existe el esquema respaldo_pre_demo. No se pisa una copia anterior: borrala a mano si ya no sirve.';
    END IF;

    EXECUTE 'CREATE SCHEMA respaldo_pre_demo';

    FOR t IN
        SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename
    LOOP
        EXECUTE format('CREATE TABLE respaldo_pre_demo.%I AS TABLE public.%I', t.tablename, t.tablename);
    END LOOP;

    -- Marca de cuando se hizo, para no confundirla con otra copia.
    EXECUTE 'CREATE TABLE respaldo_pre_demo._respaldo_info AS SELECT now() AS creado_at';
END $$;

-- Resumen: filas por tabla, en public y en la copia (tienen que coincidir).
SELECT c.relname AS tabla,
       (xpath('/row/n/text()', query_to_xml(format('SELECT count(*) AS n FROM public.%I', c.relname), false, true, '')))[1]::text::bigint AS filas_public,
       (xpath('/row/n/text()', query_to_xml(format('SELECT count(*) AS n FROM respaldo_pre_demo.%I', c.relname), false, true, '')))[1]::text::bigint AS filas_copia
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'public' AND c.relkind = 'r'
ORDER BY c.relname;
