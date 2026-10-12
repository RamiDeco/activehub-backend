package com.activehub.usecases.recibirnotificacionmercadopago;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sin {@code @PreAuthorize} y en {@code permitAll} a proposito: lo llama Mercado Pago, sin JWT.
 * La seguridad la da la firma {@code x-signature}, que verifica el servicio.
 */
@RestController
public class RecibirNotificacionMercadoPagoController {

    private final RecibirNotificacionMercadoPagoService service;

    public RecibirNotificacionMercadoPagoController(RecibirNotificacionMercadoPagoService service) {
        this.service = service;
    }

    @PostMapping("/api/instructor/mercadopago/webhook")
    public ResponseEntity<Void> recibir(
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestParam(required = false) String type,
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId) {
        var resultado = service.procesar(new RecibirNotificacionMercadoPagoRequest(dataId, type, xSignature, xRequestId));
        // MP solo necesita saber si lo recibimos: 200 aunque lo hayamos ignorado (si no, reintenta
        // para siempre). 401 unicamente si la firma no es de Mercado Pago.
        return resultado == RecibirNotificacionMercadoPagoService.Resultado.FIRMA_INVALIDA
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
                : ResponseEntity.ok().build();
    }
}
