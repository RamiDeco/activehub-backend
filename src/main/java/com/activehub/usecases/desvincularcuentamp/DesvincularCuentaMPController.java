package com.activehub.usecases.desvincularcuentamp;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DesvincularCuentaMPController {

    private final DesvincularCuentaMPService service;

    public DesvincularCuentaMPController(DesvincularCuentaMPService service) {
        this.service = service;
    }

    @DeleteMapping("/api/instructor/mercadopago")
    @PreAuthorize("@permisos.puede('mercadopago.vincular')")
    public ResponseEntity<Void> desvincular(Authentication authentication) {
        service.desvincular((UUID) authentication.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
