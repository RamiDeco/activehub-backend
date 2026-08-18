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

    @GetMapping
    @PreAuthorize("hasRole('ALUMNO')")
    public List<ListarMisDenunciasResponse> listar(Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        return listarMisDenunciasService.listar(alumnoId);
    }
}
