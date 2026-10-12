package com.activehub.usecases.actualizarmisintereses;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios/me/intereses")
public class ActualizarMisInteresesController {

    private final ActualizarMisInteresesService actualizarMisInteresesService;

    public ActualizarMisInteresesController(ActualizarMisInteresesService actualizarMisInteresesService) {
        this.actualizarMisInteresesService = actualizarMisInteresesService;
    }

    @PutMapping
    public ActualizarMisInteresesResponse actualizar(
            @Valid @RequestBody ActualizarMisInteresesRequest request, Authentication authentication
    ) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return actualizarMisInteresesService.actualizar(usuarioId, request);
    }
}
