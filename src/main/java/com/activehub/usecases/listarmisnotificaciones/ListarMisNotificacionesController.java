package com.activehub.usecases.listarmisnotificaciones;

import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class ListarMisNotificacionesController {

    private final ListarMisNotificacionesService listarMisNotificacionesService;

    public ListarMisNotificacionesController(ListarMisNotificacionesService listarMisNotificacionesService) {
        this.listarMisNotificacionesService = listarMisNotificacionesService;
    }

    @GetMapping
    public List<ListarMisNotificacionesResponse> listar(Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return listarMisNotificacionesService.listar(usuarioId);
    }
}
