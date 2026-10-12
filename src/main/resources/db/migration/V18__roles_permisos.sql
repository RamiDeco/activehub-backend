-- E4Ad-HU08 / RN-19: los permisos por rol se resuelven contra la base (ConfiguracionRol),
-- no con condicionales hardcodeados. Hasta acá la pantalla "Roles y permisos" era una
-- maqueta: los checkboxes vivían en el componente y no cambiaban nada.

-- Idempotente a propósito: la base es Supabase y las migraciones viajan por el pooler,
-- que no garantiza DDL transaccional — un intento fallido puede dejar objetos creados y
-- Flyway reintenta el script entero. Todo va con IF NOT EXISTS / ON CONFLICT.

-- rol: nombre libre (para roles nuevos creados por el admin) + metadatos de la tarjeta.
ALTER TABLE rol ALTER COLUMN nombre TYPE VARCHAR(40);
ALTER TABLE rol ADD COLUMN IF NOT EXISTS descripcion VARCHAR(200);
-- Los tres roles del sistema son los únicos que el motor de seguridad conoce (ALUMNO,
-- INSTRUCTOR, ADMIN): no se pueden borrar ni renombrar.
ALTER TABLE rol ADD COLUMN IF NOT EXISTS sistema BOOLEAN NOT NULL DEFAULT false;

UPDATE rol SET sistema = true WHERE nombre IN ('ALUMNO', 'INSTRUCTOR', 'ADMIN');
UPDATE rol SET descripcion = 'Explora actividades, se inscribe, reseña y reporta.' WHERE nombre = 'ALUMNO';
UPDATE rol SET descripcion = 'Publica actividades y clases, gestiona inscriptos y cobros.' WHERE nombre = 'INSTRUCTOR';
UPDATE rol SET descripcion = 'Gobierna la plataforma: usuarios, taxonomía, denuncias y reportes.' WHERE nombre = 'ADMIN';

-- permiso: catálogo fijo de acciones. Cada clave se corresponde con una guarda real en el
-- código; si agregás una clave nueva acá, tiene que haber un @PreAuthorize que la use.
CREATE TABLE IF NOT EXISTS permiso (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clave       VARCHAR(60) NOT NULL,
    modulo      VARCHAR(60) NOT NULL,
    accion      VARCHAR(120) NOT NULL,
    -- Crítico = sin él la plataforma se queda sin administración (E4Ad-HU08 criterio 6).
    critico     BOOLEAN NOT NULL DEFAULT false,
    orden       INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted     BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT uq_permiso_clave UNIQUE (clave)
);

CREATE TABLE IF NOT EXISTS configuracion_rol (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rol_id      UUID NOT NULL REFERENCES rol(id),
    permiso_id  UUID NOT NULL REFERENCES permiso(id),
    habilitado  BOOLEAN NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_configuracion_rol UNIQUE (rol_id, permiso_id)
);
CREATE INDEX IF NOT EXISTS ix_configuracion_rol_rol ON configuracion_rol (rol_id);

INSERT INTO permiso (clave, modulo, accion, critico, orden) VALUES
    ('catalogo.explorar',        'Catálogo',       'Explorar actividades y ver detalle',        false, 10),
    ('inscripciones.gestionar',  'Inscripciones',  'Preinscribirse, inscribirse y cancelar',    false, 20),
    ('resenias.escribir',        'Reseñas',        'Calificar clases finalizadas',              false, 30),
    ('denuncias.crear',          'Denuncias',      'Reportar una inasistencia',                 false, 40),
    ('actividades.publicar',     'Actividades',    'Crear, editar y eliminar actividades',      false, 50),
    ('clases.gestionar',         'Clases',         'Crear, editar, cancelar y ver el roster',   false, 60),
    ('cobros.confirmar',         'Cobros',         'Confirmar cobros en efectivo',              false, 70),
    ('usuarios.gestionar',       'Usuarios',       'Ver, editar y suspender cuentas',           true,  80),
    ('instructores.validar',     'Instructores',   'Aprobar o rechazar solicitudes',            false, 90),
    ('taxonomia.gestionar',      'Tipos y niveles','ABM de categorías y tipos de actividad',    false, 100),
    ('penalizaciones.gestionar', 'Penalizaciones', 'Aplicar y listar penalizaciones',           false, 110),
    ('denuncias.resolver',       'Denuncias',      'Auditar y resolver denuncias',              false, 120),
    ('auditoria.ver',            'Auditoría',      'Consultar la trazabilidad del sistema',     false, 130),
    ('reportes.ver',             'Reportes',       'Ver y exportar reportes',                   false, 140),
    ('roles.configurar',         'Roles',          'Configurar roles y permisos',               true,  150)
ON CONFLICT (clave) DO NOTHING;

-- Configuración inicial = lo que cada rol podía hacer hasta hoy, para que activar el motor
-- no le saque nada a nadie.
INSERT INTO configuracion_rol (rol_id, permiso_id, habilitado)
SELECT r.id, p.id,
       CASE
           WHEN r.nombre = 'ALUMNO' THEN p.clave IN (
               'catalogo.explorar', 'inscripciones.gestionar', 'resenias.escribir', 'denuncias.crear')
           WHEN r.nombre = 'INSTRUCTOR' THEN p.clave IN (
               'catalogo.explorar', 'actividades.publicar', 'clases.gestionar', 'cobros.confirmar')
           WHEN r.nombre = 'ADMIN' THEN p.clave <> 'inscripciones.gestionar'
           ELSE false
       END
FROM rol r
CROSS JOIN permiso p
WHERE r.deleted = false
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
