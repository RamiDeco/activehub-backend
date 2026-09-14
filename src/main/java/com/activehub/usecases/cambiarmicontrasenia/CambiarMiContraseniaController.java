package com.activehub.usecases.cambiarmicontrasenia;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me/password")
public class CambiarMiContraseniaController {

    private final CambiarMiContraseniaService cambiarMiContraseniaService;

    public CambiarMiContraseniaController(CambiarMiContraseniaService cambiarMiContraseniaService) {
        this.cambiarMiContraseniaService = cambiarMiContraseniaService;
    }

    @PostMapping
    public ResponseEntity<Void> cambiar(
            @Valid @RequestBody CambiarMiContraseniaRequest request, Authentication authentication
    ) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        cambiarMiContraseniaService.cambiar(usuarioId, request);
        return ResponseEntity.noContent().build();
    }
}
