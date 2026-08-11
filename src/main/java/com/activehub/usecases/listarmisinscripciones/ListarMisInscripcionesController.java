package com.activehub.usecases.listarmisinscripciones;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/inscripciones")
public class ListarMisInscripcionesController {

    private final ListarMisInscripcionesService listarMisInscripcionesService;

    public ListarMisInscripcionesController(ListarMisInscripcionesService listarMisInscripcionesService) {
        this.listarMisInscripcionesService = listarMisInscripcionesService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ALUMNO')")
    public List<ListarMisInscripcionesResponse> listar(
            @RequestParam(required = false) String estado, Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        return listarMisInscripcionesService.listar(alumnoId, estado);
    }
}
