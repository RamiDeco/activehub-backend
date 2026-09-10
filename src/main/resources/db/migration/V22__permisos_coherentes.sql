-- RN-19, tercera pasada. Reportado: "los permisos andan raro y no tienen sentido".
--
-- Dos problemas distintos, los dos de configuracion, no de codigo:
--
-- 1) El ADMIN nacia (V18) con TODOS los permisos menos `inscripciones.gestionar`. Eso le daba
--    `actividades.publicar`, `clases.gestionar`, `cobros.confirmar`, `resenias.escribir`,
--    `denuncias.crear` y `resenias.responder` — permisos de las otras dos areas. Efecto
--    visible: al administrador le aparecian los botones "Ir a Instructor" e "Ir a Alumno" en
--    la barra, porque el frontend decide el area por permisos (RN-19) y el admin cumplia los
--    requisitos de las tres. El administrador gobierna la plataforma; no publica actividades
--    ni se inscribe. Se le dejan solo los permisos de administracion.
--
-- 2) `catalogo.explorar` no es una decision: cualquiera que entre a la plataforma explora el
--    catalogo, y la unica guarda que lo usa son los favoritos. Como checkbox solo servia para
--    romper un rol. Pasa a ser un permiso IMPLICITO: se conserva en la tabla y en las guardas
--    (no hay que reprogramar nada), se habilita para todos los roles y desaparece de la lista
--    de seleccion de la pantalla "Roles y permisos". Ver `Permiso.IMPLICITOS` en el codigo.
--
-- Idempotente a proposito: la base es Supabase y las migraciones viajan por el pooler, que no
-- garantiza DDL transaccional (ver la nota de V18).

-- (2) Implicito: habilitado para todo rol, presente o futuro.
INSERT INTO configuracion_rol (rol_id, permiso_id, habilitado)
SELECT r.id, p.id, true
FROM rol r
CROSS JOIN permiso p
WHERE r.deleted = false AND p.clave = 'catalogo.explorar'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

UPDATE configuracion_rol SET habilitado = true, updated_at = now()
WHERE permiso_id = (SELECT id FROM permiso WHERE clave = 'catalogo.explorar');

-- (1) El ADMIN se queda solo con administracion. No se tocan ALUMNO, INSTRUCTOR ni los roles
-- que haya creado el administrador a mano.
UPDATE configuracion_rol c SET habilitado = false, updated_at = now()
FROM rol r, permiso p
WHERE c.rol_id = r.id AND c.permiso_id = p.id
  AND r.nombre = 'ADMIN'
  AND p.clave IN (
      'inscripciones.gestionar', 'resenias.escribir', 'denuncias.crear',
      'actividades.publicar', 'clases.gestionar', 'cobros.confirmar', 'resenias.responder');
