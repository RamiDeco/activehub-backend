package com.activehub.usecases.completarvinculacionmercadopago;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sin {@code @PreAuthorize} y en {@code permitAll} a proposito: lo llama el navegador al
 * volver de Mercado Pago, sin JWT. Quien es el instructor lo dice el {@code state} firmado,
 * que solo se obtiene desde {@code /vinculo}, que SI exige {@code mercadopago.vincular}.
 */
@RestController
public class CompletarVinculacionMercadoPagoController {

    private final CompletarVinculacionMercadoPagoService service;

    public CompletarVinculacionMercadoPagoController(CompletarVinculacionMercadoPagoService service) {
        this.service = service;
    }

    @GetMapping("/api/instructor/mercadopago/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        String destino = service.completar(new CompletarVinculacionMercadoPagoRequest(code, state, error));
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(destino)).build();
    }
}
