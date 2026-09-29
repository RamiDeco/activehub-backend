package com.activehub.usecases.obtenercuentamercadopago;

import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ObtenerCuentaMercadoPagoController {

    private final ObtenerCuentaMercadoPagoService service;

    public ObtenerCuentaMercadoPagoController(ObtenerCuentaMercadoPagoService service) {
        this.service = service;
    }

    @GetMapping("/api/instructor/mercadopago")
    @PreAuthorize("@permisos.puede('mercadopago.vincular')")
    public ObtenerCuentaMercadoPagoResponse obtener(Authentication authentication) {
        return service.obtener((UUID) authentication.getPrincipal());
    }
}
