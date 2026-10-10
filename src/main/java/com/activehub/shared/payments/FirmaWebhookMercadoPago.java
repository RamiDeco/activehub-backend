package com.activehub.shared.payments;

import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Verifica que una notificacion (webhook) la haya mandado de verdad Mercado
 * Pago (KAN-60).
 */
@Component
public class FirmaWebhookMercadoPago {

    private static final Logger log = LoggerFactory.getLogger(FirmaWebhookMercadoPago.class);

    private final String secret;

    public FirmaWebhookMercadoPago(@Value("${app.mercadopago.webhook-secret:}") String secret) {
        this.secret = secret;
    }

    /**
     * @param xSignature el header {@code x-signature} tal como llego (puede ser null)
     * @param xRequestId el header {@code x-request-id} (puede ser null)
     * @param dataId     el query param {@code data.id} (puede ser null)
     * @return true solo si la firma es de Mercado Pago. Ante cualquier duda (secreto sin
     *         configurar, header ausente o mal formado), false: nunca procesar algo no verificado.
     */
    public boolean esValida(String xSignature, String xRequestId, String dataId) {
        if (xSignature == null) {
            log.warn("Firma invalida: xSignature={}, xRequestId={}, dataId={}", xSignature, xRequestId, dataId);
            return false;
        }

        if ( secret == null || secret.isBlank()) {
            // Error y no warn: con el secreto vacio se rechazan TODAS las notificaciones.
            log.error("MP_WEBHOOK_SECRET sin configurar: no se puede verificar ninguna notificacion de Mercado Pago");
            return false;
        }

        String v1 = null;
        String ts = null;
        
        String[] xSignatureArray = xSignature.split(",");
        for (String s : xSignatureArray) {
            String[] sa= s.split("=", 2);
            if (sa.length != 2) {
                continue;
            }
            if (sa[0].trim().equals("v1")) {
                v1 = sa[1].trim();
            } else if (sa[0].trim().equals("ts")) {
                ts = sa[1].trim();
            }
        }

        if (v1 == null || v1.isEmpty() || ts == null || ts.isEmpty()) {
            log.warn("Firma invalida: xSignature={}, xRequestId={}, dataId={}", xSignature, xRequestId, dataId);
            return false;
        }

        String signedTemplate = null;

        if (dataId != null) {
            signedTemplate = "id:" + dataId.toLowerCase() + ";";
        }

        if (xRequestId != null) {
            signedTemplate = (signedTemplate == null ? "" : signedTemplate) + "request-id:" + xRequestId + ";";
        }

        signedTemplate = (signedTemplate == null ? "" : signedTemplate) + "ts:" + ts + ";";

        String cyphedSignature = new HmacUtils("HmacSHA256", secret).hmacHex(signedTemplate);
        return cyphedSignature.equals(v1);
    }
}
