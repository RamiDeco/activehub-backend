package com.activehub.usecases.iniciarvinculacionmercadopago;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IniciarVinculacionMercadoPagoController {

    private final IniciarVinculacionMercadoPagoService service;

    public IniciarVinculacionMercadoPagoController(IniciarVinculacionMercadoPagoService service) {
        this.service = service;
    }

    @GetMapping("/api/instructor/mercadopago/vinculo")
    @PreAuthorize("@permisos.puede('mercadopago.vincular')")
    public IniciarVinculacionMercadoPagoResponse iniciar(Authentication authentication) {
        return service.iniciar((UUID) authentication.getPrincipal());
    }
}
