package com.activehub.usecases.obtenercuentamercadopago;

import java.time.Instant;

/**
 * Estado de la vinculacion. NUNCA lleva tokens ni la public key: el frontend solo necesita
 * saber si mostrar "Vincular" o "Vinculada".
 *
 * @param vinculada true solo si el estado es VINCULADA y el token no vencio: es lo que decide
 *                  si el instructor puede cobrar con Mercado Pago hoy
 * @param estado    VINCULADA / DESVINCULADA / TOKEN_VENCIDO, o null si nunca vinculo
 */
public record ObtenerCuentaMercadoPagoResponse(
        boolean vinculada,
        String estado,
        String mpUserId,
        Instant actualizadaEl,
        Instant expiraEl
) {
}
