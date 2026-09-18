-- Verificacion de email por codigo de 6 digitos, y lo que eso cambia sobre la unicidad.
--
-- Regla pedida: mientras el codigo esta enviado y NO se ingreso, ese correo sigue libre y
-- otra persona puede usarlo. Recien cuando el usuario ingresa el codigo el correo queda
-- reservado para su cuenta, y solo se libera si el dueño lo cambia desde su perfil.
--
-- Eso es exactamente un indice unico PARCIAL: la unicidad se exige solo entre los correos ya
-- verificados. El indice viejo (ux_usuario_email_activo) la exigia sobre todas las cuentas
-- vivas, asi que un correo sin confirmar bloqueaba el alta de cualquier otro -- el escenario
-- clasico de alguien que se equivoca al tipear el correo de otra persona y se lo deja
-- inutilizable para siempre.
--
-- Idempotente: la base es Supabase y las migraciones viajan por el pooler, que no garantiza
-- DDL transaccional (ver la nota de V18).

ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_verificado    BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS email_verificado_at TIMESTAMPTZ;

-- Con que credencial se creo la cuenta. Una cuenta de Google no tiene contraseña utilizable
-- (se le pone un hash aleatorio para que `password_hash` siga siendo NOT NULL), asi que hay
-- que saberlo para no ofrecerle "cambiar contraseña" ni dejarla entrar por el login normal.
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS auth_proveedor VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

-- Las cuentas que ya existian se dan por verificadas. No se les puede pedir que confirmen un
-- correo retroactivamente (quedarian con el correo liberado para que otro lo tome), y ademas
-- el admin sembrado por `app.admin-seed` tiene que poder entrar sin pasar por un mail.
UPDATE usuario SET email_verificado = true, email_verificado_at = COALESCE(created_at, now())
WHERE email_verificado = false;

DROP INDEX IF EXISTS ux_usuario_email_activo;
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuario_email_verificado
    ON usuario (lower(email)) WHERE deleted = false AND email_verificado = true;

-- Varias cuentas sin verificar pueden compartir correo, asi que el login por correo dejo de
-- poder resolverse con un unico registro: este indice es el que sostiene esa consulta.
CREATE INDEX IF NOT EXISTS ix_usuario_email_lower ON usuario (lower(email)) WHERE deleted = false;

-- Un codigo emitido. Solo-append salvo `usado_at` e `intentos`, como notificacion.
--
--  * `email` es el correo AL QUE SE MANDO, que no siempre es el de la cuenta: en un cambio de
--    correo es el nuevo, y `usuario.email` recien se actualiza cuando el codigo se confirma.
--    Asi un error de tipeo en el correo nuevo no deja al usuario sin el viejo, que sigue
--    siendo su credencial verificada hasta que el cambio se completa.
--  * `codigo_hash`: el codigo no se guarda en claro. Son 6 digitos, pero es un secreto de un
--    solo uso y la tabla la puede leer cualquiera con acceso a la base.
--  * `proposito` decide el texto del mail (alta vs. cambio de correo) y no lleva CHECK, por la
--    misma razon que `notificacion.tipo`: el enum crece sin migracion.
CREATE TABLE IF NOT EXISTS verificacion_email (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id  UUID         NOT NULL REFERENCES usuario (id),
    email       VARCHAR(255) NOT NULL,
    codigo_hash VARCHAR(255) NOT NULL,
    proposito   VARCHAR(30)  NOT NULL,
    expira_at   TIMESTAMPTZ  NOT NULL,
    usado_at    TIMESTAMPTZ,
    intentos    INTEGER      NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- La consulta caliente es "el ultimo codigo vigente de este usuario".
CREATE INDEX IF NOT EXISTS ix_verificacion_email_usuario
    ON verificacion_email (usuario_id, created_at DESC);
