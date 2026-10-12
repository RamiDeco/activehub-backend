package com.activehub.usecases.darmedebaja;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me")
public class DarmeDeBajaController {

    private final DarmeDeBajaService darmeDeBajaService;

    public DarmeDeBajaController(DarmeDeBajaService darmeDeBajaService) {
        this.darmeDeBajaService = darmeDeBajaService;
    }

    @DeleteMapping
    public ResponseEntity<Void> darDeBaja(Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        darmeDeBajaService.darDeBaja(usuarioId);
        return ResponseEntity.noContent().build();
    }
}
