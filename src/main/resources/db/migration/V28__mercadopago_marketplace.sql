-- Marketplace de Mercado Pago (KAN-52 / MP-03): solo modelo de datos, sin logica.
--
-- Decisiones de MP-01 (KAN-50) que condicionan este esquema:
--   * Orders API (/v1/orders): el identificador externo del pago es un order_id.
--   * Captura diferida: se autoriza al inscribirse y se captura al pasar la clase a Finalizada.
--     La autorizacion dura 5 dias, de ahi `autorizado_hasta`.
--   * La plataforma no cobra comision hoy; `comision` queda en 0 pero existe para no migrar
--     de nuevo el dia que se cobre.
--
-- Idempotente: la base es Supabase y las migraciones viajan por el pooler, que no garantiza
-- DDL transaccional (ver la nota de V18).

-- cuenta_mercadopago_instructor ---------------------------------------------------
-- Una por instructor. Los tokens OAuth se guardan CIFRADOS (AES-GCM, clave en
-- MP_TOKEN_ENCRYPTION_KEY) por `TokenCifradoConverter`: son credenciales que permiten cobrar
-- en nombre del instructor, y un volcado de la base no tiene que alcanzar para usarlas.
-- TEXT porque el cifrado + IV + base64 no tiene un largo fijo acotable con seguridad.
CREATE TABLE IF NOT EXISTS cuenta_mercadopago_instructor (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    instructor_id  UUID NOT NULL REFERENCES usuario (id),
    mp_user_id     VARCHAR(50),
    access_token   TEXT,
    refresh_token  TEXT,
    public_key     VARCHAR(100),
    expires_at     TIMESTAMPTZ,
    estado         VARCHAR(20) NOT NULL DEFAULT 'VINCULADA',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_cuenta_mp_instructor') THEN
        ALTER TABLE cuenta_mercadopago_instructor ADD CONSTRAINT uq_cuenta_mp_instructor
            UNIQUE (instructor_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cuenta_mp_estado') THEN
        ALTER TABLE cuenta_mercadopago_instructor ADD CONSTRAINT ck_cuenta_mp_estado
            CHECK (estado IN ('VINCULADA', 'DESVINCULADA', 'TOKEN_VENCIDO'));
    END IF;
END $$;

-- pago: campos de Mercado Pago -----------------------------------------------------
ALTER TABLE pago ADD COLUMN IF NOT EXISTS mp_order_id      VARCHAR(100);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS status_detail    VARCHAR(100);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS comision         NUMERIC(10,2) NOT NULL DEFAULT 0;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS monto_instructor NUMERIC(10,2);
ALTER TABLE pago ADD COLUMN IF NOT EXISTS autorizado_hasta TIMESTAMPTZ;
ALTER TABLE pago ADD COLUMN IF NOT EXISTS idempotency_key  VARCHAR(100);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_pago_idempotency_key') THEN
        ALTER TABLE pago ADD CONSTRAINT uq_pago_idempotency_key UNIQUE (idempotency_key);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pago_comision') THEN
        ALTER TABLE pago ADD CONSTRAINT ck_pago_comision CHECK (comision >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_pago_mp_order_id ON pago (mp_order_id);

-- EstadoPago suma Rechazado y Reembolsado. El CHECK de V5 no tenia nombre (Postgres lo llamo
-- pago_estado_check); se reemplaza por uno nombrado. DROP + ADD es idempotente.
ALTER TABLE pago DROP CONSTRAINT IF EXISTS pago_estado_check;
ALTER TABLE pago DROP CONSTRAINT IF EXISTS ck_pago_estado;
ALTER TABLE pago ADD CONSTRAINT ck_pago_estado
    CHECK (estado IN ('Retenido', 'Liberado', 'Cancelado', 'Efectivo', 'Rechazado', 'Reembolsado'));
