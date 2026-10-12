-- Vincular la cuenta de Mercado Pago del instructor por OAuth (KAN-54 / MP-05).
--
-- Permiso propio y no `cobros.confirmar`: confirmar un cobro en efectivo y entregarle a la
-- plataforma acceso a una cuenta de Mercado Pago son cosas distintas, y el admin tiene que
-- poder habilitar una sin la otra desde "Roles y permisos". `GuardasDePermisoTest` exige que
-- la clave exista aca y que la usen los @PreAuthorize de los dos endpoints del instructor.
--
-- Idempotente: la base es Supabase y las migraciones viajan por el pooler, que no garantiza
-- DDL transaccional (ver la nota de V18).

INSERT INTO permiso (clave, modulo, accion, critico, orden) VALUES
    ('mercadopago.vincular', 'Cobros', 'Vincular su cuenta de Mercado Pago', false, 75)
ON CONFLICT (clave) DO NOTHING;

-- Solo el INSTRUCTOR: es quien cobra. Los demas roles nacen sin el (la fila se crea igual,
-- en false, para que la matriz de "Roles y permisos" este completa).
INSERT INTO configuracion_rol (rol_id, permiso_id, habilitado)
SELECT r.id, p.id, r.nombre = 'INSTRUCTOR'
FROM rol r
CROSS JOIN permiso p
WHERE r.deleted = false
  AND p.clave = 'mercadopago.vincular'
ON CONFLICT (rol_id, permiso_id) DO NOTHING;
