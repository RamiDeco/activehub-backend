package com.activehub.usecases.listarmisdenuncias;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/denuncias")
public class ListarMisDenunciasController {

    private final ListarMisDenunciasService listarMisDenunciasService;

    public ListarMisDenunciasController(ListarMisDenunciasService listarMisDenunciasService) {
        this.listarMisDenunciasService = listarMisDenunciasService;
    }

    // También INSTRUCTOR: desde que puede denunciar una reseña (E2I-HU11) necesita ver
    // el seguimiento de la suya, igual que el alumno.
    @GetMapping
    // Devuelve SOLO las denuncias de quien llama, así que la guarda es "¿puede denunciar
    // algo?": el alumno reporta inasistencias y el instructor reporta reseñas (RN-19).
    @PreAuthorize("@permisos.puede('denuncias.crear') or @permisos.puede('resenias.responder')")
    public List<ListarMisDenunciasResponse> listar(Authentication authentication) {
        UUID denuncianteId = (UUID) authentication.getPrincipal();
        return listarMisDenunciasService.listar(denuncianteId);
    }
}
