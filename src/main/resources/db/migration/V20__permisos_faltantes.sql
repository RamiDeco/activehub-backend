-- RN-19, segunda pasada. V18 movió a la base los permisos de los módulos de administración,
-- pero quedaron 14 endpoints con `hasRole('ALUMNO'|'INSTRUCTOR')` hardcodeado en el
-- @PreAuthorize y 4 con un `ROLE_ADMIN` chequeado a mano dentro del método. Efecto práctico:
-- darle `actividades.publicar` a un rol nuevo no le habilitaba nada, porque la guarda real
-- seguía preguntando por el nombre del rol. Es exactamente el síntoma reportado
-- ("los permisos no están andando").
--
-- Faltaban dos claves para poder expresar en la matriz lo que el código decidía solo:
--
--   * actividades.moderar  — el `esAdmin` de eliminar/editar actividad y de la galería de
--                            imágenes. No es "publicar": es operar sobre la actividad de OTRO
--                            instructor. Hasta ahora estaba cableado al nombre del rol.
--   * resenias.responder   — las reseñas que recibe el instructor (listar, responder,
--                            reportar). Iban con hasRole('INSTRUCTOR').
--
-- Idempotente a propósito: la base es Supabase y las migraciones viajan por el pooler, que no
-- garantiza DDL transaccional (ver la nota de V18).

INSERT INTO permiso (clave, modulo, accion, critico, orden) VALUES
    ('resenias.responder',  'Reseñas',     'Responder y reportar las reseñas recibidas', false, 65),
    ('actividades.moderar', 'Actividades', 'Editar y eliminar actividades de otro instructor', false, 145)
ON CONFLICT (clave) DO NOTHING;

-- Configuración inicial = lo que cada rol podía hacer hasta hoy, para que cerrar el agujero
-- no le saque nada a nadie: el instructor ya respondía reseñas y el admin ya moderaba.
INSERT INTO configuracion_rol (rol_id, permiso_id, habilitado)
SELECT r.id, p.id,
       CASE
           WHEN r.nombre = 'INSTRUCTOR' THEN p.clave = 'resenias.responder'
           WHEN r.nombre = 'ADMIN'      THEN p.clave = 'actividades.moderar'
           ELSE false
       END
FROM rol r
CROSS JOIN permiso p
WHERE r.deleted = false
  AND p.clave IN ('resenias.responder', 'actividades.moderar')
ON CONFLICT (rol_id, permiso_id) DO NOTHING;

-- Los favoritos pasan a pedir catalogo.explorar, que hasta ahora era la única clave del
-- catálogo sin ninguna guarda que la usara (estaba en el catálogo y no hacía nada).
-- El alumno ya la tiene desde V18; el instructor también.
