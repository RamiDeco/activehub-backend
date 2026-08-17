package com.activehub.usecases.listarmisresenas;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alumno/resenas")
public class ListarMisResenasController {

    private final ListarMisResenasService listarMisResenasService;

    public ListarMisResenasController(ListarMisResenasService listarMisResenasService) {
        this.listarMisResenasService = listarMisResenasService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ALUMNO')")
    public List<ListarMisResenasResponse> listar(Authentication authentication) {
        UUID alumnoId = (UUID) authentication.getPrincipal();
        return listarMisResenasService.listar(alumnoId);
    }
}
