-- KAN-61: cuantas veces fallo la captura en Mercado Pago de un pago Retenido.
--
-- El scheduler reintenta la captura cada 5 minutos hasta que entra o vence la autorizacion.
-- Sin un contador no hay forma de distinguir un tropiezo de MP de un pago que no va a entrar
-- nunca (cuenta del instructor desvinculada, orden rechazada): con el contador se avisa a la
-- administracion al llegar a VentanaPagos.MAX_INTENTOS_CAPTURA, una sola vez.
--
-- Idempotente (ver la nota de V18).
ALTER TABLE pago ADD COLUMN IF NOT EXISTS intentos_captura INTEGER NOT NULL DEFAULT 0;
