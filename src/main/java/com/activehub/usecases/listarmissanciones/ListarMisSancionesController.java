package com.activehub.usecases.listarmissanciones;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instructor/sanciones")
public class ListarMisSancionesController {

    private final ListarMisSancionesService listarMisSancionesService;

    public ListarMisSancionesController(ListarMisSancionesService listarMisSancionesService) {
        this.listarMisSancionesService = listarMisSancionesService;
    }

    @GetMapping
    // Sin permiso: son las sanciones de quien llama, no un modulo. El que no tiene ninguna
    // recibe la lista vacia.
    public ListarMisSancionesResponse listar(Authentication authentication) {
        return listarMisSancionesService.listar((UUID) authentication.getPrincipal());
    }
}
