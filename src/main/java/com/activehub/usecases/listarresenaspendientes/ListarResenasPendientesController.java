package com.activehub.usecases.listarresenaspendientes;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resenas")
public class ListarResenasPendientesController {

    private final ListarResenasPendientesService listarResenasPendientesService;

    public ListarResenasPendientesController(ListarResenasPendientesService listarResenasPendientesService) {
        this.listarResenasPendientesService = listarResenasPendientesService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('denuncias.resolver')")
    public List<ListarResenasPendientesResponse> listar() {
        return listarResenasPendientesService.listar();
    }
}
