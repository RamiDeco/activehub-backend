package com.activehub.usecases.listarnivelesintensidad;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Público, igual que categorías y tipos: los filtros del catálogo (E3A-HU02) necesitan la
 * lista de niveles antes de que nadie inicie sesión.
 */
@RestController
@RequestMapping("/api/niveles-intensidad")
public class ListarNivelesIntensidadController {

    private final ListarNivelesIntensidadService listarNivelesIntensidadService;

    public ListarNivelesIntensidadController(ListarNivelesIntensidadService listarNivelesIntensidadService) {
        this.listarNivelesIntensidadService = listarNivelesIntensidadService;
    }

    @GetMapping
    public List<ListarNivelesIntensidadResponse> listar() {
        return listarNivelesIntensidadService.listar();
    }
}
