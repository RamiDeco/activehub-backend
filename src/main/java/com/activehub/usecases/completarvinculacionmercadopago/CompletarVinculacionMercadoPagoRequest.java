package com.activehub.usecases.completarvinculacionmercadopago;

/**
 * Query params con los que Mercado Pago vuelve al callback. Si el instructor cancela en la
 * pantalla de MP no viene {@code code} (viene {@code error}).
 */
public record CompletarVinculacionMercadoPagoRequest(String code, String state, String error) {
}
