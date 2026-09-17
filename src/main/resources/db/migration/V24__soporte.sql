-- Reportes de soporte: el formulario "Reportar un problema" de /ayuda.
--
-- Reportado: "toda la seccion de Soporte no funciona". El form existia desde el primer dia
-- pero `submitReport` solo hacia `setReportSent(true)` y descartaba lo escrito: le decia
-- "¡Gracias! Recibimos tu reporte" a alguien cuyo reporte no se guardaba en ningun lado.
-- No habia tabla, ni endpoint, ni caso de uso.
--
-- `usuario_id` es NULLABLE a proposito: /ayuda es publica y quien necesita soporte muchas
-- veces es justamente alguien que no pudo crear su cuenta. El `email` si es obligatorio,
-- porque sin cuenta y sin correo no hay forma de responder.
--
-- Idempotente: la base es Supabase y las migraciones viajan por el pooler, que no garantiza
-- DDL transaccional (ver la nota de V18).

CREATE TABLE IF NOT EXISTS reporte_soporte (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id    UUID REFERENCES usuario (id),
    email         VARCHAR(150) NOT NULL,
    asunto        VARCHAR(150) NOT NULL,
    detalle       TEXT         NOT NULL,
    estado        VARCHAR(20)  NOT NULL DEFAULT 'Abierto',
    respuesta     TEXT,
    cerrado_por_id UUID REFERENCES usuario (id),
    cerrado_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_reporte_soporte_estado') THEN
        ALTER TABLE reporte_soporte ADD CONSTRAINT ck_reporte_soporte_estado
            CHECK (estado IN ('Abierto', 'Cerrado'));
    END IF;
END $$;

-- La bandeja se ordena por estado y antiguedad; con pocos cientos de filas alcanza, pero el
-- indice evita que crezca mal cuando la tabla sea solo-append durante meses.
CREATE INDEX IF NOT EXISTS ix_reporte_soporte_estado_created
    ON reporte_soporte (estado, created_at);

-- Permiso propio: es un modulo nuevo, con su pestaña en Gestion. `GuardasDePermisoTest` exige
-- que toda clave del catalogo gatee algo y que toda guarda use una clave del catalogo, asi que
-- la fila y los dos @PreAuthorize de `listarreportessoporte`/`cerrarreportesoporte` van juntos.
INSERT INTO permiso (clave, modulo, accion, critico, orden) VALUES
    ('soporte.gestionar', 'Soporte', 'Ver y cerrar los reportes de soporte', false, 155)
ON CONFLICT (clave) DO NOTHING;

-- Solo el ADMIN, como el resto de los modulos de administracion. Los demas roles nacen sin el
-- (la fila se crea igual, en false, para que la matriz de "Roles y permisos" este completa).
INSERT INTO configuracion_rol (rol_id, permiso_id, habilitado)
SELECT r.id, p.id, r.nombre = 'ADMIN'
FROM rol r
CROSS JOIN permiso p
WHERE r.deleted = false
  AND p.clave = 'soporte.gestionar'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
