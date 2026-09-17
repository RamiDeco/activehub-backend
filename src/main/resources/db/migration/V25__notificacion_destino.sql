-- Notificaciones clickeables: cada notificacion guarda A DONDE lleva el click.
--
-- Reportado: la campana mostraba texto muerto. "Nueva inscripcion en la clase de Yoga del
-- 12/03" no llevaba a ningun lado; habia que salir a buscar la clase a mano.
--
-- `entidad_id` no alcanzaba para resolverlo del lado del cliente: su significado cambia segun
-- el tipo (a veces es una inscripcion, a veces una clase, una resenia, una denuncia o un
-- usuario) y ademas el destino util NO siempre es esa entidad -- al instructor una inscripcion
-- nueva le sirve abierta en el roster de la CLASE, y un horario nuevo de un favorito le sirve
-- al alumno abierto en la ACTIVIDAD, que es donde puede anotarse. Esa resolucion la hace el
-- usecase, que ya tiene la cadena cargada, y queda persistida en estas dos columnas.
--
-- Idempotente: la base es Supabase y las migraciones viajan por el pooler, que no garantiza
-- DDL transaccional (ver la nota de V18).

ALTER TABLE notificacion ADD COLUMN IF NOT EXISTS destino_tipo VARCHAR(30);
ALTER TABLE notificacion ADD COLUMN IF NOT EXISTS destino_id   UUID;

-- Backfill de lo ya emitido. Se deriva de (tipo, entidad_id) porque para cada tipo el
-- `entidad_id` historico es siempre la misma clase de registro. Las dos excepciones conocidas
-- se resuelven navegando la relacion:
--
--  * NUEVO_HORARIO_FAVORITO guardaba la clase; el destino es su actividad.
--  * NUEVA_INSCRIPCION / ALUMNO_CANCELO_INSCRIPCION guardaban la inscripcion; el destino del
--    instructor es la clase de esa inscripcion.
--
-- INSCRIPCION_CANCELADA es el unico tipo con `entidad_id` ambiguo (cancelarinscripcion guarda
-- la inscripcion; resolverdenuncia guardaba la clase), asi que se decide fila por fila segun
-- en que tabla existe el id. Sin coincidencia en ninguna, queda NINGUNO.

UPDATE notificacion n SET destino_tipo = 'ACTIVIDAD', destino_id = c.actividad_id
FROM clase c
WHERE n.destino_tipo IS NULL AND n.tipo = 'NUEVO_HORARIO_FAVORITO' AND c.id = n.entidad_id;

UPDATE notificacion n SET destino_tipo = 'CLASE', destino_id = i.clase_id
FROM inscripcion i
WHERE n.destino_tipo IS NULL
  AND n.tipo IN ('NUEVA_INSCRIPCION', 'ALUMNO_CANCELO_INSCRIPCION')
  AND i.id = n.entidad_id;

UPDATE notificacion n SET destino_tipo = 'INSCRIPCION', destino_id = n.entidad_id
WHERE n.destino_tipo IS NULL
  AND n.tipo IN ('INSCRIPCION_CONFIRMADA', 'COBRO_EFECTIVO_CONFIRMADO', 'INSCRIPCION_CANCELADA')
  AND EXISTS (SELECT 1 FROM inscripcion i WHERE i.id = n.entidad_id);

UPDATE notificacion n SET destino_tipo = 'CLASE', destino_id = n.entidad_id
WHERE n.destino_tipo IS NULL
  AND n.tipo IN ('CLASE_CANCELADA', 'AUSENCIA_PROFESOR', 'INSCRIPCION_CANCELADA')
  AND EXISTS (SELECT 1 FROM clase c WHERE c.id = n.entidad_id);

UPDATE notificacion n SET destino_tipo = 'RESENIA', destino_id = n.entidad_id
WHERE n.destino_tipo IS NULL
  AND n.tipo IN ('RESENIA_APROBADA', 'RESENIA_RESPONDIDA', 'NUEVA_RESENIA')
  AND EXISTS (SELECT 1 FROM resenia r WHERE r.id = n.entidad_id AND r.deleted = false);

-- Una denuncia DE RESEÑA se resuelve mirando la reseña, no la bandeja de denuncias: ahi
-- llegan tanto el instructor que denuncio como el alumno al que le ocultaron el comentario, y
-- ninguno de los dos tiene la denuncia listada en su pantalla.
UPDATE notificacion n SET destino_tipo = 'RESENIA', destino_id = d.resenia_id
FROM denuncia d
WHERE n.destino_tipo IS NULL
  AND n.tipo = 'DENUNCIA_RESUELTA'
  AND d.id = n.entidad_id
  AND d.resenia_id IS NOT NULL;

-- La denuncia de clase del alumno (DENUNCIA_RESUELTA) abre "Mis denuncias". Las del instructor
-- (DENUNCIA_RECIBIDA / DENUNCIA_DESESTIMADA / INSTRUCTOR_SUSPENDIDO) no tienen bandeja propia:
-- se las manda a la clase denunciada, que es el contexto del reclamo.
UPDATE notificacion n SET destino_tipo = 'DENUNCIA', destino_id = n.entidad_id
WHERE n.destino_tipo IS NULL
  AND n.tipo = 'DENUNCIA_RESUELTA'
  AND EXISTS (SELECT 1 FROM denuncia d WHERE d.id = n.entidad_id);

UPDATE notificacion n SET destino_tipo = 'CLASE', destino_id = d.clase_id
FROM denuncia d
WHERE n.destino_tipo IS NULL
  AND n.tipo IN ('DENUNCIA_RECIBIDA', 'DENUNCIA_DESESTIMADA', 'INSTRUCTOR_SUSPENDIDO')
  AND d.id = n.entidad_id
  AND d.clase_id IS NOT NULL;

UPDATE notificacion n SET destino_tipo = 'PERFIL_INSTRUCTOR', destino_id = n.entidad_id
WHERE n.destino_tipo IS NULL
  AND n.tipo IN ('INSTRUCTOR_APROBADO', 'INSTRUCTOR_RECHAZADO');

-- Lo que quedo sin resolver (penalizaciones sueltas, actividades ya eliminadas, entidades
-- borradas) se muestra sin link en vez de llevar a una pantalla rota.
UPDATE notificacion SET destino_tipo = 'NINGUNO' WHERE destino_tipo IS NULL;

ALTER TABLE notificacion ALTER COLUMN destino_tipo SET DEFAULT 'NINGUNO';
ALTER TABLE notificacion ALTER COLUMN destino_tipo SET NOT NULL;
