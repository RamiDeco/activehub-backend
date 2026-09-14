-- El registro manual de asistencia quedó fuera de alcance por decisión del usuario,
-- alineada con la spec: "El registro de asistencia manual fue retirado de esta vista
-- (no debe implementarse)" (E2I-HU07) y "la asistencia no se considera como métrica"
-- (E2I-HU10). Se elimina la columna que agregaba V6 junto con su usecase.
ALTER TABLE inscripcion DROP COLUMN IF EXISTS presente;
