-- KAN-65: cuando se capturo en Mercado Pago un pago Retenido.
--
-- Retenido abarca dos momentos: autorizado sin capturar (hasta que la clase finaliza) y
-- capturado esperando el periodo de denuncias. Para reintegrar hay que saber en cual esta:
-- sin capturar se cancela la autorizacion; capturado se reembolsa. NULL = no capturado.
--
-- Idempotente (ver la nota de V18).
ALTER TABLE pago ADD COLUMN IF NOT EXISTS capturado_at TIMESTAMPTZ;
