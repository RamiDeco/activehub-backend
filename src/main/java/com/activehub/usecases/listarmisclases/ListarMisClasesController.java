package com.activehub.usecases.listarmisclases;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/clases")
public class ListarMisClasesController {

    private final ListarMisClasesService listarMisClasesService;

    public ListarMisClasesController(ListarMisClasesService listarMisClasesService) {
        this.listarMisClasesService = listarMisClasesService;
    }

    @GetMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public List<ListarMisClasesResponse> listar(Authentication authentication) {
        UUID instructorId = (UUID) authentication.getPrincipal();
        return listarMisClasesService.listar(instructorId);
    }
}
