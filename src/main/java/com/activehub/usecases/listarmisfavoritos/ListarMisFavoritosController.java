package com.activehub.usecases.listarmisfavoritos;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/favoritos")
public class ListarMisFavoritosController {

    private final ListarMisFavoritosService listarMisFavoritosService;

    public ListarMisFavoritosController(ListarMisFavoritosService listarMisFavoritosService) {
        this.listarMisFavoritosService = listarMisFavoritosService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ALUMNO')")
    public List<UUID> listar(Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        return listarMisFavoritosService.listar(alumnoId);
    }
}
