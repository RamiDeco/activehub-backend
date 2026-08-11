package com.activehub.usecases.obtenerusuarioactual;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/me")
public class ObtenerUsuarioActualController {

    private final ObtenerUsuarioActualService obtenerUsuarioActualService;

    public ObtenerUsuarioActualController(ObtenerUsuarioActualService obtenerUsuarioActualService) {
        this.obtenerUsuarioActualService = obtenerUsuarioActualService;
    }

    @GetMapping
    public ObtenerUsuarioActualResponse obtenerActual(Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return obtenerUsuarioActualService.obtener(usuarioId);
    }
}
