package com.activehub.usecases.marcartodasnotificacionesleidas;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class MarcarTodasNotificacionesLeidasController {

    private final MarcarTodasNotificacionesLeidasService marcarTodasNotificacionesLeidasService;

    public MarcarTodasNotificacionesLeidasController(
            MarcarTodasNotificacionesLeidasService marcarTodasNotificacionesLeidasService) {
        this.marcarTodasNotificacionesLeidasService = marcarTodasNotificacionesLeidasService;
    }

    @PostMapping("/marcar-leidas")
    public void marcarLeidas(Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        marcarTodasNotificacionesLeidasService.marcarTodasLeidas(usuarioId);
    }
}
