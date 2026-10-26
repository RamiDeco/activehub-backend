-- V35: documentación del instructor y "Volver a postularme" (2026-10-26).
--
-- Para volver a postularse después de un rechazo, el instructor tiene que subir documentación
-- NUEVA: posterior al rechazo. Hace falta saber cuándo fue el último rechazo, y el motivo y la
-- fecha se conservan después de reabrir (el administrador los ve para comparar).
ALTER TABLE perfil_instructor ADD COLUMN IF NOT EXISTS rechazado_at TIMESTAMPTZ;

-- Solicitudes ya rechazadas antes de esta versión: la mejor aproximación de la fecha del
-- rechazo es la última modificación del perfil. Idempotente: sólo completa las que faltan.
UPDATE perfil_instructor
SET rechazado_at = updated_at
WHERE estado_verificacion = 'RECHAZADO' AND rechazado_at IS NULL;

-- documento_instructor.estado existía desde V1 pero nunca se usó (siempre 'PENDIENTE'). Desde
-- esta versión distingue el documento vigente ('PENDIENTE') del que el instructor quitó
-- ('RETIRADO'): ese deja de verlo el instructor, pero el administrador lo conserva como
-- historial. No hace falta tocar datos: todo lo existente sigue vigente.
COMMENT ON COLUMN documento_instructor.estado IS 'PENDIENTE = vigente; RETIRADO = quitado por el instructor (historial del admin)';
