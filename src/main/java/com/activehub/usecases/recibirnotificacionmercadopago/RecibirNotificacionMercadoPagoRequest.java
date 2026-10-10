package com.activehub.usecases.recibirnotificacionmercadopago;

/**
 * Lo que importa de una notificacion de Mercado Pago. El BODY no esta a proposito: no se le
 * cree (ver el servicio); solo se usa el id de la orden para consultarla.
 *
 * @param dataId     query param {@code data.id}: el id de la orden
 * @param type       query param {@code type}: solo se procesa {@code order}
 * @param xSignature header con la firma ({@code ts=...,v1=...})
 * @param xRequestId header que entra en el manifest de la firma
 */
public record RecibirNotificacionMercadoPagoRequest(String dataId, String type, String xSignature, String xRequestId) {
}
