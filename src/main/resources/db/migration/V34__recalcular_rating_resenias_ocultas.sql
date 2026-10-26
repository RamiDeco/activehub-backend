-- Recalcula la calificación de todas las actividades.
--
-- Bug corregido junto con esta migración: resolverdenuncia con OCULTAR_RESENIA marcaba la reseña
-- como oculta pero NO llamaba a ActividadRepository.recalcularRating. La reseña desaparecía del
-- detalle de la actividad y su puntaje seguía sumando al promedio, hasta que otra cosa
-- recalculara esa actividad. ocultarresenia (el otro camino para ocultar) sí recalculaba.
--
-- El código ya recalcula al resolver la denuncia; esto corrige las actividades que quedaron con
-- el promedio desactualizado. Se recalculan todas y no sólo las que tienen reseñas ocultas: el
-- valor correcto siempre es el de esta fórmula, y en las que ya estaban bien no cambia nada.
--
-- Misma fórmula que ActividadRepository.recalcularRating: promedio de las reseñas no borradas y
-- no ocultas de todas las clases de la actividad; 0 si no tiene ninguna.
--
-- Idempotente (ver la nota de V18): correrla dos veces deja los mismos valores.

UPDATE actividad a
SET rating = COALESCE((
        SELECT AVG(r.puntaje)
        FROM resenia r
        JOIN clase c ON c.id = r.clase_id
        WHERE c.actividad_id = a.id AND r.deleted = false AND r.oculta = false
    ), 0);
