-- E3A-HU10 criterio 3: la calificación (1-5) es obligatoria y el comentario es OPCIONAL
-- ("Tu comentario (opcional)"). La tabla lo pedía NOT NULL, así que una reseña de solo
-- estrellas era imposible de guardar.
ALTER TABLE resenia ALTER COLUMN comentario DROP NOT NULL;
