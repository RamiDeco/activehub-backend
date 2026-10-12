-- KAN-58: Mercado Pago puede no decidir en el momento (status `processing` / `in_review` en
-- la Orders API: analisis antifraude o revision manual). El pago queda `EnRevision` hasta que
-- el webhook (KAN-60) informe el resultado.
--
-- El estado nuevo va en el PAGO y no en la inscripcion a proposito: para la inscripcion,
-- "PagoPendiente" ya significa lo correcto en los dos casos (tiene el lugar reservado y el
-- pago no esta confirmado); lo que cambia es POR QUE el pago esta pendiente y quien lo
-- resuelve, y eso es del pago.
--
-- Idempotente (ver la nota de V18): DROP + ADD del CHECK nombrado en V28.
ALTER TABLE pago DROP CONSTRAINT IF EXISTS ck_pago_estado;
ALTER TABLE pago ADD CONSTRAINT ck_pago_estado
    CHECK (estado IN ('Retenido', 'Liberado', 'Cancelado', 'Efectivo', 'Rechazado', 'Reembolsado', 'EnRevision'));
