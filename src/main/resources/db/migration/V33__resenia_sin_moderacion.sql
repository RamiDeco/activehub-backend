-- Reseñas sin moderación previa: una reseña se publica en el momento en que el alumno la crea.
--
-- Hasta acá toda reseña nacía con en_moderacion = true y no se veía ni sumaba al rating hasta
-- que un administrador la aprobaba (aprobarresenia / rechazarresenia / listarresenaspendientes).
-- Esos casos de uso se retiraron: el administrador no revisa cada reseña, atiende las denuncias
-- que hace el instructor (denunciarresenia -> resolverdenuncia con OCULTAR_RESENIA) y puede
-- ocultar una publicada (ocultarresenia). La edición (actualizarresenia) y la baja por el
-- alumno (eliminarresenia) también se retiraron: una reseña publicada no se modifica.
--
-- Qué hace esta migración:
--   1. Publica las reseñas que estaban esperando moderación. Sin esto quedarían invisibles para
--      siempre: ya no existe la pantalla ni el endpoint que las aprobaba.
--   2. Recalcula el rating de las actividades que tenían alguna, con la misma fórmula que
--      ActividadRepository.recalcularRating (reseñas no borradas y no ocultas).
--   3. Cambia el DEFAULT de la columna a false.
--
-- La columna en_moderacion NO se borra todavía, a propósito: la base de Supabase es compartida
-- por todo el equipo y un integrante que siga con el código anterior a este cambio la sigue
-- leyendo y escribiendo; con un DROP COLUMN cada consulta de reseñas de ese código fallaría.
-- El código nuevo ya no la mapea (Resenia no tiene el campo). Se elimina en una migración
-- posterior, cuando todos estén sobre esta versión.
--
-- Idempotente (ver la nota de V18): correrla dos veces no cambia nada la segunda.

UPDATE actividad a
SET rating = COALESCE((
        SELECT AVG(r.puntaje)
        FROM resenia r
        JOIN clase c ON c.id = r.clase_id
        WHERE c.actividad_id = a.id AND r.deleted = false AND r.oculta = false
    ), 0)
WHERE a.id IN (
    SELECT c.actividad_id
    FROM resenia r
    JOIN clase c ON c.id = r.clase_id
    WHERE r.en_moderacion = true AND r.deleted = false
);

UPDATE resenia SET en_moderacion = false WHERE en_moderacion = true;

ALTER TABLE resenia ALTER COLUMN en_moderacion SET DEFAULT false;
