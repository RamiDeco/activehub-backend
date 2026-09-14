package com.activehub.usecases.listarinscripcionesmisclases;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/inscripciones")
public class ListarInscripcionesMisClasesController {

    private final ListarInscripcionesMisClasesService listarInscripcionesMisClasesService;

    public ListarInscripcionesMisClasesController(
            ListarInscripcionesMisClasesService listarInscripcionesMisClasesService) {
        this.listarInscripcionesMisClasesService = listarInscripcionesMisClasesService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('clases.gestionar')")
    public List<ListarInscripcionesMisClasesResponse> listar(Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return listarInscripcionesMisClasesService.listar(instructorId);
    }
}
