package com.activehub.usecases.listarreseniaspublicadas;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/resenas/publicadas")
public class ListarReseniasPublicadasController {

    private final ListarReseniasPublicadasService listarReseniasPublicadasService;

    public ListarReseniasPublicadasController(ListarReseniasPublicadasService listarReseniasPublicadasService) {
        this.listarReseniasPublicadasService = listarReseniasPublicadasService;
    }

    @GetMapping
    @PreAuthorize("@permisos.puede('denuncias.resolver')")
    public List<ListarReseniasPublicadasResponse> listar() {
        return listarReseniasPublicadasService.listar();
    }
}
