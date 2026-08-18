package com.activehub.usecases.listarauditoria;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auditoria")
public class ListarAuditoriaController {

    private final ListarAuditoriaService listarAuditoriaService;

    public ListarAuditoriaController(ListarAuditoriaService listarAuditoriaService) {
        this.listarAuditoriaService = listarAuditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<ListarAuditoriaResponse> listar() {
        return listarAuditoriaService.listar();
    }
}
